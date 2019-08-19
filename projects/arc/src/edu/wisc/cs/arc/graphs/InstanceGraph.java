package edu.wisc.cs.arc.graphs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.wisc.cs.arc.graphs.DirectedEdge.EdgeType;
import edu.wisc.cs.arc.graphs.Process.ProcessType;
import edu.wisc.cs.arc.graphs.Vertex.VertexType;
import edu.wisc.cs.arc.virl.Link;
import org.jgrapht.alg.CycleDetector;

/**
 * An extended topology graph where the vertices are routing instances and
 * edges represent instance adjacencies and route-redistribution.
 *
 * @author Raajay Viswanathan (raajay.v@gmail.com)
 * @author Aaron Gember-Jacobson (agember@cs.wisc.edu)
 */
public class InstanceGraph extends ExtendedTopologyGraph<InstanceVertex> {
  private static final long serialVersionUID = -808081253375932844L;

  /**
   * The process graph from which this instance graph is created.
   */
  protected ProcessGraph rpg;

  /**
   * A collection of Instances and its constituent processes
   */
  protected Map<Instance, Set<Process>> instances;

  /**
   * Reverse mapping from process to instance
   */
  protected Map<Process, Instance> process2instance;

  /**
   * Map from edge in Instance graph to a collection of process graph edges
   * that contributed to creation of the edge. Used to determine connectivity
   * under
   * failures.
   */
  protected Map<DirectedEdge<InstanceVertex>, Set<DirectedEdge<ProcessVertex>>>
    generatingEdgeMap;


  /**
   * Create an instance-based extended topology graph from a process-based
   * extended topology graph
   *
   * @param rpg The routing process ETG from which this instance based ETG is
   *            created
   */
  public InstanceGraph(ProcessGraph rpg) {
    super(rpg.settings);
    this.rpg = rpg;
    this.instances = new HashMap<Instance, Set<Process>>();
    this.process2instance = new HashMap<Process, Instance>();
    this.generatingEdgeMap = new HashMap<DirectedEdge<InstanceVertex>, 
    		Set<DirectedEdge<ProcessVertex>>>();

    this.constructVertices();
    this.constructEdges();
  }


  /**
   * Create the vertices of the {@code InstanceGraph} from {@code ProcessGraph}.
   * We identify instances by finding connected components of processes, using
   * {@code adjProcesses} as the adjacency. We employ a recursive strategy to
   * find connected components.
   */
  private void constructVertices() {
    Set<Process> visited = new HashSet<>();
    for (Device device : rpg.getDevices()) {
      for (Process rp : device.getRoutingProcesses()) {

        if (visited.contains(rp)) {
          continue;
        }

        // 0. Identify the component rooted at the current vertex
        Set<Process> component = visit(rp, visited, new HashSet<Process>());

        // 1. Create a new Instance for this component.
        Instance instance = new Instance(component);

        // 1a. Update the internal data structures for bookkeeping
        instances.put(instance, component);
        for (Process p : component) {
         process2instance.put(p, instance);
        }

        // 2. Add the corresponding instance vertex to the graph
        this.addVertex(instance.getVertex());

        // 3. Update the list of visited processes
        visited.addAll(component);
      }
    }

    // Add flow source/destination vertices
    if (rpg.getFlow() != null) {
      this.customize(rpg.getFlow());
    }
  }


  /**
   * Helper function to recursively identify the connected component of
   * processes.
   *
   * @param root      The current process serving as root of the component
   * @param visited   A set of processes part of other components
   * @param component A set of processes part of current component
   * @return An updated set of processes after traversing root's neighbors.
   */
  private Set<Process> visit(Process root, Set<Process> visited,
                               Set<Process> component) {
    // add the current vertex to the component
    component.add(root);

    Iterator<Process> iter = root.getAdjacentProcessesIterator();
    while (iter.hasNext()) {
      Process neighbor = iter.next();
      if (!visited.contains(neighbor) && !component.contains(neighbor)) {
        component = visit(neighbor, visited, component);
      }
    }
    return component;
  }


  /**
   * Add directed edges between {@code InstanceVertex}. An edge is added from
   * {@code InstanceVertex} u to {@code InstanceVertex} v, if any of the
   * constituent process in v, redistributes its route to any of the process in
   * u.
   */
  private void constructEdges() {
    Iterator<DirectedEdge<ProcessVertex>> processEdgeIter = this.rpg
      .getEdgesIterator();
    while (processEdgeIter.hasNext()) {
      DirectedEdge<ProcessVertex> edge = processEdgeIter.next();
      Process source = edge.getSource().getProcess();
      Process destination = edge.getDestination().getProcess();

      Instance sourceInstance = process2instance.get(source);
      Instance destinationInstance = process2instance.get(destination);

      if (sourceInstance.equals(destinationInstance)) {
        continue;
      }

      // 1. Create a new edge if none is found
      if (!this.containsEdge(sourceInstance.getVertex(),
        destinationInstance.getVertex())) {
        this.addEdge(sourceInstance.getVertex(),
          destinationInstance.getVertex(), edge.getWeight(), EdgeType.INTER_INSTANCE);
      }

      // 2. Append the process pair to the instance edge
      DirectedEdge<InstanceVertex> instanceEdge
        = this.getEdge(sourceInstance.getVertex(),
        destinationInstance.getVertex());

      if (!generatingEdgeMap.containsKey(instanceEdge)) {
        generatingEdgeMap.put(instanceEdge, 
        		new HashSet<DirectedEdge<ProcessVertex>>());
      }
      generatingEdgeMap.get(instanceEdge).add(edge);
    }
  }


