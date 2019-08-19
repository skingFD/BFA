package edu.tsinghua.lyf;

public class edge{
	int src;
	int dst;
	String srcname;
	String dstname;
	int change;
	public edge() {
		this.src = 0;
		this.dst = 0;
		this.srcname = "";
		this.dstname = "";
		this.change = 0;
	}
	public edge(int src, int dst, int change) {
		this.src = src;
		this.dst = dst;
		this.srcname = "";
		this.dstname = "";
		this.change = change;
	}
	public edge(int src, int dst, String srcname, String dstname, int change) {
		this.src = src;
		this.dst = dst;
		this.srcname = srcname;
		this.dstname = dstname;
		this.change = change;
	}
	public int getSrc() {
		return src;
	}
	public void setSrc(int src) {
		this.src = src;
	}
	public int getDst() {
		return dst;
	}
	public void setDst(int dst) {
		this.dst = dst;
	}
	public String getSrcname() {
		return srcname;
	}
	public void setSrcname(String srcname) {
		this.srcname = srcname;
	}
	public String getDstname() {
		return dstname;
	}
	public void setDstname(String dstname) {
		this.dstname = dstname;
	}
	public int getChange() {
		return change;
	}
	public void setChange(int change) {
		this.change = change;
	}
}