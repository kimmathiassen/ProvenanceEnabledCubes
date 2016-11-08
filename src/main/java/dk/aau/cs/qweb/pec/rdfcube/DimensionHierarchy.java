package dk.aau.cs.qweb.pec.rdfcube;

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
	
	@Override
	public String toString() {
		StringBuilder strBuilder = new StringBuilder();
		List<String> values = new ArrayList<String>(relations);
		values.removeAll(rollupGraph.values());
		strBuilder.append(name + ": ");
		if (values.size() == 1) {
			String start = values.get(0);
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

}
