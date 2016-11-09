package dk.aau.cs.qweb.pec.data;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;

import dk.aau.cs.qweb.pec.types.Signature;


public class RDFCubeStructure {
	
	private Set<String> factualRelations;
	
	private Set<String> cubeRelations;
	
	private Set<String> measures;
	
	private MultiValuedMap<String, String> levelAttributes;
	
	private Map<String, DimensionHierarchy> dimensions;
	
	private Map<String, String> domains;
	
	private Map<String, String> ranges;
	
	// We define some relations in our schema
	private static final String typeRelation = "rdf:type";
		
	private static final String attributeRelation = ":hasAttribute";
	
	private static final String levelRelation = ":hasLevel";
	
	private static final String rollupRelation = "skos:broader";
	
	private static final String domainRelation = "rdfs:domain";
	
	private static final String rangeRelation = "rdfs:range";
	
	// And three types
	private static final String measureType = "Measure";
	
	private static final String factualRelation = "FactualRelation";
	
	private static final String cubeRelation = "CubeRelation";
	
	
	private RDFCubeStructure() {
		factualRelations = new LinkedHashSet<>();
		cubeRelations = new LinkedHashSet<>();
		measures = new LinkedHashSet<>();
		levelAttributes = new HashSetValuedHashMap<>();
		dimensions = new HashMap<>();
		domains = new HashMap<>();
		ranges = new HashMap<>();
	}
		
	/**
	 * Builds an RDFCubeSchema object from the definitions contained in the file
	 * sent as argument.
	 * @param fileName
	 * @return
	 */
	public static RDFCubeStructure build(String fileName) {
		RDFCubeStructure cube = new RDFCubeStructure();
		TsvParserSettings settings = new TsvParserSettings();
	    settings.getFormat().setLineSeparator("\n");

	    // creates a TSV parser
	    TsvParser parser = new TsvParser(settings);

	    // parses all rows in one go.
	    List<String[]> allRows = null;
	    try {
			allRows = parser.parseAll(new FileReader(fileName));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.err.println("Parsing the schema failed!");
			e.printStackTrace();
		}
	    
	    cube.parseTSVRows(allRows);
	    return cube;
	}

	/**
	 * It builds the schema object from a list of RDF triplets
	 * in the form of string arrays.
	 * @param allRows
	 */
	private void parseTSVRows(List<String[]> allRows) {
		Map<String, String> levels2Dimensions = new HashMap<>();
		int line = 0;
		for (String[] row : allRows) {
			++line;
			String subject = row[0];
			String relation = row[1];
			String object = row[2];
			switch (relation) {
			case typeRelation :
				switch(object){
				case measureType :
					measures.add(subject);					
					break;
				case factualRelation :
					factualRelations.add(subject);
					break;
				case cubeRelation :
					cubeRelations.add(subject);
					break;
				}				
				break;
			case levelRelation :
				// Triples of the form <dimension hasLevel relation>
				DimensionHierarchy hierarchy = dimensions.get(subject);
				
				if (hierarchy == null) {
					hierarchy = new DimensionHierarchy(subject);
					dimensions.put(subject, hierarchy);
				}
				hierarchy.addRelation(object);
				levels2Dimensions.put(object, subject);
				break;
			case attributeRelation :
				// Triples of the form <level hasAttribute attribute>
				levelAttributes.put(subject, object);
				break;
			case rollupRelation :
				// Triples of the form <level skos:broader anotherLevel>
				// Get the dimension
				String dimension = levels2Dimensions.get(subject);
				DimensionHierarchy dimensionHierarchy = dimensions.get(dimension);
				if (dimensionHierarchy != null) {
					dimensionHierarchy.addRollup(subject, object);
				} else {
					System.err.println("Line " + line + 
							": The relation " + subject 
							+ " has not been associated to any dimension"); 
				}
				break;
			case domainRelation : 
				domains.put(subject, object);
				break;
				
			case rangeRelation :
				ranges.put(subject, object);
				break;
			}
		
		}
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Factual relations: " + factualRelations + "\n");
		builder.append("Cube relations: " + cubeRelations + "\n");
		builder.append("Measures: " + measures + "\n");
		builder.append("Domains: " + domains + "\n");
		builder.append("Ranges: " + ranges + "\n");
		builder.append("Level attributes: " + levelAttributes + "\n");
		builder.append("Dimensions\n");
		
		for (String dimension : dimensions.keySet()) {
			builder.append(dimensions.get(dimension) + "\n");
		}
		
		return builder.toString();
	}

//	public static void main(String[] args) {
//		RDFCubeStructure cubeSchema = RDFCubeStructure.build(args[0]);
//		System.out.println(cubeSchema);
//
//	}

	public Pair<String, String> getDomainAndRange(String relation) {
		return new MutablePair<>(domains.get(relation), ranges.get(relation));
	}
	
	public boolean containsMeasureTriples(Collection<Signature<String, String, String, String>> signatures) {
		for (Signature<String, String, String, String> signature : signatures) {
			if (signature.getSecond() != null && measures.contains(signature.getSecond())) {
				return true;
			}
		}
		return false;
	}

	public boolean isMetadataRelation(String relation) {
		return cubeRelations.contains(relation);
	}
	
	public boolean isFactualRelation(String relation) {
		return factualRelation.contains(relation);
	}

}
