package edu.wisc.cs.arc.graphs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.batfish.representation.Ip;
import org.batfish.representation.Prefix;
import org.batfish.util.SubRange;
import org.junit.Assert;
import org.junit.Test;

public class PolicyGroupTest {
	
	@Test
	public void testContainsPrefixContainsLeft() {
		PolicyGroup groupA = new PolicyGroup(new Prefix("10.0.1.0/24"));
		PolicyGroup groupB = new PolicyGroup(new Prefix("10.0.1.0/25"));

		Assert.assertTrue(groupA.contains(groupB));
		Assert.assertFalse(groupB.contains(groupA));
	}
	
	@Test
	public void testContainsDefaultPrefixContains() {
		PolicyGroup groupA = new PolicyGroup(new Prefix("0.0.0.0/0"));
		PolicyGroup groupB = new PolicyGroup(new Prefix("10.0.1.0/24"));

		Assert.assertTrue(groupA.contains(groupB));
		Assert.assertFalse(groupB.contains(groupA));
	}
	
	@Test
	public void testContainsPortRangeContainsRight() {
		PolicyGroup groupA = new PolicyGroup(new Prefix("10.0.1.0/24"),
				new SubRange(1,100));
		PolicyGroup groupB = new PolicyGroup(new Prefix("10.0.1.0/24"),
				new SubRange(51,100));

		Assert.assertTrue(groupA.contains(groupB));
		Assert.assertFalse(groupB.contains(groupA));
	}
	
	@Test
	public void testContainsBothContainsLowerLeft() {
		PolicyGroup groupA = new PolicyGroup(new Prefix("10.0.1.0/24"),
				new SubRange(1,100));
		PolicyGroup groupB = new PolicyGroup(new Prefix("10.0.1.0/25"),
				new SubRange(1,50));

		Assert.assertTrue(groupA.contains(groupB));
		Assert.assertFalse(groupB.contains(groupA));
	}
	
	@Test
	public void testIntersectsPrefixContainsLeft() {
		PolicyGroup groupA = new PolicyGroup(new Prefix("10.0.1.0/24"));
		PolicyGroup groupB = new PolicyGroup(new Prefix("10.0.1.0/25"));

		Assert.assertTrue(groupA.intersects(groupB));
		Assert.assertTrue(groupB.intersects(groupA));
	}
	
	@Test
	public void testIntersectsDefaultPrefixContains() {
		PolicyGroup groupA = new PolicyGroup(new Prefix("0.0.0.0/0"));
		PolicyGroup groupB = new PolicyGroup(new Prefix("10.0.1.0/24"));

		Assert.assertTrue(groupA.intersects(groupB));
		Assert.assertTrue(groupB.intersects(groupA));
	}
	
	@Test
	public void testGetNonOverlappingPrefixContainsLeft() {
		PolicyGroup groupA = new PolicyGroup(new Prefix("10.0.1.0/24"));
		PolicyGroup groupB = new PolicyGroup(new Prefix("10.0.1.0/25"));
		
		List<PolicyGroup> expected = new ArrayList<PolicyGroup>();
		expected.add(new PolicyGroup(new Prefix("10.0.1.0/25")));
		expected.add(new PolicyGroup(new Prefix("10.0.1.128/25")));
		
		Set<PolicyGroup> nonOverlapping = groupA.getNonOverlapping(groupB);
		
		Assert.assertArrayEquals(expected.toArray(), nonOverlapping.toArray());
	}
	
	@Test
	public void testGetNonOverlappingPrefixContainsCenter() {
		PolicyGroup groupA = new PolicyGroup(new Prefix("10.0.1.0/24"));
		PolicyGroup groupB = new PolicyGroup(new Prefix("10.0.1.64/26"));
		
		List<PolicyGroup> expected = new ArrayList<PolicyGroup>();
		expected.add(new PolicyGroup(new Prefix("10.0.1.0/26")));
		expected.add(new PolicyGroup(new Prefix("10.0.1.64/26")));
		expected.add(new PolicyGroup(new Prefix("10.0.1.128/25")));
		
		Set<PolicyGroup> nonOverlapping = groupA.getNonOverlapping(groupB);
		
		Assert.assertArrayEquals(expected.toArray(), nonOverlapping.toArray());
	}
	
