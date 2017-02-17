package dk.aau.cs.qweb.pec.lattice;

import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.fragment.Fragment;

public class NoMergeLattice extends Lattice {

	NoMergeLattice(Fragment root, RDFCubeStructure schema, RDFCubeDataSource data) {
		super(root, schema, data);
	}

	@Override
	public boolean isMergeStartConditionForfilled() {
		return false;
	}

	@Override
	public boolean isMergeEndConditionForfilled() {
		return true;
	}

	@Override
	public boolean merge() {
		return false;
	}

}
