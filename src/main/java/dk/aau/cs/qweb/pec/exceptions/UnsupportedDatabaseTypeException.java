package dk.aau.cs.qweb.pec.exceptions;

public class UnsupportedDatabaseTypeException extends Exception {

	private static final long serialVersionUID = 1L;
	public UnsupportedDatabaseTypeException() { super(); }
	public UnsupportedDatabaseTypeException(String message) { super(message); }
	public UnsupportedDatabaseTypeException(String message, Throwable cause) { super(message, cause); }
	public UnsupportedDatabaseTypeException(Throwable cause) { super(cause); }
}
