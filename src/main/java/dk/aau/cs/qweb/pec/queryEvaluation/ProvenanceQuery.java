package dk.aau.cs.qweb.pec.queryEvaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;

public class ProvenanceQuery {

	private String originalQuery;
	private File file;
	private Set<String> provenanceIdentifiers = null;
	long runtime;
	

	public ProvenanceQuery(File queryFile) throws IOException {
		originalQuery = FileUtils.readFileToString(queryFile);
		file = queryFile;
		runtime = -1;
	}
	
	protected ProvenanceQuery(ProvenanceQuery anotherQuery) {
		originalQuery = anotherQuery.originalQuery;
		file = anotherQuery.file;
		runtime = anotherQuery.runtime;
		provenanceIdentifiers = anotherQuery.provenanceIdentifiers;
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
		if (provenanceIdentifiers == null) {
			provenanceIdentifiers = new LinkedHashSet<>();
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
	
	public long getRuntime() {
		return runtime;
	}
	
	public void setRuntime(long runtime) {
		this.runtime = runtime;
	}

	public String getFilename() {
		return file.getName();
	}
	
	public String toString() {
		return getFilename();
	}
	
	public ProvenanceQuery clone() {
		return new ProvenanceQuery(this);
	}
}
