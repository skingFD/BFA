package edu.wisc.cs.arc.graphs;

import java.util.*;
import java.util.Map.Entry;

import org.batfish.representation.Ip;
import org.batfish.representation.cisco.BgpProcess;
import org.batfish.representation.cisco.BgpRedistributionPolicy;
import org.batfish.representation.cisco.OspfProcess;
import org.batfish.representation.cisco.OspfRedistributionPolicy;

import edu.wisc.cs.arc.GeneratorException;
import edu.wisc.cs.arc.Settings;
import edu.wisc.cs.arc.graphs.DirectedEdge.EdgeType;
import edu.wisc.cs.arc.graphs.Process.ProcessType;
import edu.wisc.cs.arc.graphs.Vertex.VertexType;
import edu.wisc.cs.arc.virl.Link;

/**
 * An extended topology graph where vertices are routing processes and edges
 * represent routing process adjacencies and route redistribution.
 * @author Aaron Gember-Jacobson (agember@cs.wisc.edu)
 */
public class ProcessGraph extends ExtendedTopologyGraph<ProcessVertex> {
	private static final long serialVersionUID = -808081253375932844L;

	/** A list of device-interface pairs based on interfaces' IP address */
	private Map<Ip, Map<Device, Interface>> interfacesByAddress;
	
	/** A list of internal devices */
	private List<Device> devices;
	
	/** The BGP processes that speak to BGP processes outside the network */
	private Set<Process> bgpWithOutsidePeers;
	
	/** The maximum cost assigned to any edge by an interior gateway protocol */
	private double maxIGPLinkCost;
	
	/** The autonomous system numbers internal to the network */
	private Set<Integer> internalASes;
	
	/** Device-based ETG constructed while this ETG is constructed */
	private DeviceGraph deviceEtg;
	
	/** Instance-based ETG constructed while this ETG is constructed */
	private InstanceGraph instanceEtg;

	/**
	 * Create a process-based extended topology graph based on device 
	 * configurations.
	 * @param "devices" devices in the network
	 * @param settings
	 */
	public ProcessGraph(DeviceGraph deviceEtg, Settings settings) {
		super(settings);
		
		this.interfacesByAddress = 
				new HashMap<Ip, Map<Device, Interface>>();
		this.deviceEtg = deviceEtg;
		this.devices = new ArrayList<Device>();
		this.bgpWithOutsidePeers = new LinkedHashSet<Process>();
		
		this.maxIGPLinkCost = 0;
		this.internalASes = new LinkedHashSet<Integer>();
		
		// Process each device
		for (Device device : deviceEtg.getDevices().values()) {
			this.addDevice(device);
		}
		
		// Add vertices
		this.constructVertices();
		
		// Add edges
		this.constructEdges();
	}
	
	/**
	 * Get the instance-based ETG constructed while this graph was constructed.
	 * @return instance-based ETG
	 */
	public InstanceGraph getInstanceEtg() {
		return this.instanceEtg;
	}
	
	/**
	 * Customize the graph for a specific flow.
	 * @param flow the flow for which the graph should be customized
	 * @return true if the graph was successfully customized, otherwise false
	 */
	@Override
	public boolean customize(Flow flow) {
		return this.customize(flow, 
				Arrays.asList(new PolicyGroup[]{flow.getSource()}));
	}

	/**
	 * Customize the graph for a destination and list of sources
	 * @param sources the list of source policy groups
	 * @param flow the flow for which the graph should be customized
	 */
	@Override
	public boolean customize(Flow flow, List<PolicyGroup> sources) {
		Map<PolicyGroup, ProcessVertex> sourceVertices = 
				new HashMap<PolicyGroup, ProcessVertex>(sources.size());

		// Create a source vertex for each source
		for (PolicyGroup source: sources) {
			ProcessVertex sourceVertex = new ProcessVertex(source, 
					VertexType.SOURCE);
			sourceVertices.put(source, sourceVertex);
		}
		
		return this.customize(flow, sourceVertices,
				new ProcessVertex(flow.getDestination(), 
						VertexType.DESTINATION));
	}
	/**
	 * Create a copy of the process-based extended topology graph.
	 * @return a new process-based extended topology graph
	 */	
	@Override
	public Object clone() {
		ProcessGraph pgClone = (ProcessGraph)super.clone();
		pgClone.interfacesByAddress = this.interfacesByAddress;
		pgClone.devices = this.devices;
		pgClone.bgpWithOutsidePeers = this.bgpWithOutsidePeers;
		pgClone.maxIGPLinkCost = this.maxIGPLinkCost;
		pgClone.internalASes = this.internalASes;
		return pgClone;
	}
	
