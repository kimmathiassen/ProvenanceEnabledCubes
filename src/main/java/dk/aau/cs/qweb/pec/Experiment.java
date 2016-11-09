package dk.aau.cs.qweb.pec;

import java.io.IOException;

import dk.aau.cs.qweb.pec.data.InMemoryRDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.exceptions.UnsupportedDatabaseTypeException;
import dk.aau.cs.qweb.pec.rdfcube.lattice.ExampleFragmentLatticeBuilder;
import dk.aau.cs.qweb.pec.rdfcube.lattice.FragmentLattice;

public class Experiment {
	
	private RDFCubeDataSource data;
	private RDFCubeStructure structure;
	private FragmentLattice lattice;
	
	public Experiment() throws IOException, UnsupportedDatabaseTypeException {
		data = constructDateStore();
		structure = RDFCubeStructure.build(Config.getCubeStructureLocation());
		ExampleFragmentLatticeBuilder builder = new ExampleFragmentLatticeBuilder();
		lattice = builder.build(data, structure);
		System.out.println(lattice);
	}
	
	private RDFCubeDataSource constructDateStore() throws IOException, UnsupportedDatabaseTypeException {
		if (Config.getDatabaseType().equals("inMemory")) {
			return InMemoryRDFCubeDataSource.build(Config.getInstanceDataLocation()); 
		}
		throw new UnsupportedDatabaseTypeException();
	}

	public void run() {
		// TODO Auto-generated method stub
	}
}
