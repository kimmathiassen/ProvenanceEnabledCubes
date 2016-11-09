package dk.aau.cs.qweb.pec.rdfcube.fragmentsselector;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import dk.aau.cs.qweb.pec.rdfcube.fragment.Fragment;
import dk.aau.cs.qweb.pec.rdfcube.lattice.FragmentLattice;

public class GreedyFragmentsSelector implements FragmentsSelector {

	@Override
	public Set<Fragment> select(FragmentLattice lattice, long budget) {
		Set<Fragment> result = new LinkedHashSet<>();
		PriorityQueue<Pair<Fragment, Float>> benefitQueue = new PriorityQueue<>(lattice.size(), 
				new Comparator<Pair<Fragment, Float>>(
						) {

							@Override
							public int compare(Pair<Fragment, Float> o1, Pair<Fragment, Float> o2) {
								int compare = Float.compare(o1.getRight(), o2.getRight());
								if (compare == 0) {
									return Long.compare(o1.getLeft().size(), o2.getLeft().size());
								} else {
									return compare;
								}
								
							}
			
				});
		long cost = 0;
		calculateBenefits(lattice, benefitQueue, result);
		while (true) {
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
		}
		
		return result;
	}

	/**
	 * 
	 * @param lattice
	 * @param benefitQueue
	 * @param selectedSoFar
	 */
	private void calculateBenefits(FragmentLattice lattice, PriorityQueue<Pair<Fragment, Float>> benefitQueue, 
			Set<Fragment> selectedSoFar) {
		benefitQueue.clear();
		for (Fragment fragment : lattice) {
			if (!fragment.isMetadata()) {
				float benefit = getBenefit(fragment, selectedSoFar, lattice);
				benefitQueue.add(new MutablePair<>(fragment, benefit));
			}
		}
	}
	
	private float getBenefit(Fragment fragment, Set<Fragment> selectedSoFar, FragmentLattice lattice) {
		return 0f;
	}

}