	/**
	 * Add a device to the graph.
	 * @param device the device to add
	 */
	private void addDevice(Device device) {
		// Store device
		this.devices.add(device);

		// Store interfaces by address
		for (Interface iface : device.getInterfaces()) {
			if (null == iface.getPrefix()) {
				continue;
			}
			
			Ip address = iface.getPrefix().getAddress();
			if (!this.interfacesByAddress.containsKey(address)) {
				this.interfacesByAddress.put(address, 
						new HashMap<Device,Interface>());
			}
			
			// Add interface by address
			Map<Device,Interface> interfacesWithAddress = 
					this.interfacesByAddress.get(address);
			if (interfacesWithAddress.containsKey(device.getName())) {
				throw new GeneratorException(device.getName() 
						+ " has more than one interface with the address " 
						+ address.toString());
			}
			interfacesWithAddress.put(device, iface);
			
			/*if (iface.getName().startsWith("Vlan")) {
				//this.logger.debug(iface.getName());
				Device hostDevice = new Device("hosts"
						+ prefix.getNetworkAddress().toString());
				if (this.deviceEtg != null) {
					this.deviceEtg.addDevice(hostDevice);
					this.deviceEtg.addLink(device, iface, hostDevice, null);
				}
			}*/
		}
		
		// Store process information
		for (Process process : device.getRoutingProcesses()) {
			switch(process.getType()) {
			case BGP:
				this.internalASes.add(process.getBgpConfig().getPid());
				break;
			case OSPF:
				for (Interface iface : process.getInterfaces()) {
					if (iface.getOspfCost() != null 
							&& iface.getOspfCost() > this.maxIGPLinkCost) {
						this.maxIGPLinkCost = iface.getOspfCost();
					}
				}
				break;
			case STATIC:
				break;
			}
		}
	}
	
	/**
	 * Add vertices for routing processes.
	 */
	private void constructVertices() {
		for (Device device : this.devices) {
			for (Process routingProcess : device.getRoutingProcesses()) {
				this.addVertex(routingProcess.getInVertex());
				this.addVertex(routingProcess.getOutVertex());
			}
		}
	}
	
	/**
	 * Add edges between routing processes.
	 */
	private void constructEdges() {
		// Set up edges for adjacencies to other routing processes
		for (Device device : this.devices) {			
			for (Process routingProcess : device.getRoutingProcesses()) {
				if (routingProcess.isBgpProcess()) {
					this.constructBgpPeeringEdges(routingProcess);
				}
				else if (routingProcess.isOspfProcess()) {
					this.constructOspfNeighborEdges(routingProcess);
				}
				else if (routingProcess.isStaticProcess()) {
					this.constructStaticRouteEdges(routingProcess);
				}
			}
		}
				
		// Set up edges for route redistribution
		for (Device device : this.devices) {
			this.constructRouteRedistributionEdges(
					device.getRoutingProcesses());
		}
		
		// Generate instance graph
		this.instanceEtg = new InstanceGraph(this);
		
		// Adjust weights on edges for route redistribution
		for (Device device : this.devices) {
			this.adjustRouteRedistributionEdgeWeights(
					device.getRoutingProcesses());
		}
		
		// Scale edge weights if instance graph is a DAG
		if (!instanceEtg.hasCycles()) {
			this.scaleEdgeWeights();
		}
	}
	
	/**
	 * Add edges for BGP peering sessions.
	 * @param localProcess local BGP process
	 */
	private void constructBgpPeeringEdges(Process localProcess) {
		// Add an edge for each peer
		for (Ip remotePeer : 
				localProcess.getBgpConfig().getIpPeerGroups().keySet()) {
			// Determine local interface
			Interface localInterface = localProcess.getInterfaceToReach(
					remotePeer);

			// Make sure there is a local interface
			if (localInterface == null) {
				this.logger.warn(localProcess.getDevice() +
						" does not have an interface to reach BGP peer "
						+ remotePeer);
				continue;
			}
			
			// Get the remote interface that is physically connected
			Interface remoteInterface = this.deviceEtg.getConnectedInterface(
					localInterface);
			
			// If no interface is physically connected, then assume the peer is
			// outside the network
			if (null == remoteInterface) {
				this.bgpWithOutsidePeers.add(localProcess);
				continue;
			}

			// Determine remote BGP process
			Process remoteProcess = null;

			// Check every remote process
			for (Process tmpRemoteProcess : 
					remoteInterface.getDevice().getRoutingProcesses()) {
				// Only consider BGP processes
				if (!tmpRemoteProcess.isBgpProcess()) {
					continue;
				}
				
				if (tmpRemoteProcess.getBgpConfig().getIpPeerGroups().containsKey(
						localInterface.getPrefix().getAddress())) {
					remoteProcess = tmpRemoteProcess;
				}
			}

			// Make sure there is a remote BGP process
			if (remoteProcess == null) {
				this.assumptionViolated(remoteInterface.getDevice()
						+" does not have a BGP process associated with process "
						+ localProcess.getName());
				continue;
			}

			// Create an edge
			// FIXME: Account for local preference
			// Edge cost is 1 because we want to count AS hops
			this.addEdge(localProcess.getOutVertex(), 
					remoteProcess.getInVertex(), 1, //this.getMaxASPathWeight(), 
					EdgeType.INTER_DEVICE, localInterface, remoteInterface);
			localProcess.addAdjacentProcess(remoteProcess);
		}
	}
	
