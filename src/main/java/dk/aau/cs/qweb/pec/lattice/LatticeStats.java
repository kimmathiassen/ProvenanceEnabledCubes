package dk.aau.cs.qweb.pec.lattice;

import java.util.Set;

import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.types.Signature;

public class LatticeStats {

	public int[] specificity;
	
	public LatticeStats() {
		specificity = new int[] {0, 0, 0, 0, 0};
	}
	
	public static LatticeStats getStats(Set<Fragment> selectedFragments) {
		LatticeStats stats = new LatticeStats();
		for (Fragment fragment : selectedFragments) {
			for (Signature signature : fragment.getSignatures()) {
				stats.specificity[signature.getSpecificity()]++;	
			}
		}
		
		return stats;
	}

}
