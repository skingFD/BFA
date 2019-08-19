package edu.wisc.cs.arc.graphs;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.batfish.representation.cisco.BgpProcess;
import org.batfish.representation.cisco.BgpRedistributionPolicy;
import org.batfish.representation.cisco.OspfProcess;
import org.batfish.representation.cisco.OspfRedistributionPolicy;
import org.batfish.representation.cisco.StaticRoute;

import edu.wisc.cs.arc.graphs.Vertex.VertexType;

import org.batfish.representation.Ip;
import org.batfish.representation.Prefix;
import org.batfish.representation.RoutingProtocol;

/**
 * A routing process running on a router.
 * @author Aaron Gember-Jacobson
 */
public class Process implements Comparable<Process>, Serializable{	
	private static final long serialVersionUID = -6455256694157735816L;
	
	/** Default administrative distances for various types of protocols */
	//https://en.wikipedia.org/wiki/Administrative_distance
	private static final int AD_STATICROUTE = 1;
	private static final int AD_EBGP = 20;
	private static final int AD_OSPF = 110;
	
	/** Name of the device on which this process is running */
	private Device device;
	
	/** BGP configuration snippet, if this is a BGP process */
	private BgpProcess bgpConfig;
	
	/** OSPF configuration snippet, if this is an OSPF process */
	private OspfProcess ospfConfig;
	
	/** Static route configuration snippet, if this is a static route process */
	private StaticRoute staticConfig;
	
	/** Interfaces over which routing messages are sent/received */
	private Collection<Interface> interfaces;
	
	/** ETG vertex used to enter the device via this process */
	private ProcessVertex inVertex;
	
	/** ETG vertex used to exit the device via this process */
	private ProcessVertex outVertex;
	
	/** Administrative distance for this process */
	private int administrativeDistance;
	
	/** Processes that are adjacent */
	private Set<Process> adjacentProcesses;
	
	public enum ProcessType {
		BGP,
		OSPF,
		STATIC
	}
	
	/** Type of process */
	private ProcessType type;
	
	/**
	 * Creates a routing process.
	 * @param device the device on which the process is running
	 */
	private Process(Device device) {
		this.device = device;
		this.adjacentProcesses = new HashSet<Process>();
	}
	
	/**
	 * Creates a BGP routing process.
	 * @param device the device on which the process is running
	 * @param interfaces interfaces on which BGP messages are sent/received
	 * @param bgpConfig configuration snippet for the process
	 */
	public Process(Device device, BgpProcess bgpConfig,
			Collection<Interface> interfaces) {
		this(device);
		this.type = ProcessType.BGP;
		this.bgpConfig = bgpConfig;
		this.interfaces = interfaces;
		this.administrativeDistance = AD_EBGP;
		this.createVertices();
	}
	
	/**
	 * Creates an OSPF routing process.
	 * @param device the device on which the process is running
	 * @param interfaces interfaces on which OSPF messages are sent/received
	 * @param ospfConfig configuration snippet for the process
	 */
	public Process(Device device, OspfProcess ospfConfig, 
			Collection<Interface> interfaces) {
		this(device);
		this.type = ProcessType.OSPF;
		this.ospfConfig = ospfConfig;
		this.interfaces = interfaces;
		this.administrativeDistance = AD_OSPF;
		this.createVertices();
	}
	
	/**
	 * Creates a static routing process.
	 * @param device the device on which the static route is setup
	 * @param interfaces interfaces out which traffic is routed
	 * @param staticConfig configuration snippet for the static route
	 */
	public Process(Device device, StaticRoute staticConfig,
			Collection<Interface> interfaces) {
		this(device);
		this.type = ProcessType.STATIC;
		this.staticConfig = staticConfig;
		this.interfaces = interfaces;
		this.administrativeDistance = AD_STATICROUTE;
		this.createVertices();
	}
	
	private void createVertices() {
		this.inVertex = new ProcessVertex(this, VertexType.IN);
		this.outVertex = new ProcessVertex(this, VertexType.OUT);
	}
	
	/**
	 * Get the device on which this process is running.
	 * @return the device on which this process is running
	 */
	public Device getDevice() {
		return this.device;
	}
	
	/**
	 * Determine if this process is a BGP routing process. 
	 * @return true if this process is a BGP process, otherwise false
	 */
	public boolean isBgpProcess() {
		return (this.bgpConfig != null);
	}
	
