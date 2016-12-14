package dk.aau.cs.qweb.pec;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Set;

import dk.aau.cs.qweb.pec.QueryEvaluation.AnalyticalQuery;
import dk.aau.cs.qweb.pec.QueryEvaluation.MaterializedFragments;

public abstract class ResultFactory {
	protected PrintStream outStream;
	protected String resultLogLocation;
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
		outStream = new PrintStream(resultLogLocation);
		outStream = System.out;
	}

	public abstract Set<String> evaluate(ProvenanceQuery analyticalQuery) throws FileNotFoundException, IOException ;

	public abstract String evaluate(MaterializedFragments materializedFragment, AnalyticalQuery analyticalQuery) ;

	public abstract String evaluate(Set<String> provenanceIdentifiers) ;
	
	@Override
	protected void finalize() {
		if (outStream != System.out)
			outStream.close();
	}
	
	protected void log(AnalyticalQuery analyticalQuery, String result, long timeInMilliseconds) {
		outStream.println("");
		outStream.println("=== "+new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()) +" ===");
		outStream.print(analyticalQuery.getQuery());
		outStream.println("Analytical Query: "+ analyticalQuery);
		outStream.println("Provenance Query: "+ provenanceQuery.getFilename());
		outStream.println(result);
		outStream.println("Budget: "+ budget);
		outStream.println("Fragment Selector: "+ selectFragmentStrategy);
		outStream.println("Cache Strategy: "+ cacheStretegy);
		outStream.println("dataset: "+ datasetPath);
		outStream.println("time: "+timeInMilliseconds+" ms");
	}

	public void setProvenanceQuery(ProvenanceQuery provenanceQuery) {
		this.provenanceQuery = provenanceQuery;
		
	}
}
