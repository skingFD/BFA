package edu.wisc.cs.arc.graphs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.batfish.representation.Prefix;

import edu.wisc.cs.arc.Settings;
import edu.wisc.cs.arc.graphs.DirectedEdge.EdgeType;
import edu.wisc.cs.arc.graphs.Interface.InterfaceType;
import edu.wisc.cs.arc.graphs.Vertex.VertexType;
import edu.wisc.cs.arc.virl.Link;

/**
 * An extended topology graph where vertices are devices and edges represent
 * physical connections.
 * @author Aaron Gember-Jacobson (agember@cs.wisc.edu)
 */
public class DeviceGraph extends ExtendedTopologyGraph<DeviceVertex> {
	private static final long serialVersionUID = -3018750171585312043L;
	
	/** A list of internal devices by name */
	private Map<String,Device> devices;

	/**
	 * Create an empty device-based extended topology graph.
	 */
	public DeviceGraph(Settings settings) {
		super(settings);
		this.devices = new HashMap<String,Device>();
	}
	
	/**
	 * Create a copy of the device-based extended topology graph.
	 * @return a new device-based extended topology graph
	 */	
	@Override
	public Object clone() {
		DeviceGraph dgClone = (DeviceGraph)super.clone();
		dgClone.devices = this.devices;
		return dgClone;
	}
	
	/**
	 * Customize the graph for a specific flow.
	 * @param flow the flow for which the graph should be customized
	 * @return true if the graph was successfully customized, otherwise false
	 */
	@Override
	public boolean customize(Flow flow) {
		Map<PolicyGroup, DeviceVertex> sourceVertices = new HashMap<>();
		sourceVertices.put(flow.getSource(), new DeviceVertex(null, VertexType.SOURCE));
		return this.customize(flow, sourceVertices,
				new DeviceVertex(null, VertexType.DESTINATION));
	}

	/**
	 * Customize the graph for a destination and list of sources
	 * @param sources the list of source policy groups
	 * @param flow the flow for which the graph should be customized
	 */
	@Override
	public boolean customize(Flow flow, List<PolicyGroup> sources) {
		Map<PolicyGroup, DeviceVertex> sourceVertices = new HashMap<>();
		for (PolicyGroup source: sources) {
			DeviceVertex sourceVertex = new DeviceVertex(null, VertexType.SOURCE);
			sourceVertices.put(source, sourceVertex);
		}
		return this.customize(flow, sourceVertices,
				new DeviceVertex(null, VertexType.DESTINATION));
	}

	/**
	 * Get the devices contained in the graph.
	 * @return the devices contained in the graph, indexed by device name
	 */
	public Map<String,Device> getDevices() {
		return this.devices;
	}
	
	/**
	 * Create a device-based extended topology graph based on interface
	 * descriptions.
	 * @param devices devices in the network
	 * @param settings
	 */
	public DeviceGraph(List<Device> devices, Settings settings) {
		this(settings);
		
		// Add each device
		for (Device device : devices) {
			this.addDevice(device);
		}
		
		if (settings.shouldUseInterfaceDescriptions()) {
			this.constructEdgesByDescription();
		}
		else {
			this.constructEdgesBySubnet();
		}
	}
	
	/**
	 * Add edges between devices based on subnets.
	 */
	private void constructEdgesBySubnet() {
		Map<Prefix, List<Interface>> interfacesByPrefix =
				new LinkedHashMap<Prefix, List<Interface>>();
		for (Device device : this.devices.values()) {
			for (Interface iface : device.getInterfaces()) {
				if (!iface.hasPrefix()) {
					continue;
				}
				
				// Determine prefix for interface
				Prefix prefix = new Prefix(
						iface.getPrefix().getNetworkAddress(),
						iface.getPrefix().getPrefixLength());
				
				// Get interfaces by prefix
				List<Interface> interfacesForPrefix = 
						interfacesByPrefix.get(prefix);
				if (null == interfacesForPrefix) {
					interfacesForPrefix = new ArrayList<Interface>();
					interfacesByPrefix.put(prefix, interfacesForPrefix);
				}
				
				// Add interface to list
				interfacesForPrefix.add(iface);
			}
		}
		
		// Connect interfaces participating in the same prefix
		for (Prefix prefix : interfacesByPrefix.keySet()) {
			List<Interface> interfaces = interfacesByPrefix.get(prefix);
			if (interfaces.size() > 2) {
				logger.warn("More than two interfaces in the subnet " + prefix);
			}
			for (int s = 0; s < interfaces.size(); s++) {
				for (int d = s+1; d < interfaces.size(); d++) {
					Interface srcIface = interfaces.get(s);
					Interface dstIface = interfaces.get(d);
					this.addLink(srcIface.getDevice(), srcIface, 
							dstIface.getDevice(), dstIface);
					logger.debug("Add link from " + srcIface.getDevice() + ":" 
							+ srcIface + " to " + dstIface.getDevice() + ":" 
							+ dstIface);
				}
			}
		}
	}
	
