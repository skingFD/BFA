package edu.wisc.cs.arc.graphs;

import org.batfish.representation.Ip;
import org.batfish.representation.LineAction;
import org.batfish.representation.Prefix;
import org.batfish.representation.cisco.StandardAccessList;
import org.batfish.representation.cisco.StandardAccessListLine;
import org.junit.Assert;
import org.junit.Test;

public class FlowTest {

	@Test
	public void testStandard() {
		Flow flow = new Flow(new PolicyGroup(new Prefix("10.0.1.0/24")),
				new PolicyGroup(new Prefix("10.0.2.0/24")));
		StandardAccessList acl = new StandardAccessList("blocked");
		acl.addLine(new StandardAccessListLine(LineAction.REJECT, 
				new Ip("10.0.1.0"), new Ip("0.0.0.255")));
		acl.addLine(new StandardAccessListLine(LineAction.ACCEPT, 
				new Ip("0.0.0.0"), new Ip("255.255.255.255")));
		Assert.assertTrue(flow.isBlocked(acl));
		
		acl = new StandardAccessList("allowed");
		acl.addLine(new StandardAccessListLine(LineAction.ACCEPT, 
				new Ip("0.0.0.0"), new Ip("255.255.255.255")));
		acl.addLine(new StandardAccessListLine(LineAction.REJECT, 
				new Ip("10.0.1.0"), new Ip("0.0.0.255")));
		Assert.assertFalse(flow.isBlocked(acl));
	}
}
