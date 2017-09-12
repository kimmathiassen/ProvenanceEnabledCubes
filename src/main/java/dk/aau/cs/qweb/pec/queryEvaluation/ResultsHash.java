package dk.aau.cs.qweb.pec.queryEvaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

public class ResultsHash {
	
	public static int hash(ResultSet rs) {
		return serialize(rs).hashCode();
	}
	
	public static int hash(List<Map<String, String>> rs) {
		return serialize(rs).hashCode();
	}
	
	public static String serialize(List<Map<String, String>> rs) {
		StringBuilder strBuilder = new StringBuilder();
		for (Map<String, String> entry : rs) {
			strBuilder.append(serialize(entry));
		}
		return strBuilder.toString();
	}
	
	
	public static List<Map<String, String>> serialize(ResultSet rs) {
		List<Map<String, String>> result = new ArrayList<>();
		List<String> vars = rs.getResultVars();
		for ( ; rs.hasNext() ; ) {
			QuerySolution soln = rs.nextSolution();
			Map<String, String> entry = new LinkedHashMap<>();
			for (String var : vars) {
				String val = soln.get(var) == null ? null : soln.get(var).toString();
				entry.put(var, val);
			}
			result.add(entry);
	    }
		
		Collections.sort(result, new Comparator<Map<String, String>>() {
			
			@Override
			public int compare(Map<String, String> o1, Map<String, String> o2) {
				String s1 = ResultsHash.serialize(o1);
				String s2 = ResultsHash.serialize(o2);
				return s1.compareTo(s2);
			}
			
		});
		
		return result;
		
	}
	
	public static String serialize(Map<String, String> map) {
		StringBuilder strBuilder = new StringBuilder();
		for (Entry<String, String> entry : map.entrySet()) {
			strBuilder.append(entry.getKey());
			strBuilder.append(entry.getValue());
		}
		return strBuilder.toString();
	}

}
