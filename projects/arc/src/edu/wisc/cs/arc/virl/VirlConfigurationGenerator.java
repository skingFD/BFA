package edu.wisc.cs.arc.virl;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

import edu.wisc.cs.arc.GeneratorException;
import edu.wisc.cs.arc.Logger;
import edu.wisc.cs.arc.graphs.Device;
import edu.wisc.cs.arc.graphs.DeviceGraph;
import edu.wisc.cs.arc.graphs.DeviceVertex;
import edu.wisc.cs.arc.graphs.DirectedEdge;
import edu.wisc.cs.arc.graphs.Interface;
import edu.wisc.cs.arc.graphs.Interface.InterfaceType;

/**
 * Methods to generate a Cisco Virtual Internet Routing Lab (VIRL) file.
 * @author Aaron Gember-Jacobson
 */
public class VirlConfigurationGenerator {
	
	/** Logger for VIRL generation */
	private Logger logger;
	
	public VirlConfigurationGenerator(Logger logger) {
		this.logger = logger;
	}
	
	public String toVirl(Map<String, String> rawConfigs,
			DeviceGraph deviceEtg) {
		String virl = "<?xml version=\"1.0\" encoding=\"UTF-8\""
				+ " standalone=\"yes\"?>\n"
				+ "<topology xmlns=\"http://www.cisco.com/VIRL\""
				+ " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
				+ " schemaVersion=\"0.9\" simulationEngine=\"OPENSTACK\""
				+ " xsi:schemaLocation=\"http://www.cisco.com/VIRL"
				+ " https://raw.github.com/CiscoVIRL/schema/v0.9/virl.xsd\">\n";
		
		Map<Interface,String> interfaceIds = 
				new LinkedHashMap<Interface,String>();
		
		// Determine which interfaces are used
		Set<Interface> usedInterfaces = new LinkedHashSet<Interface>();
		Iterator<DirectedEdge<DeviceVertex>> edgeIterator = 
				deviceEtg.getEdgesIterator();
		while (edgeIterator.hasNext()) {
			DirectedEdge<DeviceVertex> edge = edgeIterator.next();
			usedInterfaces.add(edge.getSourceInterface());
			usedInterfaces.add(edge.getDestinationInterface());
		}
			
		// Add nodes
		int nodeCount = 0;
		for (Entry<String,String> entry : rawConfigs.entrySet()) {
			nodeCount++;
			String deviceName = entry.getKey();
			String config = entry.getValue();
			logger.debug("Adding node " + deviceName + " to VIRL");
			
			// Attempt to determine deviceType
			String deviceType = "IOSv";
			Pattern pattern = null;
			Matcher matcher = null;
			pattern = Pattern.compile(
					"!DeviceType: (?<type>[^\\n]+)\\n");
			matcher = pattern.matcher(config);
			if (matcher.find()) {
				if (matcher.group("type").equals("NEXUS")) {
					deviceType = "NX-OSv";
				}
			}
			
			virl += "\t<node name=\"" + deviceName 
					+ "\" type=\"SIMPLE\" subtype=\"" + deviceType + "\">\n";
			
			// Clean configuration for simulation
			config = this.cleanConfig(config);
						
			// Add node interfaces
			String interfacesXml = "";
			Device device = deviceEtg.getDevices().get(deviceName);
			int ifaceCount = 0;
			int overlapRenum = 100;
			for (Interface iface : device.getInterfaces()) {
				// Ignore non-Ethernet interfaces
				if (iface.getType() != InterfaceType.ETHERNET) {
					continue;
				}
				
				// Ignore inactive interfaces
				if (!iface.getActive()) {
					config = this.removeInterfaceFromConfig(config, iface);
					//logger.debug("\tSkip inactive interface "+ iface.getName());
					continue;
				}
				// Ignore unused interfaces
				if (!usedInterfaces.contains(iface) 
						&& null == iface.getPrefix()) {
					config = this.removeInterfaceFromConfig(config, iface);
					//logger.debug("\tSkip unused interface " + iface.getName());
					continue;
				}
				ifaceCount++;
				
				// Interface name must conform to VIRL limitations
				String rename = "GigabitEthernet0/" + ifaceCount;
				if (deviceType.equals("NX-OSv")) {
					rename = "Ethernet2/" + ifaceCount;
				}
				
				// Determine if another interface already has the new name
				Interface overlapIface = device.getInterface(rename);
				if (overlapIface != null && overlapIface != iface) {
					// Find new name for overlapping interface
					Interface renumOverlapIface = null;
					String overlapRename = null;
					do {
						overlapRenum++;
						overlapRename = "GigabitEthernet1/" + overlapRenum;
						renumOverlapIface = device.getInterface(overlapRename);
					} while (renumOverlapIface != null);
					
					// Rename overlapping interface
					config = this.renameInterfaceInConfig(config, overlapIface,
							overlapRename);
					overlapIface.setName(overlapRename);
				}
				
				//logger.debug("\tAdd interface " + iface.getName()
				//	+ " with name " + rename);
				// Rename the interface
				config = this.renameInterfaceInConfig(config, iface, rename);
				iface.setName(rename);
				
				interfacesXml += "\t\t<interface id=\"" + (ifaceCount-1) 
						+ "\" name=\"" + iface.getName() + "\"/>\n";
				interfaceIds.put(iface, getInterfaceId(nodeCount, ifaceCount));
			}
			
			config = this.cleanWhitespace(config);
			
			// Add node configuration
			virl += "\t\t<extensions>\n";
			virl += "\t\t<entry key =\"config\" type=\"String\">" + config 
					+ "</entry>\n";
			virl += "\t\t</extensions>\n";
			
			virl += interfacesXml;
			
			virl += "\t</node>\n";
		}
		
		// Add connections
		edgeIterator = deviceEtg.getEdgesIterator();
		while (edgeIterator.hasNext()) {
			DirectedEdge<DeviceVertex> edge = edgeIterator.next();
			String dst = interfaceIds.get(edge.getDestinationInterface());
			String src = interfaceIds.get(edge.getSourceInterface());
			if ((null == dst) || (null == src)) {
				throw new GeneratorException("Could not determine interface ids"
						+ " for edge " + edge);
			}
			virl += "\t<connection dst=\"" + dst + "\" src=\"" + src + "\"/>\n";
		}
		
		virl += "</topology>";
		return virl;
	}
	
