package edu.wisc.cs.arc.graphs;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import edu.wisc.cs.arc.graphs.Process.ProcessType;
import edu.wisc.cs.arc.graphs.Vertex.VertexType;

/**
 * A instance of routing; a collection of {@code Process}
 * @author Raajay Viswanathan
 */
public class Instance implements Comparable<Instance>, Serializable {
	private static final long serialVersionUID = -6455256694157735816L;

	// TODO(raajay) check if we need AD information

	/** The collection of routing processes that form this routing instance.*/
	protected Set<Process> constituentProcesses;

	/** The corresponding instance vertex */
	protected InstanceVertex vertex;

	// TODO(raajay)  need an identifier for type of routing instance

	/**
	 * Create a new routing instance
	 * @param processes The set of constituent processes
	 */
	public Instance(Set<Process> processes) {
		this.constituentProcesses = processes;
		this.vertex = new InstanceVertex(this, VertexType.NORMAL);
	}

	/**
	 * Get the constituent processes of this {@code Instance}
	 * @return The {@code Set} of {@code Process} belonging to this instance
	 */
	public Set<Process> getConstituentProcesses() {
		return this.constituentProcesses;
	}

	/**
	 * Get the name for the {@code Instance}.
	 * @return The name of the vertex, as a string
	 */
	public String getName() {
		String name = null;
		Set<Integer> pids = new LinkedHashSet<Integer>();
		for (Process process : this.constituentProcesses) {
			switch(process.getType()) {
			case OSPF:
				pids.add(process.getOspfConfig().getPid());
				name = "ospf." + StringUtils.join(pids.iterator(), "/");
				break;
			case BGP:
				pids.add(process.getBgpConfig().getPid());
				name = "bgp." + StringUtils.join(pids.iterator(), "/");
				break;
			case STATIC:
				name = "static." + process.getDevice().getName() + "." 
						+ process.getStaticRouteConfig().getPrefix();
				break;
			}
		}
		return name;
	}

	/**
	 * Get the maximum weight.
	 * @return maximum weight
	 */
	public double getMaxEdgeWeight() {
		double maxWeight = 0;
		for (Process process : this.constituentProcesses) {
			switch(process.getType()) {
			case OSPF:
				for (Interface iface : process.getInterfaces()) {
					if (iface.getOspfCost() != null 
							&& iface.getOspfCost() > maxWeight) {
						maxWeight = iface.getOspfCost();
					}
				}
				break;
			case BGP:
				maxWeight = 1;
				break;
			case STATIC:
				break;
			}
		}
		return maxWeight;
	}

	/**
	 * Get the {@code InstanceVertex} corresponding this {@code Instance}.
	 * @return An {@code InstanceVertex} for this {@code Instance} to be used in
	 * an extended topology graph.
	 */
	public InstanceVertex getVertex() {
		return vertex;
	}
	
	/**
	 * Get the type of instance.
	 * @param type of instance
	 */
	public ProcessType getType() {
		for (Process process : this.constituentProcesses) {
			return process.getType();
		}
		return null;
	}

	@Override
	public int compareTo(Instance arg0) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public String toString() {
		return this.getName();
	}
}
