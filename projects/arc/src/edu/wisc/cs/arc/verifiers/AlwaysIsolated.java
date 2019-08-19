package edu.wisc.cs.arc.verifiers;

import java.util.Iterator;
import java.util.Map;

import edu.wisc.cs.arc.Settings;
import edu.wisc.cs.arc.graphs.DirectedEdge;
import edu.wisc.cs.arc.graphs.ExtendedTopologyGraph;
import edu.wisc.cs.arc.graphs.Flow;

/**
 * Checks if two flows always traverse different links.
 * @author Aaron Gember-Jacobson (agember@cs.wisc.edu)
 */
@SuppressWarnings("rawtypes")
public class AlwaysIsolated extends Verifier {
	
	/**
	 * Construct a verifier.
	 * @param etgs the extended topology graphs to use for verification
	 * @param settings the settings to use during verification
	 */
	public AlwaysIsolated(Map<Flow, ? extends ExtendedTopologyGraph> etgs,
			Settings settings) {
		super(etgs, settings);
	}

	/**
	 * Check the property for a specific pair of flows.
	 * @param flow flow for which to check the property
	 * @param arg flow from which we want to be isolated
	 * @return true if the property holds, otherwise false
	 */
	public boolean verify(Flow flow, Object arg) {
		// Check and cast argument
		if (!(arg instanceof Flow)) {
			throw new VerifierException("Argument must be a flow");
		}
		Flow flowA = flow;
		Flow flowB = (Flow)arg;
		
		// Get ETGs
		ExtendedTopologyGraph etgA = this.etgs.get(flowA);
		if (null == etgA) {
			throw new VerifierException("No ETG for flow "+flowA);
		}
		ExtendedTopologyGraph etgB = this.etgs.get(flowB);
		if (null == etgB) {
			throw new VerifierException("No ETG for flow "+flowB);
		}
		
		// If any edges are shared between the ETGs, then isolation is not
		// guaranteed: in the worst case all links have failed except those
		// used in paths that contain a common edge.
		Iterator<DirectedEdge> iterator = etgA.getEdgesIterator();
		while(iterator.hasNext()) {
			DirectedEdge edgeA = iterator.next();
			if (etgB.getGraph().containsEdge(edgeA.getSource(), 
					edgeA.getDestination())) {
				return false;
			}
		}
		
		return true;
	}
}