	/**
	 * Add edges between devices based on interface descriptions.
	 */
	private void constructEdgesByDescription() {
		for (Device device : this.devices.values()) {
			//this.logger.debug(device.getName());
			for (Interface iface : device.getInterfaces()) {
				if (!iface.getActive() || null == iface.getDescription()
						|| !iface.getDescription().startsWith("INFRA")
						|| iface.getType() != InterfaceType.ETHERNET) {
					continue;
				}
				
				String parts[] = iface.getDescription().split(":");
				if (parts.length < 3) {
					logger.warn("Unknown interface description format for "
							+ iface.getName() + " on " + device.getName() 
							+ ": " + iface.getDescription());
					continue;
				}

                int shift = 0;
                if (parts.length == 3) {
                   shift = -1; 
                }
				
				String neighborDeviceName = parts[2+shift].toLowerCase();
				String neighborIfaceName = parts[3+shift];
			
				boolean matched = false;
				Pattern pattern = null;
				Matcher matcher = null;
				pattern = Pattern.compile(
						"^((Te)|(TenGigabitEthernet))"
                        + "(?<num>\\d+/\\d+(/\\d+)?)");
				matcher = pattern.matcher(neighborIfaceName);
				if (matcher.find()) {
					neighborIfaceName = "TenGigabitEthernet" 
							+ matcher.group("num");
					matched = true;
				}
				pattern = Pattern.compile(
						"^((G)|(Gi)|(gig)|(xe-)|(GigabitEthernet))"
                        + "(?<num>\\d+/\\d+(/\\d+)?)");
				matcher = pattern.matcher(neighborIfaceName);
				if (matcher.find()) {
					neighborIfaceName = "GigabitEthernet" 
							+ matcher.group("num");
					matched = true;
				}
				pattern = Pattern.compile(
						"^((e)|(et)|(eth)|(E)|(Et)|(Eth)|(ethernet)|(Ethernet))"
                        + "(?<num>\\d+/\\d+(/\\d+)?)");
				matcher = pattern.matcher(neighborIfaceName);
				if (matcher.find()) {
					neighborIfaceName = "Ethernet" + matcher.group("num");
					matched = true;
				}
				pattern = Pattern.compile(
						"^((Po)|(po)|(Port-channel))(?<num>\\d+(/\\d+)?)");
				matcher = pattern.matcher(neighborIfaceName);
				if (matcher.find()) {
					neighborIfaceName = Interface.NAME_PREFIX_PORT_CHANNEL 
							+ matcher.group("num");
					matched = true;
				}
				pattern = Pattern.compile(
						"^((mg)|(mgmt))(?<num>\\d+(/\\d+)?)");
				matcher = pattern.matcher(neighborIfaceName);
				if (matcher.find()) {
					neighborIfaceName = "mgmt" + matcher.group("num");
					matched = true;
				}
				pattern = Pattern.compile(
						"^((vl)|(Vlan))(?<num>\\d+(/\\d+)?)");
				matcher = pattern.matcher(neighborIfaceName);
				if (matcher.find()) {
					neighborIfaceName = "Vlan" + matcher.group("num");
					matched = true;
				}
				
				if (!matched) {
					logger.warn("Unknown interface type in description for "
							+ iface.getName() + " on " + device.getName() 
							+ ": " + neighborIfaceName);
					continue;
				}
				
				Device neighborDevice = this.devices.get(neighborDeviceName);
				if (neighborDevice != null) {
					Interface neighborIface = 
							neighborDevice.getInterface(neighborIfaceName);
					if (neighborIface != null && neighborDevice != device) {
						this.addLink(device, iface, neighborDevice, 
							neighborIface);
						logger.debug("Add link from " + device + ":" + iface +
								" to " + neighborDevice + ":" + neighborIface);
					}
				}
			}
		}
	}
	
