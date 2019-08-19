package edu.wisc.cs.arc.graphs;

/**
 * A vertex for an interface-based extended topology graph.
 * @author Aaron Gember-Jacobson
 */
public class InterfaceVertex extends Vertex {
	private static final long serialVersionUID = 8318085428941828229L;
	
	/** Interface associated with the vertex */ 
	private Interface iface;
	
	/**
	 * Create a vertex for an interface-based extended topology graph.
	 * @param interface interface associated with the vertex
	 * @param type type of vertex
	 */
	public InterfaceVertex(Interface iface, VertexType type) {
		super(type);
		if (iface != null) {
			this.setName(iface.getDevice().getName() + "." 
					+ iface.getName() + "." + type.toString());
		}
		this.iface = iface;
	}
	
	/**
	 * Get the interface associated with the vertex.
	 * @return interface associated with the vertex
	 */
	public Interface getInterface() {
		return this.iface;
	}
}
