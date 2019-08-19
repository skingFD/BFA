package edu.wisc.cs.arc.modifiers;

/**
 * Runtime exception thrown during ETG modification.
 * @author Aaron Gember-Jacobson (agemberjacobson@colgate.edu)
 */
public class ModifierException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public ModifierException(String msg) {
		super(msg);
	}
	
	public ModifierException(String msg, Exception ex) {
		super(msg, ex);
	}
}
