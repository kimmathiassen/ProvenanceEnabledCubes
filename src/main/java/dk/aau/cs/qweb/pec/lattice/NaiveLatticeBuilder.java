package dk.aau.cs.qweb.pec.lattice;

import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.fragment.Fragment;


public class NaiveLatticeBuilder implements LatticeBuilder {

	@Override
	public Lattice build(RDFCubeDataSource dataSource, RDFCubeStructure schema) throws DatabaseConnectionIsNotOpen {
		Fragment root = Lattice.createFragment(); 
		Lattice lattice = new Lattice(root, schema, dataSource);
		
		try {
			dataSource.open();
			
			while (dataSource.hasNext()) {
				lattice.registerTuple(dataSource.next());
			}
			
		} finally {
			dataSource.close();
		}
		
		// Create the metadata relations between the fragments
		lattice.linkData2MetadataFragments();
			
		return lattice;
	}

}
