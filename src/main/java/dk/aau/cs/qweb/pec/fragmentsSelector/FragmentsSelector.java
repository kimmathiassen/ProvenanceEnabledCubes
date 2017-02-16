package dk.aau.cs.qweb.pec.fragmentsSelector;

import java.io.FileNotFoundException;
import java.util.Set;

import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.lattice.Lattice;
import dk.aau.cs.qweb.pec.logger.Logger;

/**
 * Interface that defines a family of classes that implement a selection strategy (under a given budget) 
 * for the cube fragments defined in a cube lattice.
 * @author galarraga
 *
 */
public abstract class FragmentsSelector {
	
	protected Lattice lattice;
	
	protected String logFile;
	
	protected boolean loggingEnabled;
	
	protected FragmentsSelector(Lattice lattice) {
		this.lattice = lattice;
		this.loggingEnabled = true;
	}
	
	protected FragmentsSelector(Lattice lattice, String logFile) throws FileNotFoundException {
		this.lattice = lattice;
		this.logFile = logFile;
		this.loggingEnabled = true;
	}
	
	public void setLoggingEnabled(boolean loggingEnabled) {
		this.loggingEnabled = loggingEnabled;
	}
	
	public boolean getLoggingEnabled() {
		return loggingEnabled;
	}
	
	public abstract Set<Fragment> select(long budget, Logger logger) throws DatabaseConnectionIsNotOpen;

}
