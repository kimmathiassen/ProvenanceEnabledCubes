package dk.aau.cs.qweb.pec.fragmentsSelector;

import java.io.FileNotFoundException;

import dk.aau.cs.qweb.pec.Config;
import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.lattice.Lattice;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;

public class ILPWithObservationDistanceFragmentsSelector extends ILPFragmentsSelector {

	public ILPWithObservationDistanceFragmentsSelector(Lattice lattice, boolean output2Std)
			throws GRBException, DatabaseConnectionIsNotOpen {
		super(lattice, output2Std);
	}
	
	public ILPWithObservationDistanceFragmentsSelector(Lattice lattice, String logFile, boolean output2Std) throws FileNotFoundException, GRBException, DatabaseConnectionIsNotOpen {
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
				//Reward bigger fragments, penalize complex signatures, reward fragments with properties that are close to the observations in the schema
				double measuresRatio = 1 + (fragment.getMeasureTriplesCount() / fragment.size());
				double distanceFactor = getDistance2ObservationFactor(fragment);
				double term = measuresRatio * distanceFactor * fragment.size();
				expr.addTerm(term, fragments2Variables.get(fragment));
 			}
		}
		
		lattice.getData().close();
		ilp.setObjective(expr, GRB.MAXIMIZE);
	}

	private double getDistance2ObservationFactor(Fragment fragment) {
		double minDistance = Config.getMaximalDistance2ObservationInSchema();
		for (String predicate : lattice.getAllPredicates(fragment)) {
			double distance = lattice.getStructure().getDistanceToObservation(predicate);				
			if (distance < minDistance) {
				minDistance = distance;
			}
		}
			
		return 1.0 / minDistance;	
	}


}
