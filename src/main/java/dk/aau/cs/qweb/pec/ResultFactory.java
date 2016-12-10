package dk.aau.cs.qweb.pec;

import java.util.List;
import java.util.Set;

import dk.aau.cs.qweb.pec.QueryEvaluation.AnalyticalQuery;
import dk.aau.cs.qweb.pec.QueryEvaluation.MaterializedFragments;

public abstract class ResultFactory {

	public abstract List<String> evaluate(Set<ProvenanceQuery> analyticalQuery) ;

	public abstract String evaluate(MaterializedFragments materializedFragment, AnalyticalQuery analyticalQuery) ;

}