	/**
	 * Add edges for OSPF neighbor sessions.
	 * @param localProcess local OSPF process
	 */
	private void constructOspfNeighborEdges(Process localProcess) {
		// Construct neighbor edges for every interface over which the process
		// sends/receives OSPF messages
		for (Interface localInterface : localProcess.getInterfaces()) {
			logger.debug(localInterface + " participates in " + localProcess);
			double weight = 0;
			if (localInterface.getOspfCost() != null) {
				weight = localInterface.getOspfCost();
			}
			
			// Get the remote interface that is physically connected
			Interface remoteInterface = 
					this.deviceEtg.getConnectedInterface(localInterface);
			if (null == remoteInterface) {
				logger.warn("No interface connected to " 
						+ localInterface.getDevice() + ":" + localInterface
						+ " which is used by process " + localProcess);
				continue;
			}
					
			// Check every remote process
			for (Process remoteProcess : 
				remoteInterface.getDevice().getRoutingProcesses()) {
				// Only consider OSPF processes
				if (!remoteProcess.isOspfProcess()) {
					continue;
				}
				
				// Only consider remote processes in which the remote
				// interface participates
				if (!remoteProcess.getInterfaces().contains(remoteInterface)) {
					continue;
				}

				// Create an edge
				this.addEdge(localProcess.getOutVertex(),
						remoteProcess.getInVertex(), weight, 
						EdgeType.INTER_DEVICE, localInterface, remoteInterface);
				localProcess.addAdjacentProcess(remoteProcess);
				logger.debug(remoteProcess + " is adjacent to " + localProcess);
			}
		}
	}

	/**
	 * Add edges for static routes.
	 * @param localProcess local static routing process
	 */
	private void constructStaticRouteEdges(Process localProcess) {
		// Construct neighbor edges for every interface over which the static 
		// route sends traffic
		for (Interface localInterface : localProcess.getInterfaces()) {	
			// Get the remote interface that is physically connected
			Interface remoteInterface = 
					this.deviceEtg.getConnectedInterface(localInterface);
			if (null == remoteInterface) {
				logger.warn("No interface connected to " 
						+ localInterface.getDevice() + ":" + localInterface
						+ " which is used by process " + localProcess);
				continue;
			}
			
			// Add connection to every remote process
			for (Process remoteProcess : 
					remoteInterface.getDevice().getRoutingProcesses()) {	
				// Create an edge
				this.addEdge(localProcess.getOutVertex(),
						remoteProcess.getInVertex(), 0, EdgeType.INTER_DEVICE);
				localProcess.addAdjacentProcess(remoteProcess);
			}
		}
	}
	
	/**
	 * Add edges for route redistribution between processes on the same device.
	 * @param processes the routing processes running on a device
	 */
	private void constructRouteRedistributionEdges(
			List<Process> processes) {
		// Sort routing processes based on administrative distance
		Collections.sort(processes);
		
		// Add redistribution edges within a process and between processes
		// that redistribute routes
		for (Process redistributor : processes) {
			for (Process redistributee : processes) {
				double weight = 0;
				
				// Edge to stay within a process is based on process's ordering
				// relative to other processes; we do not want to stay within
				// a process if another process has a lower administrative
				// distance (AD) because it will win in the global RIB if it
				// has a route to the destination
				if (redistributee == redistributor) {
					// Edge direction is the inverse of the direction of
					// redistribution
					this.addEdge(redistributee.getInVertex(), 
							redistributor.getOutVertex(), weight, 
							EdgeType.INTRA_DEVICE);
					continue;
				}
				
				// Consider OSPF redistribution policy
				switch (redistributee.getType()) {
				case OSPF:
				{
					OspfProcess ospfProcess = redistributee.getOspfConfig();
					OspfRedistributionPolicy policy = 
							ospfProcess.getRedistributionPolicies().get(
								redistributor.getProtocol());
					// FIXME: Make sure PID matches
					if (policy == null) {
						// Don't add an edge if there is no redistribution
						continue;
					}
					else if (policy.getMetric() != null) {
						// FIXME: Consider metric type
						weight = policy.getMetric();
					}
					break;
				}
				// Consider BGP redistribution policy
				case BGP:
				{
					BgpProcess bgpProcess = redistributee.getBgpConfig();
					BgpRedistributionPolicy policy =
							bgpProcess.getRedistributionPolicies().get(
									redistributor.getProtocol());
					// FIXME: Make sure PID matches
					if (policy == null) {
						// Don't add an edge if there is no redistribution
						continue;
					}
					else if (policy.getMetric() != null) {
						weight = policy.getMetric();
					}
					break;
				}
				case STATIC:
					// Cannot redistribute into a static route
					continue;
				}
				
				// Edge direction is the inverse of the direction of
				// redistribution
				this.addEdge(redistributee.getInVertex(), 
						redistributor.getOutVertex(), weight, 
						EdgeType.INTRA_DEVICE);
			}
		}
	}
	