	/**
	 * Add a device to the topology graph.
	 * @param device device to add
	 */
	public void addDevice(Device device) {
		this.devices.put(device.getName(), device);
		this.addVertex(new DeviceVertex(device, VertexType.NORMAL));
	}
	
	/** 
	 * Add a link to the topology graph.
	 * @param srcDevice device on one end of the link
	 * @param srcIface device interface on one end of the link
	 * @param dstDevice device on the other end of the link
	 * @param dstIface device interface no the other end of the link
	 */
	public void addLink(Device srcDevice, Interface srcIface, Device dstDevice,
			Interface dstIface) {
		// If interfaces are port channels, then connect the physical interfaces
		if ((srcIface.getType() == InterfaceType.PORT_CHANNEL)
				&& (dstIface.getType() == InterfaceType.PORT_CHANNEL)) {
			logger.debug("Connect Port-channels: " + srcDevice + "." + srcIface
					+ " -> " + dstDevice + "." + dstIface);
			if (srcIface.getSubInterfaces().size() == 0) {
				logger.warn("No subinterfaces: for " + srcDevice + "." 
						+ srcIface);
				return;
			}
			if (dstIface.getSubInterfaces().size() == 0) {
				logger.warn("No subinterfaces: for " + dstDevice + "." 
						+ dstIface);
				return;
			}
			for (int i = 0; i < srcIface.getSubInterfaces().size(); i++) {
				this.addLink(srcDevice, srcIface.getSubInterfaces().get(i), 
						dstDevice, dstIface.getSubInterfaces().get(i));
			}
			return;
		}
		
		DeviceVertex srcVertex = this.getVertex(srcDevice.getName());
		DeviceVertex dstVertex = this.getVertex(dstDevice.getName());
		
		if (!(this.containsEdge(srcVertex, dstVertex)
				|| this.containsEdge(dstVertex, srcVertex))) {
			this.addEdge(srcVertex, dstVertex, 0, EdgeType.INTER_DEVICE, 
					srcIface, dstIface);
		}
	}
	
	/** 
	 * Remove a link from the topology graph.
	 * @param link the link to remove
	 */
	public void removeLink(Link link) {
		DeviceVertex srcVertex = this.getVertex(link.getSourceDeviceName());
		DeviceVertex dstVertex = 
				this.getVertex(link.getDestinationDeviceName());
		
		if (this.containsEdge(srcVertex, dstVertex)) {
			this.removeEdge(srcVertex, dstVertex);
		}
		if (this.containsEdge(dstVertex, srcVertex)) {
			this.removeEdge(dstVertex, srcVertex);
		}
	}
	
	/**
	 * Get the device that lies at the other end of a link.
	 * @param srcInterface the interface at the same end of the link
	 * @return the device at the other end of the link, null if no such link
	 */
	public Device getConnectedDevice(Interface srcInterface) {
		DeviceVertex srcVertex = this.getVertex(
				srcInterface.getDevice().getName());

		for (DirectedEdge<DeviceVertex> edge : 
				this.getOutgoingEdges(srcVertex)) {
			if (edge.getSourceInterface() == srcInterface) {
				return this.devices.get(edge.getDestination().getName());
			}
		}	
		for (DirectedEdge<DeviceVertex> edge : 
				this.getIncomingEdges(srcVertex)) {
			if (edge.getDestinationInterface() == srcInterface) {
				return this.devices.get(edge.getSource().getName());
			}
		}
		
		return null;
	}
	
