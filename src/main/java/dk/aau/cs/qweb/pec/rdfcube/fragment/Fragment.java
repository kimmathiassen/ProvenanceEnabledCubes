package dk.aau.cs.qweb.pec.rdfcube.fragment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import dk.aau.cs.qweb.pec.types.Quadruple;
import dk.aau.cs.qweb.pec.types.Signature;


public abstract class Fragment {

	// Fragment definition
	private Set<Signature<String, String, String, String>> signatures;
	
	private long size;
	
	private boolean root;
	
	protected Fragment() {
		signatures = new LinkedHashSet<>();
		signatures.add(new Signature<String, String, String, String>(null, null, null, null));
		root = true;
		size = 0;
	}
	
	protected Fragment(Signature<String, String, String, String> signature) {
		signatures = new LinkedHashSet<>();
		signatures.add(signature);
		root = false;
		size = 0;
	}
	
	protected Fragment(String provenanceId) {
		signatures = new LinkedHashSet<>();
		signatures.add(new Signature<String, String, String, String>(null, null, null, provenanceId));
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
	
	public Signature<String, String, String, String> getFirstSignature() {
		return signatures.iterator().next();
	}
	
	public long size() {
		return size;
	}
	
	public void increaseSize() {
		++size;
	}
	
	public Collection<Signature<String, String, String, String>> getSignatures() {
		// TODO Auto-generated method stub
		return new ArrayList<Signature<String, String, String, String>>(signatures);
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
		Fragment other = (Fragment) obj;
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
