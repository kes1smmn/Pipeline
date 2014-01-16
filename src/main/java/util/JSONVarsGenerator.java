package util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import json.AnnotatedVarsJsonConverter;
import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import util.reviewDir.DefaultManifestFactory;
import util.reviewDir.ManifestParseException;
import util.reviewDir.SampleManifest;
import util.reviewDir.WritableManifest;
import buffer.CSVFile;
import buffer.variant.CSVLineReader;
import buffer.variant.VariantLineReader;
import buffer.variant.VariantRec;

/**
 * Handles creation of a new 'annotated.json.gz' file from an input annotated.csv file
 * @author brendan
 *
 */
public class JSONVarsGenerator {

	public static String createJSONVariants(CSVFile inputVars, File destDir) throws JSONException, IOException {
		JSONObject jsonResponse = new JSONObject();
		String destFilename = inputVars.getFilename().replace(".csv", ".json.gz");
		File dest = new File(destDir.getAbsolutePath() + "/" + destFilename);
		
		VariantLineReader varReader = new CSVLineReader(inputVars.getFile());
		AnnotatedVarsJsonConverter converter = new AnnotatedVarsJsonConverter();

		JSONArray jsonVarList = new JSONArray();
		
		//Trim, etc
		List<String> map = new ArrayList<String>();
		String[] headerToks =  varReader.getHeader().trim().replace("#",  "").split("\t");
		for(int i=0; i<headerToks.length; i++) {
			map.add(headerToks[i].trim().replace(" ", ""));
		}
		converter.setKeys(map);
		
		//Danger: could create huge json object if variant list is big
		VariantRec var = varReader.toVariantRec();
		while(var != null) {
			jsonVarList.put( converter.toJSON(var) );
			varReader.advanceLine();
			var = varReader.toVariantRec();
		}
		
		jsonResponse.put("variant.list", jsonVarList);

		//Get the json string, then compress it to a byte array
		String str = jsonResponse.toString();			
		byte[] bytes = compressGZIP(str);

		if (dest.exists()) {
			throw new IOException("Destination file already exists");
		}

		BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(dest));
		writer.write(bytes);
		writer.close();
		return destFilename;
	}
	
	/** 
	 * GZIP compress the given string to a byte array
	 * @param str
	 * @return
	 */
	public static byte[] compressGZIP(String str){
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try{
			GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
			gzipOutputStream.write(str.getBytes("UTF-8"));
			gzipOutputStream.close();
		} catch(IOException e){
			throw new RuntimeException(e);
		}
		return byteArrayOutputStream.toByteArray();
	}
	
	
	public static void main(String[] args) {
		
		DefaultManifestFactory manifestReader = new DefaultManifestFactory();
		
		for(int i=0; i<args.length; i++) {
			File resultsDir = new File(args[i]);
			if (! resultsDir.exists()) {
				System.err.println("Results directory " + resultsDir.getAbsolutePath() + " does not exist, skipping it.");
				continue;
			}
			try {
				SampleManifest manifest = manifestReader.readManifest(resultsDir.getAbsolutePath());
				
				String jsonVars = manifest.getProperty("json.vars");
				if (jsonVars != null) {
					System.err.println(resultsDir.getAbsolutePath() + " already has a json.vars file, not replacing it.");
					continue;
				}
				
				String annotatedCSV = manifest.getProperty("annotated.vars");
				File annotatedVarsFile = new File(resultsDir.getAbsolutePath() + "/" + annotatedCSV);
				if (! annotatedVarsFile.exists()) {
					System.err.println("Annotated variant file " + annotatedCSV + " is specified in manifest, but does not exist!");
					continue;
				}
				
				File dest = new File(resultsDir.getAbsolutePath() + "/var/");
				String destFilename = JSONVarsGenerator.createJSONVariants(new CSVFile(annotatedVarsFile), dest);
				
				WritableManifest writable = new WritableManifest(manifest);
				writable.put("json.vars", "var/" + destFilename);
				writable.save();
				
			} catch (ManifestParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
		
	}
}
