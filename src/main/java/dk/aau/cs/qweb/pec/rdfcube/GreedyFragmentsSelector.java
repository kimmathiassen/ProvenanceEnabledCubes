package dk.aau.cs.qweb.pec.rdfcube;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class GreedyFragmentsSelector implements FragmentsSelector {

	@Override
	public Set<RDFCubeFragment> select(FragmentLattice lattice, long budget) {
		Set<RDFCubeFragment> result = new LinkedHashSet<>();
		PriorityQueue<Pair<RDFCubeFragment, Float>> benefitQueue = new PriorityQueue<>(lattice.size(), 
			new Comparator<Pair<RDFCubeFragment, Float>>(
					) {
	
						@Override
						public int compare(Pair<RDFCubeFragment, Float> o1, Pair<RDFCubeFragment, Float> o2) {
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
			Pair<RDFCubeFragment, Float> best =  benefitQueue.poll();
			RDFCubeFragment bestFragment = best.getLeft();
			additionalCost += bestFragment.size();
			Set<RDFCubeFragment> metaFragments = lattice.getMetadataFragments(bestFragment);
			for (RDFCubeFragment metaFragment : metaFragments) {
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
			calculateBenefits(lattice, benefitQueue, result);
		}
		
		return result;
	}

	/**
	 * 
	 * @param lattice
	 * @param benefitQueue
	 * @param selectedSoFar
	 */
	private void calculateBenefits(FragmentLattice lattice, PriorityQueue<Pair<RDFCubeFragment, Float>> benefitQueue, 
			Set<RDFCubeFragment> selectedSoFar) {
		benefitQueue.clear();
		for (RDFCubeFragment fragment : lattice) {
			if (!fragment.isMetadata()) {
				float benefit = getBenefit(fragment, selectedSoFar, lattice);
				benefitQueue.add(new MutablePair<>(fragment, benefit));
			}
		}
	}
	
	private float getBenefit(RDFCubeFragment fragment, Set<RDFCubeFragment> selectedSoFar, FragmentLattice lattice) {
		float duplicatesCost = 0f;
		float joinBenefit = 0f;
		float metadataCost = 0f;
		float measureFactor = lattice.getStructure().containsMeasureTriples(fragment.getSignatures()) ? 2 : 1;
		Set<RDFCubeFragment> ancestors = lattice.getAncestors(fragment);
		
		// Account for supplementary metadata that should be materialized
		Set<RDFCubeFragment> metaFragments = lattice.getMetadataFragments(fragment);
		metaFragments.removeAll(selectedSoFar);
		for (RDFCubeFragment metaFragment : metaFragments) {
			metadataCost += metaFragment.size();
		}
		
		for (RDFCubeFragment selected : selectedSoFar) {
			// Join benefit w.r.t this fragment
			joinBenefit += lattice.getData().joinCount(fragment.getSignatures(), 
					selected.getSignatures());
			
			// Duplicate cost w.r.t ancestors
			if (ancestors.contains(selected)) {
				duplicatesCost += fragment.size();
			}
		}
		
		return joinBenefit * measureFactor / (fragment.size() + metadataCost + duplicatesCost);
	}

}
