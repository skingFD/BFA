package edu.wisc.cs.arc.verifiers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.wisc.cs.arc.Settings;
import edu.wisc.cs.arc.graphs.ExtendedTopologyGraph;
import edu.wisc.cs.arc.graphs.Flow;

/**
 * A network verifier.
 * @author Aaron Gember-Jacobson (agember@cs.wisc.edu)
 */
@SuppressWarnings("rawtypes")
public abstract class Verifier {
	
	/** The ETGs to use for verification */
	protected Map<Flow, ? extends ExtendedTopologyGraph> etgs;
	
	/** Settings for the verification process */
	protected Settings settings;

	/**
	 * Create a verifier.
	 * @param etgs the ETGs to use for verification
	 * @param settings the settings to use during verification
	 */
	protected Verifier(Map<Flow, ? extends ExtendedTopologyGraph> etgs, 
			Settings settings) {
		this.etgs = etgs;
		this.settings = settings;
	}
	
	/**
	 * Check the property for all flows.
	 * @param arg optional additional argument
	 * @return a table of flows and the result of the property check for 
	 *         each flow
	 */
	public Map<Flow,Boolean> verifyAll(Object arg) {
		Map<Flow, Boolean> results = new LinkedHashMap<Flow, Boolean>();
		
		if (this.settings.shouldParallelize()) {
			// Create a queue of flows to verify
			Queue<Flow> queue = new ConcurrentLinkedQueue<Flow>();
			queue.addAll(this.etgs.keySet());
			
			// Create a thread pool
			int numThreads = Runtime.getRuntime().availableProcessors();
			ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
			
			// Start a VerificationTask for each thread
			List<Future<Map<Flow,Boolean>>> futures = 
					new ArrayList<Future<Map<Flow,Boolean>>>(numThreads);
			for (int t = 0; t < numThreads; t++) {
				VerificationTask task = new VerificationTask(this, queue, arg);
				futures.add(threadPool.submit(task));
			}
			
			// Get the results from each thread
			try {
				for (Future<Map<Flow,Boolean>> future : futures) {
					
						// Get the result from the thread, waiting for the thread to
						// complete, if necessary
						Map<Flow, Boolean> result = future.get();
						results.putAll(result);
				}
			}
			catch (Exception exception) {
				exception.printStackTrace();
				throw new VerifierException("Verication task failed",exception);
			}
			finally {
				threadPool.shutdown();
			}
		}
		else 
		{
			for (Flow flow : this.etgs.keySet()) {
				long startTime = System.nanoTime();
				boolean result = this.verify(flow, arg);
				results.put(flow, result);
				long endTime = System.nanoTime();
	            if (settings.shouldOutputPerflowVerifcationTimes()) {
	            	System.out.println("TIMEONE: " 
	            			+ this.getClass().getSimpleName()
	            			+ " " + (endTime - startTime) + " ns");
	            			/*+ this.etgs.get(flow).getGraph().vertexSet().size()
	            			+ " vertices " 
	            			+ this.etgs.get(flow).getGraph().edgeSet().size()
	            			+ " edges " + result);*/
	            }
			}
		}
		return results;
	}
	
	/**
	 * Check the property for a specific flow.
	 * @param flow flow for which to check the property
	 * @param arg optional additional argument
	 * @return true if the property holds, otherwise false
	 */
	public abstract boolean verify(Flow flow, Object arg);
}