	@Test
	public void testGetNonOverlappingDefaultPrefixContains() {
		PolicyGroup groupA = new PolicyGroup(new Prefix("0.0.0.0/0"));
		PolicyGroup groupB = new PolicyGroup(new Prefix("10.0.1.0/24"));
		
		List<PolicyGroup> expected = new ArrayList<PolicyGroup>();
		expected.add(new PolicyGroup(new Ip("0.0.0.0"), new Ip("10.0.0.255")));
		expected.add(new PolicyGroup(new Prefix("10.0.1.0/24")));
		expected.add(new PolicyGroup(new Ip("10.0.2.0"),
				new Ip("255.255.255.255")));
		
		Set<PolicyGroup> nonOverlapping = groupA.getNonOverlapping(groupB);
		System.out.println(nonOverlapping);
		
		Assert.assertArrayEquals(expected.toArray(), nonOverlapping.toArray());
	}
	
	@Test
	public void testGetNonOverlappingPortRangeContainsRight() {
		PolicyGroup groupA = new PolicyGroup(new Prefix("10.0.1.0/24"),
				new SubRange(1,100));
		PolicyGroup groupB = new PolicyGroup(new Prefix("10.0.1.0/24"),
				new SubRange(51,100));
		
		List<PolicyGroup> expected = new ArrayList<PolicyGroup>();
		expected.add(new PolicyGroup(new Prefix("10.0.1.0/24"),
				new SubRange(1,50)));
		expected.add(new PolicyGroup(new Prefix("10.0.1.0/24"),
				new SubRange(51,100)));
		
		Set<PolicyGroup> nonOverlapping = groupA.getNonOverlapping(groupB);
		
		Assert.assertArrayEquals(expected.toArray(), nonOverlapping.toArray());
	}
	
	@Test
	public void testGetNonOverlappingPortRangeIntersectLeft() {
		PolicyGroup groupA = new PolicyGroup(new Prefix("10.0.1.0/24"),
				new SubRange(1,100));
		PolicyGroup groupB = new PolicyGroup(new Prefix("10.0.1.0/24"),
				new SubRange(51,150));
		
		List<PolicyGroup> expected = new ArrayList<PolicyGroup>();
		expected.add(new PolicyGroup(new Prefix("10.0.1.0/24"),
				new SubRange(1,50)));
		expected.add(new PolicyGroup(new Prefix("10.0.1.0/24"),
				new SubRange(51,100)));
		expected.add(new PolicyGroup(new Prefix("10.0.1.0/24"),
				new SubRange(101,150)));
		
		Set<PolicyGroup> nonOverlapping = groupA.getNonOverlapping(groupB);
		
		Assert.assertArrayEquals(expected.toArray(), nonOverlapping.toArray());
	}
	
	@Test
	public void testGetNonOverlappingBothContainsLowerLeft() {
		PolicyGroup groupA = new PolicyGroup(new Prefix("10.0.1.0/24"),
				new SubRange(1,100));
		PolicyGroup groupB = new PolicyGroup(new Prefix("10.0.1.0/25"),
				new SubRange(1,50));
		
		List<PolicyGroup> expected = new ArrayList<PolicyGroup>();
		expected.add(new PolicyGroup(new Prefix("10.0.1.0/25"),
				new SubRange(1,50)));
		expected.add(new PolicyGroup(new Prefix("10.0.1.128/25"),
				new SubRange(1,50)));
		expected.add(new PolicyGroup(new Prefix("10.0.1.0/25"),
				new SubRange(51,100)));
		expected.add(new PolicyGroup(new Prefix("10.0.1.128/25"),
				new SubRange(51,100)));
		
		Set<PolicyGroup> nonOverlapping = groupA.getNonOverlapping(groupB);
		
		Assert.assertArrayEquals(expected.toArray(), nonOverlapping.toArray());
	}
	
