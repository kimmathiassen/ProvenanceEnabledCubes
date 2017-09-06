package dk.aau.cs.qweb.pec.queryEvaluation;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.Model;

import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.lattice.Lattice;

public class ResultMaterializedFragments extends MaterializedFragments {

	private MaterializedFragments source;
	
	public ResultMaterializedFragments(MaterializedFragments source, Lattice sourceLattice) {
		super(Collections.emptySet(), "", sourceLattice);
		this.source = source;
	}

	@Override
	public String getFragmentURL(Fragment fragment) {
		return source.getFragmentURL(fragment);
	}

	@Override
	public Map<String, Set<Model>> getMaterializedFragments() {
		return source.getMaterializedFragments();
	}

	@Override
	public Model getMaterializedModel(String graph) {
		return source.getMaterializedModel(graph);
	}

}
