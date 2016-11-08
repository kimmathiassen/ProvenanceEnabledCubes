package dk.aau.cs.qweb.pec.rdfcube;

import dk.aau.cs.qweb.pec.types.Quadruple;

public class RDFCubeMetadataFragment extends RDFCubeFragment {

	public RDFCubeMetadataFragment(Quadruple<String, String, String, String> relationSignature) {
		super(relationSignature);
	}

	@Override
	public boolean isMetadata() {
		return true;
	}

}
