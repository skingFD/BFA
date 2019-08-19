package edu.wisc.cs.arc.modifiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.KShortestPaths;

import edu.wisc.cs.arc.Logger;
import edu.wisc.cs.arc.graphs.DirectedEdge;
import edu.wisc.cs.arc.graphs.ExtendedTopologyGraph;
import edu.wisc.cs.arc.graphs.Vertex;
import edu.wisc.cs.arc.verifiers.VerifierException;

public abstract class CanonicalETGConverter<V extends Vertex> {

	public static final long serialVersionUID = 1L;
	protected static final Double UPPER_BOUND = 1000000.0;
	protected static final Double EPSILON = 0.00001;
	protected static final Double _PATH_GAP = 0.5;
	protected static final Double _LAMBDA = 0.001;

	protected ExtendedTopologyGraph<V> _etg;
	protected List<String> _ordered_edges;

	protected int _num_paths;
	
	public transient Logger _logger;
	  
	public CanonicalETGConverter(ExtendedTopologyGraph<V> etg)
			throws ModifierException {
		// by default we will use 10 paths, thus resulting in 9 constraints
		this(etg, 10);
	}
	
	public CanonicalETGConverter(ExtendedTopologyGraph<V> etg, 
			int num_paths) throws ModifierException {
		_num_paths = num_paths;
		_etg = etg;
		_logger =  new Logger(Logger.Level.DEBUG);

		if (_etg.getGraph() == null || _etg.getFlowSourceVertex(_etg.getFlow().getSource()) == null
				|| _etg.getFlowDestinationVertex() == null) {
			throw new ModifierException("Either of graph, src-vertex or dst-vertex is null in ETG");
		}

		_ordered_edges = new ArrayList<>();

		setup();
	}

	private void setup() throws ModifierException {
		for (DirectedEdge<V> edge : _etg.getGraph().edgeSet()) {
			_ordered_edges.add(edge.getName());
		}
		Collections.sort(_ordered_edges);
		if(_ordered_edges.size() != _etg.getGraph().edgeSet().size()) {
			throw new VerifierException("Edge names are not distinct.");
		}
	}
	
	protected Double getPathLength(GraphPath<V, DirectedEdge<V>> path) {
		Double retval = 0.0;
		for(DirectedEdge<V> edge : path.getEdgeList()) {
			retval += edge.getWeight();
		}
		return retval;
	}
	
	/**
	 * Obtain the new canonical weights for all edges in the graph
	 *
	 * @return a dictionary with a mapping from all edge to new canonical
	 * weights.
	 */
	public Map<DirectedEdge<V>, Double> getCanonicalEdgeWeights() {

		_logger.debug("\nObtaining canonical weights for ETG:"+_etg.getFlow());
		KShortestPaths<V, DirectedEdge<V>> ksp = 
				new KShortestPaths<V, DirectedEdge<V>>(_etg.getGraph(), 
						_etg.getFlowSourceVertex(_etg.getFlow().getSource()), _num_paths);
		List<GraphPath<V, DirectedEdge<V>>> paths = 
				ksp.getPaths(_etg.getFlowDestinationVertex());
		_logger.debug("Number of paths from src->dst = " 
				+ ((paths == null)? 0:paths.size()));

		Map<DirectedEdge<V>, Double> retval = new HashMap<>();
		if (paths == null || paths.size() == 0) {
			// There are no paths from the source to the destination. In this 
			// case, we will return zero weights for all edges.
			for(DirectedEdge<V> edge : _etg.getGraph().edgeSet()) {
				retval.put(edge, 0.0);
			}
		} else {
			retval = obtainEdgeWeights(paths);
		}
		return retval;
	}
	
	protected abstract Map<DirectedEdge<V>, Double> obtainEdgeWeights(
			List<GraphPath<V, DirectedEdge<V>>> paths);
}
