package dk.aau.cs.qweb.pec.exceptions;

public class UnsupportedDatabaseTypeException extends Exception {

	public UnsupportedDatabaseTypeException() { super(); }
	public UnsupportedDatabaseTypeException(String message) { super(message); }
	public UnsupportedDatabaseTypeException(String message, Throwable cause) { super(message, cause); }
	public UnsupportedDatabaseTypeException(Throwable cause) { super(cause); }
}
