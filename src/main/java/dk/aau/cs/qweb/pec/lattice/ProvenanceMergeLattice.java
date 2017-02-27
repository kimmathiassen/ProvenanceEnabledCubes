package dk.aau.cs.qweb.pec.lattice;

import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.fragment.Fragment;

public class ProvenanceMergeLattice extends MergeLattice {

	ProvenanceMergeLattice(Fragment root, RDFCubeStructure schema, RDFCubeDataSource data) {
		super(root, schema, data);
	}

	@Override
	public boolean merge() {
		// TODO Auto-generated method stub
		return provenanceMerge();
	}

}
