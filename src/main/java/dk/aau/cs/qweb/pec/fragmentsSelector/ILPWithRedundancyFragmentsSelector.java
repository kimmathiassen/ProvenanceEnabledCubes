package dk.aau.cs.qweb.pec.fragmentsSelector;

import java.io.FileNotFoundException;

import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.lattice.Lattice;
import gurobi.GRBException;

public class ILPWithRedundancyFragmentsSelector extends ILPFragmentsSelector {

	public ILPWithRedundancyFragmentsSelector(Lattice lattice, String logFile, boolean output2Std)
			throws FileNotFoundException, GRBException, DatabaseConnectionIsNotOpen {
		super(lattice, logFile, output2Std);
	}

	@Override
	protected void defineAncestorsRedundancyConstraint() {
		
	}
}
