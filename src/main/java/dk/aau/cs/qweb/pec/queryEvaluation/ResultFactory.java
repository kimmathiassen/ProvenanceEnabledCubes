package dk.aau.cs.qweb.pec.queryEvaluation;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Set;

import dk.aau.cs.qweb.pec.Config;

public abstract class ResultFactory {
	protected PrintStream resultOutStream;
	protected PrintStream dataOutStream;
	protected String resultLogLocation;
	protected String dataLogLocation;
	protected ProvenanceQuery provenanceQuery;
	protected Long budget;
	protected String selectFragmentStrategy;
	protected String datasetPath;
	protected String cacheStrategy;
	protected String evaluationStrategy;
	protected String mergeStrategy;
	
	public ResultFactory(String resultLogLocation, Long budget, String selectFragmentStrategy, String cacheStretegy, String datasetPath, String evaluationStrategy, String mergeStrategy) throws FileNotFoundException {
		this.resultLogLocation = resultLogLocation;
		this.budget = budget;
		this.selectFragmentStrategy = selectFragmentStrategy;
		this.cacheStrategy = cacheStretegy;
		this.datasetPath = datasetPath;
		this.evaluationStrategy = evaluationStrategy;
		this.mergeStrategy = mergeStrategy;
		resultOutStream = new PrintStream(new FileOutputStream(resultLogLocation, true));
		resultOutStream = System.out;
	}
	
	protected void logHeaders() {
		if (dataOutStream == null)
			return;
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("Timestamp");
		strBuilder.append("\t");
		strBuilder.append("Analytical query");
		strBuilder.append("\t");
		strBuilder.append("Provenance query");
		strBuilder.append("\t");
		strBuilder.append("Dataset");
		strBuilder.append("\t");
		strBuilder.append("Budget");
		strBuilder.append("\t");
		strBuilder.append("Budget-jena (%)");
		strBuilder.append("\t");
		strBuilder.append("Evaluation strategy");
		strBuilder.append("\t");
		strBuilder.append("Fragment selection strategy");
		strBuilder.append("\t");
		strBuilder.append("Cache strategy");
		strBuilder.append("\t");
		strBuilder.append("Materialized fragments size");
		strBuilder.append("\t");
		strBuilder.append("Materialized data size");
		strBuilder.append("\t");
		strBuilder.append("Analytical q. FROM clauses size");
		strBuilder.append("\t");
		strBuilder.append("No. fragments");
		strBuilder.append("\t");
		strBuilder.append("Merge strategy");		
		strBuilder.append("\t");
		strBuilder.append("Number of results");		
		strBuilder.append("\t");
		strBuilder.append("Run #");
		strBuilder.append("\t");
		strBuilder.append("Cache build time");
		strBuilder.append("\t");
		strBuilder.append("Materialization time (ms)");
		strBuilder.append("\t");
		strBuilder.append("Construct time (ms)");
		strBuilder.append("\t");
		strBuilder.append("Query rewriting (ms)");
		strBuilder.append("\t");
		strBuilder.append("Runtime analytical (ms)");
		strBuilder.append("\t");
		strBuilder.append("Runtime provenance (ms)");
		dataOutStream.println(strBuilder.toString());
	}
	
	public ResultFactory(String resultLogLocation, String dataLogLocation, Long budget, String selectFragmentStrategy, String cacheStretegy, String datasetPath, String evaluationStrategy, String mergeStrategy) throws FileNotFoundException {
		this(resultLogLocation, budget, selectFragmentStrategy, cacheStretegy, datasetPath, evaluationStrategy, mergeStrategy);
		this.dataLogLocation = dataLogLocation;
		dataOutStream = new PrintStream(new FileOutputStream(this.dataLogLocation, true));
		logHeaders();
	}

	public abstract Set<String> evaluate(ProvenanceQuery provenanceQuery) throws FileNotFoundException, IOException ;

	public abstract String evaluate(MaterializedFragments materializedFragment, AnalyticalQuery analyticalQuery,int run) ;

	//public abstract String evaluate(Set<String> provenanceIdentifiers) ;
	
	@Override
	protected void finalize() {
		if (resultOutStream != System.out)
			resultOutStream.close();
		
		if (dataOutStream != null) {
			dataOutStream.close();
		}
	}
	
	protected void log(AnalyticalQuery analyticalQuery, String result, long timeInMilliseconds, int numberOfResults, int run, 
			long materializedDataSize, long materializedFragmentsSize) {
		long bytesInMB = 0x1 << 20;
		run++;
		resultOutStream.println("");
		resultOutStream.println("=== "+new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()) +" ===");
		resultOutStream.println("Analytical Query file: "+ analyticalQuery);
		resultOutStream.println("From clauses: "+ analyticalQuery.getFromClause().size());
		resultOutStream.println("No. of fragments: " + analyticalQuery.getFragments().size());
		resultOutStream.println("Fragments: " + analyticalQuery.getFragments());
		resultOutStream.println("Materialized data size: " + materializedDataSize + " quadruples");
		resultOutStream.println("Materialization time: " + analyticalQuery.getMaterializationTime() + " ms");
		resultOutStream.println("Construct query execution time: " + analyticalQuery.getConstructQueryTime() + " ms");
		resultOutStream.println("Materialized data size coming from cached fragments: " + materializedFragmentsSize + " quadruples");
		resultOutStream.println("Provenance Query: "+ provenanceQuery.getFilename());
		resultOutStream.println("Run nr.: "+ run);
		
