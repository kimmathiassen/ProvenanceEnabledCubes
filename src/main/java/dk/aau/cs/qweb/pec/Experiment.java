package dk.aau.cs.qweb.pec;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import dk.aau.cs.qweb.pec.data.InMemoryRDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.JenaTDBDatabaseConnection;
import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.exceptions.UnsupportedDatabaseTypeException;
import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.fragmentsSelector.FragmentsSelector;
import dk.aau.cs.qweb.pec.fragmentsSelector.GreedyFragmentsSelector;
import dk.aau.cs.qweb.pec.fragmentsSelector.ILPFragmentsSelector;
import dk.aau.cs.qweb.pec.fragmentsSelector.ILPWithRedundancyFragmentsSelector;
import dk.aau.cs.qweb.pec.fragmentsSelector.NaiveFragmentsSelector;
import dk.aau.cs.qweb.pec.lattice.Lattice;
import dk.aau.cs.qweb.pec.lattice.LatticeBuilder;
import dk.aau.cs.qweb.pec.logger.Logger;
import dk.aau.cs.qweb.pec.queryEvaluation.AnalyticalQuery;
import dk.aau.cs.qweb.pec.queryEvaluation.JenaMaterializedFragments;
import dk.aau.cs.qweb.pec.queryEvaluation.JenaResultFactory;
import dk.aau.cs.qweb.pec.queryEvaluation.MaterializedFragments;
import dk.aau.cs.qweb.pec.queryEvaluation.ProvenanceQuery;
import dk.aau.cs.qweb.pec.queryEvaluation.ResultFactory;
import dk.aau.cs.qweb.pec.queryEvaluation.ResultMaterializedFragments;
import dk.aau.cs.qweb.pec.types.QueryPair;
import dk.aau.cs.qweb.pec.types.Signature;
import gurobi.GRBException;

public class Experiment {
	
	private RDFCubeDataSource data;
	private RDFCubeStructure structure;
	private Lattice lattice;
	private String mergeStrategy;
	private Map<Long, Map<String, MaterializedFragments>> budget2MaterializedFragments = new HashMap<>();
	private String dataSetPath;
	private String cachingStrategy;
	private Map<Pair<String, String>, Integer> hashesDebugMap = new HashMap<>();
	
