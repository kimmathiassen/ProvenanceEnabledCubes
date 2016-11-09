package dk.aau.cs.qweb.pec.rdfcube.lattice;

import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;

public interface FragmentLatticeBuilder {

	/**
	 * Builds a fragments lattice from a given cube and its corresponding structure.
	 * @param source
	 * @param structure
	 * @return
	 */
	public FragmentLattice build(RDFCubeDataSource source, RDFCubeStructure structure);
}
