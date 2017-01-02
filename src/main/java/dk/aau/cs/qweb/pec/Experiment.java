package dk.aau.cs.qweb.pec;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import dk.aau.cs.qweb.pec.QueryEvaluation.AnalyticalQuery;
import dk.aau.cs.qweb.pec.QueryEvaluation.JenaMaterializedFragment;
import dk.aau.cs.qweb.pec.QueryEvaluation.JenaResultFactory;
import dk.aau.cs.qweb.pec.QueryEvaluation.MaterializedFragments;
import dk.aau.cs.qweb.pec.QueryEvaluation.ProvenanceQuery;
import dk.aau.cs.qweb.pec.data.InMemoryRDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.JenaTDBDatabaseConnection;
import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.exceptions.UnsupportedDatabaseTypeException;
import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.fragmentsselector.FragmentsSelector;
import dk.aau.cs.qweb.pec.fragmentsselector.GreedyFragmentsSelector;
import dk.aau.cs.qweb.pec.fragmentsselector.ILPFragmentsSelector;
import dk.aau.cs.qweb.pec.fragmentsselector.NaiveFragmentsSelector;
import dk.aau.cs.qweb.pec.lattice.Lattice;
import dk.aau.cs.qweb.pec.lattice.NaiveLatticeBuilder;
import dk.aau.cs.qweb.pec.logging.ResultFactory;
import dk.aau.cs.qweb.pec.types.Signature;
import gurobi.GRBException;

public class Experiment {
	
	private RDFCubeDataSource data;
	private RDFCubeStructure structure;
	private Lattice lattice;
	private Map<Long, Map<String, MaterializedFragments>> budget2MaterializedFragments = new HashMap<Long,Map<String,MaterializedFragments>>();
	private String dataSetPath;
	private String cachingStrategy;
	
	public Experiment(String dataset, String cachingStrategy) 
			throws IOException, UnsupportedDatabaseTypeException, DatabaseConnectionIsNotOpen, GRBException, ParseException {
		System.out.print("////////////////////////////");
		System.out.print(" Offline ");
		System.out.println("////////////////////////////");
		
		dataSetPath = dataset;
		this.cachingStrategy = cachingStrategy;
		
		long timea = System.currentTimeMillis();
		data = constructDataStore(dataSetPath);
		System.out.println("Construct data store " + (System.currentTimeMillis() - timea) + " ms");
		
		timea = System.currentTimeMillis();
		structure = RDFCubeStructure.build(Config.getCubeStructureLocation());
		System.out.println("Loading schema took " + (System.currentTimeMillis() - timea) + " ms");
		
		timea = System.currentTimeMillis();
		NaiveLatticeBuilder builder = new NaiveLatticeBuilder();
		lattice = builder.build(data, structure);
		System.out.println("Building lattice took " + (System.currentTimeMillis() - timea) + " ms");

		for (long budget : Config.getBudget()) {
			Map<String, MaterializedFragments> materializedFragmetMap = new HashMap<String,MaterializedFragments>();
			
			for (String fragmentSelectorName : Config.getFragmentSelectors()) {
				timea = System.currentTimeMillis();
				FragmentsSelector selector = getFragmentSelector(lattice,fragmentSelectorName);
				Set<Fragment> selectedFragments = selector.select(budget);
				System.out.println("Fragments selection took " + (System.currentTimeMillis() - timea) + " ms");
				
				timea = System.currentTimeMillis();
				MaterializedFragments materializedFragments = new JenaMaterializedFragment(selectedFragments, dataSetPath);
				System.out.println("Materialization took " + (System.currentTimeMillis() - timea) + " ms");
				materializedFragmetMap.put(fragmentSelectorName,materializedFragments);
			}
			budget2MaterializedFragments.put(budget,materializedFragmetMap);
		}
	}
	
	private FragmentsSelector getFragmentSelector(Lattice lattice2, String fragmentSelectorName) throws FileNotFoundException, GRBException, DatabaseConnectionIsNotOpen {
		FragmentsSelector selector;
		if (fragmentSelectorName.equals("greedy")) {
			selector = new GreedyFragmentsSelector(lattice2,Config.getGreedyLogLocation());
		} else if (fragmentSelectorName.equals("naive")) {
			selector = new NaiveFragmentsSelector(lattice2,Config.getNaiveLogLocation());
		} else if (fragmentSelectorName.equals("ilp")) {
			selector = new ILPFragmentsSelector(lattice2,Config.getILPLogLocation());
		} else {
			selector = new NaiveFragmentsSelector(lattice2,Config.getILPLogLocation());
		}
		return selector;
	}

