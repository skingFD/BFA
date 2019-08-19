package edu.tsinghua.lyf;

import java.util.ArrayList;
import java.util.Scanner;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import edu.wisc.cs.arc.graphs.DirectedEdge;
import edu.wisc.cs.arc.graphs.Vertex;
import edu.wisc.cs.arc.graphs.Vertex.VertexType;

public class maxFlow{
	public int maxV = Integer.MAX_VALUE;
	public int[][] capacity;//统计给定图前向边和后向边剩余流量
	public int[][] graph;//用于记录当前图
	public int[] flow;//用于统计从源点到图中任意一点的最大可增加的流量
	public int[] pre;//用于记录当前到达顶点的前驱结点
	public int result = 0;
	public int nodenumber = 0;
	public DefaultDirectedWeightedGraph<Vertex,DirectedEdge> inputgraph;
	public ArrayList<Vertex> nodelist;
	public ArrayList<DirectedEdge> edgelist;
	public Vertex Svertex;
	public Vertex Dvertex;
	public int Snodeindex;
	public int Dnodeindex;
	
	public maxFlow(){
		
	}
	
	public maxFlow(DefaultDirectedWeightedGraph<Vertex,DirectedEdge> inputgraph)
	{
		this.inputgraph = inputgraph;
		nodelist = new ArrayList<Vertex>(inputgraph.vertexSet());
		edgelist = new ArrayList<DirectedEdge>(inputgraph.edgeSet());
		nodenumber = nodelist.size();
		capacity = new int[nodenumber][nodenumber];
		graph = new int[nodenumber][nodenumber];
		flow = new int[nodenumber];
		pre = new int[nodenumber];
		
		for(int i = 0; i<edgelist.size();i++){
			int Sindex = nodelist.indexOf(edgelist.get(i).getSource());
			int Dindex = nodelist.indexOf(edgelist.get(i).getDestination());
			if((Sindex==-1)||(Dindex==-1))
			{
				continue;
			}
			if(edgelist.get(i).getWeight()>10000)
			{
				capacity[Sindex][Dindex] = 10000;
				graph[Sindex][Dindex] = 10000;
			}else{
				capacity[Sindex][Dindex] += (int) (edgelist.get(i).getWeight());
				graph[Sindex][Dindex] += (int)(edgelist.get(i).getWeight());
			}
		}
	}
	
	public int bfs(int source, int dest, int weight) {  //使用BFS遍历，寻找给定图的增广路径
        ArrayList<Integer> list = new ArrayList<Integer>();
        list.add(source);      //源点为顶点source
        for(int i = 0;i < nodenumber;i++) {
            pre[i] = -1;   //初始化所有顶点的前驱顶点为-1
        }
        pre[source] = source;     //源点的前驱顶点设定为自己
        flow[source] = maxV; //源点的前驱顶点到源点的增加流量设定为无穷大
        while(!list.isEmpty()) {
            int index = list.get(0);
            list.remove(0);
            if(index == dest)
                break;
            for(int i = 0;i < capacity.length;i++) {
                if(capacity[index][i] > 0 && pre[i] == -1) {//当顶点i未被访问且到达顶点i有剩余流量时
                    pre[i] = index;  //顶点i的前驱顶点为index
                    flow[i] = Math.min(flow[index],Math.min( capacity[index][i],weight));
                    list.add(i);
                }
            }
        }
        if(pre[dest] != -1)
            return flow[dest];
        return -1;
    }
	
    public int bfs_remove(int source, int dest, int weight) {  //使用BFS遍历，寻找给定图的增广路径
        ArrayList<Integer> list = new ArrayList<Integer>();
        list.add(source);      //源点为顶点source
        for(int i = 0;i < nodenumber;i++) {
            pre[i] = -1;   //初始化所有顶点的前驱顶点为-1
        }
        pre[source] = source;     //源点的前驱顶点设定为自己
        flow[source] = maxV; //源点的前驱顶点到源点的增加流量设定为无穷大
        while(!list.isEmpty()) {
            int index = list.get(0);
            list.remove(0);
            if(index == dest)
                break;
            for(int i = 0;i < capacity.length;i++) {
                if(capacity[index][i] > 0 && pre[i] == -1) {//当顶点i未被访问且到达顶点i有剩余流量时
                    pre[i] = index;  //顶点i的前驱顶点为index
                    flow[i] = Math.min(flow[index],Math.min( capacity[index][i],weight));
                    list.add(i);
                }
            }
        }
        if(pre[dest] != -1)
            return flow[dest];
        return -1;
    }
    
