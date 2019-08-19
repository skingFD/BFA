package edu.wisc.cs.arc;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.tsinghua.lyf.maxFlowutil;
import edu.wisc.cs.arc.graphs.*;

import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.batfish.representation.Ip;
import org.batfish.representation.VendorConfiguration;
import org.batfish.representation.cisco.CiscoVendorConfiguration;
import org.batfish.representation.cisco.ExtendedAccessList;
import org.batfish.representation.cisco.StandardAccessList;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.FloydWarshallShortestPaths;

import edu.wisc.cs.arc.configs.ConfigurationParser;
import edu.wisc.cs.arc.graphs.Process.ProcessType;
import edu.wisc.cs.arc.verifiers.AlwaysBlocked;
import edu.wisc.cs.arc.verifiers.AlwaysIsolated;
import edu.wisc.cs.arc.verifiers.AlwaysReachable;
import edu.wisc.cs.arc.verifiers.ComputedPaths;
import edu.wisc.cs.arc.verifiers.CurrentlyBlocked;
import edu.wisc.cs.arc.verifiers.Equivalent;
import edu.wisc.cs.arc.virl.Scenario;
import edu.wisc.cs.arc.virl.VirlConfigurationGenerator;
import edu.wisc.cs.arc.virl.VirlOutputParser;

/**
 * Starts the ETG generator.
 * @author Aaron Gember-Jacobson (agember@cs.wisc.edu)
 */
public class newDriver {

