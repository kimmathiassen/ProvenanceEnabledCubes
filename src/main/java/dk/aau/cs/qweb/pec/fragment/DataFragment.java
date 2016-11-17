package dk.aau.cs.qweb.pec.fragment;

import dk.aau.cs.qweb.pec.types.Signature;

public class DataFragment extends Fragment {
	

	public DataFragment(String provenanceId, int id) {
		super(provenanceId, id);
	}

	public DataFragment(int id) {
		super(id);
	}

	public DataFragment(Signature<String, String, String, String> relationSignature, int id) {
		super(relationSignature, id);
	}

	@Override
	public boolean isMetadata() {
		return false;
	}
}