	private RDFCubeDataSource constructDataStore(String datasetPath) throws IOException, UnsupportedDatabaseTypeException {
		if (Config.getDatabaseType().equals("inMemory")) {
			return InMemoryRDFCubeDataSource.build(datasetPath); 
		} else if (Config.getDatabaseType().equals("tdb")) {
			return JenaTDBDatabaseConnection.build(datasetPath);
		}
		throw new UnsupportedDatabaseTypeException();
	}

	public void run() throws DatabaseConnectionIsNotOpen, IOException {
		System.out.print("////////////////////////////");
		System.out.print(" Online ");
		System.out.println("////////////////////////////");
		
		Set<AnalyticalQuery> analyticalQueries = getAnalyticalQueries();
		Set<ProvenanceQuery> provenanceQueries = getProvenanceQueries(dataSetPath);
		
		for (Entry<Long, Map<String, MaterializedFragments>> budgetEntry : budget2MaterializedFragments.entrySet()) {
			Long budget = budgetEntry.getKey();
			
			for (Entry<String, MaterializedFragments> materializedFragmentEntry : budgetEntry.getValue().entrySet()) {
				String fragmentSelectionStrategy = materializedFragmentEntry.getKey();
				MaterializedFragments materializedFragments = materializedFragmentEntry.getValue();
				ResultFactory resultFactory = new JenaResultFactory(Config.getResultLogLocation(), Config.getExperimentalLogLocation(), budget,fragmentSelectionStrategy, cachingStrategy, dataSetPath);
				
				for (ProvenanceQuery provenanceQuery : provenanceQueries) {
					Set<String> provenanceIdentifiers =  resultFactory.evaluate(provenanceQuery); 
					resultFactory.setProvenanceQuery(provenanceQuery);
					
					for (AnalyticalQuery analyticalQuery : analyticalQueries) {
						
						for (Signature partialTriplePatternSignature : analyticalQuery.getTriplePatterns()) {
							Set<Fragment> allFragments = lattice.getFragmentsForRelation(partialTriplePatternSignature.getPredicate());
							Set<Fragment> requiredFragments = removeFragmentsNotAllowedByProvenanceQuery(allFragments,provenanceIdentifiers);
							analyticalQuery.addFrom(getMetadataGraphs(partialTriplePatternSignature));
							
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
		}
	}

	private Set<String> getMetadataGraphs(Signature signature) {
		Set<String> graphs = new HashSet<String>();
		if (signature.getRange() == null) {
			
		} else if (signature.getRange().equals("<http://purl.org/linked-data/cube#Observation>")) {
			graphs.add("http://example.com/CubeInstanceMetadata/observation");
		} 
		
		if (signature.getPredicate().equals("<http://example.com/commitdate>")) {
			graphs.add("http://example.com/CubeInstanceMetadata/commitdate");
		} else if (signature.getPredicate().equals("<http://example.com/suppkey>")) {
			graphs.add("http://example.com/CubeInstanceMetadata/suppkey");
		} else if (signature.getPredicate().equals("<http://example.com/custkey>")) {
			graphs.add("http://example.com/CubeInstanceMetadata/custkey");
		} else if (signature.getPredicate().equals("<http://example.com/partkey>")) {
			graphs.add("http://example.com/CubeInstanceMetadata/partkey");
		} else if (signature.getPredicate().equals("<http://example.com/orderdate>")) {
			graphs.add("http://example.com/CubeInstanceMetadata/orderdate");
		}
		return graphs;
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

	private Set<ProvenanceQuery> getProvenanceQueries(String datasetPath) throws IOException {
		
		Set<ProvenanceQuery> queries = new HashSet<ProvenanceQuery>();
		for (String queryFile : Config.getProvenanceQueryPath()) {
			File path = new File(datasetPath+"/"+queryFile+"/");
			if (path.isDirectory()) {
				for (File file : path.listFiles()) {
				    if (file.isFile()) {
				    	queries.add(new ProvenanceQuery(file));
				    }
				}
			} else {
				queries.add(new ProvenanceQuery(path));
			}
		}
		return queries;
	}

	private Set<AnalyticalQuery> getAnalyticalQueries() throws IOException {
		Set<AnalyticalQuery> queries = new HashSet<AnalyticalQuery>();
		for (String queryFile : Config.getAnalyticalQueryPath()) {
			queries.add(new AnalyticalQuery(new File(queryFile), structure));
		}
		return queries;
	}
}