	public static void main(String[] args) {
		while(true){
			System.out.println("please input: <-var x -configs dir> or <q to quit>:");
			Scanner scanner = new Scanner(System.in);
			String temp = scanner.nextLine();
			if(temp.equals("q"))break;
			String[] input = temp.split(" ");
			newDriver.test(input);
		}
//		String[] input0 = {"-var", "0", "-configs", "D:\\eclipse-workspace\\arc\\configs\\examples\\batfish-nsdi"};
//		String[] input1 = {"-var", "0", "-configs", "D:\\eclipse-workspace\\arc\\configs\\examples\\batfish-nsdi2"};
	}
	public static void test(String[] args) {
		Logger logger = new Logger(Logger.Level.DEBUG);

		Settings settings = null;
		try {
			settings = new Settings(args, logger);
		} catch (ParseException e) {
			logger.fatal(e.getMessage());
			logger.fatal("Run with '-" + Settings.HELP + "' to show help");
			return;
		}

		logger.info(settings.toString());

		// Parse configurations
		ConfigurationParser parser = new ConfigurationParser(logger,
				settings.getConfigsDirection(), settings.shouldParallelize());
		long startTime = System.currentTimeMillis();
		Map<String, VendorConfiguration> vendorConfigs = parser.parse();
		Map<String, String> rawConfigs = parser.getRawConfigurations();
		long endTime = System.currentTimeMillis();
		System.out.println("TIME: parse "+(endTime - startTime)+" ms");

		// Exclude non-routers, if requested
		if (settings.shouldExcludeNonRouters()) {
			List<String> devicesToExclude = new ArrayList<String>();

			// Check each device configuration to see if it contains a router
			// stanza
			for (Entry<String,VendorConfiguration> configEntry :
					vendorConfigs.entrySet()) {
				if (configEntry.getValue() instanceof CiscoVendorConfiguration){
					CiscoVendorConfiguration ciscoConfig =
							(CiscoVendorConfiguration)configEntry.getValue();
					if (0 == ciscoConfig.getBgpProcesses().size()
							&& 0 == ciscoConfig.getOspfProcesses().size()) {
						devicesToExclude.add(configEntry.getKey());
					}
				}
				else {
					throw new GeneratorException(
							"Only Cisco configurations are supported");
				}
			}

			// Remove devices without a router stanza
			for (String deviceToExclude : devicesToExclude) {
				vendorConfigs.remove(deviceToExclude);
				rawConfigs.remove(deviceToExclude);
			}
		}

		// Anonymize device names, if requested
		if (settings.shouldAnonymize()) {
			Map<String, VendorConfiguration> anonVendorConfigs =
					new LinkedHashMap<String, VendorConfiguration>();
			int i = 0;
			for (Entry<String, VendorConfiguration> entry :
					vendorConfigs.entrySet()) {
				anonVendorConfigs.put("dev"+i, entry.getValue());
				i++;
			}
			vendorConfigs = anonVendorConfigs;
		}

		// Extract configuration details
		List<Device> devices = new ArrayList<Device>();
		for (Entry<String, VendorConfiguration> entry :
				vendorConfigs.entrySet()) {
			if (entry.getValue() instanceof CiscoVendorConfiguration) {
				Device device = new Device(entry.getKey(),
						(CiscoVendorConfiguration)entry.getValue(), logger);
				devices.add(device);
			}
			else {
				throw new GeneratorException(
						"Only Cisco configurations are supported");
			}
		}

		// List devices
		System.out.println("COUNT: devices "+devices.size());
		logger.info("Devices:");
		for  (Device device : devices) {
			logger.info("\t"+device.getName());
		}

		// Generate device-based ETG
		logger.info("*** Generate device-based ETG ***");
		DeviceGraph deviceEtg = new DeviceGraph(devices, settings);
		System.out.println("COUNT: deviceETGVertices "
				+ deviceEtg.getVertexCount());
		System.out.println("COUNT: deviceETGEdges "
				+ deviceEtg.getEdgeCount());

		// Create process-based ETG
		logger.info("*** Generate process-based ETG ***");
		startTime = System.currentTimeMillis();
		ProcessGraph processEtg = new ProcessGraph(deviceEtg, settings);
		ExtendedTopologyGraph baseEtg = processEtg;
		endTime = System.currentTimeMillis();
		System.out.println("TIME: baseETG "+(endTime - startTime)+" ms");
		logger.info(baseEtg.toString());

		// Convert process-based ETG to interface-based ETG
		if (settings.shouldGenerateInterfaceETG()) {
			logger.info("*** Generate interface-based ETG ***");
			baseEtg = new InterfaceGraph(processEtg);
		}

		System.out.println("COUNT: baseETGVertices "
				+ baseEtg.getVertexCount());
		System.out.println("COUNT: baseETGEdges "
				+ baseEtg.getEdgeCount());

		System.out.println("COUNT: ospfProcesses "
				+ processEtg.numberOfType(ProcessType.OSPF));
		System.out.println("COUNT: bgpProcesses "
				+ processEtg.numberOfType(ProcessType.BGP));
		System.out.println("COUNT: staticProcesses "
				+ processEtg.numberOfType(ProcessType.STATIC));

		FloydWarshallShortestPaths<Vertex,DirectedEdge<Vertex>> fwsp =
				new FloydWarshallShortestPaths<Vertex, DirectedEdge<Vertex>>(
						baseEtg.getGraph());
		// TODO : Check if this definition of diameter is correct. Is it the max
		// of all the pair-wise shortest paths or the longest path through the
		// graph ?
		int diameter = 0;
		for (GraphPath<Vertex,DirectedEdge<Vertex>> path :
				fwsp.getShortestPaths()) {
			if (path.getEdgeList().size() > diameter) {
				diameter = path.getEdgeList().size();
			}
		}
		System.out.println("COUNT: baseETGDiameter " + diameter);

		// Generate Base Instance Graph
		logger.info("*** Generate instance-based ETG ***");
		InstanceGraph instanceEtg = processEtg.getInstanceEtg();
		System.out.println("COUNT: instanceETGVertices "
				+ instanceEtg.getVertexCount());
		System.out.println("COUNT: instanceETGEdges "
				+ instanceEtg.getEdgeCount());

		System.out.println("COUNT: ospfInstances "
				+ instanceEtg.numberOfType(ProcessType.OSPF));
		System.out.println("COUNT: bgpInstances "
				+ instanceEtg.numberOfType(ProcessType.BGP));
		System.out.println("COUNT: staticInstances "
				+ instanceEtg.numberOfType(ProcessType.STATIC));

		System.out.println("PROP: instanceIsDag "
				+ !((InstanceGraph)instanceEtg).hasCycles());

		// Generate VIRL
		if (settings.shouldGenerateVirl()) {
			generateVirl(settings, rawConfigs, deviceEtg);
		}

		// Determine policy groups
		Set<PolicyGroup> policyGroups = determinePolicyGroups(vendorConfigs,
				settings);

		Map<Flow,? extends ExtendedTopologyGraph> flowEtgs = null;

        /* // Measure memory usage
        Runtime runtime = Runtime.getRuntime();
        try {
		    System.out.println("MEM: preGCa "
                    + (runtime.totalMemory() - runtime.freeMemory()));
            System.gc();
            Thread.sleep(3000);
		    System.out.println("MEM: postGCa1 "
                    + (runtime.totalMemory() - runtime.freeMemory()));
            System.gc();
            Thread.sleep(3000);
		    System.out.println("MEM: postGCa2 "
                    + (runtime.totalMemory() - runtime.freeMemory()));
            System.gc();
            Thread.sleep(3000);
		    System.out.println("MEM: postGCa3 "
                    + (runtime.totalMemory() - runtime.freeMemory()));
        } catch (Exception e) {}
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("MEM: preFlowETGs " + memoryBefore);*/

		if (settings.shouldGenerateFlowETGs()) {
			// Create ETGs for every possible flow
			flowEtgs = generateFlowETGs(settings, baseEtg, policyGroups, devices);
		}

		/* // Measure memory usage
        try {
		    System.out.println("MEM: preGCb "
                    + (runtime.totalMemory() - runtime.freeMemory()));
            System.gc();
            Thread.sleep(3000);
		    System.out.println("MEM: postGCb1 "
                    + (runtime.totalMemory() - runtime.freeMemory()));
            System.gc();
            Thread.sleep(3000);
		    System.out.println("MEM: postGCb2 "
                    + (runtime.totalMemory() - runtime.freeMemory()));
            System.gc();
            Thread.sleep(3000);
		    System.out.println("MEM: postGCb3 "
                    + (runtime.totalMemory() - runtime.freeMemory()));
        } catch (Exception e) {}
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("MEM: postFlowETGs " + memoryAfter);
        long memoryUsed = memoryAfter - memoryBefore;
		System.out.println("MEM: flowETGs " + memoryUsed);*/

		// Generate graphs
		if (settings.shouldGenerateGraphs()) {
			generateGraphs(settings, baseEtg, instanceEtg, deviceEtg, flowEtgs);
		}

		// Serialize ETGs
		if (settings.shouldSerializeETGs() && flowEtgs != null) {
			serializeETGs(settings, flowEtgs);
		}

		// Run verification tasks
		if (flowEtgs != null) {
			runVerificationTasks(settings, flowEtgs, deviceEtg);
		}
	}

