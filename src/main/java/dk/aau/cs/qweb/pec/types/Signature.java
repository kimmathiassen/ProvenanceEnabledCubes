package dk.aau.cs.qweb.pec.types;

/**
 * Template class for tuples of 4 elements.
 * @author Kim
 */
public class Signature {
	
	private String subject;
	
	private String property;
	
	private String object;
	
	private String provenanceIdentifier;

	public Signature(String subject, String property, String object, String provenanceIdentifier) {
		this.setSubject(subject);
		this.setProperty(property);
		this.setObject(object);
		this.setProvenanceIdentifier(provenanceIdentifier);
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String first) {
		this.subject = first;
	}

	public String getPredicate() {
		return property;
	}

	public void setProperty(String second) {
		this.property = second;
	}

	public String getObject() {
		return object;
	}

	public void setObject(String third) {
		this.object = third;
	}

	public String getGraphLabel() {
		return provenanceIdentifier;
	}

	public void setProvenanceIdentifier(String fourth) {
		this.provenanceIdentifier = fourth;
	}
	
	public int getSpecificity() {
		int specificity = 0;
		if (object != null) ++specificity;
		if (property != null) ++specificity;
		if (subject != null) ++specificity;
		if (provenanceIdentifier != null) ++specificity;
			
		return specificity;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
		result = prime * result + ((provenanceIdentifier == null) ? 0 : provenanceIdentifier.hashCode());
		result = prime * result + ((property == null) ? 0 : property.hashCode());
		result = prime * result + ((object == null) ? 0 : object.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Signature other = (Signature) obj;
		if (subject == null) {
			if (other.subject != null)
				return false;
		} else if (!subject.equals(other.subject))
			return false;
		if (provenanceIdentifier == null) {
			if (other.provenanceIdentifier != null)
				return false;
		} else if (!provenanceIdentifier.equals(other.provenanceIdentifier))
			return false;
		if (property == null) {
			if (other.property != null)
				return false;
		} else if (!property.equals(other.property))
			return false;
		if (object == null) {
			if (other.object != null)
				return false;
		} else if (!object.equals(other.object))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "[" + subject + ", " + property + ", " + object + ", " + provenanceIdentifier + "]";
	}

	/**
	 * Create a clone of the signature
	 * @return
	 */
	public Signature copy() {
		return new Signature(subject, property, object, provenanceIdentifier);
	}

	public void generalize() {
		if (object != null) {
			object = null;
			return;
		}
		
		if (property != null) {
			property = null;
			return;
		}
		
		if (subject != null) {
			subject = null;
			return;
		}
		
		if (provenanceIdentifier != null) {
			provenanceIdentifier = null;
		}
		
	}

}
