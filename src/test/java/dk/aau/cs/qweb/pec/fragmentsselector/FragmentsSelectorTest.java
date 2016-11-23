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

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		NaiveLatticeBuilder builder = new NaiveLatticeBuilder();
		RDFCubeStructure schema = RDFCubeStructure.build(structureLocation);
		RDFCubeDataSource source = InMemoryRDFCubeDataSource.build(cubeLocation); 
		lattice = builder.build(source, schema);
		System.out.println(lattice);
	}

	@Test
	public void testGreedySelector() {
		GreedyFragmentsSelector greedy = new GreedyFragmentsSelector(lattice);
		greedy.setLoggingEnabled(false);
		Set<Fragment> selected = null;
		try {
			selected = greedy.select(3);
		} catch (DatabaseConnectionIsNotOpen e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		int budget = 3;
		// It should select one information triple fragment, including 2 metadata ones
		assertEquals(3, selected.size());
		// Check it did not exceed the budget
		assertTrue(Fragment.aggregateSize(selected) <= budget);
		
		System.out.println("[Greedy] Selected with budget " + budget + ": " + selected);
		budget = 12;
		try {
			selected = greedy.select(budget);
		} catch (DatabaseConnectionIsNotOpen e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		System.out.println("[Greedy] Selected with budget " + budget + ": " + selected);
		// Greedy will not take either the metadata [null, null, null, provid] fragments
		// or the root. Size must be lattice - 3
		assertEquals(lattice.size() - 3, selected.size());
		// Check it did not exceed the budget
		assertTrue(Fragment.aggregateSize(selected) <= budget);
		assertFalse(selected.contains(lattice.getRoot()));
	}
	
	@Test
	public void testILPSelector() {
		ILPFragmentsSelector ilp = null;
		try {
			ilp = new ILPFragmentsSelector(lattice, ilpLogLocation);
		} catch (GRBException | DatabaseConnectionIsNotOpen | FileNotFoundException e) {
			fail();
			e.printStackTrace();
		}
		int budget = 3;
		Set<Fragment> selected = null;
		try {
			selected = ilp.select(budget);
		} catch (DatabaseConnectionIsNotOpen e) {
			fail();
			e.printStackTrace();
		}
		// It should select one information triple fragment, including 2 metadata ones
		assertEquals(3, selected.size());
		budget = (int) Fragment.aggregateSize(lattice);
		try {
			selected = ilp.select(budget);
		} catch (DatabaseConnectionIsNotOpen e) {
			e.printStackTrace();
			fail();
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
