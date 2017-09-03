package dk.aau.cs.qweb.pec.queryEvaluation;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.Model;

import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.lattice.Lattice;

public class ResultMaterializedFragments extends MaterializedFragments {

	public ResultMaterializedFragments(Lattice sourceLattice) {
		super(Collections.emptySet(), "", sourceLattice);
	}

	@Override
	public String getFragmentURL(Fragment fragment) {
		return null;
	}

	@Override
	public Map<String, Set<Model>> getMaterializedFragments() {
		return null;
	}

	@Override
	public Model getMaterializedModel(String graph) {
		return null;
	}

}
