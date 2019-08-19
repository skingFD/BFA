package edu.wisc.cs.arc.graphs;

/**
 * A vertex for instance-based extended topology graph.
 * @author Raajay Viswanathan (raajay.v@gmail.com)
 * @author Aaron Gember-Jacobson (agember@cs.wisc.edu)
 */
public class InstanceVertex extends Vertex {
  private static final long serialVersionUID = 8318085428941828229L;

  /** Routing instance associated with the vertex*/;
  protected Instance instance;

  /**
   * Create a vertex for instance-based extended topology graph.
   * @param instance routing instance associated with the vertex
   * @param type type of vertex
   */
  public InstanceVertex(Instance instance, VertexType type) {
    super(type);
    if(instance != null) {
      this.setName(instance.getName());
    }
    this.instance = instance;
  }

  /**
   * Get the instance associated with the vertex.
   * @return The {@Instance} associated with the vertex.
   */
  public Instance getInstance() {
    return this.instance;
  }
}
