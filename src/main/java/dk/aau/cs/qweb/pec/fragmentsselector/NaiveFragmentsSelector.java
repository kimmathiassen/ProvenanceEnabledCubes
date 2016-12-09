package dk.aau.cs.qweb.pec.fragmentsselector;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.lattice.Lattice;
import dk.aau.cs.qweb.pec.types.Signature;

/**
 * It implements a naive fragments selector that selects the cube fragments in a breadth-first
 * search manner, starting with the measures, and the different dimension levels.
 * @author galarraga
 *
 */
public class NaiveFragmentsSelector extends GreedyFragmentsSelector {

	class FragmentsSizeComparator implements Comparator<Fragment> {

		public int compare(Signature s1, Signature s2) {
			int result;
			
			if (s1.getFirst() != null && s2.getFirst() != null) {
				result = s2.getFirst().compareTo(s1.getFirst());
				if (result != 0) {
					return result;
				}
			} else if (s1.getFirst() == null) {
				return 1;
			} else if (s2.getFirst() == null) {
				return -1;
			}
			
			if (s1.getPredicate() != null && s2.getPredicate() != null) {
				result = s2.getPredicate().compareTo(s1.getPredicate());
				if (result != 0) {
					return result;
				}
			} else if (s1.getPredicate() == null) {
				return 1;
			} else if (s2.getPredicate() == null) {
				return -1;
			}
			
			if (s1.getThird() != null && s2.getThird() != null) {
				result = s2.getThird().compareTo(s1.getThird());
				if (result != 0) {
					return result;
				}
			} else if (s1.getThird() == null) {
				return 1;
			} else if (s2.getThird() == null) {
				return -1;
			}
			
			if (s1.getGraphLabel() != null && s2.getGraphLabel() != null) {
				result = s2.getGraphLabel().compareTo(s1.getGraphLabel());
				if (result != 0) {
					return result;
				}
			} else if (s1.getGraphLabel() == null) {
				return 1;
			} else if (s2.getGraphLabel() == null) {
				return -1;
			}

			
			return 0;
		}
		
		@Override
		public int compare(Fragment o1, Fragment o2) {
			int sizeCmp = Long.compare(o2.size(), o1.size());
			if (sizeCmp == 0) {
				return compare(o2.getSomeSignature(), o1.getSomeSignature());
			}
			return sizeCmp;
		}
		
	}
	
	protected NaiveFragmentsSelector(Lattice lattice) {
		super(lattice);
	}

	public NaiveFragmentsSelector(Lattice lattice, String logFile) throws FileNotFoundException {
		super(lattice, logFile);
	}

	@Override
	public Set<Fragment> select(long budget) throws DatabaseConnectionIsNotOpen {
		Set<Fragment> result = new LinkedHashSet<>();
		SortedSet<Fragment> measureFragments = new TreeSet<Fragment>(new FragmentsSizeComparator());
		measureFragments.addAll(lattice.getMeasureFragments());
		if (loggingEnabled) this.outStream.println("Selecting the measures with available budget " + budget);
		
		long newBudget = budget - select(measureFragments, budget, result);
		
		if (newBudget > 0) {
			RDFCubeStructure schema = lattice.getStructure();
			Set<String> dimensions = schema.getDimensions();
			int level = 0;
			do {
				if (loggingEnabled) this.outStream.println("Current budget: " + newBudget);
				SortedSet<Fragment> dimensionFragments = new TreeSet<>(new FragmentsSizeComparator());
				List<String> coveredDimensions = new ArrayList<>();
				for (String dimension : dimensions) {
					Set<String> relationsAtLevel = schema.getDimensionRelationsAtLevel(dimension, level);
					if (relationsAtLevel.isEmpty()) {
						coveredDimensions.add(dimension);
						continue;
					}
					
					for (String relationAtLevel : relationsAtLevel) {
						Set<Fragment> fragments = lattice.getFragmentsForRelation(relationAtLevel);
						for (Fragment fragment : fragments) {
							if (!fragment.containsMetadata()) {
								dimensionFragments.add(fragment);
							}
						}
						
						Set<String> attributesAtLevelI = schema.getAttributes(relationAtLevel);
						for (String attrRelation : attributesAtLevelI) {
							Set<Fragment> attrFragments = lattice.getFragmentsForRelation(attrRelation);
							for (Fragment attrFragment : attrFragments) {
								if (!attrFragment.containsMetadata()) {
									dimensionFragments.add(attrFragment);
								}								
							}
						}
					}
				}
				
				newBudget -= select(dimensionFragments, newBudget, result);
				dimensions.removeAll(coveredDimensions);
				++level;
			} while(newBudget > 0 && !dimensions.isEmpty());
		}
		
	
		return result;
	}


	/**
	 * Remove a set of fragments from the input set and add them to the output set.
	 * The sum of size of the selected fragments plus its metadata should not exceed  
	 * the budget.
	 * 
	 * @param fragments
	 * @param budget
	 * @param output
	 * @return
	 */
	private long select(SortedSet<Fragment> input, long budget, Set<Fragment> output) {		
		long selected = 0l;
		for (Fragment fragment : input) {
			long cost = 0l;
			cost += fragment.size();
			Set<Fragment> metadataFragments = lattice.getMetadataFragments(fragment);
			for (Fragment metadataFragment : metadataFragments) {
				if (!output.contains(metadataFragment)) {
					cost += metadataFragment.size();
				}
			}
			
			if (selected + cost <= budget) {
				this.outStream.print(fragment + ", ");
				output.add(fragment);
				this.outStream.println("Metadata " + metadataFragments);
				output.addAll(metadataFragments);
				selected += cost;
			}
		}
		return selected;
	}
}
