package dk.aau.cs.qweb.pec.queryEvaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;

public class ProvenanceQuery {

	private String originalQuery;
	private File file;
	private Set<String> provenanceIdentifiers = new HashSet<String>();
	

	public ProvenanceQuery(File queryFile) throws IOException {
		originalQuery = FileUtils.readFileToString(queryFile);
		file = queryFile;
	}

	public String getQuery() {
		return originalQuery;
	}

	public boolean isQuery() {		
		if (originalQuery.toLowerCase().contains("select")) {
			return true;
		} else {
			return false;
		}
	}

	public Set<String> getProvenanceIdentifiers() throws FileNotFoundException, IOException {
		if (provenanceIdentifiers.isEmpty()) {
			try(BufferedReader br = new BufferedReader(new FileReader(file))) {
			    for(String line; (line = br.readLine()) != null; ) {
			    	provenanceIdentifiers.add(removeUnwantedChars(line));
			    }
			}
			return provenanceIdentifiers;
		} else {
			return provenanceIdentifiers;
		}
	}
	
	private String removeUnwantedChars(String line) {
		if (line.startsWith("<") && line.endsWith(">")) {
			line = line.substring(1, line.length()-1);
		}
		return line;
	}

	public String getFilename() {
		return file.getName();
	}
	
	public String toString() {
		return getFilename();
	}
	
	public ProvenanceQuery clone() {
		try {
			return new ProvenanceQuery(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}