	/**
	 * Get the interface that lies at the other end of a link.
	 * @param srcInterface the interface at the same end of the link
	 * @return the interface at the other end of the link, null if no such link
	 */
	public Interface getConnectedInterface(Interface srcInterface) {
		DeviceVertex srcVertex = this.getVertex(
				srcInterface.getDevice().getName());

		for (DirectedEdge<DeviceVertex> edge : 
				this.getOutgoingEdges(srcVertex)) {
			if (edge.getSourceInterface() == srcInterface) {
				return edge.getDestinationInterface();
			}
		}	
		for (DirectedEdge<DeviceVertex> edge : 
				this.getIncomingEdges(srcVertex)) {
			if (edge.getDestinationInterface() == srcInterface) {
				return edge.getSourceInterface();
			}
		}
		
		return null;
	}
	
	/**
	 * Add edges for the source and destination of the flow.
	 */
	protected void constructEndpointEdges() {
		Flow flow = this.getFlow();
		
		// If source and/or destination is external, then connect to all
		// external devices
		if (!flow.getSource().isInternal() 
				|| !flow.getDestination().isInternal()) {
			for (Device device : this.devices.values()) {
				if (!device.isExternal()) {
					continue;
				}
				
				if (!flow.getSource().isInternal()) {
					this.addEdge(this.getFlowSourceVertex(flow.getSource()),
							this.getVertex(device.getName()), 0, 
							EdgeType.INTER_DEVICE);
				}
				if (!flow.getDestination().isInternal()) {
					this.addEdge(this.getVertex(device.getName()),
							this.getFlowDestinationVertex(), 0,
							EdgeType.INTER_DEVICE);
				}
			}
		}
		
		// Iterate over all devices
		for (Device device : this.devices.values()) {
			for (Interface iface : device.getInterfaces()) {
				if (!iface.getActive() || null == iface.getPrefix()) {
					continue;
				}
				
				DeviceVertex sourceVertex = null;
				DeviceVertex destinationVertex = null;
				
				// We only care about interfaces whose prefix falls within the 
				// source or destination policy groups
				Interface sourceIface = null;
				Interface destinationIface = null;
				if (flow.getSource().contains(iface.getPrefix().getAddress())) {
					sourceVertex = this.getFlowSourceVertex(flow.getSource());
					destinationIface = iface;
				}
				else if (flow.getDestination().contains(
						iface.getPrefix().getAddress())) {
					destinationVertex = this.getFlowDestinationVertex();
					sourceIface = iface;
				}
				else {
					continue;
				}
									
				// Set device vertex as source or destination vertex,
				// depending on whether the device's interface matched
				// the source or destination policy group
				if (null == sourceIface) {
					destinationVertex = this.getVertex(device.getName());
				}
				else {											
					sourceVertex = this.getVertex(device.getName());
				}
					
				// Add an edge
				if (!this.containsEdge(sourceVertex, destinationVertex)) {
					DirectedEdge<DeviceVertex> edge = this.addEdge(sourceVertex,
							destinationVertex, 0, EdgeType.INTER_DEVICE,
							sourceIface, destinationIface);
					if (null == sourceIface) {
						edge.checkAndBlockIncoming(flow, device);
					}
					else {
						edge.checkAndBlockOutgoing(flow, device);
					}
				}
			}
		}
	}
	
	protected void customizeEdges() {
		for (Device device : this.devices.values()) {
			this.customizeEdges(device);
		}
	}
	
	private void customizeEdges(Device device) {
		for (DirectedEdge<DeviceVertex> edge : 
				this.getOutgoingEdges(this.getVertex(device.getName()))) {
			if (null == edge.getSourceInterface()) {
				continue;
			}
			edge.checkAndBlockOutgoing(this.getFlow(), device);
		}
		
		for (DirectedEdge<DeviceVertex> edge : 
				this.getIncomingEdges(this.getVertex(device.getName()))) {
			if (null == edge.getDestinationInterface()) {
				continue;
			}
			edge.checkAndBlockIncoming(this.getFlow(), device);
		}
	
		// FIXME: Account for route-maps
	}

