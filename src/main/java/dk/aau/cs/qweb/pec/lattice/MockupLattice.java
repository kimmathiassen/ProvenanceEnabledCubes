package dk.aau.cs.qweb.pec.lattice;

import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.types.Quadruple;

public class MockupLattice extends Lattice {

	MockupLattice(Fragment root, RDFCubeStructure schema, RDFCubeDataSource data) {
		super(root, schema, data);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean isMergeStartConditionFulfilled() {
		return true;
	}

	@Override
	public boolean isMergeEndConditionFulfilled() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean merge() {
		return false;
	}
	
	@Override
	void registerTuple(Quadruple quad) {}

}
