package dk.aau.cs.qweb.pec;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

import dk.aau.cs.qweb.pec.QueryEvaluation.AnalyticalQuery;
import dk.aau.cs.qweb.pec.QueryEvaluation.JenaMaterializedFragment;
import dk.aau.cs.qweb.pec.QueryEvaluation.MaterializedFragments;
import dk.aau.cs.qweb.pec.data.InMemoryRDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.JenaTDBDatabaseConnection;
import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.exceptions.UnsupportedDatabaseTypeException;
import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.fragmentsselector.FragmentsSelector;
import dk.aau.cs.qweb.pec.fragmentsselector.GreedyFragmentsSelector;
import dk.aau.cs.qweb.pec.lattice.Lattice;
import dk.aau.cs.qweb.pec.lattice.NaiveLatticeBuilder;
import dk.aau.cs.qweb.pec.types.Signature;
import gurobi.GRBException;

public class Experiment {
	
	private RDFCubeDataSource data;
	private RDFCubeStructure structure;
	private Lattice lattice;
	private MaterializedFragments materializedFragments;
	
	public Experiment() throws IOException, UnsupportedDatabaseTypeException, DatabaseConnectionIsNotOpen, GRBException, ParseException {
		data = constructDataStore();
		long timea = System.currentTimeMillis();
		structure = RDFCubeStructure.build(Config.getCubeStructureLocation());
		System.out.println("Loading schema took " + (System.currentTimeMillis() - timea) + " ms");
		//System.out.println(structure);
		NaiveLatticeBuilder builder = new NaiveLatticeBuilder();
		timea = System.currentTimeMillis();				
		lattice = builder.build(data, structure);
		System.out.println("Building lattice took " + (System.currentTimeMillis() - timea) + " ms");
		//System.out.println(lattice);
		timea = System.currentTimeMillis();
		FragmentsSelector greedySelector = new GreedyFragmentsSelector(lattice, Config.getGreedyLogLocation());
		Set<Fragment> selectedFragments = greedySelector.select(Config.getBudget());
		System.out.println("Fragments selection took " + (System.currentTimeMillis() - timea) + " ms");
		//FragmentsSelector ilpSelector = new ILPFragmentsSelector(lattice, Config.getILPLogLocation());
		//FragmentsSelector ilpImprovedSelector = new ImprovedILPFragmentsSelector(lattice, Config.getILPLogLocation());
		//FragmentsSelector naiveSelector = new NaiveFragmentsSelector(lattice, Config.getNaiveLogLocation());
		timea = System.currentTimeMillis();
		materializedFragments = new JenaMaterializedFragment(selectedFragments);
		System.out.println("Materialization took " + (System.currentTimeMillis() - timea) + " ms");
		
		//System.out.println("[Greedy]: " + greedySelector.select(Config.getBudget()));
		//System.out.println("[ILP]: " + ilpSelector.select(Config.getBudget()));
		//System.out.println("[Improved ILP]: " + ilpImprovedSelector.select(Config.getBudget()));
		//System.out.println("[Naive]: " + naiveSelector.select(Config.getBudget()));		
	}
	
	private RDFCubeDataSource constructDataStore() throws IOException, UnsupportedDatabaseTypeException {
		if (Config.getDatabaseType().equals("inMemory")) {
			return InMemoryRDFCubeDataSource.build(Config.getInstanceDataLocation()); 
		} else if (Config.getDatabaseType().equals("tdb")) {
			return JenaTDBDatabaseConnection.build(Config.getInstanceDataLocation());
		}
		throw new UnsupportedDatabaseTypeException();
	}

	public void run() throws DatabaseConnectionIsNotOpen, IOException {
		Set<AnalyticalQuery> analyticalQueries = getAnalyticalQueries();
		Set<ProvenanceQuery> provenanceQueries = getProvenanceQueries();
		ResultFactory resultFactory = new JenaResultFactory(Config.getResultLogLocation());
		
		for (ProvenanceQuery provenanceQuery : provenanceQueries) {
			System.out.println(provenanceQuery.getFilename());
			Set<String> provenanceIdentifiers =  resultFactory.evaluate(provenanceQuery); 
			System.out.println(resultFactory.evaluate(provenanceIdentifiers));
			
			for (AnalyticalQuery analyticalQuery : analyticalQueries) {
				for (Signature partialTriplePatternSignature : analyticalQuery.getTriplePatterns()) {
					Set<Fragment> allFragments = lattice.getFragmentsForRelation(partialTriplePatternSignature.getPredicate());
					Set<Fragment> requiredFragments = removeFragmentsNotAllowedByProvenanceQuery(allFragments,provenanceIdentifiers);
					System.out.println("Basic Triple Pattern "+partialTriplePatternSignature);
					System.out.println(requiredFragments);
						
					for (Fragment fragment : requiredFragments) {
						
						if(materializedFragments.contains(fragment)) {
							analyticalQuery.addFrom(materializedFragments.getFragmentURL(fragment));
						} else {
							Set<Fragment> ancestors = lattice.getAncestors(fragment);
							for (Fragment ancestor : ancestors) {
								if (materializedFragments.contains(ancestor)) {
									analyticalQuery.addFrom(materializedFragments.getFragmentURL(ancestor));
								} else {
									analyticalQuery.addFrom(fragment.getProvenanceIdentifers());
								}
							}
						}
					}
				}
				resultFactory.evaluate(materializedFragments,analyticalQuery);
			}
		}
		
	}

	private Set<Fragment> removeFragmentsNotAllowedByProvenanceQuery(Set<Fragment> allFragments,
			Set<String> provenanceIdentifiers) {
		Set<Fragment> allowedFragments = new HashSet<Fragment>();
		for (Fragment fragment : allFragments) {
			if (provenanceIdentifiers.containsAll(fragment.getProvenanceIdentifers())){
				allowedFragments.add(fragment);
			}
		}
		return allowedFragments;
	}

	private Set<ProvenanceQuery> getProvenanceQueries() throws IOException {
		File folder = new File(Config.getProvenanceQueryPath());
		Set<ProvenanceQuery> queries = new HashSet<ProvenanceQuery>();
		for (File queryFile : folder.listFiles()) {
			queries.add(new ProvenanceQuery(queryFile));
		}
		return queries;
	}

	private Set<AnalyticalQuery> getAnalyticalQueries() throws IOException {
		File folder = new File(Config.getAnalyticalQueryPath());
		Set<AnalyticalQuery> queries = new HashSet<AnalyticalQuery>();
		for (File queryFile : folder.listFiles()) {
			queries.add(new AnalyticalQuery(queryFile, structure));
		}
		return queries;
	}
}
