package edu.wisc.cs.arc.graphs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.batfish.representation.Ip;
import org.batfish.representation.Prefix;
import org.batfish.representation.cisco.BgpProcess;
import org.batfish.representation.cisco.CiscoVendorConfiguration;
import org.batfish.representation.cisco.ExtendedAccessList;
import org.batfish.representation.cisco.OspfNetwork;
import org.batfish.representation.cisco.OspfProcess;
import org.batfish.representation.cisco.PrefixList;
import org.batfish.representation.cisco.RouteMap;
import org.batfish.representation.cisco.StandardAccessList;
import org.batfish.representation.cisco.StaticRoute;
import org.batfish.util.SubRange;

import edu.wisc.cs.arc.GeneratorException;
import edu.wisc.cs.arc.Logger;
import edu.wisc.cs.arc.graphs.Interface.InterfaceType;

/**
 * A device in a network.
 * @author Aaron Gember-Jacobson (agember@cs.wisc.edu)
 */
public class Device implements Serializable {
	private static final long serialVersionUID = -9130905482529614009L;

	/** Name */
	private String name;
	
	/** Interfaces on the device */
	private List<Interface> interfaces;
	
	/** Routing processes on the device */
	private List<Process> routingProcesses;
	
	/** Standard ACLs on the device */
	private Map<String, StandardAccessList> standardAcls;
	
	/** Extended ACLs on the device */
	private Map<String, ExtendedAccessList> extendedAcls;
	
	/** Route maps on the device */
	private Map<String, RouteMap> routeMaps;
	
	/** Prefix lists on the device */
	private Map<String, PrefixList> prefixLists;
	
	/** Whether the device is external */
	private boolean external;
	
	/**
	 * Create a device.
	 * @param name name of the device
	 */
	public Device(String name) {
		this.name = name;
		this.interfaces = new ArrayList<Interface>();
		this.routingProcesses = new ArrayList<Process>();
		this.standardAcls = null;
		this.extendedAcls = null;
		this.routeMaps = null;
		this.prefixLists = null;
		this.external = true;
	}
	
	/**
	 * Create a device.
	 * @param name name of the device
	 * @param standardAcls standard ACLs on the device
	 * @param extendedAcls extended ACLs on the device
	 * @param routeMaps route maps on the device
	 * @param prefixLists prefix lists on the device
	 */
	public Device(String name, Map<String, StandardAccessList> standardAcls,
			Map<String, ExtendedAccessList> extendedAcls,
			Map<String, RouteMap> routeMaps, 
			Map<String, PrefixList> prefixLists) {
		this(name);
		this.standardAcls = standardAcls;
		this.extendedAcls = extendedAcls;
		this.routeMaps = routeMaps;
		this.prefixLists = prefixLists;
		this.external = false;
	}
	
	/**
	 * Create a device from a Cisco device configuration
	 * @param name name of the device
	 * @param config configuration for the device
	 * @param logger
	 */
	public Device(String name, CiscoVendorConfiguration config, Logger logger) {
		this(name, config.getStandardAcls(), 
				config.getExtendedAcls(), config.getRouteMaps(), 
				config.getPrefixLists());

		// Extract interfaces
		for (org.batfish.representation.cisco.Interface ciscoIface : 
				config.getInterfaces().values()) {
			
			Interface iface = new Interface(this, ciscoIface);
			
			/*// Make sure interface is active
			if (!iface.getActive()) {
				continue;
			}
			
			// Get address interface
			Prefix prefix = iface.getPrefix();
			if (prefix == null) {
				//this.logger.warn("Interface " + device + "." 
				//		+ iface.getName() + " does not have a prefix");
				continue;
			}*/
			
			this.addInterface(iface);
		}
		
		// Set up sub interfaces
		for (Interface iface : this.getInterfaces()) {
			if (iface.getChannelGroup() != null) {
				Interface portChannel = this.getInterface(
						Interface.NAME_PREFIX_PORT_CHANNEL 
						+ iface.getChannelGroup());
				if (null == portChannel) {
					throw new GeneratorException("No port channel " 
							+ iface.getChannelGroup() + " for interface "
							+ iface.getDevice() + ":" + iface.getName());
				}
				else {
					portChannel.addSubInterface(iface);
				}
			}
			if (iface.getType() != InterfaceType.VLAN 
					&& iface.getAccessVlan() != null) {
				Interface vlan = this.getInterface(Interface.NAME_PREFIX_VLAN
						+ iface.getAccessVlan());
				if (null == vlan) {
					logger.warn("No vlan " + iface.getAccessVlan() 
							+ " for interface " + iface.getDevice() + ":" 
							+ iface.getName());
				}
				else {
					vlan.addSubInterface(iface);
				}
			}
            else if (iface.getType() != InterfaceType.VLAN 
                    && iface.getAllowedVlans() != null) {
                for (Interface tmpiface : this.getInterfaces()) {
                    if (tmpiface.getType() != InterfaceType.VLAN
                            || null == tmpiface.getAccessVlan()) {
                        continue;
                    }
                    boolean allowed = false;
                    for (SubRange range : iface.getAllowedVlans()) {
                        if (tmpiface.getAccessVlan() >= range.getStart()
                                && tmpiface.getAccessVlan() <= range.getEnd()) {
                            allowed = true;
                            break;
                        }
                    }
                    if (allowed) {
                        tmpiface.addSubInterface(iface);
                    }
                }
            }
		}
		
		// Extract routing processes
		
		// Extract BGP processes
		for (BgpProcess bgpProcess : config.getBgpProcesses().values()) {
			// Determine interfaces participating in the process
			Collection<Interface> bgpInterfaces = new ArrayList<Interface>();
			for (Ip remotePeer : bgpProcess.getIpPeerGroups().keySet()) {
				// Check if each interface can be used to reach peer
				for (Interface localInterface : this.getInterfaces()) {
					if (localInterface.hasPrefix() 
							&& localInterface.getPrefix().contains(remotePeer)){
						bgpInterfaces.add(localInterface);
						break;
					}
				}
			}
			this.addRoutingProcess(new Process(this, bgpProcess, 
					bgpInterfaces));
		}
		
		// Extract OSPF processes
		for (OspfProcess ospfProcess : config.getOspfProcesses()) {			
			// Determine interfaces participating in the process
			Collection<Interface> ospfInterfaces = new ArrayList<Interface>();
			// Consider every prefix that is advertised by the OSPF process
			for (OspfNetwork network : ospfProcess.getNetworks()) {
				Prefix prefix = network.getPrefix();

				// Check if each interface is within the prefix
				for (Interface iface : this.getInterfaces()) {
					// Do not consider black-listed interfaces
					if (ospfProcess.getInterfaceBlacklist().contains(
							iface.getName())) {
						continue;
					}
					
					// If interfaces do not participate by default (i.e., 
					// interfaces are passive), then only consider white-listed 
					// interfaces
					if (ospfProcess.getPassiveInterfaceDefault()) {
						if (!ospfProcess.getInterfaceWhitelist().contains(
								iface.getName())) {
							continue;
						}
					}
					
					// Check if interface is within the prefix
					if (iface.hasPrefix() 
							&& prefix.contains(iface.getAddress())) {
						ospfInterfaces.add(iface);
					}
				}
			}
			
			this.addRoutingProcess(new Process(this, ospfProcess, 
					ospfInterfaces));
		}
		
		// Extract static routes
		for (StaticRoute staticRoute : config.getStaticRoutes()) {
			List<Interface> staticInterfaces = new ArrayList<Interface>();
			if (staticRoute.getNextHopInterface() != null
					&& !staticRoute.getNextHopInterface().startsWith("Null")) {
				Interface iface = this.getInterface(
						staticRoute.getNextHopInterface());
				if (null == iface) {
					logger.warn(this.getName() + " does not have the interface " 
							+ staticRoute.getNextHopInterface() 
							+ " specified as the next hop for the static route "
							+ staticRoute.toString());
				}
				else {
					staticInterfaces.add(iface);
				}
			}
			else if (staticRoute.getNextHopIp() != null) {
				for (Interface iface : this.getInterfaces()) {
					if (iface.hasPrefix() && iface.getPrefix().contains(
							staticRoute.getNextHopIp())) {
						staticInterfaces.add(iface);
						break;
					}
				}
			}
			
			this.addRoutingProcess(new Process(this, staticRoute, 
					staticInterfaces));
		}
	}
	
