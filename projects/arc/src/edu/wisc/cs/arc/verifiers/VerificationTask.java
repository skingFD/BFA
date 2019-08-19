package edu.wisc.cs.arc.verifiers;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;

import edu.wisc.cs.arc.graphs.Flow;

/**
 * A task that invokes a verifier for a queue of flows.
 * @author Aaron Gember-Jacobson (agember@cs.wisc.edu)
 */
public class VerificationTask implements Callable<Map<Flow,Boolean>> {

	/** Verifier to invoke */
	private Verifier verifier;
	
	/** Queue from which to pull flows to verify */
	private Queue<Flow> queue;
	
	/** Optional argument to pass to verification procedure */
	private Object arg;
	
	/**
	 * Create a task to invoke a verifier for a queue of flows.
	 * @param verifier verifier to invoke
	 * @param queue queue from which to pull flows to verify
	 * @param arg optional argument to pass to verification procedure
	 */
	public VerificationTask(Verifier verifier, Queue<Flow> queue, Object arg) {
		this.verifier = verifier;
		this.queue = queue;
		this.arg = arg;
	}
	
	/**
	 * Pulls flows from a queue and invokes the verifier for each flow, until
	 * the queue is empty.
	 * @return results for each flow for which the verification procedure was
	 * 			invoked
	 */
	@Override
	public Map<Flow,Boolean> call() throws Exception {
		Map<Flow, Boolean> results = new HashMap<Flow, Boolean>();
		Flow flow = this.queue.poll();
		while(flow != null) {
			long startTime = System.nanoTime();
			boolean result = verifier.verify(flow, this.arg);
			results.put(flow, result);
			long endTime = System.nanoTime();
			if (verifier.settings.shouldOutputPerflowVerifcationTimes()) {
            	System.out.println("TIMEONE: " 
            			+ verifier.getClass().getSimpleName()
            			+ " " + (endTime - startTime) + " ns "
            			+ verifier.etgs.get(flow).getGraph().vertexSet().size()
            			+ " vertices " 
            			+ verifier.etgs.get(flow).getGraph().edgeSet().size()
            			+ " edges " + result);
            }
			flow = this.queue.poll();
		}
		return results;
	}
}
