package dk.aau.cs.qweb.pec.QueryEvaluation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	public AnalyticalQuery(File queryFile, RDFCubeStructure structure) throws IOException {
		
		originalQuery = FileUtils.readFileToString(queryFile);
		firstPart = extractString("SELECT","WHERE");
		lastPart = extractString("WHERE","\\Z");
		
		for (String triplePattern : lastPart.split("\\.")) {
			triplePattern = triplePattern.replaceAll("{", "");
			triplePattern = triplePattern.replaceAll("}", "");
			triplePattern = triplePattern.trim();
			
			if (triplePattern.contains("UNION")) { 				
			} else if (triplePattern.contains("FILTER")) {				
			} else {
				String[] elements = triplePattern.split(" ");
				
				if (elements.length != 3) {
					throw new IllegalArgumentException("triples pattern could not be passed: "+ triplePattern);
				}
				String subject = elements[0];
				String predicate = elements[1];
				String object = elements[2];
				
				Pair<String, String> domainRange = structure.getDomainAndRange(subject);
				
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

	private String extractString(String string, String string2) {
		String result = "";
		Pattern p = Pattern.compile(Pattern.quote(string) + "(.*?)" + Pattern.quote(string2));
		Matcher m = p.matcher(originalQuery);
		while (m.find()) {
			result = m.group(1);
		}
		return result;
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
