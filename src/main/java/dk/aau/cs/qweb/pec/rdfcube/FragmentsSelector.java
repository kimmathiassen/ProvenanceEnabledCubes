package dk.aau.cs.qweb.pec.rdfcube;

import java.util.Set;

/**
 * Interface defines a family of classes that implement a selection strategy (under a given budget) 
 * for the cube fragments defined in a cube lattice.
 * @author galarraga
 *
 */
public interface FragmentsSelector {
	
	public Set<RDFCubeFragment> select(FragmentLattice lattice, long budget);

}
