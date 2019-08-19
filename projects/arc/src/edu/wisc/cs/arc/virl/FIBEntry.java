package edu.wisc.cs.arc.virl;

import org.batfish.representation.Ip;
import org.batfish.representation.Prefix;

import edu.wisc.cs.arc.graphs.PolicyGroup;

/**
 * An entry in a forwarding information base (FIB).
 * @author Aaron Gember-Jacobson (agember@cs.wisc.edu)
 */
public class FIBEntry {
	
	/** Destination prefix */
	private Prefix prefix;
	
	/** IP address of next hop, null if no gateway */
	private Ip nextHop;
	
	/** Name of outgoing interface */
	private String ifaceName;
	
	/**
	 * Create a FIB entry.
	 * @param prefix destination prefix
	 * @param nextHop IP address of next hop, null if no gateway
	 * @param ifaceName name of outgoing interface
	 */
	public FIBEntry(String prefix, String nextHop, String ifaceName) {
		this.prefix = new Prefix(prefix);
		if (nextHop != null) {
			this.nextHop = new Ip(nextHop);
		}
		this.ifaceName = ifaceName;
	}

	/**
	 * Create a FIB entry.
	 * @param prefix destination prefix
	 * @param ifaceName name of outgoing interface
	 */
	public FIBEntry(String prefix, String ifaceName) {
		this(prefix, null, ifaceName);
	}
	
	/**
	 * Determine if the entry matches a particular destination.
	 * @param destination destination to match
	 * @return true if the entry and destination prefixes intersect, otherwise
	 * 		false
	 */
	public boolean matches(PolicyGroup destination) {
		PolicyGroup entry = new PolicyGroup(prefix);
		return entry.intersects(destination);
	}
	
	/**
	 * Get the length of the prefix.
	 * @return length of the prefix
	 */
	public int getPrefixLength() {
		return this.prefix.getPrefixLength();
	}
	
	/**
	 * Determine whether there is a next hop.
	 * @return true if a next hop is specified
	 */
	public boolean hasNextHop() {
		return (this.nextHop != null);
	}
	
	public Ip getNextHop() {
		return this.nextHop;
	}
	
	/**
	 * Get the name of the outgoing interface.
	 * @return name of the outgoing interface, null if none
	 */
	public String getInterface() {
		return this.ifaceName;
	}
	
	@Override
	public String toString() {
		return String.format("%-14s\t%-12s\t%s", this.prefix, this.nextHop, 
				this.ifaceName);
	}
}
