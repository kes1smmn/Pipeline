package operator.annovar;

import java.io.File;
import java.io.IOException;

import operator.OperationFailedException;
import buffer.variant.FileAnnotator;
import buffer.variant.VariantRec;

public class DBSNPAnnotator extends AnnovarAnnotator {

	@Override
	public void performOperation() throws OperationFailedException {
		if (variants == null)
			throw new OperationFailedException("Variant pool not initialized", this);
		
		if (annovarInputFile == null) {
			throw new OperationFailedException("AnnovarInputFile object not specified", this);
		}
		
		if (annovarInputFile.getFile() == null) {
			throw new OperationFailedException("AnnovarInputFile file not specified", this);
		}
		
		if (!annovarInputFile.getFile().exists()) {
			throw new OperationFailedException("AnnovarInputFile file " + annovarInputFile.getFile().getAbsolutePath() + " does not exist", this);
		}
		
		String command = "perl " + annovarPath + "annotate_variation.pl -filter -dbtype snp132 --buildver " + buildVer + " " + annovarInputFile.getAbsolutePath() + " --outfile " + annovarPrefix + " " + annovarPath + "humandb/";

		executeCommand(command);
		
		File resultsFile = new File(annovarPrefix + ".hg19_snp132_dropped");
		FileAnnotator annotator = new FileAnnotator(resultsFile, VariantRec.RSNUM, 1, 5, 6, variants);
		try {
			annotator.annotateAll(false);
		} catch (IOException e) {
			throw new OperationFailedException("Error occurred during dbSNP annotation: " + e.getMessage(), this);
		}
	}

}
