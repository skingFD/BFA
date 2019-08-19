package edu.wisc.cs.arc.graphs;

import org.batfish.representation.cisco.ExtendedAccessList;
import org.batfish.representation.cisco.StandardAccessList;
import org.jgrapht.graph.DefaultWeightedEdge;

/**
 * A directed edge for an extended topology graph.
 *  @author Aaron Gember-Jacobson (agember@cs.wisc.edu)
 */
public class DirectedEdge<V extends Vertex> extends DefaultWeightedEdge {
	
	private static final long serialVersionUID = 1662486723525719870L;
	
	public static final double INFINITE_WEIGHT = Double.MAX_VALUE;
	
	private Interface sourceInterface = null;
	
	private Interface destinationInterface = null;
	
	private boolean blocked = false;
	
	public enum EdgeType {
		INTER_DEVICE,
		INTRA_DEVICE,
		INTER_INSTANCE
	}
	
	private EdgeType type;
	
	/**
	 * Get the name of the edge.
	 * @return name of the edge
	 */
	public String getName() {
		return this.getSource().getName()+"->"+this.getDestination().getName();
	}
	
	/**
	 * Get the source vertex for the edge.
	 * @return source of the edge
	 */
	@SuppressWarnings("unchecked")
	public V getSource() {
		return (V)super.getSource();
	}
	
	/**
	 * Get the destination vertex for the edge.
	 * @return destination of the edge
	 */
	@SuppressWarnings("unchecked")
	public V getDestination() {
		return (V)super.getTarget();
	}
	
	/**
	 * Get the weight assigned to the edge.
	 * @return weight of the edge
	 */
	public double getWeight() {
		return super.getWeight();
	}
	
	/**
	 * Set the type of the edge.
	 * @param type type of edge
	 */
	public void setType(EdgeType type) {
		this.type = type;
	}
	
	/**
	 * Get the type assigned to the edge.
	 * @return type of the edge
	 */
	public EdgeType getType() {
		return this.type;
	}
	
	/**
	 * Store the interfaces associated with the edge.
	 * @param sourceInterface source interface
	 * @param destinationInterface destination interface
	 */
	public void setInterfaces(Interface sourceInterface, 
			Interface destinationInterface) {
		this.sourceInterface = sourceInterface;
		this.destinationInterface = destinationInterface;
	}
	
	/**
	 * Get the source interface associated with the edge.
	 * @return the source interface associated with the edge
	 */
	public Interface getSourceInterface() {
		return this.sourceInterface;
	}
	
	/**
	 * Get the destination interface associated with the edge.
	 * @return the destination interface associated with the edge
	 */
	public Interface getDestinationInterface() {
		return this.destinationInterface;
	}
	
	/**
	 * Indicate that an ACL prevents traffic from traversing the edge.
	 */
	public void markBlocked() {
		this.blocked = true;
	}
	
	/**
	 * Check if an ACL prevents traffic from traversing the edge.
	 * @return true if an ACL blocks traffic on the edge, otherwise false
	 */
	public boolean isBlocked() {
		return this.blocked;
	}
	
	public void checkAndBlockOutgoing(Flow flow, Device device) {
		Interface iface = this.getSourceInterface();
        if (null == iface) {
            return; // FIXME
        }
		if (iface.getOutgoingFilter() != null) {
			StandardAccessList stdAcl = device.getStandardAcl(
							iface.getOutgoingFilter());
			ExtendedAccessList extAcl = device.getExtendedAcl(
							iface.getOutgoingFilter());
			if ((stdAcl != null && flow.isBlocked(stdAcl))
					|| (extAcl != null && flow.isBlocked(extAcl))) {
				this.markBlocked();
			}
		}
	}
	
	public void checkAndBlockIncoming(Flow flow, Device device) {
		Interface iface = this.getDestinationInterface();
        if (null == iface) {
            return; // FIXME
        }
		if (iface.getIncomingFilter() != null) {
			StandardAccessList stdAcl = device.getStandardAcl(
							iface.getIncomingFilter());
			ExtendedAccessList extAcl = device.getExtendedAcl(
							iface.getIncomingFilter());
			if ((stdAcl != null && flow.isBlocked(stdAcl))
					|| (extAcl != null && flow.isBlocked(extAcl))) {
				this.markBlocked();
			}
		}
	}
	
	/**
	 * Get the name and weight of the edge.
	 */
	@Override
	public String toString() {
		String result = this.getName();
		if (this.getWeight() == INFINITE_WEIGHT) {
			result += " INF";
		}
		else {
			result += " " + this.getWeight();
		}
		if (this.sourceInterface != null && this.destinationInterface != null) {
			result += " (" + this.sourceInterface.getName() + "->"
					+ this.destinationInterface.getName() + ")";
		}
		return result;
	}
}
