package dk.aau.cs.qweb.pec.fragment;

import dk.aau.cs.qweb.pec.types.Signature;

public class MetadataFragment extends Fragment {

	public MetadataFragment(Signature<String, String, String, String> relationSignature) {
		super(relationSignature);
	}

	@Override
	public boolean isMetadata() {
		return true;
	}

}
