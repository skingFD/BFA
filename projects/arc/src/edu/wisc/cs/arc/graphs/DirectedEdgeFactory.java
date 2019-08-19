package edu.wisc.cs.arc.graphs;

import java.io.Serializable;

import org.jgrapht.EdgeFactory;

public class DirectedEdgeFactory<V extends Vertex> 
		implements Serializable, EdgeFactory<V,DirectedEdge<V>> {
	private static final long serialVersionUID = -7825911388245593138L;

	@Override
	public DirectedEdge<V> createEdge(V sourceVertex, V destinationVertex) {
		return new DirectedEdge<V>();
	}

}
