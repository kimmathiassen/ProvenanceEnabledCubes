package dk.aau.cs.qweb.pec.fragmentsselector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import dk.aau.cs.qweb.pec.data.InMemoryRDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.fragmentsSelector.GreedyFragmentsSelector;
import dk.aau.cs.qweb.pec.fragmentsSelector.ILPFragmentsSelector;
import dk.aau.cs.qweb.pec.fragmentsSelector.NaiveFragmentsSelector;
import dk.aau.cs.qweb.pec.lattice.Lattice;
import dk.aau.cs.qweb.pec.lattice.LatticeBuilder;
import dk.aau.cs.qweb.pec.logger.Logger;
import gurobi.GRBException;

public class FragmentsSelectorTest {
	
	static final String structureLocation = "src/test/test.schema.tsv";
	
	static final String cubeLocation = "src/test/test.cube.tsv";
	
	static final String ilpLogLocation = "src/test/logs/test.ilp.log";
	
	static Lattice lattice;
	static Logger testLogger;
	static int[] budgets;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		testLogger = new Logger();
		RDFCubeStructure schema = RDFCubeStructure.build(structureLocation);
		RDFCubeDataSource source = InMemoryRDFCubeDataSource.build(cubeLocation); 
		lattice = LatticeBuilder.build(source, schema, null);
		System.out.println(lattice);
		budgets = new int[]{3, 12, (int) Fragment.aggregateSize(lattice)};
		GreedyFragmentsSelector.setMininumFragmentSize(1);
	}

	@Test
	public void testGreedySelector() {
		
		GreedyFragmentsSelector greedy = new GreedyFragmentsSelector(lattice);
		greedy.setLoggingEnabled(false);
		Set<Fragment> selected = null;
		for (int budget : budgets) {
			try {
				selected = greedy.select(budget,testLogger);
			} catch (DatabaseConnectionIsNotOpen e) {
				System.err.println("Budget: " + budget);
				e.printStackTrace();
				fail();
			}
			System.out.println("[Greedy] Selected with budget " + budget + ": " + selected);
			// It should select one information triple fragment, including 2 metadata ones
			if (budget == 3)
				assertEquals(2, selected.size());
			
			// Check it did not exceed the budget
			assertTrue(Fragment.aggregateSize(selected) <= budget);
		}
	}

	@Test
	public void testILPSelector() {
		ILPFragmentsSelector ilp = null;
		Set<Fragment> selected = null;
		try {
			ilp = new ILPFragmentsSelector(lattice, ilpLogLocation, false);
		} catch (GRBException | DatabaseConnectionIsNotOpen | FileNotFoundException e) {
			fail();
			e.printStackTrace();
		}
		
		for (int budget : budgets) {
			try {
				selected = ilp.select(budget,testLogger);
			} catch (DatabaseConnectionIsNotOpen e) {
				fail();
				e.printStackTrace();
			}
			System.out.println("[ILP] Selected with budget " + budget + ": " + selected);
			// Budget constraint
			assertTrue(Fragment.aggregateSize(selected) <= budget);
			// Redundancy w.r.t parents
			for (Fragment f : selected) {
				Set<Fragment> ancestors = lattice.getAncestors(f);
				for (Fragment fa : ancestors) {
					assertFalse(selected.contains(fa));
				}
			}
		}
	}
	
	
	@Test
	public void testNaiveSelector() {
		NaiveFragmentsSelector naive = new NaiveFragmentsSelector(lattice);
		Set<Fragment> selected = null;
		
		for (int budget : budgets) {
			try {
				selected = naive.select(budget,testLogger);
			} catch (DatabaseConnectionIsNotOpen e) {
				fail();
				e.printStackTrace();
			}
			
			System.out.println("[Naive] Selected with budget " + budget + ": " + selected);
			// Budget constraint
			assertTrue(Fragment.aggregateSize(selected) <= budget);
		}
	}
}
