package dk.aau.cs.qweb.pec.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;

import dk.aau.cs.qweb.pec.types.Quadruple;


/**
 * Proof-of-concept class that loads an RDF cube into memory
 * @author galarraga
 *
 */
public class InMemoryRDFCubeDataSource implements RDFCubeDataSource {

	private Set<Quadruple<String, String, String, String>> data;
	
	private MultiValuedMap<String, Quadruple<String, String, String, String>> subject2Tuple;
	
	private MultiValuedMap<String, Quadruple<String, String, String, String>> object2Tuple;
	
	
	private InMemoryRDFCubeDataSource() {
		data = new LinkedHashSet<>();
		subject2Tuple = new HashSetValuedHashMap<>();
		object2Tuple = new HashSetValuedHashMap<>();
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
			source.subject2Tuple.put(row[0], quad);
			source.object2Tuple.put(row[2], quad);
		}
			
		return source;		
	}

	@Override
	public Iterator<Quadruple<String, String, String, String>> iterator() {
		return data.iterator();
	}
	
}