  /**
   * Customize the graph for a specific flow.
   *
   * @param flow the flow for which the instance graph should be customized
   * @return true if the graph is successfully customized, otherwise false
   */
  @Override
  public boolean customize(Flow flow) {
    Map<PolicyGroup, InstanceVertex> sourceVertices = new HashMap<>();
    sourceVertices.put(flow.getSource(), new InstanceVertex(null, VertexType.SOURCE));
    return this.customize(flow, sourceVertices,
      new InstanceVertex(null, VertexType.DESTINATION));
  }

  /**
   * Customize the graph for a destination and list of sources
   * @param sources the list of source policy groups
   * @param flow the flow for which the graph should be customized
   */
  @Override
  public boolean customize(Flow flow, List<PolicyGroup> sources) {
    Map<PolicyGroup, InstanceVertex> sourceVertices = new HashMap<>();
    for (PolicyGroup source: sources) {
      InstanceVertex sourceVertex = new InstanceVertex(null, VertexType.SOURCE);
      sourceVertices.put(source, sourceVertex);
    }
    return this.customize(flow, sourceVertices,
            new InstanceVertex(null, VertexType.DESTINATION));
  }


  /**
   * Adding the edges from source and destination vertices, to instance
   * vertices. The instance vertex is determined by the process vertex
   * connected to the source/destination nodes.
   */
  @Override
  protected void constructEndpointEdges() {
    ProcessVertex rpg_source = this.rpg.getFlowSourceVertex(this.rpg.getFlow().getSource()); // not quite sure if
                                                                  // we should be calling getFlowSourceVertex with
                                                                  // this.rpg.getFlow().getSource()
                                                                  // because the flow can have a null source
    for (DirectedEdge<ProcessVertex> src_edge :
      this.rpg.getOutgoingEdges(rpg_source)) {
      Process process = src_edge.getDestination().getProcess();
      InstanceVertex vertex = process2instance.get(process).getVertex();
      this.addEdge(this.getFlowSourceVertex(this.rpg.getFlow().getSource()), vertex, 1.0,
        EdgeType.INTER_INSTANCE);
    }

    ProcessVertex rpg_destination = this.rpg.getFlowDestinationVertex();
    for (DirectedEdge<ProcessVertex> dst_edge :
      this.rpg.getIncomingEdges(rpg_destination)) {
      Process process = dst_edge.getSource().getProcess();
      InstanceVertex vertex = process2instance.get(process).getVertex();
      this.addEdge(vertex, this.getFlowDestinationVertex(), 1.0,
        EdgeType.INTER_INSTANCE);
    }
  }


  @Override
  protected void customizeEdges() {
    // FIXME Most likely nothing is required here; since ACls and other
    // things tha remove edges would already have been taken care of in
    // ProcessGraph
  }


  /**
   * @return A boolean indicating if the instance graph has cycles
   */
  public boolean hasCycles() {
    CycleDetector<InstanceVertex, DirectedEdge<InstanceVertex>> detector;
    detector = new CycleDetector<>(this.getGraph());
    return detector.detectCycles();

  }
  
  /**
   * Get the instance to which a process belongs.
   * @param process process whose instance to retrieve
   * @return the instance to which a process belongs, null if unknown
   */
  public Instance getInstance(Process process) {
	  return this.process2instance.get(process);
  }
  
  /**
   * Get the instances.
   * @return the instances in the network
   */
  public Set<Instance> getInstances() {
	  return this.instances.keySet();
  }
  
	/**
	 * Get the number of instances of a particular type.
	 * @param type of instance to count
	 * @return the number of instances of a particular type in the graph
	 */
	public int numberOfType(ProcessType type) {
		int count = 0;
		for (Instance instance : this.instances.keySet()) {
			if (instance.getType() == type) {
				count++;
			}
		}
		return count;
	}

  @Override
  public void prune() {
    // TODO Auto-generated method stub
    super.prune();
  }


  @Override
  public void removeLinks(List<Link> links) {
    // TODO Auto-generated method stub
    super.removeLinks(links);
  }


  @Override
  public void removeLink(Link link) {
    // TODO Auto-generated method stub

  }

}
