package edu.wisc.cs.arc.virl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import edu.wisc.cs.arc.Logger;
import edu.wisc.cs.arc.graphs.DeviceGraph;

/**
 * Handles the parsing of output from Cisco Virtual Internet Routing Lab (VIRL).
 * @author Aaron Gember-Jacobson (agember@cs.wisc.edu)
 */
public class VirlOutputParser {
	/** Logger */
	private Logger logger;
	
	/** File containing the output from VIRL */
	private File outputFile;
	
	/** Device-based extended topology graph */
	private DeviceGraph deviceEtg;
	
	/**
	 * Creates a new VIRL output parser.
	 * @param path location of the output file
	 */
	public VirlOutputParser(String path, DeviceGraph deviceEtg, Logger logger) {
		this.logger = logger;
		this.outputFile = Paths.get(path).toFile();
		this.deviceEtg = deviceEtg;
	}
	
	/**
	 * Parse VIRL output.
	 * @return a list of scenarios contained in the output
	 */
	public List<Scenario> parse() {
		List<Scenario> scenarios = new ArrayList<Scenario>();
		List<String> outputLines;
		try {
			outputLines = FileUtils.readLines(this.outputFile);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		List<String> scenarioLines = new ArrayList<String>();
		for (String line : outputLines) {
			if (line.contains("START SCENARIO")) {
				scenarioLines = new ArrayList<String>();
			}
			else if (line.contains("END SCENARIO")) {
				Scenario scenario = this.parseScenario(scenarioLines);
				scenarios.add(scenario);
			}
			else {
				scenarioLines.add(line);
			}
		}
		
		return scenarios;
	}
	
	/**
	 * Parse a scenario contained in VIRL output
	 * @param scenarioLines output associated with the scenario
	 * @return the scenario
	 */
	private Scenario parseScenario(List<String> scenarioLines) {
		Scenario scenario = new Scenario(this.deviceEtg, this.logger);
		List<String> fibLines = new ArrayList<String>();
		String node = null;
		for (String line : scenarioLines) {
			// Failed link
			if (line.startsWith("DOWN")) {
				int startIfaceNode = line.indexOf(' ') + 1;
				int endIfaceNode = line.indexOf(':', startIfaceNode);
				String ifaceNode = line.substring(startIfaceNode, endIfaceNode);
				
				int startLink = line.indexOf('(') + 1;
				int endLink = line.indexOf(')', startLink);
				String link = line.substring(startLink, endLink);
				
				int startSrc = 0;
				int endDst = link.length();
				int endSrc = ifaceNode.length();
				if (link.endsWith(ifaceNode)) {
					endSrc = link.length() - 1 - ifaceNode.length();
				}
				int startDst = endSrc + 1;
				
				String srcNode = link.substring(startSrc, endSrc);
				String dstNode = link.substring(startDst, endDst);
				logger.debug("Failed link: " + srcNode + " -- " + dstNode);
				
				/*int startSrc = line.indexOf('(') + 1;
				int endSrc = line.indexOf(' ', startSrc);
				int startDst = endSrc + 1;
				int endDst = line.indexOf(')', startDst);
				
				String srcNode = line.substring(startSrc, endSrc);
				String dstNode = line.substring(startDst, endDst);*/

				scenario.addFailedLink(new Link(srcNode, dstNode));
			}
			// Start FIB
			else if (line.startsWith("FIB")) {
				node = line.substring(4);
				fibLines = new ArrayList<String>();
			}
			// End FIB
			else if (node != null && line.endsWith(node+"#")) {
				FIB fib = new FIB(fibLines);
				logger.debug(node + "\n" + fib.toString());
				scenario.addFIB(node, fib);
				node = null;
			}
			// FIB line
			else {
				fibLines.add(line);
			}
		}
		return scenario;
	}
}
