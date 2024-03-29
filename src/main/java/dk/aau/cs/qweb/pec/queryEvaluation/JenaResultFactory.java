package dk.aau.cs.qweb.pec.queryEvaluation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryCancelledException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb.TDBFactory;

import dk.aau.cs.qweb.pec.Config;

public class JenaResultFactory extends ResultFactory {
	
	public static final long INTERRUPTED = -1l;
	

	public JenaResultFactory(String resultLogLocation, Long budget, String selectFragmentStrategy, String cacheStrategy, String datasetPath, String evaluationStrategy, String mergeStrategy) throws FileNotFoundException {
		super(resultLogLocation, budget, selectFragmentStrategy,cacheStrategy,datasetPath, evaluationStrategy, mergeStrategy);
	}
	
	public JenaResultFactory(String resultLogLocation, String dataLogLocation, Long budget, String selectFragmentStrategy, String cacheStrategy, String datasetPath, String evaluationStrategy, String mergeStrategy) throws FileNotFoundException {
		super(resultLogLocation, dataLogLocation, budget, selectFragmentStrategy, cacheStrategy, datasetPath, evaluationStrategy, mergeStrategy);
	}


	@Override
	//Execute provenance queries
	public Set<String> evaluate(ProvenanceQuery provenanceQuery) throws FileNotFoundException, IOException {
		Dataset dataset = TDBFactory.createDataset(datasetPath) ;
		Set<String> provenanceIdentifiers = new HashSet<String>();
		dataset.begin(ReadWrite.READ) ;
			
		if (provenanceQuery.isQuery()) {
			Query newQuery = QueryFactory.create(provenanceQuery.getQuery());
			newQuery.addGraphURI(Config.getProvenanceGraphLabel());
			
			QueryExecution qexec = QueryExecutionFactory.create(newQuery, dataset) ;
			qexec.setTimeout(Config.getTimeout(), TimeUnit.MINUTES);
			long timeStart = System.currentTimeMillis();
			ResultSet results = qexec.execSelect();
			if (provenanceQuery.getRuntime() == -1) {
				// Only if it has not been updated it
				provenanceQuery.setRuntime(System.currentTimeMillis() - timeStart);
			}
			while (results.hasNext()) {
				QuerySolution qs = results.nextSolution();
				String var = qs.varNames().next();
				provenanceIdentifiers.add(qs.get(var).toString());
			}
		} else {
			provenanceIdentifiers.addAll(provenanceQuery.getProvenanceIdentifiers());
		}
		dataset.end();
		
		return provenanceIdentifiers;
	}

	@Override
	//Execute analytical query
	public String evaluate(MaterializedFragments materializedfragments, 
			AnalyticalQuery analyticalQuery, int run) {
		String result = "";
		Dataset dataset = TDBFactory.createDataset(datasetPath) ;
		dataset.begin(ReadWrite.READ) ;
		
		try {
			if (evaluationStrategy.equals("fullMaterialization")) {
				result = fullImprovedMaterializationEvaluation(materializedfragments, analyticalQuery, dataset, run);
			} else {
				result = basicEvaluation(materializedfragments, analyticalQuery, dataset, run);
			}
		} catch (QueryCancelledException e) {
			logExperimentalData(analyticalQuery, INTERRUPTED, 0, 0, run, 0l);
			System.err.println(e.getStackTrace());
		} finally {
			dataset.end();
		}
		return result;
	}

	private String fullImprovedMaterializationEvaluation(MaterializedFragments materializedfragments,
			AnalyticalQuery analyticalQuery, Dataset dataset, int run) {
		Dataset inMemoryDataset = DatasetFactory.create();
		String result;
		long materializedFragmentsSize = 0;
		long timea = System.currentTimeMillis();
		Set<String> fromClauses = analyticalQuery.getFromClause();
		//Query materializationQuery = QueryFactory.create("CONSTRUCT {?s ?p ?o} WHERE {?s ?p ?o}");
		
		long timeMaterialized = System.currentTimeMillis();
		for (String graph : fromClauses) {	
			Model model = null; 
			if (graph.contains("fragment")) {
				model = materializedfragments.getMaterializedModel(graph);
				materializedFragmentsSize += model.size();
			} else {
				model = dataset.getNamedModel(graph);
			}
			
			inMemoryDataset.getDefaultModel().add(model);
		}
		
		analyticalQuery.setMaterializationTime(System.currentTimeMillis() - timeMaterialized);
		
		long totalMaterializedDataSize = inMemoryDataset.getDefaultModel().size();
		
		Query analyticalQueryPlus = QueryFactory.create(analyticalQuery.getOriginalQuery());
		
		QueryExecution qexec = QueryExecutionFactory.create(analyticalQueryPlus, inMemoryDataset.getDefaultModel()) ;
		qexec.setTimeout(Config.getTimeout(), TimeUnit.MINUTES);
		
		ResultSet results = qexec.execSelect() ;
		List<Map<String, String>> resultsList = ResultsHash.serialize(results);
		result = ResultsHash.serialize(resultsList);
		long timeb = System.currentTimeMillis();
		long runtime = timeb - timea;
		
		log(analyticalQuery, result, runtime, resultsList.size(), run, totalMaterializedDataSize, materializedFragmentsSize);
		logExperimentalData(analyticalQuery, runtime, materializedFragmentsSize, resultsList.size(), run, totalMaterializedDataSize);
		return result;
	}
	
