package dk.aau.cs.qweb.pec.lattice.QB4OLAP;

public class HierarchyStep {

	private String subject;
	private String rollup;
	private String cardinality;
	private String parentLevel;
	private String hierarchy;
	private String childLevel;

	public HierarchyStep(String subject) {
		this.subject = subject;
	}

	public void setChildLevel(String childLevel) {
		this.childLevel = childLevel;
	}

	public void setHierarchy(String hierarchy) {
		this.hierarchy = hierarchy;
	}

	public void setParentLevel(String parentLevel) {
		this.parentLevel = parentLevel;
	}

	public void setCardinality(String cordinality) {
		this.cardinality = cordinality;
	}

	public void setRollup(String rollup) {
		this.rollup = rollup;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getCordinality() {
		return cardinality;
	}

	public void setCordinality(String cordinality) {
		this.cardinality = cordinality;
	}

	public String getRollup() {
		return rollup;
	}

	public String getParentLevel() {
		return parentLevel;
	}

	public String getHierarchy() {
		return hierarchy;
	}

	public String getChildLevel() {
		return childLevel;
	}

	@Override 
	public String toString() {
		return subject + " child: " + childLevel + " parent: " + parentLevel + " hierarachy: " + hierarchy + " rollup: " + rollup + " cardinality: " + cardinality; 
	}
}
