package edu.wisc.cs.arc.configs;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
//import org.apache.commons.lang.exception.ExceptionUtils;
import org.batfish.job.ParseVendorConfigurationJob;
import org.batfish.job.ParseVendorConfigurationResult;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishLogger;
import org.batfish.main.Settings;
import org.batfish.main.Warnings;
import org.batfish.representation.Configuration;
import org.batfish.representation.VendorConfiguration;

import edu.wisc.cs.arc.GeneratorException;
import edu.wisc.cs.arc.Logger;

/**
 * Handles the parsing of configuration files.
 * THIS FILE CONTAINS CODE COPIED FROM BATFISH.
 * @author Aaron Gember-Jacobson
 *
 */
public class ConfigurationParser {
	private static final long JOB_POLLING_PERIOD_MS = 1000l;

	private Logger logger;
	private String path;
	private boolean parallelize;
	private Settings batfishSettings;
	private Map<String, VendorConfiguration> vendorConfigurations;
	private Map<String, Configuration> genericConfigurations;
	private Map<String, String> rawConfigurations;

	public ConfigurationParser(Logger logger, String path, boolean parallelize){
		this.logger = logger;
		this.path = path;
		this.parallelize = parallelize;
		try {
			this.batfishSettings = new Settings();
		}
		catch(ParseException e) {
			throw new GeneratorException("Failed to create Batfish settings", e);
		}
		this.batfishSettings.setLogger(new BatfishLogger(
				this.batfishSettings.getLogLevel(), 
				this.batfishSettings.getTimestamp()));
	}

	public Map<String, VendorConfiguration> parse() {
		try {
			Map<File, String> configurationData = this.readConfigurationFiles();
			this.vendorConfigurations = 
					this.parseVendorConfigurations(configurationData);
			//this.genericConfigurations = 
			//		convertConfigurations(vendorConfigurations);
			return this.vendorConfigurations;
		}
		catch (BatfishException e) {
			throw new GeneratorException("Failed to parse configs", e);
		}
	}
	
	public Map<String, VendorConfiguration> getVendorConfigurations() {
		return this.vendorConfigurations;
	}
	
	public Map<String, Configuration> getGenericConfigurations() {
		return this.genericConfigurations;
	}

