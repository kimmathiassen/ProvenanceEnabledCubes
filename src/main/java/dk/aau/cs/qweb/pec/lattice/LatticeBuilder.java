package dk.aau.cs.qweb.pec.lattice;

import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.fragment.Fragment;


public class LatticeBuilder {

	static public Lattice build(RDFCubeDataSource dataSource, RDFCubeStructure schema,String latticeMergeStrategy) throws DatabaseConnectionIsNotOpen {
		Fragment root = Lattice.createFragment(); 
		Lattice lattice;
		
		switch (latticeMergeStrategy) {
		case "noMerge":
			lattice = new NoMergeLattice(root, schema, dataSource);
			break;
		case "naive" :
			lattice = new NaiveMergeLattice(root, schema, dataSource);
		default:
			lattice = new NoMergeLattice(root, schema, dataSource);
			break;
		}
		
		
		try {
			dataSource.open();
			
			while (dataSource.hasNext()) {
				lattice.registerTuple(dataSource.next());
			}
			
		} finally {
			dataSource.close();
		}
		
		// If a parent and a child have the same number of triples, 
		// mark one of them as redundant
		lattice.markRedundantFragments();
			
		return lattice;
	}

}
