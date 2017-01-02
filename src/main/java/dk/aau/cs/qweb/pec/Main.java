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
		options.addOption("i", "load-instance-data", true, "path to media containing the instance data of the cube");
		options.addOption("s", "load-cube-structure", true, "path to media containing the cube structure data");
		options.addOption("p", "provenance-queries", true, "path to file containing provenance queries");
		options.addOption("a", "analytical-queries", true, "path to file containing analytical queries");
		options.addOption("t", "database-type", true, "choose between: in-memory, jena-tdb. Default: in-memory");
		options.addOption("ilog", "ilp-log-location", true, "path to the file where the ILP-based fragment selector will log its messages");
		options.addOption("glog", "greedy-log-location", true, "path to the file where the greedy fragment selector will log its messages");
		options.addOption("nlog", "naive-log-location", true, "path to the file where the naive fragment selector will log its messages");
		options.addOption("c", "config", true, "path to config file, this file takes precedens over other input");
		options.addOption("b", "budget", true, "budget in terms of number of triples that will be materialized");
		options.addOption("rlog", "result-log-location", true, "log file with exhaustive information about the run");
		options.addOption("xlog", "experimental-log-location", true, "log file with the experimental information about the run");		
		try {
		    CommandLine line = parser.parse( options, args );
				    
		    if (line.hasOption( "help" )) {
		    	printHelp(null,options);
		    	System.exit(0);
			} 
			    
		    if (line.hasOption("load-instance-data")) {
				Config.addInstanceDataLocation(line.getOptionValue("load-instance-data"));
			}
				    
		    if (line.hasOption("load-cube-structure")) {
				Config.setCubeStructureLocation(line.getOptionValue("load-cube-structure"));
			}
		    
		    if (line.hasOption("provenance-queries")) {
				Config.addProvenanceQueryPath((line.getOptionValue("provenance-queries")));
			}
		    
		    if (line.hasOption("analytical-queries")) {
				Config.addAnalyticalQueryPath(addFolderOrFile(line.getOptionValue("analytical-queries")));
			}
		    
		    if (line.hasOption("database-type")) {
				Config.setDatabaseType(line.getOptionValue("database-type"));
			}
		    
		    if (line.hasOption("ilp-log-location")) {
		    	Config.setILPLogLocation(line.getOptionValue("ilp-log-location"));
		    }
		    
		    if (line.hasOption("greedy-log-location")) {
		    	Config.setGreedyLogLocation(line.getOptionValue("greedy-log-location"));
		    }
		    
		    if (line.hasOption("naive-log-location")) {
		    	Config.setGreedyLogLocation(line.getOptionValue("naive-log-location"));
		    }
		    
		    if (line.hasOption("budget")) {
		    	Config.addBudget(Long.parseLong(line.getOptionValue("budget")));
		    }
		    
		    if (line.hasOption("result-log")) {
		    	Config.setResultLogLocation(line.getOptionValue("result-log"));
		    }
		    
		    if (line.hasOption("experimental-log")) {
		    	Config.setExperimentalLogLocation(line.getOptionValue("experimental-log"));
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
					}
				}
		    }
		    
		    for (String dataset : Config.getInstanceDataLocation()) {
		    	for (String cacheStrategy : Config.getCacheSettings()) {
		    		Experiment experiment = new Experiment(dataset,cacheStrategy);
				    experiment.run();
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
