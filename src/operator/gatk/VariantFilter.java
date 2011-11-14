package operator.gatk;

import buffer.BAMFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;
import buffer.VCFFile;
import operator.CommandOperator;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

public class VariantFilter extends CommandOperator {

	public static final String FILTER = "filter";
	public final String defaultMemOptions = " -Xms2048m -Xmx8g";
	public static final String PATH = "path";
	public static final String JVM_ARGS="jvmargs";
	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	
	@Override
	protected String getCommand() {
		Object propsPath = Pipeline.getPropertyStatic(PipelineXMLConstants.GATK_PATH);
		if (propsPath != null)
			gatkPath = propsPath.toString();
		
		String path = properties.get(PATH);
		if (path != null) {
			gatkPath = path;
		}
		
		String filterStr = properties.get(FILTER);
		if (filterStr == null)
			throw new IllegalArgumentException("Must provide 'filter' attribute to filter for variants");
		
		//Additional args for jvm
		String jvmARGStr = properties.get(JVM_ARGS);
		if (jvmARGStr == null)
			jvmARGStr = "";
		
		String reference = getInputBufferForClass(ReferenceFile.class).getAbsolutePath();
		String inputFile = getInputBufferForClass(VCFFile.class).getAbsolutePath();
		String outputVCF = outputBuffers.get(0).getAbsolutePath();
				
		String command = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath;
		command = command + " -R " + reference;
		command = command + " --variant " + inputFile + " -T VariantFiltration";
		command = command + " --filterExpression \"" + filterStr + "\" ";
		command = command + " --out " + outputVCF;
		return command;
	}

}
