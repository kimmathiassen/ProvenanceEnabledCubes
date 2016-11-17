package dk.aau.cs.qweb.pec;

public class Config {
	static private String instanceDataLocation;
	static private String cubeStructureLocation;
	static private String provenanceDataLocation;
	static private String ilpLogLocation;
	static private String greedyLogLocation;
	private static String databaseType = "inMemory";

	public static void setInstanceDataLocation(String optionValue) {
		instanceDataLocation = optionValue;
	}
	
	public static String getInstanceDataLocation() {
		return instanceDataLocation;
	}

	public static void setCubeStructureLocation(String optionValue) {
		cubeStructureLocation = optionValue;
	}
	
	public static String getCubeStructureLocation() {
		return cubeStructureLocation; 
	}

	public static void setProvenanceDataLocation(String optionValue) {
		provenanceDataLocation = optionValue;
	}
	
	public static String getProvenanceDataLocation() {
		return provenanceDataLocation;
	}

	public static void setDatabaseType(String optionValue) {
		databaseType = optionValue;
	}
	
	public static String getDatabaseType() {
		return databaseType;
	}

	public static String getILPLogFile() {
		return ilpLogLocation;
	}
	
	public static void setILPLogFile(String optionValue) {
		ilpLogLocation = optionValue;
	}

	public static String getGreedyLogFile() {
		return greedyLogLocation;
	}
	
	public static void setGreedyLogFile(String optionValue) {
		greedyLogLocation = optionValue;
	}
}
