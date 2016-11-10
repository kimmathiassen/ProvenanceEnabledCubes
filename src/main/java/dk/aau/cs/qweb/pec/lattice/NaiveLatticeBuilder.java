package dk.aau.cs.qweb.pec.lattice;

import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.fragment.Fragment;


public class NaiveLatticeBuilder implements LatticeBuilder {

	@Override
	public Lattice build(RDFCubeDataSource data, RDFCubeStructure schema) throws DatabaseConnectionIsNotOpen {
		Fragment root = Lattice.createFragment(); 
		Lattice lattice = new Lattice(root, schema, data);
		
		try {
			data.open();
			
			while (data.hasNext()) {
				lattice.registerTuple(data.next());
			}
			
		} finally {
			data.close();
		}
		
		// Create the metadata relations between the fragments
		lattice.linkData2MetadataFragments();
			
		return lattice;
	}

}
