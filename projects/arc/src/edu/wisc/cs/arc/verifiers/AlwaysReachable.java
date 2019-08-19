package edu.wisc.cs.arc.verifiers;

import java.util.Iterator;
import java.util.Map;

import org.jgrapht.Graphs;
import org.jgrapht.alg.MinSourceSinkCut;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import edu.tsinghua.lyf.maxFlow;
import edu.tsinghua.lyf.maxFlowutil;
import edu.wisc.cs.arc.Settings;
import edu.wisc.cs.arc.graphs.DirectedEdge;
import edu.wisc.cs.arc.graphs.DirectedEdge.EdgeType;
import edu.wisc.cs.arc.graphs.ExtendedTopologyGraph;
import edu.wisc.cs.arc.graphs.Flow;
import edu.wisc.cs.arc.graphs.Vertex;

/**
 * Checks if a source and destination can always communicate when there are
 * fewer than k failures in the network.
 * @author Aaron Gember-Jacobson (agember@cs.wisc.edu)
 */
@SuppressWarnings("rawtypes")
public class AlwaysReachable extends Verifier {
	
	/**
	 * Construct a verifier.
	 * @param etgs the extended topology graphs to use for verification
	 * @param settings the settings to use during verification
	 */
	public AlwaysReachable(Map<Flow, ? extends ExtendedTopologyGraph> etgs,
			Settings settings) {
		super(etgs, settings);
	}

	/**
	 * Check the property for a specific flow.
	 * @param flow flow for which to check the property
	 * @param arg maximum number of link failures to tolerate
	 * @return true if the property holds, otherwise false
	 */
	@Override
	public boolean verify(Flow flow, Object arg) {
		if (!(arg instanceof Integer)) {
			throw new VerifierException("Argument must be an integer");
		}
		int maxFailuresExclusive = (Integer)arg;
		
		// Get ETG
		ExtendedTopologyGraph etg = this.etgs.get(flow);
		if (null == etg) {
			throw new VerifierException("No ETG for flow "+flow);
		}
		
		// Create unit weight graph
		DefaultDirectedWeightedGraph<Vertex,DirectedEdge> unitWeightGraph =
				new DefaultDirectedWeightedGraph<Vertex,DirectedEdge>(
						DirectedEdge.class);
		Graphs.addAllVertices(unitWeightGraph, etg.getGraph().vertexSet());
		Iterator<DirectedEdge> iterator = etg.getEdgesIterator();
		while (iterator.hasNext()) {
			DirectedEdge edge = iterator.next();
			DirectedEdge newEdge = unitWeightGraph.addEdge(edge.getSource(), 
					edge.getDestination());
			if (EdgeType.INTER_DEVICE == edge.getType()) {
				unitWeightGraph.setEdgeWeight(newEdge, 1);
			}
			else {
				unitWeightGraph.setEdgeWeight(newEdge, 
						DirectedEdge.INFINITE_WEIGHT);
			}
		}
		
		// We can tolerate up to maxFailures failures and still have 
		// reachability if the min cut (or max flow) onthe unit weight graph is 
		// at least one unit more than maxFailures
		MinSourceSinkCut<Vertex,DirectedEdge> minCutAlgorithm = 
				new MinSourceSinkCut<Vertex,DirectedEdge>(unitWeightGraph);
		//System.out.println(System.nanoTime());
		minCutAlgorithm.computeMinCut(etg.getFlowSourceVertex(flow.getSource()),
				etg.getFlowDestinationVertex());
		Vertex source = etg.getFlowSourceVertex(flow.getSource());
		Vertex dest = etg.getFlowDestinationVertex();
		//System.out.println(source);
		//System.out.println(dest);

		//double minCut = minCutAlgorithm.getCutWeight();
		//System.out.println(minCut);
		double minCut = maxFlowutil.getResult(unitWeightGraph, source, dest);
		//System.out.println(minCut);
        if (flow.getDestination().getStartIp().asLong()==0) {
            System.out.println("MINCUT: " + minCut + " " + flow.toString());
        }
		return (minCut > maxFailuresExclusive);
	}
}
