package dk.aau.cs.qweb.pec.rdfcube;

import java.util.LinkedHashSet;
import java.util.Set;

import dk.aau.cs.qweb.pec.types.Quadruple;


public abstract class RDFCubeFragment {

	// Fragment definition
	private Set<Quadruple<String, String, String, String>> signatures;
	
	private long size;
	
	private boolean root;
	
	protected RDFCubeFragment() {
		signatures = new LinkedHashSet<>();
		signatures.add(new Quadruple<String, String, String, String>(null, null, null, null));
		root = true;
		size = 0;
	}
	
	protected RDFCubeFragment(Quadruple<String, String, String, String> signature) {
		signatures = new LinkedHashSet<>();
		signatures.add(signature);
		root = false;
		size = 0;
	}
	
	protected RDFCubeFragment(String provenanceId) {
		signatures = new LinkedHashSet<>();
		signatures.add(new Quadruple<String, String, String, String>(null, null, null, provenanceId));
		root = false;
		size = 0;		
	}
	
	public boolean isRoot() {
		return root;
	}
	
	public abstract boolean isMetadata();
	
	
	public boolean hasSignature(Quadruple<String, String, String, String> signature) {
		return signatures.contains(signature);
	}
	
	public Quadruple<String, String, String, String> getFirstSignature() {
		return signatures.iterator().next();
	}
	
	public long size() {
		return size;
	}
	
	public void increaseSize() {
		++size;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((signatures == null) ? 0 : signatures.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RDFCubeFragment other = (RDFCubeFragment) obj;
		if (signatures == null) {
			if (other.signatures != null)
				return false;
		} else if (!signatures.equals(other.signatures))
			return false;
		return true;
	}

	@Override
	public String toString() {
		if (root)
			return "[All, " +  size + " triples]";
		else
			return "[" + signatures + "  " + size + " triples]"; 
	}

}
