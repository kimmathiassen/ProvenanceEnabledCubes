package dk.aau.cs.qweb.pec.queryEvaluation;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.lattice.Lattice;
import dk.aau.cs.qweb.pec.logger.Logger;
import dk.aau.cs.qweb.pec.types.Signature;

public class LRUCache {
	
	private MaterializedFragments materializedFragments;
	
	private String datasetPath;
	
	private Lattice sourceLattice;
	
	private long budget;
	
	private Logger logger;
	
	public LRUCache(long budget, String datasetPath, Lattice sourceLattice, Logger logger) {
		materializedFragments = new JenaMaterializedFragments(Collections.emptySet(), 
				datasetPath, sourceLattice, logger);
		this.budget = budget;
		this.datasetPath = datasetPath;
		this.sourceLattice = sourceLattice;
		this.logger = logger;
	}
	
	public long getBudget() {
		return budget;
	}
	
	public void updateCache(Set<String> fromClauses, MaterializedFragments lruFragments) {
		purge();
		Set<Fragment> sortedFragments = getFragmentsFromFromClauses(fromClauses, lruFragments);
		Set<Fragment> selectedFragments = new LinkedHashSet<>();
		// Put them in the knapsack while the budget is not surpassed
		Iterator<Fragment> it = sortedFragments.iterator();
		Fragment current = it.next();
		long usedBudget = 0;
		while ((usedBudget + current.size()) < budget) {
			selectedFragments.add(current);
			usedBudget += current.size();
			if (it.hasNext()) {
				current = it.next();
			} else {
				break;
			}
		}
		
		materializedFragments = new JenaMaterializedFragments(selectedFragments, datasetPath, sourceLattice, logger);
	}
	
	private Set<Fragment> getFragmentsFromFromClauses(Set<String> fromClauses, MaterializedFragments lruFragments) {
		Set<Fragment> result = new TreeSet<>(new Comparator<Fragment>() {
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
	
		int nFragments = 0;
		int nGraphs = 0;
		for (String fromClause : fromClauses) {
			// If it is a fragment, add it directly
			Fragment f = null;
			if (fromClause.contains("fragment")) {
				f = lruFragments.getFragmentByUrl(fromClause);
				++nFragments;
			} else {
				// Then it is a graph and we should get it from the lattice
				f = sourceLattice.getFragmentBySingleSignature(new Signature(null, null, null, fromClause));
				assert(f != null);
				++nGraphs;
			}
			
			if (f != null) {
				result.add(f);
			}
		}
		
		System.out.println("LRU has reused " + nFragments + " fragments");
		System.out.println("LRU has materialized " + nGraphs + " graphs");
		
		return result;
	}

	public MaterializedFragments getContents() {
		return materializedFragments;
	}
	
	public void purge() {
		materializedFragments = null;
	}
	
	@Override
	public String toString() {
		return materializedFragments.toString();
	}

}
