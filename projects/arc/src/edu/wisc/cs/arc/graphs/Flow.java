package edu.wisc.cs.arc.graphs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

import org.batfish.representation.LineAction;
import org.batfish.representation.Prefix;
import org.batfish.representation.cisco.ExtendedAccessList;
import org.batfish.representation.cisco.ExtendedAccessListLine;
import org.batfish.representation.cisco.PrefixList;
import org.batfish.representation.cisco.PrefixListLine;
import org.batfish.representation.cisco.RouteMap;
import org.batfish.representation.cisco.RouteMapClause;
import org.batfish.representation.cisco.RouteMapMatchIpAccessListLine;
import org.batfish.representation.cisco.RouteMapMatchIpPrefixListLine;
import org.batfish.representation.cisco.RouteMapMatchLine;
import org.batfish.representation.cisco.StandardAccessList;
import org.batfish.representation.cisco.StandardAccessListLine;

import edu.wisc.cs.arc.GeneratorException;

/**
 * Represents a pair of communicating policy groups. 
 * 
 * @author Aaron Gember-Jacobson
 */
public class Flow implements Serializable {
	private static final long serialVersionUID = 4251002186681527261L;

	/** Entities sending traffic */
	private PolicyGroup source;
	
	/** Entities receiving traffic */
	private PolicyGroup destination;
	
	/**
	 * Creates a flow between two policy groups.
	 * @param source entities sending traffic
	 * @param destination entities receiving traffic
	 */
	public Flow(PolicyGroup source, PolicyGroup destination) {
		this.source = source;
		this.destination = destination;
	}
	
	/**
	 * Creates a flow with a wildcard source policy group and a specific 
	 * destination policy group.
	 * @param destination entities receiving traffic
	 */
	public Flow(PolicyGroup destination) {
		this(null, destination);
	}
	
	/**
	 * Get the entities sending traffic.
	 * @return entities sending traffic
	 */
	public PolicyGroup getSource() {
		return this.source;
	}
	
	/**
	 * Get the entities receiving traffic.
	 * @return entities receiving traffic
	 */
	public PolicyGroup getDestination() {
		return this.destination;
	}

	/**
	 * Change the entities sending traffic.
	 */
	public void setSource(PolicyGroup source) {
		this.source = source;
	}
	
	/**
	 * Determine if the flow corresponds to multiple flows with the same
	 * destination.
	 * @return true if the flow corresponds to multiple flows, otherwise false
	 */
	public boolean hasWildcardSource() {
		return (null == this.source);
	}
	
	@Override
	public String toString() {
		String sourceString;
		if (source == null) {
			sourceString = "*";
		} else {
			sourceString = this.source.toString();
		}
		return sourceString + " -> " + this.destination.toString();
	}
	
    @Override
    public boolean equals(Object other) {
    	if (this == other) {
    		return true;
    	}
    	if (!(other instanceof Flow)) {
    		return false;
    	}
		Flow otherFlow = (Flow)other;
//    	if (this.source == null) {
//    		return this.destination.equals(otherFlow.destination);
//		}
		if ((this.source != null && !this.source.equals(otherFlow.source))
				|| !this.destination.equals(otherFlow.destination)) {
			return false;
		}
		return true;
	}
    
    @Override
    public int hashCode() {
    	return this.toString().hashCode();
    }
    
    /**
     * Checks if the flow is blocked by an ACL.
     * @param acl access control list to check
     * @return true if the flow is blocked the ACL; otherwise false
     */
    public boolean isBlocked(StandardAccessList acl) {
    	// "Standard ACLs control traffic by comparing the source address of the
    	// IP packets to the addresses configured in the ACL."
    	// [http://cisco.com/c/en/us/support/docs/ip/access-lists/26448-ACLsamples.html]
    	for (StandardAccessListLine line : acl.getLines()) {
    		// Check if the source is covered by the current line in the ACL
			Prefix prefix = new Prefix(line.getIP(),
					32 - line.getWildcard().numWildcardBits());
			if (this.source.within(prefix)) {
				return (line.getAction() == LineAction.REJECT);
			}
		}
    	
    	// "By default, there is an implicit deny all clause at the end of every
    	// ACL." 
    	// [http://cisco.com/c/en/us/support/docs/ip/access-lists/26448-ACLsamples.html]
    	return true;
    }
    
