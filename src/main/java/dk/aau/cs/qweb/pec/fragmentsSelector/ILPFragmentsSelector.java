package dk.aau.cs.qweb.pec.fragmentsSelector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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

	protected GRBModel ilp;
	
	protected double defaultBudget = 1.0;
	
	protected GRBConstr budgetConstraint;
	
	protected Map<Fragment, GRBVar> fragments2Variables;
	
	public ILPFragmentsSelector(Lattice lattice) throws GRBException, DatabaseConnectionIsNotOpen {
		super(lattice);
		GRBEnv env = new GRBEnv();
		ilp = new GRBModel(env);
		fragments2Variables = new LinkedHashMap<>();
		populateModel();
	}
	
	public ILPFragmentsSelector(Lattice lattice, String logFile) throws FileNotFoundException, GRBException, DatabaseConnectionIsNotOpen {
		super(lattice, logFile);
		GRBEnv env = new GRBEnv(this.logFile);		
		ilp = new GRBModel(env);
		fragments2Variables = new LinkedHashMap<>();
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
		defineMaterializeMeasuresConstraint();
	}

	protected void defineAncestorsRedundancyConstraint() throws GRBException {
		for (Fragment fragment : lattice) {			
			if (lattice.isRoot(fragment) || fragment.isRedundant())
				continue;
			
			List<List<Fragment>> ancestorsPaths = lattice.getAncestorPaths(fragment);
			int pathIdx = 0;
			for (List<Fragment> path : ancestorsPaths) {
				path.remove(fragment);
				GRBLinExpr expression = new GRBLinExpr();
				expression.addTerm(1.0, fragments2Variables.get(fragment));
				for (Fragment ancestor : path) {
					if (fragments2Variables.containsKey(ancestor)) {
						expression.addTerm(1.0, fragments2Variables.get(ancestor));
					}
				}
				ilp.addConstr(expression, GRB.LESS_EQUAL, 1.0, "redundancy_" + fragment.getShortName() + "_" + pathIdx);
				++pathIdx;
			}
		}
	}

	protected void defineMaterializeMeasuresConstraint() throws GRBException {
		Set<Fragment> measureFragments = lattice.getMeasureFragments();
		GRBLinExpr expression = new GRBLinExpr();
		for (Fragment fragment : measureFragments) {
			if (fragments2Variables.containsKey(fragment)) {
				expression.addTerm(1.0, fragments2Variables.get(fragment));
			}
		}
		ilp.addConstr(expression, GRB.GREATER_EQUAL, 1.0, "measures");
	}
	
	protected void defineBudgetConstraint() throws GRBException {
		GRBLinExpr expression = new GRBLinExpr();
		for (Fragment fragment : fragments2Variables.keySet()) {
			expression.addTerm((double)fragment.size(), fragments2Variables.get(fragment));
		}
		budgetConstraint = ilp.addConstr(expression, GRB.LESS_EQUAL, defaultBudget, "budget");
	}

	protected void defineObjectiveFunction() throws DatabaseConnectionIsNotOpen, GRBException {
		GRBLinExpr expr = new GRBLinExpr();
		lattice.getData().open();
		
		for (Fragment fragment : lattice) {
			if (fragment.isRedundant()) continue;
				
			if (lattice.isRoot(fragment)) {
				expr.addTerm(1.0, fragments2Variables.get(fragment));
			} else {
				double term = fragment.size();
				expr.addTerm(term, fragments2Variables.get(fragment));
 			}
		}
		lattice.getData().close();
		ilp.setObjective(expr, GRB.MAXIMIZE);
	}

	/**
	 * Defines an integral variable per non-redundant fragment in the lattice
	 * @throws GRBException
	 */
	protected void defineVariables() throws GRBException {
		// There will be one variable per fragment
		for (Fragment fragment : lattice) {
			if (!fragment.isRedundant()) {
				fragments2Variables.put(fragment, 
						ilp.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x" + fragment.getId()));
			}
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
		if (budget == 0) {
			// If budget is 0 then the model is infeasible
			// select nothing
			return Collections.emptySet();
		}
		
		lattice.getData().open();
		Set<Fragment> selected = new LinkedHashSet<>();
		try {
			budgetConstraint.set(GRB.DoubleAttr.RHS, (double) budget);

			ilp.optimize();
			
			if (ilp.get(GRB.IntAttr.Status) == GRB.OPTIMAL) {
				for (Fragment fragment : fragments2Variables.keySet()) {
					GRBVar variable = fragments2Variables.get(fragment);
					double assignment = variable.get(GRB.DoubleAttr.X);
					if (assignment == 1.0) {
						selected.add(fragment);
					}
				}
			} else {
				ilp.computeIIS();
			}
			
			if (logFile != null && loggingEnabled)
				dumpModel();


		} catch (GRBException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			lattice.getData().close();
			return null;
		}		
		
		lattice.getData().close();			
		return selected;
	}

	private void dumpModel() throws IOException, GRBException {
		File tmpFile = File.createTempFile("ilp", ".lp");
		tmpFile.deleteOnExit();
		ilp.write(tmpFile.getAbsolutePath());
		String model = new String(Files.readAllBytes(Paths.get(tmpFile.getAbsolutePath())), 
				StandardCharsets.UTF_8);
		
	    FileWriter fw = new FileWriter(logFile, true);
		fw.write("\nILP model\n");
		fw.write(model);
		fw.close();
	}
}
