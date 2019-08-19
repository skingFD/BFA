package edu.wisc.cs.arc;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Stores and parses settings for the ETG generator/verifier.
 * @author Aaron Gember-Jacobson (agember@cs.wisc.edu)
 */
public class Settings {
	private final static String EXECUTABLE_NAME = "etggenerator";
	
	public final static String HELP = "help";
	
	private final static String CONFIGS_DIRECTORY = "configs";
	
	private final static String ANONYMIZE = "anon";
	private final static String IGNORED_POLICY_GROUP_SIZE = "minhosts";
	private final static String PARALLELIZE = "parallelize";
	private final static String GENERATE_GRAPHS = "graphs";
	private final static String SERIALIZE_ETGS = "serialize";
	private final static String WARN_ASSUMPTIONS = "warn";
	private final static String ENTIRE_FLOWSPACE = "allflows";
	private final static String INTERNAL_ONLY = "internalonly";
	private final static String NO_PRUNING = "noprune";
	private final static String BASE_ONLY = "baseonly";
	//private final static String PHYSICAL_ONLY = "physicalonly";
	private final static String USE_DESCRIPTIONS = "descriptions";
	private final static String INTERFACE_BASED = "interface";
	private final static String GENERATE_VIRL = "virl";
	private final static String ROUTERS_ONLY = "routersonly";
	private final static String SUMMARIZE = "summarize";
	
	private final static String VERIFY_ALL = "vall";
	private final static String VERIFY_CURRENTLY_BLOCKED = "vcb";
	private final static String VERIFY_ALWAYS_BLOCKED = "vab";
	private final static String VERIFY_ALWAYS_REACHABLE = "var";
	private final static String VERIFY_ALWAYS_ISOLATED = "vai";
	private final static String VERIFY_PATHS = "vpaths";
	private final static String VERIFY_EQUIVALENCE = "veq";
	private final static String DETAILED_TIMING = "t";
	
	/** Where are the config files store? */
	private String configsDirectory;

	/** Should the output be anonymized? */
	private boolean anonymize;
	
	/** What is the minimum size policy group? */
	private int minPolicyGroupsSize;
	
	/** Should ETGs be generated and verified in parallel? */
	private boolean parallelize;
	
	/** Where should graph files be stored? */
	private String graphsDirectory;
	
	/** Where should serialized ETGs be stored? */
	private String serializedETGsFile;
	
	/** Should violations of assumptions during ETG generation just trigger
	 * warnings? */
	private boolean warnAssumptions;
	
	/** Should the entire flowspace be added as a policy group? */
	private boolean entireFlowspace;
	
	/** Should only internal policy groups be considered? */
	private boolean internalOnly;
	
	/** Should ETGs be pruned? */
	private boolean prune;
	
	/** Should only the base ETG be generated? */
	private boolean baseOnly;
	
	/** Should only the physical ETG be generated? */
	//private boolean physicalOnly;
	
	/** Should the device-based ETG be constructed based on interface
	 * descriptions in device configurations? */
	private boolean useDescriptions;
	
	/** Should the process-based ETGs be converted to interface-based ETGs? */
	private boolean interfaceBased;
	
	/** Where should a Cisco Virtual Internet Routing Lab (VIRL) file be 
	 * stored? */
	private String virlFile;
	
	/** Should only devices with routing stanzas be considered? */
	private boolean routersOnly;
	
	/** Should the verification results be summarized? */
	private boolean summarize;
		
	/** Should the currently blocked verifier be run? */
	private boolean verifyCurrentlyBlocked;
	
	/** Should the always blocked verifier be run? */
	private boolean verifyAlwaysBlocked;
	
	/** Should the always reachable verifier be run? */
	private int verifyAlwaysReachable;
	
	/** Should the always isolated verifier be run? */
	private boolean verifyAlwaysIsolated;
	
	/** Should the paths verifier be run? */
	private String verifyPaths;
	
	/** Should the equivalence verifier be run? */
	private String verifyEquivalence;
	
	/** Should per-flow timing information be output during verification? */
	private boolean perflowVerifcationTimes;
	
	/** Logger */
	private Logger logger;
	
