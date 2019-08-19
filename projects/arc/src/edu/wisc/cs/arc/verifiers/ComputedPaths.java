package edu.wisc.cs.arc.verifiers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.alg.KShortestPaths;

import edu.wisc.cs.arc.Settings;
import edu.wisc.cs.arc.graphs.Device;
import edu.wisc.cs.arc.graphs.DirectedEdge;
import edu.wisc.cs.arc.graphs.ExtendedTopologyGraph;
import edu.wisc.cs.arc.graphs.Flow;
import edu.wisc.cs.arc.graphs.InterfaceVertex;
import edu.wisc.cs.arc.graphs.ProcessVertex;
import edu.wisc.cs.arc.virl.Scenario;

/**
 * Checks if the path computed using an extended topology graph matches the path
 * computing using FIBs dumped from Cisco Virtual Internet Routing Lab (VIRL).
 * @author Aaron Gember-Jacobson (agember@cs.wisc.edu)
 */
@SuppressWarnings("rawtypes")
public class ComputedPaths extends Verifier {
	/**
	 * Construct a verifier.
	 * @param etgs the extended topology graphs to use for verification
	 * @param settings the settings to use during verification
	 */
	public ComputedPaths(Map<Flow, ? extends ExtendedTopologyGraph> etgs, 
			Settings settings) {
		super(etgs, settings);
	}

	/**
	 * Check the property for a specific flow and scenario.
	 * @param flow flow for which to check the property
	 * @param arg unused
	 * @return true if the property holds, otherwise false
	 */
	@Override
	public boolean verify(Flow flow, Object arg) {
		// Check and cast argument
		if (!(arg instanceof Scenario)) {
			throw new VerifierException("Argument must be a scenario");
		}
		Scenario scenario = (Scenario)arg;
				
		// Get ETG
		ExtendedTopologyGraph etg = this.etgs.get(flow);
		if (null == etg) {
			throw new VerifierException("No ETG for flow "+flow);
		}
		
		// Clone ETG and fail links
		ExtendedTopologyGraph failureEtg = (ExtendedTopologyGraph)etg.clone();
		failureEtg.removeLinks(scenario.getFailedLinks());
		
		// Compute path using ETG
		KShortestPaths shortestPaths = new KShortestPaths(etg.getGraph(), 
				etg.getFlowSourceVertex(flow.getSource()), 2);
		List<GraphPath> paths = shortestPaths.getPaths(
				etg.getFlowDestinationVertex());
		
		List<List<DirectedEdge>> etgEdgePaths = 
				new ArrayList<List<DirectedEdge>>();
		if (paths != null) {
			double minWeight = paths.get(0).getWeight();
			for (GraphPath path : paths) {
				if (path.getWeight() > minWeight) {
					break;
				}
				etgEdgePaths.add(path.getEdgeList());
			}
		}
		
		List<List<Device>> etgPaths = new ArrayList<List<Device>>();
		for (List<DirectedEdge> etgEdgePath : etgEdgePaths) {
			List<Device> etgPath = this.convertPath(etgEdgePath);
			if (etgPath != null) {
				etgPaths.add(etgPath);
			}
		}
		
		/*// Compute path using ETG
		List<DirectedEdge> etgEdgePath = DijkstraShortestPath.findPathBetween(
				etg.getGraph(), etg.getFlowSourceVertex(), 
				etg.getFlowDestinationVertex());
		List<Device> etgPath = this.convertPath(etgEdgePath);*/

		// Compute path using FIBs
		List<Device> fibPath = null;
		if (0 == etgPaths.size()) {
			fibPath = scenario.computePath(flow);
		} else {
			Device startingDevice = etgPaths.get(0).get(0);
			
			// If starting device is external, then start at the internal device
			Device prepend = null;
			if (startingDevice.isExternal()) {
				prepend = startingDevice;
				startingDevice = etgPaths.get(0).get(1);
			}
			fibPath = scenario.computePath(flow, startingDevice);
			
			// If starting device was external, then add it to the front of the
			// path
			if (prepend != null && fibPath != null) {
				fibPath.add(0, prepend);
			}
		}
		
		List<Device> equalEtgPath = null;
		boolean equal = false;
		// If neither method resulted in a path, then they are equal
		if (0 == etgPaths.size() && null == fibPath) {
			equal = true;
		}
		// If only one method resulted in a path, then they aren't equal
		else if (0 == etgPaths.size() || null == fibPath) {
			if (etgPaths.size() > 0) {
				equalEtgPath = etgPaths.get(0);
			}
			equal = false;
		}
		else {
			for (List<Device> etgPath : etgPaths) {
				// If the paths are different lengths, then they aren't equal
				if (etgPath.size() != fibPath.size()) {
					continue;
				}
				
				// Check if every device in the path is the same
				boolean currentEqual = true;
				for (int i = 0; i < etgPath.size(); i++) {
					if (!etgPath.get(i).getName().equals(
							fibPath.get(i).getName())) {
						currentEqual = false;
						break;
					}
				}
				
				// If every device in the path is the same, then they are equal
				if (currentEqual) {
					equalEtgPath = etgPath;
					equal = true;
					break;
				}
			}
		}

		if (!equal) {
			System.out.println("---------------------------------------------");
			//System.out.println("Scenario: " + scenario);
			System.out.println(String.format("Flow: %s-%s_%s-%s",
					flow.getSource().getStartIp(),
					flow.getSource().getEndIp(),
					flow.getDestination().getStartIp(),
					flow.getDestination().getEndIp()));
			System.out.println("FIB-based path: " + fibPath);
			//System.out.println("ETG-based edge path: " + etgEdgePath);
			//System.out.println("ETG-based path: " + equalEtgPath);
			System.out.println("ETG-based path: " + etgPaths);
		}
		
		return equal;
	}
	
	/**
	 * Convert ETG-based path to a list of devices
	 * @param edgePath list of edges in the path
	 * @return list of devices, null if provided path is invalid
	 */
	private List<Device> convertPath(List<DirectedEdge> edgePath) {
		// If no edgePath is provided, then return null
		if (null == edgePath) {
			return null;
		}

		List<Device> devicePath = new ArrayList<Device>();
		Device lastDevice = null;
		for (DirectedEdge edge : edgePath) {
			Device currentDevice = null;
			
			// Get the device based on the type of vertices in the edge
			if (edge.getDestination() instanceof ProcessVertex) {
				ProcessVertex destination = 
						(ProcessVertex)edge.getDestination();
				if (destination.getProcess() != null) {
					currentDevice = destination.getProcess().getDevice();
				}
			} else if (edge.getDestination() instanceof InterfaceVertex) {
				InterfaceVertex destination = 
						(InterfaceVertex)edge.getDestination();
				if (destination.getInterface() != null) {
					currentDevice = destination.getInterface().getDevice();
				}
			}		

			if (currentDevice != lastDevice && currentDevice != null) {
				devicePath.add(currentDevice);
			}
			lastDevice = currentDevice;
		}
		return devicePath;
	}
}
