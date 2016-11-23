package dk.aau.cs.qweb.pec.fragment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import dk.aau.cs.qweb.pec.types.Quadruple;
import dk.aau.cs.qweb.pec.types.Signature;


public class Fragment {

	// Fragment definition
	private Set<Signature<String, String, String, String>> signatures;
	
	private long size;
	
	private boolean containsMetadata;
	
	private boolean containsInfoTriples;
	
	protected int id;
	
	public static final Signature<String, String, String, String> allSignature = new Signature<>(null, null, null, null);
	
	public Fragment(int id) {
		signatures = new LinkedHashSet<>();
		signatures.add(allSignature);
		size = 0;
		containsMetadata = true;
		containsInfoTriples = true;
		this.id = id;
	}
	
	public Fragment(Signature<String, String, String, String> signature, int id) {
		signatures = new LinkedHashSet<>();
		signatures.add(signature);
		size = 0;
		containsMetadata = true;
		containsInfoTriples = true;
		this.id = id;
	}
	
	public Fragment(Collection<Signature<String, String, String, String>> signature, int id) {
		this.signatures = new LinkedHashSet<>();
		this.signatures.addAll(signatures);
		size = 0;
		containsMetadata = true;
		containsInfoTriples = true;
		this.id = id;
		
	}
	
	public Fragment(String provenanceId, int id) {
		signatures = new LinkedHashSet<>();
		signatures.add(new Signature<String, String, String, String>(null, null, null, provenanceId));
		size = 0;
		containsMetadata = true;
		containsInfoTriples = true;
		this.id = id;
	}

	/**
	 * Does this fragment contain metadata triples.
	 * @return
	 */
	public boolean containsMetadata() {
		return containsMetadata;
	}
	
	/**
	 * Does this fragment contain information triples.
	 * @return
	 */
	public boolean containsInfoTriples() {
		return containsInfoTriples;
	}
	
	
	/**
	 * Creates a new fragment whose signature is the union of the signatures
	 * of the current fragment and the fragment provided as argument. 
	 *  
	 * @param f2
	 * @return
	 */
	public Fragment merge(Fragment f2, int newId) {
		Fragment newFragment = new Fragment(signatures, newId);
		newFragment.setContainsInfoTriples(containsInfoTriples || f2.containsInfoTriples);
		newFragment.setContainsMetadata(containsMetadata || f2.containsMetadata);
		newFragment.size = size + f2.size;
		newFragment.signatures.addAll(signatures);
		newFragment.signatures.addAll(f2.signatures);
		return newFragment;
	}

	
	
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
		String metadata = containsMetadata ? "M" : "";
		String info = containsInfoTriples ? "I" : "";
		if (signatures.contains(allSignature))
			return "[" + metadata + info + " " + id + " All, " +  size + " triples]";
		else
			return "[" + metadata + info + " " + id + "  " + signatures + "  " + size + " triples]"; 
	}

	public String getShortName() {
		StringBuilder strBuilder = new StringBuilder();
		Signature<String, String, String, String> sig = getSomeSignature();
		if (signatures.contains(allSignature)) {
			strBuilder.append("all");
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
	 * Returns true if the lattice contains a fragment whose signature contains
	 * the given relation name. 
	 * @param relation
	 * @return
	 */
	public boolean containsSignatureWithRelation(String relation) {
		for (Signature<String, String, String, String> signature : signatures) {
			if (signature.getSecond() != null && signature.getSecond().equals(relation))
				return true;
		}
		
		return false;
	}

	public void setContainsMetadata(boolean containsMetadata) {
		this.containsMetadata = containsMetadata;
	}
	
	public void setContainsInfoTriples(boolean containsInfoTriples) {
		this.containsInfoTriples = containsInfoTriples;
	}
	
	/**
	 * It computes the sum of the sizes of all the fragments in the iterable object
	 * @param fragments
	 * @return
	 */
	public static long aggregateSize(Iterable<Fragment> fragments) {
		long sum = 0l;
		for (Fragment f : fragments) {
			sum += f.size();
		}
		return sum;
	}

}
