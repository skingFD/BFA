package edu.wisc.cs.arc.verifiers;

import java.util.Map;

import edu.wisc.cs.arc.Settings;
import edu.wisc.cs.arc.graphs.ExtendedTopologyGraph;
import edu.wisc.cs.arc.graphs.Flow;

/**
 * Checks if two control planes (i.e., their ETGs) are equivalent.
 * @author Aaron Gember-Jacobson (agember@cs.wisc.edu)
 */
public class Equivalent extends Verifier {

	/**
	 * Construct a verifier.
	 * @param etgs the extended topology graphs to use for verification
	 * @param settings the settings to use during verification
	 */
	public Equivalent(Map<Flow, ? extends ExtendedTopologyGraph> etgs, 
			Settings settings) {
		super(etgs, settings);
	}

	/**
	 * Check the property for a specific flow.
	 * @param flow flow for which to check the property
	 * @param arg the ETGs for the control plane to compare against
	 * @return true if the property holds, otherwise false
	 */
	@Override
	public boolean verify(Flow flow, Object arg) {
		// Get ETGs
		ExtendedTopologyGraph etgA = this.etgs.get(flow);
		if (null == etgA) {
			throw new VerifierException("No ETG for flow "+flow);
		}

		Map<Flow, ? extends ExtendedTopologyGraph> otherEtgs = 
				(Map<Flow, ? extends ExtendedTopologyGraph>)arg;
		
		ExtendedTopologyGraph etgB = otherEtgs.get(flow);
		if (null == etgB) {
			throw new VerifierException("No ETG for flow "+flow);
		}
		
		return etgA.isEquivalent(etgB);
	}
}
