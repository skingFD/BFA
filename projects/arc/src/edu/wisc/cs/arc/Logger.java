package edu.wisc.cs.arc;

/**
 * Interface for writing error/debug messages to the console.
 * @author Aaron Gember-Jacobson
 */
public class Logger {
    public enum Level
    { FATAL, ERROR, WARN, INFO, DEBUG }
    
    private Level levelEnabled;
    
    /**
     * Create a logger.
     * @param level lowest level of messages to log
     */
    public Logger(Level level) {
    	this.levelEnabled = level;
    }
    
    /**
     * Log a fatal message.
     * @param msg message to log
     */
    public void fatal(String msg) {
		this.write(msg, Level.FATAL);
	}
    
    /**
     * Log an error message.
     * @param msg message to log
     */
    public void error(String msg) {
		this.write(msg, Level.ERROR);
	}
    
    /**
     * Log a warning message.
     * @param msg message to log
     */
    public void warn(String msg) {
		this.write(msg, Level.WARN);
	}

    /**
     * Log an informational message.
     * @param msg message to log
     */
	public void info(String msg) {
		this.write(msg, Level.INFO);
	}
	
	/**
     * Log a debug message.
     * @param msg message to log
     */
    public void debug(String msg) {
		this.write(msg, Level.DEBUG);
	}
	
    /**
     * Output a message to the log if the level is higher than or equal to
     * the currently enabled logging level.
     * @param msg message to log
     * @param level level of message to log
     */
	private void write(String msg, Level level) {
		if (this.levelEnabled.compareTo(level) >= 0) {
			System.out.println(msg); 
		}
	}
}
