package dk.aau.cs.qweb.pec;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Set;

import dk.aau.cs.qweb.pec.QueryEvaluation.AnalyticalQuery;
import dk.aau.cs.qweb.pec.QueryEvaluation.MaterializedFragments;

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
	
	public ResultFactory(String resultLogLocation, Long budget, String selectFragmentStrategy, String cacheStretegy, String datasetPath) throws FileNotFoundException {
		this.resultLogLocation = resultLogLocation;
		this.budget = budget;
		this.selectFragmentStrategy = selectFragmentStrategy;
		this.cacheStretegy = cacheStretegy;
		this.datasetPath = datasetPath;
		resultOutStream = new PrintStream(new FileOutputStream(resultLogLocation, true));
		resultOutStream = System.out;
	}
	
	public ResultFactory(String resultLogLocation, String dataLogLocation, Long budget, String selectFragmentStrategy, String cacheStretegy, String datasetPath) throws FileNotFoundException {
		this(resultLogLocation, budget, selectFragmentStrategy, cacheStretegy, datasetPath);
		this.dataLogLocation = dataLogLocation;
		dataOutStream = new PrintStream(new FileOutputStream(this.dataLogLocation, true));
	}

	public abstract Set<String> evaluate(ProvenanceQuery analyticalQuery) throws FileNotFoundException, IOException ;

	public abstract String evaluate(MaterializedFragments materializedFragment, AnalyticalQuery analyticalQuery) ;

	public abstract String evaluate(Set<String> provenanceIdentifiers) ;
	
	@Override
	protected void finalize() {
		if (resultOutStream != System.out)
			resultOutStream.close();
		
		if (dataOutStream != null) {
			dataOutStream.close();
		}
	}
	
	protected void log(AnalyticalQuery analyticalQuery, String result, long timeInMilliseconds) {
		resultOutStream.println("");
		resultOutStream.println("=== "+new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()) +" ===");
		resultOutStream.println("Analytical Query: "+ analyticalQuery);
		//resultOutStream.println("Analytical Query: "+ analyticalQuery.getQuery());
		resultOutStream.println("From clauses: "+ analyticalQuery.getFromClause().size());
		resultOutStream.println("Fragments: "+ analyticalQuery.getFragments());
		resultOutStream.println("Provenance Query: "+ provenanceQuery.getFilename());
		
		resultOutStream.println(result);
		resultOutStream.println("Budget: "+ budget);
		resultOutStream.println("Fragment Selector: "+ selectFragmentStrategy);
		resultOutStream.println("Cache Strategy: "+ cacheStretegy);
		resultOutStream.println("dataset: "+ datasetPath);
		resultOutStream.println("time: "+timeInMilliseconds+" ms");
	}
	
	protected void logExperimentalData(AnalyticalQuery analyticalQuery, long timeInMilliseconds) {
		if (dataOutStream == null)
			return;
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append(analyticalQuery);
		strBuilder.append("\t");
		strBuilder.append(provenanceQuery.getFilename());
		strBuilder.append("\t");
		strBuilder.append(datasetPath);
		strBuilder.append("\t");
		strBuilder.append(budget);
		strBuilder.append("\t");
		strBuilder.append(selectFragmentStrategy);
		strBuilder.append("\t");
		strBuilder.append(cacheStretegy);
		strBuilder.append("\t");
		strBuilder.append(timeInMilliseconds);
		dataOutStream.println(strBuilder.toString());
	}

	public void setProvenanceQuery(ProvenanceQuery provenanceQuery) {
		this.provenanceQuery = provenanceQuery;
		
	}
}
