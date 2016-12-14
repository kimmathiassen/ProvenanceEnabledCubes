package dk.aau.cs.qweb.pec;

import java.util.ArrayList;
import java.util.List;

public class Config {
	static private List<String> instanceDataLocation = new ArrayList<String>();
	static private String cubeStructureLocation;
	static private String ilpLogLocation;
	static private String resultLogLocation;
	static private String greedyLogLocation;
	static private String naiveLogLocation;
	static private List<Long> budget = new ArrayList<Long>();
	private static String namespace = "http://qweb.cs.aau.dk/airbase/";
	private static String databaseType = "inMemory";
	private static List<String> provenanceQueryPath = new ArrayList<String>();
	private static List<String> analyticalQueryPath = new ArrayList<String>();
	private static List<String> fragmentselector = new ArrayList<String>();
	private static List<String> cacheSetting = new ArrayList<String>();
	private static int timeoutMinutes = 30;

	public static void addInstanceDataLocation(String optionValue) {
		instanceDataLocation.add(optionValue);
	}
	
	public static List<String> getInstanceDataLocation() {
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

	public static List<Long> getBudget() {
		return budget;
	}
	
	public static void addBudget(long optionValue) {
		budget.add(optionValue);
	}

	public static void setResultLogLocation(String resultLogLocation) {
		Config.resultLogLocation = resultLogLocation;
	}
	
	public static String getResultLogLocation() {
		return resultLogLocation;
	}

	public static void addAnalyticalQueryPath(String analyticalQueryPath) {
		Config.analyticalQueryPath.add(analyticalQueryPath);
	}
	
	public static void addAnalyticalQueryPath(List<String> analyticalQueryPaths) {
		Config.analyticalQueryPath.addAll(analyticalQueryPaths);
	}

	public static void addProvenanceQueryPath(String provenanceQueryPath) {
		Config.provenanceQueryPath.add(provenanceQueryPath);
	}
	
	public static void addProvenanceQueryPath(List<String> provenanceQueryPaths) {
		Config.provenanceQueryPath.addAll(provenanceQueryPaths);
	}
	
	public static List<String> getAnalyticalQueryPath() {
		return Config.analyticalQueryPath;
	}
	
	public static List<String> getProvenanceQueryPath(){
		return Config.provenanceQueryPath;
	}

	public static int getTimeout() {
		return timeoutMinutes;
	}

	public static String getProvenanceGraphLabel() {
		return namespace+"provenance/";
	}

	public static String getNamespace() {
		return namespace;
	}

	public static void addFragmentSelector(String string) {
		fragmentselector.add(string);
	}
	
	public static List<String> getFragmentSelectors() {
		return fragmentselector;
	}

	public static void addCacheSetting(String string) {
		cacheSetting.add(string);
	}
	
	public static List<String> getCacheSettings() {
		return cacheSetting;
	}

	public static void setTimeout(String string) {
		timeoutMinutes = Integer.getInteger(string);
	}
}
