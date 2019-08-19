package edu.wisc.cs.arc.graphs;

/**
 * A vertex for a device-based extended topology graph.
 * @author Aaron Gember-Jacobson (agember@cs.wisc.edu)
 */
public class DeviceVertex extends Vertex {	
	private static final long serialVersionUID = -1046057989380346332L;
	
	/** Device associated with the vertex */ 
	private Device device;
	
	/**
	 * Create a vertex for a device-based extended topology graph.
	 * @param device device associated with the vertex
	 * @param type type of vertex
	 */
	public DeviceVertex(Device device, VertexType type) {
		super(type);
		if (device != null) {
			this.setName(device.getName());
		}
		this.device = device;
	}
	
	/**
	 * Get the device associated with the vertex.
	 * @return device associated with the vertex
	 */
	public Device getDevice() {
		return this.device;
	}
}
