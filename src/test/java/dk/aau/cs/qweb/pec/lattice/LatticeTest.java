package dk.aau.cs.qweb.pec.lattice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.ext.com.google.common.collect.Sets;
import org.junit.BeforeClass;
import org.junit.Test;

import dk.aau.cs.qweb.pec.data.InMemoryRDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.types.Signature;

public class LatticeTest {
	
	static Lattice lattice;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
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
		lattice = LatticeBuilder.build(source, schema, null);
		System.out.println(lattice);
	}
	
	@Test
	public void testLatticeBuild() {
		assertNotNull(lattice);
	}
	
	@Test
	public void testSize() {
		assertEquals(12, lattice.size());
	}
	
	@Test
	public void testRedundancy() {
		Fragment fa = lattice.getFragmentBySignature(Sets.newHashSet(new Signature(null, "measure1", null, ":B")));
		assertFalse(fa.isRedundant());
		fa = lattice.getFragmentBySignature(Sets.newHashSet(new Signature(null, "measure2", null, ":C")));
		assertFalse(fa.isRedundant());
		fa = lattice.getFragmentBySignature(Sets.newHashSet(new Signature(null, "dim1", null, ":ETL1")));
		assertFalse(fa.isRedundant());
		fa = lattice.getFragmentBySignature(Sets.newHashSet(new Signature(null, "dim1", null, ":ETL2")));
		assertFalse(fa.isRedundant());
		fa = lattice.getFragmentBySignature(Sets.newHashSet(new Signature(null, "measure1", null, ":A")));
		assertFalse(fa.isRedundant());
		fa = lattice.getFragmentBySignature(Sets.newHashSet(new Signature(null, null, null, ":B")));
		assertTrue(fa.isRedundant());
		fa = lattice.getFragmentBySignature(Sets.newHashSet(new Signature(null, null, null, ":C")));
		assertTrue(fa.isRedundant());
		fa = lattice.getFragmentBySignature(Sets.newHashSet(new Signature(null, null, null, ":ETL2")));
		assertTrue(fa.isRedundant());
		
	}
	

	@Test
	public void testGetAncestorPaths() {
		Fragment fa = lattice.getFragmentBySignature(Sets.newHashSet(new Signature(null, "measure1", null, ":B")));
		List<List<Fragment>> paths = lattice.getAncestorPaths(fa);
		assertEquals(paths.size(), 1);
		assertEquals(paths.get(0).size(), 3);
		
		fa = lattice.getFragmentBySignature(Sets.newHashSet(new Signature(null, null, null, ":A")));
		paths = lattice.getAncestorPaths(fa);
		assertEquals(paths.size(), 1);
		assertEquals(paths.get(0).size(), 2);
	}

}
