package dk.aau.cs.qweb.pec.lattice.QB4OLAP;

import java.util.ArrayList;
import java.util.List;

public class Dimension {
	private String subject;
	private String label;

	private List<Level> levels = new ArrayList<Level>();
	private List<String> heirarchies = new ArrayList<String>();

	public Dimension (String subject) {
		this.subject = subject;
	}
	
	@Override
	public boolean equals(Object o) {
		return o.equals(subject) ? true : false;
	}
	
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
	
	public void addHierarchy(String heirarchy) {
		heirarchies.add(heirarchy);
	}

}