	/**
	 * Obtain settings from command line arguments.
	 * @param args command line arguments
	 * @throws ParseException
	 */
	public Settings(String[] args, Logger logger) throws ParseException {
		this.logger = logger;
		
		Options options = this.getOptions();
		
		// Output help text, if help argument is passed
		for (String arg : args) {
			if (arg.equals("-"+HELP)) {
		        HelpFormatter formatter = new HelpFormatter();
		        formatter.setLongOptPrefix("-");
		        formatter.printHelp(EXECUTABLE_NAME, options, true);
				System.exit(0);
			}
		}		
		
		// Parse command line arguments
		CommandLineParser parser = new DefaultParser();
		CommandLine line = null;
		try {
			line = parser.parse(options, args);
		}
		catch(MissingOptionException e) {
			throw new ParseException("Missing required argument: -" 
					+ e.getMissingOptions().get(0));
		}
		catch(MissingArgumentException e) {
			throw new ParseException("Missing argument for option " 
					+ e.getOption().getLongOpt());
		}
		
		// Store settings
		this.configsDirectory = line.getOptionValue(CONFIGS_DIRECTORY);
		this.anonymize = line.hasOption(ANONYMIZE);
		try {
			this.minPolicyGroupsSize = Integer.parseInt(
					line.getOptionValue(IGNORED_POLICY_GROUP_SIZE,"0"));
		} catch(NumberFormatException e) {
			throw new ParseException(
					"Ignored policy group size is not a number");
		}
		this.parallelize = line.hasOption(PARALLELIZE);
		this.graphsDirectory = line.getOptionValue(GENERATE_GRAPHS);
		this.serializedETGsFile = line.getOptionValue(SERIALIZE_ETGS);
		this.warnAssumptions = line.hasOption(WARN_ASSUMPTIONS);
		this.entireFlowspace = line.hasOption(ENTIRE_FLOWSPACE);
		this.internalOnly = line.hasOption(INTERNAL_ONLY);
		this.prune = !line.hasOption(NO_PRUNING);
		this.baseOnly = line.hasOption(BASE_ONLY);
		//this.physicalOnly = line.hasOption(PHYSICAL_ONLY);
		this.useDescriptions = line.hasOption(USE_DESCRIPTIONS);
		this.interfaceBased = line.hasOption(INTERFACE_BASED);
		this.virlFile = line.getOptionValue(GENERATE_VIRL);
		this.routersOnly = line.hasOption(ROUTERS_ONLY);
		this.summarize = line.hasOption(SUMMARIZE);
		
		if(line.hasOption(VERIFY_ALL)) {
			this.verifyCurrentlyBlocked = true;
			this.verifyAlwaysBlocked = true;
			this.verifyAlwaysReachable = 1;
			this.verifyAlwaysIsolated = true;
		}
		else {
			this.verifyCurrentlyBlocked = 
					line.hasOption(VERIFY_CURRENTLY_BLOCKED);
			this.verifyAlwaysBlocked = line.hasOption(VERIFY_ALWAYS_BLOCKED);
			try {
				this.verifyAlwaysReachable = Integer.parseInt(
						line.getOptionValue(VERIFY_ALWAYS_REACHABLE, "-1"));
			} catch(NumberFormatException e) {
				throw new ParseException(
						"Failure bound is not a number");
			}
			this.verifyAlwaysIsolated = line.hasOption(VERIFY_ALWAYS_ISOLATED);
			this.verifyPaths = line.getOptionValue(VERIFY_PATHS);
			this.verifyEquivalence = line.getOptionValue(VERIFY_EQUIVALENCE);
		}
		this.perflowVerifcationTimes = line.hasOption(DETAILED_TIMING);
	}
	
