package dk.aau.cs.qweb.pec.rdfcube;

import dk.aau.cs.qweb.pec.types.Quadruple;

public class RDFCubeDataFragment extends RDFCubeFragment {

	public RDFCubeDataFragment(String provenanceId) {
		super(provenanceId);
	}

	public RDFCubeDataFragment() {
		super();
	}

	public RDFCubeDataFragment(Quadruple<String, String, String, String> relationSignature) {
		super(relationSignature);
	}

	@Override
	public boolean isMetadata() {
		return false;
	}
}
