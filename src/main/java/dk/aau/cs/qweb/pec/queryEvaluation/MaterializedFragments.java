package dk.aau.cs.qweb.pec.queryEvaluation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.Model;

import dk.aau.cs.qweb.pec.fragment.Fragment;

public abstract class MaterializedFragments {
	protected Set<Fragment> fragments = new HashSet<Fragment>();
	protected String datasetPath;
	
	protected MaterializedFragments(Set<Fragment> fragments, String datasetPath) {
		this.fragments = fragments;
		this.datasetPath = datasetPath;
	}

	public boolean contains(Fragment fragment) {
		return fragments.contains(fragment);
	}

	public abstract String getFragmentURL(Fragment fragment) ;

	public abstract Map<String,Set<Model>> getMaterializedFragments() ;

	public abstract Model getMaterializedModel(String graph) ;
	
	@Override
	public String toString() {
		return datasetPath + " "+ fragments.toString();
	}
	
	public int size() {
		return fragments.size();
	}
}
