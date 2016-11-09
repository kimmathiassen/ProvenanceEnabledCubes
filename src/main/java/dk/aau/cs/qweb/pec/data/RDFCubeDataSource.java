package dk.aau.cs.qweb.pec.data;

import java.util.Collection;

import dk.aau.cs.qweb.pec.types.Quadruple;

public interface RDFCubeDataSource extends Iterable<Quadruple<String, String, String, String>> {

	public long joinCount(Collection<Quadruple<String, String, String, String>> signatures1,
			Collection<Quadruple<String, String, String, String>> signatures2);

}