	/**
	 * Determine policy groups
	 * @param vendorConfigs
	 * @param settings
	 * @return non-overlapping policy groups
	 */
	private static Set<PolicyGroup> determinePolicyGroups(
			Map<String, VendorConfiguration> vendorConfigs, Settings settings) {
		Logger logger = settings.getLogger();

		// Extract policy groups
		logger.info("*** Extract Policy Groups ***");
		long startTime = System.currentTimeMillis();
		Set<PolicyGroup> groups = PolicyGroup.extract(vendorConfigs);
		long endTime = System.currentTimeMillis();
		System.out.println("TIME: policyGroups "+(endTime - startTime)+" ms");

		// Output raw policy groups
		List<PolicyGroup> sortedGroups = new ArrayList<PolicyGroup>(groups);
		Collections.sort(sortedGroups);
		for (PolicyGroup group : sortedGroups) {
			if (settings.shouldExcludeExternalFlows() && !group.isInternal()) {
				groups.remove(group);
				continue;
			}

			String groupString = group.toString();
			if (settings.shouldAnonymize()) {
				group.makeAnonymous();
				//groupString += "(" + group.toString() + ")";
				groupString = group.toString();
			}
			logger.debug("\t" + groupString
					+ (group.isInternal() ? " INTERNAL" : " EXTERNAL"));
		}
		System.out.println("COUNT: policyGroups "+groups.size());

		// Add entire address space
		if (settings.shouldIncludeEntireFlowspace()) {
			groups.add(new PolicyGroup(new Ip("0.0.0.0"),
					new Ip("255.255.255.255")));
		}

		// Merge policy groups
		/*startTime = System.currentTimeMillis();
		Set<PolicyGroup> mergedGroups = PolicyGroup.getMerged(groups);
		endTime = System.currentTimeMillis();
		System.out.println("TIME: mergePolicyGroups "
					+(endTime - startTime)+" ms");
		System.out.println("COUNT: mergedPolicyGroups "+mergedGroups.size());
		System.out.println("Merged policy groups:");
		sortedGroups = new ArrayList<PolicyGroup>(mergedGroups);
		Collections.sort(sortedGroups);
		for (PolicyGroup group : sortedGroups) {
			System.out.println("\t"+group);
		}*/

		// Compute non-overlapping policy groups
		logger.info("*** Processed Policy Groups ***");
		startTime = System.currentTimeMillis();
		Set<PolicyGroup> nonOverlappingGroups =
				PolicyGroup.getNonOverlapping(groups);
		endTime = System.currentTimeMillis();
		System.out.println("TIME: separatePolicyGroups " +
				(endTime - startTime) + " ms");

		// Remove policy groups with tiny prefixes
		List<PolicyGroup> toRemove = new ArrayList<PolicyGroup>();
		for (PolicyGroup group : nonOverlappingGroups) {
			if (group.getEndIp().asLong() - group.getStartIp().asLong()
					< settings.getMinPolicyGroupsSize()) {
				toRemove.add(group);
			}
		}
		nonOverlappingGroups.removeAll(toRemove);

		// Output unfiltered, non-overlapping policy groups
		System.out.println("COUNT: separatePolicyGroups "
				+nonOverlappingGroups.size());
		sortedGroups = new ArrayList<PolicyGroup>(nonOverlappingGroups);
		Collections.sort(sortedGroups);
		for (PolicyGroup group : sortedGroups) {
			if (settings.shouldExcludeExternalFlows() && !group.isInternal()) {
				nonOverlappingGroups.remove(group);
				continue;
			}

			String groupString = group.toString();
			if (settings.shouldAnonymize()) {
				group.makeAnonymous();
				//groupString += "(" + group.toString() + ")";
				groupString = group.toString();
			}
			logger.info("\t" + groupString
					+ (group.isInternal() ? " INTERNAL" : " EXTERNAL"));
		}

		return nonOverlappingGroups;
	}

