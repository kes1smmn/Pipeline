package operator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import buffer.FileBuffer;
import buffer.TextBuffer;
import buffer.VCFFile;

/**
 * A class that compares the calls in a .vcf file to those in a tabular "true" variants file
 * @author brendan
 *
 */
public class SimComparison extends IOOperator {

	
	protected Map<Integer, Integer> simVariantMap = new HashMap<Integer, Integer>();
	protected Map<Integer, Integer> trueVariantMap = new HashMap<Integer, Integer>();
	
	
	private void buildSimMap(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = reader.readLine();
		
		//skip initial comments, etc
		while(line != null && line.startsWith("#"))
			line = reader.readLine();
		
		while(line != null) {
			String[] toks = line.split("\\s");
			int pos = Integer.parseInt(toks[1]);
			simVariantMap.put(pos, 1);
			line = reader.readLine();
		}
		reader.close();
	}
	
	private String inspectSim(File file, List<Integer> positions) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = reader.readLine();
		Collections.sort(positions);
		
		//skip initial comments, etc
		while(line != null && line.startsWith("#"))
			line = reader.readLine();
		
		StringBuffer msg = new StringBuffer();
		
		while(line != null) {
			String[] toks = line.split("\\s");
			int pos = Integer.parseInt(toks[1]);
			if (Collections.binarySearch(positions, pos) >= 0) {
				msg.append("false.pos.line=\"" + line + " \" \n");
			}
			
			line = reader.readLine();
		}
		reader.close();
		return msg.toString();
	}
	
	private void buildTrueMap(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = reader.readLine();
		
		//skip initial comments, etc
		while(line != null && line.startsWith("#"))
			line = reader.readLine();
		
		while(line != null) {
			String[] toks = line.split("\\s");
			int pos = Integer.parseInt(toks[0]);
			pos++; //Convert from 0 coords to 1-indexed coords!
			trueVariantMap.put(pos, 1);
			line = reader.readLine();
		}
		reader.close();
	}
	
	public void performOperation() {
		FileBuffer simVariants = getInputBufferForClass(VCFFile.class);
		FileBuffer trueVariants = getInputBufferForClass(TextBuffer.class);
		
		FileBuffer reportBuffer = outputBuffers.get(0);
		
		try {

			BufferedWriter report = new BufferedWriter(new FileWriter(reportBuffer.getFile()));
			
			buildTrueMap(trueVariants.getFile());
			
			buildSimMap(simVariants.getFile());
			
			int simTotalCount = simVariantMap.size();
			int trueTotalCount = trueVariantMap.size();
			
			List<Integer> simList = new ArrayList<Integer>(1000);
			for(Integer site : simVariantMap.keySet()) {
				simList.add(site);
			}
			
			List<Integer> trueList = new ArrayList<Integer>(1000);
			for(Integer site : trueVariantMap.keySet()) {
				trueList.add(site);
			}
			
			Collections.sort(simList);
			Collections.sort(trueList);
			
//			for(int i=0; i<Math.max(simList.size(), trueList.size()); i++) {
//				if (i < trueList.size())
//					System.out.print(trueList.get(i));
//				if (i < simList.size())
//					System.out.print("\t" + simList.get(i));
//				System.out.println();
//			}
			
			report.write("# Simulation validation report : \n");
			report.write("true.variants.file=" + trueVariants.getFile().getAbsolutePath() + "\n");
			report.write("sim.variants.file=" + simVariants.getFile().getAbsolutePath() + "\n");
			report.write("true.variants.total=" + trueTotalCount + "\n");
			report.write("sim.variants.total=" + simTotalCount + "\n");
			
		
			//Count number of variants found that are actually true variants
			int simsInTrue = 0;
			List<Integer> falsePositivesList = new ArrayList<Integer>();
			for(Integer simPos : simVariantMap.keySet()) {
				boolean truth = trueVariantMap.get(simPos) != null;
				if (truth)
					simsInTrue++; //True positives
				else {
					//This was a snp found in the simulated set, but NOT in the true set, so it's a false positive
					falsePositivesList.add(simPos);
				}
			}
			

			NumberFormat formatter = new DecimalFormat("##0.000");
			
			report.write("true.variants.found=" + simsInTrue + "\n");
			
			int trueVariantsMissed = trueTotalCount - simsInTrue;
			double falseNegPercentage = trueVariantsMissed / (double)trueTotalCount;
			
			report.write("false.negatives=" + trueVariantsMissed + " (" + formatter.format(falseNegPercentage) + ") \n");
			
			int falsePositives = falsePositivesList.size();
			double falsePosPercentage = falsePositives / (double)simTotalCount;
			report.write("false.positives=" + falsePositives + " (" + formatter.format(falsePosPercentage) + ") \n");
			
			String fpSummary = inspectSim(simVariants.getFile(), falsePositivesList);
			report.write(fpSummary + "\n");
			
			
			report.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
}