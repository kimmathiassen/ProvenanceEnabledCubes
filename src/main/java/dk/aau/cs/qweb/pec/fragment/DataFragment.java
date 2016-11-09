package dk.aau.cs.qweb.pec.fragment;

import dk.aau.cs.qweb.pec.types.Signature;

public class DataFragment extends Fragment {

	public DataFragment(String provenanceId) {
		super(provenanceId);
	}

	public DataFragment() {
		super();
	}

	public DataFragment(Signature<String, String, String, String> relationSignature) {
		super(relationSignature);
	}

	@Override
	public boolean isMetadata() {
		return false;
	}
}
