package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;

import pipeline.Pipeline;

import buffer.VCFFile;
import buffer.variant.VariantRec;

/**
 * This class provides a uniform interface for extracting values from a single line of a vcf file.
 * It uses a differed-implementation strategy so we dont have to store too much info 
 * @author brendan
 *
 */
public class VCFLineParser {

		private File file;
		private BufferedReader reader;
		private int currentLineNumber = -1;
		private String currentLine = null;
		private String[] lineToks = null;
		private String[] formatToks = null; //Tokenized format string, produced as needed
		private int gtCol = -1; //Format column which contains genotype info
				
		private String sample = null; //Emit information for only this sample if specified (when not given, defaults to first sample)
		private int sampleColumn = -1; //Column that stores information for the given sample
		
		public VCFLineParser(File file, String sample) throws IOException {
			this.file = file;
			this.reader = new BufferedReader(new FileReader(file));
			currentLine = reader.readLine();
			this.sample = sample; //Sample must be specified before header is read
			readHeader();

		}
		
		public VCFLineParser(File file) throws IOException {
			this.file = file;
			this.reader = new BufferedReader(new FileReader(file));
			currentLine = reader.readLine();
			sampleColumn = 9; //First column with info, this is the default when no sample is specified
			readHeader();
		}

		
		
		public VCFLineParser(VCFFile file) throws IOException {
			this(file.getFile());
		}
		
		private void readHeader() throws IOException {
			while (currentLine != null && currentLine.startsWith("#")) {
				advanceLine();
				
				if (currentLine.startsWith("#CHROM")) {
					String[] toks = currentLine.split("\t");
					if (sample == null) {
						sampleColumn = 9;
						sample = toks[9];
					}
					else {
						for(int col = 0; col<toks.length; col++) {
							if (toks[col].equals(sample)) {
								sampleColumn = col;
							}
						}
					}
					if (sampleColumn < 0) {
						throw new IllegalArgumentException("Cannot find column for sample " + sample);
					}
				}
				
				
			}
		}
		
		public String getSampleName() {
			return sample;
		}
		
		/**
		 * Advance the current line until the contig found is the given contig. If
		 * already at the given contig, do nothing
		 * @param contig
		 * @throws IOException 
		 */
		public void advanceToContig(String contig) throws IOException {
			while (hasLine() && (!getContig().equals(contig))) {
				advanceLine();
			}
			if (! hasLine()) {
				throw new IllegalArgumentException("Could not find contig " + contig + " in vcf");
			}
		}
		
		/**
		 * Advance the current line until we reach a contig whose name matches the contig arg,
		 * and we find a variant whose position is equal to or greater than the given position
		 * @throws IOException 
		 */
		public void advanceTo(String contig, int pos) throws IOException {
			advanceToContig(contig);
			while(hasLine() && getPosition() < pos) {
				advanceLine();
				if (! hasLine()) {
					throw new IllegalArgumentException("Advanced beyond end file looking for pos: " + pos);
				}
				if (! getContig().equals(contig)) {
					throw new IllegalArgumentException("Could not find position " + pos + " in contig " + contig);
				}
			}
		}
		
		public boolean isPassing() {
			return currentLine.contains("PASS");
		}
		
		
		/**
		 * Converts the information in the current line to a VariantRec, by default this
		 * will strip 'chr' from all contig names
		 * @return
		 */
		public VariantRec toVariantRec() {
			return toVariantRec(true);
		}
		
		/**
		 * Convert current line into a variant record
		 * @param stripChr If true, strip 'chr' from contig name, if false do not alter contig name
		 * @return A new variant record containing the information in this vcf line
		 */
		public VariantRec toVariantRec(boolean stripChr) {
			if (currentLine == null)
				return null;
			else {
				String contig = getContig();
				if (stripChr)
					contig = contig.replace("chr", "");
				//System.out.println(currentLine);
				VariantRec rec = new VariantRec(contig, getStart(), getStart()+1,  getRef(), getAlt(), getQuality(), false, isHetero() );
				Integer depth = getDepth();
				if (depth != null)
					rec.addProperty(VariantRec.DEPTH, new Double(getDepth()));
				return rec;
			}
		}
		
		/**
		 * Read one more line of input, returns false if line cannot be read
		 * @return
		 * @throws IOException
		 */
		public boolean advanceLine() throws IOException {
			currentLine = reader.readLine();
			if (currentLine == null)
				lineToks = null;
			else
				lineToks = currentLine.split("\\t");

			currentLineNumber++;
			return currentLine != null;
		}

		/**
		 * Returns true if the current line is not null. 
		 * @return
		 */
		public boolean hasLine() {
			return currentLine != null;
		}
		