	/**
	 * Create ETGs for every possible flow
	 * @param settings
	 * @param baseEtg the ETG on which to base the ETG for each flow
	 * @param policyGroups the policy groups from which to define flows
	 * @return the created ETGs
	 */
	private static Map<Flow,ExtendedTopologyGraph> generateFlowETGs(
			Settings settings, ExtendedTopologyGraph baseEtg,
			Set<PolicyGroup> policyGroups, List<Device> devices) {
		Logger logger = settings.getLogger();

		// Create a queue of flows for which to construct ETGs
		Queue<Flow> queue = new ConcurrentLinkedQueue<Flow>();
		List<Flow> flows = new ArrayList<Flow>(policyGroups.size()
				* policyGroups.size() - policyGroups.size());
		for (PolicyGroup source : policyGroups) {
			for (PolicyGroup destination : policyGroups) {
				if (source.equals(destination)) {
					continue;
				}
				Flow flow = new Flow(source, destination);
				flows.add(flow);
			}
		}

		System.out.println("Flows requiring specific ETGs:");
		Map<PolicyGroup, List<PolicyGroup>> dstToSources =
				new HashMap<PolicyGroup, List<PolicyGroup>>();
		// Check if a flow needs a custom ETG or we can use an ETG with multiple
		// sources and a common destination
		for (Flow flow : flows) {
			if (flowNeedsCustomEtg(flow, devices)) {
				System.out.println(flow.toString());
				queue.add(flow);
			}
			else {
				if (!dstToSources.containsKey(flow.getDestination())) {
					dstToSources.put(flow.getDestination(),
							new ArrayList<PolicyGroup>());
					Flow wildcardFlow = new Flow(flow.getDestination());
					queue.add(wildcardFlow);
				}
				dstToSources.get(flow.getDestination()).add(flow.getSource());
			}
		}

		System.out.println("Flows requiring general ETGs:");
		for (PolicyGroup destination : dstToSources.keySet()) {
			System.out.println("* -> " + destination.toString());
		}

		logger.debug("Need to generate " + queue.size() + " ETGs");

		/*Collection<List<Flow>> flowGroups = FlowGrouper.groupFlows(flows,
				baseEtg);
		baseEtg.logger.debug("Condensable to " + flowGroups.size()
				+ " flow groups");
		for (List<Flow> flowGroup : flowGroups) {
			baseEtg.logger.debug("\t" + flowGroup.toString());
		}*/

        /*// Measure memory usage
        Runtime runtime = Runtime.getRuntime();
        try {
		    System.out.println("MEM: preGCa "
                    + (runtime.totalMemory() - runtime.freeMemory()));
            System.gc();
            Thread.sleep(3000);
		    System.out.println("MEM: postGCa1 "
                    + (runtime.totalMemory() - runtime.freeMemory()));
            System.gc();
            Thread.sleep(3000);
		    System.out.println("MEM: postGCa2 "
                    + (runtime.totalMemory() - runtime.freeMemory()));
            System.gc();
            Thread.sleep(3000);
		    System.out.println("MEM: postGCa3 "
                    + (runtime.totalMemory() - runtime.freeMemory()));
        } catch (Exception e) {}
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("MEM: preFlowETGs " + memoryBefore);*/

		// Generate flow-specific ETGs
		Map<Flow, ExtendedTopologyGraph> flowEtgs =
				new LinkedHashMap<Flow, ExtendedTopologyGraph>();
		long startTime = System.currentTimeMillis();
		if (settings.shouldParallelize()) {
			// Create a thread pool
			int numThreads = Runtime.getRuntime().availableProcessors();
			ExecutorService threadPool = Executors.newFixedThreadPool(
					numThreads);

			// Start a VerificationTask for each thread
			List<Future<Map<Flow,ExtendedTopologyGraph>>> futures =
					new ArrayList<Future<Map<Flow,ExtendedTopologyGraph>>>(
							numThreads);
			for (int t = 0; t < numThreads; t++) {
				ConstructTask task = new ConstructTask(baseEtg, queue, dstToSources);
				futures.add(threadPool.submit(task));
			}

			// Get the results from each thread
			try {
				for (Future<Map<Flow,ExtendedTopologyGraph>> future : futures) {
					// Get the result from the thread, waiting for the thread to
					// complete, if necessary
					Map<Flow,ExtendedTopologyGraph> result = future.get();
					flowEtgs.putAll(result);
				}
			}
			catch (Exception exception) {
				throw new GeneratorException("Generation task failed",
						exception);
			}
			finally {
				threadPool.shutdown();
			}
		}
		else {
			while (!queue.isEmpty()) {
				Flow flow = queue.remove();
				ExtendedTopologyGraph flowEtg =
						(ExtendedTopologyGraph)baseEtg.clone();
				if (flow.hasWildcardSource()) {
					List<PolicyGroup> sources = dstToSources.get(flow.getDestination());
					flowEtg.customize(flow, sources);
					for (PolicyGroup source : sources) {
						Flow tmpFlow = new Flow(source, flow.getDestination());
						flowEtgs.put(tmpFlow, flowEtg);
					}
				} else {
					flowEtg.customize(flow);
					flowEtgs.put(flow, flowEtg);
				}
			}
		}
		long endTime = System.currentTimeMillis();
		System.out.println("TIME: flowETGs "+(endTime - startTime)+" ms");

        /*// Measure memory usage
        try {
		    System.out.println("MEM: preGCb "
                    + (runtime.totalMemory() - runtime.freeMemory()));
            System.gc();
            Thread.sleep(3000);
		    System.out.println("MEM: postGCb1 "
                    + (runtime.totalMemory() - runtime.freeMemory()));
            System.gc();
            Thread.sleep(3000);
		    System.out.println("MEM: postGCb2 "
                    + (runtime.totalMemory() - runtime.freeMemory()));
            System.gc();
            Thread.sleep(3000);
		    System.out.println("MEM: postGCb3 "
                    + (runtime.totalMemory() - runtime.freeMemory()));
        } catch (Exception e) {}
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("MEM: postFlowETGs " + memoryAfter);
        long memoryUsed = memoryAfter - memoryBefore;
		System.out.println("MEM: onlyFlowETGs " + memoryUsed);*/

		return flowEtgs;
	}