	/**
	 * Determine if this process is an OSPF routing process. 
	 * @return true if this process is an OSPF process, otherwise false
	 */
	public boolean isOspfProcess() {
		return (this.ospfConfig != null);
	}
	
	/**
	 * Determine if this process is a static routing process. 
	 * @return true if this process is a static process, otherwise false
	 */
	public boolean isStaticProcess() {
		return (this.staticConfig != null);
	}
	
	/**
	 * Get the routing protocol used by the routing process.
	 * @return routing protocol used by the routing process
	 */
	public RoutingProtocol getProtocol() {
		if (this.isBgpProcess()) {
			return RoutingProtocol.BGP;
		}
		else if (this.isOspfProcess()) {
			return RoutingProtocol.OSPF;
		}
		else if (this.isStaticProcess()) { 
			return RoutingProtocol.STATIC;
		}
		else {
			return null;
		}
	}
	
	/**
	 * Get the BGP configuration snippet for this process. 
	 * @return BGP configuration snippet, or null if not a BGP process
	 */
	public BgpProcess getBgpConfig() {
		return this.bgpConfig;
	}
	
	/**
	 * Get the OSPF configuration snippet for this process. 
	 * @return OSPF configuration snippet, or null if not an OSPF process
	 */
	public OspfProcess getOspfConfig() {
		return this.ospfConfig;
	}
	
	/**
	 * Get the static route configuration snippet for this process. 
	 * @return Static route configuration snippet, or null if not a static 
	 * 		   process
	 */
	public StaticRoute getStaticRouteConfig() {
		return this.staticConfig;
	}
	
	/**
	 * Get the interfaces over which routing messages are sent/received.
	 * @return interfaces over which routing messages are sent/received
	 */
	public Collection<Interface> getInterfaces() {
		return this.interfaces;
	}
	
	/**
	 * Get the interface over which routing messages are sent/received to a
	 * particular peer.
	 * @return interface over which routing messages are sent/received; null if
	 * 		no matching interface
	 */
	public Interface getInterfaceToReach(Ip peerIp) {
		for (Interface iface : this.interfaces) {
			if (iface.getPrefix().contains(peerIp)) {
				return iface;
			}
		}
		return null;
	}
	
	/**
	 * Add an adjacent routing process.
	 * @param adjacent the adjacent routing process
	 */
	public void addAdjacentProcess(Process adjacentProcess) {
		this.adjacentProcesses.add(adjacentProcess);
	}
	
	public Iterator<Process> getAdjacentProcessesIterator() {
		return this.adjacentProcesses.iterator();
	}
	
	/**
	 * Get the name of the routing process.
	 * @return name of the routing process
	 */
	public String getName() {
		String name = this.device.getName();
		if (this.bgpConfig != null) {
			name += ".bgp." + this.bgpConfig.getPid();
		}
		else if (this.ospfConfig != null) {
			name += ".ospf." + this.ospfConfig.getPid();
		}
		else if (this.staticConfig != null) {
			name += ".static." + 
					this.staticConfig.getPrefix().getNetworkAddress().asLong();
		}
		else {
			name += ".proc";
		}
		return name;
	}
	
	/**
	 * Set the entry and exit vertices for this routing process.
	 * @param inVertex ETG vertex used to enter the device via this process 
	 * @param outVertex ETG vertex used to exit the device via this process 
	 */
	public void setVertices(ProcessVertex inVertex, ProcessVertex outVertex) {
		this.inVertex = inVertex;
		this.outVertex = outVertex;
	}
	
	/**
	 * Get the ETG vertex used to enter the device via this process.
	 * @return ETG vertex used to enter the device via this process.
	 */
	public ProcessVertex getInVertex() {
		return this.inVertex;
	}
	
	/**
	 * Get the ETG vertex used to exit the device via this process.
	 * @return ETG vertex used to exit the device via this process.
	 */
	public ProcessVertex getOutVertex() {
		return this.outVertex;
	}
	
	/**
	 * Get the type of process.
	 * @return type of process
	 */
	public ProcessType getType() {
		return this.type;
	}
	
	/**
	 * Get the name of this routing process.
	 * @return name of this routing process
	 */
	@Override
	public String toString() {
		return this.getName();
	}

