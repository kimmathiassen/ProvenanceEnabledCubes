package dk.aau.cs.qweb.pec;

import java.io.IOException;

import dk.aau.cs.qweb.pec.data.InMemoryRDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.exceptions.UnsupportedDatabaseTypeException;
import dk.aau.cs.qweb.pec.lattice.Lattice;
import dk.aau.cs.qweb.pec.lattice.NaiveLatticeBuilder;
import gurobi.GRBException;
import dk.aau.cs.qweb.pec.fragmentsselector.GreedyFragmentsSelector;
import dk.aau.cs.qweb.pec.fragmentsselector.ILPFragmentsSelector;

public class Experiment {
	
	private RDFCubeDataSource data;
	private RDFCubeStructure structure;
	private Lattice lattice;
	
	public Experiment() throws IOException, UnsupportedDatabaseTypeException, DatabaseConnectionIsNotOpen, GRBException {
		data = constructDataStore();
		structure = RDFCubeStructure.build(Config.getCubeStructureLocation());
		System.out.println(structure);
		NaiveLatticeBuilder builder = new NaiveLatticeBuilder();
		lattice = builder.build(data, structure);
		System.out.println(lattice);
		GreedyFragmentsSelector greedySelector = new GreedyFragmentsSelector(lattice, Config.getGreedyLogFile());
		System.out.println(greedySelector.select(10));		
		ILPFragmentsSelector ilpSelector = new ILPFragmentsSelector(lattice, Config.getILPLogFile());
		System.out.println(ilpSelector.select(10));
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
