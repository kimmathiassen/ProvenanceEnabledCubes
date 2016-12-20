package dk.aau.cs.qweb.pec.data;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;

import dk.aau.cs.qweb.pec.lattice.QB4OLAP.CubeStructure;
import dk.aau.cs.qweb.pec.types.Signature;


public class RDFCubeStructure {
	
	private Set<String> informationRelations;
	
	private Set<String> metadataRelations;
	
	private Set<String> measures;
	
	private MultiValuedMap<String, String> levelAttributes;
	
	private Map<String, DimensionHierarchy> dimensions;
	
	private Map<String, String> domains;
	
	private Map<String, String> ranges;
	
	// We define some relations in our schema
	public static final String typeRelation = "rdf:type";
		
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
		informationRelations = new LinkedHashSet<>();
		metadataRelations = new LinkedHashSet<>();
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
	 * @throws ParseException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static RDFCubeStructure build(String fileName) throws ParseException, FileNotFoundException, IOException {
		
		String ext = FilenameUtils.getExtension(fileName);
		if (ext.equals("tsv")) {
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
		} else if (ext.equals("ttl")) {
			CubeStructure structure = CubeStructure.getInstance(fileName);
			RDFCubeStructure cube = new RDFCubeStructure();
			//TODO not done
			return null;
		} else {
			System.out.println(fileName);
			throw new IllegalArgumentException("File extension "+ ext+" is not known");
		}
		
	}
	
	public static RDFCubeStructure build(Iterable<String[]> schemaTriples) throws ParseException {
		RDFCubeStructure cube = new RDFCubeStructure();
		cube.parseTSVRows(schemaTriples);
		return cube;
	}
	
	public Set<String> getDimensions() {
		return new LinkedHashSet<>(dimensions.keySet());
	}
	
	private void parseTriple(String[] triple, int line, Map<String, String> levels2Dimensions) throws ParseException {
		String subject = triple[0];
		String relation = triple[1];
		String object = triple[2];
		switch (relation) {
		case typeRelation :
			switch(object){
			case measureType :
				measures.add(subject);					
				break;
			case factualRelation :
				informationRelations.add(subject);
				break;
			case cubeRelation :
				metadataRelations.add(subject);
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
				throw new ParseException("The relation " + subject 
						+ " has not been associated to any dimension", 0); 
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

	/**
	 * It builds the schema object from a list of RDF triplets
	 * in the form of string arrays.
	 * @param allRows
	 * @throws ParseException 
	 */
	private void parseTSVRows(Iterable<String[]> allRows) throws ParseException {
		Map<String, String> levels2Dimensions = new HashMap<>();
		int line = 0;
		for (String[] row : allRows) {
			++line;
			parseTriple(row, line, levels2Dimensions);
		}
	}
	
	/**
	 * If the schema has a dimension A: a' -> a'' -> a''', getDimensionRelationsAtLevel("A", 1)
	 * will return a set containing a''. 
	 * @param dimension
	 * @param level
	 * @return
	 */
	public Set<String> getDimensionRelationsAtLevel(String dimension, int level) {
		DimensionHierarchy hierarchy = dimensions.get(dimension);
		if (hierarchy != null) {
			return hierarchy.getRelationsAtLevel(level);
		} else {
			return Collections.emptySet();
		}
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Factual relations: " + informationRelations + "\n");
		builder.append("Cube relations: " + metadataRelations + "\n");
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

	public Pair<String, String> getDomainAndRange(String relation) {
		return new MutablePair<>(domains.get(relation), ranges.get(relation));
	}

	public boolean isMetadataProperty(String relation) {
		return metadataRelations.contains(relation);
	}
	
	public boolean isFactualProperty(String relation) {
		return informationRelations.contains(relation);
	}
	
	public boolean isMeasureProperty(String relation) {
		return measures.contains(relation);
	}

	public Set<String> getAttributes(String relationLevel) {
		return new LinkedHashSet<>(levelAttributes.get(relationLevel));
	}
	
}
