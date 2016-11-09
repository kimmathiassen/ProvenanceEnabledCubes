package dk.aau.cs.qweb.pec.lattice;

import java.util.Iterator;

import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.types.Quadruple;


public class NaiveLatticeBuilder implements LatticeBuilder {

	@Override
	public Lattice build(RDFCubeDataSource data, RDFCubeStructure schema) {
		Fragment root = Lattice.createFragment(); 
		Lattice lattice = new Lattice(root, schema, data);
		
		Iterator<Quadruple<String, String, String, String>> iterator = data.iterator();
		// Register all the triples in the fragments
		while (iterator.hasNext()) {
			lattice.registerTuple(iterator.next());
		}
		
		// Create the metadata relations between the fragments
		lattice.linkData2MetadataFragments();
			
		return lattice;
	}

}
