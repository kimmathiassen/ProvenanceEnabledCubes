package dk.aau.cs.qweb.pec;

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
		if (originalQuery.contains("SELECT")) {
			return true;
		} else {
			return false;
		}
	}

	public Set<String> getProvenanceIdentifiers() throws FileNotFoundException, IOException {
		if (provenanceIdentifiers.isEmpty()) {
			try(BufferedReader br = new BufferedReader(new FileReader(file))) {
			    for(String line; (line = br.readLine()) != null; ) {
			    	provenanceIdentifiers.add(line);
			    }
			}
			return provenanceIdentifiers;
		} else {
			return provenanceIdentifiers;
		}
	}
	
	public String getFilename() {
		return file.getName();
	}
	
	public String toString() {
		return getFilename();
	}
}
