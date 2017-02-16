package dk.aau.cs.qweb.pec;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import dk.aau.cs.qweb.pec.fragmentsSelector.NaiveFragmentsSelector;
import dk.aau.cs.qweb.pec.lattice.Lattice;
import dk.aau.cs.qweb.pec.lattice.LatticeBuilder;
import dk.aau.cs.qweb.pec.logger.Logger;
import dk.aau.cs.qweb.pec.queryEvaluation.AnalyticalQuery;
import dk.aau.cs.qweb.pec.queryEvaluation.JenaMaterializedFragment;
import dk.aau.cs.qweb.pec.queryEvaluation.JenaResultFactory;
import dk.aau.cs.qweb.pec.queryEvaluation.MaterializedFragments;
import dk.aau.cs.qweb.pec.queryEvaluation.ProvenanceQuery;
import dk.aau.cs.qweb.pec.queryEvaluation.ResultFactory;
import dk.aau.cs.qweb.pec.types.Signature;
import gurobi.GRBException;

public class Experiment {
	
	private RDFCubeDataSource data;
	private RDFCubeStructure structure;
	private Lattice lattice;
	private Map<Long, Map<String, MaterializedFragments>> budget2MaterializedFragments = new HashMap<Long,Map<String,MaterializedFragments>>();
	private String dataSetPath;
	private String cachingStrategy;
	
	public Experiment(String dataset, String cachingStrfragmentsategy, String mergeStretegy) 
			throws IOException, UnsupportedDatabaseTypeException, DatabaseConnectionIsNotOpen, GRBException, ParseException {
		System.out.print("////////////////////////////");
		System.out.print(" Offline ");
		System.out.println("////////////////////////////");
		Logger logger = new Logger();
		
		Config.setTimestamp(new Timestamp(System.currentTimeMillis()));
		
		dataSetPath = dataset;
		this.cachingStrategy = cachingStrfragmentsategy;
		
		
		logger.startTimer("constructDataStore");
		data = constructDataStore(dataSetPath,cachingStrategy);
		logger.endTimer("constructDataStore");
		
		logger.startTimer("constructCubeStructure");
		structure = RDFCubeStructure.build(Config.getCubeStructureLocation());
		logger.endTimer("constructCubeStructure");
		
		logger.startTimer("buildLattice_"+mergeStretegy);
		lattice = LatticeBuilder.build(data, structure,mergeStretegy);
		logger.endTimer("buildLattice_"+mergeStretegy);
		
		for (long budget : getBudget()) {
			Map<String, MaterializedFragments> materializedFragmetMap = new HashMap<String,MaterializedFragments>();
			
			for (String fragmentSelectorName : Config.getFragmentSelectors()) {
				logger.startTimer(fragmentSelectorName);
				FragmentsSelector selector = getFragmentSelector(lattice,fragmentSelectorName);
				Set<Fragment> selectedFragments = selector.select(budget,logger);
				logger.endTimer(fragmentSelectorName);
				
				logger.startTimer("materialize fragments with budget "+budget);
				MaterializedFragments materializedFragments = new JenaMaterializedFragment(selectedFragments, dataSetPath,logger);
				materializedFragmetMap.put(fragmentSelectorName,materializedFragments);
				logger.endTimer("materialize fragments with budget "+budget);
			}
			budget2MaterializedFragments.put(budget,materializedFragmetMap);
		}
		logger.write();
		
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

	private List<Long> getBudget() {
		List<Long> budget = Config.getBudget();
		
		for (Long budgetPercent : Config.getBudgetPercentages()) {
			budget.add(data.count()*budgetPercent/100);
		}
		
		return budget;
	}

	private RDFCubeDataSource constructDataStore(String datasetPath,String cachingStrategy) throws IOException, UnsupportedDatabaseTypeException {
		if (Config.getDatabaseType().equals("inMemory")) {
			return InMemoryRDFCubeDataSource.build(datasetPath); 
		} else if (Config.getDatabaseType().equals("tdb")) {
			return JenaTDBDatabaseConnection.build(datasetPath, cachingStrategy);
		}
		throw new UnsupportedDatabaseTypeException();
	}

	public void run() throws DatabaseConnectionIsNotOpen, IOException {
		System.out.print("////////////////////////////");
		System.out.print(" Online ");
		System.out.println("////////////////////////////");
		
		
		
		for (Entry<Long, Map<String, MaterializedFragments>> budgetEntry : budget2MaterializedFragments.entrySet()) {
			Long budget = budgetEntry.getKey();
			
			for (Entry<String, MaterializedFragments> materializedFragmentEntry : budgetEntry.getValue().entrySet()) {
				String fragmentSelectionStrategy = materializedFragmentEntry.getKey();
				MaterializedFragments materializedFragments = materializedFragmentEntry.getValue();
				
				for (String evaluationStrategy : Config.getEvaluationStrategies()) {
					
					if (wasAnyFragmentsMaterialized(materializedFragments,budget)) {
						ResultFactory resultFactory = new JenaResultFactory(Config.getResultLogLocation(), Config.getExperimentalLogLocation(), budget,fragmentSelectionStrategy, cachingStrategy, dataSetPath,evaluationStrategy);
						
						Set<ProvenanceQuery> provenanceQueries = getProvenanceQueries(dataSetPath);
						for (ProvenanceQuery provenanceQuery : provenanceQueries) {
							Set<String> provenanceIdentifiers =  resultFactory.evaluate(provenanceQuery); 
							resultFactory.setProvenanceQuery(provenanceQuery);
							Set<AnalyticalQuery> analyticalQueries = getAnalyticalQueries();
							
							for (AnalyticalQuery analyticalQuery : analyticalQueries) {
								
								for (Signature partialTriplePatternSignature : analyticalQuery.getTriplePatterns()) {
								Set<Fragment> fragmentsForTriplePattern = lattice.getFragmentsForPartialSignatureWithProvenanceIdentifiers(partialTriplePatternSignature,provenanceIdentifiers); 
								
									for (Fragment fragment : fragmentsForTriplePattern) {
										
										if (!analyticalQuery.containsFragmentProvenanceIdentifer(fragment)) {
											
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
								}
								resultFactory.evaluate(materializedFragments,analyticalQuery);
							}
						}
					} else {
						System.out.println("No fragments was materialized, see selection strategy for more info");
					}
					
				}
			}
		}
	}

	private boolean wasAnyFragmentsMaterialized(MaterializedFragments materializedFragments, Long budget) {
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
