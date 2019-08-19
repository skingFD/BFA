package edu.wisc.cs.arc.virl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.wisc.cs.arc.Logger;
import edu.wisc.cs.arc.graphs.Device;
import edu.wisc.cs.arc.graphs.DeviceGraph;
import edu.wisc.cs.arc.graphs.DeviceVertex;
import edu.wisc.cs.arc.graphs.DirectedEdge;
import edu.wisc.cs.arc.graphs.Flow;
import edu.wisc.cs.arc.graphs.Interface;
import edu.wisc.cs.arc.graphs.PolicyGroup;
import edu.wisc.cs.arc.verifiers.VerifierException;

/**
 * A failure scenario.
 * @author Aaron Gember-Jacobson (agember@cs.wisc.edu)
 */
public class Scenario {
	/** Logger */
	private Logger logger;

	/** Device FIBs */
	private Map<String, FIB> fibs;
	
	/** Failed links */
	private List<Link> failedLinks;
	
	/** Physical topology */
	private DeviceGraph deviceEtg;
	
	/**
	 * Create a failure scenario.
	 */
	public Scenario(DeviceGraph deviceEtg, Logger logger) {
		this.logger = logger;
		this.fibs = new HashMap<String, FIB>();
		this.failedLinks = new ArrayList<Link>();
		this.deviceEtg = deviceEtg;
	}
	
	/**
	 * Add a failed link to the scenario.
	 * @param failedLink the link that has failed
	 */
	public void addFailedLink(Link failedLink) {
		this.failedLinks.add(failedLink);
	}
	
	/**
	 * Get the failed links.
	 * @return links that are failed in the scenario
	 */
	public List<Link> getFailedLinks() {
		return this.failedLinks;
	}
	
	/**
	 * Add a FIB for a device in the scenario.
	 * @param node name of device
	 * @param fib device's FIB
	 */
	public void addFIB(String node, FIB fib) {
		fibs.put(node, fib);
	}
	
	/**
	 * Compute a path for a given destination.
//	 * @param destination destination policy group
	 * @return computed path
	 */
	public List<Device> computePath(Flow flow) {
		DeviceGraph flowEtg = (DeviceGraph)deviceEtg.clone();
		flowEtg.customize(flow);
		
		List<Device> path = null;
		for (DirectedEdge<DeviceVertex> edge : 
			flowEtg.getOutgoingEdges(flowEtg.getFlowSourceVertex(flow.getSource()))) {
			Device startingDevice = edge.getDestination().getDevice();
			path = this.computePath(flow, startingDevice);
			//System.out.println("FIB-based path option: " + path);
		}
		return path;
	}
	
	/**
	 * Compute a path for a given destination.
//	 * @param destination destination policy group
	 * @param startingDevice where the path should start
	 * @return computed path
	 */
	public List<Device> computePath(Flow flow, Device startingDevice) {
		DeviceGraph flowEtg = (DeviceGraph)deviceEtg.clone();
		flowEtg.customize(flow);
				
		List<Device> path = new ArrayList<Device>();
		Device currentDevice = startingDevice;
		while (!path.contains(currentDevice)) {
			// Add device to path
			path.add(currentDevice);
			
			// Get matching FIB entry
			FIB fib = this.fibs.get(currentDevice.getName());
			FIBEntry entry = fib.getMatchingEntry(flow.getDestination());
			
			// If no entry matches, then there is not a complete path
			if (null == entry) {
				return null;
			}
			
			// If there is no next hop, then we have reached a device that is
			// directly connected to the destination
			if (!entry.hasNextHop()) {
				break;	
			}
			
			// Determine the outgoing interface to reach the next device
			Interface outIface = currentDevice.getInterface(
					entry.getInterface());
			// If the interface isn't specified, then we need to lookup the next
			// hop to determine the outgoing interface
			if (null == outIface) {
				FIBEntry outEntry = fib.getMatchingEntry(
						new PolicyGroup(entry.getNextHop(),entry.getNextHop()));
				if (null == outEntry || null == outEntry.getInterface()) {
					throw new VerifierException("Unexpected FIB structure");
				}
				outIface = currentDevice.getInterface(outEntry.getInterface());
				if (null == outIface) {
					throw new VerifierException(currentDevice.getName()
							+ " doesn't have an interface '" 
							+ outEntry.getInterface() + "'");
				}
			}
			
			// Determine which device is next
			Device nextDevice = this.deviceEtg.getConnectedDevice(outIface);
			if (null == nextDevice) {
				//throw new VerifierException("No device connected to " 
				//		+ currentDevice + ":" + outIface);
				return null;
			}
			
			currentDevice = nextDevice;
		}
		return path;
	}
	
	@Override
	public String toString() {
		if (0 == this.failedLinks.size()) {
			return "No failures";
		}
		
		String result = "Failures: ";
		for (Link link : this.failedLinks) {
			result += link + ", ";
		}
		return result.substring(0, result.length()-2);
	}
}