	/**
	 * Given a flow, and a list of network devices, check if the flow contains any ACL blocking its traffic class
	 */
	private static boolean flowNeedsCustomEtg(Flow flow, List<Device> devices) {
		for (Device device : devices) {
			for (Interface deviceIface: device.getInterfaces()) {
				if (deviceIface.hasPrefix()) {
					if ((flow.getDestination().contains(deviceIface.getPrefix()) &&
							checkIfIncomingIsBlocked(flow, deviceIface)) ||
							flow.getSource().contains(deviceIface.getPrefix()) &&
									checkIfOutgoingIsBlocked(flow, deviceIface)) {
						return true;
					}
				}
			}

			// FIXME: Also check for static routes
		}
		return false;
	}

	private static boolean checkIfIncomingIsBlocked(Flow flow, Interface deviceIface) {
		if (deviceIface.getIncomingFilter() != null) {
			Device device = deviceIface.getDevice();
			StandardAccessList stdAcl = device.getStandardAcl(
					deviceIface.getIncomingFilter());
			ExtendedAccessList extAcl = device.getExtendedAcl(
					deviceIface.getIncomingFilter());
			if ((stdAcl != null && flow.isBlocked(stdAcl))
					|| (extAcl != null && flow.isBlocked(extAcl))) {
				return true;
			}
		}
		return false;
	}

