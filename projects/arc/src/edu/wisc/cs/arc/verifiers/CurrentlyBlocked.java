package edu.wisc.cs.arc.verifiers;

import java.util.List;
import java.util.Map;

import org.jgrapht.alg.DijkstraShortestPath;

import edu.wisc.cs.arc.Settings;
import edu.wisc.cs.arc.graphs.DirectedEdge;
import edu.wisc.cs.arc.graphs.ExtendedTopologyGraph;
import edu.wisc.cs.arc.graphs.Flow;

/**
 * Checks if a flow is always blocked.
 * @author Aaron Gember-Jacobson (agember@cs.wisc.edu)
 */
@SuppressWarnings("rawtypes")
public class CurrentlyBlocked extends Verifier {
	
	/**
	 * Construct a verifier.
	 * @param etgs the extended topology graphs to use for verification
	 * @param settings the settings to use during verification
	 */
	public CurrentlyBlocked(Map<Flow, ? extends ExtendedTopologyGraph> etgs,
			Settings settings) {
		super(etgs, settings);
	}

	/**
	 * Check the property for a specific flow.
	 * @param flow flow for which to check the property
	 * @param arg unused
	 * @return true if the property holds, otherwise false
	 */
	@Override
	public boolean verify(Flow flow, Object arg) {
		// Get ETG
		ExtendedTopologyGraph etg = this.etgs.get(flow);
		if (null == etg) {
			throw new VerifierException("No ETG for flow "+flow);
		}
		
		// Find the shortest path to the destination
		List<DirectedEdge> dstPath = DijkstraShortestPath.findPathBetween(
				etg.getGraph(), etg.getFlowSourceVertex(flow.getSource()),
				etg.getFlowDestinationVertex());
		
		// If there is no shortest path to the destination, then the flow is
		// currently (and always) blocked
		if (null == dstPath) {
			return true;
		}
		
		double dstPathCost = 0;
		for (DirectedEdge edge : dstPath) {
			if (edge.isBlocked()) {
				System.out.println("BLOCKED: " + flow.toString() + " ON " 
						+ edge.toString());
				return true;
			}
			dstPathCost += 0;
		}
		
		return (dstPathCost >= DirectedEdge.INFINITE_WEIGHT);
		
		/*// Find the shortest path to drop
		List<DirectedEdge> dropPath = DijkstraShortestPath.findPathBetween(
				etg.getGraph(), etg.getFlowSourceVertex(), 
				etg.getDropVertex());
		
		// If there is no shortest path to drop, then the flow is not currently
		// (and never will be) blocked
		if (null == dropPath) {
			return false;
		}
		
		// Compute the cost of the shortest path to the destination
		double dstPathCost = 0;
		for (DirectedEdge edge : dstPath) {
			dstPathCost += edge.getWeight();
		}
		
		// Compute the cost of the shortest path to drop
		double dropPathCost = 0;
		for (DirectedEdge edge : dropPath) {
			dropPathCost += edge.getWeight();
		}
		
		// If the shortest path to drop is shorter than the shortest path to the
		// destination, then the flow is currently blocked; otherwise, the flow
		// is currently allowed
		return (dropPathCost < dstPathCost);*/
	}
}