	/**
	 * Add edges for route redistribution between processes on the same device.
	 * @param processes the routing processes running on a device
	 */
	private void adjustRouteRedistributionEdgeWeights(
			List<Process> processes) {
		// Sort routing processes based on administrative distance
		Collections.sort(processes);
		
		// Add redistribution edges between all pairs
		int redistributorPriority = 0;
		for (Process redistributor : processes) {
			for (Process redistributee : processes) {
				DirectedEdge<ProcessVertex> edge = this.getEdge(
						redistributee.getInVertex(), 
						redistributor.getOutVertex());
				if (edge != null) {
					// Get the maximum edge weight within the redistributee's
					// instance
					Instance redistributeeInstance = 
							this.instanceEtg.getInstance(redistributee);
					double weight = redistributeeInstance.getMaxEdgeWeight();
					
					// Check if the fixed cost assigned to redistributed routes
					// is greater, and update the weight with that value
					Instance redistributorInstance = 
							this.instanceEtg.getInstance(redistributor);
					// Edge direction is the inverse of the direction of
					// redistribution
					DirectedEdge<InstanceVertex> instanceEdge = 
							this.instanceEtg.getEdge(
									redistributeeInstance.getVertex(), 
									redistributorInstance.getVertex());
					if (instanceEdge != null 
							&& instanceEdge.getWeight() > weight) {
						weight = instanceEdge.getWeight();
					}
					
					// Multiply by the redistributee's instance size, to get 
					// the maximum cost of a path through the instance
					weight *= redistributeeInstance.getConstituentProcesses().
							size();
					
					if (weight == 0) {
						weight = 1;
					}
					
					// Multiply by the redistributor's priority, as determined
					// by administrative distance
					weight *= redistributorPriority;
					
					// Add the existing edge weight
					weight += edge.getWeight();
					
					// Update weight of edge; edge direction is the inverse of 
					// the direction of redistribution
					this.setEdgeWeight(redistributee.getInVertex(),
							redistributor.getOutVertex(), weight);
				}
			}
			redistributorPriority++;
		}
	}
	
	/**
	 * Add edges for route redistribution between processes on the same device.
	 * @param processes the routing processes running on a device
	 */
	/*private void constructRouteRedistributionEdges(
			List<Process> processes) {
		// Sort routing processes based on administrative distance
		Collections.sort(processes);
		
		// Add redistribution edges between all pairs
		int multiplier = 0;
		for (Process redistributee : processes) {
			for (Process redistributor : processes) {
				double weight = multiplier * this.getMaxNetworkPathWeight();
				weight = 0; // FIXME
				
				// Edge to stay within a process is based on process's ordering
				// relative to other processes; we do not want to stay within
				// a process if another process has a lower administrative
				// distance (AD) because it will win in the global RIB if it
				// has a route to the destination
				if (redistributor == redistributee) {
					double withinProcessWeight = weight;
					withinProcessWeight = 0;
					if (redistributor.isBgpProcess()) {
						continue;
					}
					this.addEdge(redistributor.getInVertex(), 
							redistributee.getOutVertex(), withinProcessWeight, 
							EdgeType.INTRA_DEVICE);
					continue;
				}
				
				// Consider OSPF redistribution policy
				if (redistributor.isOspfProcess()) {
					OspfProcess ospfProcess = redistributor.getOspfConfig();
					OspfRedistributionPolicy policy = 
							ospfProcess.getRedistributionPolicies().get(
								redistributee.getProtocol());
					if (policy == null) {
						//weight = DirectedEdge.INFINITE_WEIGHT;
						// Don't add an edge if there is no redistribution
						continue;
					}
					else if (policy.getMetric() != null) {
						// FIXME: Consider metric type
						weight += policy.getMetric();
					}
					// FIXME: consider OSPF proc number
				}
				// Consider BGP redistribution policy
				else if (redistributor.isBgpProcess()) {
					BgpProcess bgpProcess = redistributor.getBgpConfig();
					BgpRedistributionPolicy policy =
							bgpProcess.getRedistributionPolicies().get(
									redistributee.getProtocol());
					if (policy == null) {
						//weight = DirectedEdge.INFINITE_WEIGHT;
						// Don't add an edge if there is no redistribution
						continue;
					}
					else if (policy.getMetric() != null) {
						weight += policy.getMetric();
					}
					// FIXME: consider OSPF proc number
				}
				else if (redistributor.isStaticProcess()) {
					continue;
				}
				
				this.addEdge(redistributor.getInVertex(), 
						redistributee.getOutVertex(), weight, 
						EdgeType.INTRA_DEVICE);
			}
			multiplier++;
		}
	}*/
	
