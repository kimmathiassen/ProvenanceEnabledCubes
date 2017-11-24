package dk.aau.cs.qweb.pec;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Config {
	static private List<String> instanceDataLocation = new ArrayList<String>();
	static private String cubeStructureLocation;
	static private String ilpLogLocation;
	static private String resultLogLocation;
	static private String experimentalLogLocation;
	static private String greedyLogLocation;
	static private String naiveLogLocation;
	static private String offlineLogLocation = "offline.log";
	static private List<String> evaluationStrategies = new ArrayList<String>();
	static private List<Long> budget = new ArrayList<Long>();
	static private List<Long> budgetPercentages = new ArrayList<Long>();
	static private boolean optimizedQueryRewriting = true;
	static private boolean debugQuery = true;
	private static String namespace = "http://qweb.cs.aau.dk/airbase/";
	private static String databaseType = "inMemory";
	private static List<String> provenanceQueryPath = new ArrayList<String>();
	private static List<String> analyticalQueryPath = new ArrayList<String>();
	private static List<String> fragmentselector = new ArrayList<String>();
	private static List<String> cacheSetting = new ArrayList<String>();
	private static int timeoutMinutes = 30;
	private static Timestamp timestamp;
	private static List<String> latticeMergeStrategy = new ArrayList<String>();
	static private boolean outputILP2Stdout = false;
	private static float reduceRatio = 0.5f;
	private static int numberOfExperimentalRuns = 3;
	private static int maximalDistance2ObservationInSchema = 4;
	private static boolean randomExecutionOrder = true;

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
	
	public static boolean isOptimizedQueryRewriting() {
		return optimizedQueryRewriting;
	}
	
	public static void setOptimizedQueryRewriting(boolean value) {
		optimizedQueryRewriting = value;
	}
	
	public static boolean isDebugQuery() {
		return debugQuery;
	}
	
	public static void setDebugQuery(boolean value) {
		debugQuery = value;
	}

	public static void setResultLogLocation(String resultLogLocation) {
		Config.resultLogLocation = resultLogLocation;
	}
	
	public static String getResultLogLocation() {
		return resultLogLocation;
	}
	
	public static String getOfflineLogLocation() {
		return offlineLogLocation;
	}
	
	public static void setOfflineLogLocation(String location) {
		offlineLogLocation = location;
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
		timeoutMinutes = Integer.parseInt(string);
	}

	public static void setExperimentalLogLocation(String string) {
		experimentalLogLocation = string;
		
	}

	public static String getExperimentalLogLocation() {
		return experimentalLogLocation;
	}

	public static List<String> getEvaluationStrategies() {
		return evaluationStrategies;
	}
	
	public static void addEvaluationStrategy(String evaluationStrategy) {
		evaluationStrategies.add(evaluationStrategy);
	}

	public static void addBudgetPercentage(long percentage) {
		budgetPercentages.add(percentage);
	}
	
	public static List<Long> getBudgetPercentages() {
		return budgetPercentages;
	}

	public static Timestamp getTimestamp() {
		return timestamp;
	}

	public static void setTimestamp(Timestamp timestamp) {
		Config.timestamp = timestamp;
		
	}

	public static List<String> getLatticeMergeStrategies() {
		return latticeMergeStrategy ;
	}

	public static void addLatticeMergeStrategy(String mergeStrategy) {
		latticeMergeStrategy.add(mergeStrategy);
	}

	public static boolean getOutputILP2Stdout() {
		return outputILP2Stdout;
	}
	
	public static void setOutputILP2Stdout(boolean output) {
		outputILP2Stdout = output;
	}

	public static void setReduceRatio(float ratio) {
		reduceRatio = ratio;
	}

	public static float getReduceRatio() {
		return reduceRatio;
	}

	public static int getNumberOfExperimentalRuns() {
		return numberOfExperimentalRuns ;
	}
	
	public static void setNumberOfExperimentalRuns(int runs) {
		numberOfExperimentalRuns = runs;
	}
	
	public static int getMaximalDistance2ObservationInSchema() {
		return maximalDistance2ObservationInSchema;
	}
	
	public static void setMaximalDistance2ObservationInSchema(int value) {
		maximalDistance2ObservationInSchema = value;
	}

	public static boolean isRandomExecutionOrder() {
		return randomExecutionOrder;
	}
	
	public static void setRandomExecutionOrder(boolean value) {
		randomExecutionOrder = value;
	}
}
