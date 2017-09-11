package dk.aau.cs.qweb.pec.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;

import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;

import dk.aau.cs.qweb.pec.exceptions.DatabaseConnectionIsNotOpen;
import dk.aau.cs.qweb.pec.types.Quadruple;


/**
 * Proof-of-concept class that loads an RDF cube into memory. 
 * Note: This class does not support concurrent iteration of the same object.
 * 
 * @author galarraga
 *
 */
public class InMemoryRDFCubeDataSource implements RDFCubeDataSource {

	private Set<Quadruple> data;
	
	private Map<String, Set<String>> relation2Subject;
	
	private Map<String, Set<String>> provid2Subject;

	private boolean open = false;

	private Iterator<Quadruple> iterator;
	
	private InMemoryRDFCubeDataSource() {
		data = new LinkedHashSet<>();
		relation2Subject = new HashMap<>();
		provid2Subject = new HashMap<>();
	}
	
	public static InMemoryRDFCubeDataSource build(Iterable<String[]> quads) {
		InMemoryRDFCubeDataSource source = new InMemoryRDFCubeDataSource();
		for (String[] quad : quads) {
			source.add(new Quadruple(quad[0], quad[1], quad[2], quad[3]));
		}
		
		return source;
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
			Quadruple quad = new Quadruple(row[0], row[1], row[2], row[3]);
			source.add(quad);
		}
		
		source.iterator = source.data.iterator();
		return source;		
	}
	
	/**
	 * Adds a quadruple to the memory store.
	 * @param quad
	 */
	private void add(Quadruple quad) {
		String relation = quad.getPredicate();
		String subject = quad.getSubject();
		String provid = quad.getGraphLabel();
		Set<String> subjects = relation2Subject.get(relation);			
		if (subjects == null) {
			subjects = new LinkedHashSet<>();
			relation2Subject.put(relation, subjects);
		}
		
		Set<String> subjects2 = provid2Subject.get(provid);
		if (subjects2 == null) {
			subjects2 = new LinkedHashSet<>();
			provid2Subject.put(provid, subjects2);
		}
		
		subjects.add(subject);
		subjects2.add(subject);
		data.add(quad);
	}

	public Iterator<Quadruple> iterator() {
		return data.iterator();
	}

	@Override
	public void open() {
		open = true;
		iterator = data.iterator();
		
	}

	@Override
	public void close() {
		open = false;
		iterator = data.iterator();
		
	}

	@Override
	public Quadruple next() throws DatabaseConnectionIsNotOpen {
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

	@Override
	public int count() {
		return data.size();
	}

	public static RDFCubeDataSource build(String datasetPath, String cachingStrategy) throws IOException {
		return InMemoryRDFCubeDataSource.build(datasetPath);
	}

	@Override
	public List<Map<String, String>> execSelectQuery(String selectQuery) {
		throw new NotImplementedException("execSelectQuery not implemented for class InMemoryRDFCubeDataSource");
	}

	
}
