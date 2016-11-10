package dk.aau.cs.qweb.pec.lattice;

import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;

public interface LatticeBuilder {

	/**
	 * Builds a fragments lattice from a given cube and its corresponding structure.
	 * @param source
	 * @param structure
	 * @return
	 * @throws DatabaseConnectionIsNotOpen 
	 */
	public Lattice build(RDFCubeDataSource source, RDFCubeStructure structure) throws DatabaseConnectionIsNotOpen;
}
