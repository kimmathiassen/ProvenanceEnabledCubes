package dk.aau.cs.qweb.pec.queryEvaluation;

import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.jena.rdf.model.Model;

import dk.aau.cs.qweb.pec.fragment.Fragment;

public abstract class MaterializedFragments {
	protected SortedSet<Fragment> fragments = new TreeSet<Fragment>();
	protected String datasetPath;
	
	protected MaterializedFragments(Set<Fragment> fragments, String datasetPath) {
		this.fragments.addAll(fragments);
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

	public Set<Fragment> getFragments() {
		return fragments;
	}
}
