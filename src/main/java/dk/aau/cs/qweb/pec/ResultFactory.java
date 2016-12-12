package dk.aau.cs.qweb.pec;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

import dk.aau.cs.qweb.pec.QueryEvaluation.AnalyticalQuery;
import dk.aau.cs.qweb.pec.QueryEvaluation.MaterializedFragments;

public abstract class ResultFactory {

	public abstract Set<String> evaluate(ProvenanceQuery analyticalQuery) throws FileNotFoundException, IOException ;

	public abstract String evaluate(MaterializedFragments materializedFragment, AnalyticalQuery analyticalQuery) ;

	public abstract String evaluate(Set<String> provenanceIdentifiers) ;

}
