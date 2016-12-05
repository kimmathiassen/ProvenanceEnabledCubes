package dk.aau.cs.qweb.pec;

public class Config {
	static private String instanceDataLocation;
	static private String cubeStructureLocation;
	static private String ilpLogLocation;
	static private String resultLogLocation;
	static private String greedyLogLocation;
	static private String naiveLogLocation;
	static private long budget = 10;
	private static String databaseType = "inMemory";
	private static String provenanceQueryPath;
	private static String analyticalQueryPath;

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

	public static void setDatabaseType(String optionValue) {
		databaseType = optionValue;
	}
	
	public static String getDatabaseType() {
		return databaseType;
	}

	public static String getILPLogLocation() {
		return ilpLogLocation;
	}
	
	public static void setILPLogLocation(String optionValue) {
		ilpLogLocation = optionValue;
	}

	public static String getGreedyLogLocation() {
		return greedyLogLocation;
	}
	
	public static void setGreedyLogLocation(String optionValue) {
		greedyLogLocation = optionValue;
	}
	
	public static String getNaiveLogLocation() {
		return naiveLogLocation;
	}
	
	public static void setNaiveLogLocation(String optionValue) {
		naiveLogLocation = optionValue;
	}

	public static long getBudget() {
		return budget;
	}
	
	public static void setBudget(long optionValue) {
		budget = optionValue;
	}

	public static void setResultLogLocation(String resultLogLocation) {
		Config.resultLogLocation = resultLogLocation;
	}
	
	public static String getResultLogLocation() {
		return resultLogLocation;
	}

	public static void setAnalyticalQueryPath(String analyticalQueryPath) {
		Config.analyticalQueryPath = analyticalQueryPath;
	}

	public static void setProvenanceQueryPath(String provenanceQueryPath) {
		Config.provenanceQueryPath = provenanceQueryPath;
	}
	
	public static String getAnalyticalQueryPath() {
		return Config.analyticalQueryPath;
	}
	
	public static String getProvenanceQueryPath(){
		return Config.provenanceQueryPath;
	}
}