	/**
	 * Set up the list of command line arguments the program accepts.
	 * @return arguments the program accepts
	 */
	private Options getOptions() {
		Options options = new Options();
		options.addOption(HELP, false,
				"Print usage information");
		
		Option option = new Option(CONFIGS_DIRECTORY, true,
				"Directory containing configuration files");
		option.setRequired(true);
		option.setArgName("DIR");
		options.addOption(option);

		options.addOption(ANONYMIZE, false,
				"Anonymize output");
		
		option = new Option(IGNORED_POLICY_GROUP_SIZE, true,
				"Ignore policy groups which have no more than SIZE hosts");
		option.setArgName("SIZE");
		options.addOption(option);
		
		options.addOption(PARALLELIZE, false,
				"Generate and verify ETGs in parallel");
		
		option = new Option(GENERATE_GRAPHS, true,
				"Generate graph files in DIR");
		option.setArgName("DIR");
		options.addOption(option);
		
		option = new Option(SERIALIZE_ETGS, true, 
				"Serialize ETGs to FILE");
		option.setArgName("FILE");
		options.addOption(option);
		
		options.addOption(WARN_ASSUMPTIONS, false,
				"Output a warning when an assumption is violated during ETG "
				+ "construction, rather than ending the process with an error");
		
		options.addOption(ENTIRE_FLOWSPACE, false,
				"Include the entire flowspace as a policy group");
		
		options.addOption(INTERNAL_ONLY, false,
				"Only include internal policy groups");
		
		options.addOption(NO_PRUNING, false,
				"Skip pruning of ETGs");
		
		options.addOption(BASE_ONLY, false,
				"Only generate base ETG");
		
		/*options.addOption(PHYSICAL_ONLY, false,
				"Only generate physical topology ETG");*/
		
		options.addOption(USE_DESCRIPTIONS, false,
				"Construct the device-based ETG based on interface descriptions"
				+ " in device configurations");
		
		options.addOption(INTERFACE_BASED, false,
				"Convert process-based ETGs to interface-based ETGs");
		
		option = new Option(GENERATE_VIRL, true, 
				"Generate a Cisco Virtual Internet Routing Lab (VIRL) in FILE");
		option.setArgName("FILE");
		options.addOption(option);
		
		options.addOption(ROUTERS_ONLY, false,
				"Only include devices that containing router stanzas");
		
		options.addOption(SUMMARIZE, false,
				"Only output a summary of verification results");
		
		options.addOption(VERIFY_ALL, false,
				"Run all verifiers");
		
		options.addOption(VERIFY_CURRENTLY_BLOCKED, false,
				"Verify currently blocked");
		
		options.addOption(VERIFY_ALWAYS_BLOCKED, false,
				"Verify always blocked");
		
		option = new Option(VERIFY_ALWAYS_REACHABLE, true,
				"Verify always reachable under less than K failures");
		option.setArgName("K");
		options.addOption(option);
		
		options.addOption(VERIFY_ALWAYS_ISOLATED, false,
				"Verify always isolated");
		
		option = new Option(VERIFY_PATHS, true, 
				"Verify paths computed using ETGs are equivalent to paths"
				+ " computed using output from Parse Cisco Virtual Internet"
				+ " Routing Lab (VIRL) stored in FILE");
		option.setArgName("FILE");
		options.addOption(option);
		
		option = new Option(VERIFY_EQUIVALENCE, true, 
				"Verify ETGs for the specified configurations are equivalent"
				+ " to the serialized ETGs stored in FILE");
		option.setArgName("FILE");
		options.addOption(option);
		
		options.addOption(DETAILED_TIMING, false,
				"Output per-flow (pair) verification times information");
		return options;
	}
	
	/**
	 * Determine where configuration files are stored.
	 * @return the path to a directory containing the configuration files
	 */
	public String getConfigsDirection() {
		return this.configsDirectory;
	}

	/**
	 * Determine if output should be anonymized.
	 * @return true the output should be anonymized, otherwise false
	 */
	public boolean shouldAnonymize() {
		return this.anonymize;
	}
	
	/**
	 * Determine if policy groups should be ignored based on the number of
	 * hosts they contain.
	 * @return true if some policy groups should be ignored, otherwise false 
	 */
	public boolean shouldIgnoreSmallPolicyGroups() {
		return (this.minPolicyGroupsSize <= 0);
	}
	
	/**
	 * Determine the minimum number of hosts a policy group must contain to be
	 * of interest.
	 * @return the minimum size of policy groups of interest
	 */
	public int getMinPolicyGroupsSize() {
		return this.minPolicyGroupsSize;
	}
	
	/**
	 * Determine if ETGs should be generated/verified in parallel.
	 * @return true these actions should happen in parallel, otherwise false
	 */
	public boolean shouldParallelize() {
		return this.parallelize;
	}
	