	/**
	 * Scale edge weights to ensure precision when route redistribution is a 
	 * DAG.
	 */
	private void scaleEdgeWeights() {
		// Make all edges have higher weights to avoid very small weights
		// after scaling
		Iterator<DirectedEdge<ProcessVertex>> iterator = 
				this.getEdgesIterator();
		double scaleFactor = Math.pow(10, 
				this.instanceEtg.getInstances().size());
		while (iterator.hasNext()) {
			DirectedEdge<ProcessVertex> edge = iterator.next();
			this.setEdgeWeight(edge.getSource(), edge.getDestination(),
					edge.getWeight() * scaleFactor);
		}
		
		List<Instance> unvisited = new ArrayList<Instance>();
		unvisited.addAll(this.instanceEtg.getInstances());
		while (unvisited.size() > 0) {
			this.scaleEdgeWeights(unvisited.get(0), unvisited);
		}
	}
	
	/**
	 * Scale a routing instances edge weights to ensure precision when route
	 * redistribution is a DAG.
	 * @param instance the instance whose edge weights to scale
	 * @param unvisited the instances whose weights have not yet been scaled
	 */
	private void scaleEdgeWeights(Instance instance, List<Instance> unvisited) {
		// Remove instance from unvisited list
		unvisited.remove(instance);
		
		// No need to scale if there is no parent instance
		if (this.instanceEtg.getIncomingEdges(instance.getVertex()).size() 
				== 0) {
			return;
		}
		
		// Scale edge weights on all parent instances first
		for (DirectedEdge<InstanceVertex> edge : 
				this.instanceEtg.getIncomingEdges(instance.getVertex())) {
			Instance parent = edge.getSource().getInstance();
			if (unvisited.contains(parent)) {
				this.scaleEdgeWeights(parent, unvisited);
			}
		}
		
		/*// Get the maximum edge weight within the instance
		double maxWeight = instance.getMaxEdgeWeight();
		// Check if the fixed cost assigned to redistributed routes
		// is greater, and update the maximum edge weight with that value
		for (DirectedEdge<InstanceVertex> edge :
				this.instanceEtg.getOutgoingEdges(instance.getVertex())) {
			if (edge.getWeight() > maxWeight) {
				maxWeight = edge.getWeight();
			}
		}
		maxWeight *= Math.pow(10, 
				this.instanceEtg.getInstances().size()-1);
		
		// Get the maximum number of hops through the instance
		int maxHops = instance.getConstituentProcesses().size() + 1;
		
		// Get the maximum number of redistribution edges
		int maxRedistributions = 0;
		for (Process process : instance.getConstituentProcesses()) {
			int numRedistributions = 
					process.getDevice().getRoutingProcesses().size();
			if (numRedistributions > maxRedistributions) {
				maxRedistributions = numRedistributions;
			}
		}
		
		// Compute the maximum cost to traverse the instance
		double instanceMax = maxWeight * maxHops * maxRedistributions + 1;*/
		
		// Compute the maximum cost to traverse the instance
		double instanceMax = 0;
		for (Process process : instance.getConstituentProcesses()) {
			for (DirectedEdge<ProcessVertex> edge :
					this.getOutgoingEdges(process.getInVertex())) {
				instanceMax += edge.getWeight();
			}
			for (DirectedEdge<ProcessVertex> edge :
					this.getOutgoingEdges(process.getOutVertex())) {
				instanceMax += edge.getWeight();
			}
		}
		
		// Get the minimum gap cost among the parent instances
		double minGap = DirectedEdge.INFINITE_WEIGHT;
		for (DirectedEdge<InstanceVertex> edge : 
				this.instanceEtg.getIncomingEdges(instance.getVertex())) {
			Instance parent = edge.getSource().getInstance();
			double gap = this.getMinimumWeightGap(parent);
			if (gap < minGap) {
				minGap = gap;
			}
		}
				
		// Compute the scaling factor
		double scaleFactor = minGap / instanceMax;
		/*logger.info(String.format(
				"%s minGap %f maxWeight %f maxHops %d maxRedist %d scale %f", 
				instance, minGap, maxWeight, maxHops, maxRedistributions,
				scaleFactor));*/
		logger.info(String.format("%s minGap %f instanceMax %f scale %f", 
				instance, minGap, instanceMax, scaleFactor));
		if (instanceMax < minGap) {
			// No need to scale
			return;
		}
		
		// Scale weights for the outgoing edges of each process's in and out 
		// vertices
		for (Process process : instance.getConstituentProcesses()) {
			for (DirectedEdge<ProcessVertex> edge :
					this.getOutgoingEdges(process.getInVertex())) {
				double newWeight = edge.getWeight() * scaleFactor;
				this.setEdgeWeight(edge.getSource(), edge.getDestination(), 
						newWeight);
			}
			for (DirectedEdge<ProcessVertex> edge :
					this.getOutgoingEdges(process.getOutVertex())) {
				double newWeight = edge.getWeight() * scaleFactor;
				this.setEdgeWeight(edge.getSource(), edge.getDestination(), 
						newWeight);
			}
		}
	}

