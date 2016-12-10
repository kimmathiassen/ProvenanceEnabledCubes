package dk.aau.cs.qweb.pec;

import java.util.ArrayList;
import java.util.List;
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

	private String resultLogLocation;

	public JenaResultFactory(String resultLogLocation) {
		this.resultLogLocation = resultLogLocation;
	}

	@Override
	public List<String> evaluate(Set<ProvenanceQuery> provenanceQueries) {
		
		Dataset dataset = TDBFactory.createDataset(Config.getInstanceDataLocation()) ;
		List<String> provenanceIdentifiers = new ArrayList<String>();
		dataset.begin(ReadWrite.READ) ;
		
		for (ProvenanceQuery provenanceQuery : provenanceQueries) {
			
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
		}
		dataset.end();
		
		return provenanceIdentifiers;
	}

	@Override
	public String evaluate(MaterializedFragments materializedfragments, AnalyticalQuery analyticalQuery) {

		Dataset dataset = TDBFactory.createDataset(Config.getInstanceDataLocation()) ;
		dataset.begin(ReadWrite.READ) ;
		
		List<String> fromClauses = analyticalQuery.getFromClause();
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
		
		dataset.end();
	
		return result;
	}
}