	private Map<File, String> readConfigurationFiles() {
		logger.info("*** Reading configuration files ***");
		Map<File, String> configurationData = new TreeMap<File, String>();
		File configsPath = Paths.get(path).toFile();
		File[] configFilePaths = configsPath.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return !name.startsWith(".");
			}
		});
		if (configFilePaths == null) {
			throw new BatfishException("Error reading configs directory");
		}
		for (File file : configFilePaths) {
			logger.debug("Reading: " + file.toString());
			String fileText = readFile(file.getAbsoluteFile()) + "\n";
			configurationData.put(file, fileText);
		}
		logger.debug("Read "+configurationData.size()+ " configuration files");
		return configurationData;
	}

	private String readFile(File file) {
		String text = null;
		try {
			text = FileUtils.readFileToString(file);
		}
		catch (IOException e) {
			throw new BatfishException("Failed to read file: " + file.toString(),
					e);
		}
		return text;
	}

	private Map<String, VendorConfiguration> parseVendorConfigurations(
			Map<File, String> configurationData) {
		logger.info("*** Parsing vendor configuration files ***");

		ExecutorService pool;
		boolean shuffle;
		if (this.parallelize) {
	    	int numConcurrentThreads = Runtime.getRuntime().availableProcessors();
	    	pool = Executors.newFixedThreadPool(numConcurrentThreads);
	    	shuffle = true;
	    }
	    else {
	    	pool = Executors.newSingleThreadExecutor();
	    	shuffle = false;
	    }

	    Map<String, VendorConfiguration> vendorConfigurations = new TreeMap<String, VendorConfiguration>();
	    List<ParseVendorConfigurationJob> jobs = new ArrayList<ParseVendorConfigurationJob>();
	    this.rawConfigurations = new TreeMap<String, String>();

	    boolean processingError = false;
	    for (File currentFile : configurationData.keySet()) {
	    	Warnings warnings = new Warnings(true, true, true, true, 
	    			false, true, false);
	    	String fileText = configurationData.get(currentFile);
	    	ParseVendorConfigurationJob job = new ParseVendorConfigurationJob(
	    			batfishSettings, fileText, currentFile, warnings);
	    	jobs.add(job);
	    }
	    if (shuffle) {
	    	Collections.shuffle(jobs);
	    }
	    List<Future<ParseVendorConfigurationResult>> futures = new ArrayList<Future<ParseVendorConfigurationResult>>();
	    for (ParseVendorConfigurationJob job : jobs) {
	    	Future<ParseVendorConfigurationResult> future = pool.submit(job);
	    	futures.add(future);
	    }
	    // try {
	    // futures = pool.invokeAll(jobs);
	    // }
	    // catch (InterruptedException e) {
	    // throw new BatfishException("Error invoking parse jobs", e);
	    // }
	    while (!futures.isEmpty()) {
	    	List<Future<ParseVendorConfigurationResult>> currentFutures = new ArrayList<Future<ParseVendorConfigurationResult>>();
	    	currentFutures.addAll(futures);
	    	for (Future<ParseVendorConfigurationResult> future : currentFutures) {
	    		if (future.isDone()) {
	    			futures.remove(future);
	    			ParseVendorConfigurationResult result = null;
	    			try {
	    				result = future.get();
	    			}
	    			catch (InterruptedException | ExecutionException e) {
	    				throw new BatfishException("Error executing parse job", e);
	    			}
	    			String terseLogLevelPrefix ="";
	    			/*if (logger.isActive(BatfishLogger.LEVEL_INFO)) {
	    				terseLogLevelPrefix = "";
	    			}
	    			else {
	    				terseLogLevelPrefix = result.getFile().toString() + ": ";
	    			}*/
	    			this.batfishSettings.getLogger().append(result.getHistory(), terseLogLevelPrefix);
	    			Throwable failureCause = result.getFailureCause();
	    			if (failureCause != null) {
	    				if (batfishSettings.getExitOnFirstError()) {
	    					throw new BatfishException("Failed parse job",
	    							failureCause);
	    				}
	    				else {
	    					processingError = true;
	    					logger.error(ExceptionUtils.getStackTrace(failureCause));
	    				}
	    			}
	    			else {
	    				VendorConfiguration vc = result.getVendorConfiguration();
	    				if (vc != null) {
	    					String hostname = vc.getHostname().toLowerCase();
	    					if (vendorConfigurations.containsKey(hostname)) {
	    						throw new BatfishException("Duplicate hostname: "
	    								+ hostname);
	    					}
	    					else {
	    						vendorConfigurations.put(hostname, vc);
	    						this.rawConfigurations.put(hostname, 
	    								configurationData.get(result.getFile()));
	    					}
	    				}
	    			}
	    		}
	    		else {
	    			continue;
	    		}
	    	}
	    	if (!futures.isEmpty()) {
	    		try {
	    			Thread.sleep(JOB_POLLING_PERIOD_MS);
	    		}
	    		catch (InterruptedException e) {
	    			throw new BatfishException("interrupted while sleeping", e);
	    		}
	    	}
	    }
	    pool.shutdown();
	    if (processingError) {
	    	return null;
	    }
	    else {
	    	return vendorConfigurations;
	    }
	}

	private Map<String, Configuration> convertConfigurations(
			Map<String, VendorConfiguration> vendorConfigurations) {
		boolean processingError = false;
		Map<String, Configuration> configurations = 
				new TreeMap<String, Configuration>();
		logger.info(
			"*** Converting vendor configurations to independent format ***");
		for (String name : vendorConfigurations.keySet()) {
			logger.debug("Processing: \"" + name + "\"");
			VendorConfiguration vc = vendorConfigurations.get(name);
			Warnings warnings = new Warnings(true, true, true, true, true, true, 
					false);
			try {
				Configuration config = vc
						.toVendorIndependentConfiguration(warnings);
				configurations.put(name, config);
				logger.debug(" ...OK");
			}
			catch (BatfishException e) {
				logger.fatal("...CONVERSION ERROR");
				logger.fatal(ExceptionUtils.getStackTrace(e));
				processingError = true;
				//if (_settings.getExitOnFirstError()) {
					break;
				/*}
				else {
					continue;
				}*/
			}
			finally {
				for (String warning : warnings.getRedFlagWarnings()) {
					logger.warn(warning);
				}
				for (String warning : warnings.getUnimplementedWarnings()) {
					logger.warn(warning);
				}
				for (String warning : warnings.getPedanticWarnings()) {
					logger.info(warning);
				}
			}
		}
		if (processingError) {
			throw new BatfishException("Vendor conversion error(s)");
		}
		else {
			return configurations;
		}
	}
	
	public Map<String, String> getRawConfigurations() {
		return this.rawConfigurations;
	}
}
