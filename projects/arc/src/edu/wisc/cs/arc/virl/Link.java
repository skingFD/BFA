package edu.wisc.cs.arc.virl;

/**
 * A physical link in a failure scenario.
 * @author Aaron Gember-Jacobson (agember@cs.wisc.edu)
 */
public class Link {
	/** Name of the device at one end of the link */
	private String srcDeviceName;
	
	/** Name of the device at the other end of the link */
	private String dstDeviceName;
	
	/** Name of the interface at one end of the link */
	private String srcIfaceName;
	
	/** Name of the interface at the other end of the link */
	private String dstIfaceName;
	
	/**
	 * Create a link.
	 * @param srcDeviceName name of the device at one end of the link
	 * @param dstDeviceName name of the device at the other end of the link
	 */
	public Link(String srcDeviceName,String dstDeviceName) {
		this(srcDeviceName, null, dstDeviceName, null);
	}
	
	/**
	 * Create a link.
	 * @param srcDeviceName name of the device at one end of the link
	 * @param srcIfaceName name of the interface at one end of the link
	 * @param dstDeviceName name of the device at the other end of the link
	 * @param dstIfaceName name of the interface at the other end of the link
	 */
	public Link(String srcDeviceName, String srcIfaceName, String dstDeviceName,
			String dstIfaceName) {
		this.srcDeviceName = srcDeviceName;
		this.srcIfaceName = srcIfaceName;
		this.dstDeviceName = dstDeviceName;
		this.dstIfaceName = dstIfaceName;
	}
	
	/**
	 * Get the name of the device at one end of the link.
	 * @return device nameone
	 */
	public String getSourceDeviceName() {
		return this.srcDeviceName;
	}
	
	/**
	 * Get the name of the device at the other end of the link.
	 * @return device name
	 */
	public String getDestinationDeviceName() {
		return this.dstDeviceName;
	}
	
	/**
	 * Get the name of the interface at one end of the link.
	 * @return interface name
	 */
	public String getSourceInterfaceName() {
		return this.srcIfaceName;
	}
	
	/**
	 * Get the name of the interface at the other end of the link.
	 * @return interface name
	 */
	public String getDestinationInterfaceName() { 
		return this.dstIfaceName;
	}
	
	@Override
	public String toString() {
		return this.srcDeviceName + " -- " + this.dstDeviceName;
	}
}
