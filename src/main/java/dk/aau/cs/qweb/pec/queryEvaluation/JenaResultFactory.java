package dk.aau.cs.qweb.pec.queryEvaluation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryCancelledException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
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
			
			ResultSet results = qexec.execSelect() ;
			String result = ResultSetFormatter.asText(results);
			
			for (String provenanceIdentifier : result.split(",")) {
				provenanceIdentifiers.add(provenanceIdentifier);
			}
		} else {
			provenanceIdentifiers.addAll(provenanceQuery.getProvenanceIdentifiers());
		}
		dataset.end();
		
		return provenanceIdentifiers;
	}

	@Override
	//Execute analytical query
	public String evaluate(MaterializedFragments materializedfragments, AnalyticalQuery analyticalQuery) {
		String result = "";
		Dataset dataset = TDBFactory.createDataset(datasetPath) ;
		dataset.begin(ReadWrite.WRITE) ;
		
		try {
			if (evaluationStrategy.equals("fullMaterialization")) {
				result = fullMaterializationEvaluation(materializedfragments, analyticalQuery, dataset);
			} else {
				result = basicEvaluation(materializedfragments, analyticalQuery, dataset);
			}
		} catch (QueryCancelledException e) {
			logExperimentalData(analyticalQuery, INTERRUPTED, 0);
			System.out.println(e.getStackTrace());
		} finally {
			dataset.end();
		}
		return result;
	}

	private String fullMaterializationEvaluation(MaterializedFragments materializedfragments,
			AnalyticalQuery analyticalQuery, Dataset dataset) {
		String result;
		int materializedFragmentsSize = 0;
		long timea = System.currentTimeMillis();
		Set<String> fromClauses = analyticalQuery.getFromClause();
		Query materializationQuery = QueryFactory.create("CONSTRUCT {?s ?p ?o} WHERE {?s ?p ?o}");
		
		for (String graph : fromClauses) {
			Model model = materializedfragments.getMaterializedModel(graph);
			materializedFragmentsSize += model.size();
			if (!model.isEmpty()) {
				dataset.addNamedModel(graph, model);
			}
			materializationQuery.addGraphURI(graph);
		}
		
		QueryExecution materializationExecution = QueryExecutionFactory.create(materializationQuery, dataset) ;
		Model materializedData = materializationExecution.execConstruct() ;
		
		Query analyticalQueryPlus = QueryFactory.create(analyticalQuery.getOriginalQuery());
		
		QueryExecution qexec = QueryExecutionFactory.create(analyticalQueryPlus, materializedData) ;
		qexec.setTimeout(Config.getTimeout(), TimeUnit.MINUTES);
		
		ResultSet results = qexec.execSelect() ;
		result = ResultSetFormatter.asText(results);
		long timeb = System.currentTimeMillis();
		long runtime = timeb - timea;
		
		log(analyticalQuery, result, runtime);
		logExperimentalData(analyticalQuery, runtime, materializedFragmentsSize);
		return result;
	}

	private String basicEvaluation(MaterializedFragments materializedfragments, AnalyticalQuery analyticalQuery,
			Dataset dataset) {
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
		result = ResultSetFormatter.asText(results);
		long timeb = System.currentTimeMillis();
		long runtime = timeb - timea;
		log(analyticalQuery, result, runtime);
		logExperimentalData(analyticalQuery, runtime, materializedFragmentsSize, numberOfMaterializedFragments);
		return result;
	}

//	@Override
//	// execute 
//	public String evaluate(Set<String> provenanceIdentifiers) {
//		String result = "";
//		Dataset dataset = TDBFactory.createDataset(datasetPath) ;
//		
//		//StoreParamsBuilder builder = StoreParams.builder();
//		//StoreParams params =  builder.build();
//		//Location location = Location.create(datasetPath);
//		
//		//TDBFactory.setup(location, params) ;
//		//TDBFactory.release(dataset);
//		dataset.begin(ReadWrite.READ) ;
//		
//		try {
//			String query = "Select * ";
//			for (String graph : provenanceIdentifiers) {
//				query += "FROM <"+graph+"> ";
//			}
//			query += "WHERE {?a ?b ?c}";
//			
//			Query newQuery = QueryFactory.create(query);
//			
//			QueryExecution qexec = QueryExecutionFactory.create(newQuery, dataset) ;
//			qexec.setTimeout(Config.getTimeout(), TimeUnit.MINUTES);
//			
//			ResultSet results = qexec.execSelect() ;
//			result = ResultSetFormatter.asText(results);
//			
//		} catch (QueryCancelledException e) {
//			System.out.println(e.getStackTrace());
//		} finally {
//			dataset.end();
//		}
//		return result;
//	}
}