	public Experiment(String dataset, String cachingStrfragmentsategy, String mergeStrategy) 
			throws IOException, UnsupportedDatabaseTypeException, DatabaseConnectionIsNotOpen, GRBException, ParseException {
		System.out.print("////////////////////////////");
		System.out.print(" Offline ");
		System.out.println("////////////////////////////");
		this.mergeStrategy = mergeStrategy;
		Logger logger = new Logger();
		
		Config.setTimestamp(new Timestamp(System.currentTimeMillis()));
		long bytesInMB = 0x1 << 20;
		logger.log("Total memory (before offline phase): " + (Runtime.getRuntime().totalMemory() / bytesInMB) + " MB");
		logger.log("Free memory (before offline phase): " + (Runtime.getRuntime().freeMemory() / bytesInMB) + " MB");		
		logger.log("Max memory (before offline phase): " + (Runtime.getRuntime().maxMemory() / bytesInMB) + " MB");
		Map<String, String> latticeConfMap = new LinkedHashMap<>();
		latticeConfMap.put("mergeStrategy", mergeStrategy);
		latticeConfMap.put("reduceRatio", "" + Config.getReduceRatio());
		
		dataSetPath = dataset;
		this.cachingStrategy = cachingStrfragmentsategy;
		
		logger.log("Caching strategy: " + cachingStrfragmentsategy);
		logger.startTimer("constructDataStore");
		data = constructDataStore(dataSetPath, cachingStrategy);
		logger.endTimer("constructDataStore");
		
		logger.startTimer("constructCubeStructure");
		structure = RDFCubeStructure.build(Config.getCubeStructureLocation());
		logger.endTimer("constructCubeStructure");
		
		logger.startTimer("buildLattice_"+mergeStrategy);
		lattice = LatticeBuilder.build(data, structure, latticeConfMap);
		logger.log("Lattice initial size: " + lattice.getInitialSize());
		logger.log("Lattice size: " + lattice.size());		
		logger.log("Lattice merging steps: " + lattice.getMergingSteps());
		logger.log(lattice.toString());
		logger.endTimer("buildLattice_"+mergeStrategy);
		
		for (long budget : getBudget()) {
			Map<String, MaterializedFragments> materializedFragmetMap = new HashMap<String,MaterializedFragments>();
			
			for (String fragmentSelectorName : Config.getFragmentSelectors()) {
				logger.startTimer(fragmentSelectorName);
				FragmentsSelector selector = getFragmentSelector(lattice,fragmentSelectorName);
				Set<Fragment> selectedFragments = selector.select(budget, logger);
				logger.log("Selected fragments (budget " + budget + ") : " + selectedFragments);
				logger.endTimer(fragmentSelectorName);
				
				logger.startTimer("materialize fragments with budget "+budget);
				MaterializedFragments materializedFragments = new JenaMaterializedFragments(selectedFragments, 
						dataSetPath, lattice,  logger);
				materializedFragmetMap.put(fragmentSelectorName,materializedFragments);
				logger.endTimer("materialize fragments with budget "+budget);
			}
			budget2MaterializedFragments.put(budget,materializedFragmetMap);
		}
		
		logger.log("Total memory (after offline phase): " + (Runtime.getRuntime().totalMemory() / bytesInMB) + " MB");
		logger.log("Free memory (after offline phase): " + (Runtime.getRuntime().freeMemory() / bytesInMB) + " MB");		
		logger.log("Max memory (after offline phase): " + (Runtime.getRuntime().maxMemory() / bytesInMB) + " MB");
		logger.write();
		
		if (Config.isDebugQuery()) {
			// Compute the hashes for the queries
			logger.log("Computing the hashes for the query results for verification purposes");
			System.out.println("Computing the hashes for the query results for verification purposes");
			Set<ProvenanceQuery> provenanceQueries = getProvenanceQueries(dataSetPath);
			Set<AnalyticalQuery> analyticalQueries = getAnalyticalQueries();
			ResultFactory resultFactory = new JenaResultFactory(Config.getResultLogLocation(), 
					Config.getExperimentalLogLocation(), 0l,
					"ilp", cachingStrategy, 
					dataSetPath, "fullMaterialization", mergeStrategy);
			MaterializedFragments mockupMaterializedFragments = new JenaMaterializedFragments(Collections.emptySet(), dataSetPath, lattice, logger);
			for (ProvenanceQuery provenanceQuery : provenanceQueries) {
				for (AnalyticalQuery analyticalQuery : analyticalQueries) {
					String serializedResult = 
							runProvenanceAwareQueryOnMaterializedFragments(provenanceQuery, analyticalQuery, 
									resultFactory, mockupMaterializedFragments, 0);	
					hashesDebugMap.put(new MutablePair<String, String>(provenanceQuery.getFilename(), analyticalQuery.getQueryFile()), 
							serializedResult.hashCode());
				}
			}
			System.out.println("Hashes computed");
			logger.log("Hashes computed");
		}		
	}

	
	private FragmentsSelector getFragmentSelector(Lattice lattice2, String fragmentSelectorName) throws FileNotFoundException, GRBException, DatabaseConnectionIsNotOpen {
		FragmentsSelector selector;
		if (fragmentSelectorName.equals("greedy")) {
			selector = new GreedyFragmentsSelector(lattice2, Config.getGreedyLogLocation());
		} else if (fragmentSelectorName.equals("naive")) {
			selector = new NaiveFragmentsSelector(lattice2, Config.getNaiveLogLocation());
		} else if (fragmentSelectorName.equals("ilp")) {
			selector = new ILPFragmentsSelector(lattice2, Config.getILPLogLocation(), Config.getOutputILP2Stdout());
		} else if (fragmentSelectorName.equals("redundant-ilp")) {
			selector = new ILPWithRedundancyFragmentsSelector(lattice2, Config.getILPLogLocation(), Config.getOutputILP2Stdout());
		} else {
			selector = new NaiveFragmentsSelector(lattice2,Config.getILPLogLocation());
		}
		return selector;
	}

