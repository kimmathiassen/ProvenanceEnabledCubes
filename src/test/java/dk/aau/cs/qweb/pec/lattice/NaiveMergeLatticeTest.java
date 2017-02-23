package dk.aau.cs.qweb.pec.lattice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.ext.com.google.common.collect.Sets;
import org.junit.BeforeClass;
import org.junit.Test;

import dk.aau.cs.qweb.pec.data.InMemoryRDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.types.Signature;

public class NaiveMergeLatticeTest {
	
	static NaiveMergeLattice originalLattice;
	
	static NaiveMergeLattice mergeLattice1;
	
	static NaiveMergeLattice mergeLattice2;
	
	static NaiveMergeLattice mergeLattice3;
	
	static NaiveMergeLattice mergeLattice4;
	
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
		
		// You can find the initial lattice under XXXXX
		
		quadruples.add(new String[]{":obs1", "measure1", "5", ":A"});
		quadruples.add(new String[]{":obs1", "measure2", "6", ":A"});
		quadruples.add(new String[]{":obs2", "measure1", "7", ":A"});
		quadruples.add(new String[]{":obs2", "measure2", "8", ":A"});
		quadruples.add(new String[]{":obs3", "measure1", "2", ":A"});
		quadruples.add(new String[]{":obs3", "measure2", "1", ":A"});
		quadruples.add(new String[]{":obs4", "measure1", "0", ":A"});
		quadruples.add(new String[]{":obs4", "measure2", "32", ":A"});
		quadruples.add(new String[]{":obs5", "measure1", "0", ":A"});
		quadruples.add(new String[]{":obs5", "measure2", "32", ":A"});
		
		quadruples.add(new String[]{":obs1", "dim1", ":obj11", ":ETL1"});
		quadruples.add(new String[]{":obs2", "dim1", ":obj12", ":ETL1"});
		quadruples.add(new String[]{":obs3", "dim1", ":obj13", ":ETL1"});
		quadruples.add(new String[]{":obs4", "dim1", ":obj14", ":ETL1"});
		quadruples.add(new String[]{":obs5", "dim1", ":obj15", ":ETL1"});
		
		quadruples.add(new String[]{":obs1", "dim2", ":obj21", ":ETL1"});
		quadruples.add(new String[]{":obs2", "dim2", ":obj22", ":ETL1"});
		quadruples.add(new String[]{":obs3", "dim2", ":obj23", ":ETL1"});
		quadruples.add(new String[]{":obs4", "dim2", ":obj24", ":ETL1"});
		quadruples.add(new String[]{":obs5", "dim2", ":obj25", ":ETL1"});

		
		quadruples.add(new String[]{":obs5", "measure1", "5", ":B"});
		quadruples.add(new String[]{":obs5", "measure2", "6", ":C"});
		quadruples.add(new String[]{":obs5", "dim1", ":obj12", ":ETL2"});

		
		RDFCubeDataSource source = InMemoryRDFCubeDataSource.build(quadruples); 
		Map<String, String> latticeConfMap = new LinkedHashMap<>();
		latticeConfMap.put("mergeStrategy", "naive");
		latticeConfMap.put("maxFragmentsCount", "13");
		latticeConfMap.put("minFragmentsCount", "10");
		// I know, the downcast is low :( but I could not find any solution without changing the design.
		originalLattice = (NaiveMergeLattice) LatticeBuilder.build(source, schema, latticeConfMap);

		latticeConfMap.put("maxFragmentsCount", "11");
		latticeConfMap.put("minFragmentsCount", "11");		
		mergeLattice1 = (NaiveMergeLattice) LatticeBuilder.build(source, schema, latticeConfMap);
		
		latticeConfMap.put("maxFragmentsCount", "11");
		latticeConfMap.put("minFragmentsCount", "9");
		mergeLattice2 = (NaiveMergeLattice) LatticeBuilder.build(source, schema, latticeConfMap);
		
		latticeConfMap.put("maxFragmentsCount", "11");
		latticeConfMap.put("minFragmentsCount", "7");
		mergeLattice3 = (NaiveMergeLattice) LatticeBuilder.build(source, schema, latticeConfMap);
		
		latticeConfMap.put("maxFragmentsCount", "11");
		latticeConfMap.put("minFragmentsCount", "5");
		mergeLattice4 = (NaiveMergeLattice) LatticeBuilder.build(source, schema, latticeConfMap);
		
		System.out.println(mergeLattice2);
	}
	
	@Test
	public void testLatticeBuild() {
		assertNotNull(originalLattice);
	}
	
	@Test
	public void testSize() {
		assertEquals(13, originalLattice.size());
		assertEquals(11, mergeLattice1.size());
		assertEquals(9, mergeLattice2.size());
		assertEquals(7, mergeLattice3.size());
		assertEquals(7, mergeLattice4.size());
		
	}
	
	@Test
	public void testRedundancy() {
		Fragment fa = originalLattice.getFragmentBySignature(Sets.newHashSet(new Signature(null, "measure1", null, ":B")));
		assertFalse(fa.isRedundant());
		fa = originalLattice.getFragmentBySignature(Sets.newHashSet(new Signature(null, "measure2", null, ":C")));
		assertFalse(fa.isRedundant());
		fa = originalLattice.getFragmentBySignature(Sets.newHashSet(new Signature(null, "dim1", null, ":ETL1")));
		assertFalse(fa.isRedundant());
		fa = originalLattice.getFragmentBySignature(Sets.newHashSet(new Signature(null, "dim1", null, ":ETL2")));
		assertFalse(fa.isRedundant());
		fa = originalLattice.getFragmentBySignature(Sets.newHashSet(new Signature(null, "measure1", null, ":A")));
		assertFalse(fa.isRedundant());
		fa = originalLattice.getFragmentBySignature(Sets.newHashSet(new Signature(null, null, null, ":B")));
		assertTrue(fa.isRedundant());
		fa = originalLattice.getFragmentBySignature(Sets.newHashSet(new Signature(null, null, null, ":C")));
		assertTrue(fa.isRedundant());
		fa = originalLattice.getFragmentBySignature(Sets.newHashSet(new Signature(null, null, null, ":ETL2")));
		assertTrue(fa.isRedundant());

		
		for (Fragment f : mergeLattice4) {
			assertFalse(f.isRedundant());
		}

	}
	

	@Test
	public void testGetAncestorPaths() {
		Fragment fa = originalLattice.getFragmentBySignature(Sets.newHashSet(new Signature(null, "measure2", null, ":C")));
		List<List<Fragment>> paths = originalLattice.getAncestorPaths(fa);
		assertEquals(paths.size(), 1);
		assertEquals(paths.get(0).size(), 3);
		
		fa = mergeLattice2.getFragmentBySignature(Sets.newHashSet(new Signature(null, "dim1", null, ":ETL1"), new Signature(null, "dim2", null, ":ETL1")));
		paths = mergeLattice2.getAncestorPaths(fa);
		assertEquals(paths.size(), 1);
		assertEquals(paths.get(0).size(), 3);
	}
}
