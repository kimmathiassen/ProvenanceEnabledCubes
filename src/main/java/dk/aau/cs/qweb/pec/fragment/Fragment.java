package dk.aau.cs.qweb.pec.fragment;

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
	
	protected int id;
	
	protected Fragment(int id) {
		signatures = new LinkedHashSet<>();
		signatures.add(new Signature<String, String, String, String>(null, null, null, null));
		root = true;
		size = 0;
		this.id = id;
	}
	
	protected Fragment(Signature<String, String, String, String> signature, int id) {
		signatures = new LinkedHashSet<>();
		signatures.add(signature);
		root = false;
		size = 0;
		this.id = id;
	}
	
	protected Fragment(String provenanceId, int id) {
		signatures = new LinkedHashSet<>();
		signatures.add(new Signature<String, String, String, String>(null, null, null, provenanceId));
		root = false;
		size = 0;
		this.id = id;
	}
	
	public boolean isRoot() {
		return root;
	}
	
	public abstract boolean isMetadata();
	
	
	public boolean hasSignature(Quadruple<String, String, String, String> signature) {
		return signatures.contains(signature);
	}
	
	public Signature<String, String, String, String> getSomeSignature() {
		return signatures.iterator().next();
	}
	
	public boolean canSignatureJoinSubject2Subject(Fragment otherFragment) {
		Set<String> domainsThis = new LinkedHashSet<>();
		Set<String> domainsOther = new LinkedHashSet<>();
		for (Signature<String, String, String, String> signature : signatures) {
			if (signature.getFirst() == null) {
				// This means this fragment can contain any type of triple
				return true;
			} else {
				domainsThis.add(signature.getFirst());
			}
		}
		
		for (Signature<String, String, String, String> signature : otherFragment.signatures) {
			if (signature.getFirst() == null) {
				// This means this fragment can contain any type of triple
				return true;
			} else {
				domainsOther.add(signature.getFirst());
			}
		}

		domainsOther.retainAll(domainsThis);
		
		return !domainsOther.isEmpty();
	}
	
	public int getId() {
		return id;
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
			return "[ " + id + " All, " +  size + " triples]";
		else
			return "[" + id + "  " + signatures + "  " + size + " triples]"; 
	}

	public String getShortName() {
		StringBuilder strBuilder = new StringBuilder();
		Signature<String, String, String, String> sig = getSomeSignature();
		if (root) {
			strBuilder.append("root");
		} else {
			if (sig.getSecond() != null) {
				strBuilder.append("_");
				strBuilder.append(sig.getSecond());
			}
			
			if (sig.getFourth() != null) {
				strBuilder.append(getSomeSignature().getFourth());
			}
		}
		
		return strBuilder.toString().replaceAll("[^a-zA-Z0-9]+","_");
	}

	/**
	 * 
	 * @param typerelation
	 * @return
	 */
	public boolean containsSignatureWithRelation(String typerelation) {
		for (Signature<String, String, String, String> signature : signatures) {
			if (signature != null && signature.getSecond().equals(typerelation))
				return true;
		}
		
		return false;
	}
}