	private List<Long> getBudget() {
		List<Long> budget = Config.getBudget();
		
		for (Long budgetPercent : Config.getBudgetPercentages()) {
			budget.add(data.count() * budgetPercent / 100);
		}
		
		return budget;
	}

	private RDFCubeDataSource constructDataStore(String datasetPath, String cachingStrategy) throws IOException, UnsupportedDatabaseTypeException {
		if (Config.getDatabaseType().equals("inMemory")) {
			return InMemoryRDFCubeDataSource.build(datasetPath); 
		} else if (Config.getDatabaseType().equals("tdb")) {
			return JenaTDBDatabaseConnection.build(datasetPath, cachingStrategy);
		}
		throw new UnsupportedDatabaseTypeException();
	}

	/**
	 * The main method of the experimental setup. It runs all the combinations of analytical and provenance queries.
	 * 
	 * @throws DatabaseConnectionIsNotOpen
	 * @throws IOException
	 */
	public void run() throws DatabaseConnectionIsNotOpen, IOException {
		System.out.print("////////////////////////////");
		System.out.print(" Online ");
		System.out.println("////////////////////////////");
		
		// Entries have the form Budget -> Map[evaluationStrategy -> MaterializedFragments]
		for (Entry<Long, Map<String, MaterializedFragments>> budgetEntry : budget2MaterializedFragments.entrySet()) {
			Long budget = budgetEntry.getKey();
			runForBudgetEntry(budget, budgetEntry);
		}
	}

	public String runProvenanceAwareQueryOnMaterializedFragments(ProvenanceQuery provenanceQuery, AnalyticalQuery analyticalQuery, 
			ResultFactory resultFactory, MaterializedFragments materializedFragments, int round) throws FileNotFoundException, IOException {
		Set<String> provenanceIdentifiers =  resultFactory.evaluate(provenanceQuery); 
		resultFactory.setProvenanceQuery(provenanceQuery);

		//ensure that materialized fragments are sorted ancestor first.
		if (Config.isOptimizedQueryRewriting()) {
			selectMaterializedFragmentsForQueryOptimized(analyticalQuery, provenanceIdentifiers, materializedFragments);	
			return resultFactory.evaluate(materializedFragments, analyticalQuery, round);
		} else {
			// ISWC 2017 code
			selectMaterializedFragmentsForQueryNonOptimized(analyticalQuery, provenanceIdentifiers, materializedFragments);	
			return resultFactory.evaluate(materializedFragments, analyticalQuery, round);
		}

	}
	
	private void runForBudgetEntry(Long budget, Entry<Long, Map<String, MaterializedFragments>> budgetEntry) throws IOException {
		Set<AnalyticalQuery> analyticalQueries = getAnalyticalQueries();
		
		for (Entry<String, MaterializedFragments> materializedFragmentEntry : budgetEntry.getValue().entrySet()) {
			String fragmentSelectionStrategy = materializedFragmentEntry.getKey();
			MaterializedFragments materializedFragments = materializedFragmentEntry.getValue();
			
			for (String evaluationStrategy : Config.getEvaluationStrategies()) {
				if (wasAnyFragmentsMaterialized(materializedFragments,budget)) {
					ResultFactory resultFactory = new JenaResultFactory(Config.getResultLogLocation(), 
							Config.getExperimentalLogLocation(), budget,
							fragmentSelectionStrategy, cachingStrategy, 
							dataSetPath, evaluationStrategy, mergeStrategy);
											
					// Create pairs of analytical and provenance queries
					for (int i = 0; i < Config.getNumberOfExperimentalRuns(); i++) {
						List<QueryPair> queryPairs = createQueryPairList(getProvenanceQueries(dataSetPath), analyticalQueries);
						for (QueryPair pair : queryPairs) {
							ProvenanceQuery provenanceQuery = pair.getProvenanceQuery();
							AnalyticalQuery analyticalQuery = pair.getAnalyticalQuery();							
							String result = runProvenanceAwareQueryOnMaterializedFragments(provenanceQuery, 
									analyticalQuery, resultFactory, materializedFragments, i);
							if (Config.isDebugQuery()) {
								Pair<String, String> executionPair = 
										new MutablePair<>(provenanceQuery.getFilename(), analyticalQuery.getQueryFile());
								int expectedHashCode = hashesDebugMap.get(executionPair);
								if (result.hashCode() != expectedHashCode) {
									System.err.println("Hashes do not match for query pair " + executionPair);
									System.exit(1);
								}
							}
						}
					}
				} else {
					System.out.println("No fragments were materialized, see selection strategy for more info");
				}
			}
		}
	}

