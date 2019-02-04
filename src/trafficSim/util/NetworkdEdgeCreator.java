package trafficSim.util;

import java.util.Arrays;

import repast.simphony.space.graph.EdgeCreator;
import trafficSim.model.StreetNetworkEdge;

public class NetworkdEdgeCreator<T> implements EdgeCreator<StreetNetworkEdge<T>, T> {

	@Override
	public StreetNetworkEdge<T> createEdge(T source, T target, boolean isDirected, double weight) {
		return new StreetNetworkEdge<T>(source, target, isDirected, weight);
	}

	/**
	 * Gets the edge type produced by this EdgeCreator.
	 * 
	 * @return the edge type produced by this EdgeCreator.
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public Class<StreetNetworkEdge> getEdgeType() {
		return StreetNetworkEdge.class;
	}

}
