package dk.aau.cs.qweb.pec.fragmentsselector;

import java.util.Set;

import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.lattice.Lattice;

/**
 * Interface defines a family of classes that implement a selection strategy (under a given budget) 
 * for the cube fragments defined in a cube lattice.
 * @author galarraga
 *
 */
public interface FragmentsSelector {
	
	public Set<Fragment> select(Lattice lattice, long budget) throws DatabaseConnectionIsNotOpen;

}