	/**
	 * Get an optimal selection of fragments, that if materialized, would allow us to answer
	 * the provenance-aware query completely in memory.
	 * @param analyticalQuery
	 * @param provenanceIdentifiers
	 * @return
	 */
	private void selectMaterializedFragmentsForQueryOptimized(AnalyticalQuery analyticalQuery, Set<String> provenanceIdentifiers, MaterializedFragments materializedFragments) {
		ResultMaterializedFragments result = new ResultMaterializedFragments(materializedFragments, lattice);
		for (Signature partialTriplePatternSignature : analyticalQuery.getTriplePatterns()) {
			// Correct the method getFragmentsForPartialSignatureWithProvenanceIdentifiers
			Set<Fragment> specificRelevantFragments = lattice.getMostSpecificFragmentsForPartialSignatureWithProvenanceIdentifiers(partialTriplePatternSignature,
					provenanceIdentifiers);
			for (Fragment candidate : specificRelevantFragments) {
				if (materializedFragments.contains(candidate)) {
					result.add(candidate);
				} else {
					PriorityQueue<Fragment> materializedAncestors = 
							materializedFragments.getSortedIntersection(lattice.getAncestors(candidate));
					if (materializedAncestors.isEmpty()) {
						// Add the default fragments of the form <*, *, *, I>
						result.addAll(lattice.getLeastSpecificFragmentsForPartialSignatures(candidate.getProvenanceIdentifiers()));
					} else {
						// Add the first ancestor
						result.add(materializedAncestors.peek());
					}
					
				}
			}
		}
		
		// This method will remove children if they were selected with their parents.
		analyticalQuery.optimizeFromClause2(result, materializedFragments, lattice);
	}

	private void selectMaterializedFragmentsForQueryNonOptimized(AnalyticalQuery analyticalQuery, Set<String> provenanceIdentifiers, 
			MaterializedFragments materializedFragments) {
		for (Signature partialTriplePatternSignature : analyticalQuery.getTriplePatterns()) {
			Set<Fragment> fragmentsForTriplePattern = lattice.getFragmentsForPartialSignatureWithProvenanceIdentifiers(partialTriplePatternSignature,
					provenanceIdentifiers); 
			//Check that this method does the correct thing.   
			for (Fragment fragment : fragmentsForTriplePattern) {
				analyticalQuery.addFrom(fragment.getProvenanceIdentifiers());
			}
		}
		
		analyticalQuery.optimizeFromClause(materializedFragments);
	}

	private List<QueryPair> createQueryPairList(Set<ProvenanceQuery> provenanceQueries,
			Set<AnalyticalQuery> analyticalQueries) {
		List<QueryPair> result = new ArrayList<QueryPair>();
		for (AnalyticalQuery analyticalQuery : analyticalQueries) {
			for (ProvenanceQuery provenanceQuery : provenanceQueries) {
				result.add(new QueryPair(provenanceQuery, analyticalQuery));
			}
		}
		Collections.shuffle(result);
		return result;
	}

	private boolean wasAnyFragmentsMaterialized(MaterializedFragments materializedFragments, Long budget) {
		//Fails if fragments should be created but was not.
		if (budget == 0) {
			return true;
		} else {
			if (materializedFragments.size() == 0) {
				return false;
			} else {
				return true;
			}
		}
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
