package dk.aau.cs.qweb.pec.QueryEvaluation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;

import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.types.Signature;

public class AnalyticalQuery {
	private String  originalQuery = "";
	private String  firstPart = "";
	private String  lastPart = "";
	private List<Signature> triplePatterns = new ArrayList<Signature>(); 
	private List<String> fromClause = new ArrayList<String>();
	private Map<String,String> prefixes = new HashMap<String,String>();


	public AnalyticalQuery(File queryFile, RDFCubeStructure structure) throws IOException {
		
		originalQuery = FileUtils.readFileToString(queryFile);
		String[] split = originalQuery.split("WHERE");
		firstPart = split[0];
		lastPart = split[1];
		
		getPrefixes(firstPart);
		
		for (String triplePatternDotSplit : lastPart.split("\\.")) {
			triplePatternDotSplit = triplePatternDotSplit.replaceAll("\\{", "");
			triplePatternDotSplit = triplePatternDotSplit.replaceAll("\\}", "");
			triplePatternDotSplit = triplePatternDotSplit.trim();
			
			if (triplePatternDotSplit.contains("UNION")) { 				
			} else if (triplePatternDotSplit.contains("FILTER")) {				
			} else {
				String subject = "";
				String predicate = "";
				String object = "";
				
				for (String triplePatternSimiColonSplit : triplePatternDotSplit.split(";")) {
					triplePatternSimiColonSplit = triplePatternSimiColonSplit.trim();
					String[] elements = triplePatternSimiColonSplit.split(" ");
					
					if (elements.length == 3) {
						//throw new IllegalArgumentException("triples pattern could not be passed: "+ triplePattern);
						subject = addPrefix(elements[0]);
						predicate = addPrefix(elements[1]);
						object = addPrefix(elements[2]);
					} else if (elements.length == 2) {
						predicate = addPrefix(elements[0]);
						object = addPrefix(elements[1]);
					}
					
					Pair<String, String> domainRange = structure.getDomainAndRange(predicate);
					
					if (subject.contains("?")) {
						subject = domainRange.getKey();
					}
					
					if (object.contains("^^")) {
						String[] hatSplit = object.split("^^");
						object = hatSplit[1];
					} else {
						object = domainRange.getValue();
					}
					triplePatterns.add(new Signature(subject, predicate, object, null));
				}
				
				
			}
		}
	}

	private void getPrefixes(String firstPart2) {
		String[] selectSplit = firstPart2.split("SELECT");
		String[] PrefixSplit = selectSplit[0].split("PREFIX");
		for (String prefix : PrefixSplit) {
			if (!prefix.isEmpty()) {
				prefix = prefix.trim();
				String[] split =  prefix.split(" ");
				String url = split[1];
				url = url.replaceAll("<", "");
				url = url.replaceAll(">", "");
				
				prefixes.put(split[0], url);
			}
		}
	}

	private String addPrefix(String string) {
		if (string.contains(":")) {
			String[] colonSplit = string.split(":");
			if (colonSplit[0].isEmpty()) {
				return "<"+prefixes.get(":")+colonSplit[1]+">";
			} else {
				return "<"+prefixes.get(colonSplit[0])+colonSplit[1]+">";
			}
		}
		return string;
	}

	public List<Signature> getTriplePatterns() {
		return triplePatterns;
	}

	public void addFrom(List<String> graphNames) {
		fromClause.addAll(graphNames);
	}

	public void addFrom(String graphLabel) {
		fromClause.add(graphLabel);
	}

	public String getQuery() {
		String query = firstPart+"\n";
		for (String from : fromClause) {
			query +="FROM "+ from + "\n";
		}
		query += lastPart;
		
		return query;
	}

	public List<String> getFromClause() {
		return fromClause;
	}
}
