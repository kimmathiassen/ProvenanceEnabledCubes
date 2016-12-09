package dk.aau.cs.qweb.pec.types;

/**
 * Template class for tuples of 4 elements.
 * @author Kim
 */
public class Signature {
	
	private String range;
	
	private String property;
	
	private String domain;
	
	private String provenanceIdentifier;

	public Signature(String first, String second, String third, String fourth) {
		this.setFirst(first);
		this.setSecond(second);
		this.setThird(third);
		this.setFourth(fourth);
	}

	public String getFirst() {
		return range;
	}

	public void setFirst(String first) {
		this.range = first;
	}

	public String getPredicate() {
		return property;
	}

	public void setSecond(String second) {
		this.property = second;
	}

	public String getThird() {
		return domain;
	}

	public void setThird(String third) {
		this.domain = third;
	}

	public String getGraphLabel() {
		return provenanceIdentifier;
	}

	public void setFourth(String fourth) {
		this.provenanceIdentifier = fourth;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((range == null) ? 0 : range.hashCode());
		result = prime * result + ((provenanceIdentifier == null) ? 0 : provenanceIdentifier.hashCode());
		result = prime * result + ((property == null) ? 0 : property.hashCode());
		result = prime * result + ((domain == null) ? 0 : domain.hashCode());
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
		if (range == null) {
			if (other.range != null)
				return false;
		} else if (!range.equals(other.range))
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
		if (domain == null) {
			if (other.domain != null)
				return false;
		} else if (!domain.equals(other.domain))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "[" + range + ", " + property + ", " + domain + ", " + provenanceIdentifier + "]";
	}

}