	private static boolean checkIfOutgoingIsBlocked(Flow flow, Interface deviceIface) {
		if (deviceIface.getOutgoingFilter() != null) {
			Device device = deviceIface.getDevice();
			StandardAccessList stdAcl = device.getStandardAcl(
					deviceIface.getOutgoingFilter());
			ExtendedAccessList extAcl = device.getExtendedAcl(
					deviceIface.getOutgoingFilter());
			if ((stdAcl != null && flow.isBlocked(stdAcl))
					|| (extAcl != null && flow.isBlocked(extAcl))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Serialize the ETGs.
	 * @param "serializedETGsFile" file where the serialized ETGs should be stored
	 * @param flowEtgs the ETGs for each flow
	 * @param settings
	 */
	private static void serializeETGs(Settings settings,
									  Map<Flow,? extends ExtendedTopologyGraph> flowEtgs) {
		Logger logger = settings.getLogger();
		logger.info("*** Serialize ETGs ***");
		try {
			FileOutputStream fileOut = new FileOutputStream(
					settings.getSerializedETGsFile());
			ObjectOutputStream objOut = new ObjectOutputStream(fileOut);
			for (ExtendedTopologyGraph flowEtg : flowEtgs.values()) {
				objOut.writeObject(flowEtg);
			}
			objOut.close();
			fileOut.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Generate Cisco Virtual Internet Routing Lab (VIRL) file.
	 * @param settings
	 * @param rawConfigs raw device configurations
	 * @param deviceEtg device graph
	 */
	private static void generateVirl(Settings settings,
									 Map<String, String> rawConfigs, DeviceGraph deviceEtg) {
		Logger logger = settings.getLogger();
		logger.info("*** Generate VIRL ***");
		VirlConfigurationGenerator virlGenerator =
				new VirlConfigurationGenerator(logger);
		String virl = virlGenerator.toVirl(rawConfigs, deviceEtg);
		try {
			FileUtils.writeStringToFile(
					Paths.get(settings.getVirlFile()).toFile(), virl, false);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Generate graph files.
	 * @param settings
	 * @param baseEtg the base ETG
	 * @param topoEtg an ETG representing the layer-3 network topology
	 * @param flowEtgs the ETGs for each flow
	 */
	private static void generateGraphs(Settings settings,
									   ExtendedTopologyGraph baseEtg, InstanceGraph instanceEtg,
									   DeviceGraph topoEtg,
									   Map<Flow,? extends ExtendedTopologyGraph> flowEtgs) {
		Logger logger = settings.getLogger();

		logger.info("***Generate Graphs***");
		File graphFile;

		if (baseEtg != null) {
			graphFile = Paths.get(settings.getGraphsDirectory(),
					"base.gv").toFile();
			try {
				FileUtils.writeStringToFile(graphFile,
						baseEtg.toGraphviz(), false);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (instanceEtg != null) {
			graphFile = Paths.get(settings.getGraphsDirectory(),
					"instance.gv").toFile();
			try {
				FileUtils.writeStringToFile(graphFile,
						instanceEtg.toGraphviz(), false);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (topoEtg != null) {
			graphFile = Paths.get(settings.getGraphsDirectory(),
					"topo.gv").toFile();
			try {
				FileUtils.writeStringToFile(graphFile,
						topoEtg.toGraphviz(), false);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (flowEtgs != null) {
			for (ExtendedTopologyGraph flowEtg : flowEtgs.values()) {
				String flowSrcStartIp, flowSrcEndIp;
				if (flowEtg.getFlow().hasWildcardSource()) { // if a flow has no ACLs, route filters,...
					flowSrcStartIp = "_";
					flowSrcEndIp = "_";
				} else {
					flowSrcStartIp = flowEtg.getFlow().getSource().getStartIp().toString();
					flowSrcEndIp = flowEtg.getFlow().getSource().getEndIp().toString();
				}
				graphFile = Paths.get(settings.getGraphsDirectory(),
						String.format("%s-%s_%s-%s.gv",
						flowSrcStartIp,
						flowSrcEndIp,
						flowEtg.getFlow().getDestination().getStartIp(),
						flowEtg.getFlow().getDestination().getEndIp())
				).toFile();
				try {
					FileUtils.writeStringToFile(graphFile,
							flowEtg.toGraphviz(), false);
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Run verification tasks.
	 * @param settings settings
	 * @param flowEtgs the per-flow ETGs to use for verification
	 */
	private static void runVerificationTasks(Settings settings,
											 Map<Flow, ? extends ExtendedTopologyGraph> flowEtgs,
											 DeviceGraph deviceEtg) {

		// Verify currently blocked
		Map<Flow, Boolean> currentlyBlockedResults = null;
		if (settings.shouldVerifyCurrentlyBlocked()) {
			CurrentlyBlocked verifier = new CurrentlyBlocked(flowEtgs,settings);

			// Run verification
			long startTime = System.currentTimeMillis();
			currentlyBlockedResults = verifier.verifyAll(null);
			long endTime = System.currentTimeMillis();
			System.out.println("TIME: currentlyBlocked " + (endTime - startTime)
					+ " ms");

			// Output results
			if (!settings.shouldSummarizeVerificationResults()) {
				System.out.println("*** Currently Blocked ***");
				for (Entry<Flow, Boolean> result :
						currentlyBlockedResults.entrySet()) {
					System.out.println("\t" + result.getValue() + "\t"
							+ result.getKey());
				}
			}
		}

		// Verify equivalence
		if (settings.shouldVerifyEquivalence()) {
			Equivalent verifier = new Equivalent(flowEtgs, settings);

			// Deserialize the ETGs to compare against
			Map<Flow,ProcessGraph> comparisonEtgs =
					new LinkedHashMap<Flow,ProcessGraph>();
			try {
				FileInputStream fileIn = new FileInputStream(
						settings.getComparisonETGsFile());
				ObjectInputStream objIn = new ObjectInputStream(fileIn);
				try {
					while (true) {
						Object obj = objIn.readObject();
						if (obj instanceof ProcessGraph) {
							ProcessGraph processEtg = (ProcessGraph)obj;
							comparisonEtgs.put(processEtg.getFlow(), processEtg);
							//settings.getLogger().debug("Loaded process graph"
							//		+ " for flow " + processEtg.getFlow());
						}
						else {
							break;
						}
					}
				}
				catch (EOFException e) {

				}
				objIn.close();
				fileIn.close();
			} catch(IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

			// Run verification
			long startTime = System.currentTimeMillis();
			Map<Flow, Boolean> results = verifier.verifyAll(comparisonEtgs);
			long endTime = System.currentTimeMillis();
			System.out.println("TIME: equivalent " + (endTime - startTime)
					+ " ms");

			// Output results
			System.out.println("*** Equivalent ***");
			for (Entry<Flow, Boolean> result : results.entrySet()) {
				System.out.println("\t" + result.getValue() + "\t"
						+ result.getKey());
			}
		}

		// Prune ETGs
		if (settings.shouldPrune()) {
			long startTime = System.currentTimeMillis();
			for (ExtendedTopologyGraph flowEtg : flowEtgs.values()) {
				flowEtg.prune();
			}
			long endTime = System.currentTimeMillis();
			System.out.println("TIME: pruneETGs "+(endTime - startTime)+" ms");
		}

		// Verify always blocked
		if (settings.shouldVerifyAlwaysBlocked()) {
			AlwaysBlocked verifier = new AlwaysBlocked(flowEtgs, settings);

			// Run verification
			long startTime = System.currentTimeMillis();
			Map<Flow, Boolean> results = verifier.verifyAll(null);
			long endTime = System.currentTimeMillis();
			System.out.println("TIME: alwaysBlocked " + (endTime - startTime)
					+ " ms");

			// Output results
			System.out.println("*** Always Blocked ***");
			for (Entry<Flow, Boolean> result : results.entrySet()) {
				String exclaim = "";
				if (currentlyBlockedResults != null
						&& currentlyBlockedResults.get(result.getKey())
						&& !result.getValue()) {
					exclaim = "!";
				}
				System.out.println("\t" + result.getValue() + exclaim + "\t"
						+ result.getKey());
			}
		}

		// Verify always reachable
		if (settings.shouldVerifyAlwaysReachable()) {
			int maxFailuresExclusive =
					settings.getAlwaysReachableFailureCount();
			AlwaysReachable verifier = new AlwaysReachable(flowEtgs, settings);

			// Run verification
			long startTime = System.currentTimeMillis();
			Map<Flow, Boolean> results = verifier.verifyAll(
					maxFailuresExclusive);
			long endTime = System.currentTimeMillis();
			System.out.println("TIME: alwaysReachable " + (endTime - startTime)
					+ " ms");

			// Output results
			if (!settings.shouldSummarizeVerificationResults()) {
				System.out.println("*** Always Reachable (< "
						+ maxFailuresExclusive + " failures) ***");
				for (Entry<Flow, Boolean> result : results.entrySet()) {
					System.out.println("\t" + result.getValue() + "\t"
							+ result.getKey());
				}
			}
		}
		
		//maxFlowutil.testadd();
		//maxFlowutil.testdel();
		//DirectedEdgeFactory newedge = new DirectedEdgeFactory("20.0.0.0-20.0.0.255 1-65535 [TCP, UDP]");

		// Verify always isolated
		if (settings.shouldVerifyAlwaysIsolated()) {
			AlwaysIsolated verifier = new AlwaysIsolated(flowEtgs, settings);

			// Run verification
			long startTime = System.currentTimeMillis();
			Map<Flow,Map<Flow,Boolean>> results =
					new LinkedHashMap<Flow,Map<Flow,Boolean>>();
			for (Flow flow : flowEtgs.keySet()) {
				Map<Flow,Boolean> flowResults = verifier.verifyAll(flow);
				if (!settings.shouldSummarizeVerificationResults()) {
					results.put(flow, flowResults);
				}
			}
			long endTime = System.currentTimeMillis();
			System.out.println("TIME: alwaysIsolated " + (endTime - startTime)
					+ " ms");

			// Output results
			if (!settings.shouldSummarizeVerificationResults()) {
				System.out.println("*** Always Isolated ***");
				for (Entry<Flow,Map<Flow,Boolean>> flowResults :
						results.entrySet()) {
					for (Entry<Flow,Boolean> result :
							flowResults.getValue().entrySet()) {
						System.out.println("\t" + result.getValue() + "\t"
								+ flowResults.getKey() + " | "
								+ result.getKey());
					}
				}
			}
		}

		// Verify paths
		if (settings.shouldVerifyPaths()) {
			// Limit to internal flows, since we are not precise for flows with
			// an external source/destination
			Map<Flow, ExtendedTopologyGraph> internalFlowEtgs =
					new LinkedHashMap<Flow, ExtendedTopologyGraph>();
			for (Flow flow : flowEtgs.keySet()) {
				if (flow.getSource().isInternal()
						&& flow.getDestination().isInternal()) {
					internalFlowEtgs.put(flow, flowEtgs.get(flow));
				}
			}

			// Run verification
			ComputedPaths verifier = new ComputedPaths(flowEtgs, settings);
			long startTime = System.currentTimeMillis();
			VirlOutputParser virlOutputParser = new VirlOutputParser(
					settings.getFIBfile(), deviceEtg, settings.getLogger());
			Map<Scenario,Map<Flow,Boolean>> results =
					new LinkedHashMap<Scenario,Map<Flow,Boolean>>();
			for (Scenario scenario : virlOutputParser.parse()) {
				Map<Flow,Boolean> scenarioResults =
						verifier.verifyAll(scenario);
				results.put(scenario, scenarioResults);
			}
			long endTime = System.currentTimeMillis();
			System.out.println("TIME: computedPaths "+(endTime - startTime)
					+" ms");

			// Output results
			System.out.println("*** Computed Paths ***");
			boolean allMatch = true;
			for (Entry<Scenario,Map<Flow,Boolean>> scenarioResults :
					results.entrySet()) {
				for (Entry<Flow,Boolean> result :
						scenarioResults.getValue().entrySet()) {
					if (!settings.shouldSummarizeVerificationResults()) {
						System.out.println("\t" + result.getValue() + "\t"
								+ scenarioResults.getKey() + " | "
								+ result.getKey());
					}
					allMatch = (allMatch && result.getValue());
				}
			}
			if (allMatch) {
				System.out.println("All paths matched");
			} else {
				System.out.println("Some paths did not match");
			}
		}
	}
}
