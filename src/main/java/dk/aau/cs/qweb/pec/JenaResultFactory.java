package dk.aau.cs.qweb.pec;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb.TDBFactory;

import dk.aau.cs.qweb.pec.QueryEvaluation.AnalyticalQuery;
import dk.aau.cs.qweb.pec.QueryEvaluation.MaterializedFragments;

public class JenaResultFactory extends ResultFactory {

	public JenaResultFactory(String resultLogLocation, Long budget, String selectFragmentStrategy, String cacheStretegy, String datasetPath ) throws FileNotFoundException {
		super(resultLogLocation, budget, selectFragmentStrategy,cacheStretegy,datasetPath);
	}

	@Override
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
	public String evaluate(MaterializedFragments materializedfragments, AnalyticalQuery analyticalQuery) {

		Dataset dataset = TDBFactory.createDataset(datasetPath) ;
		dataset.begin(ReadWrite.WRITE) ;
		
		long timea = System.currentTimeMillis();
		Set<String> fromClauses = analyticalQuery.getFromClause();
		for (String graph : fromClauses) {
			Model model = materializedfragments.getMaterializedModel(graph);
			if (!model.isEmpty()) {
				dataset.addNamedModel(graph, model);
			}
		}
		
		QueryExecution qexec = QueryExecutionFactory.create(analyticalQuery.getQuery(), dataset) ;
		qexec.setTimeout(Config.getTimeout(), TimeUnit.MINUTES);
		
		ResultSet results = qexec.execSelect() ;
		String result = ResultSetFormatter.asText(results);
		long timeb = System.currentTimeMillis();
		dataset.end();
		
		
		
		log(analyticalQuery, result, timeb-timea);
		//System.out.println(analyticalQuery);
		//System.out.println(result);
		return result;
	}

	@Override
	public String evaluate(Set<String> provenanceIdentifiers) {
		
		Dataset dataset = TDBFactory.createDataset(datasetPath) ;
		dataset.begin(ReadWrite.READ) ;
		
		String query = "Select * ";
		for (String graph : provenanceIdentifiers) {
			query += "FROM <"+graph+"> ";
		}
		query += "WHERE {?a ?b ?c}";
		
		Query newQuery = QueryFactory.create(query);
		
		QueryExecution qexec = QueryExecutionFactory.create(newQuery, dataset) ;
		qexec.setTimeout(Config.getTimeout(), TimeUnit.MINUTES);
		
		ResultSet results = qexec.execSelect() ;
		String result = ResultSetFormatter.asText(results);
			
		dataset.end();
		return result;
	}
}
