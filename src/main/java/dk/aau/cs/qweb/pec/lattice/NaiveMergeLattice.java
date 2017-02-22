package dk.aau.cs.qweb.pec.lattice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.fragment.Fragment;

/**
 * A naive merge lattice. This lattice supports iterative merging of fragments when 
 * (1) all quads in the data source have been read by the lattice, and (2) the number of fragments is bigger
 * than the maximal fragments count (1000 by default). Merging is not allowed when the number
 * of fragments is smaller than the minimal fragments count (50 by default).
 * This lattice implements two merging strategies: by property/predicate and by provenance identifier. The
 * first strategy, called property merge, merges fragments with signatures 
 * <null, p, null, :A> and <null, p', null, :A> (same provenance identifier). The strategy 'provenance merge'
 * merges fragments such as <null, null, null, :A> and <null, null, null, :B>. When two fragments are combined,
 * the original fragments are removed from the lattice.
 * @author galarraga
 *
 */
public class NaiveMergeLattice extends Lattice {
	
	
	class FragmentPairComparator implements Comparator<Pair<Fragment, Fragment>> {
		@Override
		public int compare(Pair<Fragment, Fragment> o1, Pair<Fragment, Fragment> o2) {
			// TODO Auto-generated method stub
			return Long.compare(o2.getLeft().size() + o2.getRight().size(), o1.getLeft().size() + o1.getRight().size());
		}
		
	}
	
	private PriorityQueue<Pair<Fragment, Fragment>> propertyMergeQueue;
	
	private PriorityQueue<Pair<Fragment, Fragment>> provenanceMergeQueue;
	
	private boolean initializedPropertyMerge;
	
	private boolean initializedProvenanceMerge;
	
	public static int defaultMaxFragmentsCount = 1000;
	
	public static int defaultMinFragmentsCount = 50;
	
	private int maxFragmentsCount;
	
	private int minFragmentsCount;

	NaiveMergeLattice(Fragment root, RDFCubeStructure schema, RDFCubeDataSource data) {
		super(root, schema, data);
		initializedPropertyMerge = false;
		initializedProvenanceMerge = false;
		setMaxFragmentsCount(defaultMaxFragmentsCount);
		setMinFragmentsCount(defaultMinFragmentsCount);
	}

	
	public int getMaxFragmentsCount() {
		return maxFragmentsCount;
	}


	public void setMaxFragmentsCount(int maxFragmentsCount) {
		this.maxFragmentsCount = maxFragmentsCount;
	}


	public int getMinFragmentsCount() {
		return minFragmentsCount;
	}


	public void setMinFragmentsCount(int minFragmentsCount) {
		this.minFragmentsCount = minFragmentsCount;
	}


	private void initializePropertyMergeQueue() {
		// This priority queue contains all pairs of fragments that share a relation
		// sorted ascendently by aggregated size (we want to merge smaller fragments first)
		propertyMergeQueue = new PriorityQueue<>(provenanceId2FragmentMap.size() * 3, new FragmentPairComparator());
		for (String pid : provenanceId2FragmentMap.keys()) {
			List<Fragment> fragmentsWithProvenanceId = new ArrayList<Fragment>(provenanceId2FragmentMap.get(pid));
			for (int i = 0; i < fragmentsWithProvenanceId.size() - 1; ++i) {
				for (int j = i + 1; j < fragmentsWithProvenanceId.size(); ++j) {
					Fragment fi = fragmentsWithProvenanceId.get(i); 
					Fragment fj = fragmentsWithProvenanceId.get(j);
					if (!fi.getPredicates().isEmpty() && !fj.getPredicates().isEmpty()) {
						propertyMergeQueue.add(new MutablePair<>(fi, fj));
					}
				}
			}
		}
	}
	
	private void initializeProvenanceMergeQueue() {
		provenanceMergeQueue = new PriorityQueue<>(provenanceId2FragmentMap.size() * 3, new FragmentPairComparator());
		for (int i = 0; i < onlyProvenanceIdFragments.size() - 1; ++i) {
			for (int j = i + 1; j < onlyProvenanceIdFragments.size(); ++j) {
				provenanceMergeQueue.add(new MutablePair<>(onlyProvenanceIdFragments.get(i), onlyProvenanceIdFragments.get(j)));
			}
		}
		
	}

	@Override
	public boolean isMergeStartConditionForfilled() {
		return this.data.count() == getRoot().size() 
				&& size() > maxFragmentsCount;
	}

	@Override
	public boolean isMergeEndConditionForfilled() {
		return size() <= minFragmentsCount;
	}

	/**
	 * It merges the most promising couple of fragments, first by predicate, and if not possible,
	 * by provenance.
	 */
	@Override
	public boolean merge() {
		if (!initializedPropertyMerge) {
			initializePropertyMergeQueue();
			initializedPropertyMerge = true;
		}
		
		// First merge by property
		if (!propertyMergeQueue.isEmpty()) {
			while (!propertyMergeQueue.isEmpty()) {
				Pair<Fragment, Fragment> fragmentsToMerge = propertyMergeQueue.poll();
				
				// Check whether one of the fragments was already merged
				if (!contains(fragmentsToMerge.getLeft()) || 
						!contains(fragmentsToMerge.getRight())) {
					continue;
				} else {
					// Merge the fragments
					Fragment newFragment = mergeByRelation(fragmentsToMerge.getLeft(), fragmentsToMerge.getRight());
					if (newFragment != null) {
						// This loop will run once because the fragments have only one provenance identifier
						for (String pid : newFragment.getProvenanceIdentifiers()) {
							// Schedule the new fragment for merging
							Collection<Fragment> mergeCandidates = provenanceId2FragmentMap.get(pid);
							for (Fragment mergeCandidate : mergeCandidates) {
								if (mergeCandidate != newFragment && !mergeCandidate.getPredicates().isEmpty()) {
									propertyMergeQueue.add(new MutablePair<>(newFragment, mergeCandidate));
								}
							}
						}
											
						return true;
					}
				}
			}
		}
		
		// Merge according to the other strategy
		if (!initializedProvenanceMerge) {
			initializeProvenanceMergeQueue();
			initializedProvenanceMerge = true;
		}
		
		if (!provenanceMergeQueue.isEmpty()) {
			while (!provenanceMergeQueue.isEmpty()) {
				Pair<Fragment, Fragment> fragmentsToMerge = provenanceMergeQueue.poll();
				if (!contains(fragmentsToMerge.getLeft()) || 
						!contains(fragmentsToMerge.getRight())) {
					continue;
				} else {
					Fragment newFragment = mergeByProvenanceId(fragmentsToMerge.getLeft(), fragmentsToMerge.getRight());
					if (newFragment != null) {
						// The fragments at this stage may have multiple predicates
						for (Fragment mergeCandidate : onlyProvenanceIdFragments) {
							if (mergeCandidate != newFragment && !mergeCandidate.hasCommonProvenanceIds(newFragment)) {
								provenanceMergeQueue.add(new MutablePair<>(newFragment, mergeCandidate));
							}
						}
						
						return true;
					}
				}
			}			
		}
		
		return false;
	}

}
