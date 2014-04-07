package buffer.variant;


/**
 * A tiny version of variant rec that does not store any properties or annotations. 
 * @author brendan
 *
 */
public class VariantStub extends VariantRec {

	
	public VariantStub(String contig, int start, int end, String ref, String alt, boolean isHet) {
		super(contig, start, end, ref, alt);
		props = null;
		annotations = null;
		super.isHetero = isHet;
	}
	
	public VariantRec toFullRecord() {
		return new VariantRec(contig, start, end, ref, alt, 100.0, super.isHetero);
	}
	
	public synchronized void addProperty(String key, Double val) {
		throw new IllegalStateException("VariantStubs cant have properties or annotations");
	}
	
	public synchronized void addAnnotation(String key, String anno) {
		throw new IllegalStateException("VariantStubs cant have properties or annotations");
	}

	public Double getProperty(String key) {
		return null;
	}
	
	public String getAnnotation(String key) {
		return null;
	}
}
