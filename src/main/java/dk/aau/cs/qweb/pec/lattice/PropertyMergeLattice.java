package dk.aau.cs.qweb.pec.lattice;

import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.fragment.Fragment;

public class PropertyMergeLattice extends MergeLattice {

	PropertyMergeLattice(Fragment root, RDFCubeStructure schema, RDFCubeDataSource data) {
		super(root, schema, data);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean merge() {
		boolean ret = propertyMerge();
		if (ret) ++propertyMergeSteps;
		return ret;
	}

}
