package dk.aau.cs.qweb.pec.lattice;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Iterator;
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

	static Lattice lattice;
	
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
		quadruples.add(new String[]{":obs2", "measure2", "6", ":B"});
		quadruples.add(new String[]{":obs2", "dim1", ":obj12", ":ETL2"});

		
		RDFCubeDataSource source = InMemoryRDFCubeDataSource.build(quadruples); 
		lattice = builder.build(source, schema);
		System.out.println(lattice);
	}
	
	@Test
	public void testLatticeBuild() {
		assertNotNull(lattice);
	}
	
	@Test
	public void testSize() {
		assertEquals(11, lattice.size());
	}
	
	@Test 
	public void testSubjectColocation() {
		Fragment f = lattice.getFragmentBySignature(new Signature<>(null, null, null, ":A"));
		assertNotNull(f);
		Set<Fragment> metadata = lattice.getMetadataFragments(f);
		assertEquals(1, metadata.size());
		Iterator<Fragment> fragmentIt = metadata.iterator();
		Fragment metaF = fragmentIt.next();
		System.out.println(metaF);
		assertSame(metaF.getSomeSignature().getSecond(), "dim1");
	}

	@Test
	public void testGetAncestorPaths() {
		fail("Not yet implemented");
	}

}