		public String getContig() {
			if (lineToks != null) {
				return lineToks[0];
			}
			else
				return null;
		}
		
		/**
		 * Return the (starting) position item for current line
		 * @return
		 */
		public int getPosition() {
			if (lineToks != null) {
				return Integer.parseInt(lineToks[1]);
			}
			else
				return -1;
		}
		
		public Integer getDepth() {
			String info = lineToks[7];
			
			String target = "DP";
			int index = info.indexOf(target);
			if (index < 0) {
				return null;
			}
			
			//System.out.println( info.substring(index, index+10) + " ...... " + info.substring(index+target.length()+1, info.indexOf(';', index)));
			try {
				Integer value = Integer.parseInt(info.substring(index+target.length()+1, info.indexOf(';', index)));
				return value;
			}
			catch (NumberFormatException nfe) {
				Logger.getLogger(Pipeline.primaryLoggerName).warning("Could not parse depth from vcf line: " );
			}
			return null;
		}
		
		public int getStart() {
			return getPosition();
		}
		
		/**
		 * Return the end of this variant
		 * @return
		 */
		public int getEnd() {
			if (lineToks != null) {
				return Integer.parseInt(lineToks[2]);
			}
			else
				return -1;
		}
		
		public Double getQuality() {
			if (lineToks != null) {
				return Double.parseDouble(lineToks[5]);
			}
			else
				return -1.0;
		}
		
		public String getRef() {
			if (lineToks != null) {
				return lineToks[3];
			}
			else
				return "?";
		}
		
		public String getAlt() {
			if (lineToks != null) {
				return lineToks[4];
			}
			else
				return "?";
		}
		
		public int getLineNumber() {
			return currentLineNumber;
		}
		
		/**
		 * 
		 */
		public boolean isHetero() {
			if (lineToks != null) {
				String[] fields = lineToks[9].split(":");
				//Right now we assume genotype is in FIRST format-field element, this may not always be true 
				if (fields[0].length() != 3) {
					throw new IllegalStateException("Wrong number of characters in string for is hetero... (got " + fields[0].length() + ", but should be 3)");
				}
				
				if (fields[0].charAt(1) == '/' || fields[0].charAt(1) == '|') {
					if (fields[0].charAt(0) != fields[0].charAt(2))
						 return true;
					else
						return false;
						
				}
				else {
					throw new IllegalStateException("Genotype separator char does not seem to be normal (found " + fields[0].charAt(1) + ")");
				}
				
			}
			else
				return false;
		}
		
		public boolean isHomo() {
			return ! isHetero();
		}

		public String getCurrentLine() {
			return currentLine;
		}
		
		/**
		 * Returns true if the phasing separator is "|" and not "/" 
		 * @return
		 */
		public boolean isPhased() {
			if (formatToks == null) {
				createFormatString();
			}
			
			String[] formatValues = lineToks[sampleColumn].split("\t");
			String GTStr = formatValues[gtCol];
			if (GTStr.charAt(1) == '|') {
				return true;
			}
			else {
				return false;
			}
			
		}
		
		/**
		 * True if the first item in the genotype string indicates an 'alt' allele
		 * @return
		 */
		public boolean firstIsAlt() {
			if (formatToks == null) {
				createFormatString();
			}
	
			String[] formatValues = lineToks[sampleColumn].split("\t");
			String GTStr = formatValues[gtCol];
			if (GTStr.charAt(0) == '1') {
				return true;
			}
			else {
				return false;
			}
		}
		
		/**
		 * True if the second item in the genotype string indicates an 'alt' allele
		 * @return
		 */
		public boolean secondIsAlt() {
			if (formatToks == null) {
				createFormatString();
			}
			
			String[] formatValues = lineToks[sampleColumn].split("\t");
			String GTStr = formatValues[gtCol];
			if (GTStr.charAt(2) == '1') {
				return true;
			}
			else {
				return false;
			}
		}
		
		private void createFormatString() {
			String formatStr = lineToks[8];
			formatToks = formatStr.split(":");
			for(int i=0; i<formatToks.length; i++) {
				if (formatToks[i].equals("GT")) {
					gtCol = i;
					break;
				}
			}

		}
		
//		public static void main(String[] args) {
//			File file = new File("/media/DATA/exome_compare/ex1.cap.pass.vcf");
//			try {
//				VCFLineParser vParser = new VCFLineParser(file);
//				for(int i=0; i<10;i++) {
//					System.out.println(vParser.getLineNumber() + " : " + vParser.getContig() + "\t" + vParser.getPosition() + "\t" + vParser.getQuality() + "\t het: " + vParser.isHetero());
//					vParser.advanceLine();
//				}
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			
//		}
}
