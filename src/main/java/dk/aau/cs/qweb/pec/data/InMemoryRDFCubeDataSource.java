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

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;

import dk.aau.cs.qweb.pec.types.Quadruple;
import dk.aau.cs.qweb.pec.types.Signature;


/**
 * Proof-of-concept class that loads an RDF cube into memory
 * @author galarraga
 *
 */
public class InMemoryRDFCubeDataSource implements RDFCubeDataSource {

	private Set<Quadruple<String, String, String, String>> data;
	
	private Map<String, MultiValuedMap<String, Quadruple<String, String, String, String>>> relation2Subject2Tuple;
	
	
	private InMemoryRDFCubeDataSource() {
		data = new LinkedHashSet<>();
		relation2Subject2Tuple = new HashMap<>();
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
			String relation = row[1];
			String subject = row[0];
			MultiValuedMap<String, Quadruple<String, String, String, String>> subject2Tuple = 
					source.relation2Subject2Tuple.get(relation);
			if (subject2Tuple == null) {
				subject2Tuple = new HashSetValuedHashMap<String, Quadruple<String,String,String,String>>();
				source.relation2Subject2Tuple.put(relation, subject2Tuple);
			}
			subject2Tuple.put(subject, quad);
			source.data.add(quad);
		}
			
		return source;		
	}

	@Override
	public Iterator<Quadruple<String, String, String, String>> iterator() {
		return data.iterator();
	}

	@Override
	public long joinCount(Collection<Signature<String, String, String, String>> signatures1,
			Collection<Signature<String, String, String, String>> signatures2) {
		long jointCount = 0;
		for (Signature<String, String, String, String> signature1 : signatures1) {
			String relation1 = signature1.getSecond();
			MultiValuedMap<String, Quadruple<String, String, String, String>> subject2Tuple1 = 
					relation2Subject2Tuple.get(relation1);
			if (subject2Tuple1 == null)
				continue;
			
			for (Signature<String, String, String, String> signature2 : signatures2) {
				String relation2 = signature2.getSecond();
				MultiValuedMap<String, Quadruple<String, String, String, String>> subject2Tuple2 = 
						relation2Subject2Tuple.get(relation2);
				
				if (subject2Tuple2 == null)
					continue;
				
				Set<String> subjects1 = new LinkedHashSet<>(subject2Tuple1.keySet());
				subjects1.retainAll(subject2Tuple2.keySet());
				jointCount += subjects1.size();
			}
		}
		
		return jointCount;
	}
	
}
