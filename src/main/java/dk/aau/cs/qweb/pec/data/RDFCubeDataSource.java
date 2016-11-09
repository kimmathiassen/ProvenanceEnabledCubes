package dk.aau.cs.qweb.pec.data;

import java.util.Collection;

import dk.aau.cs.qweb.pec.types.Quadruple;
import dk.aau.cs.qweb.pec.types.Signature;

public interface RDFCubeDataSource extends Iterable<Quadruple<String, String, String, String>> {

	public long joinCount(Collection<Signature<String, String, String, String>> signatures1,
			Collection<Signature<String, String, String, String>> signatures2);

}