    public void findDiff(DefaultDirectedWeightedGraph<Vertex,DirectedEdge> inputgraph) {
    	int[][] newgraph = new int[nodenumber][nodenumber];
		nodelist = new ArrayList<Vertex>(inputgraph.vertexSet());
		edgelist = new ArrayList<DirectedEdge>(inputgraph.edgeSet());
		for(int i = 0; i<edgelist.size();i++){
			int Sindex = nodelist.indexOf(edgelist.get(i).getSource());
			int Dindex = nodelist.indexOf(edgelist.get(i).getDestination());
			if(edgelist.get(i).getWeight()>10000)
			{
				newgraph[Sindex][Dindex] = 10000;
			}else{
				newgraph[Sindex][Dindex] += (int)(edgelist.get(i).getWeight());
			}
		}
    	ArrayList<edge> edgeAdd = new ArrayList<edge>();
    	ArrayList<edge> edgeDel = new ArrayList<edge>();
    	for(int i = 0; i<nodenumber;i++) {
    		for(int j = 0;j<nodenumber;j++) {
    			if(newgraph[i][j]>graph[i][j]) {
    				edgeAdd.add(new edge(i,j,newgraph[i][j]-graph[i][j]));
    			}else if(newgraph[i][j]<graph[i][j]) {
    				edgeDel.add(new edge(i,j,graph[i][j]-newgraph[i][j]));
    			}
    		}
    	}
    	for(edge temp:edgeDel) {
    		if(temp.change<=capacity[temp.src][temp.dst]) {
    			capacity[temp.src][temp.dst] -= temp.change;
    			graph[temp.src][temp.dst] -= temp.change;
    			continue;
    		}else {
    			temp.change -= capacity[temp.src][temp.dst];
    			graph[temp.src][temp.dst] -= capacity[temp.src][temp.dst];
    			capacity[temp.src][temp.dst] = 0;
    		}
    		if(temp.src != Snodeindex) {
    			getResult(temp.src,Snodeindex,temp.change);
    		}
    		if(temp.dst != Dnodeindex) {
    			getResult(Dnodeindex,temp.dst,temp.change);
    		}
    		graph[temp.src][temp.dst] -= temp.change;
    		result -= temp.change;
    	}
    	for(edge temp:edgeAdd) {
    		capacity[temp.src][temp.dst] += temp.change;
    		graph[temp.src][temp.dst] += temp.change;
    	}
    	result += getResult(Snodeindex,Dnodeindex,maxV);
    	return;
    }
    
    public void addResult(int source, int dest){
    	capacity[source][dest] += 1;
		graph[source][dest] += 1;
		getResult(Svertex,Dvertex);
    }
    
    public void addResult(Vertex source, Vertex dest){
    	//if(dest.getType() == VertexType.SOURCE){
    	//	//System.out.println("okasi");
    	//}
    	//boolean changea = false;
    	//boolean changeb = false;
    	int Snodeindex = nodelist.indexOf(source);
    	if(Snodeindex == -1){
    	//	changea = true;
    		source.changeType();
    		Snodeindex = nodelist.indexOf(source);
    	}
		int Dnodeindex = nodelist.indexOf(dest);
		if(Dnodeindex == -1){
		//	changeb = true;
			dest.changeType();
			Dnodeindex = nodelist.indexOf(dest);
		}
		capacity[Snodeindex][Dnodeindex] += 1;
		graph[Snodeindex][Dnodeindex] += 1;
		
		//if(changea){
    	//	source.changeType();
    	//}
    	
    	//if(changeb){
    	//	dest.changeType();
    	//}
		getResult(Svertex,Dvertex);
    }
    
    public void getResult(Vertex source, Vertex dest){
    	Svertex = source;
    	Dvertex = dest;
    	Snodeindex = nodelist.indexOf(Svertex);
    	Dnodeindex = nodelist.indexOf(Dvertex);
    	result += getResult(nodelist.indexOf(source),nodelist.indexOf(dest),maxV);
    }
    
    public int getResult(int source, int dest,int weight) {
    	int tempresult = 0;
        int temp = bfs(source,dest,weight);
        while((temp != -1)&&(weight!=0)) {
            tempresult = tempresult + temp;
            int start = pre[dest];
            int end = dest;
            while(start != source) {
                capacity[start][end] -= temp;   //前向边剩余流量减少temp
                capacity[end][start] += temp;   //后向边剩余流量增加temp
                end = start;
                start = pre[end];
            }
            capacity[source][end] -= temp;
            capacity[end][source] += temp;
            weight -= temp;
            temp = bfs(source,dest,weight);
        }
        if(tempresult == 0){
        	//Vertex testS = new Vertex(VertexType.SOURCE);
        	maxFlowutil.setZeroVertex(Svertex, Dvertex);
        }
        //System.out.println(result);
        //System.out.println(Svertex);
        //System.out.println(Dvertex);
        return tempresult;
    }
    
    public void getResult_remove(Vertex removeSource, Vertex removeDest, int value){
    	int a = nodelist.indexOf(Svertex);
    	int b = nodelist.indexOf(Dvertex);
    	//boolean changea = false;
    	//boolean changeb = false;
   
    	int removea = nodelist.indexOf(removeSource);
    	if(removea == -1){
    	//	changea = true;
    		removeSource.changeType();
    		removea = nodelist.indexOf(removeSource);
    	}
		int removeb = nodelist.indexOf(removeDest);
		if(removeb == -1){
		//	changeb = true;
			removeDest.changeType();
			removeb = nodelist.indexOf(removeDest);
		}
		//System.out.println(graph[removea][removeb]);
		//System.out.println(capacity[removea][removeb]);
    	int remove = Math.min(graph[removea][removeb]-capacity[removea][removeb], value);
    	if(remove!=0){
    		if(!removeSource.equals(Svertex) ){
    			getResult_remove(removea,a,remove);
    		}
    		if(!removeDest.equals(Dvertex)){
    			getResult_remove(b,removeb,remove);
    		}
    	}
    	graph[removea][removeb] -= value;
    	capacity[removea][removeb] -= value;
    	result -= remove;
    	if(remove!=0)
    	{
    		getResult(a,b,maxV);
    	}
    	
    	//if(changea){
    	//	removeSource.changeType();
    	//}
    	
    	//if(changeb){
    	//	removeSource.changeType();
    	//}
    	//System.out.println(result);
        //System.out.println(Svertex);
        //System.out.println(Dvertex);
    }
    
    public void getResult_remove(int source, int dest,int weight) {
        int temp = bfs(source,dest,weight);
        while(temp != -1) {
            int start = pre[dest];
            int end = dest;
            while(start != source) {
                capacity[start][end] -= temp;   //前向边剩余流量减少temp
                capacity[end][start] += temp;   //后向边剩余流量增加temp
                end = start;
                start = pre[end];
            }
            capacity[source][end] -= temp;
            capacity[end][source] += temp;
            temp = bfs(source,dest,weight);
        }
        return;
    }
	
}