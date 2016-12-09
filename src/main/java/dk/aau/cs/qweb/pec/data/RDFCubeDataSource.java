package dk.aau.cs.qweb.pec.data;

import java.util.Collection;

import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.types.Quadruple;
import dk.aau.cs.qweb.pec.types.Signature;

public interface RDFCubeDataSource {

	public void open() ;
	
	public void close();
	
	public Quadruple next() throws DatabaseConnectionIsNotOpen;
	
	public Boolean hasNext() throws DatabaseConnectionIsNotOpen;
	
	public long joinCount(Collection<Signature> signatures1,
			Collection<Signature> signatures2) throws DatabaseConnectionIsNotOpen;
}