	/**
	 * Determine if graph files should be generated.
	 * @return true if graph files should be generated, otherwise false
	 */
	public boolean shouldGenerateGraphs() {
		return (this.graphsDirectory != null);
	}
	
	/**
	 * Determine where graph files should be stored.
	 * @return the path to a directory where graph files should be stored, or 
	 * 		null if they should not be generated
	 */
	public String getGraphsDirectory() {
		return this.graphsDirectory;
	}
	
	/**
	 * Determine if ETGs should be serialized and stored.
	 * @return true if ETGs should be serialized, otherwise false
	 */
	public boolean shouldSerializeETGs() {
		return (this.serializedETGsFile != null);
	}

	/**
	 * Determine where serialized ETGs should be stored.
	 * @return the path to a file where serialized ETGs should be stored, or 
	 * 		null if they should not be serialized
	 */
	public String getSerializedETGsFile() {
		return this.serializedETGsFile;
	}
	
	/**
	 * Determine whether violations of assumptions during ETG generation should
	 * just trigger a warning instead of ending the process with an error.
	 * @return true if assumption violations should only result in warnings,
	 * 		otherwise false
	 */
	public boolean shouldWarnAssumptions() {
		return this.warnAssumptions;
	}
	
	/**
	 * Determine whether the entire flowspace should be added as a policy group.
	 * @return true if the entire flowspace should be added
	 */
	public boolean shouldIncludeEntireFlowspace() {
		return this.entireFlowspace;
	}
	
	/**
	 * Determine whether external policy groups should be ignored.
	 * @return true if external policy groups should be ignored; otherwise false
	 */
	public boolean shouldExcludeExternalFlows() {
		return this.internalOnly;
	}
	
	/**
	 * Determine whether ETGs should be pruned.
	 * @return true if ETGs should be pruned, otherwise false
	 */
	public boolean shouldPrune() {
		return this.prune;
	}
	
	/**
	 * Determine whether to generate physical ETG.
	 * @return true if only physical ETG should be generated, otherwise false
	 */
	/*public boolean shouldGeneratePhysicalETGOnly() {
		return this.physicalOnly;
	}*/
	
	/**
	 * Determine whether to generate the device-based ETG based on interface 
	 * descriptions in device configurations
	 * @return true if interface descriptions should be used, false if the
	 * 		device-based ETG should be generated based on the process-based ETG
	 */
	public boolean shouldUseInterfaceDescriptions() {
		return this.useDescriptions;
	}
	
	/**
	 * Determine whether to generate per-flow ETGs.
	 * @return true if per-flow ETGs should be generated, otherwise false
	 */
	public boolean shouldGenerateFlowETGs() {
		return !this.baseOnly; // || !this.physicalOnly;
	}
	
	/**
	 * Determine whether to convert process-based ETGs to interface-based ETGs.
	 * @return true if process-based ETGs should be converted, otherwise false
	 */
	public boolean shouldGenerateInterfaceETG() {
		return this.interfaceBased;
	}
	
	/**
	 * Determine whether a Cisco Virtual Internet Routing Lab (VIRL) file should
	 * be generated.
	 * @return true if a VIRL file should be generated, otherwise false
	 */
	public boolean shouldGenerateVirl() {
		return (this.virlFile != null);
	}
	
	/**
	 * Determine where a Cisco Virtual Internet Routing Lab (VIRL) file should
	 * be generated.
	 * @return the path to a file where a VIRL file should be stored, or 
	 * 		null if no VIRL file should be generated
	 */
	public String getVirlFile() {
		return this.virlFile;
	}
	
	/**
	 * Determine whether to exclude devices that do not containing routing
	 * stanzas.
	 * @return true if devices without routing stanzas should be ignored, 
	 * 		otherwise false
	 */
	public boolean shouldExcludeNonRouters() {
		return this.routersOnly;
	}
	
	/**
	 * Determine whether to only output a summary of verification results
	 * rather than the results for every (pair of) traffic class(es).
	 * @return true if only a summary should be output, otherwise false
	 */
	public boolean shouldSummarizeVerificationResults() {
		return this.summarize;
	}
	
