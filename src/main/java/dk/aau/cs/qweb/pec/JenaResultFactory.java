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

	private String resultLogLocation;

	public JenaResultFactory(String resultLogLocation) {
		this.resultLogLocation = resultLogLocation;
	}

	@Override
	public Set<String> evaluate(ProvenanceQuery provenanceQuery) throws FileNotFoundException, IOException {
		
		Dataset dataset = TDBFactory.createDataset(Config.getInstanceDataLocation()) ;
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

		Dataset dataset = TDBFactory.createDataset(Config.getInstanceDataLocation()) ;
		dataset.begin(ReadWrite.WRITE) ;
		
		Set<String> fromClauses = analyticalQuery.getFromClause();
		for (String graph : fromClauses) {
			Model model = materializedfragments.getMaterializedModel(graph);
			if (!model.isEmpty()) {
				dataset.addNamedModel(graph, model);
			}
		}
//		System.out.println(dataset.containsNamedModel("http://qweb.cs.aau.dk/airbase/fragment/151"));
//		Model frag = dataset.getNamedModel("http://qweb.cs.aau.dk/airbase/fragment/151");
//		StmtIterator iterator = frag.listStatements();
//		while (iterator.hasNext()) {
//			 Statement stmt      = iterator.nextStatement();  // get next statement
//			    String  subject   = stmt.getSubject().toString();     // get the subject
//			    String  predicate = stmt.getPredicate().toString();   // get the predicate
//			    String   object    = stmt.getObject().toString();      // get the object
//			    
//			    System.out.println(subject + " " + predicate+ " "+ object);
//		}
		
		
		QueryExecution qexec = QueryExecutionFactory.create(analyticalQuery.getQuery(), dataset) ;
		qexec.setTimeout(Config.getTimeout(), TimeUnit.MINUTES);
		
		ResultSet results = qexec.execSelect() ;
		String result = ResultSetFormatter.asText(results);
		
		dataset.end();
		System.out.println(analyticalQuery);
		System.out.println(result);
		return result;
	}

	@Override
	public String evaluate(Set<String> provenanceIdentifiers) {
		
		Dataset dataset = TDBFactory.createDataset(Config.getInstanceDataLocation()) ;
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
