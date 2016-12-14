package dk.aau.cs.qweb.pec.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DimensionHierarchy {
	private Set<String> relations;
	
	private Map<String, String> rollupGraph;
	
	private String name;

	public DimensionHierarchy(String dimensionName) {
		relations = new LinkedHashSet<>();
		rollupGraph = new HashMap<>();
		name = dimensionName;
	}
	
	/**
	 * Adds a relation between l1 and l2 saying the 
	 * @param l1
	 * @param l2
	 */
	public void addRollup(String l1, String l2) {
		relations.add(l1);
		relations.add(l2);
		rollupGraph.put(l1, l2);
	}
	
	public String getRoot() {
		List<String> values = new ArrayList<String>(relations);
		values.removeAll(rollupGraph.values());
		if (values.size() == 1) {
			return values.get(0);
		} else {
			return null;
		}
	}
	
	@Override
	public String toString() {
		StringBuilder strBuilder = new StringBuilder();
		String root = getRoot();
		strBuilder.append(name + ": ");
		if (root != null) {
			String start = root;
			strBuilder.append(start);
			
			while (true) {
				String next = rollupGraph.get(start);
				if (next == null) break;
				strBuilder.append("-> " + next);
				start = next;
			}
		}
			
		return strBuilder.toString();
	}

	public void addRelation(String relation) {
		relations.add(relation);		
	}

	/**
	 * If there is a rollup sequence  a' -> a'' -> a''', a' -> b, getDimensionRelationsAtLevel("A", 2)
	 * will return a set containing a'' and b since they are reachable at 2 hops. 
	 * @param dimension
	 * @param level An integer greater or equal than 0.
	 * @return
	 */
	public Set<String> getRelationsAtLevel(int level) {
		Set<String> result = new LinkedHashSet<>();
		String start = getRoot();
		for (int i = 0; i <= level; ++i) {
			String next = rollupGraph.get(start);
			start = next;
			if (next == null) break;
		}
		
		if (start != null)
			result.add(start);
		
		return result;
	}

}
