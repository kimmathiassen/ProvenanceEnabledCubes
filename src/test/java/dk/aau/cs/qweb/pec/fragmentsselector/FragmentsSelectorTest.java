package dk.aau.cs.qweb.pec.fragmentsselector;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import dk.aau.cs.qweb.pec.data.InMemoryRDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.lattice.Lattice;
import dk.aau.cs.qweb.pec.lattice.NaiveLatticeBuilder;
import gurobi.GRBException;

public class FragmentsSelectorTest {
	
	static final String structureLocation = "src/test/test.schema.tsv";
	
	static final String cubeLocation = "src/test/test.cube.tsv";
	
	static final String ilpLogLocation = "src/test/logs/test.ilp.log";
	
	static Lattice lattice;
	
	static int[] budgets;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		NaiveLatticeBuilder builder = new NaiveLatticeBuilder();
		RDFCubeStructure schema = RDFCubeStructure.build(structureLocation);
		RDFCubeDataSource source = InMemoryRDFCubeDataSource.build(cubeLocation); 
		lattice = builder.build(source, schema);
		System.out.println(lattice);
		budgets = new int[]{3, 12, (int) Fragment.aggregateSize(lattice)};
	}

	@Test
	public void testGreedySelector() {
		GreedyFragmentsSelector greedy = new GreedyFragmentsSelector(lattice);
		greedy.setLoggingEnabled(false);
		Set<Fragment> selected = null;
		for (int budget : budgets) {
			try {
				selected = greedy.select(budget);
			} catch (DatabaseConnectionIsNotOpen e) {
				System.err.println("Budget: " + budget);
				e.printStackTrace();
				fail();
			}
			System.out.println("[Greedy] Selected with budget " + budget + ": " + selected);
			// It should select one information triple fragment, including 2 metadata ones
			if (budget == 3)
				assertEquals(3, selected.size());
			
			// Check it did not exceed the budget
			assertTrue(Fragment.aggregateSize(selected) <= budget);		
			assertFalse(selected.contains(lattice.getRoot()));
			checkColocation(selected);
		}
	}
	
	private void checkColocation(Set<Fragment> selected) {
		for (Fragment infoFragment: selected) {
			if (infoFragment.containsInfoTriples() && !infoFragment.containsMetadata()) {
				Set<Fragment> metaFragments = lattice.getMetadataFragments(infoFragment);
				for (Fragment metaFragment : lattice.getMetadataFragments(infoFragment)) {
					metaFragments.addAll(lattice.getAncestors(metaFragment));
				}
				int size = metaFragments.size();
				// We should be able to remove at least one fragment.
				// This means the selected contains at least one of the metadata fragments
				// for this information fragment
				metaFragments.removeAll(selected);
				assertTrue(metaFragments.size() < size);
			}
		}
		
	}

	@Test
	public void testILPSelector() {
		ILPFragmentsSelector ilp = null;
		Set<Fragment> selected = null;
		try {
			ilp = new ILPFragmentsSelector(lattice, ilpLogLocation);
		} catch (GRBException | DatabaseConnectionIsNotOpen | FileNotFoundException e) {
			fail();
			e.printStackTrace();
		}
		
		for (int budget : budgets) {
			try {
				selected = ilp.select(budget);
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
			checkColocation(selected);
		}
	}
	
	@Test
	public void testImprovedILPSelector() {
		ImprovedILPFragmentsSelector improvedILP = null;
		Set<Fragment> selected = null;
		try {
			improvedILP = new ImprovedILPFragmentsSelector(lattice, ilpLogLocation);
		} catch (FileNotFoundException | GRBException | DatabaseConnectionIsNotOpen e) {
			e.printStackTrace();
			fail();
		}
		
		for (int budget : budgets) {
			try {
				selected = improvedILP.select(budget);
			} catch (DatabaseConnectionIsNotOpen e) {
				fail();
				e.printStackTrace();
			}
			System.out.println("[Improved ILP] Selected with budget " + budget + ": " + selected);
			// Budget constraint
			assertTrue(Fragment.aggregateSize(selected) <= budget);
			// Redundancy w.r.t parents
			for (Fragment f : selected) {
				Set<Fragment> ancestors = lattice.getAncestors(f);
				for (Fragment fa : ancestors) {
					assertFalse(selected.contains(fa));
				}
			}
			checkColocation(selected);
		}
		
	}
	
	@Test
	public void testNaiveSelector() {
		NaiveFragmentsSelector naive = new NaiveFragmentsSelector(lattice);
		Set<Fragment> selected = null;
		
		for (int budget : budgets) {
			try {
				selected = naive.select(budget);
			} catch (DatabaseConnectionIsNotOpen e) {
				fail();
				e.printStackTrace();
			}
			
			System.out.println("[Naive] Selected with budget " + budget + ": " + selected);
			// Budget constraint
			assertTrue(Fragment.aggregateSize(selected) <= budget);
			checkColocation(selected);
		}
	}
}