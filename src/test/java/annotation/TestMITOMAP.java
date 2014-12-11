package annotation;

import java.io.File;

import operator.variant.MITOMAP_rRNAtRNA;
import operator.variant.MITOMAPcoding;
import junit.framework.Assert;
import junit.framework.TestCase;
import pipeline.Pipeline;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;




public class TestMITOMAP extends TestCase {
	
	/**
	 * This TEST CASE currently checks accuracy of BOTH the MITOMAP_rRNAtRNA and MITOMAPcoding annotators
	 * 
	 * author chrisk
	 */
	
	
	File inputFile = new File("src/test/java/annotation/testMITOMAP.xml");
	File inputFile2 = new File("src/test/java/annotation/testMITOMAPtRNArRNA.xml");
	File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml");

	
	public void testMITOMAP() {
		
		try {
			Pipeline ppl = new Pipeline(inputFile, propertiesFile.getAbsolutePath());

			ppl.setProperty("MITOMAP.coding.path", "src/test/java/testcsvs/MitoMap_coding.csv.gz");

			ppl.initializePipeline();
			ppl.stopAllLogging();
			ppl.execute();

			MITOMAPcoding annotator = (MITOMAPcoding)ppl.getObjectHandler().getObjectForLabel("GeneAnnotate");
			
			VariantPool vars = annotator.getVariants();	
			
			VariantRec var = vars.findRecord("M",150,"C","T");
			Assert.assertTrue(var != null);
			Assert.assertTrue(var.getAnnotation(VariantRec.MITOMAP_DIS_CODING).equals("Longevity / Cervical Carcinoma / HPV infection risk"));
			
			VariantRec var1 = vars.findRecord("M",3733,"G","A");
			Assert.assertTrue(var1 != null);
			Assert.assertTrue(var1.getAnnotation(VariantRec.MITOMAP_DIS_CODING).equals("LHON"));
			
			VariantRec var2 = vars.findRecord("M",12425,"A","-");
			Assert.assertTrue(var2 != null);
			Assert.assertTrue(var2.getAnnotation(VariantRec.MITOMAP_DIS_CODING).equals("Mitochondrial Myopathy & Renal Failure"));		
			
			VariantRec var3 = vars.findRecord("M",3733,"G","C");
			Assert.assertTrue(var3 != null);
			Assert.assertTrue(var3.getAnnotation(VariantRec.MITOMAP_DIS_CODING).equals("LHON"));
			
			VariantRec var4 = vars.findRecord("M",9479,"TTTTTCTTCGCAGGA","-");
			Assert.assertTrue(var4 != null);
			Assert.assertTrue(var4.getAnnotation(VariantRec.MITOMAP_DIS_CODING).equals("Myoglobinuria"));
			
			VariantRec var5 = vars.findRecord("M",15498,"GGCGACCCAGACAATTATACCCTA","-");
			Assert.assertTrue(var5 != null);
			Assert.assertTrue(var5.getAnnotation(VariantRec.MITOMAP_DIS_CODING).equals("EXIT"));	
			
			VariantRec var6 = vars.findRecord("M",15498, "G","A");
			Assert.assertTrue(var6 != null);
			Assert.assertTrue(var6.getAnnotation(VariantRec.MITOMAP_DIS_CODING).equals("HiCM / WPW, DEAF"));	
			

						
			
		} catch (Exception ex) {
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
		
		try {
			
			Pipeline ppl = new Pipeline(inputFile2, propertiesFile.getAbsolutePath());

			ppl.setProperty("MITOMAP.rRNAtRNA.path", "src/test/java/testcsvs/MM_rRNAtRNA.csv.gz");

			ppl.initializePipeline();
			ppl.stopAllLogging();
			ppl.execute();

			MITOMAP_rRNAtRNA annotator2 = (MITOMAP_rRNAtRNA)ppl.getObjectHandler().getObjectForLabel("GeneAnnotate");
			
			VariantPool vars = annotator2.getVariants();	
			
			VariantRec var7 = vars.findRecord("M",622,"G","A");
			Assert.assertTrue(var7 != null);
			Assert.assertTrue(var7.getAnnotation(VariantRec.MITOMAP_DIS_tRNArRNA).equals("EXIT & Deafness"));
			
			VariantRec var8 = vars.findRecord("M",960,"-","C");
			Assert.assertTrue(var8 != null);
			Assert.assertTrue(var8.getAnnotation(VariantRec.MITOMAP_DIS_tRNArRNA).equals("Possibly DEAF-associated"));
			
			VariantRec var10 = vars.findRecord("M",4285,"T","C");
			Assert.assertTrue(var10 != null);
			Assert.assertTrue(var10.getAnnotation(VariantRec.MITOMAP_DIS_tRNArRNA).equals("CPEO"));
			
			VariantRec var11 = vars.findRecord("M",4401,"A","G");
			Assert.assertTrue(var11 != null);
			Assert.assertTrue(var11.getAnnotation(VariantRec.MITOMAP_DIS_tRNArRNA).equals("Hypertension+Ventricular Hypertrophy"));
			
			VariantRec var12 = vars.findRecord("M",7445,"A","C");
			Assert.assertTrue(var12 != null);
			Assert.assertTrue(var12.getAnnotation(VariantRec.MITOMAP_DIS_tRNArRNA).equals("DEAF"));
						
			
		} catch (Exception ex) {
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
		
		
		
	}
}