package dk.aau.cs.qweb.pec.fragmentsselector;

import java.beans.Expression;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.lattice.Lattice;
import gurobi.GRB;
import gurobi.GRBConstr;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

public class ILPFragmentsSelector extends FragmentsSelector {

	private GRBModel ilp;
	
	private double defaultBudget;
	
	private Map<Fragment, GRBVar> variables;
	
	public ILPFragmentsSelector(Lattice lattice) throws GRBException, DatabaseConnectionIsNotOpen {
		super(lattice);
		GRBEnv env = new GRBEnv();
		ilp = new GRBModel(env);
		defaultBudget = 1.0;
		variables = new LinkedHashMap<>();
		populateModel();
	}
	
	public ILPFragmentsSelector(Lattice lattice, String logFile) throws FileNotFoundException, GRBException, DatabaseConnectionIsNotOpen {
		super(lattice, logFile);
		GRBEnv env = new GRBEnv(this.logFile);		
		ilp = new GRBModel(env);		
		defaultBudget = 1.0;
		variables = new LinkedHashMap<>();
		populateModel();
	}
	
	/**
	 * Populates the underlying ILP model, i.e., it defines the variables, the objective function
	 * and the constraints.
	 * @throws GRBException 
	 * @throws DatabaseConnectionIsNotOpen 
	 */
	private void populateModel() throws GRBException, DatabaseConnectionIsNotOpen {
		defineVariables();
		defineObjectiveFunction();
		defineBudgetConstraint();
		defineAncestorsRedundancyConstraint();
		defineMetadataColocationConstraint();
		defineMaterializeMeasuresConstraint();
		
	}

	private void defineAncestorsRedundancyConstraint() throws GRBException {
		for (Fragment fragment : lattice) {
			if (fragment.isRoot())
				continue;
			
			Set<Fragment> ancestors = lattice.getAncestors(fragment);
			if (!ancestors.isEmpty()) {
				GRBLinExpr expression = new GRBLinExpr();
				expression.addTerm(1.0, variables.get(fragment));
				for (Fragment ancestor : ancestors) {
					expression.addTerm(1.0 / ancestors.size(), variables.get(ancestor));
				}
				ilp.addConstr(expression, GRB.LESS_EQUAL, 1.0, "redundancy_" + fragment.getShortName());
			}
		}
		
	}

	private void defineMaterializeMeasuresConstraint() throws GRBException {
		Set<Fragment> measureFragments = new LinkedHashSet<>();
		GRBLinExpr expression = new GRBLinExpr();
		for (Fragment fragment : measureFragments) {
			expression.addTerm(1.0, variables.get(fragment));
		}
		ilp.addConstr(expression, GRB.GREATER_EQUAL, 1.0, "measures");
	}

	private void defineMetadataColocationConstraint() throws GRBException {
		for (Fragment fragment : lattice) {
			if (fragment.isRoot() || fragment.isMetadata())
				continue;
			
			Set<Fragment> metaFragments = lattice.getMetadataFragments(fragment);
			if (!metaFragments.isEmpty()) {
				for (Fragment metaFragment : metaFragments) {
					GRBLinExpr expression = new GRBLinExpr();
					expression.addTerm(1.0, variables.get(fragment));
					expression.addTerm(-1.0, variables.get(metaFragment));
					ilp.addConstr(expression, GRB.LESS_EQUAL, 0.0, 
							"colocation_" + fragment.getShortName() + "_" + metaFragment.getShortName());
				}
			}
		}
	}

	
	private void defineBudgetConstraint() throws GRBException {
		GRBLinExpr expression = new GRBLinExpr();
		for (Fragment fragment : variables.keySet()) {
			expression.addTerm((double)fragment.size(), variables.get(fragment));
		}
		ilp.addConstr(expression, GRB.LESS_EQUAL, defaultBudget, "budget");
	}

	private void defineObjectiveFunction() throws DatabaseConnectionIsNotOpen, GRBException {
		GRBLinExpr expr = new GRBLinExpr();
		
		for (Fragment fragment : lattice) {
			if (fragment.isMetadata() || fragment.isRoot()) {
				expr.addTerm(1.0, variables.get(fragment));
			} else {
				// Check the s-s join fragments
				Set<Fragment> joinCandidates = 
						lattice.ssjoinCandidates(fragment);
			
				double nCandidates = joinCandidates.size() + 1.0;
				double joinTerm = 1.0;
				for (Fragment joinCandidate : joinCandidates) {
					joinTerm += lattice.getData().joinCount(fragment.getSignatures(), joinCandidate.getSignatures());
				}
				expr.addTerm(joinTerm * nCandidates, variables.get(fragment));
 			}
		}
		
		ilp.setObjective(expr, GRB.MAXIMIZE);
	}

	/**
	 * Defines an integral variable per fragment in the lattice
	 * @throws GRBException
	 */
	private void defineVariables() throws GRBException {
		// There will be one variable per fragment
		for (Fragment fragment : lattice) {
			variables.put(fragment, 
					ilp.addVar(0.0, 1.0, 0.0, GRB.BINARY, fragment.getShortName()));
		}
		
	}

	
	@Override
	protected void finalize() {
		GRBEnv env = null;
		try {
			env = ilp.getEnv();
		} catch (GRBException e) {
			e.printStackTrace();
		}
		ilp.dispose();
		try {
			env.dispose();
		} catch (GRBException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Set<Fragment> select(long budget) throws DatabaseConnectionIsNotOpen {
		lattice.getData().open();
		Set<Fragment> selected = new LinkedHashSet<>();
		GRBConstr budgetConstraint;
		try {
			budgetConstraint = ilp.getConstrByName("budget");
			budgetConstraint.set(GRB.DoubleAttr.RHS, (double) budget);
			ilp.optimize();
			
			for (Fragment fragment : variables.keySet()) {
				GRBVar variable = variables.get(fragment);
				double assignment = variable.get(GRB.DoubleAttr.X);
				if (assignment == 1.0) {
					selected.add(fragment);
				}
			}

		} catch (GRBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			lattice.getData().close();
			return null;
		}		
		
		lattice.getData().close();	
		return selected;
	}

}
