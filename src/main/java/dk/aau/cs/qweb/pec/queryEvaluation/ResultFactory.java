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
	protected String cacheStretegy;
	protected String evaluationStrategy;
	protected String mergeStrategy;
	
	public ResultFactory(String resultLogLocation, Long budget, String selectFragmentStrategy, String cacheStretegy, String datasetPath, String evaluationStrategy, String mergeStrategy) throws FileNotFoundException {
		this.resultLogLocation = resultLogLocation;
		this.budget = budget;
		this.selectFragmentStrategy = selectFragmentStrategy;
		this.cacheStretegy = cacheStretegy;
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
		strBuilder.append("Evaluation strategy");
		strBuilder.append("\t");
		strBuilder.append("Fragment selection strategy");
		strBuilder.append("\t");
		strBuilder.append("Cache strategy");
		strBuilder.append("\t");
		strBuilder.append("Materialized fragments size");
		strBuilder.append("\t");
		strBuilder.append("Analytical q. FROM clauses size");
		strBuilder.append("\t");
		strBuilder.append("No. fragments");
		strBuilder.append("\t");
		strBuilder.append("Merge strategy");		
		strBuilder.append("\t");
		strBuilder.append("Number of results");		
		strBuilder.append("\t");
		strBuilder.append("Runtime");
		dataOutStream.println(strBuilder.toString());
	}
	
	public ResultFactory(String resultLogLocation, String dataLogLocation, Long budget, String selectFragmentStrategy, String cacheStretegy, String datasetPath, String evaluationStrategy, String mergeStrategy) throws FileNotFoundException {
		this(resultLogLocation, budget, selectFragmentStrategy, cacheStretegy, datasetPath, evaluationStrategy, mergeStrategy);
		this.dataLogLocation = dataLogLocation;
		dataOutStream = new PrintStream(new FileOutputStream(this.dataLogLocation, true));
		logHeaders();
	}

	public abstract Set<String> evaluate(ProvenanceQuery analyticalQuery) throws FileNotFoundException, IOException ;

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
	
	protected void log(AnalyticalQuery analyticalQuery, String result, long timeInMilliseconds, int numberOfResults, int run) {
		long bytesInMB = 0x1 << 20;
		run++;
		resultOutStream.println("");
		resultOutStream.println("=== "+new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()) +" ===");
		resultOutStream.println("Analytical Query file: "+ analyticalQuery);
		resultOutStream.println("From clauses: "+ analyticalQuery.getFromClause().size());
		resultOutStream.println("No. of fragments: " + analyticalQuery.getFragments().size());
		resultOutStream.println("Fragments: " + analyticalQuery.getFragments());
		resultOutStream.println("Provenance Query: "+ provenanceQuery.getFilename());
		resultOutStream.println("Run nr.: "+ run);
		
		resultOutStream.println("result: "+ result);
		resultOutStream.println("result hash: "+ result.hashCode());
		resultOutStream.println("Budget: "+ budget);
		resultOutStream.println("Fragment Selector: "+ selectFragmentStrategy);
		resultOutStream.println("Cache Strategy: "+ cacheStretegy);
		resultOutStream.println("Dataset: "+ datasetPath);
		resultOutStream.println("Evaluation strategy: "+evaluationStrategy);
		resultOutStream.println("Merge strategy: " + mergeStrategy);
		resultOutStream.println("Number of results: "+ numberOfResults);
		resultOutStream.println("time: "+timeInMilliseconds+" ms");
		resultOutStream.println("Total memory: " + (Runtime.getRuntime().totalMemory() / bytesInMB) + " MB");
		resultOutStream.println("Free memory: " + (Runtime.getRuntime().freeMemory() / bytesInMB) + " MB");		
		resultOutStream.println("Max memory: " + (Runtime.getRuntime().maxMemory() / bytesInMB) + " MB");

	}
	
	protected void logExperimentalData(AnalyticalQuery analyticalQuery, long timeInMilliseconds, int materializedFragmentsSize, int resultsSize) {
		logExperimentalData(analyticalQuery, 
				timeInMilliseconds, materializedFragmentsSize, resultsSize, 
				analyticalQuery.getFragments().size());
	}
	
	protected void logExperimentalData(AnalyticalQuery analyticalQuery, long timeInMilliseconds, int materializedFragmentsSize, 
			int resultsSize, int numberOfFragments ) {
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
		strBuilder.append(evaluationStrategy);
		strBuilder.append("\t");
		strBuilder.append(selectFragmentStrategy);
		strBuilder.append("\t");
		strBuilder.append(cacheStretegy);
		strBuilder.append("\t");
		strBuilder.append(materializedFragmentsSize);
		strBuilder.append("\t");
		strBuilder.append(analyticalQuery.getFromClause().size());
		strBuilder.append("\t");
		strBuilder.append(numberOfFragments);
		strBuilder.append("\t");
		strBuilder.append(mergeStrategy);
		strBuilder.append("\t");
		strBuilder.append(resultsSize);
		strBuilder.append("\t");
		strBuilder.append(timeInMilliseconds);
		dataOutStream.println(strBuilder.toString());
	}

	public void setProvenanceQuery(ProvenanceQuery provenanceQuery) {
		this.provenanceQuery = provenanceQuery;
	}
}
