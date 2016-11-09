package dk.aau.cs.qweb.pec.rdfcube;

import dk.aau.cs.qweb.pec.types.Signature;

public class RDFCubeDataFragment extends RDFCubeFragment {

	public RDFCubeDataFragment(String provenanceId) {
		super(provenanceId);
	}

	public RDFCubeDataFragment() {
		super();
	}

	public RDFCubeDataFragment(Signature<String, String, String, String> relationSignature) {
		super(relationSignature);
	}

	@Override
	public boolean isMetadata() {
		return false;
	}
}