	/**
	 * Get the minimum difference in weights between any two edges within a 
	 * routing instance.
	 * @param instance routing instance whose minimum weight gap to compute
	 * @return the minimum difference in weights between any two edges within a 
	 * 		routing instance; infinity if all weights are zero
	 */
	private double getMinimumWeightGap(Instance instance) {
		// Determine weights assigned to edges within the instance
		Set<Double> sortedWeights = new TreeSet<Double>();
		sortedWeights.add(0.0);
		// Add weights for the outgoing edges of each process's in and out 
		// vertices
		for (Process process : instance.getConstituentProcesses()) {
			for (DirectedEdge<ProcessVertex> edge :
					this.getOutgoingEdges(process.getInVertex())) {
				sortedWeights.add(edge.getWeight());
			}
			for (DirectedEdge<ProcessVertex> edge :
					this.getOutgoingEdges(process.getOutVertex())) {
				sortedWeights.add(edge.getWeight());
			}
		}
		
		// Compute the minimum difference between two consecutive weights
		double minGap = DirectedEdge.INFINITE_WEIGHT;
		Iterator<Double> iterator = sortedWeights.iterator();
		double prevWeight = iterator.next();
		while (iterator.hasNext()) {
			double currWeight = iterator.next();
			double gap = currWeight - prevWeight;
			if (gap < minGap) {
				minGap = gap;
			}
			prevWeight = currWeight;
		}
		
		return minGap;
		
		/*// Determine weights assigned to edges within the instance
		Set<Integer> sortedWeights = new TreeSet<Integer>();
		// Add weights for the outgoing edges of each process's in and out 
		// vertices
		for (Process process : instance.getConstituentProcesses()) {
			for (DirectedEdge<ProcessVertex> edge :
					this.getOutgoingEdges(process.getInVertex())) {
				sortedWeights.add((int)edge.getWeight());
			}
			for (DirectedEdge<ProcessVertex> edge :
					this.getOutgoingEdges(process.getOutVertex())) {
				sortedWeights.add((int)edge.getWeight());
			}
		}
		
		// Compute the greatest common divisor among all weights
		Iterator<Integer> iterator = sortedWeights.iterator();
		int gcd = iterator.next();
		while (iterator.hasNext()) {
			int a = gcd;
			int b = iterator.next();
			while (b != 0) {
				int t = b;
				b = a % b;
				a = t;
			}
			gcd = a;
		}
		return gcd;*/
	}
	
	/**
	 * Add edges for the source(s) and destination of the flow.
	 */
	protected void constructEndpointEdges() {
		Set<PolicyGroup> sources = this.getFlowSourceVertices().keySet();
		for (PolicyGroup source : sources) {
			this.constructSourceEndpointEdges(source);
		}
		
		this.constructDestinationEndpointEdges(this.getFlow().getDestination());
	}
	
