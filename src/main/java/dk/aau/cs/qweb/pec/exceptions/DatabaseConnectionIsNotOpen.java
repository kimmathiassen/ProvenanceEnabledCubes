package dk.aau.cs.qweb.pec.exceptions;

public class DatabaseConnectionIsNotOpen extends Exception {
	private static final long serialVersionUID = 1L;
	public DatabaseConnectionIsNotOpen() { super(); }
	public DatabaseConnectionIsNotOpen(String message) { super(message); }
	public DatabaseConnectionIsNotOpen(String message, Throwable cause) { super(message, cause); }
	public DatabaseConnectionIsNotOpen(Throwable cause) { super(cause); }
}
