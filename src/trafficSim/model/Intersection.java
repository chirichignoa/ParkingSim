package trafficSim.model;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;

import trafficSim.util.FixedGeography;

public class Intersection implements FixedGeography{
	
	private Coordinate coords;
	private List<Street> streets;
	
	public Intersection() {
		this.streets = new ArrayList<>();
	}

	@Override
	public Coordinate getCoords() {
		return coords;
	}

	@Override
	public void setCoords(Coordinate c) {
		this.coords = c;
	}
	
	public void addRoad(Street street) {
		this.streets.add(street);
	}
	
	public List<Street> getRoads() {
		return this.streets;
	}

}