		resultOutStream.println("result: "+ result);
		resultOutStream.println("result hash: "+ result.hashCode());
		resultOutStream.println("Budget: "+ budget);
		
		if (cacheStrategy.equals("tepid") && Config.getBudgetPercentages().size() > 0) {
			resultOutStream.println("Budget-jena %: "+ Config.getBudgetPercentages().iterator().next());
		} else if (cacheStrategy.equals("cold")) {
			resultOutStream.println("Budget-jena %: "+ 0);
		} else {
			resultOutStream.println("Budget-jena %: default");
		}
		
		resultOutStream.println("Fragment Selector: "+ selectFragmentStrategy);
		resultOutStream.println("Cache Strategy: "+ cacheStrategy);
		resultOutStream.println("Dataset: "+ datasetPath);
		resultOutStream.println("Evaluation strategy: "+evaluationStrategy);
		resultOutStream.println("Merge strategy: " + mergeStrategy);
		resultOutStream.println("Reduce ratio: " + Config.getReduceRatio());
		resultOutStream.println("Number of results: " + numberOfResults);
		resultOutStream.println("Cache building time: " + analyticalQuery.getCacheBuildTime() + " ms");
		resultOutStream.println("Query rewriting time: " + analyticalQuery.getQueryRewritingTime()  + " ms");
		resultOutStream.println("Time analytical query: "+ timeInMilliseconds + " ms");
		resultOutStream.println("Time provenance query: "+provenanceQuery.getRuntime()+" ms");
		resultOutStream.println("Total memory: " + (Runtime.getRuntime().totalMemory() / bytesInMB) + " MB");
		resultOutStream.println("Free memory: " + (Runtime.getRuntime().freeMemory() / bytesInMB) + " MB");		
		resultOutStream.println("Max memory: " + (Runtime.getRuntime().maxMemory() / bytesInMB) + " MB");

	}
	
	protected void logExperimentalData(AnalyticalQuery analyticalQuery, long timeAnalytical,
			long materializedFragmentsSize, int resultsSize, int run, long materializedDataSize) {
		logExperimentalData(analyticalQuery, timeAnalytical, materializedFragmentsSize, resultsSize, analyticalQuery.getFragments().size(), run, materializedDataSize);
	}
	
	protected void logExperimentalData(AnalyticalQuery analyticalQuery, long timeAnalytical, long materializedFragmentsSize, 
			int resultsSize, int numberOfFragments, int run, long materializedDataSize ) {
		if (dataOutStream == null)
			return;
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append(Config.getTimestamp());
		strBuilder.append("\t");
		strBuilder.append(analyticalQuery);
		strBuilder.append("\t");
		strBuilder.append(provenanceQuery.getFilename());
		strBuilder.append("\t");
		strBuilder.append(datasetPath);
		strBuilder.append("\t");
		strBuilder.append(budget);
		strBuilder.append("\t");
		
		if (cacheStrategy.equals("tepid") && Config.getBudgetPercentages().size() > 0) {
			strBuilder.append(Config.getBudgetPercentages().iterator().next());
		} else if (cacheStrategy.equals("cold")) {
			strBuilder.append(0);
		} else {
			strBuilder.append("default");
		}
		strBuilder.append("\t");
		strBuilder.append(evaluationStrategy);
		strBuilder.append("\t");
		strBuilder.append(selectFragmentStrategy);
		strBuilder.append("\t");
		strBuilder.append(cacheStrategy);
		strBuilder.append("\t");
		strBuilder.append(materializedFragmentsSize);
		strBuilder.append("\t");
		strBuilder.append(materializedDataSize);
		strBuilder.append("\t");
		strBuilder.append(analyticalQuery.getFromClause().size());
		strBuilder.append("\t");
		strBuilder.append(numberOfFragments);
		strBuilder.append("\t");
		strBuilder.append(mergeStrategy); 
		if (!mergeStrategy.equals("noMerge")) {
			strBuilder.append(" (r" + Config.getReduceRatio()+")");
		}
		strBuilder.append("\t");
		strBuilder.append(resultsSize);
		strBuilder.append("\t");
		strBuilder.append(run);
		strBuilder.append("\t");
		strBuilder.append(analyticalQuery.getCacheBuildTime());		
		strBuilder.append("\t");	
		strBuilder.append(analyticalQuery.getMaterializationTime());
		strBuilder.append("\t");
		strBuilder.append(analyticalQuery.getConstructQueryTime());
		strBuilder.append("\t");
		strBuilder.append(analyticalQuery.getQueryRewritingTime());
		strBuilder.append("\t");
		strBuilder.append(timeAnalytical);
		strBuilder.append("\t");
		strBuilder.append(provenanceQuery.getRuntime());
		dataOutStream.println(strBuilder.toString());
	}

	public void setProvenanceQuery(ProvenanceQuery provenanceQuery) {
		this.provenanceQuery = provenanceQuery;
	}
}
