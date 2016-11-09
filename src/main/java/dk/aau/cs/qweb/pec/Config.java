package dk.aau.cs.qweb.pec;

public class Config {
	static private String instanceDataLocation;
	static private String cubeStructureLocation;
	static private String provenanceDataLocation;

	public static void setInstanceDataLocation(String optionValue) {
		instanceDataLocation = optionValue;
	}
	
	public static String getInstanceDataLocation() {
		return instanceDataLocation;
	}

	public static void setCubeStructureLocation(String optionValue) {
		cubeStructureLocation = optionValue;
	}
	
	public static String getCubeStructureLocation() {
		return cubeStructureLocation; 
	}

	public static void setProvenanceDataLocation(String optionValue) {
		provenanceDataLocation = optionValue;
	}
	
	public static String getProvenanceDataLocation() {
		return provenanceDataLocation;
	}
}
