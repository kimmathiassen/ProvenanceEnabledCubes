package dk.aau.cs.qweb.pec.fragmentsselector;

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

	public ImprovedILPFragmentsSelector(Lattice lattice) throws GRBException, DatabaseConnectionIsNotOpen {
		super(lattice);
	}
	
	public ImprovedILPFragmentsSelector(Lattice lattice, String logLocation) throws FileNotFoundException, GRBException, DatabaseConnectionIsNotOpen {
		super(lattice, logLocation);
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
			expression.addTerm(1.0, fragments2Variables.get(fragment));
		}
		ilp.addConstr(expression, GRB.GREATER_EQUAL, 1.0, "measures");
	}

}
