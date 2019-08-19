package edu.wisc.cs.arc.graphs;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;

import edu.wisc.cs.arc.graphs.Flow;

/**
 * A task that invokes a constructor to make a flow-specific extended topology 
 * graph.
 * @author Aaron Gember-Jacobson (agember@cs.wisc.edu)
 */
@SuppressWarnings("rawtypes")
public class ConstructTask implements Callable<Map<Flow,ExtendedTopologyGraph>> {

	/** Non-flow-specific extended topology graph */
	private ExtendedTopologyGraph baseEtg;
	
	/** Queue from which to pull flows to verify */
	private Queue<Flow> queue;

	/** Data Structure that maps a given flow to its list of sources */
	private Map<PolicyGroup, List<PolicyGroup>> dstToSources;
	
	/**
	 * Create a task to invoke a constructor to make flow-specific extended
	 * topology graphs.
	 * @param baseEtg non-flow-specific extended topology graph
	 * @param queue queue from which to pull flows to verify
	 */
	public ConstructTask(ExtendedTopologyGraph baseEtg, Queue<Flow> queue,
						 Map<PolicyGroup, List<PolicyGroup>> dstToSources) {
		this.baseEtg = baseEtg;
		this.queue = queue;
		this.dstToSources = dstToSources;
	}
	
	/**
	 * Pulls flows from a queue and invokes the constructor for a flow-specific
	 * extended topology graph for each flow, until the queue is empty.
	 * @return extended topology graph for each flow for which the constructor
	 * 			was invoked
	 */
	@Override
	public Map<Flow,ExtendedTopologyGraph> call() throws Exception {
        int count = 0;
		Map<Flow, ExtendedTopologyGraph> results = 
				new LinkedHashMap<Flow, ExtendedTopologyGraph>();
		Flow flow = this.queue.poll();
		while(flow != null) {
			ExtendedTopologyGraph flowEtg = 
					(ExtendedTopologyGraph)this.baseEtg.clone();
			if (flow.hasWildcardSource()) {
				flowEtg.customize(flow, 
						this.dstToSources.get(flow.getDestination()));
			} else {
				flowEtg.customize(flow);
			}
			results.put(flow, flowEtg);
            count++;
            if (count % 100 == 0) {
                baseEtg.logger.debug("Constructed "+count+" ETGs");
            }
			flow = this.queue.poll();
		}
		return results;
	}
}
