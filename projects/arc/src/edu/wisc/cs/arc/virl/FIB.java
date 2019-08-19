package edu.wisc.cs.arc.virl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.wisc.cs.arc.graphs.PolicyGroup;

public class FIB {
	private List<FIBEntry> entries;

	public FIB(List<String> lines) {
		this.entries = new ArrayList<FIBEntry>();
		this.parse(lines);
	}
	
	private void parse(List<String> lines) {
		String prefix = null;
		String carryOver = null;
		for (String line : lines) {			
			Pattern pattern;
			Matcher matcher;
			
			if (carryOver != null) {
				line = carryOver + line;
				carryOver = null;
			}
			
			pattern = Pattern.compile("^(L|C|S|R|B|D|EX|O|IA|N1|N2|E1|E2)\\s");
			matcher = pattern.matcher(line);
			if (matcher.find()) {
				// Directly connected IOS
				pattern = Pattern.compile(
						"(?<prefix>\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+) is directly "
						+ "connected, (?<iface>.+)");
				matcher = pattern.matcher(line);
				if (matcher.find()) {
					this.entries.add(new FIBEntry(matcher.group("prefix"), 
							matcher.group("iface")));
					continue;
				}
				
				// Via gateway IOS
				pattern = Pattern.compile(
						"(?<prefix>\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+)\\s+[^\\s]+"
						+ " via (?<gateway>\\d+\\.\\d+\\.\\d+\\.\\d+)"
					    + "(, \\d\\d:\\d\\d:\\d\\d)?"
						+ "(, (?<iface>.*Ethernet.+))?");
				matcher = pattern.matcher(line);
				if (matcher.find()) {
					this.entries.add(new FIBEntry(matcher.group("prefix"), 
							matcher.group("gateway"), matcher.group("iface")));
					continue;
				}
				
				carryOver = line;
				continue;
			}
			
			// Via NX-OS
			pattern = Pattern.compile(
					"via (?<gateway>\\d+\\.\\d+\\.\\d+\\.\\d+), "
					+ "(?<iface>[^,]+), [^,]+, \\d\\d:\\d\\d:\\d\\d, "
					+ "(?<type>.+)");
			matcher = pattern.matcher(line);
			if (matcher.find()) {
				// If there is no active prefix, then this must be the second
				// entry, which we ignore
				if (null == prefix) {
					continue;
				}
				
				// Determine whether to provide gateway depending on type of
				// route entry
				if (matcher.group("type").equals("local")
						|| matcher.group("type").equals("direct")) {
					this.entries.add(new FIBEntry(prefix, 
							matcher.group("iface")));
				} else {
					System.out.println("OTHER TYPE: " + matcher.group("type"));
					this.entries.add(new FIBEntry(prefix, 
							matcher.group("gateway"), matcher.group("iface")));
				}
				prefix = null;
				continue;
			}
			
			// Prefix NX-OS
			pattern = Pattern.compile(
					"(?<prefix>\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+), ubest/mbest: "
					+ "\\d+/\\d+, attached");
			matcher = pattern.matcher(line);
			if (matcher.find()) {
				prefix = matcher.group("prefix");
				continue;
			}
			
		}
	}
	
	public FIBEntry getMatchingEntry(PolicyGroup destination) {
		FIBEntry bestEntry = null;
		for (FIBEntry entry : this.entries) {
			if (entry.matches(destination)) {
				if ((null == bestEntry) || (entry.getPrefixLength() 
						> bestEntry.getPrefixLength())) {
					bestEntry = entry;
				}
			}
		}
		return bestEntry;
	}
	
	public String toString() {
		String result = "";
		for (FIBEntry entry : this.entries) {
			result += entry + "\n";
		}
		return result;
	}
}
