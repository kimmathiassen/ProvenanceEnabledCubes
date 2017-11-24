package dk.aau.cs.qweb.pec.lattice;

import java.util.Map;

import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.fragment.Fragment;


public class LatticeBuilder {

	static public Lattice buildEmptyLattice(RDFCubeDataSource dataSource, RDFCubeStructure schema) {
		Fragment root = Lattice.createFragment();
		return new NoMergeLattice(root, schema, dataSource);
	}
	
	static public Lattice build(RDFCubeDataSource dataSource, RDFCubeStructure schema, Map<String, String> conf) throws DatabaseConnectionIsNotOpen {
		Fragment root = Lattice.createFragment(); 
		Lattice lattice;		
		String latticeMergeStrategy = "noMerge";
		
		if (conf != null) {
			if (conf.containsKey("mergeStrategy"))
				latticeMergeStrategy = conf.get("mergeStrategy");
		}
		
		switch (latticeMergeStrategy) {
		case "noMerge":
			lattice = new NoMergeLattice(root, schema, dataSource);
			break;
		case "naive" :
			lattice = new NaiveMergeLattice(root, schema, dataSource);
			NaiveMergeLattice mergeLattice = (NaiveMergeLattice) lattice;
			if (conf.containsKey("maxFragmentsCount")) {
				mergeLattice.setMaxFragmentsCount(Integer.parseInt(conf.get("maxFragmentsCount")));
			}
			if (conf.containsKey("minFragmentsCount")) {
				mergeLattice.setMinFragmentsCount(Integer.parseInt(conf.get("minFragmentsCount")));
			}
			if (conf.containsKey("reduceRatio")) {
				mergeLattice.setReduceRatio(Float.parseFloat(conf.get("reduceRatio")));
			}
			break;
		case "property" :
			lattice = new PropertyMergeLattice(root, schema, dataSource);
			PropertyMergeLattice mergePropertyLattice = (PropertyMergeLattice) lattice;
			if (conf.containsKey("maxFragmentsCount")) {
				mergePropertyLattice.setMaxFragmentsCount(Integer.parseInt(conf.get("maxFragmentsCount")));
			}
			if (conf.containsKey("minFragmentsCount")) {
				mergePropertyLattice.setMinFragmentsCount(Integer.parseInt(conf.get("minFragmentsCount")));
			}
			if (conf.containsKey("reduceRatio")) {
				mergePropertyLattice.setReduceRatio(Float.parseFloat(conf.get("reduceRatio")));
			}
			break;
		case "provenance" :
			lattice = new ProvenanceMergeLattice(root, schema, dataSource);
			ProvenanceMergeLattice mergeProvenanceLattice = (ProvenanceMergeLattice) lattice;
			if (conf.containsKey("maxFragmentsCount")) {
				mergeProvenanceLattice.setMaxFragmentsCount(Integer.parseInt(conf.get("maxFragmentsCount")));
			}
			if (conf.containsKey("minFragmentsCount")) {
				mergeProvenanceLattice.setMinFragmentsCount(Integer.parseInt(conf.get("minFragmentsCount")));
			}
			
			if (conf.containsKey("reduceRatio")) {
				mergeProvenanceLattice.setReduceRatio(Float.parseFloat(conf.get("reduceRatio")));
			}
			break;
		case "mockup" :
			lattice = new MockupLattice(root, schema, dataSource);
			break;
		default:
			lattice = new NoMergeLattice(root, schema, dataSource);
			break;
		}
		
		
		try {
			if (!latticeMergeStrategy.equals("mockup")) {
				dataSource.open();
				
				while (dataSource.hasNext()) {
					lattice.registerTuple(dataSource.next());
				}
			}
			
		} finally {
			dataSource.close();
		}
		
		// If a parent and a child have the same number of triples, 
		// mark one of them as redundant
		lattice.markRedundantFragments();
			
		return lattice;
	}

}
