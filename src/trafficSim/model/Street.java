package trafficSim.model;

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Coordinate;

import trafficSim.util.FixedGeography;

public class Street implements FixedGeography{
	
	private int osmId;
	private Coordinate[] coords;
	private String name, oneWay, direction;
	private ArrayList<Intersection> intersections;
	
	public Street(int osmId) {
		this.osmId = osmId;
		this.intersections = new ArrayList<>();
	}
	
	public Coordinate[] getCoords() {
		return this.coords;
	}
	
	public void setCoords(Coordinate[] c) {
		this.coords = c;
	}

	public void addIntersection(Intersection intersection) {
		this.intersections.add(intersection);
	}
	
	public ArrayList<Intersection> getIntersections() {
		return this.intersections;
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOneWay() {
		return oneWay;
	}

	public void setOneWay(String oneWay) {
		this.oneWay = oneWay;
	}

	public void setIntersections(ArrayList<Intersection> intersections) {
		this.intersections = intersections;
	}

	public int getOsmId() {
		return osmId;
	}

	public void setOsmId(int osmId) {
		this.osmId = osmId;
	}
	
	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((coords == null) ? 0 : coords.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Street other = (Street) obj;
		if (coords == null) {
			if (other.coords != null)
				return false;
		} else if (!coords.equals(other.coords))
			return false;
		return true;
	}

	@Override
	public Coordinate getCoord() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setCoord(Coordinate c) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String toString() {
		return "Street: " + name;
	}
	
	
	
}