	/**
	 * Determine whether the currently blocked verifier should be run.
	 * @return true if the verifier should be run, otherwise false
	 */
	public boolean shouldVerifyCurrentlyBlocked() {
		return this.verifyCurrentlyBlocked;
	}
	
	/**
	 * Determine whether the always blocked verifier should be run.
	 * @return true if the verifier should be run, otherwise false
	 */
	public boolean shouldVerifyAlwaysBlocked() {
		return this.verifyAlwaysBlocked;
	}
	
	/**
	 * Determine whether the always reachable verifier should be run.
	 * @return true if the verifier should be run, otherwise false
	 */
	public boolean shouldVerifyAlwaysReachable() {
		return (this.verifyAlwaysReachable >= 0);
	}
	
	/**
	 * Determine the maximum number of failures allowed for verifying always 
	 * reachable.
	 * @return the maximum number of failures to tolerate
	 */
	public int getAlwaysReachableFailureCount() {
		return this.verifyAlwaysReachable;
	}
	
	/**
	 * Determine whether the always isolated verifier should be run.
	 * @return true if the verifier should be run, otherwise false
	 */
	public boolean shouldVerifyAlwaysIsolated() {
		return this.verifyAlwaysIsolated;
	}
	
	/**
	 * Determine whether the paths verifier should be run.
	 * @return true if the verifier should be run, otherwise false
	 */
	public boolean shouldVerifyPaths() {
		return (this.verifyPaths != null);
	}
	
	/**
	 * Determine the location of a Cisco Virtual Internet Routing Lab (VIRL)
	 * output file used for verifying paths. 
	 * @return the filepath to a VIRL output file, or null if paths should not
	 * 		be verified
	 */
	public String getFIBfile() {
		return this.verifyPaths;
	}
	
	/**
	 * Determine whether the equivalence verifier should be run.
	 * @return true if the verifier should be run, otherwise false
	 */
	public boolean shouldVerifyEquivalence() {
		return (this.verifyEquivalence != null);
	}
	
	/**
	 * Determine the location of a file containing serialized ETGs to compare
	 * against for equivalence checking. 
	 * @return the path to a file of serialized ETGs, or null if equivalence 
	 * 		should not be checked
	 */
	public String getComparisonETGsFile() {
		return this.verifyEquivalence;
	}
	
	/**
	 * Determine if per-flow (pair) verification times should be output.
	 * @return true if detailed times should be output, otherwise false
	 */
	public boolean shouldOutputPerflowVerifcationTimes() {
		return this.perflowVerifcationTimes;
	}
	
	/**
	 * Get the logger.
	 * @return logger for producing output
	 */
	public Logger getLogger() {
		return this.logger;
	}
	
	public String toString() {
		String result = "";
		result += "Configs directory: " + this.configsDirectory;
		result += "\nWarn assumptions: " + this.warnAssumptions;
		result += "\nRouters Only: " + this.routersOnly;
		result += "\nMinimum policy group size: " + this.minPolicyGroupsSize;
		result += "\nInclude entire flowspace: " + this.entireFlowspace;
		result += "\nGenerate per-flow ETGs: " + !this.baseOnly;
		result += "\nUse interface descriptions to generate device-based ETG: "
				+ this.useDescriptions;
		result += "\nConvert process-based ETG to interface-based ETG: "
				+ this.interfaceBased;
		result += "\nParallelize: " + this.parallelize;
		result += "\nPrune: " + this.prune;
		result += "\nGraphs directory: " + this.graphsDirectory;
		result += "\nSerialized ETGs file: " + this.serializedETGsFile;
		result += "\nVerify currently blocked: " + this.verifyCurrentlyBlocked;
		result += "\nVerify always blocked: " + this.verifyAlwaysBlocked;
		result += "\nVerify always reachable: "
				+ this.shouldVerifyAlwaysReachable() + " K="
				+ this.verifyAlwaysReachable;
		result += "\nVerify always isolated: " + this.verifyAlwaysIsolated;
		result += "\nVerify paths: "+this.shouldVerifyPaths() + " VIRL log=" 
				+ this.verifyPaths;
		result += "\nVerify equivalence: "+this.shouldVerifyEquivalence()
				+ " Comparison ETGs=" + this.getComparisonETGsFile();
		return result;
	}
}
