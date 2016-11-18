package dk.aau.cs.qweb.pec.fragmentsselector;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.lattice.Lattice;

public class GreedyFragmentsSelector extends FragmentsSelector {

	protected PrintStream outStream;
	
	/**
	 * It stores the subject-subject join counts between fragments.
	 */
	private Map<Pair<Fragment, Fragment>, Long> ssJoinCountCache;


	public GreedyFragmentsSelector(Lattice lattice) {
		super(lattice);
		ssJoinCountCache = new LinkedHashMap<>();
		outStream = System.out;
	}
	
	public GreedyFragmentsSelector(Lattice lattice, String logFile) throws FileNotFoundException {
		super(lattice, logFile);
		ssJoinCountCache = new LinkedHashMap<>();
		outStream = new PrintStream(logFile);
	}
	
	@Override
	protected void finalize() {
		if (outStream != System.out)
			outStream.close();
	}
	
	public Lattice getLattice() {
		return lattice;
	}
	
	
	@Override
	public Set<Fragment> select(long budget) throws DatabaseConnectionIsNotOpen {
		lattice.getData().open();
		Set<Fragment> result = new LinkedHashSet<>();
		PriorityQueue<Pair<Fragment, Float>> benefitQueue = new PriorityQueue<>(lattice.size(), 
				new Comparator<Pair<Fragment, Float>>(
						) {

							@Override
							public int compare(Pair<Fragment, Float> o1, Pair<Fragment, Float> o2) {
								int compare = Float.compare(o2.getRight(), o1.getRight());
								if (compare == 0) {
									return Long.compare(o1.getLeft().size(), o2.getLeft().size());
								} else {
									return compare;
								}
								
							}
			
				});
		long cost = 0;
		calculateBenefits(benefitQueue, result, budget);
		while (!benefitQueue.isEmpty()) {
			log(benefitQueue);
			
			long additionalCost = 0;
			Pair<Fragment, Float> best =  benefitQueue.poll();
			Fragment bestFragment = best.getLeft();
			additionalCost += bestFragment.size();
			Set<Fragment> metaFragments = lattice.getMetadataFragments(bestFragment);
			for (Fragment metaFragment : metaFragments) {
				if (!result.contains(metaFragment)) {
					additionalCost += metaFragment.size();
					metaFragments.add(metaFragment);
				}
			}
			//TODO: Discuss whether I should try to get the second most benefitial fragment then
			if (cost + additionalCost > budget)
				break;
			
			result.add(bestFragment);
			result.addAll(metaFragments);
			cost += additionalCost;
			calculateBenefits(benefitQueue, result, budget - cost);
		}
		lattice.getData().close();
		
		return result;
	}

	private void log(PriorityQueue<Pair<Fragment, Float>> benefitQueue) {
		outStream.println("====== Iteration ==== ");
		PriorityQueue<Pair<Fragment, Float>> copy = new PriorityQueue<>(benefitQueue);
		while (!copy.isEmpty())
			outStream.println(copy.poll());
		
		outStream.println("====== Iteration ==== ");
		
	}

	/**
	 * 
	 * @param lattice
	 * @param benefitQueue
	 * @param selectedSoFar
	 * @throws DatabaseConnectionIsNotOpen 
	 */
	private void calculateBenefits(PriorityQueue<Pair<Fragment, Float>> benefitQueue, 
			Set<Fragment> selectedSoFar, long availableBudget) throws DatabaseConnectionIsNotOpen {
		benefitQueue.clear();
		for (Fragment fragment : lattice) {
			if (!fragment.isMetadata() 
					&& !selectedSoFar.contains(fragment)
					&& fragment.size() <= availableBudget) {
				float benefit = getBenefit(fragment, selectedSoFar, lattice);
				benefitQueue.add(new MutablePair<>(fragment, benefit));
			}
		}
	}
	

	private float getBenefit(Fragment fragment, Set<Fragment> selectedSoFar, Lattice lattice) throws DatabaseConnectionIsNotOpen {
		float duplicatesCost = 0f;
		float joinBenefit = 0f;
		float metadataCost = 0f;
		float measureFactor = lattice.getStructure().containsMeasureTriples(fragment.getSignatures()) ? 2 : 1;
		Set<Fragment> ancestors = lattice.getAncestors(fragment);
		
		// Account for supplementary metadata that should be materialized
		Set<Fragment> metaFragments = lattice.getMetadataFragments(fragment);
		metaFragments.removeAll(selectedSoFar);
		for (Fragment metaFragment : metaFragments) {
			metadataCost += metaFragment.size();
		}
		
		for (Fragment selected : selectedSoFar) {
			// Join benefit w.r.t this fragment
			joinBenefit += joinCount(fragment, selected);
			
			// Duplicate cost w.r.t ancestors
			if (ancestors.contains(selected)) {
				duplicatesCost += fragment.size();
			}
		}
		
		return (joinBenefit + 1) * measureFactor / (fragment.size() + metadataCost + duplicatesCost);
	}
	
	private Long computeJoinFromCachedComputations(Fragment fragment, Set<Fragment> children) {
		long result = 0l;
		
		// First verify whether we can compute the join based on 
		// the children of one of the fragments.
		for (Fragment child : children) {
			Pair<Fragment, Fragment> key1 = new MutablePair<>(child, fragment);
			Pair<Fragment, Fragment> key2 = new MutablePair<>(fragment, child);
			if (ssJoinCountCache.containsKey(key1)) {
				result += ssJoinCountCache.get(key1).longValue();
			} else if (ssJoinCountCache.containsKey(key2)) {
				result += ssJoinCountCache.get(key2).longValue();
			} else {
				return null;
			}
		}
		
		return new Long(result);
	}

	/**
	 * It calculates the number of subject-subject joins between two fragments.
	 * @param fragment
	 * @param selected
	 * @return
	 * @throws DatabaseConnectionIsNotOpen 
	 */
	private long joinCount(Fragment fragment1, Fragment fragment2) throws DatabaseConnectionIsNotOpen {
		Pair<Fragment, Fragment> key1 = new MutablePair<>(fragment1, fragment2);
		Pair<Fragment, Fragment> key2 = new MutablePair<>(fragment2, fragment1);
		
		if (ssJoinCountCache.containsKey(key1) 
				&& ssJoinCountCache.containsKey(key2)) {
			return ssJoinCountCache.get(key1);		
		}
		// Use the schema information to figure out if the fragments can potentially join
		long count = 0l;
		if (fragment1.canSignatureJoinSubject2Subject(fragment2)) {
			Set<Fragment> childrenOfFragment2 = lattice.getChildren(fragment2);
			Long cachedCount = null;
			if (!childrenOfFragment2.isEmpty()) {
				cachedCount = computeJoinFromCachedComputations(fragment1, childrenOfFragment2);
			}
			
			if (cachedCount == null) {
				Set<Fragment> childrenOfFragment1 = lattice.getChildren(fragment1);
				if (!childrenOfFragment1.isEmpty()) {
					cachedCount = computeJoinFromCachedComputations(fragment2, childrenOfFragment1);
				}
			}
						
			
			if (cachedCount == null) {
				count = lattice.getData().joinCount(fragment1.getSignatures(), 
						fragment2.getSignatures());
			} else {
				count = cachedCount.longValue();
			}
			ssJoinCountCache.put(key1, count);
			ssJoinCountCache.put(key2, count);

		}
		
		return count;
	}

}