	private String getInterfaceId(int nodeNum, int interfaceNum) {
		return "/virl:topology/virl:node[" + nodeNum + "]/virl:interface[" 
				+ interfaceNum + "]";
	}
	
	private String removeInterfaceFromConfig(String rawConfig, 
			Interface iface) {		
		//logger.debug("\tRemove Iface=" + iface.getName());
		Pattern pattern = null;
		Matcher matcher = null;
		pattern = Pattern.compile("interface "+iface.getName()+"\\n"
				+ "( +[^\\n]+\\n)*");
		matcher = pattern.matcher(rawConfig);
		/*if (matcher.find()) {
			logger.debug(rawConfig.substring(matcher.start(), matcher.end()));
		}*/
		return matcher.replaceAll("");
	}
	
	private String renameInterfaceInConfig(String rawConfig, 
			Interface iface, String rename) {
		//logger.debug("\tRename Iface=" + iface.getName() + " Rename=" 
		//		+ rename);
		rawConfig = rawConfig.replace(" "+iface.getName()+" ", " "+rename+" ");
		return rawConfig.replace(" "+iface.getName()+"\n", " "+rename+"\n");
	}
	
	private String cleanConfig(String rawConfig) {
		//logger.debug("\tClean config");
		
		// Remove banner command with unprintable characters from config
		while (rawConfig.indexOf(3) >= 0) {
			int startIndex = rawConfig.indexOf(3);
			int endIndex = rawConfig.indexOf(3, startIndex+1);
            if (endIndex < 0) {
                endIndex = rawConfig.indexOf("^C", startIndex+1);
            }
			startIndex = rawConfig.lastIndexOf('\n', startIndex);
			rawConfig = rawConfig.replace(
					rawConfig.substring(startIndex, endIndex+1), "");
		}

        // Remove ip ssh break-string command with unprintable characters from 
        // config
		while (rawConfig.indexOf(2) >= 0) {
			int endIndex = rawConfig.indexOf(2);
			int startIndex = rawConfig.lastIndexOf('\n', endIndex);
			rawConfig = rawConfig.replace(
					rawConfig.substring(startIndex, endIndex+1), "");
		}
		
		// Remove ampersands from config
		rawConfig = rawConfig.replace("&", "and");
		
		// Remove console and VTY settings
		Pattern pattern = null;
		Matcher matcher = null;
		pattern = Pattern.compile("line ((con)|(vty))[^\\n]*\\n"
				+ "(\\s+[^\\n]+\\n)*");
		matcher = pattern.matcher(rawConfig);
		rawConfig = matcher.replaceAll("");
		/*while (matcher.find()) {
			logger.debug("\t\tRemove Line: " + rawConfig.substring(
					matcher.start(), matcher.end()));
		}*/
		
		// Remove enable password
		pattern = Pattern.compile("enable secret [^\\n]+\\n");
		matcher = pattern.matcher(rawConfig);
		rawConfig = matcher.replaceAll("");
		
		// Fix admin user password
		pattern = Pattern.compile("username admin [^\\n]+\\n");
		matcher = pattern.matcher(rawConfig);
		rawConfig = matcher.replaceAll("username admin password ETGcompar3 role network-admin\n");
		
		// Remove authentication, authorization, and accounting (AAA) settings
		pattern = Pattern.compile("aaa[^\\n]+\\n"
				+ "(\\s+[^\\n]+\\n)*");
		matcher = pattern.matcher(rawConfig);
		rawConfig = matcher.replaceAll("");
		
		// Remove boot settings
		pattern = Pattern.compile("(boot[^\\n]+\\n)*");
		matcher = pattern.matcher(rawConfig);
		rawConfig = matcher.replaceAll("");
		
		// Remove TACACS settings
		pattern = Pattern.compile("tacacs-server [^\\n]+\\n");
		matcher = pattern.matcher(rawConfig);
		rawConfig = matcher.replaceAll("");
		
		// Remove role settings
		pattern = Pattern.compile("role name [^\\n]+\\n"
				+ "(\\s+[^\\n]+\\n)*");
		matcher = pattern.matcher(rawConfig);
		rawConfig = matcher.replaceAll("");
		
		// Remove SNMP settings
		pattern = Pattern.compile("snmp-server [^\\n]+\\n");
		matcher = pattern.matcher(rawConfig);
		rawConfig = matcher.replaceAll("");
		
		// Remove NTP settings
		pattern = Pattern.compile("ntp [^\\n]+\\n");
		matcher = pattern.matcher(rawConfig);
		rawConfig = matcher.replaceAll("");
		
		// Remove logging settings
		pattern = Pattern.compile("(no )?logging [^\\n]+\\n");
		matcher = pattern.matcher(rawConfig);
		rawConfig = matcher.replaceAll("");
		
		return rawConfig;	
	}
	
	private String cleanWhitespace(String rawConfig) {
		Pattern pattern = null;
		Matcher matcher = null;
		pattern = Pattern.compile("\\n\\n\\n+");
		matcher = pattern.matcher(rawConfig);
		rawConfig = matcher.replaceAll("\n\n");
		pattern = Pattern.compile("!\\n(!\\n)+");
		matcher = pattern.matcher(rawConfig);
		rawConfig = matcher.replaceAll("!\n");
		return rawConfig;
	}
}
