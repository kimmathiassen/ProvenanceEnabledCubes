package dk.aau.cs.qweb.pec.queryEvaluation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;

import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.lattice.Lattice;
import dk.aau.cs.qweb.pec.types.Signature;

public class AnalyticalQuery {
	private String  originalQuery = "";
	private String  firstPart = "";
	private String  lastPart = "";
	private List<Signature> triplePatterns = new ArrayList<Signature>(); 
	private Set<String> fromClause = new HashSet<String>();
	private Map<String,String> prefixes = new HashMap<String,String>();
	private File queryFile;


	public AnalyticalQuery(File queryFile, RDFCubeStructure structure) throws IOException {
		
		this.queryFile = queryFile;
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
			} else if (triplePatternDotSplit.contains("GROUP BY")) {				
			} else {
				String predicate = "";
				
				for (String triplePatternSimiColonSplit : triplePatternDotSplit.split(";")) {
					triplePatternSimiColonSplit = triplePatternSimiColonSplit.trim();
					String[] elements = triplePatternSimiColonSplit.split(" ");
					
					if (elements.length == 3) {
						predicate = addPrefix(elements[1]);
					} else if (elements.length == 2) {
						predicate = addPrefix(elements[0]);
					}
					Pair<String, String> domainRange = structure.getDomainAndRange(predicate);
					
					Signature signature = new Signature(domainRange.getRight(), predicate, domainRange.getLeft(), null);
					triplePatterns.add(signature);
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
				return prefixes.get(":")+colonSplit[1];
			} else {
				return prefixes.get(colonSplit[0])+colonSplit[1];
			}
		} else if (string.equals("a")) {
			return "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
		}
		return string;
	}

	public List<Signature> getTriplePatterns() {
		return triplePatterns;
	}

	public void addFrom(Set<String> graphNames) {
		fromClause.addAll(graphNames);
	}

	public void addFrom(String graphLabel) {
		if (!graphLabel.isEmpty()) {
			fromClause.add(graphLabel);
		}
	}

	public String getQuery() {
		String query = firstPart+"\n";
		for (String from : fromClause) {
			query +="FROM <"+ from + ">\n";
		}
		query += lastPart;
		
		return query;
	}

	public Set<String> getFromClause() {
		return fromClause;
	}
	
	public String toString() {
		return queryFile.getName();
	}
	
	public Set<String> getFragments() {
		Set<String> fragments = new HashSet<String>();
		for (String string : fromClause) {
			if (string.contains("fragment")) {
				fragments.add(string);
			}
		}
		return fragments;
 	}

	public String getOriginalQuery() {
		return originalQuery;
	}

	public boolean containsFragmentProvenanceIdentifer(Fragment fragment) {
		Set<String> provenanceIdentifiers = fragment.getProvenanceIdentifiers();
		for (String provenanceIdentifier : provenanceIdentifiers) {
			if (fromClause.contains(provenanceIdentifier)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Removes redundant children from the list of materialized fragments.
	 * @param materializedFragments
	 * @param lattice
	 */
	public void optimizeFromClause2(MaterializedFragments materializedFragments,
			Lattice lattice) {
		List<Fragment> toRemove = new ArrayList<>();
		for (Fragment materializedFragment : materializedFragments.getFragments()) {		
			if (materializedFragments.containsAny(lattice.getAncestors(materializedFragment))) {
				toRemove.add(materializedFragment);
			}
			
		}
		for (Fragment fragment : toRemove) {
			materializedFragments.remove(fragment);
		}
		
		for (Fragment materializedFragment : materializedFragments.getFragments()) {
			fromClause.add(materializedFragments.getFragmentURL(materializedFragment));
		}
	}

	public void optimizeFromClause(MaterializedFragments materializedFragments) {
		for (Fragment materializedFragment : materializedFragments.getFragments()) {
			if (hasMatchingProvenanceIdentifiers(materializedFragment)) {
				
				if (containsAtLeastOneCommonPredicate(materializedFragment)) { //Contains at least one common element
					fromClause.removeAll(getMatchingProvenanceIdentifiers(materializedFragment));
					fromClause.add(materializedFragments.getFragmentURL(materializedFragment));
				}
			}
		}
	}

	private boolean containsAtLeastOneCommonPredicate(Fragment materializedFragment) {
		for (Signature triplePatternSignature : triplePatterns) {
			for (Signature fragmentSignature : materializedFragment.getSignatures()) {
				if (triplePatternSignature.getPredicate().equals(fragmentSignature.getPredicate())) {
					return true;
				}
			}
		}
		return false;
	}

	private Set<String> getMatchingProvenanceIdentifiers(Fragment materializedFragment) {
		Set<String> result = new HashSet<String>();
		
		for (String graph : fromClause) {
			for (String pi : materializedFragment.getProvenanceIdentifiers()) {
				if (graph.equals(pi)) {
					result.add(pi);
				}
			}
		}
		
		return result;
	}

	/**
	 * This method has a problem.
	 * @param materializedFragment
	 * @return
	 */
	private boolean hasMatchingProvenanceIdentifiers(Fragment materializedFragment) {
		return fromClause.containsAll(materializedFragment.getProvenanceIdentifiers()) ? true : false;
	}
}
