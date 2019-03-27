package trafficSim.agents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.ArrayUtils;
import org.geotools.referencing.GeodeticCalculator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunState;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.space.graph.ShortestPath;
import repast.simphony.util.ContextUtils;
import trafficSim.model.Intersection;
import trafficSim.model.Street;
import trafficSim.model.StreetNetworkEdge;
import trafficSim.main.*;

public class CarAgent {

	private static Logger LOGGER = Logger.getLogger(CarAgent.class.getName());
	private int id;
	private Street source, destination;
	private int currentPosition;
	private List<Coordinate> route;

	public CarAgent(int id, Street source) {
		this.currentPosition = 0;
		this.id = id;
		this.source = source;
		System.out.println("Source street -> " + this.source.getName() );
		if(this.destination == null) {
			this.destination = (Street) ContextCreator.getStreetContext().getRandomObject();
			while(this.destination.equals(this.source)) {
				this.destination = (Street) ContextCreator.getStreetContext().getRandomObject();
			}
			System.out.println("Destination street -> " + this.destination.getName() );
		}
	}

	@ScheduledMethod(start = 1, interval = 1, priority = ScheduleParameters.FIRST_PRIORITY)
	public void step() {
		if(this.route == null) {
			route = this.getRoute();
		}
		if (!this.atDestination()) {
			System.out.println("Travelling");
			if(this.route != null) {
				this.travel();
			} else {
				System.out.println("There isn't a route for connect source and destination");
			}
		}
	}
	
	public void travel() {
		// Check that the route has been created
		int distancePerTurn = 1; 
		if (this.atDestination()) {
			System.out.println("Arrive");
			return;
		}

		// Store the roads the agent walks along (used to populate the awareness space)
		// List<Road> roadsPassed = new ArrayList<Road>();
		double distTravelled = 0; // The distance travelled so far
		Coordinate currentCoord = null; // Current location
		Coordinate target = null; // Target coordinate we're heading for (in route list)
		boolean travelledMaxDist = false; // True when travelled maximum distance this iteration
		double speed = 40; // The speed to travel to next coord
		GeometryFactory geomFac = new GeometryFactory();
		currentCoord = ContextCreator.getAgentGeography().getGeometry(this).getCoordinate();
		while (!travelledMaxDist && !this.atDestination()) {
			target = route.get(this.currentPosition);
			// Work out the distance and angle to the next coordinate
			double[] distAndAngle = new double[2];
			this.distance(currentCoord, target, distAndAngle);
			// divide by speed because distance might effectively be shorter

			double distToTarget = distAndAngle[0] / speed;
			// If we can get all the way to the next coords on the route then just go there
			if (distTravelled + distToTarget < distancePerTurn) {
				distTravelled += distToTarget;
				currentCoord = target;

				// See if agent has reached the end of the route.
				if (this.currentPosition == (route.size() - 1)) {
					ContextCreator.getAgentGeography().move(this, geomFac.createPoint(currentCoord));
					break; // Break out of while loop, have reached end of route.
				}
				// Haven't reached end of route, increment the counter
				this.currentPosition++;
			} else if (distTravelled + distToTarget == distancePerTurn) {
				travelledMaxDist = true;
				ContextCreator.getAgentGeography().move(this, geomFac.createPoint(target));
				this.currentPosition++;
			} else {
				// Otherwise move as far as we can towards the target along the road we're on.
				// Move along the vector the maximum distance we're allowed this turn (take into account relative
				// speed)
				double distToTravel = (distancePerTurn - distTravelled) * speed;
				// Move the agent, first move them to the current coord (the first part of the while loop doesn't do
				// this for efficiency)
				ContextCreator.getAgentGeography().move(this, geomFac.createPoint(currentCoord));
				// Now move by vector towards target (calculated angle earlier).
				ContextCreator.getAgentGeography().moveByVector(this, distToTravel, distAndAngle[1]);

				travelledMaxDist = true;
			} // else
		}
	}
	
	private double getDistance(Coordinate c1, Coordinate c2, GeodeticCalculator calculator) {
		calculator.setStartingGeographicPoint(c1.x, c1.y);
		calculator.setDestinationGeographicPoint(c2.x, c2.y);
		return calculator.getOrthodromicDistance();
	}
	
