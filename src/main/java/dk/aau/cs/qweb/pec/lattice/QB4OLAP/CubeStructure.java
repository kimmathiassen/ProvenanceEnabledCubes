package dk.aau.cs.qweb.pec.lattice.QB4OLAP;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dk.aau.cs.qweb.pec.Config;

public class CubeStructure {
	private Map<String,String> prefix = new HashMap<String,String>();
	private String dataStructureDefinition;
	private List<Measure> meassures = new ArrayList<Measure>();
	private List<Dimension> dimensions = new ArrayList<Dimension>();
	private List<Hierarchy> hierarchies = new ArrayList<Hierarchy>();
	private List<HierarchyStep> hierarchySteps = new ArrayList<HierarchyStep>();
	private List<Level> levels = new ArrayList<Level>();
	private List<String> rollupProperties = new ArrayList<String>();
	private List<Attribute> attributes = new ArrayList<Attribute>();
	private String lineBuffer = "";
	private String title ="";
	
	private static CubeStructure instance = null;
	private CubeStructure(String path) throws FileNotFoundException, IOException {
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
		    for(String line; (line = br.readLine()) != null; ) {
		    	lineBuffer += line+" ";
		        if (endOfStatement(line)) {
					parseTriplesStatement(lineBuffer);
					lineBuffer = "";
				}
		    }
		}
	}
	
	private void parseTriplesStatement(String line) {
		line = line.trim(); // ensure that spaces between elements are not trimmed
		line = line.substring(0, line.length()-1);
		if (line.startsWith("@prefix")) {
			addPrefix(line);
		} else if (line.isEmpty()) {
			//line is empty
		} else if (line.startsWith("#")) {
			//Comment
		} else if (line.contains("[") && line.contains("]")) {
			parseLineWithSquareBrackets(line);
		} else if (line.startsWith("_:")) {
			parseLineWithBlankNode(line);
		} else if (prefix.containsKey(line.split(":")[0])) {
			parseLine(line);
		} else {
			throw new IllegalStateException("the Cube Structure file contains an line that cannot be passed: "+ line);
		}
	}
	
	private void parseLine(String line) {
		String[] colonSplit = line.split(";");
		
		String subject = "";
		String predicate = "";
		String object = "";
		String type = "";
		
		for (String colonSplitLine : colonSplit) {
			for (String commaSplitLine : colonSplitLine.split(",")) {
				List<String> elements = new ArrayList<String>();
				Pattern regex = Pattern.compile("[^\\s\"']+|\"[^\"]*\"|'[^']*'");
				Matcher regexMatcher = regex.matcher(commaSplitLine);
				while (regexMatcher.find()) {
				    elements.add(transformPrefixIntoFullURL(regexMatcher.group()));
				} 
				
				if (elements.size() == 3) {
					subject = elements.get(0);
					predicate = elements.get(1);
					object = elements.get(2);
				} else if (elements.size() == 2) {
					predicate = elements.get(0);
					object = elements.get(1);
				} else if (elements.size() == 1) {
					object = elements.get(0);
				} else {
					String errorString = "";
					for (String string : elements) {
						errorString += string + " ";
					}
					throw new IllegalArgumentException("unexpected number of elements in triple: "+ errorString);
				}
					
				if (predicate.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") || predicate.equals("a")) {
					type = elements.get(2);
				}
				addTripleToStructure(subject,predicate,object,type);
			}
		}
	}

	public String transformPrefixIntoFullURL(String colonSplit) {
		String result = "";
		colonSplit = colonSplit.trim();
		
		if (colonSplit.contains("_:")) {
			result = colonSplit + " ";
		} else if (colonSplit.contains(":")) {
			String[] element = colonSplit.split(":");
			result += getPrefix(element[0]) + element[1] + " ";
			
		} else {
			result = colonSplit + " ";
		}
		result = result.substring(0, result.length()-1);
		return result;
	}
	
	public String getPrefix(String string) {
		return prefix.get(string);
	}
	
	private void addTripleToStructure(String subject, String predicate, String object, String type) {
		if (type.equals("http://purl.org/linked-data/cube#DataStructureDefinition")) {
			if (predicate.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") || predicate.equals("a")) {
				dataStructureDefinition = subject;
			}
		} else if (type.equals("http://purl.org/linked-data/cube#DataSet")) {
			if (predicate.equals("http://purl.org/dc/terms/title")) {
				title = object;
			}
		} else if (type.equals("http://purl.org/linked-data/cube#MeasureProperty")) {
			boolean exists = false;
			for (Measure measure : meassures) {
				if (measure.equals(subject)) {
					if (predicate.equals("http://www.w3.org/2000/01/rdf-schema#label")) {
						measure.setLabel(object);
					} else if (predicate.equals("http://www.w3.org/2000/01/rdf-schema#range")) {
						measure.setRange(object);
					}
					exists = true;
				}
			}
			if (!exists) {
				Measure measure = new Measure(subject);
				meassures.add(measure);
			}
		} else if (type.equals("http://purl.org/linked-data/cube#DimensionProperty")) {
			boolean exists = false;
			for (Dimension dimension : dimensions) {
				if (dimension.equals(subject)) {
					if (predicate.equals("http://www.w3.org/2000/01/rdf-schema#label")) {
						dimension.setLabel(object);
					} else if (predicate.equals("http://purl.org/qb4olap/cubes#hasHierarchy")) {
						dimension.addHierarchy(object);
					}
					exists = true;
				}
			}
			if (!exists) {
				Dimension dimension = new Dimension(subject);
				dimensions.add(dimension);
			}
		} else if (type.equals("http://purl.org/qb4olap/cubes#Hierarchy")) {
			boolean exists = false;
			for (Hierarchy hierarchy : hierarchies) {
				if (hierarchy.equals(subject)) {
					if (predicate.equals("http://www.w3.org/2000/01/rdf-schema#label")) {
						hierarchy.setLabel(object);
					} else if (predicate.equals("http://purl.org/qb4olap/cubes#inDimension")) {
						hierarchy.addInDimension(object);
					} else if (predicate.equals("http://purl.org/qb4olap/cubes#hasLevel")) {
						hierarchy.addLevel(object);
					}
					exists = true;
				}
			}
			if (!exists) {
				Hierarchy hierarchy = new Hierarchy(subject);
				hierarchies.add(hierarchy);
			}
		} else if (type.equals("http://purl.org/qb4olap/cubes#HierarchyStep")) {
			boolean exists = false;
			for (HierarchyStep hierarchyStep : hierarchySteps) {
				if (hierarchyStep.getSubject().equals(subject)) {
					if (predicate.equals("http://purl.org/qb4olap/cubes#childLevel")) {
						hierarchyStep.setChildLevel(object);
					} else if (predicate.equals("http://purl.org/qb4olap/cubes#inHierarchy")) {
						hierarchyStep.setHierarchy(object);
					} else if (predicate.equals("http://purl.org/qb4olap/cubes#parentLevel")) {
						hierarchyStep.setParentLevel(object);
					} else if (predicate.equals("http://purl.org/qb4olap/cubes#pcCardinality")) {
						hierarchyStep.setCardinality(object);
					} else if (predicate.equals("http://purl.org/qb4olap/cubes#rollup")) {
						hierarchyStep.setRollup(object);
					}
					exists = true;
				}
			}
			if (!exists) {
				HierarchyStep hierarchyStep = new HierarchyStep(subject);
				hierarchySteps.add(hierarchyStep);
			}
		} else if (type.equals("http://purl.org/qb4olap/cubes#RollupProperty")) {
			boolean exists = false;
			for (String rollupProperty : rollupProperties) {
				if (rollupProperty.equals(subject)) {
					exists = true;
				}
			}
			if (!exists) {
				rollupProperties.add(subject);
			}
		} else if (type.equals("http://purl.org/qb4olap/cubes#LevelProperty")) {
			boolean exists = false;
			for (Level level : levels) {
				if (level.equals(subject)) {
					if (predicate.equals("http://www.w3.org/2000/01/rdf-schema#label")) {
						level.setLabel(object);
					} else if (predicate.equals("http://purl.org/qb4olap/cubes#hasAttribute")) {
						level.addAttribute(object);
					}
					exists = true;
				}
			}
			if (!exists) {
				Level hierarchyStep = new Level(subject);
				levels.add(hierarchyStep);
			}
		} else if (type.equals("http://purl.org/qb4olap/cubes#LevelAttribute")) {
			boolean exists = false;
			for (Attribute attribute : attributes) {
				if (attribute.equals(subject)) {
					if (predicate.equals("http://www.w3.org/2000/01/rdf-schema#label")) {
						attribute.setLabel(object);
					} else if (predicate.equals("http://www.w3.org/2000/01/rdf-schema#range")) {
						attribute.setRange(object);
					}
					exists = true;
				}
			}
			
			if (!exists) {
				Attribute attribute = new Attribute(subject);
				attributes.add(attribute);
			}
		} else {
			throw new IllegalArgumentException("Not able to parse the line: "+ subject + " " +predicate+" " +object);
		}
	}
	private void parseLineWithBlankNode(String line) {
		parseLine(line);
	}
	
	private void parseLineWithSquareBrackets(String line) {
		String subject = line.split(" ")[0];
		String result = "";
		for (String split : line.split(";")) {
			if (!split.contains("[") && !split.contains("]")) {
				result += split + " ; ";
			}
		}
		
		result = replaceLastSimicolonWithDot(result);
		
		Pattern regex2 = Pattern.compile("(qb:component.*?\\[.*?\\].*?;)");
		Matcher regexMatcher2 = regex2.matcher(line);
		int index = 1;
		while (regexMatcher2.find()) {
			String blankNode = Config.getNamespace()+"blankNode/"+index;
			String match = regexMatcher2.group();
			String[] stringWithBlankNode = match.split("\\[");
			result += " " + subject +" "+ stringWithBlankNode[0] + blankNode + " . ";
			
			for (String string : stringWithBlankNode[1].split(";")) {
				string = string.trim();
				String[] element = string.split(" ");
				result += blankNode +" "+ element[0]+ " "+ element[1] + " . ";
			}
			index++;
		} 
		
		for (String tripleStatement : result.split(".")) {
			parseLine(tripleStatement);
		}
	}
	
	private String replaceLastSimicolonWithDot(String result) {
		result = result.trim();
		result = result.substring(0, result.length()-1);
		return result +".";
	}
	
	private void addPrefix(String line) {
		String[] split = line.split("\\ +");
		prefix.put(split[1].replaceAll(":", ""), split[2].substring(1, split[2].length()-1));
	}
	
	private boolean endOfStatement(String line) {
		return line.matches(".+\\.\\s*") ? true : false;
	}
	
	public static CubeStructure getInstance(String path) throws FileNotFoundException, IOException {
		if(instance == null) {
			instance = new CubeStructure(path);
		}
		return instance;
	}

	public String getTitle() {
		return title;
	}
	
	public String getDataStructureDefinition() {
		return dataStructureDefinition;
	}
	public List<Level> getLevels() {
		return levels;
	}
	
	public List<HierarchyStep> getHierarchyStepByParentLevel(String level) {
		List<HierarchyStep> hs = new ArrayList<HierarchyStep>();
		for (HierarchyStep step : hierarchySteps) {
			if (step.getParentLevel().equals(level)) {
				hs.add(step);
			}
		}
		return hs;
	}
	
	public HierarchyStep getHierarchyStepByChildLevel(String level) {
		for (HierarchyStep step : hierarchySteps) {
			if (step.getChildLevel().equals(level)) {
				return step;
			}
		}
		throw new IllegalArgumentException("no hierarchy step found for level: "+level);
	}
}