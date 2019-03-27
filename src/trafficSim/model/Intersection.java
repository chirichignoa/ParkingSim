package trafficSim.model;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;

import trafficSim.util.FixedGeography;

public class Intersection implements FixedGeography{
	
	private Coordinate coords;
	private List<Street> streets;
	private List<Intersection> next, previous;
	
	public Intersection() {
		this.streets = new ArrayList<>();
		this.next = new ArrayList<>();
		this.previous = new ArrayList<>();
	}

	@Override
	public Coordinate getCoord() {
		return coords;
	}

	@Override
	public void setCoord(Coordinate c) {
		this.coords = c;
	}
	
	public void addRoad(Street street) {
		this.streets.add(street);
	}
	
	public List<Street> getRoads() {
		return this.streets;
	}

	public List<Intersection> getNext() {
		return next;
	}

	public void addNext(Intersection next) {
		this.next.add(next);
	}
	
	public List<Intersection> getPrevious() {
		return previous;
	}

	public void addPrevious(Intersection previous) {
		this.previous.add(previous);
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("Intersection: ");
		for(Street s: streets) {
			str.append(s.getName());
			str.append(" ");
		}
		return str.toString();
	}
	
	

}