	public synchronized double[] distance(Coordinate c1, Coordinate c2, double[] returnVals) {
		// TODO check this now, might be different way of getting distance in new Simphony
		GeodeticCalculator calculator = new GeodeticCalculator(ContextCreator.getStreetProjection().getCRS());
		calculator.setStartingGeographicPoint(c1.x, c1.y);
		calculator.setDestinationGeographicPoint(c2.x, c2.y);
		double distance = getDistance(c1, c2, calculator);
		if (returnVals != null && returnVals.length == 2) {
			returnVals[0] = distance;
			double angle = Math.toRadians(calculator.getAzimuth()); // Angle in range -PI to PI
			// Need to transform azimuth (in range -180 -> 180 and where 0 points north)
			// to standard mathematical (range 0 -> 360 and 90 points north)
			if (angle > 0 && angle < 0.5 * Math.PI) { // NE Quadrant
				angle = 0.5 * Math.PI - angle;
			} else if (angle >= 0.5 * Math.PI) { // SE Quadrant
				angle = (-angle) + 2.5 * Math.PI;
			} else if (angle < 0 && angle > -0.5 * Math.PI) { // NW Quadrant
				angle = (-1 * angle) + 0.5 * Math.PI;
			} else { // SW Quadrant
				angle = -angle + 0.5 * Math.PI;
			}
			returnVals[1] = angle;
		}
		return returnVals;
	}
	
	private List<Coordinate> getRoute() {
		// Lista que contiene todos los movimientos a hacer
		List<Coordinate> coordinateMovements = new ArrayList<>();

		Coordinate currentCoord = ContextCreator.getAgentGeography().getGeometry(this).getCoordinate();
		Coordinate destCoord = ContextCreator.getStreetProjection().getGeometry(this.destination).getCoordinate();
				
		// TODO: obtener la interseccion mas cercana a las coordenadas
		//ArrayList<Intersection> sourceIntersections = this.source.getIntersections();
		List<Intersection> sourceIntersections = getIntersections(this.source, currentCoord, true);
		//ArrayList<Intersection> destinationIntersections = this.destination.getIntersections();
		List<Intersection> destinationIntersections = getIntersections(this.destination, destCoord, false);
		
		// Busco el camino mas corto que pueda existir entre la combinacion de las esquinas
 		double shortestPathLength = Double.MAX_VALUE;
 		double pathLength = 0;
 		ShortestPath<Intersection> p;
 		List<RepastEdge<Intersection>> shortestPath = null;
 		Intersection currentIntersection = null, destinationIntersection = null;
 		for (Intersection s : sourceIntersections) {
 			for (Intersection d : destinationIntersections) {
 				p = new ShortestPath<Intersection>(ContextCreator.getStreetNetwork());
 				pathLength = p.getPathLength(s,d);
 				if (pathLength < shortestPathLength) {
 					shortestPathLength = pathLength;
 					shortestPath = p.getPath(s,d);
 					currentIntersection = s;
 					destinationIntersection = d;
 				}
 			}
 		}
 		
 		// Ver aca
 		

	// Ahora debo buscar el camino hacia cada una de las intersecciones elegidas
	// Intersection sourceIntersection = routeEndpoints[0];
	// Intersection destinationIntersection = routeEndpoints[1];
		if(currentIntersection != null || destinationIntersection != null) {
			// Buscar las coordenadas desde la posicion del agente hacia sourceIntersection
	 		Coordinate sourceIntersectionCoords = ContextCreator.getIntersectionGeography().getGeometry(currentIntersection).getCoordinate();
			List<Coordinate> coordsToIntersection = getCoordsToAPoint(currentCoord, sourceIntersectionCoords, this.source, true);
			coordinateMovements.addAll(coordsToIntersection);
			
		// Buscar las coordenadas del camino entre las intersections
			coordinateMovements.addAll(getCoordsBetweenTwoIntersections(shortestPath, currentIntersection, coordinateMovements));
			
		// Buscar las coordenadas desde la destIntersection hasta el destino
			List<Coordinate> coordsToDestination = getCoordsToAPoint(ContextCreator.getIntersectionGeography().getGeometry(destinationIntersection).getCoordinate(),
																	destCoord,
																	this.destination,
																	false);
			coordinateMovements.addAll(coordsToDestination);
			return coordinateMovements;
		}
		return null;
	}
	
	
	private List<Intersection> getIntersections(Street street, Coordinate currentCoord, boolean source) {
//		List<Intersection> ret = new ArrayList<>();
//		GeodeticCalculator calculator = new GeodeticCalculator(ContextCreator.getStreetProjection().getCRS());
//		List<Intersection> intersections = street.getIntersections();
//		double distance = 0;
//		for(Intersection intersection: intersections) {
//			distance = getDistance(currentCoord, ContextCreator.getIntersectionGeography().getGeometry(intersection).getCoordinate(), calculator);
//			if(distance < 115) {
//				ret.add(intersection);
//			}
//		}
//		return ret;
		List<Intersection> ret = new ArrayList<>();
		GeodeticCalculator calculator = new GeodeticCalculator(ContextCreator.getStreetProjection().getCRS());
		List<Intersection> intersections = street.getIntersections();
		Intersection intersection, in;
		double distance = 0;
		if(street.getDirection().equals("S") || street.getDirection().equals("O")) {
			Collections.reverse(intersections);
		}
		for(int i = 0; i < intersections.size(); i++) {
			intersection = intersections.get(i);
			distance = getDistance(currentCoord, ContextCreator.getIntersectionGeography().getGeometry(intersection).getCoordinate(), calculator);
			if(distance < 115) {
				if(source) {
					if(i < intersections.size()) { 
						ret.add(intersections.get(i+1));
					} else {
						ret.add(intersection);
					}
				} else {
					if(i > 0) {
						ret.add(intersections.get(i-1));
					} else {
						ret.add(intersection);
					}
				}
				return ret;
			}
		}
		return ret;
	}
	
