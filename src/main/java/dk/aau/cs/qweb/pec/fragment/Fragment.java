package dk.aau.cs.qweb.pec.fragment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import dk.aau.cs.qweb.pec.types.Signature;

public class Fragment implements Comparable<Fragment> {

	// Fragment definition
	private Set<Signature> signatures;
	
	private long size;
	
	protected int id;
	
	private boolean redundant;
	
	private long measureTriplesCount;
	
	public static final Signature allSignature = new Signature(null, null, null, null);
	
	public Fragment(int id) {
		signatures = new LinkedHashSet<>();
		signatures.add(allSignature);
		size = 0;
		measureTriplesCount = 0;
		this.id = id;
		redundant = false;
	}
	
	public Fragment(Signature signature, int id) {
		signatures = new LinkedHashSet<>();
		signatures.add(signature);
		size = 0;
		measureTriplesCount = 0;
		this.id = id;
		redundant = false;
	}
	
	public Fragment(Collection<Signature> signature, int id) {
		this.signatures = new LinkedHashSet<>();
		this.signatures.addAll(signatures);
		size = 0;
		measureTriplesCount = 0;
		this.id = id;
		redundant = false;
	}
	
	public Fragment(String provenanceId, int id) {
		signatures = new LinkedHashSet<>();
		signatures.add(new Signature(null, null, null, provenanceId));
		size = 0;
		this.id = id;
		redundant = false;
	}
	
	/**
	 * Returns the number of different provenance identifiers 
	 * associated to this fragment
	 * @return
	 */
	public int getProvenanceSignatureSize() {
		int count = 0;
		for (Signature sig : signatures) {
			if (sig.getGraphLabel() != null)
				++count;
		}
		return count;
	}
	
	public double getPredicatesSignatureSize() {
		int count = 0;
		for (Signature sig : signatures) {
			if (sig.getPredicate() != null)
				++count;
		}
		return count;
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
		newFragment.markAsRedundant(redundant && f2.isRedundant());
		newFragment.setMeasureTriplesCount(measureTriplesCount + f2.measureTriplesCount);
		newFragment.size = size + f2.size;
		newFragment.signatures.addAll(signatures);
		newFragment.signatures.addAll(f2.signatures);
		return newFragment;
	}


	public boolean hasSignature(Signature signature) {
		return signatures.contains(signature);
	}
	
	public Signature getSomeSignature() {
		return signatures.iterator().next();
	}
	
	public boolean canSignatureJoinSubject2Subject(Fragment otherFragment) {
		Set<String> domainsThis = new LinkedHashSet<>();
		Set<String> domainsOther = new LinkedHashSet<>();
		for (Signature signature : signatures) {
			if (signature.getSubject() == null) {
				// This means this fragment can contain any type of triple
				return true;
			} else {
				domainsThis.add(signature.getSubject());
			}
		}
		
		for (Signature signature : otherFragment.signatures) {
			if (signature.getSubject() == null) {
				// This means this fragment can contain any type of triple
				return true;
			} else {
				domainsOther.add(signature.getSubject());
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
	
	public boolean containsMeasureTriples() {
		return measureTriplesCount > 0;
	}
	
	public long getMeasureTriplesCount() {
		return measureTriplesCount;
	}
	
	private void setMeasureTriplesCount(long l) {
		this.measureTriplesCount = l;
		
	}

	public void increaseMeasureTriplesCount() {
		++measureTriplesCount;
	}

	public void increaseSize() {
		++size;
	}
	
	public Set<Signature> getSignatures() {
		return new LinkedHashSet<>(signatures);
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
		String redundantStr = redundant ? "R" : "";
		if (signatures.contains(allSignature))
			return "[" + id + " Root " +  size + "q]";
		else
			return "[" + redundantStr + " id: " + id + "  Signatures:" + signatures + ", " + size + "q" + ", " + measureTriplesCount + "m]"; 
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

	public Set<String> getProvenanceIdentifiers() {
		Set<String> provenanceIdentifiers = new HashSet<String>();
		for (Signature signature : signatures) {
			provenanceIdentifiers.add(signature.getGraphLabel());
		}
		return provenanceIdentifiers;
	}

	public Set<String> getPredicates() {
		Set<String> predicates = new HashSet<String>();
		for (Signature signature : signatures) {
			if (signature.getPredicate() != null) {
				predicates.add(signature.getPredicate());
			}
		}
		return predicates;
	}
	
	public String getPredicatesConcat() {
		Set<String> predicates = new TreeSet<String>();
		for (Signature signature : signatures) {
			if (signature.getPredicate() != null) {
				predicates.add(signature.getPredicate());
			}
		}
		
		StringBuilder concat = new StringBuilder();
		for (String predicate : predicates) {
			concat.append(predicate);
		}
		
		return concat.toString();

	}

	@Override
	public int compareTo(Fragment o) {
		final int BEFORE = -1;
	    final int AFTER = 1;
	    
	    if (getProvenanceSignatureSize() == o.getProvenanceSignatureSize() ) {
	    	// If same number of PI, then the smaller number of predicate the better.
	    	return getPredicates().size() < o.getPredicates().size() ? BEFORE : AFTER;
		} else {
			// largest number of provenance identifiers are in the top of the list
			return getProvenanceSignatureSize() > o.getProvenanceSignatureSize() ? BEFORE : AFTER;
		}
	}

	
	public Collection<Triple<String, String, String>> getTriplesPredicateObjectAndProvenanceId() {
		Collection<Triple<String, String, String>> triples = new ArrayList<>();
		for (Signature sig : signatures) {
			if (sig.getPredicate() != null && sig.getSubject() != null && sig.getGraphLabel() != null)
				triples.add(new MutableTriple<>(sig.getPredicate(), sig.getSubject(), sig.getGraphLabel()));
		}
		
		return triples;
	}

	public Collection<Pair<String, String>> getPairsPredicateProvenanceId() {
		Collection<Pair<String, String>> pairs = new ArrayList<>();
		for (Signature sig : signatures) {
			if (sig.getPredicate() != null && sig.getGraphLabel() != null)
				pairs.add(new MutablePair<>(sig.getPredicate(), sig.getGraphLabel()));
		}
		
		return pairs;
	}

	/**
	 * It returns true if the fragment sent as argument shares at least one provenance identifier
	 * with the current fragment.
	 * @param newFragment
	 * @return
	 */
	public boolean hasCommonProvenanceIds(Fragment newFragment) {
		Set<String> provids = newFragment.getProvenanceIdentifiers();
		for (String pid : getProvenanceIdentifiers()) {
			if (provids.contains(pid)) {
				return true;
			}
		}
	
		return false;
	}

}
