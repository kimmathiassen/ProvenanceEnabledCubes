package dk.aau.cs.qweb.pec.fragmentsselector;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.lattice.Lattice;

public class GreedyFragmentsSelector implements FragmentsSelector {

	@Override
	public Set<Fragment> select(Lattice lattice, long budget) {
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
		calculateBenefits(lattice, benefitQueue, result, budget);
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
			if (cost + additionalCost > budget)
				break;
			
			result.add(bestFragment);
			result.addAll(metaFragments);
			cost += additionalCost;
			calculateBenefits(lattice, benefitQueue, result, budget - cost);
		}
		
		return result;
	}

	private void log(PriorityQueue<Pair<Fragment, Float>> benefitQueue) {
		System.out.println("====== Iteration ==== ");
		PriorityQueue<Pair<Fragment, Float>> copy = new PriorityQueue<>(benefitQueue);
		while (!copy.isEmpty())
			System.out.println(copy.poll());
		
		System.out.println("====== Iteration ==== ");
		
	}

	/**
	 * 
	 * @param lattice
	 * @param benefitQueue
	 * @param selectedSoFar
	 */
	private void calculateBenefits(Lattice lattice, PriorityQueue<Pair<Fragment, Float>> benefitQueue, 
			Set<Fragment> selectedSoFar, long availableBudget) {
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
	

	private float getBenefit(Fragment fragment, Set<Fragment> selectedSoFar, Lattice lattice) {
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
			joinBenefit += joinCount(fragment, selected, lattice);
			
			// Duplicate cost w.r.t ancestors
			if (ancestors.contains(selected)) {
				duplicatesCost += fragment.size();
			}
		}
		
		return (joinBenefit + 1) * measureFactor / (fragment.size() + metadataCost + duplicatesCost);
	}

	/**
	 * It calculates the number of subject-subject joins between two fragments.
	 * @param fragment
	 * @param selected
	 * @return
	 */
	private float joinCount(Fragment fragment1, Fragment fragment2, Lattice lattice) {
		// Use the schema information to figure out if the fragments can potentially join
		if (fragment1.canJoinSubject2Subject(fragment2)) {
			return lattice.getData().joinCount(fragment1.getSignatures(), 
					fragment2.getSignatures());
		} else {
			return 0f;
		}
	}

}
