package dk.aau.cs.qweb.pec.queryEvaluation;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Model;

import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.lattice.Lattice;
import dk.aau.cs.qweb.pec.types.Signature;

public abstract class MaterializedFragments {
	protected Set<Fragment> fragments = new LinkedHashSet<Fragment>();
	protected Map<String, Fragment> urlToFragment = new HashMap<>();
	protected String datasetPath;
	private Lattice sourceLattice;
	
	// Add indexes
	protected MultiValuedMap<String, Fragment> provenanceId2FragmentMap;
	protected MultiValuedMap<Pair<String, String>, Fragment> predicatesAndProvid2FragmentsMap;
	
	
	protected MaterializedFragments(Set<Fragment> fragments, String datasetPath, Lattice sourceLattice) {
		this.fragments.addAll(fragments);
		this.datasetPath = datasetPath;
		this.sourceLattice = sourceLattice;
		this.provenanceId2FragmentMap = new ArrayListValuedHashMap<>();
		this.predicatesAndProvid2FragmentsMap = new ArrayListValuedHashMap<>();
		indexFragments();
	}
	
	/**
	 * Create indexes for the set of materialized fragments.
	 */
	private void indexFragments() {
		for (Fragment fragment : fragments) {
			indexFragment(fragment);
		}
	}
	
	private void indexFragment(Fragment fragment) {
		urlToFragment.put(getFragmentURL(fragment), fragment);
		for (Signature signature : fragment.getSignatures()) {
			provenanceId2FragmentMap.put(signature.getGraphLabel(), fragment);
			if (signature.getProperty() != null) {
				predicatesAndProvid2FragmentsMap.put(new MutablePair<>(signature.getProperty(), 
						signature.getGraphLabel()), fragment);
			}
		}	
	}
	
	public Set<Fragment> getRelevantFragments(Set<String> provenanceIdentifiers) {
		Set<Fragment> relevantFragments = new LinkedHashSet<>();
		for (String provenanceIdentifier : provenanceIdentifiers) {
			relevantFragments.addAll(provenanceId2FragmentMap.get(provenanceIdentifier));
		}
		return relevantFragments;
	}


	public boolean contains(Fragment fragment) {
		return fragments.contains(fragment);
	}

	public abstract String getFragmentURL(Fragment fragment) ;

	public abstract Map<String,Set<Model>> getMaterializedFragments() ;

	public abstract Model getMaterializedModel(String graph) ;
	
	public Lattice getSourceLattice() {
		return sourceLattice;
	}
	
	@Override
	public String toString() {
		return datasetPath + " "+ fragments.toString();
	}
	
	public int size() {
		return fragments.size();
	}

	public Set<Fragment> getFragments() {
		return fragments;
	}

	public boolean containsAny(Set<Fragment> otherFragments) {
		for (Fragment f : otherFragments) {
			if (fragments.contains(f)) {
				return true;
			}
		}
		
		return false;
	}

	public PriorityQueue<Fragment> getSortedIntersection(Set<Fragment> ancestors) {
		PriorityQueue<Fragment> queue = new PriorityQueue<>(ancestors.size(), new Comparator<Fragment>() {

			@Override
			public int compare(Fragment o1, Fragment o2) {
				int cmp = Long.compare(o2.size(), o1.size());
				if (cmp == 0) {
					return Integer.compare(o1.getId(), o2.getId());
				} else {
					return cmp;
				}
			}
		});
		
		for (Fragment ancestor : ancestors) {
			if (fragments.contains(ancestor)) {
				queue.add(ancestor);
			}
		}
		
		return queue;
	}
	
	public void add(Fragment candidate) {
		if (!fragments.contains(candidate)) {
			indexFragment(candidate);
			fragments.add(candidate);
		}
	}

	public void addAll(Set<Fragment> fragments) {
		for (Fragment fragment : fragments) {
			add(fragment);
		}
	}

	/**
	 * Removes a fragment from the set of materialized fragments.
	 * @param fragment
	 */
	public void remove(Fragment fragment) {
		fragments.remove(fragment);
		unidexFragment(fragment);
	}

	/**
	 * Remove a fragment from the indexes.
	 * @param fragment
	 */
	private void unidexFragment(Fragment fragment) {
		urlToFragment.remove(getFragmentURL(fragment));
		for (Signature signature : fragment.getSignatures()) {
			provenanceId2FragmentMap.removeMapping(signature.getGraphLabel(), fragment);
			if (signature.getProperty() != null) {
				predicatesAndProvid2FragmentsMap.removeMapping(new MutablePair<>(signature.getProperty(), 
						signature.getGraphLabel()), fragment);
			}
		}	
	}

	public Fragment getFragmentByUrl(String fromClause) {
		return urlToFragment.get(fromClause);
	}
}
