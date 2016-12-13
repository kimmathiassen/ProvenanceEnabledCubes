package dk.aau.cs.qweb.pec.QueryEvaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.tdb.TDBFactory;

import dk.aau.cs.qweb.pec.Config;
import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.types.Signature;

public class JenaMaterializedFragment extends MaterializedFragments {

	private Map<String,Set<Model>> materializedFragments = new HashMap<String,Set<Model>>();

	public JenaMaterializedFragment(Set<Fragment> fragments) {
		super(fragments);
		final Dataset dataset = TDBFactory.createDataset(Config.getInstanceDataLocation()) ;
		System.out.println(Config.getInstanceDataLocation());
		
		
		Queue<Thread> threadsQueue = new LinkedList<>();
		for (Fragment fragment : fragments) {
			Thread thread = new Thread(new Runnable() {
				
				@Override
				public void run() {
					dataset.begin(ReadWrite.READ) ;
					Set<Model> models = new HashSet<Model>();
					for (Signature signature : fragment.getSignatures()) {
						QueryExecution qexec = QueryExecutionFactory.create(createQuery(signature), dataset) ;
						ResultSet results = qexec.execSelect() ;
						Model model = ModelFactory.createDefaultModel();

						for ( ; results.hasNext() ; )
					    {
					      QuerySolution soln = results.nextSolution() ;
					      Property predicate = ResourceFactory.createProperty(signature.getPredicate());
					      
					      model.add(soln.getResource("subject"),predicate , soln.get("object"));
					    }
						models.add(model);
					}
					synchronized (materializedFragments) {
						materializedFragments.put(createFragmentURL(fragment.getId()),models);						
					}
					dataset.end();
				}
			});
			threadsQueue.add(thread);
		}
		
		while (!threadsQueue.isEmpty()) {
			int rounds = Math.min(threadsQueue.size(), Runtime.getRuntime().availableProcessors());
			List<Thread> running = new ArrayList<>();
			for (int i = 0; i < rounds; ++i) {
				Thread thread2Schedule = threadsQueue.poll();
				thread2Schedule.start();
				running.add(thread2Schedule);
			}
			
			for (Thread t: running) {
				try {
					t.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private String createQuery(Signature signature) {
		String query = "";
		query += "Select ?subject ?object "
				+ "FROM <"+signature.getGraphLabel() + ">"+
				" WHERE { ?subject <"+signature.getPredicate()+"> ?object }" ;
		return query;
	}

	@Override
	public String getFragmentURL(Fragment fragment) {
		return createFragmentURL(fragment.getId());
	}
	
	private String createFragmentURL(int id) {
		return Config.getNamespace()+"fragment/"+id;
	}

	@Override
	public Map<String, Set<Model>> getMaterializedFragments() {
		return materializedFragments;
	}

	@Override
	public Model getMaterializedModel(String graph) {
		Model result = ModelFactory.createDefaultModel();
		if (!materializedFragments.isEmpty()) {
			if (materializedFragments.containsKey(graph)) {
				for (Model fragment : materializedFragments.get(graph)) {
					result.add(fragment);
				}
			}
		}
		return result;
	}
}
