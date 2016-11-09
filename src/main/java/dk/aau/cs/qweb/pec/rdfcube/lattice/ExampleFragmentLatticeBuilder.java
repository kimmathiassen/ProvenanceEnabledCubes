package dk.aau.cs.qweb.pec.rdfcube.lattice;

import java.util.Iterator;

import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.rdfcube.fragment.Fragment;
import dk.aau.cs.qweb.pec.types.Quadruple;


public class ExampleFragmentLatticeBuilder implements FragmentLatticeBuilder {

	@Override
	public FragmentLattice build(RDFCubeDataSource data, RDFCubeStructure schema) {
		Fragment root = FragmentLattice.createFragment(); 
		FragmentLattice lattice = new FragmentLattice(root, schema, data);
		
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