	private List<Coordinate> getCoordsToAPoint(Coordinate currentCoords,
			Coordinate destinationStreetCoords,
			Street street,
			boolean findingIntersection) {
		List<Coordinate> ret = new ArrayList<>();
		Coordinate[] streetCoords = ContextCreator.getStreetProjection().getGeometry(street).getCoordinates();
		GeometryFactory geomFac = new GeometryFactory();
		// nuevo code
		Coordinate[] segmentCoords = new Coordinate[] {currentCoords, destinationStreetCoords};
		Geometry buffer = geomFac.createLineString(segmentCoords);
		Coordinate[] coordinatesTo = buffer.getCoordinates();
		for(int i = 0; i < coordinatesTo.length ; i++) {
			ret.add(coordinatesTo[i]);
		}	
		//end nuevo code
		
//		Point destinationPointGeom = geomFac.createPoint(destinationStreetCoords);
//		Point currentPointGeom = geomFac.createPoint(currentCoords);
//		for(int i = 0; i < streetCoords.length ; i++) {
//			Coordinate[] segmentCoords = new Coordinate[] { streetCoords[i], streetCoords[i + 1] };
//			Geometry buffer = geomFac.createLineString(segmentCoords).buffer(0.001);
//			if(findingIntersection) {
//				// Al buscar una interseccion, si dicho punto se encuentra dentro del buffer agrego todas las coordenadas que componen el segmento
//				// y finalmente agrego las coordenadas de destino
//				if (currentPointGeom.within(buffer)) {
//					for (int j = i + 1; j < streetCoords.length; j++) {
//						ret.add(streetCoords[j]);
//					}
//					ret.add(destinationStreetCoords);
//					break;
//				}
//			} else {
//				// Al no buscar una interseccion se agrega la coordenada, y si dentro del buffer esta el el punto de destino se lo agrega, de modo
//				// que finalmente se llegaria al destino
//				ret.add(streetCoords[i]);
//				if (destinationPointGeom.within(buffer)) {
//					ret.add(destinationStreetCoords);
//					break;
//				}
//			}
//		}
		return ret;
	}
	
	private List<Coordinate> getCoordsBetweenTwoIntersections(List<RepastEdge<Intersection>> shortestPath, Intersection sourceIntersection, List<Coordinate> coordinateMovements) {
		List<Coordinate> ret = new ArrayList<>();
		StreetNetworkEdge<Intersection> edge = null;
		Street street;
		// Indica la direccion que toma el agente por el grafo, es decir si primero va por el origen hacia el destino
		// si la primera onterseccion es la origen sera verdadero
		boolean sourceFirst;
		// Itero sobre el camino mas corto encontrado previamente
		for (int i = 0; i < shortestPath.size(); i++) {
			edge = (StreetNetworkEdge<Intersection>) shortestPath.get(i);
//			if (i == 0) {
//				// No coords in route yet, compare the source to the starting junction
//				sourceFirst = (edge.getSource().equals(sourceIntersection)) ? true : false;
//			} else {
//				// Otherwise compare the source to the last coord added to the list
//				sourceFirst = (edge.getSource().getCoord().equals(coordinateMovements.get(coordinateMovements.size() - 1))) ? true
//						: false;
//			}
//			street = edge.getStreet();
//			// Si el arco no tiene una calle asociada solo agrego las intersecciones
//			if (street == null) {
//				if (sourceFirst) {
//					ret.add(edge.getSource().getCoord());
//					ret.add(edge.getTarget().getCoord());
//				} else {
//					ret.add(edge.getTarget().getCoord());
//					ret.add(edge.getSource().getCoord());
//				}
//			} else {
//				// Anado todas las coordenadas que forman la geometria de la calle
//				Coordinate[] roadCoords = ContextCreator.getStreetProjection().getGeometry(street).getCoordinates();
//				if (!sourceFirst) {
//					ArrayUtils.reverse(roadCoords);
//				}
//				for (int j = 0; j < roadCoords.length; j++) {
//					ret.add(roadCoords[j]);
//				}
//			}
			ret.add(edge.getSource().getCoord());
		}
		if(edge != null) {
			ret.add(edge.getTarget().getCoord());
		}
		return ret;
	}
	
	public boolean atDestination() {
		return ContextCreator.getAgentGeography().getGeometry(this).getCoordinate().equals(this.destination);
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
