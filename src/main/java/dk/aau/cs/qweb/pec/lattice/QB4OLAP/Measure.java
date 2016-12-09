package dk.aau.cs.qweb.pec.lattice.QB4OLAP;

public class Measure {
	private String subject;
	private String label;
	
	private String Range;

	public Measure (String subject) {
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

	public String getRange() {
		return Range;
	}

	public void setRange(String range) {
		Range = range;
	}

}