	@Test
	public void testGetNonOverlappingBothContainsCenter() {
		PolicyGroup groupA = new PolicyGroup(new Prefix("10.0.1.0/24"),
				new SubRange(1,150));
		PolicyGroup groupB = new PolicyGroup(new Prefix("10.0.1.128/26"),
				new SubRange(51,100));
		
		List<PolicyGroup> expected = new ArrayList<PolicyGroup>();
		expected.add(new PolicyGroup(new Prefix("10.0.1.0/25"),
				new SubRange(1,50)));
		expected.add(new PolicyGroup(new Prefix("10.0.1.128/26"),
				new SubRange(1,50)));
		expected.add(new PolicyGroup(new Prefix("10.0.1.192/26"),
				new SubRange(1,50)));
		expected.add(new PolicyGroup(new Prefix("10.0.1.0/25"),
				new SubRange(51,100)));
		expected.add(new PolicyGroup(new Prefix("10.0.1.128/26"),
				new SubRange(51,100)));
		expected.add(new PolicyGroup(new Prefix("10.0.1.192/26"),
				new SubRange(51,100)));
		expected.add(new PolicyGroup(new Prefix("10.0.1.0/25"),
				new SubRange(101,150)));
		expected.add(new PolicyGroup(new Prefix("10.0.1.128/26"),
				new SubRange(101,150)));
		expected.add(new PolicyGroup(new Prefix("10.0.1.192/26"),
				new SubRange(101,150)));
		
		Set<PolicyGroup> nonOverlapping = groupA.getNonOverlapping(groupB);
		
		Assert.assertArrayEquals(expected.toArray(), nonOverlapping.toArray());
	}
	
	@Test
	public void testGetNonOverlappingListIsolated() {
		Set<PolicyGroup> groups = new HashSet<PolicyGroup>();
		groups.add(new PolicyGroup(new Prefix("10.0.1.0/24")));
		groups.add(new PolicyGroup(new Prefix("10.0.2.0/24")));
		
		Set<PolicyGroup> expected = groups;
		
		Set<PolicyGroup> nonOverlapping = 
				PolicyGroup.getNonOverlapping(groups);
		Assert.assertArrayEquals(expected.toArray(), nonOverlapping.toArray());
	}
	
	@Test
	public void testGetNonOverlappingListOneIsolated() {
		Set<PolicyGroup> groups = new HashSet<PolicyGroup>();
		groups.add(new PolicyGroup(new Prefix("10.0.1.0/24")));
		groups.add(new PolicyGroup(new Prefix("10.0.1.128/25")));
		groups.add(new PolicyGroup(new Prefix("10.0.2.0/24")));
		
		Set<PolicyGroup> expected = new HashSet<PolicyGroup>();
		expected.add(new PolicyGroup(new Prefix("10.0.2.0/24")));
		expected.add(new PolicyGroup(new Prefix("10.0.1.0/25")));
		expected.add(new PolicyGroup(new Prefix("10.0.1.128/25")));
		
		Set<PolicyGroup> nonOverlapping = 
				PolicyGroup.getNonOverlapping(groups);
		Assert.assertArrayEquals(expected.toArray(), nonOverlapping.toArray());
	}
	
	@Test
	public void testGetNonOverlappingListChainedOverlap() {
		Set<PolicyGroup> groups = new HashSet<PolicyGroup>();
		groups.add(new PolicyGroup(new Prefix("10.0.2.0/24")));
		groups.add(new PolicyGroup(new Prefix("10.0.2.0/23")));
		groups.add(new PolicyGroup(new Prefix("10.0.3.0/24")));
		
		Set<PolicyGroup> expected = new HashSet<PolicyGroup>();
		expected.add(new PolicyGroup(new Prefix("10.0.3.0/24")));
		expected.add(new PolicyGroup(new Prefix("10.0.2.0/24")));
		
		Set<PolicyGroup> nonOverlapping = 
				PolicyGroup.getNonOverlapping(groups);
		Assert.assertArrayEquals(expected.toArray(), nonOverlapping.toArray());
	}
}