	/**
	 * Add edges for a specific destination.
	 * @param destination the destination for whom to add edges
	 */
	private void constructDestinationEndpointEdges(PolicyGroup destination) {
		if (!destination.isInternal()) {
			for (Process process : bgpWithOutsidePeers) {
				this.addEdge(process.getInVertex(),
						this.getFlowDestinationVertex(), 0,
						EdgeType.INTER_DEVICE);
				// TODO: Check for ACLs and route-maps
			}
		}
		
		// Add edges from process vertices to destination vertex
		for (Entry<Ip,Map<Device,Interface>> interfacesForAddress :
				this.interfacesByAddress.entrySet()) {
			ProcessVertex sourceVertex = null;
			ProcessVertex destinationVertex = null;

			// We only care about interfaces whose prefix falls within the
			//  destination policy groups
			if (destination.contains(interfacesForAddress.getKey())) {
				destinationVertex = this.getFlowDestinationVertex();
			}
			else {
				continue;
			}

			// Iterate over all devices that have an interface whose address
			// falls within the destination policy group
			for (Entry<Device,Interface> interfaceEntry :
					interfacesForAddress.getValue().entrySet()) {
				Device device = interfaceEntry.getKey();
				Interface iface = interfaceEntry.getValue();

				// Iterate over all routing processes on the device
				for (Process process : device.getRoutingProcesses()) {
					// Make sure destination is advertised by the process
					if (!process.advertises(destination, this)) {
						continue;
					}

					// Set process vertex as source vertex
					sourceVertex = process.getInVertex();
					Interface sourceIface = iface;

					// Add an edge
					if (!this.containsEdge(sourceVertex, destinationVertex)) {
						DirectedEdge<ProcessVertex> edge = this.addEdge(
									sourceVertex, destinationVertex, 0, 
									EdgeType.INTRA_DEVICE, sourceIface, null);

						// For a graph covering a single flow, check if
						// edges need to be marked as blocked
						if (!this.getFlow().hasWildcardSource()) {
							edge.checkAndBlockOutgoing(this.getFlow(), device);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Add edges for a specific source.
	 * @param source the source for whom to add edges
	 */
	private void constructSourceEndpointEdges(PolicyGroup source) {
		// If source is external, then connect to all external interfaces
		if (!source.isInternal()) {
			for (Process process : bgpWithOutsidePeers) {
				this.addEdge(this.getFlowSourceVertex(source),
						process.getOutVertex(), 0,
						EdgeType.INTER_DEVICE);
				// TODO: Check for ACLs and route-maps
			}
		}

		// Add edges from source vertex to process vertices
		for (Entry<Ip,Map<Device,Interface>> interfacesForAddress :
				this.interfacesByAddress.entrySet()) {
			// We only care about interfaces whose prefix falls within the
			// source policy group
			if (!source.contains(interfacesForAddress.getKey())) {
				continue;
			}

			// Iterate over all devices that have an interface whose address
			// falls within the source policy group
			for (Entry<Device,Interface> interfaceEntry :
					interfacesForAddress.getValue().entrySet()) {
				Device device = interfaceEntry.getKey();
				Interface iface = interfaceEntry.getValue();

				// See if the destination is also connected to the device
				Interface flowDestinationIface = null;
				for (Interface deviceIface : device.getInterfaces()) {
					if (deviceIface.hasPrefix()
							&& this.getFlow().getDestination().contains(
							deviceIface.getPrefix())) {
						flowDestinationIface = deviceIface;
						break;
					}
				}

				// Multiplier used to compute weights for edges from the source
				// vertex
				int multiplier = 0;
				
				ProcessVertex sourceVertex = this.getFlowSourceVertex(source);

				// Iterate over all routing processes on the device
				for (Process process : device.getRoutingProcesses()) {
					// Set process vertex as source or destination vertex,
					// depending on whether the device's interface matched
					// the source or destination policy group					
					ProcessVertex destinationVertex = process.getOutVertex();
					Interface destinationIface = iface;

					// Add an edge
					if (!this.containsEdge(sourceVertex, destinationVertex)) {
						DirectedEdge<ProcessVertex> edge = this.addEdge(
									sourceVertex, destinationVertex,
									multiplier * this.getMaxNetworkPathWeight(),
									EdgeType.INTRA_DEVICE, null,
									destinationIface);

						// For a graph covering a single flow, check if
						// edges need to be marked as blocked
						if (!this.getFlow().hasWildcardSource()) {
							edge.checkAndBlockIncoming(this.getFlow(), device);
						}
					}

					// Add an edge from source to destination (sort of)
					// FIXME?
					if (flowDestinationIface != null
							&& !this.containsEdge(destinationVertex,
							this.getFlowDestinationVertex())) {
						// Add an edge
						DirectedEdge<ProcessVertex> edge = this.addEdge(
								destinationVertex,
								this.getFlowDestinationVertex(), 0,
								EdgeType.INTER_DEVICE, flowDestinationIface,
								null);
						if (!this.getFlow().hasWildcardSource()) {
							edge.checkAndBlockOutgoing(this.getFlow(), device);
						}	
					}

					multiplier++;
				}
			}
		}
	}
	
	protected void customizeEdges() {		
		for (Device device : this.devices) {
			this.customizeEdges(device);
		}
		/*for (Device device : this.externalDevices.values()) {
			this.customizeEdges(device);
		}*/
	}
	
	private void customizeEdges(Device device) {
		for (Process process : device.getRoutingProcesses()) {
			// Remove static process vertices if the static route does not apply
			// to the flow for which the ETG is customized
			if (process.isStaticProcess()) {
				PolicyGroup matchGroup = new PolicyGroup(
						process.getStaticRouteConfig().getPrefix());
				if (!this.getFlow().getDestination().intersects(matchGroup)) {
					this.removeVertex(process.getInVertex());
					this.removeVertex(process.getOutVertex());
					continue;
				}
			}
			
			for (DirectedEdge<ProcessVertex> edge : 
					this.getOutgoingEdges(process.getOutVertex())) {
				/*if (null == edge.getSourceInterface()) {
					continue;
				}*/
				edge.checkAndBlockOutgoing(this.getFlow(), process.getDevice());
			}
			
			for (DirectedEdge<ProcessVertex> edge : 
					this.getIncomingEdges(process.getInVertex())) {
				/*if (null == edge.getDestinationInterface()) {
					continue;
				}*/
				edge.checkAndBlockIncoming(this.getFlow(), process.getDevice());
			}
		}
	
		// FIXME: Account for route-maps
	}
	
	/*private void updateBgpEdges(Process localProcess, Flow flow) {
		BgpProcess localBgpProcess = localProcess.getBgpConfig();
		
		// Check if the BGP process advertises the destination prefix
		boolean advertisesDestination = false;
		for (Prefix network : localBgpProcess.getNetworks()) {
			if (flow.getDestination().within(network)) {
				advertisesDestination = true;
				break;
			}
		}
		
		// Check if the BGP process redistributes connected prefixes
		BgpRedistributionPolicy policy = 
				localBgpProcess.getRedistributionPolicies().get(
						RoutingProtocol.CONNECTED);
		if (policy != null) {
			RouteMap routeMap = 
					localProcess.getDevice().getRouteMap(policy.getMap());
			for (Interface iface : localProcess.getDevice().getInterfaces()) {
				if (flow.getDestination().within(iface.getPrefix())
						&& (null == routeMap || !flow.isBlocked(routeMap, 
								localProcess.getDevice()))) {
					advertisesDestination = true;
					break;
				}
			}
		}
		
		// FIXME: Are there other valid ways of deciding what is advertised?
		
		if (!advertisesDestination) {
			Iterator<Process> iterator =
					localProcess.getAdjacentProcessesIterator();
			while (iterator.hasNext()) {
				Process remoteProcess = iterator.next();
				this.setEdgeWeight(remoteProcess.getOutVertex(), 
						localProcess.getInVertex(), 
						DirectedEdge.INFINITE_WEIGHT);
			}
		}
	}*/


	/**
	 * Get the maximum cross-autonomous system weight (maximum IGP link cost
	 * times the maximum possible network diameter).
	 * @return the maximum cross-network weight
	 */
	private double getMaxASPathWeight() {
		return this.maxIGPLinkCost * this.devices.size();
	}
	
	/**
	 * Get the maximum cross-network weight (maximum AS path weight times the 
	 * maximum possible internal AS path length).
	 * @return the maximum cross-network weight
	 */
	private double getMaxNetworkPathWeight() {
		return this.getMaxASPathWeight() * this.internalASes.size();
	}
	

	/**
	 * Get all internal and external devices whose processes are part of this
	 * graph.
	 * @return all internal and external devices
	 */
	public List<Device> getDevices() {
		return this.devices;
	}

	
	/** 
	 * Remove a link from the topology graph.
	 * @param link the link to remove
	 */
	public void removeLink(Link link) {
		List<DirectedEdge<ProcessVertex>> toRemove = 
				new ArrayList<DirectedEdge<ProcessVertex>>();
		
		// Find edges whose source and destination devices correspond to the
		// link being removed
		Iterator<DirectedEdge<ProcessVertex>> iterator =
				this.getEdgesIterator();
		while (iterator.hasNext()) {
			DirectedEdge<ProcessVertex> edge = iterator.next();
			ProcessVertex srcVertex = (ProcessVertex)edge.getSource();
			ProcessVertex dstVertex = (ProcessVertex)edge.getDestination();
			
			// Ignore endpoint and intra-device edges
			if (srcVertex.getType() == VertexType.SOURCE
					|| dstVertex.getType() == VertexType.DESTINATION
					|| (srcVertex.getType() == VertexType.IN 
						&& dstVertex.getType() == VertexType.OUT)) {
				continue;
			}
			
			// Check if the edge corresponds to the link of interest
			String srcDeviceName = srcVertex.getProcess().getDevice().getName();
			String dstDeviceName = dstVertex.getProcess().getDevice().getName();
			if ((srcDeviceName.equals(link.getSourceDeviceName())
					&& dstDeviceName.equals(link.getDestinationDeviceName()))
				|| (srcDeviceName.equals(link.getDestinationDeviceName())
					&& dstDeviceName.equals(link.getSourceDeviceName()))) {
				toRemove.add(this.getEdge(srcVertex, dstVertex));
			}
		}
		
		// Remove matching edges
		for (DirectedEdge<ProcessVertex> edge : toRemove) {
			this.removeEdge(edge);
		}
	}
	
	/**
	 * Get the number of processes of a particular type.
	 * @param type of process to count
	 * @return the number of process of a particular type in the graph
	 */
	public int numberOfType(ProcessType type) {
		int count = 0;
		for (Device device : this.devices) {
			for (Process process : device.getRoutingProcesses()) {
				if (process.getType() == type) {
					count++;
				}
			}
		}
		return count;
	}
}
