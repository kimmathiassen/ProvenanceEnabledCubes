package dk.aau.cs.qweb.pec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main {

	public static void main(String[] args) {
		// create the command line parser
		CommandLineParser parser = new DefaultParser();
			
		// create the Options
		Options options = new Options();
		options.addOption("h", "help", false, "Display this message." );
		options.addOption("c", "config", true, "path to config file, this file takes precedens over other input");
		
		try {
		    CommandLine line = parser.parse( options, args );
				    
		    if (line.hasOption( "help" )) {
		    	printHelp(null,options);
		    	System.exit(0);
			} 
				    
		    if (line.hasOption("config")) {
		    	try (BufferedReader br = new BufferedReader(new FileReader(line.getOptionValue("config")))) {

					String fileLine;
					while ((fileLine = br.readLine()) != null) {
						if (fileLine.startsWith("load-instance-data")) {
							Config.addInstanceDataLocation(fileLine.split(" ")[1]);
						}
						else if (fileLine.startsWith("load-cube-structure")) {
							Config.setCubeStructureLocation(fileLine.split(" ")[1]);
						}
						else if (fileLine.startsWith("database-type")) {
							Config.setDatabaseType(fileLine.split(" ")[1]);
						}
						else if (fileLine.startsWith("provenance-queries")) {
							Config.addProvenanceQueryPath((fileLine.split(" ")[1]));
						}
						else if (fileLine.startsWith("analytical-queries")) {
							Config.addAnalyticalQueryPath(addFolderOrFile(fileLine.split(" ")[1]));
						}
						else if (fileLine.startsWith("ilp-log-location")) {
							Config.setILPLogLocation(fileLine.split(" ")[1]);
						}
						else if (fileLine.startsWith("greedy-log-location")) {
							Config.setGreedyLogLocation(fileLine.split(" ")[1]);							
						}
						else if (fileLine.startsWith("naive-log-location")) {
							Config.setNaiveLogLocation(fileLine.split(" ")[1]);
						}
						else if (fileLine.startsWith("budget-percentage")) {
							Config.addBudgetPercentage(Long.parseLong(fileLine.split(" ")[1]));
						}
						else if (fileLine.startsWith("budget")) {
							Config.addBudget(Long.parseLong(fileLine.split(" ")[1]));
						}
						else if (fileLine.startsWith("result-log-location")) {
							Config.setResultLogLocation(fileLine.split(" ")[1]);
						} 
						else if (fileLine.startsWith("experimental-log-location")) {
							Config.setExperimentalLogLocation(fileLine.split(" ")[1]);
						}
						else if (fileLine.startsWith("fragment-selector")) {
							Config.addFragmentSelector(fileLine.split(" ")[1]);
						}
						else if (fileLine.startsWith("add-cache")) {
							Config.addCacheSetting(fileLine.split(" ")[1]);
						} 
						else if (fileLine.startsWith("timeout")) {
							Config.setTimeout(fileLine.split(" ")[1]);
						}
						else if (fileLine.startsWith("evaluation-strategy")) {
							Config.addEvaluationStrategy(fileLine.split(" ")[1]);
						}
						else if (fileLine.startsWith("lattice-merge-strategy")) {
							Config.addLatticeMergeStrategy(fileLine.split(" ")[1]);
						}
						else if (fileLine.startsWith("output-ilp-to-stdout")) {
							Config.setOutputILP2Stdout(Boolean.parseBoolean(fileLine.split(" ")[1]));
						} 
						else if (fileLine.startsWith("reduce-ratio")) {
							Config.setReduceRatio(Float.parseFloat(fileLine.split(" ")[1]));
						}
						else if (fileLine.startsWith("experimental-runs")) {
							Config.setNumberOfExperimentalRuns(Integer.parseInt(fileLine.split(" ")[1]));
						}
					}
				}
		    }
		    
		    for (String dataset : Config.getInstanceDataLocation()) {
		    	for (String cacheStrategy : Config.getCacheSettings()) {
		    		for (String mergeStrategy : Config.getLatticeMergeStrategies()) {
		    			Experiment experiment = new Experiment(dataset, cacheStrategy, mergeStrategy);
					    experiment.run();
					}
				}
			}
		}
		catch( ParseException exp ) {
			printHelp(exp, options);
		} 
		catch (Exception exp) {
			exp.printStackTrace();
		}
	}

	private static List<String> addFolderOrFile(String string) {
		File input = new File(string);
		List<String> results = new ArrayList<String>();
		if (input.isDirectory()) {
			for (File file : input.listFiles()) {
			    if (file.isFile()) {
			        results.add(file.toString());
			    }
			}
		} else {
			results.add(string);
		}
		return results;
	}

	private static void printHelp(ParseException exp, Options options) {
		String header = "";
		HelpFormatter formatter = new HelpFormatter();
		if (exp != null) {
			header = "Unexpected exception:" + exp.getMessage();
		}
		formatter.printHelp("Provenance Enabled Cubes App", header, options, null, true);
	}
}
