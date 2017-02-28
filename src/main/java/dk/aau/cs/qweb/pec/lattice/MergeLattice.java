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

public abstract class MergeLattice extends Lattice {

	protected PriorityQueue<Pair<Fragment, Fragment>> propertyMergeQueue;
	
	protected PriorityQueue<Pair<Fragment, Fragment>> provenanceMergeQueue;
	
	protected boolean initializedPropertyMerge;
	
	protected boolean initializedProvenanceMerge;
	
	public static int defaultMaxFragmentsCount = 1000;
	
	public static int defaultMinFragmentsCount = 50;
	
	protected int maxFragmentsCount;
	
	protected int minFragmentsCount;
	
	
	class FragmentPairComparator implements Comparator<Pair<Fragment, Fragment>> {
		@Override
		public int compare(Pair<Fragment, Fragment> o1, Pair<Fragment, Fragment> o2) {
			// TODO Auto-generated method stub
			return Long.compare(o1.getLeft().size() + o1.getRight().size(), o2.getLeft().size() + o2.getRight().size());
		}
		
	}
	
	
	MergeLattice(Fragment root, RDFCubeStructure schema, RDFCubeDataSource data) {
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
	

	protected void initializePropertyMergeQueue() {
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
	
	protected void initializeProvenanceMergeQueue() {
		provenanceMergeQueue = new PriorityQueue<>(provenanceId2FragmentMap.size() * 3, new FragmentPairComparator());
		for (int i = 0; i < onlyProvenanceIdFragments.size() - 1; ++i) {
			for (int j = i + 1; j < onlyProvenanceIdFragments.size(); ++j) {
				provenanceMergeQueue.add(new MutablePair<>(onlyProvenanceIdFragments.get(i), onlyProvenanceIdFragments.get(j)));
			}
		}
		
	}

	
	protected boolean propertyMerge() {
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
		
		return false;
	}
	
	protected boolean provenanceMerge() {
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
	

	@Override
	public boolean isMergeStartConditionForfilled() {
		return this.data.count() == getRoot().size() 
				&& size() > maxFragmentsCount;
	}

	@Override
	public boolean isMergeEndConditionForfilled() {
		return size() <= minFragmentsCount;
	}

}
