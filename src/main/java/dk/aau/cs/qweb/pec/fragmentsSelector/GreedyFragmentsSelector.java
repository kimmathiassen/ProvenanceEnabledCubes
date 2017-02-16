package dk.aau.cs.qweb.pec.fragmentsSelector;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.lattice.Lattice;
import dk.aau.cs.qweb.pec.logger.Logger;

public class GreedyFragmentsSelector extends FragmentsSelector {

	private static long MINIMUM_FRAGMENT_SIZE = 5;

	protected PrintStream outStream;
	
	
	public static void setMininumFragmentSize(long minimumFragmentSize) {
		MINIMUM_FRAGMENT_SIZE = minimumFragmentSize;
	}
	
	public static long getMinimumFragmentSize() {
		return MINIMUM_FRAGMENT_SIZE;
	}
	
	/**
	 * It stores the subject-subject join counts between fragments.
	 */
	public GreedyFragmentsSelector(Lattice lattice) {
		super(lattice);
		outStream = System.out;
	}
	
	public GreedyFragmentsSelector(Lattice lattice, String logFile) throws FileNotFoundException {
		super(lattice, logFile);
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
	public Set<Fragment> select(long budget, Logger logger) throws DatabaseConnectionIsNotOpen {
		logger.log("Greedy algorithm, no logging");
		lattice.getData().open();
		Set<Fragment> result = new LinkedHashSet<>();
		PriorityQueue<Pair<Fragment, Float>> benefitQueue = new PriorityQueue<>(lattice.size(), 
				new Comparator<Pair<Fragment, Float>>(
						) {

							@Override
							public int compare(Pair<Fragment, Float> o1, Pair<Fragment, Float> o2) {
								int compare = Float.compare(o2.getRight(), o1.getRight());
								if (compare == 0) {
									return Long.compare(o2.getLeft().size(), o1.getLeft().size());
								} else {
									return compare;
								}
								
							}
			
				});
		long cost = 0;
		calculateBenefits(benefitQueue, result, budget);
		while (!benefitQueue.isEmpty()) {
			if (loggingEnabled)
				log(benefitQueue, budget - cost);
			
			long additionalCost = 0;
			Pair<Fragment, Float> best =  benefitQueue.poll();
			Fragment bestFragment = best.getLeft();
			additionalCost += bestFragment.size();
			//TODO: Discuss whether I should try to get the second most benefitial fragment then
			if (cost + additionalCost > budget)
				break;
			
			result.add(bestFragment);
			cost += additionalCost;
			calculateBenefits(benefitQueue, result, budget - cost);
		}
		lattice.getData().close();
		
		return result;
	}

	private void log(PriorityQueue<Pair<Fragment, Float>> benefitQueue, long availableBudget) {
		outStream.println("Available budget: " + availableBudget);
		outStream.println("====== Iteration, benefit queue ==== ");
		PriorityQueue<Pair<Fragment, Float>> copy = new PriorityQueue<>(benefitQueue);
		while (!copy.isEmpty())
			outStream.println(copy.poll());
		
		outStream.println("====== End of benefit queue ==== ");
		
	}

	/**
	 * 
	 * @param initialLattice
	 * @param benefitQueue
	 * @param selectedSoFar
	 * @throws DatabaseConnectionIsNotOpen 
	 */
	private void calculateBenefits(PriorityQueue<Pair<Fragment, Float>> benefitQueue, 
			Set<Fragment> selectedSoFar, long availableBudget) throws DatabaseConnectionIsNotOpen {
		benefitQueue.clear();
		for (Fragment fragment : lattice) {
			if (!fragment.isRedundant() &&
					!selectedSoFar.contains(fragment)
					&& fragment.size() <= availableBudget && 
					fragment.size() >= MINIMUM_FRAGMENT_SIZE) {
				float benefit = getBenefit(fragment, selectedSoFar, lattice);
				benefitQueue.add(new MutablePair<>(fragment, benefit));
			}
		}
	}
	

	private float getBenefit(Fragment fragment, Set<Fragment> selectedSoFar, Lattice lattice) throws DatabaseConnectionIsNotOpen {
		float duplicatesCost = 0f;
		float measureFactor = fragment.containsMeasureTriples() ? 2 : 1;
		Set<Fragment> ancestors = lattice.getAncestors(fragment);
		
		// Account for supplementary metadata that should be materialized
		
		for (Fragment selected : selectedSoFar) {
			// Duplicate cost w.r.t ancestors
			if (ancestors.contains(selected)) {
				duplicatesCost += fragment.size();
			}
		}
		
		float discourageFactor = 1.0f / selectedSoFar.size();
		//float discourageFactor = 1.0f;
		
		return 1 * measureFactor / (discourageFactor * fragment.size() + duplicatesCost);
	}
}
