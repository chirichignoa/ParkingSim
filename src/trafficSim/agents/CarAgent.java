package trafficSim.agents;

import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Level;

import com.vividsolutions.jts.geom.Coordinate;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunState;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.gis.Geography;
import repast.simphony.util.ContextUtils;
import trafficSim.model.Intersection;
import trafficSim.model.Street;
import trafficSim.main.*;

public class CarAgent {

	private int id;
	private Street source, destination;
	private Context context;

	public CarAgent(int id) {
		this.id = id;
		this.context = ContextUtils.getContext(this);
//		this.streetContext = ContextUtils.findContext(RunState.getInstance().getMasterContext(), "StreetContext");
//		if(this.source == null) {
//			this.source = (Street) this.streetContext.getRandomObject();
//		}
	}

	@ScheduledMethod(start = 1, interval = 1, priority = ScheduleParameters.FIRST_PRIORITY)
	public void step() {  	
		randomWalk();
	}

	private void randomWalk() {
		if(this.destination == null) {
			this.destination = (Street) ContextCreator.getStreetContext().getRandomObject();
		}
		Geography<CarAgent> geography = (Geography)this.context .getProjection("AgentGeography");

//		geography.moveByDisplacement(this, RandomHelper.nextDoubleFromTo(-0.0005, 0.0005), 
//				RandomHelper.nextDoubleFromTo(-0.0005, 0.0005));
	}
	
	private void getRoute() {
		ArrayList<Coordinate> routeX = new ArrayList<>();
		ArrayList<Street> streetsX = new ArrayList<>();
//		ArrayList<String> routeDescriptionX = new ArrayList<>();
//		ArrayList<Double> routeSpeedsX = new ArrayList<>();

		Coordinate currentCoord = ContextCreator.getAgentGeography().getGeometry(this).getCoordinate();
		Coordinate destCoord = ContextCreator.getStreetProjection().getGeometry(this.destination).getCoordinate();
		
		// TODO: chequear si es que las coordenadas no referencian a una calle, encontrar la calle mas cercana a ese punto.
		
		ArrayList<Intersection> sourceIntersections = this.source.getIntersections();
		ArrayList<Intersection> destinationIntersections = this.destination.getIntersections();
		
		// Busco el camino mas corto que pueda existir entre la combinacion de las esquinas

// 		double shortestPathLength = Double.MAX_VALUE;
// 		double pathLength = 0;
// 		ShortestPath<Intersection> p;
// 		List<RepastEdge<Intersection>> shortestPath = null;
// 		for (Junction s : sourceIntersections) {
// 			for (Junction d : destinationIntersections) {
// 				p = new ShortestPath<Intersection>(ContextCreator.streetNetwork);
// 				pathLength = p.getPathLength(s,d);
// 				if (pathLength < shortestPathLength) {
// 					shortestPathLength = pathLength;
// 					shortestPath = p.getPath(s,d);
// 					routeEndpoints[0] = o;
// 					routeEndpoints[1] = d;
// 				}
// 			}
// 		}

	// Ahora debo buscar el camino hacia cada una de las intersecciones elegidas
	// Intersection currentJunction = routeEndpoints[0];
	// Intersection destJunction = routeEndpoints[1];

	}

	public Street getSource() {
		return source;
	}

	public void setSource(Street source) {
		this.source = source;
	}

	public Street getDestiny() {
		return destination;
	}

	public void setDestiny(Street destiny) {
		this.destination = destiny;
	}

	public int getId() {
		return id;
	}
}
