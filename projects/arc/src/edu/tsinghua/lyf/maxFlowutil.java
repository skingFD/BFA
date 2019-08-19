package edu.tsinghua.lyf;

import java.util.ArrayList;
import java.util.Scanner;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import edu.wisc.cs.arc.graphs.DirectedEdge;
import edu.wisc.cs.arc.graphs.Vertex;

public class maxFlowutil{
	public static ArrayList<maxFlow> maxFlowList = new ArrayList<maxFlow>();
	public static int directCosttime = 0;
	public static int incrementalCosttime = 0;
	public static Vertex Szero;
	public static Vertex Dzero;
	
	public static int getResult(DefaultDirectedWeightedGraph<Vertex,DirectedEdge> inputgraph, Vertex Svertex, Vertex Dvertex){
		int result = -1;
		for(int i = 0;i<maxFlowList.size();i++)
		{
			if(maxFlowList.get(i).Svertex.equals(Svertex)&&maxFlowList.get(i).Dvertex.equals(Dvertex)){
				maxFlowList.get(i).findDiff(inputgraph);
				return maxFlowList.get(i).result;
			}
		}
		maxFlow newMaxFlow = new maxFlow(inputgraph);
		long starttime = System.nanoTime();
		newMaxFlow.getResult(Svertex, Dvertex);
		long endtime = System.nanoTime();
		directCosttime += endtime-starttime;
		result = newMaxFlow.result;
		maxFlowList.add(newMaxFlow);
		return result;
	}
	
	public static void testadd(){
		long starttime = System.nanoTime();
		ArrayList<Integer> result = new ArrayList<Integer>();
		for(int i = 0;i<maxFlowList.size();i++){
			maxFlowList.get(i).addResult(Szero,Dzero);
			result.add(maxFlowList.get(i).result);
		}
		long endtime = System.nanoTime();
		incrementalCosttime += endtime-starttime;
		System.out.println("directtime:" + directCosttime);
		System.out.println("incretime:" + incrementalCosttime);
	}
	
	public static void testdel(){
		long starttime = System.nanoTime();
		ArrayList<Integer> result = new ArrayList<Integer>();
		for(int i = 0;i<maxFlowList.size();i++){
			maxFlowList.get(i).getResult_remove(Szero,Dzero,1);
			result.add(maxFlowList.get(i).result);
		}
		long endtime = System.nanoTime();
		incrementalCosttime += endtime-starttime;
		System.out.println("directtime:" + directCosttime);
		System.out.println("incretime:" + incrementalCosttime);
	}
	
	public static void setZeroVertex(Vertex SZero, Vertex DZero)
	{
		Szero = SZero;
		Dzero = DZero;
	}
}