    /**
     * Checks if the flow is blocked by an ACL.
     * @param acl access control list to check
     * @return true if the flow is blocked the ACL; otherwise false
     */
    public boolean isBlocked(ExtendedAccessList acl) {
    	// "Extended ACLs (registered customers only) control traffic by
    	// comparing the source and destination addresses of the IP packets to
    	// the addresses configured in the ACL."
    	// [http://cisco.com/c/en/us/support/docs/ip/access-lists/26448-ACLsamples.html]
    	for (ExtendedAccessListLine line : acl.getLines()) {
    		// Check if the source and destination are covered by the current
    		// line in the ACL
    		Prefix sourcePrefix = new Prefix(line.getSourceIp(),
					32 - line.getSourceWildcard().numWildcardBits());
    		Prefix destinationPrefix = new Prefix(line.getDestinationIp(),
					32 - line.getDestinationWildcard().numWildcardBits());
    		if (this.source.within(sourcePrefix)
    				&& this.destination.within(destinationPrefix)) {
    			// FIXME: Also check ports
    			return (line.getAction() == LineAction.REJECT);
    		}
    	}
    	
    	// "By default, there is an implicit deny all clause at the end of every
    	// ACL." 
    	// [http://cisco.com/c/en/us/support/docs/ip/access-lists/26448-ACLsamples.html]
    	return true;
    }
    
    /**
     * Checks if the flow is blocked by a route map.
     * @param routeMap route map to check
     * @return true if the flow is blocked the route map; otherwise false
     */
    public boolean isBlocked(RouteMap routeMap, Device device){
    	ArrayList<Integer> clauseIds = 
    			new ArrayList<Integer>(routeMap.getClauses().keySet());
    	Collections.sort(clauseIds);
    	for (Integer clauseId : clauseIds) {
    		RouteMapClause clause = routeMap.getClauses().get(clauseId);
    		for (RouteMapMatchLine line : clause.getMatchList()) {
    			switch (line.getType()) {
    			case IP_PREFIX_LIST:
    				RouteMapMatchIpPrefixListLine ipPrefixLine = 
    						(RouteMapMatchIpPrefixListLine)line;
    				for (String prefixListName : ipPrefixLine.getListNames()) {
    					PrefixList prefixList = 
    							device.getPrefixList(prefixListName);
    					return isBlocked(prefixList);
    				}
    				break;
    			case IP_ACCESS_LIST:
    				RouteMapMatchIpAccessListLine ipAclLine =
    						(RouteMapMatchIpAccessListLine)line;
    				for (String aclName : ipAclLine.getListNames()) {
    					StandardAccessList standardAcl = 
    							device.getStandardAcl(aclName);
    					if (standardAcl != null) {
    						return isBlocked(standardAcl);
    					}
    					
    					ExtendedAccessList extendedAcl = 
    							device.getExtendedAcl(aclName);
    					if (extendedAcl != null) {
    						return isBlocked(extendedAcl);
    					}
    				}
    			default:
    				throw new GeneratorException(
    						"Unhandled route-map match line type "
    						+ line.getType());
    			}
    		}
    	}
    	
    	// FIXME?
    	return true;
    }
    
    /**
     * Checks if the flow is blocked by a prefix list.
     * @param prefixList prefix list to check
     * @return true if the flow is blocked the prefix list; otherwise false
     */
    public boolean isBlocked(PrefixList prefixList) {
    	for (PrefixListLine line : prefixList.getLines()) {
    		// Check if the destination is covered by the current line in the
    		// prefix list
    		if (this.destination.within(line.getPrefix())) {
    			return (line.getAction() == LineAction.REJECT);
    		}
    	}
    	
    	// FIXME?
    	return true;
    }
}
