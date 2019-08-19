package edu.wisc.cs.arc.graphs;

import java.io.Serializable;

/**
 * A vertex for an extended topology graph.
 * @author Aaron Gember-Jacobson (agember@cs.wisc.edu)
 */
public abstract class Vertex implements Serializable {
	private static final long serialVersionUID = -7287102243220789099L;

	/** Type of the vertex */
	private VertexType type;
	
	/** Name of the vertex */
	private String name;
		
	public enum VertexType {
		IN,
		OUT,
		NORMAL,
		SOURCE,
		DESTINATION
	}
	/**
	 * Create a vertex.
	 * @param type type of the vertex
	 */
	public Vertex(VertexType type) {
		this.type = type;
		this.name = type.toString();
	}
	
	/**
	 * Set the name of the vertex.
	 * @param name of the vertex
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Get the name of the vertex.
	 * @return name of the vertex
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Set the type of the vertex
	 */
	public void changeType(){
		if(this.type == VertexType.SOURCE){
			this.type = VertexType.DESTINATION;
			this.name = this.name.replace("SOURCE", "DESTINATION");
		}else{
			this.type = VertexType.SOURCE;
			this.name = this.name.replace("DESTINATION", "SOURCE");
		}
	}
	
	/**
	 * Get the type of the vertex.
	 * @return type of the vertex
	 */
	public VertexType getType() {
		return this.type;
	}
	
	/**
	 * Get the name of the vertex.
	 */
	@Override
	public String toString() {
		return this.getName();
	}
	
	@Override
	public int hashCode() {
		return this.getName().hashCode();
	}
	
	/**
	 * Determine if two vertices have the same name and type.
	 * @param other vertex to compare against
	 */
	public boolean equals(Object other) {
		if (!(other instanceof Vertex)) {
			return false;
		}
		Vertex otherVertex  = (Vertex)other;
		return ((otherVertex.getType() == this.getType())
			&& (otherVertex.getName().equals(this.getName())));
	}
}
