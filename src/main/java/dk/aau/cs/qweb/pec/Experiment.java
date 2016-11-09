package dk.aau.cs.qweb.pec;

import java.io.IOException;

import dk.aau.cs.qweb.pec.data.InMemoryRDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.exceptions.UnsupportedDatabaseTypeException;
import dk.aau.cs.qweb.pec.rdfcube.fragmentsselector.GreedyFragmentsSelector;
import dk.aau.cs.qweb.pec.rdfcube.lattice.ExampleFragmentLatticeBuilder;
import dk.aau.cs.qweb.pec.rdfcube.lattice.FragmentLattice;

public class Experiment {
	
	private RDFCubeDataSource data;
	private RDFCubeStructure structure;
	private FragmentLattice lattice;
	
	public Experiment() throws IOException, UnsupportedDatabaseTypeException {
		data = constructDataStore();
		structure = RDFCubeStructure.build(Config.getCubeStructureLocation());
		System.out.println(structure);
		ExampleFragmentLatticeBuilder builder = new ExampleFragmentLatticeBuilder();
		lattice = builder.build(data, structure);
		System.out.println(lattice);
		GreedyFragmentsSelector selector = new GreedyFragmentsSelector();
		selector.select(lattice, 5);
	}
	
	private RDFCubeDataSource constructDataStore() throws IOException, UnsupportedDatabaseTypeException {
		if (Config.getDatabaseType().equals("inMemory")) {
			return InMemoryRDFCubeDataSource.build(Config.getInstanceDataLocation()); 
		}
		throw new UnsupportedDatabaseTypeException();
	}

	public void run() {
		// TODO Auto-generated method stub
	}
}
