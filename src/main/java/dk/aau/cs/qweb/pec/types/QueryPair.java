package dk.aau.cs.qweb.pec.types;

import dk.aau.cs.qweb.pec.queryEvaluation.AnalyticalQuery;
import dk.aau.cs.qweb.pec.queryEvaluation.ProvenanceQuery;

public class QueryPair {

	private ProvenanceQuery provenanceQuery;
	private AnalyticalQuery analyticalQuery;
	
	public QueryPair(ProvenanceQuery pq, AnalyticalQuery aq) {
		setProvenanceQuery(pq);
		setAnalyticalQuery(aq);
	}

	public ProvenanceQuery getProvenanceQuery() {
		return provenanceQuery;
	}

	public void setProvenanceQuery(ProvenanceQuery provenanceQuery) {
		this.provenanceQuery = provenanceQuery;
	}

	public AnalyticalQuery getAnalyticalQuery() {
		return analyticalQuery;
	}

	public void setAnalyticalQuery(AnalyticalQuery analyticalQuery) {
		this.analyticalQuery = analyticalQuery;
	}
	
	@Override
	public String toString() {
		return "("+provenanceQuery+" "+analyticalQuery+")";
	}
	
}
