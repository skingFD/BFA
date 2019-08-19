package edu.wisc.cs.arc;

/**
 * Runtime exception thrown during ETG generation.
 * @author Aaron Gember-Jacobson (agember@cs.wisc.edu)
 */
public class GeneratorException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public GeneratorException(String msg) {
		super(msg);
	}
	
	public GeneratorException(String msg, Exception ex) {
		super(msg, ex);
	}
}
