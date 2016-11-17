package dk.aau.cs.qweb.pec.fragmentsselector;

import java.io.FileNotFoundException;
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
	
	private double defaultBudget = 1.0;
	
	private GRBConstr budgetConstraint;
	
	private Map<Fragment, GRBVar> fragments2Variables;
	
	private boolean considerMetadataAncestors;
	
	public ILPFragmentsSelector(Lattice lattice) throws GRBException, DatabaseConnectionIsNotOpen {
		super(lattice);
		GRBEnv env = new GRBEnv();
		ilp = new GRBModel(env);
		fragments2Variables = new LinkedHashMap<>();
		considerMetadataAncestors = false;
		populateModel();
	}
	
	public ILPFragmentsSelector(Lattice lattice, String logFile) throws FileNotFoundException, GRBException, DatabaseConnectionIsNotOpen {
		super(lattice, logFile);
		GRBEnv env = new GRBEnv(this.logFile);		
		ilp = new GRBModel(env);
		fragments2Variables = new LinkedHashMap<>();
		considerMetadataAncestors = false;
		populateModel();
	}
	
	public ILPFragmentsSelector(Lattice lattice, String logFile, boolean considerMetadataAncestors) throws FileNotFoundException, GRBException, DatabaseConnectionIsNotOpen {
		super(lattice, logFile);
		GRBEnv env = new GRBEnv(this.logFile);		
		ilp = new GRBModel(env);
		fragments2Variables = new LinkedHashMap<>();
		this.considerMetadataAncestors = considerMetadataAncestors;
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
				expression.addTerm(1.0, fragments2Variables.get(fragment));
				for (Fragment ancestor : ancestors) {
					expression.addTerm(1.0 / ancestors.size(), fragments2Variables.get(ancestor));
				}
				ilp.addConstr(expression, GRB.LESS_EQUAL, 1.0, "redundancy_" + fragment.getShortName());
			}
		}
		
	}

	private void defineMaterializeMeasuresConstraint() throws GRBException {
		Set<Fragment> measureFragments = lattice.getMeasureFragments();
		GRBLinExpr expression = new GRBLinExpr();
		for (Fragment fragment : measureFragments) {
			expression.addTerm(1.0, fragments2Variables.get(fragment));
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
					expression.addTerm(1.0, fragments2Variables.get(fragment));
					expression.addTerm(-1.0, fragments2Variables.get(metaFragment));
					
					if (considerMetadataAncestors) {
						Set<Fragment> ancestorsMetaFragment = lattice.getAncestors(metaFragment);	
						ancestorsMetaFragment.remove(lattice.getRoot());
						if (ancestorsMetaFragment.isEmpty()) {
							ilp.addConstr(expression, GRB.LESS_EQUAL, 0.0, 
									"colocation_" + fragment.getShortName() + "_" + metaFragment.getShortName());							
						} else {
							for (Fragment ancestor : ancestorsMetaFragment) {
								expression.addTerm(-1.0, fragments2Variables.get(ancestor));
							}
						}
						//Get the ancestors of the metadata fragment
						ilp.addConstr(expression, GRB.EQUAL, 0.0, metaFragment.getShortName());

					} else {
						ilp.addConstr(expression, GRB.LESS_EQUAL, 0.0, 
								"colocation_" + fragment.getShortName() + "_" + metaFragment.getShortName());
					}
				}
			}
		}
	}

	
	private void defineBudgetConstraint() throws GRBException {
		GRBLinExpr expression = new GRBLinExpr();
		for (Fragment fragment : fragments2Variables.keySet()) {
			expression.addTerm((double)fragment.size(), fragments2Variables.get(fragment));
		}
		budgetConstraint = ilp.addConstr(expression, GRB.LESS_EQUAL, defaultBudget, "budget");
	}

	private void defineObjectiveFunction() throws DatabaseConnectionIsNotOpen, GRBException {
		GRBLinExpr expr = new GRBLinExpr();
		lattice.getData().open();
		
		for (Fragment fragment : lattice) {
			if (fragment.isMetadata() || fragment.isRoot()) {
				expr.addTerm(1.0, fragments2Variables.get(fragment));
			} else {
				// Check the s-s join fragments
				Set<Fragment> joinCandidates = 
						lattice.ssjoinCandidates(fragment);
			
				double nCandidates = joinCandidates.size() + 1.0;
				double joinTerm = 1.0;
				for (Fragment joinCandidate : joinCandidates) {
					joinTerm += lattice.getData().joinCount(fragment.getSignatures(), joinCandidate.getSignatures());
				}
				expr.addTerm(joinTerm * nCandidates, fragments2Variables.get(fragment));
 			}
		}
		lattice.getData().close();
		ilp.setObjective(expr, GRB.MAXIMIZE);
	}

	/**
	 * Defines an integral variable per fragment in the lattice
	 * @throws GRBException
	 */
	private void defineVariables() throws GRBException {
		// There will be one variable per fragment
		for (Fragment fragment : lattice) {
			fragments2Variables.put(fragment, 
					ilp.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x" + fragment.getId()));
		}
		
	}

	
	public boolean isConsiderMetadataAncestors() {
		return considerMetadataAncestors;
	}

	public void setConsiderMetadataAncestors(boolean considerMetadataAncestors) {
		this.considerMetadataAncestors = considerMetadataAncestors;
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
		try {
			budgetConstraint.set(GRB.DoubleAttr.RHS, (double) budget);
			ilp.optimize();
			
			for (Fragment fragment : fragments2Variables.keySet()) {
				GRBVar variable = fragments2Variables.get(fragment);
				double assignment = variable.get(GRB.DoubleAttr.X);
				if (assignment == 1.0) {
					selected.add(fragment);
				}
			}
			
			/**if (logFile != null)
				ilp.write(logFile);**/
			

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
