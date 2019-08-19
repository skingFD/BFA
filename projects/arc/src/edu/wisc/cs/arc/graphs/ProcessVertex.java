package edu.wisc.cs.arc.graphs;

import edu.wisc.cs.arc.GeneratorException;

/**
 * A vertex for a process-based extended topology graph.
 * @author Aaron Gember-Jacobson
 */
public class ProcessVertex extends Vertex {
	private static final long serialVersionUID = 8318085428941828229L;
	
	/** Routing process associated with the vertex */ 
	private Process process;
	
	/**
	 * Create a vertex for a process-based extended topology graph.
	 * @param process process associated with the vertex
	 * @param type type of vertex
	 */
	public ProcessVertex(Process process, VertexType type) {
		super(type);
		if (process != null) {
			this.setName(process.getName() + "." + type.toString());
		}
		this.process = process;
	}
	
	/**
	 * Create a vertex for a process-based extended topology graph.
	 * @param group policy group associated with the vertex
	 * @param type type of vertex
	 */
	public ProcessVertex(PolicyGroup group, VertexType type) {
		super(type);
		if (!(type == VertexType.SOURCE || type == VertexType.DESTINATION)) {
			throw new GeneratorException("Cannot create vertex of type " + type 
					+ " for policy group");
		}
		if (group != null) {
			this.setName(type.toString() + "." + group.toString());
		}
		this.process = null;
	}
	
	/**
	 * Get the routing process associated with the vertex.
	 * @return process associated with the vertex
	 */
	public Process getProcess() {
		return this.process;
	}
}