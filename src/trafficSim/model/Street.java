package trafficSim.model;

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Coordinate;

import trafficSim.util.FixedGeography;

public class Street implements FixedGeography{
	
	private Coordinate coord;
	
	private ArrayList<Intersection> intersections;
	private StreetNetworkEdge<Intersection> edge;
	
	public Street() {
		this.coord = new Coordinate();
		this.intersections = new ArrayList<>();
	}
	
	public Coordinate getCoords() {
		return this.coord;
	}
	
	public void setCoords(Coordinate c) {
		this.coord = c;
	}

	public void addIntersection(Intersection intersection) {
		this.intersections.add(intersection);
	}
	
	public ArrayList<Intersection> getIntersections() {
		return this.intersections;
	}
	
	public StreetNetworkEdge<Intersection> getEdge() {
		return edge;
	}

	public void setEdge(StreetNetworkEdge<Intersection> edge) {
		this.edge = edge;
	}
}
