package dk.aau.cs.qweb.pec.lattice;

import static org.junit.Assert.assertEquals;
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

	static Lattice inmutableLattice;
	
	static Lattice mutableLattice;
	
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
		inmutableLattice = LatticeBuilder.build(source, schema,"noMerge");
		mutableLattice = LatticeBuilder.build(source, schema,"noMerge");
		System.out.println(inmutableLattice);
	}
	
	@Test
	public void testLatticeBuild() {
		assertNotNull(inmutableLattice);
	}
	
	@Test
	public void testSize() {
		assertEquals(17, inmutableLattice.size());
	}
	
	@Test
	public void testRedundancy() {
		Fragment fa = mutableLattice.getFragmentBySignature(Sets.newHashSet(new Signature(":obs1", "measure1", null, ":A")));
		assertTrue(fa.isRedundant());
	}
	

	@Test
	public void testGetAncestorPaths() {
		assertEquals(17, mutableLattice.size());
		Fragment fa = mutableLattice.getFragmentBySignature(Sets.newHashSet(new Signature(null, "measure1", null, ":A")));
		Fragment fb = mutableLattice.getFragmentBySignature(Sets.newHashSet(new Signature(null, "measure1", null, ":B")));
		assertTrue(mutableLattice.createNewParent(fa, fb));
		System.out.println(mutableLattice);
		assertEquals(18, mutableLattice.size());
		Fragment leaf =  mutableLattice.getFragmentBySignature(Sets.newHashSet(new Signature(null, "measure1", null, ":A")));
		List<List<Fragment>> ancestorPaths = mutableLattice.getAncestorPaths(leaf);
		assertEquals(2, ancestorPaths.size());
		System.out.println(ancestorPaths);
	}

}
