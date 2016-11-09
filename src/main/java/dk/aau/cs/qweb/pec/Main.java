package dk.aau.cs.qweb.pec;

import java.io.BufferedReader;
import java.io.FileReader;

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
		options.addOption("i", "load-instance-data", true, "path to file containing the instance data of the cube");
		options.addOption("s", "load-cube-structure", true, "path to file containing the cube structure data");
		options.addOption("p", "load-provenance-data", true, "path to file containing provenance data describing the cube");
		options.addOption("t", "database-type", true, "choose between: in-memory, jena-tdb. Default: in-memory");
		options.addOption("c", "config", true, "path to config file, this file takes precedens over other input");
				
		try {
		    CommandLine line = parser.parse( options, args );
				    
		    if (line.hasOption( "help" )) {
		    	printHelp(null,options);
		    	System.exit(0);
			} 
			    
		    if (line.hasOption("load-instance-data")) {
				Config.setInstanceDataLocation(line.getOptionValue("load-instance-data"));
			}
				    
		    if (line.hasOption("load-cube-structure")) {
				Config.setCubeStructureLocation(line.getOptionValue("load-cube-structure"));
			}
				    
		    if (line.hasOption("load-provenance-data")) {
				Config.setProvenanceDataLocation(line.getOptionValue("load-provenance-data"));
			}
		    
		    if (line.hasOption("database-type")) {
				Config.setDatabaseType(line.getOptionValue("database-type"));
			}
				    
		    if (line.hasOption("config")) {
		    	try (BufferedReader br = new BufferedReader(new FileReader(line.getOptionValue("config")))) {

					String fileLine;
					while ((fileLine = br.readLine()) != null) {
						if (fileLine.startsWith("load-instance-data")) {
							Config.setInstanceDataLocation(fileLine.split(" ")[1]);
						}
						else if (fileLine.startsWith("load-cube-structure")) {
							Config.setCubeStructureLocation(fileLine.split(" ")[1]);
						}
						else if (fileLine.startsWith("load-provenance-data")) {
							Config.setProvenanceDataLocation(fileLine.split(" ")[1]);
						}
						else if (fileLine.startsWith("database-type")) {
							Config.setDatabaseType(fileLine.split(" ")[1]);
						}
					}
				}
		    }
		    Experiment experiment = new Experiment();
		    experiment.run();
		}
		catch( ParseException exp ) {
			printHelp(exp, options);
		} 
		catch (Exception exp) {
			exp.printStackTrace();
		}
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
