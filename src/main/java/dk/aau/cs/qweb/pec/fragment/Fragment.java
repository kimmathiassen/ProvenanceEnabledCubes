package dk.aau.cs.qweb.pec.fragment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import dk.aau.cs.qweb.pec.types.Quadruple;
import dk.aau.cs.qweb.pec.types.Signature;

public class Fragment {

	// Fragment definition
	private Set<Signature> signatures;
	
	private long size;
	
	private boolean containsMetadata;
	
	private boolean containsInfoTriples;
	
	protected int id;
	
	private boolean redundant;
	
	private boolean containsMeasureTriples;
	
	public static final Signature allSignature = new Signature(null, null, null, null);
	
	public Fragment(int id) {
		signatures = new LinkedHashSet<>();
		signatures.add(allSignature);
		size = 0;
		containsMetadata = true;
		containsInfoTriples = true;
		this.id = id;
		redundant = false;
		containsMeasureTriples = false;
	}
	
	public Fragment(Signature signature, int id) {
		signatures = new LinkedHashSet<>();
		signatures.add(signature);
		size = 0;
		containsMetadata = true;
		containsInfoTriples = true;
		this.id = id;
		redundant = false;
		containsMeasureTriples = false;
	}
	
	public Fragment(Collection<Signature> signature, int id) {
		this.signatures = new LinkedHashSet<>();
		this.signatures.addAll(signatures);
		size = 0;
		containsMetadata = true;
		containsInfoTriples = true;
		this.id = id;
		redundant = false;
		containsMeasureTriples = false;
	}
	
	public Fragment(String provenanceId, int id) {
		signatures = new LinkedHashSet<>();
		signatures.add(new Signature(null, null, null, provenanceId));
		size = 0;
		containsMetadata = true;
		containsInfoTriples = true;
		this.id = id;
		redundant = false;
		containsMeasureTriples = false;
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
	 * Does this fragment contain measure triples.
	 * @return
	 */
	public boolean containsMeasureTriples() {
		return containsMeasureTriples;
	}
	
	public void setContainsMeasureTriples(boolean containsMeasureTriples) {
		this.containsMeasureTriples = containsMeasureTriples;
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
		newFragment.markAsRedundant(redundant && f2.isRedundant());
		newFragment.setContainsMeasureTriples(containsMeasureTriples || f2.containsMeasureTriples);
		newFragment.size = size + f2.size;
		newFragment.signatures.addAll(signatures);
		newFragment.signatures.addAll(f2.signatures);
		return newFragment;
	}

	
	
	public boolean hasSignature(Quadruple signature) {
		return signatures.contains(signature);
	}
	
	public Signature getSomeSignature() {
		return signatures.iterator().next();
	}
	
	public boolean canSignatureJoinSubject2Subject(Fragment otherFragment) {
		Set<String> domainsThis = new LinkedHashSet<>();
		Set<String> domainsOther = new LinkedHashSet<>();
		for (Signature signature : signatures) {
			if (signature.getRange() == null) {
				// This means this fragment can contain any type of triple
				return true;
			} else {
				domainsThis.add(signature.getRange());
			}
		}
		
		for (Signature signature : otherFragment.signatures) {
			if (signature.getRange() == null) {
				// This means this fragment can contain any type of triple
				return true;
			} else {
				domainsOther.add(signature.getRange());
			}
		}

		domainsOther.retainAll(domainsThis);
		
		return !domainsOther.isEmpty();
	}
	
	public void markAsRedundant(boolean isRedundant) {
		redundant = isRedundant;
	}
	
	public boolean isRedundant() {
		return redundant;
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
	
	public Collection<Signature> getSignatures() {
		return new ArrayList<Signature>(signatures);
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
		String redundantStr = redundant ? "R" : "";
		if (signatures.contains(allSignature))
			return "[" + id + " Root " +  size + "q]";
		else
			return "[" + metadata + info + redundantStr + " id: " + id + "  Signatures:" + signatures + ", " + size + "q]"; 
	}

	public String getShortName() {
		StringBuilder strBuilder = new StringBuilder();
		Signature sig = getSomeSignature();
		if (signatures.contains(allSignature)) {
			strBuilder.append("all");
		} else {
			if (sig.getPredicate() != null) {
				strBuilder.append("_");
				strBuilder.append(sig.getPredicate());
			}
			
			if (sig.getGraphLabel() != null) {
				strBuilder.append(getSomeSignature().getGraphLabel());
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
		for (Signature signature : signatures) {
			if (signature.getPredicate() != null && signature.getPredicate().equals(relation))
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

	public boolean containsSignature(Signature triplePatternSignature) {
		for (Signature signature : signatures) {
			if (triplePatternSignature.equals(signature)) {
				return true;
			}
		}
		return false;
	}

	public Set<String> getProvenanceIdentifers() {
		Set<String> provenanceIdentifiers = new HashSet<String>();
		for (Signature signature : signatures) {
			provenanceIdentifiers.add(signature.getGraphLabel());
		}
		return provenanceIdentifiers;
	}
}
