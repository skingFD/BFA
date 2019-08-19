package edu.wisc.cs.arc.graphs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.batfish.representation.Ip;
import org.batfish.representation.Prefix;
import org.batfish.representation.SwitchportMode;
import org.batfish.util.SubRange;

import edu.wisc.cs.arc.GeneratorException;
import edu.wisc.cs.arc.graphs.Vertex.VertexType;

public class Interface implements Serializable {
	private static final long serialVersionUID = -6473140553946259836L;
	
	public final static String NAME_PREFIX_PORT_CHANNEL = "Port-channel";
	public final static String NAME_PREFIX_VLAN = "Vlan";
	
	/** Device on which the interface resides */
	private Device device;
	
	/** Name of the interface */
	private String name;
	
	/** Description of the interface */
	private String description;
	
	/** Address prefix assigned to the interface */
	private Prefix prefix;
	
	/** Whether the interface is active */
	private boolean active;
	
	/** OSPF cost associated with the interface */
	private Integer ospfCost;
	
	/** Bandwidth of the interface */
	private Double bandwidth;
	
	/** Name of the filter applied to traffic entering the device on this 
	 * interface */
	private String incomingFilter;
	
	/** Name of the filter applied to traffic leaving the device on this
	 * interface */
	private String outgoingFilter;
	
	/** Access VLAN number */
	private Integer accessVlan;

	/** Allowed VLAN numbers */
	private List<SubRange> allowedVlans;

	/** ETG vertex used to enter the device via this interface */
	private InterfaceVertex inVertex;
	
	/** ETG vertex used to exit the device via this interface */
	private InterfaceVertex outVertex;
	
	/** Interfaces participating in the VLAN or port-channel */
	private List<Interface> subInterfaces;
	
	/** Channel group */
	private Integer channelGroup;
	
	public enum InterfaceType {
		VLAN,
		PORT_CHANNEL,
		LOOPBACK,
		MANAGEMENT,
		ETHERNET,
        TUNNEL
	}
	
	private InterfaceType type;
		
	/**
	 * Creates a simple interface.
	 * @param device
	 * @param name
	 * @param prefix
	 */
	public Interface(Device device, String name, Prefix prefix) {
		this.device = device;
		this.name = name;
		this.prefix = prefix;
		this.active = true;
		this.inVertex = new InterfaceVertex(this, VertexType.IN);
		this.outVertex = new InterfaceVertex(this, VertexType.OUT);
		this.allowedVlans = new ArrayList<SubRange>();
		this.accessVlan = null;
		
		if (name.toLowerCase().contains("vlan")) {
			this.type = InterfaceType.VLAN;
			this.accessVlan = Integer.parseInt(
					name.toLowerCase().replace("vlan", ""));
			this.subInterfaces = new ArrayList<Interface>();
			this.allowedVlans = null;
		} else if (name.contains(NAME_PREFIX_PORT_CHANNEL)) {
			this.type = InterfaceType.PORT_CHANNEL;
			this.subInterfaces = new ArrayList<Interface>();
		} else if (name.toLowerCase().contains("loopback")) {
			this.type = InterfaceType.LOOPBACK;
		} else if (name.toLowerCase().contains("mgmt")) {
			this.type = InterfaceType.MANAGEMENT;
		} else if (name.toLowerCase().contains("ethernet")) {
			this.type = InterfaceType.ETHERNET;
		} else if (name.toLowerCase().contains("tunnel")) {
			this.type = InterfaceType.TUNNEL;
		} else {
			throw new GeneratorException("Unknown interface type: "+name);
		}
	}
	
	/**
	 * Creates an interface based on a Cisco device interface.
	 * @param device the device on which the process is running
	 * @param ciscoIface the Cisco device interface
	 */
	public Interface(Device device, 
			org.batfish.representation.cisco.Interface ciscoIface) {
		this(device, ciscoIface.getName(), ciscoIface.getPrefix());
		this.description = ciscoIface.getDescription();
		this.active = ciscoIface.getActive();
		this.ospfCost = ciscoIface.getOspfCost();
		this.bandwidth = ciscoIface.getBandwidth();
		if (ciscoIface.getAccessVlan() > 0) {
			this.accessVlan = ciscoIface.getAccessVlan();
		}
		this.allowedVlans = ciscoIface.getAllowedVlans();
		if (ciscoIface.getSwitchportMode() == SwitchportMode.TRUNK) {
		    if (0 == this.allowedVlans.size()) {
		        this.allowedVlans.add(new SubRange(1,4095));
		    }
		}
		else if (ciscoIface.getSwitchportMode() == SwitchportMode.ACCESS) {
		    if (0 == this.allowedVlans.size() && this.accessVlan != null) {
		        this.allowedVlans.add(new SubRange(this.accessVlan, 
		                this.accessVlan));
		    }
		}
		this.incomingFilter = ciscoIface.getIncomingFilter();
		this.outgoingFilter = ciscoIface.getOutgoingFilter();
		this.channelGroup = ciscoIface.getChannelGroup();
	}
	
