package dk.aau.cs.qweb.pec.lattice;

import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.fragment.Fragment;

/**
 * A naive merge lattice. This lattice supports iterative merging of fragments when 
 * (1) all quads in the data source have been read by the lattice, and (2) the number of fragments is bigger
 * than the maximal fragments count (1000 by default). Merging is not allowed when the number
 * of fragments is smaller than the minimal fragments count (50 by default).
 * This lattice implements two merging strategies: by property/predicate and by provenance identifier. The
 * first strategy, called property merge, merges fragments with signatures 
 * <null, p, null, :A> and <null, p', null, :A> (same provenance identifier). The strategy 'provenance merge'
 * merges fragments such as <null, null, null, :A> and <null, null, null, :B>. When two fragments are combined,
 * the original fragments are removed from the lattice.
 * @author galarraga
 *
 */
public class NaiveMergeLattice extends MergeLattice {
	

	NaiveMergeLattice(Fragment root, RDFCubeStructure schema, RDFCubeDataSource data) {
		super(root, schema, data);
	}

	/**
	 * It merges the most promising couple of fragments, first by predicate, and if not possible,
	 * by provenance.
	 */
	@Override
	public boolean merge() {
		if (mergingSteps % 2 == 0) {
			if (propertyMerge()) {
				++propertyMergeSteps;
				return true;
			}
			
			if (provenanceMerge()) {
				++provenanceMergeSteps;
				return true;
			}
		} else {
			if (provenanceMerge()) {
				++provenanceMergeSteps;
				return true;
			}
			
			if (propertyMerge()) {
				++propertyMergeSteps;
				return true;
			}
		}
		
		return false;

	}

}
