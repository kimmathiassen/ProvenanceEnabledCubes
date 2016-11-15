package dk.aau.cs.qweb.pec.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;

import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.types.Quadruple;
import dk.aau.cs.qweb.pec.types.Signature;


/**
 * Proof-of-concept class that loads an RDF cube into memory
 * @author galarraga
 *
 */
public class InMemoryRDFCubeDataSource implements RDFCubeDataSource {

	private Set<Quadruple<String, String, String, String>> data;
	
	private Map<String, Set<String>> relation2Subject;
	
	private Map<String, Set<String>> provid2Subject;

	private boolean open = false;

	private Iterator<Quadruple<String, String, String, String>> iterator;
	
	
	private InMemoryRDFCubeDataSource() {
		data = new LinkedHashSet<>();
		relation2Subject = new HashMap<>();
		provid2Subject = new HashMap<>();
	}
	
	/**
	 * It builds an in-memory source from a file path. The method assumes the file is given as
	 * quadruples in TSV format: subject relation object provenance-id
	 * @param filePath
	 * @return
	 * @throws IOException 
	 */
	public static InMemoryRDFCubeDataSource build(String filePath) throws IOException {
		InMemoryRDFCubeDataSource source = new InMemoryRDFCubeDataSource();

		TsvParserSettings settings = new TsvParserSettings();
		settings.getFormat().setLineSeparator("\n");
		TsvParser parser = new TsvParser(settings);

		parser.beginParsing(new BufferedReader(new FileReader(filePath)));

		String[] row;
		while ((row = parser.parseNext()) != null) {
			Quadruple<String, String, String, String> quad = 
					new Quadruple<>(row[0], row[1], row[2], row[3]);
			source.data.add(quad);
			String relation = row[1];
			String subject = row[0];
			String provid = row[3];
			Set<String> subjects = 
					source.relation2Subject.get(relation);			
			if (subjects == null) {
				subjects = new LinkedHashSet<>();
				source.relation2Subject.put(relation, subjects);
			}
			
			Set<String> subjects2 = 
					source.provid2Subject.get(provid);
			if (subjects2 == null) {
				subjects2 = new LinkedHashSet<>();
				source.provid2Subject.put(provid, subjects2);
			}
			
			subjects.add(subject);
			subjects2.add(subject);
			source.data.add(quad);

		}
		source.iterator = source.data.iterator();
		return source;		
	}

	public Iterator<Quadruple<String, String, String, String>> iterator() {
		return data.iterator();
	}
	
	private Set<String> getSubjectsForSignature(Signature<String, String, String, String> signature) {
		String relation = signature.getSecond();
		Set<String> subjects = new LinkedHashSet<>();
		if (relation != null) {
			Set<String> subjectsForRelation1 = relation2Subject.get(relation);
			if (subjectsForRelation1 != null)
				subjects.addAll(subjectsForRelation1);
		}
		
		String provid = signature.getFourth();
		Set<String> subjectsForProvid = provid2Subject.get(provid);
		if (relation == null) {	
			if (subjectsForProvid != null) {
				subjects.addAll(subjectsForProvid);
			}
		} else {
			if (subjectsForProvid != null) {
				subjects.retainAll(subjectsForProvid);
			}
		}
		
		return subjects;
	
	}

	@Override
	public long joinCount(Collection<Signature<String, String, String, String>> signatures1,
			Collection<Signature<String, String, String, String>> signatures2) throws DatabaseConnectionIsNotOpen {
		isConnectionOpen();
		long jointCount = 0;
		for (Signature<String, String, String, String> signature1 : signatures1) {
			Set<String> subjects1 = getSubjectsForSignature(signature1);
			
			for (Signature<String, String, String, String> signature2 : signatures2) {
				Set<String> subjects2 = getSubjectsForSignature(signature2);
				subjects1.retainAll(subjects2);
				jointCount += subjects1.size();
			}
		}
		
		return jointCount;
	}

	@Override
	public void open() {
		open = true;
		
	}

	@Override
	public void close() {
		open = false;
		
	}

	@Override
	public Quadruple<String, String, String, String> next() throws DatabaseConnectionIsNotOpen {
		isConnectionOpen();
		return iterator.next();
	}

	private void isConnectionOpen() throws DatabaseConnectionIsNotOpen {
		if (!open) {
			throw new DatabaseConnectionIsNotOpen();
		}
	}

	@Override
	public Boolean hasNext() throws DatabaseConnectionIsNotOpen {
		isConnectionOpen();
		return iterator.hasNext();
	}

	
}
