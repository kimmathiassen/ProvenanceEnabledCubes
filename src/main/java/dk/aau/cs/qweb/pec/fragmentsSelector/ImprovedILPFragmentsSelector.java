package dk.aau.cs.qweb.pec.fragmentsSelector;

import java.io.FileNotFoundException;
import java.util.LinkedHashSet;
import java.util.Set;

import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.lattice.Lattice;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;

public class ImprovedILPFragmentsSelector extends ILPFragmentsSelector {

	public ImprovedILPFragmentsSelector(Lattice lattice, boolean output2Stdout) throws GRBException, DatabaseConnectionIsNotOpen {
		super(lattice, output2Stdout);
	}
	
	public ImprovedILPFragmentsSelector(Lattice lattice, String logLocation, boolean output2Stdout) throws 
	FileNotFoundException, GRBException, DatabaseConnectionIsNotOpen {
		super(lattice, logLocation, output2Stdout);
	}

	@Override
	protected void defineMaterializeMeasuresConstraint() throws GRBException {
		Set<Fragment> measureFragments = lattice.getMeasureFragments();
		// Now collect all ancestors
		Set<Fragment> fragmentsInExpression = new LinkedHashSet<>();
		for (Fragment measureFragment : measureFragments) {
			fragmentsInExpression.addAll(lattice.getAncestors(measureFragment));
		}
		fragmentsInExpression.addAll(measureFragments);
		
		GRBLinExpr expression = new GRBLinExpr();
		for (Fragment fragment : fragmentsInExpression) {
			if (fragments2Variables.containsKey(fragment)) {
				expression.addTerm(1.0, fragments2Variables.get(fragment));
			}
		}
		ilp.addConstr(expression, GRB.GREATER_EQUAL, 1.0, "measures");
	}

}