	/**
	 * Compare two routing process based on administrative distance (AD).
	 * @param other routing process to compare to
	 * @return difference in AD between the two processes; negative if this
	 * 			process has a lower AD (i.e., is preferred); positive if this 
	 * 			process has a higher AD (i.e., is not preferred)
	 */
	@Override
	public int compareTo(Process other) {
		if (this == other) {
			return 0;
		}
		if (this.administrativeDistance != other.administrativeDistance) {
			return (this.administrativeDistance - other.administrativeDistance);
		}
		if (this.isOspfProcess() && other.isOspfProcess()) {
			return (this.ospfConfig.getPid() - other.ospfConfig.getPid());
		}
		if (this.isBgpProcess() && other.isBgpProcess()) {
			return (this.bgpConfig.getPid() - other.bgpConfig.getPid());
		}
		//FIXME: What is the default?
		return 0;
	}
	
	public boolean advertises(PolicyGroup group, ProcessGraph rpg) {
		if (this.isBgpProcess()) {
			return this.advertisesBgp(group, rpg);
		}
		else if (this.isOspfProcess()) {
			return this.advertisesOspf(group, rpg);
		}
		return false;
	}
	
	private boolean advertisesBgp(PolicyGroup group, ProcessGraph rpg) {
		for (Prefix prefix : bgpConfig.getNetworks()) {
			if (group.within(prefix)) {
				return true;
			}
		}
		
		// Check if the BGP process redistributes connected prefixes
		BgpRedistributionPolicy policy = 
				bgpConfig.getRedistributionPolicies().get(
						RoutingProtocol.CONNECTED);
		if (policy != null) {
			// Determine if the destination is part of the device's interfaces
			boolean groupConnected = false;
			for (Interface iface : device.getInterfaces()) {
				if (iface.hasPrefix() && group.within(iface.getPrefix())) {
					groupConnected = true;
				}
			}
			
			if (null == policy.getMap()) {
				return groupConnected;
			}
			else {
				/*RouteMap routeMap = this.routeMapsByDevice.get(
						localProcess.getDevice()).get(policy.getMap());
				for (Interface iface : 
					this.interfacesByDevice.get(localProcess.getDevice())) {
					if (flow.getDestination().within(iface.getPrefix())
							&& (null == routeMap || !flow.isBlocked(routeMap, 
							this.configsByDevice.get(localProcess.getDevice())))) {
						return true;
						break;
					}
				}*/
				// FIXME: Actually check route maps
				return true;
			}
		}
		
		// Check if the BGP process redistributes OSPF
		policy = bgpConfig.getRedistributionPolicies().get(
						RoutingProtocol.OSPF);
		if (policy != null) {
			int ospfProccessNumber = (int)policy.getSpecialAttributes().get(
					BgpRedistributionPolicy.OSPF_PROCESS_NUMBER);
			for (Process process : this.device.getRoutingProcesses()) {
				if (process.isOspfProcess() 
						&& process.getOspfConfig().getPid() 
							== ospfProccessNumber) {
					return process.advertisesOspf(group, rpg);
				}
			}
		}
		
		// FIXME: Are there other valid ways of deciding what is advertised?
		return false;
	}
	
	private boolean advertisesOspf(PolicyGroup group, ProcessGraph rpg) {
		for (Interface iface : this.interfaces) {
			if (group.within(iface.getPrefix())) {
				return true;
			}
		}
		
		// Check if the OSPF process redistributes connected prefixes
		OspfRedistributionPolicy policy = 
				ospfConfig.getRedistributionPolicies().get(
						RoutingProtocol.CONNECTED);
		if (policy != null) {
			// Determine if the destination is part of the device's interfaces
			boolean groupConnected = false;
			for (Interface iface : device.getInterfaces()) {
				if (group.within(iface.getPrefix())) {
					groupConnected = true;
				}
			}
			
			if (null == policy.getMap()) {
				return groupConnected;
			}
			else {
				/*RouteMap routeMap = this.routeMapsByDevice.get(
						localProcess.getDevice()).get(policy.getMap());
				for (Interface iface : 
					this.interfacesByDevice.get(localProcess.getDevice())) {
					if (flow.getDestination().within(iface.getPrefix())
							&& (null == routeMap || !flow.isBlocked(routeMap, 
							this.configsByDevice.get(localProcess.getDevice())))) {
						return true;
						break;
					}
				}*/
				// FIXME: Actually check route maps
				return true;
			}
		}
		
		// FIXME: Are there other valid ways of deciding what is advertised?
		return false;
	}
}
