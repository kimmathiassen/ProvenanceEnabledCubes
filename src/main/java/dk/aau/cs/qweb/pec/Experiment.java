package dk.aau.cs.qweb.pec;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import dk.aau.cs.qweb.pec.fragmentsSelector.ILPCubeObliviousFragmentsSelector;
import dk.aau.cs.qweb.pec.fragmentsSelector.ILPFragmentsSelector;
import dk.aau.cs.qweb.pec.fragmentsSelector.ILPWithObservationDistanceFragmentsSelector;
import dk.aau.cs.qweb.pec.fragmentsSelector.ILPWithObservationDistanceRedundancyFragmentsSelector;
import dk.aau.cs.qweb.pec.fragmentsSelector.MockupFragmentsSelector;
import dk.aau.cs.qweb.pec.fragmentsSelector.NaiveFragmentsSelector;
import dk.aau.cs.qweb.pec.lattice.Lattice;
import dk.aau.cs.qweb.pec.lattice.LatticeBuilder;
import dk.aau.cs.qweb.pec.lattice.LatticeStats;
import dk.aau.cs.qweb.pec.lattice.MergeLattice;
import dk.aau.cs.qweb.pec.logger.Logger;
import dk.aau.cs.qweb.pec.queryEvaluation.AnalyticalQuery;
import dk.aau.cs.qweb.pec.queryEvaluation.JenaMaterializedFragments;
import dk.aau.cs.qweb.pec.queryEvaluation.JenaResultFactory;
import dk.aau.cs.qweb.pec.queryEvaluation.LRUCache;
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
	private String dataSetPath;
	private String cachingStrategy;
	private Map<Pair<String, String>, Integer> hashesDebugMap = new HashMap<>();
	
	public Experiment(String dataset, String cachingStrfragmentsategy, String mergeStrategy) 
			throws IOException, UnsupportedDatabaseTypeException, DatabaseConnectionIsNotOpen, GRBException, ParseException {
		System.out.print("////////////////////////////");
		System.out.print(" Offline ");
		System.out.println("////////////////////////////");
		this.mergeStrategy = mergeStrategy;
		Logger logger = new Logger(new File(Config.getOfflineLogLocation()));
		
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
		if (this.cachingStrategy.equals("tepid")) {
			logger.log("Using the budget argument only for the Jena cache. The system will be forced to use lattice budget = 0");
		}
		
		logger.startTimer("constructDataStore");
		data = constructDataStore(dataSetPath, cachingStrategy);
		logger.endTimer("constructDataStore");
		
		logger.startTimer("constructCubeStructure");
		structure = RDFCubeStructure.build(Config.getCubeStructureLocation());
		logger.endTimer("constructCubeStructure");
				
		if (this.cachingStrategy.equals("tepid")) {
			lattice = LatticeBuilder.build(data, structure, latticeConfMap);
			logger.log("Creating empty lattice for tepid caching strategy");
			logger.log("This strategy works only with one budget value");
		} else {
			logger.startTimer("buildLattice_"+mergeStrategy);
			lattice = LatticeBuilder.build(data, structure, latticeConfMap);
			logger.log("Lattice initial size: " + lattice.getInitialSize());
			logger.log("Lattice size: " + lattice.size());		
			logger.log("Lattice merging steps: " + lattice.getMergingSteps());
			
			if (lattice instanceof MergeLattice) {
				logger.log("Lattice property merging steps: " + ((MergeLattice)lattice).getPropertyMergeSteps());
				logger.log("Lattice provenance merging steps: " + ((MergeLattice)lattice).getProvenanceMergeSteps());
			}
			
			logger.log(lattice.toString());
			logger.endTimer("buildLattice_"+mergeStrategy);
		}
		
		logger.log("Lattice stats");
		logger.log(lattice.getFragmentStats().toString());
		
		logger.log("Total memory (after offline phase): " + (Runtime.getRuntime().totalMemory() / bytesInMB) + " MB");
		logger.log("Free memory (after offline phase): " + (Runtime.getRuntime().freeMemory() / bytesInMB) + " MB");		
		logger.log("Max memory (after offline phase): " + (Runtime.getRuntime().maxMemory() / bytesInMB) + " MB");
		logger.write();
		
		if (Config.isDebugQuery()) {
			// Compute the hashes for the queries
			logger.log("Computing the hashes for the query results for verification purposes");
			System.out.println("Computing the hashes for the query results for verification purposes");
			ResultFactory resultFactory = new JenaResultFactory(Config.getResultLogLocation(), 
					Config.getExperimentalLogLocation(), 0l,
					"hash-calculation", cachingStrategy, 
					dataSetPath, "fullMaterialization", mergeStrategy);
			List<QueryPair> queryPairs = createQueryPairList(getProvenanceQueries(dataSetPath), getAnalyticalQueries());
			MaterializedFragments mockupMaterializedFragments = new JenaMaterializedFragments(Collections.emptySet(), dataSetPath, lattice, logger);
			for (QueryPair qPair : queryPairs) {
				ProvenanceQuery provenanceQuery = qPair.getProvenanceQuery();
				AnalyticalQuery analyticalQuery = qPair.getAnalyticalQuery();
					String serializedResult = 
							runProvenanceAwareQueryOnMaterializedFragments(provenanceQuery, analyticalQuery, 
									resultFactory, mockupMaterializedFragments, 0);	
					hashesDebugMap.put(new MutablePair<String, String>(provenanceQuery.getFilename(), analyticalQuery.getQueryFile()), 
							serializedResult.hashCode());
			}
			System.out.println(hashesDebugMap);
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
			selector = new ILPWithObservationDistanceRedundancyFragmentsSelector(lattice2, Config.getILPLogLocation(), Config.getOutputILP2Stdout());
		} else if (fragmentSelectorName.equals("ilp-distance")){
			selector = new ILPWithObservationDistanceFragmentsSelector(lattice2, Config.getILPLogLocation(), Config.getOutputILP2Stdout());
		} else if (fragmentSelectorName.equals("ilp-cube-oblivious")) { 
			selector = new ILPCubeObliviousFragmentsSelector(lattice2, Config.getILPLogLocation(), Config.getOutputILP2Stdout());
		} else if (fragmentSelectorName.equals("lru")) {
			selector = new MockupFragmentsSelector(lattice2);
		} else {
			selector = new NaiveFragmentsSelector(lattice2,Config.getILPLogLocation());
		}
		
		return selector;
	}

	private List<Long> getBudgets() {
		if (this.cachingStrategy.equals("tepid")) {
			return Arrays.asList(0l);
		} else {
			List<Long> budget = Config.getBudget();
			
			for (Long budgetPercent : Config.getBudgetPercentages()) {
				budget.add(data.count() * budgetPercent / 100);
			}
			
			return budget;
		}
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
	 * @throws GRBException 
	 */
	public void run() throws DatabaseConnectionIsNotOpen, IOException, GRBException {
		System.out.print("////////////////////////////");
		System.out.print(" Online ");
		System.out.println("////////////////////////////");
		Logger logger = new Logger(new File(Config.getResultLogLocation()));
		for (Long budget : getBudgets()) {
			for (String fragmentSelectorName : Config.getFragmentSelectors()) {
				System.out.println(fragmentSelectorName);
				FragmentsSelector selector = getFragmentSelector(lattice, fragmentSelectorName);
				long startTime = System.currentTimeMillis();
				Set<Fragment> selectedFragments = selector.select(budget, logger);
				System.out.println("Selection of the fragments took " + (System.currentTimeMillis() - startTime) + " ms");
				System.out.println("Selected fragments (budget " + budget + ") : " + selectedFragments.size());				
				System.out.println("Materialize fragments with budget " + budget);				
				System.out.println(LatticeStats.getStats(selectedFragments));
				System.out.println(selectedFragments);
				if (fragmentSelectorName.equals("lru")) {
					runLRUForBudgetEntry(budget, logger);
				} else {
					MaterializedFragments materializedFragments = new JenaMaterializedFragments(selectedFragments, 
							dataSetPath, lattice,  logger);
					runForBudgetEntry(budget, fragmentSelectorName, materializedFragments);
				}
			}
		}
	}

	/**
	 * Implement the execution using a dynamic LRU (last recently used) cache.
	 * @param budget
	 * @param fragmentSelectorName
	 * @throws IOException 
	 */
	private void runLRUForBudgetEntry(Long budget, Logger logger) throws IOException {
		for (String evaluationStrategy : Config.getEvaluationStrategies()) {
			ResultFactory resultFactory = new JenaResultFactory(Config.getResultLogLocation(), 
					Config.getExperimentalLogLocation(), budget,
					"lru", cachingStrategy, 
					dataSetPath, evaluationStrategy, mergeStrategy);
			LRUCache lruCache = new LRUCache(budget, dataSetPath, lattice, logger);
									
			// Create pairs of analytical and provenance queries
			for (int i = 0; i < Config.getNumberOfExperimentalRuns(); i++) {
				List<QueryPair> queryPairs = createQueryPairList(getProvenanceQueries(dataSetPath), getAnalyticalQueries());
				for (QueryPair pair : queryPairs) {
					ProvenanceQuery provenanceQuery = pair.getProvenanceQuery();
					AnalyticalQuery analyticalQuery = pair.getAnalyticalQuery();
					String result = runProvenanceAwareQueryOnMaterializedFragments(provenanceQuery, 
							analyticalQuery, resultFactory, lruCache.getContents(), i);
					// Now get the fragments that were used last time to set up the cache
					lruCache.updateCache(analyticalQuery.getFromClause(), lruCache.getContents());
					
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
		}
		
	}


	public String runProvenanceAwareQueryOnMaterializedFragments(ProvenanceQuery provenanceQuery, AnalyticalQuery analyticalQuery, 
			ResultFactory resultFactory, MaterializedFragments materializedFragments, int round) throws FileNotFoundException, IOException {
		Set<String> provenanceIdentifiers =  resultFactory.evaluate(provenanceQuery); 
		// Set the provenance query
		resultFactory.setProvenanceQuery(provenanceQuery);

		if (Config.isOptimizedQueryRewriting()) {
			long timeStart = System.currentTimeMillis();
			selectMaterializedFragmentsForQueryOptimized(analyticalQuery, provenanceIdentifiers, materializedFragments);	
			long timeQueryRewriting = System.currentTimeMillis() - timeStart;
			analyticalQuery.setQueryRewritingTime(timeQueryRewriting);
			return resultFactory.evaluate(materializedFragments, analyticalQuery, round);
		} else {
			// ISWC 2017 code
			long timeStart = System.currentTimeMillis();
			selectMaterializedFragmentsForQueryNonOptimized(analyticalQuery, provenanceIdentifiers, materializedFragments);	
			long timeQueryRewriting = System.currentTimeMillis() - timeStart;
			analyticalQuery.setQueryRewritingTime(timeQueryRewriting);
			return resultFactory.evaluate(materializedFragments, analyticalQuery, round);
		}

	}
	
	private void runForBudgetEntry(Long budget, String fragmentSelectionStrategy, MaterializedFragments materializedFragments) throws IOException {
		for (String evaluationStrategy : Config.getEvaluationStrategies()) {
			if (wasAnyFragmentsMaterialized(materializedFragments, budget)) {
				ResultFactory resultFactory = new JenaResultFactory(Config.getResultLogLocation(), 
						Config.getExperimentalLogLocation(), budget,
						fragmentSelectionStrategy, cachingStrategy, 
						dataSetPath, evaluationStrategy, mergeStrategy);
										
				// Create pairs of analytical and provenance queries
				for (int i = 0; i < Config.getNumberOfExperimentalRuns(); i++) {
					List<QueryPair> queryPairs = createQueryPairList(getProvenanceQueries(dataSetPath), getAnalyticalQueries());
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

	/**
	 * Get an optimal selection of fragments, that if materialized, would allow us to answer
	 * the provenance-aware query completely in memory.
	 * @param analyticalQuery
	 * @param provenanceIdentifiers
	 * @return
	 */
	private void selectMaterializedFragmentsForQueryOptimized(AnalyticalQuery analyticalQuery, Set<String> provenanceIdentifiers, MaterializedFragments materializedFragments) {
		ResultMaterializedFragments candidates = new ResultMaterializedFragments(materializedFragments, lattice);
		Set<String> graphsFromDisk = new LinkedHashSet<>();
		for (Signature partialTriplePatternSignature : analyticalQuery.getTriplePatterns()) {
			// Correct the method getFragmentsForPartialSignatureWithProvenanceIdentifiers
			Set<Fragment> specificRelevantFragments = lattice.getMostSpecificFragmentsForPartialSignatureWithProvenanceIdentifiers(partialTriplePatternSignature,
					provenanceIdentifiers);
			for (Fragment candidate : specificRelevantFragments) {
				if (materializedFragments.contains(candidate)) {
					candidates.add(candidate);
				} else {
					PriorityQueue<Fragment> materializedAncestors = 
							materializedFragments.getSortedIntersection(lattice.getAncestors(candidate));
					if (materializedAncestors.isEmpty()) {
						// Add the default fragments of the form <*, *, *, I>
						graphsFromDisk.addAll(candidate.getProvenanceIdentifiers());
					} else {
						// Add the first ancestor
						boolean fragmentAdded = false;
						while (!materializedAncestors.isEmpty()) {
							Fragment materializedAncestor = materializedAncestors.poll();
							if (provenanceIdentifiers.containsAll(materializedAncestor.getProvenanceIdentifiers())) {
								candidates.add(materializedAncestor);
								fragmentAdded = true;
								if (materializedAncestor.getProvenanceIdentifiers().size() > 1) {
									System.out.println("Potential save of from clauses from fragment " + materializedAncestor);
								}
								break;
							}
						}
						
						if (!fragmentAdded) {
							graphsFromDisk.addAll(candidate.getProvenanceIdentifiers());
						}
					}
					
				}
			}
		}
		
		// This method will remove children if they were selected with their parents.
		analyticalQuery.optimizeFromClause2(candidates, materializedFragments, graphsFromDisk, lattice);
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
				result.add(new QueryPair(provenanceQuery, analyticalQuery.clone()));
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
			queries.add(new AnalyticalQuery(new File(queryFile)));
		}
		return queries;
	}
}
