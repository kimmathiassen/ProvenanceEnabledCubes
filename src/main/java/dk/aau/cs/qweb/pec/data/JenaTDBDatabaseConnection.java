package dk.aau.cs.qweb.pec.data;

import java.util.Collection;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.tdb.TDBFactory;

import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.types.Quadruple;
import dk.aau.cs.qweb.pec.types.Signature;

public class JenaTDBDatabaseConnection implements RDFCubeDataSource {

	private Dataset dataset ;
	private Query query;
	private QueryExecution qexec;
	private ResultSet results;
	private Boolean hasResultSetBeenFetched = false;
	private boolean open = false;
	
	private JenaTDBDatabaseConnection() {
	//TODO missing db location
		dataset = TDBFactory.createDataset(""); 
	}
	
	public void open() {
		dataset.begin(ReadWrite.READ);
	}
	
	private void readAllQuads() {
		query = QueryFactory.create("") ;
		qexec = QueryExecutionFactory.create(query, dataset);
		results = qexec.execSelect() ;
	}
	
	public void close() {
		dataset.end();
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
	
	public Quadruple<String, String, String, String> next() throws DatabaseConnectionIsNotOpen {
		isConnectionOpen();
		
		if (!hasResultSetBeenFetched) {
			readAllQuads();
			hasResultSetBeenFetched = true;
		}
		
		Quadruple<String, String, String, String> quad = new Quadruple<String, String, String, String>(null, null, null, null);
		if (results.hasNext()) {
			QuerySolution soln = results.nextSolution() ;
			String subject = soln.get("subject").toString();
			String predicate = soln.get("predicate").toString();
			String object = soln.get("object").toString();
			String graph = soln.get("graph").toString();
			
			quad.setFirst(subject);
			quad.setSecond(predicate);
			quad.setThird(object);
			quad.setFourth(graph);
		}
		return quad;
	}

	@Override
	public long joinCount(Collection<Signature<String, String, String, String>> signatures1,
			Collection<Signature<String, String, String, String>> signatures2) throws DatabaseConnectionIsNotOpen {
		isConnectionOpen();
		// TODO Auto-generated method stub
		return 0;
	}

	public static RDFCubeDataSource build() {
		return new JenaTDBDatabaseConnection();
	}
}
