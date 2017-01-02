package dk.aau.cs.qweb.pec.lattice.QB4OLAP;

import java.util.ArrayList;
import java.util.List;

public class Hierarchy {

	private String subject;
	private List<String> inDimension = new ArrayList<String>();
	private String label;
	private List<String> levels = new ArrayList<String>();

	public Hierarchy(String subject) {
		this.subject = subject;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void addInDimension(String dimension) {
		this.inDimension.add(dimension);
	}

	public void addLevel(String level) {
		levels.add(level);
	}
	
	public String getLabel() {
		return label;
	}
	
	public String getSubject() {
		return subject;
	}
}
