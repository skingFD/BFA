package edu.wisc.cs.arc.verifiers;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jgrapht.alg.DijkstraShortestPath;

import edu.wisc.cs.arc.Settings;
import edu.wisc.cs.arc.graphs.DirectedEdge;
import edu.wisc.cs.arc.graphs.ExtendedTopologyGraph;
import edu.wisc.cs.arc.graphs.Flow;
import edu.wisc.cs.arc.graphs.Vertex;

/**
 * Checks if a flow is always blocked.
 * @author Aaron Gember-Jacobson (agember@cs.wisc.edu)
 */
@SuppressWarnings("rawtypes")
public class AlwaysBlocked extends Verifier {
	
	/**
	 * Construct a verifier.
	 * @param etgs the extended topology graphs to use for verification
	 * @param settings the settings to use during verification
	 */
	public AlwaysBlocked(Map<Flow, ? extends ExtendedTopologyGraph> etgs, 
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
		settings.getLogger().debug("Verify always blocked for " 
				+ flow.toString());
		settings.getLogger().debug("\tSource vertex=" 
				+ etg.getFlowSourceVertex(flow.getSource())
				+ " Present=" + (etg.getVertex(etg.getFlowSourceVertex(
						flow.getSource()).getName()) != null));
		settings.getLogger().debug("\tETG flow=" + etg.getFlow());
		
		Iterator<Vertex> iterator = etg.getVerticesIterator();
		while (iterator.hasNext()) {
			settings.getLogger().debug("\t"+iterator.next());
		}
		
		// Check if a path exists
		List<DirectedEdge> path = DijkstraShortestPath.findPathBetween(
				etg.getGraph(), etg.getFlowSourceVertex(flow.getSource()),
				etg.getFlowDestinationVertex());
		return (null == path);
	}
}
