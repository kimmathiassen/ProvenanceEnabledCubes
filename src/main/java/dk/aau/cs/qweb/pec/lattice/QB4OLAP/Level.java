package dk.aau.cs.qweb.pec.lattice.QB4OLAP;

import java.util.ArrayList;
import java.util.List;

public class Level {

	private String subject;
	private String label;
	private List<String> attributes = new ArrayList<String>();
	
	public Level (String subject) {
		this.subject = subject;
	}
	
	public String getSubject() {
		return subject;
	}
	
	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}

	public void addAttribute(String attribute) {
		attributes.add(attribute);
	}
}
