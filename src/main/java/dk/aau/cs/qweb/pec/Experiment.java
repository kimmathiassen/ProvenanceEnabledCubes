package dk.aau.cs.qweb.pec;

import java.io.IOException;
import java.text.ParseException;

import dk.aau.cs.qweb.pec.data.InMemoryRDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.exceptions.UnsupportedDatabaseTypeException;
import dk.aau.cs.qweb.pec.fragmentsselector.GreedyFragmentsSelector;
import dk.aau.cs.qweb.pec.fragmentsselector.ILPFragmentsSelector;
import dk.aau.cs.qweb.pec.fragmentsselector.NaiveFragmentsSelector;
import dk.aau.cs.qweb.pec.lattice.Lattice;
import dk.aau.cs.qweb.pec.lattice.NaiveLatticeBuilder;
import gurobi.GRBException;

public class Experiment {
	
	private RDFCubeDataSource data;
	private RDFCubeStructure structure;
	private Lattice lattice;
	
	public Experiment() throws IOException, UnsupportedDatabaseTypeException, DatabaseConnectionIsNotOpen, GRBException, ParseException {
		data = constructDataStore();
		structure = RDFCubeStructure.build(Config.getCubeStructureLocation());
		System.out.println(structure);
		NaiveLatticeBuilder builder = new NaiveLatticeBuilder();
		lattice = builder.build(data, structure);
		System.out.println(lattice);
		GreedyFragmentsSelector greedySelector = new GreedyFragmentsSelector(lattice, Config.getGreedyLogLocation());
		System.out.println("Fragments selected by greedy approach: " + greedySelector.select(Config.getBudget()));		
		ILPFragmentsSelector ilpSelector = new ILPFragmentsSelector(lattice, Config.getILPLogLocation());
		System.out.println("Fragments selected by ILP approach: " + ilpSelector.select(Config.getBudget()));
		NaiveFragmentsSelector naiveSelector = new NaiveFragmentsSelector(lattice, Config.getNaiveLogLocation());
		System.out.println("Fragments selected by naive approach: " + naiveSelector.select(Config.getBudget()));		
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
