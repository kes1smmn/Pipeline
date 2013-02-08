package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;

/**
 * A smallish utility to read QC data from qc.json files
 * @author brendan
 *
 */
public class QCJsonReader {

	static DecimalFormat formatter = new DecimalFormat("0.0##");
	static DecimalFormat smallFormatter = new DecimalFormat("0.00000");
	
	private static JSONObject toJSONObj(String path) throws IOException, JSONException {
		File file = new File(path);
		if (file.isDirectory()) {
			//If file is a directory, see if it's in the 'reviewdir' format
			File qcDir = new File(file.getAbsoluteFile() + "/qc/");
			if (qcDir.exists() && qcDir.isDirectory()) {
				File qcFile = new File(qcDir.getPath() + "/qc.json");
				if (qcFile.exists()) {
					file = qcFile;
				}
			}
			
		}
		
		StringBuilder str = new StringBuilder();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = reader.readLine();
		while(line != null) {
			str.append(line);
			line = reader.readLine();
		}
		reader.close();
		return new JSONObject(str.toString());
	}
	
	private static String safeDouble(JSONObject obj, String key) {
		if (obj.has(key)) {
			try {
				return "" + formatter.format(obj.getDouble(key));
			} catch (JSONException e) {
				return "Error";
			}
		}
		else {
			return "Not found";
		}
	}
	
	private static String safeInt(JSONObject obj, String key) {
		if (obj.has(key)) {
			try {
				return "" + obj.getInt(key);
			} catch (JSONException e) {
				return "Error";
			}
		}
		else {
			return "Not found";
		}
	}
	
	private static String safeLong(JSONObject obj, String key) {
		if (obj.has(key)) {
			try {
				return "" + obj.getLong(key);
			} catch (JSONException e) {
				return "Error";
			}
		}
		else {
			return "Not found";
		}
	}
	
