package dk.aau.cs.qweb.pec.data;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.base.file.Location;
import org.apache.jena.tdb.setup.StoreParams;

import dk.aau.cs.qweb.pec.Config;
import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.queryEvaluation.ResultsHash;
import dk.aau.cs.qweb.pec.types.Quadruple;

public class JenaTDBDatabaseConnection implements RDFCubeDataSource {

	private Dataset dataset ;
	private Query query;
	private QueryExecution qexec;
	private ResultSet results;
	private Boolean hasResultSetBeenFetched = false;
	private boolean open = false;
	private Map<String, Set<String>> relation2Subject = new HashMap<String,Set<String>>();
	
	private Map<String, Set<String>> provid2Subject = new HashMap<String,Set<String>>();
	private int count = 0;
	
	private JenaTDBDatabaseConnection(String dbLocation, String cache) {
		StoreParams params = createStoreCacheParameters(cache, dbLocation);
		
		
		Location location = Location.create(dbLocation);
		TDBFactory.setup(location, params);
		
		dataset = TDBFactory.createDataset(location); 
		
		String allQuadsString = "Select ?s ?p ?c WHERE {GRAPH ?c {?s ?p ?o} }";
		
		Query allQuadsQuery = QueryFactory.create(allQuadsString);
		QueryExecution qexec = QueryExecutionFactory.create(allQuadsQuery, dataset) ;
		
		ResultSet results = qexec.execSelect() ;
		while (results.hasNext())
		{
			QuerySolution binding = results.nextSolution();
			String subject = binding.get("s").toString();
			String predicate = binding.get("p").toString();
			String graph = binding.get("c").toString();
			count ++;
			addToMappings(subject,predicate,graph);
		}
	}
	
	private StoreParams createStoreCacheParameters(String cache, String dbLocation) {
		StoreParams params = null;
		if (cache.equals("cold")) {
			params = StoreParams.builder()
					.blockReadCacheSize(0)
					.blockWriteCacheSize(0)
					.nodeMissCacheSize(0)
					.node2NodeIdCacheSize(0)
					.nodeId2NodeCacheSize(0)
					.build();
		} else if (cache.equals("tepid")) {
			// If there is one budget then set the cache based on that budget
			// This calculation is based on Kim's documentation
			if (Config.getBudgetPercentages().size() == 1) {
				long budgetInPercentage = Config.getBudgetPercentages().get(0);
				long dbSizeInKB = FileUtils.sizeOfDirectory(new File(dbLocation)) / 1024;
				long node2NodeIdCacheSize = (long)((dbSizeInKB / 100) * budgetInPercentage * (1.0 / 6));
				long nodeId2NodeCacheSize = (long)((dbSizeInKB / 100) * budgetInPercentage * (5.0 / 6));
				// Use default values for the first three arguments
				params = StoreParams.builder()
						.blockReadCacheSize(10)
						.blockWriteCacheSize(10)
						.nodeMissCacheSize(100)
						.node2NodeIdCacheSize((int)node2NodeIdCacheSize)
						.nodeId2NodeCacheSize((int)nodeId2NodeCacheSize)
						.build();
			} else {
				params = StoreParams.builder().build();
			}
		} else {
			// For warm cache, keep the default values.
			params = StoreParams.builder().build();
		}
		
		System.out.println("Using " + params + " as configuration for Jena TDB");
		
		return params;
	}

	private void addToMappings(String subject, String predicate, String graph) {
		Set<String> subjects = relation2Subject.get(predicate);			
		if (subjects == null) {
			subjects = new LinkedHashSet<>();
			relation2Subject.put(predicate, subjects);
		}
		
		Set<String> subjects2 = provid2Subject.get(graph);
		if (subjects2 == null) {
			subjects2 = new LinkedHashSet<>();
			provid2Subject.put(graph, subjects2);
		}
		
		subjects.add(subject);
		subjects2.add(subject);
	}

	public void open() {
		dataset.begin(ReadWrite.READ);
		open = true;
	}
	
	private void readAllQuads() {
		query = QueryFactory.create("select ?subject ?predicate ?object ?graph WHERE { GRAPH ?graph {?subject ?predicate ?object}}") ;
		qexec = QueryExecutionFactory.create(query, dataset);
		results = qexec.execSelect() ;
	}
	
	public void close() {
		dataset.end();
		open = false;
	}
	
	public Boolean hasNext() {
		if (!hasResultSetBeenFetched) {
			readAllQuads();
			hasResultSetBeenFetched = true;
		}
		return results.hasNext();
	}
	
	private void isConnectionOpen() throws DatabaseConnectionIsNotOpen {
		if (!open) {
			throw new DatabaseConnectionIsNotOpen();
		}
	}
	
	public Quadruple next() throws DatabaseConnectionIsNotOpen {
		isConnectionOpen();
		
		if (!hasResultSetBeenFetched) {
			readAllQuads();
			hasResultSetBeenFetched = true;
		}
		
		Quadruple quad = new Quadruple(null, null, null, null);
		if (results.hasNext()) {
			QuerySolution soln = results.nextSolution() ;
			String subject = soln.get("subject").toString();
			String predicate = soln.get("predicate").toString();
			String object = soln.get("object").toString();
			String graph = soln.get("graph").toString();
			
			quad.setSubject(subject);
			quad.setPredicate(predicate);
			quad.setObject(object);
			quad.setGraphLabel(graph);
		}
		return quad;
	}

	public static RDFCubeDataSource build(String dbLocation) {
		return new JenaTDBDatabaseConnection(dbLocation,"");
	}

	@Override
	public int count() {
		return count;
	}

	public static RDFCubeDataSource build(String datasetPath, String cache) {
		return new JenaTDBDatabaseConnection(datasetPath,cache);
	}

	@Override
	public List<Map<String, String>> execSelectQuery(String selectQuery) {
		List<Map<String, String>> result = null;
		dataset.begin(ReadWrite.READ);
		Query query = QueryFactory.create(selectQuery) ;
        QueryExecution qexec = QueryExecutionFactory.create(query, dataset) ;
        ResultSet results = qexec.execSelect() ;
	    result = ResultsHash.serialize(results);
        qexec.close();
        dataset.end();
        return result;
	}
}
