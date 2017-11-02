package dk.aau.cs.qweb.pec.fragmentsSelector;

import java.util.Collections;
import java.util.Set;

import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.lattice.Lattice;
import dk.aau.cs.qweb.pec.logger.Logger;

public class MockupFragmentsSelector extends FragmentsSelector {
	
	public MockupFragmentsSelector(Lattice lattice) {
		super(lattice);
	}

	@Override
	public Set<Fragment> select(long budget, Logger logger) throws DatabaseConnectionIsNotOpen {
		// TODO Auto-generated method stub
		return Collections.emptySet();
	}

}
