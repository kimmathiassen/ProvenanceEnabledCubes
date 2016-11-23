package dk.aau.cs.qweb.pec.lattice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import dk.aau.cs.qweb.pec.data.InMemoryRDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.types.Signature;

public class LatticeTest {

	static Lattice inmutableLattice;
	
	static Lattice mutableLattice;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		NaiveLatticeBuilder builder = new NaiveLatticeBuilder();
		List<String[]> schemaTriples = new ArrayList<>();
		schemaTriples.add(new String[]{"measure1", "rdfs:domain", "Observation"});
		schemaTriples.add(new String[]{"measure2", "rdfs:domain", "Observation"});
		schemaTriples.add(new String[]{"measure1", "rdfs:range", "int"});
		schemaTriples.add(new String[]{"measure2", "rdfs:range", "int"});
		schemaTriples.add(new String[]{"measure1", "rdf:type", "FactualRelation"});
		schemaTriples.add(new String[]{"measure2", "rdf:type", "FactualRelation"});
		schemaTriples.add(new String[]{"dim1", "rdf:type", "CubeRelation"});
		schemaTriples.add(new String[]{"dim1", "rdfs:domain", "Observation"});
		schemaTriples.add(new String[]{"dim1", "rdfs:range", "Object"});

		RDFCubeStructure schema = RDFCubeStructure.build(schemaTriples);
		List<String[]> quadruples = new ArrayList<>();
		
		quadruples.add(new String[]{":obs1", "measure1", "5", ":A"});
		quadruples.add(new String[]{":obs1", "measure2", "6", ":A"});
		quadruples.add(new String[]{":obs1", "dim1", ":obj11", ":ETL1"});
		
		quadruples.add(new String[]{":obs2", "measure1", "5", ":B"});
		quadruples.add(new String[]{":obs2", "measure2", "6", ":C"});
		quadruples.add(new String[]{":obs2", "dim1", ":obj12", ":ETL2"});

		
		RDFCubeDataSource source = InMemoryRDFCubeDataSource.build(quadruples); 
		inmutableLattice = builder.build(source, schema);
		mutableLattice = builder.build(source, schema);
		System.out.println(inmutableLattice);
	}
	
	@Test
	public void testLatticeBuild() {
		assertNotNull(inmutableLattice);
	}
	
	@Test
	public void testSize() {
		assertEquals(12, inmutableLattice.size());
	}
	
	@Test 
	public void testSubjectColocation() {
		Fragment f = inmutableLattice.getFragmentBySignature(new Signature<>(null, null, null, ":A"));
		assertNotNull(f);
		Set<Fragment> metadata = inmutableLattice.getMetadataFragments(f);
		assertEquals(2, metadata.size());
		Set<String> relations = new LinkedHashSet<>();
		Set<String> provids = new LinkedHashSet<>();
		for (Fragment metaFragment : metadata) {
			relations.add(metaFragment.getSomeSignature().getSecond());
			provids.add(metaFragment.getSomeSignature().getFourth());
		}
		assertTrue(relations.contains("dim1"));
		assertTrue(provids.contains(":ETL1"));		
		assertTrue(provids.contains(":ETL2"));
	}

	@Test
	public void testGetAncestorPaths() {
		assertEquals(12, mutableLattice.size());
		Fragment fa = mutableLattice.getFragmentBySignature(new Signature<>("Observation", "measure1", "int", ":A"));
		Fragment fb = mutableLattice.getFragmentBySignature(new Signature<>("Observation", "measure1", "int", ":B"));
		assertTrue(mutableLattice.createNewParent(fa, fb));
		System.out.println(mutableLattice);
		assertEquals(13, mutableLattice.size());
		Fragment leaf =  mutableLattice.getFragmentBySignature(new Signature<>("Observation", "measure1", "int", ":A"));
		List<List<Fragment>> ancestorPaths = mutableLattice.getAncestorPaths(leaf);
		assertEquals(2, ancestorPaths.size());
		System.out.println(ancestorPaths);
	}

}
