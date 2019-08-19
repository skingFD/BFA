package edu.wisc.cs.arc.verifiers;

/**
 * Runtime exception thrown during network verification.
 * @author Aaron Gember-Jacobson (agember@cs.wisc.edu)
 */
public class VerifierException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public VerifierException(String msg) {
		super(msg);
	}
	
	public VerifierException(String msg, Exception ex) {
		super(msg, ex);
	}
}
