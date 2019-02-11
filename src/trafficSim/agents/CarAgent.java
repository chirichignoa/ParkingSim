package trafficSim.agents;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import org.apache.commons.lang3.ArrayUtils;

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
		// getRoute();
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
//		ArrayList<Coordinate> routeX = new ArrayList<>();
//		ArrayList<Street> streetsX = new ArrayList<>();
//		ArrayList<String> routeDescriptionX = new ArrayList<>();
//		ArrayList<Double> routeSpeedsX = new ArrayList<>();
		// Lista que contiene todos los movimientos a hacer
		List<Coordinate> coordinateMovements = new ArrayList<>();

		Coordinate currentCoord = ContextCreator.getAgentGeography().getGeometry(this).getCoordinate();
		Coordinate destCoord = ContextCreator.getStreetProjection().getGeometry(this.destination).getCoordinate();
		
		// TODO: chequear si es que las coordenadas no referencian a una calle, encontrar la calle mas cercana a ese punto.
		
		ArrayList<Intersection> sourceIntersections = this.source.getIntersections();
		ArrayList<Intersection> destinationIntersections = this.destination.getIntersections();
		
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

	// Ahora debo buscar el camino hacia cada una de las intersecciones elegidas
	// Intersection sourceIntersection = routeEndpoints[0];
	// Intersection destinationIntersection = routeEndpoints[1];
		
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

	}
	
	private List<Coordinate> getCoordsToAPoint(Coordinate currentCoords, Coordinate destinationStreetCoords, Street street, boolean findingIntersection) {
		List<Coordinate> ret = new ArrayList<>();
		Coordinate[] streetCoords = ContextCreator.getStreetProjection().getGeometry(street).getCoordinates();
		GeometryFactory geomFac = new GeometryFactory();
		Point destinationPointGeom = geomFac.createPoint(destinationStreetCoords);
		Point currentPointGeom = geomFac.createPoint(currentCoords);
		for(int i = 0; i < streetCoords.length ; i++) {
			Coordinate[] segmentCoords = new Coordinate[] { streetCoords[i], streetCoords[i + 1] };
			Geometry buffer = geomFac.createLineString(segmentCoords).buffer(0.001);
			if(findingIntersection) {
				// Al buscar una interseccion, si dicho punto se encuentra dentro del buffer agrego todas las coordenadas que componen el segmento
				// y finalmente agrego las coordenadas de destino
				if (currentPointGeom.within(buffer)) {
					for (int j = i + 1; j < streetCoords.length; j++) {
						ret.add(streetCoords[j]);
					}
					ret.add(destinationStreetCoords);
					break;
				}
			} else {
				// Al no buscar una interseccion se agrega la coordenada, y si dentro del buffer esta el el punto de destino se lo agrega, de modo
				// que finalmente se llegaria al destino
				ret.add(streetCoords[i]);
				if (destinationPointGeom.within(buffer)) {
					ret.add(destinationStreetCoords);
					break;
				}
			}
		}
		return ret;
	}
	
	private List<Coordinate> getCoordsBetweenTwoIntersections(List<RepastEdge<Intersection>> shortestPath, Intersection sourceIntersection, List<Coordinate> coordinateMovements) {
		List<Coordinate> ret = new ArrayList<>();
		StreetNetworkEdge<Intersection> edge;
		Street street;
		// Indica la direccion que toma el agente por el grafo, es decir si primero va por el origen hacia el destino
		// si la primera onterseccion es la origen sera verdadero
		boolean sourceFirst;
		// Itero sobre el camino mas corto encontrado previamente
		for (int i = 0; i < shortestPath.size(); i++) {
			edge = (StreetNetworkEdge<Intersection>) shortestPath.get(i);
			if (i == 0) {
				// No coords in route yet, compare the source to the starting junction
				sourceFirst = (edge.getSource().equals(sourceIntersection)) ? true : false;
			} else {
				// Otherwise compare the source to the last coord added to the list
				sourceFirst = (edge.getSource().getCoords().equals(coordinateMovements.get(coordinateMovements.size() - 1))) ? true
						: false;
			}
			street = edge.getStreet();
			// Si el arco no tiene una calle asociada solo agrego las intersecciones
			if (street == null) {
				if (sourceFirst) {
					ret.add(edge.getSource().getCoords());
					ret.add(edge.getTarget().getCoords());
				} else {
					ret.add(edge.getTarget().getCoords());
					ret.add(edge.getSource().getCoords());
				}
			} else {
				// Añado todas las coordenadas que forman la geometria de la calle
				Coordinate[] roadCoords = ContextCreator.getStreetProjection().getGeometry(street).getCoordinates();
				if (!sourceFirst) {
					ArrayUtils.reverse(roadCoords);
				}
				for (int j = 0; j < roadCoords.length; j++) {
					ret.add(roadCoords[j]);
				}
			}
		}
		return ret;
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
