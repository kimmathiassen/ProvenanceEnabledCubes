package dk.aau.cs.qweb.pec.fragmentsSelector;

import java.io.FileNotFoundException;

import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.lattice.Lattice;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;

public class ILPCubeObliviousFragmentsSelector extends ILPFragmentsSelector {

	public ILPCubeObliviousFragmentsSelector(Lattice lattice, boolean output2Std)
			throws GRBException, DatabaseConnectionIsNotOpen {
		super(lattice, output2Std);
	}
	
	public ILPCubeObliviousFragmentsSelector(Lattice lattice, String logFile, boolean output2Std) 
			throws FileNotFoundException, GRBException, DatabaseConnectionIsNotOpen {
		super(lattice, logFile, output2Std);
	}
	
	protected void defineObjectiveFunction() throws DatabaseConnectionIsNotOpen, GRBException {
		GRBLinExpr expr = new GRBLinExpr();
		lattice.getData().open();
		
		for (Fragment fragment : lattice) {
			if (fragment.isRedundant()) continue;
				
			if (lattice.isRoot(fragment)) {
				expr.addTerm(1.0, fragments2Variables.get(fragment));
			} else {
				//Reward bigger fragments,
				expr.addTerm((double)fragment.size(), fragments2Variables.get(fragment));
 			}
		}
		lattice.getData().close();
		ilp.setObjective(expr, GRB.MAXIMIZE);
	}

}
