package dk.aau.cs.qweb.pec.types;

/**
 * Template class for tuples of 4 elements.
 * @author galarraga
 */
public class Quadruple {
	
	private String subject;
	
	private String predicate;
	
	private String object;
	
	private String graphLabel;

	public Quadruple(String first, String second, String third, String fourth) {
		setSubject(first);
		setPredicate(second);
		setObject(third);
		setGraphLabel(fourth);
	}

	@Override
	public String toString() {
		return "[" + getSubject() + ", " + getPredicate() + ", " + getObject() + ", " + getGraphLabel() + "]";
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getPredicate() {
		return predicate;
	}

	public void setPredicate(String predicate) {
		this.predicate = predicate;
	}

	public String getObject() {
		return object;
	}

	public void setObject(String object) {
		this.object = object;
	}

	public String getGraphLabel() {
		return graphLabel;
	}

	public void setGraphLabel(String graphLabel) {
		this.graphLabel = graphLabel;
	}
}
