package dk.aau.cs.qweb.pec.rdfcube.fragment;

import dk.aau.cs.qweb.pec.types.Signature;

public class RDFCubeMetadataFragment extends RDFCubeFragment {

	public RDFCubeMetadataFragment(Signature<String, String, String, String> relationSignature) {
		super(relationSignature);
	}

	@Override
	public boolean isMetadata() {
		return true;
	}

}