	/**
	 * Get the name of the interface.
	 * @return name of the interface
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Set the name of the interface.
	 * @param name new name for the interface
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Get the description of the interface.
	 * @return description of the interface
	 */
	public String getDescription() {
		return this.description;
	}
	
	/**
	 * Determine if the interface has an prefix assigned.
	 * @return true if a prefix is assigned to the interface; otherwise false
	 */
	public boolean hasPrefix() {
		return (this.prefix != null);
	}
	
	/**
	 * Get the address prefix assigned to the interface, if any.
	 * @return address prefix assigned to the interface; null if none
	 */
	public Prefix getPrefix() {
		return this.prefix;
	}
	
	/**
	 * Get the address assigned to the interface, if any.
	 * @return address assigned to the interface; null if none
	 */
	public Ip getAddress() {
		if (!this.hasPrefix()) {
			return null;
		}
		return this.getPrefix().getAddress();
	}
	
	/**
	 * Determine whether the interface is active.
	 * @return true if the interface is active, otherwise false
	 */
	public boolean getActive() {
		return this.active;
	}
	
	/**
	 * Get the OSPF cost associated with the interface, if specified, or
	 * calculate based on bandwidth, if specified.
	 * @return OSPF cost associated to the interface; null if unspecified
	 */
	public Double getOspfCost() {
		if (this.ospfCost != null) {
			return 1.0 * this.ospfCost;
		}
		else if (this.bandwidth != null) {
			return 1E8 / this.bandwidth;
		}
		return null;
	}
	
	/**
	 * Get the interface's bandwidth, if specified.
	 * @return interface's bandwidth; null if unspecified
	 */
	public Double getBandwidth() {
		return this.bandwidth;
	}
	
	/**
	 * Get the access VLAN number, if specified.
	 * @return access VLAN number; null if unspecified or not in access mode
	 */
	public Integer getAccessVlan() {
		return this.accessVlan;
	}
	
	/**
	 * Get the allowed VLAN numbers.
	 * @return allowed VLAN numbers
	 */
	public List<SubRange> getAllowedVlans() {
		return this.allowedVlans;
	}

	/**
	 * Get the name of the filter applied to traffic entering the device via
	 * this interface.
	 * @return the name of the incoming filter; null if none
	 */
	public String getIncomingFilter() {
		return this.incomingFilter;
	}
	
	/**
	 * Get the name of the filter applied to traffic leaving the device via
	 * this interface.
	 * @return the name of the outgoing filter; null if none
	 */
	public String getOutgoingFilter() {
		return this.outgoingFilter;
	}
	
	/**
	 * Get the ETG vertex used to enter the device via this interface.
	 * @return ETG vertex used to enter the device via this interface.
	 */
	public InterfaceVertex getInVertex() {
		return this.inVertex;
	}
	
	/**
	 * Get the ETG vertex used to exit the device via this interface.
	 * @return ETG vertex used to exit the device via this interface.
	 */
	public InterfaceVertex getOutVertex() {
		return this.outVertex;
	}
	
	/**
	 * Get the device on which the interface resides.
	 * @return the device on which the interface resides
	 */
	public Device getDevice() {
		return this.device;
	}
	
	/**
	 * Get the interfaces participating in a VLAN or port-channel interface.
	 * @return sub interfaces
	 */
	public List<Interface> getSubInterfaces() {
		return this.subInterfaces;
	}
	
	/**
	 * Get the channel group in which the interface participates.
	 * @return channel group number, null if the interface doesn't participate
	 * 		in a channel group
	 */
	public Integer getChannelGroup() {
		return this.channelGroup;
	}
	
	/**
	 * Add an interface participating in a VLAN or port-channel interface.
	 * @param sub interface
	 */
	public void addSubInterface(Interface subInterface) {
		if (null == this.subInterfaces) {
			throw new GeneratorException("Cannot add sub interface to "
					+ this.type + " interface " + this.getName());
		}
		this.subInterfaces.add(subInterface);
	}
	
	/**
	 * Get the type of the interface.
	 * @return interface type
	 */
	public InterfaceType getType() {
		return this.type;
	}
	
	@Override
	public String toString() {
		return this.name;
	}
}
