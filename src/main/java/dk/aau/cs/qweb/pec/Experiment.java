package dk.aau.cs.qweb.pec;

import java.io.IOException;

import dk.aau.cs.qweb.pec.data.InMemoryRDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.exceptions.UnsupportedDatabaseTypeException;
import dk.aau.cs.qweb.pec.lattice.Lattice;
import dk.aau.cs.qweb.pec.lattice.NaiveLatticeBuilder;

public class Experiment {
	
	private RDFCubeDataSource data;
	private RDFCubeStructure structure;
	private Lattice lattice;
	
	public Experiment() throws IOException, UnsupportedDatabaseTypeException {
		data = constructDateStore();
		structure = RDFCubeStructure.build(Config.getCubeStructureLocation());
		NaiveLatticeBuilder builder = new NaiveLatticeBuilder();
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
