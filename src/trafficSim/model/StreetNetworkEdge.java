package trafficSim.model;

import java.util.List;

import repast.simphony.space.graph.RepastEdge;

public class StreetNetworkEdge<T> extends RepastEdge<T>{

	private Street street;
	
	public StreetNetworkEdge(T source, T target, boolean directed, double weight) {
		super(source, target, directed, weight);
	}

	public void setStreet(Street street) {
		this.street = street;
		
	}
}