	private String fullMaterializationEvaluation(MaterializedFragments materializedfragments,
			AnalyticalQuery analyticalQuery, Dataset dataset, int run) {
		String result;
		long materializedFragmentsSize = 0;
		long timea = System.currentTimeMillis();
		Set<String> fromClauses = analyticalQuery.getFromClause();
		Query materializationQuery = QueryFactory.create("CONSTRUCT {?s ?p ?o} WHERE {?s ?p ?o}");
		
		long timeMaterialized = System.currentTimeMillis();
		for (String graph : fromClauses) {			
			Model model = materializedfragments.getMaterializedModel(graph);
			if (graph.contains("fragment")) {
				materializedFragmentsSize += model.size();
			}
			
			if (!model.isEmpty()) {
				dataset.addNamedModel(graph, model);
			
				
			}
			materializationQuery.addGraphURI(graph);
		}
		
		QueryExecution materializationExecution = QueryExecutionFactory.create(materializationQuery, dataset) ;
		
		long timeExecConstruct = System.currentTimeMillis();
		Model materializedData = materializationExecution.execConstruct() ;
		analyticalQuery.setConstructQueryTime(System.currentTimeMillis() - timeExecConstruct);
		
		analyticalQuery.setMaterializationTime(System.currentTimeMillis() - timeMaterialized);
		
		long totalMaterializedDataSize = materializedData.size();
		
		Query analyticalQueryPlus = QueryFactory.create(analyticalQuery.getOriginalQuery());
		
		QueryExecution qexec = QueryExecutionFactory.create(analyticalQueryPlus, materializedData) ;
		qexec.setTimeout(Config.getTimeout(), TimeUnit.MINUTES);
		
		ResultSet results = qexec.execSelect() ;
		List<Map<String, String>> resultsList = ResultsHash.serialize(results);
		result = ResultsHash.serialize(resultsList);
		long timeb = System.currentTimeMillis();
		long runtime = timeb - timea;
		
		log(analyticalQuery, result, runtime, resultsList.size(), run, totalMaterializedDataSize, materializedFragmentsSize);
		logExperimentalData(analyticalQuery, runtime, materializedFragmentsSize, resultsList.size(), run, totalMaterializedDataSize);
		return result;
	}

	private String basicEvaluation(MaterializedFragments materializedfragments, AnalyticalQuery analyticalQuery,
			Dataset dataset, int run) {
		String result;
		int materializedFragmentsSize = 0;
		int numberOfMaterializedFragments = 0;
		long timea = System.currentTimeMillis();
		Set<String> fromClauses = analyticalQuery.getFromClause();
		for (String graph : fromClauses) {
			Model model = materializedfragments.getMaterializedModel(graph);
			materializedFragmentsSize += model.size();
			numberOfMaterializedFragments ++;
			if (!model.isEmpty()) {
				dataset.addNamedModel(graph, model);
			}
		}
		
		QueryExecution qexec = QueryExecutionFactory.create(analyticalQuery.getQuery(), dataset) ;
		qexec.setTimeout(Config.getTimeout(), TimeUnit.MINUTES);
		
		ResultSet results = qexec.execSelect() ;
		List<Map<String, String>> resultsList = ResultsHash.serialize(results);
		result = ResultsHash.serialize(resultsList);
		long timeb = System.currentTimeMillis();
		long runtime = timeb - timea;
		log(analyticalQuery, result, runtime, resultsList.size(), run, 0l, 0l);
		logExperimentalData(analyticalQuery, runtime, materializedFragmentsSize, resultsList.size(), numberOfMaterializedFragments, 0l);
		return result;
	}
}
