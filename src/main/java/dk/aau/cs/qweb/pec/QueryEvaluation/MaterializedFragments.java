package dk.aau.cs.qweb.pec.QueryEvaluation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.Model;

import dk.aau.cs.qweb.pec.fragment.Fragment;

public abstract class MaterializedFragments {
	protected Set<Fragment> fragments = new HashSet<Fragment>();
	
	protected MaterializedFragments(Set<Fragment> fragments) {
		this.fragments = fragments;
	}

	public boolean contains(Fragment fragment) {
		return fragments.contains(fragment);
	}

	public abstract String getFragmentURL(Fragment fragment) ;

	public abstract Map<String,Set<Model>> getMaterializedFragments() ;

	public abstract Model getMaterializedModel(String graph) ;
}