	/**
	 * Get the name of the device.
	 * @return name of the device
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Add an interface to the device.
	 * @param iface interface to add
	 */
	public void addInterface(Interface iface) {
		this.interfaces.add(iface);
	}
	
	/**
	 * Get the interfaces on the device.
	 * @return interfaces on the device
	 */
	public Collection<Interface> getInterfaces() {
		return this.interfaces;
	}
	
	/**
	 * Get a specific interface on the device.
	 * @param name name of the interface
	 * @return interfaces on the device with the given name; null if none exists
	 */
	public Interface getInterface(String name) {
		for (Interface iface : this.interfaces) {
			if (iface.getName().equals(name)) {
				return iface;
			}
		}
		return null;
	}
	
	/**
	 * Add a routing process to the device.
	 * @param process routing process to add
	 */
	public void addRoutingProcess(Process process) {
		this.routingProcesses.add(process);
	}
	
	/**
	 * Get the routing processes running on the device.
	 * @return routing processes running on the device
	 */
	public List<Process> getRoutingProcesses() {
		return this.routingProcesses;
	}
	
	/**
	 * Get a specific standard ACL on the device.
	 * @param name name of the ACL
	 * @return the standard ACL with the given name; null if none exists
	 */
	public StandardAccessList getStandardAcl(String name) {
		if (this.standardAcls == null) {
			return null;
		}
		return this.standardAcls.get(name);
	}
	
	/**
	 * Get a specific extended ACL on the device.
	 * @param name name of the ACL
	 * @return the extended ACL with the given name; null if none exists
	 */
	public ExtendedAccessList getExtendedAcl(String name) {
		if (this.extendedAcls == null) {
			return null;
		}
		return this.extendedAcls.get(name);
	}
	
	/**
	 * Get a specific route map on the device.
	 * @param name name of the route map
	 * @return the route map with the given name; null if none exists
	 */
	public RouteMap getRouteMap(String name) {
		if (this.routeMaps == null) {
			return null;
		}
		return this.routeMaps.get(name);
	}
	
	/**
	 * Get a specific prefix list on the device.
	 * @param name name of the prefix list
	 * @return the prefix list with the given name; null if none exists
	 */
	public PrefixList getPrefixList(String name) {
		if (this.prefixLists == null) {
			return null;
		}
		return this.prefixLists.get(name);
	}
	
	/**
	 * Determine where the device is external.
	 * @return true if the device is external, otherwise false
	 */
	public boolean isExternal() {
		return this.external;
	}
	
	@Override
	public String toString() {
		return this.getName();
	}
	
    @Override
    public boolean equals(Object other) {
    	if (this == other) {
    		return true;
    	}
    	if (!(other instanceof Device)) {
    		return false;
    	}
		Device otherDevice = (Device)other;
		return this.name.equals(otherDevice.name);
	}
}
