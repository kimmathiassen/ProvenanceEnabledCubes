package dk.aau.cs.qweb.pec.data;

import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.types.Quadruple;

public interface RDFCubeDataSource {

	public void open() ;
	
	public void close();
	
	public Quadruple next() throws DatabaseConnectionIsNotOpen;
	
	public Boolean hasNext() throws DatabaseConnectionIsNotOpen;
	
	public int count(); 
}
