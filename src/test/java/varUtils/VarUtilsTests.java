package varUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.Assert;
import org.junit.Test;

import util.VarUtils;
import buffer.VCFFile;
import buffer.variant.VariantPool;

public class VarUtilsTests {

	@Test
	public void TestSubtraction() {
		
		//These tests are run by emulating a full varUtils command line, so we can
		//make sure to catch errors that result from reading files and writing output
		String[] args;
	
		PrintStream output;
		try {
			
			System.err.println("Testing varUtils subtraction...");
			
			//Subtract a file from itself. Result should be empty. 
			args = new String[]{"subtract", "src/test/java/varUtils/testdata/a.vcf",
			"src/test/java/varUtils/testdata/a.vcf"};
			//Direct system.out to a file so we can read it. 
			File outputFile = new File("varutils.test.out");
			output = new PrintStream(new FileOutputStream(outputFile));
			System.setOut(output);
			VarUtils.main(args);
			
			output.close();
			VariantPool result = new VariantPool(new VCFFile(outputFile));
			
			Assert.assertTrue(result.size()==0);
			
			
			args = new String[]{"subtract", "src/test/java/varUtils/testdata/a.vcf",
			"src/test/java/varUtils/testdata/b.vcf"};
			output = new PrintStream(new FileOutputStream(outputFile));
			System.setOut(output);
			VarUtils.main(args);
			output.close();
			result = new VariantPool(new VCFFile(outputFile));
			Assert.assertTrue(result.size()==2);
			
			System.err.println("Subtraction tests passed.");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail();
		}
		
	}
		
	
	
}