	/**
	 * Display some quick summary information about the QC data
	 * @param paths
	 */
	public static void performSummary(List<String> paths, PrintStream output) {
		
		for(String path : paths) {
			try {
				JSONObject obj = toJSONObj(path);
				
				output.println("\t Bases & Alignment summary:");
				if (obj.has("raw.bam.metrics")) {
					JSONObject rawBamMetrics = obj.getJSONObject("raw.bam.metrics");
					JSONObject finalBamMetrics = obj.getJSONObject("final.bam.metrics");
					
					Long baseCount = rawBamMetrics.getLong("bases.read");
					Long baseQ30 = rawBamMetrics.getLong("bases.above.q30");
					Long baseQ10 = rawBamMetrics.getLong("bases.above.q10");
					
					output.println("Raw bases read: " + baseCount);
					output.println("Raw % > Q30 : " + formatter.format(100.0*(double)baseQ30 / (double)baseCount));
					output.println("Raw % > Q10 : " + formatter.format(100.0*(double)baseQ10 / (double)baseCount));
					
					long totRawReads = rawBamMetrics.getLong("total.reads");
					long unmappedRawReads = rawBamMetrics.getLong("unmapped.reads");
					long totFinalReads = finalBamMetrics.getLong("total.reads");
					long unmappedFinalReads = finalBamMetrics.getLong("unmapped.reads");
					double rawUnmappedFrac = (double)unmappedRawReads / (double) totRawReads;
					double finalUnmappedFrac = (double)unmappedFinalReads / (double) totFinalReads;
					output.println("Total raw/final reads: " + totRawReads +",  " + totFinalReads);
					output.println("Fraction unmapped raw/final : " + formatter.format(rawUnmappedFrac) +",  " + formatter.format(finalUnmappedFrac));
				}
				else {
					output.println("No raw base metrics found");
				}
				
				output.println("\n\t Coverage summary:");
				if (obj.has("raw.coverage.metrics") && obj.has("final.coverage.metrics")) {
					
					JSONObject rawCov = obj.getJSONObject("raw.coverage.metrics");
					JSONObject finalCov = obj.getJSONObject("final.coverage.metrics");
					
					JSONArray rawFracCov = rawCov.getJSONArray("fraction.above.cov.cutoff");
					JSONArray finalFracCov = finalCov.getJSONArray("fraction.above.cov.cutoff");
					JSONArray rawCovCutoff = rawCov.getJSONArray("coverage.cutoffs");
					JSONArray finalCovCutoff = rawCov.getJSONArray("coverage.cutoffs");

					if (finalCovCutoff.length() != rawCovCutoff.length()) {
						//This would be really weird, but not impossible I guess
						System.err.println("Raw and final coverage cutoffs are not the same!!");
					}
					
					output.println("\t Raw \t Final:");
					for(int i=0; i<rawFracCov.length(); i++) {
						output.println("% > " + rawCovCutoff.getInt(i) + ":\t" + formatter.format(rawFracCov.getDouble(i)) + "\t" + formatter.format(finalFracCov.getDouble(i)));
					}
			
				}
				else {
					System.out.println("No raw coverage metrics found");
				}
				
				
				
				//Variant rundown
				if (obj.has("variant.metrics") ) {
					output.println("\n\t Variant summary:");
					JSONObject varMetrics = obj.getJSONObject("variant.metrics");
					output.println("Total variants: " + safeInt(varMetrics, "total.vars"));
					output.println("Total snps: " + safeInt(varMetrics, "total.snps"));
					output.println("Overall TT: " + safeDouble(varMetrics, "total.tt.ratio"));
					output.println("Known / Novel TT: " + safeDouble(varMetrics, "known.tt") + " / " + safeDouble(varMetrics, "novel.tt"));
					Double totVars = varMetrics.getDouble("total.vars");
					Double knowns = varMetrics.getDouble("total.known");
					if (totVars > 0) {
						Double novelFrac = 1.0 - knowns / totVars;
						output.println("Fraction novel: " + formatter.format(novelFrac));
					}
					//output.println("Total novels: " + safeDouble(varMetrics, " "));
					
					if (obj.has("capture.extent")) {
						long extent = obj.getLong("capture.extent");
						double varsPerBaseCalled = totVars / (double)extent;
						output.println("Vars per base called: " + smallFormatter.format(varsPerBaseCalled));
					}
					else {
						output.println("Vars per base called: No capture.extent available");
					}
					
				}
				else {
					output.println("No variant metrics found");
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	
	public static void main(String[] args) {
		
		if (args.length==0) {
			System.err.println("Enter <command> qcfile1 [qcfile2 ...]");
			return;
		}
		String command = args[0];
		List<String> paths = new ArrayList<String>();
		for(int i=1; i<args.length; i++) {
			paths.add(args[i]);
		}
		
		if (command.equals("summary")) {
			performSummary(paths, System.out);
		}
		
		if (command.equals("varSummary")) {
			performVarSummary(paths, System.out);
		}
		
		if (command.equals("covSummary")) {
			performCovSummary(paths, System.out);
		}
		
	}

	private static void performCovSummary(List<String> paths, PrintStream out) {
		TextTable data = new TextTable(new String[]{"Mean", ">5", ">10", ">20", ">50"});
		for(String path : paths) {
			try {
				JSONObject obj = toJSONObj(path);
				JSONObject finalCov = obj.getJSONObject("final.coverage.metrics");
				JSONArray fracAbove = finalCov.getJSONArray("fraction.above.index");
				Double mean = finalCov.getDouble("mean.coverage");
				String[] covs = new String[5];
				covs[0] = formatter.format(mean);
				covs[1] = formatter.format(fracAbove.getDouble(5));
				covs[2] = formatter.format(fracAbove.getDouble(10));
				covs[3] = formatter.format(fracAbove.getDouble(20));
				covs[4] = formatter.format(fracAbove.getDouble(50));
				data.addColumn(toSampleName(path), covs);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		out.println(data.toString());
	}

	private static String toSampleName(String path) throws IOException {
		File file = new File(path);
		if (file.isDirectory()) {
			File manifestFile = new File(file.getPath() + "/sampleManifest.txt");
			if (manifestFile.exists()) {
				return sampleIDFromManifest(manifestFile);
			}
		}
		
		String shortPath = path.split("/")[0];
		return shortPath.replace(".reviewdir", "");
	}

	private static String sampleIDFromManifest(File manifestFile) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(manifestFile));
		String line = reader.readLine();
		while(line != null) {
			if (line.contains("sample.name")) {
				String[] toks = line.split("=");
				if (toks.length==2) {
					return toks[1];
				}
			}
			line = reader.readLine();
		}
		reader.close();
		return null;
	}

	private static void performVarSummary(List<String> paths, PrintStream out) {
		TextTable data = new TextTable(new String[]{"Total.vars", "SNPS", "% Novel", "Ti/Tv ratio",  "Novel Ti/Tv", "Vars / Base"});
		for(String path : paths) {
			try {
				JSONObject obj = toJSONObj(path);
				JSONObject vars = obj.getJSONObject("variant.metrics");
				String[] col = new String[6];
				col[0] = "" + vars.getInt("total.vars");
				col[1] = "" + vars.getInt("total.snps");
				
				Double totVars = vars.getDouble("total.vars");
				Double knowns = vars.getDouble("total.known");
				if (totVars > 0) {
					Double novelFrac = 1.0 - knowns / totVars;
					col[2] = "" + formatter.format(novelFrac);
				}
				else {
					col[2] = "0";
				}
				col[3] = formatter.format( vars.getDouble("total.tt.ratio"));
				col[4] = formatter.format( vars.getDouble("novel.tt"));
				
				//output.println("Total novels: " + safeDouble(varMetrics, " "));
				
				if (obj.has("capture.extent")) {
					long extent = obj.getLong("capture.extent");
					double varsPerBaseCalled = totVars / (double)extent;
					col[5] = smallFormatter.format(varsPerBaseCalled);	
				}
				else {
					col[5] = "?";
				}
				
				data.addColumn(toSampleName(path), col);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		out.println(data.toString());
	}
}
