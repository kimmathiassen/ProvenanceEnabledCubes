package dk.aau.cs.qweb.pec.QueryEvaluation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
		Dataset dataset = TDBFactory.createDataset(Config.getInstanceDataLocation()) ;
		System.out.println(Config.getInstanceDataLocation());
		dataset.begin(ReadWrite.READ) ;
		
		for (Fragment fragment : fragments) {
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
			materializedFragments.put(createFragmentURL(fragment.getId()),models);
		}
		dataset.end();
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
			for (Model fragment : materializedFragments.get(graph)) {
				result.add(fragment);
			}
		}
		return result;
	}
}
