package dk.aau.cs.qweb.pec.fragmentsSelector;

import java.io.FileNotFoundException;

import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.lattice.Lattice;
import gurobi.GRBException;

public class ILPWithObservationDistanceRedundancyFragmentsSelector extends ILPWithObservationDistanceFragmentsSelector {

	public ILPWithObservationDistanceRedundancyFragmentsSelector(Lattice lattice, String logFile, boolean output2Std)
			throws FileNotFoundException, GRBException, DatabaseConnectionIsNotOpen {
		super(lattice, logFile, output2Std);
	}
	
	public ILPWithObservationDistanceRedundancyFragmentsSelector(Lattice lattice, boolean output2Std)
			throws GRBException, DatabaseConnectionIsNotOpen {
		super(lattice, output2Std);
	}

	@Override
	protected void defineAncestorsRedundancyConstraint() {
		
	}
}
