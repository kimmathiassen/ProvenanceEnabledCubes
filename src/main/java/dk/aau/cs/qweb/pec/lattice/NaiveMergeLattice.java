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
 * A naive merge lattice 
 * @author galarraga
 *
 */
public class NaiveMergeLattice extends Lattice {
	
	
	class FragmentPairComparator implements Comparator<Pair<Fragment, Fragment>> {
		@Override
		public int compare(Pair<Fragment, Fragment> o1, Pair<Fragment, Fragment> o2) {
			// TODO Auto-generated method stub
			return Long.compare(o1.getLeft().size() + o1.getRight().size(), o2.getLeft().size() + o2.getRight().size());
		}
		
	}
	
	
	public static int MinFragmentsCount = 50;
	
	private PriorityQueue<Pair<Fragment, Fragment>> propertyMergeQueue;
	
	private PriorityQueue<Pair<Fragment, Fragment>> provenanceMergeQueue;
	
	private boolean initializedPropertyMerge;
	
	private boolean initializedProvenanceMerge;

	NaiveMergeLattice(Fragment root, RDFCubeStructure schema, RDFCubeDataSource data) {
		super(root, schema, data);
		initializedPropertyMerge = false;
		initializedProvenanceMerge = false;
	}

	
	private void initializePropertyMergeQueue() {
		// This priority queue contains all pairs of fragments that share a relation
		// sorted ascendently by aggregated size (we want to merge smaller fragments first)
		propertyMergeQueue = new PriorityQueue<>(provenanceId2FragmentMap.size() * 3, new FragmentPairComparator());
		for (String pid : provenanceId2FragmentMap.keys()) {
			List<Fragment> fragmentsWithProvenanceId = new ArrayList<Fragment>(provenanceId2FragmentMap.get(pid));
			for (int i = 0; i < fragmentsWithProvenanceId.size() - 1; ++i) {
				for (int j = i + 1; j < fragmentsWithProvenanceId.size(); ++i) {
					propertyMergeQueue.add(new MutablePair<>(fragmentsWithProvenanceId.get(i), fragmentsWithProvenanceId.get(j)));
				}
			}
		}
	}
	
	private void initializeProvenanceMergeQueue() {
		for (int i = 0; i < onlyProvenanceIdFragments.size() - 1; ++i) {
			for (int j = i + 1; j < onlyProvenanceIdFragments.size(); ++i) {
				provenanceMergeQueue.add(new MutablePair<>(onlyProvenanceIdFragments.get(i), onlyProvenanceIdFragments.get(j)));
			}
		}
		
	}

	@Override
	public boolean isMergeStartConditionForfilled() {
		return this.data.count() == getRoot().size();
	}

	@Override
	public boolean isMergeEndConditionForfilled() {
		return size() <= MinFragmentsCount;
	}

	@Override
	public boolean merge() {
		if (!initializedPropertyMerge) {
			initializePropertyMergeQueue();
			initializedPropertyMerge = true;
		}
		
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
					// This loop will run once because the fragments have only one provenance identifier
					for (String pid : newFragment.getProvenanceIdentifiers()) {
						// Schedule the new fragment for merging
						Collection<Fragment> mergeCandidates = provenanceId2FragmentMap.get(pid);
						for (Fragment mergeCandidate : mergeCandidates) {
							if (mergeCandidate != newFragment) {
								propertyMergeQueue.add(new MutablePair<>(newFragment, mergeCandidate));
							}
						}
					}
											
					if (newFragment != null) {
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
					// The fragments at this stage may have multiple predicates
					for (Fragment mergeCandidate : onlyProvenanceIdFragments) {
						if (mergeCandidate != newFragment && !mergeCandidate.hasCommonProvenanceIds(newFragment)) {
							provenanceMergeQueue.add(new MutablePair<>(newFragment, mergeCandidate));
						}
					}
					
					if (newFragment != null) {
						return false;
					}
				}
			}			
		}
		
		return false;
	}

}