	/**
	 * Get a graphviz representation of the graph.
	 * @return graphviz code for the graph
	 */
	@Override
	public String toGraphviz() {
		String gvCode = "graph {\n";
		
		List<Prefix> prefixes = new ArrayList<Prefix>();
		
		// Add edges
		Iterator<DirectedEdge<DeviceVertex>> edgesIterator = 
				this.getEdgesIterator();
		//this.logger.debug("Edges:");
		while (edgesIterator.hasNext()) {
			DirectedEdge<DeviceVertex> edge = edgesIterator.next();
			//this.logger.debug("\t"+edge);
			if (null == edge.getDestinationInterface()) {
				Prefix prefix = new Prefix(
						edge.getSourceInterface().getPrefix().getNetworkAddress(), 
						edge.getSourceInterface().getPrefix().getPrefixLength());
				prefixes.add(prefix);

				String subnetString = prefix.toString();
				if (this.settings.shouldAnonymize()) {
						PolicyGroup policyGroup = new PolicyGroup(prefix);
						policyGroup.makeAnonymous();
						//subnetString += "(" + policyGroup.toString() + ")";
						subnetString = policyGroup.toString();
				}
	
				gvCode += "\t\"" + edge.getSource().getName() + "\" -- \""
						+ subnetString + "\""
						+ (settings.shouldAnonymize() ? "" : ("[label=\""
						+ edge.getSourceInterface().getName() + "\n"
						+ edge.getSourceInterface().getPrefix() + "\"]")) 
						+ "\n";
			}
			else if (edge.getSourceInterface().getPrefix() != null)
			{
				Prefix prefix = new Prefix(
						edge.getSourceInterface().getPrefix().getNetworkAddress(), 
						edge.getSourceInterface().getPrefix().getPrefixLength());
				prefixes.add(prefix);

				String subnetString = prefix.toString();
				if (this.settings.shouldAnonymize()) {
						PolicyGroup policyGroup = new PolicyGroup(prefix);
						policyGroup.makeAnonymous();
						//subnetString += "(" + policyGroup.toString() + ")";
						subnetString = policyGroup.toString();
				}
	
				gvCode += "\t\"" + edge.getSource().getName() + "\" -- \""
						+ subnetString +"\""
						+ (settings.shouldAnonymize() ? "" : ("[label=\"" 
						+ edge.getSourceInterface().getName() + "\n"
						+ edge.getSourceInterface().getPrefix() + "\"]"))
						+ "\n";
				
				gvCode += "\t\"" + subnetString + "\" -- \""
						+ edge.getDestination().getName() + "\""
						+ (settings.shouldAnonymize() ? "" : ("[label=\"" 
						+ edge.getDestinationInterface().getName() + "\n"
						+ edge.getDestinationInterface().getPrefix() + "\"]"))
						+ "\n";
			}
			else {
				gvCode += "\t\"" + edge.getSource().getName() + "\" -- \""
						+ edge.getDestination().getName() +"\""
						+ (settings.shouldAnonymize() ? "" : ("[label=\"" 
						+ edge.getSourceInterface().getName() + "\n"
						+ edge.getDestinationInterface().getName() + "\"]"))
						+ "\n";
			}
		}
		
		// Add vertices
		Iterator<DeviceVertex> verticesIterator = this.getVerticesIterator();
		while (verticesIterator.hasNext()) {
			DeviceVertex vertex = verticesIterator.next();
			if (vertex.getName().contains("hosts")) {
				continue;
			}
			gvCode += "\t\"" + vertex.getName()
				+ "\"[shape=box, style=filled, fillcolor=";
			if (vertex.getName().contains("external")) {
				gvCode += "yellow";
			}
			else {
				gvCode += "white";
			}
			gvCode += "]\n";
		}
		
		for (Prefix prefix : prefixes) {
			String subnetString = prefix.toString();
			if (this.settings.shouldAnonymize()) {
					PolicyGroup policyGroup = new PolicyGroup(prefix);
					policyGroup.makeAnonymous();
					//subnetString += "(" + policyGroup.toString() + ")";
					subnetString = policyGroup.toString();
			}

			gvCode += "\t\"" + subnetString
				+ "\"[shape=oval, style=filled, fillcolor=cyan]\n";
		}
		
		gvCode += "label=\"Physical Topology\"\n";
		gvCode += "}";
		return gvCode;
	}
}
