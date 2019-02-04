package trafficSim.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import repast.simphony.context.Context;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;
import repast.simphony.space.gis.ShapefileLoader;
import repast.simphony.space.gis.SimpleAdder;
import repast.simphony.space.graph.Network;

import trafficSim.agents.CarAgent;
import trafficSim.contexts.AgentContext;
import trafficSim.contexts.IntersectionContext;
import trafficSim.contexts.StreetContext;
import trafficSim.model.Intersection;
import trafficSim.model.StreetNetworkEdge;
import trafficSim.model.Street;
import trafficSim.util.FixedGeography;
import trafficSim.util.NetworkdEdgeCreator;


public class ContextCreator implements ContextBuilder<Object> {
	
	private static Logger LOGGER = Logger.getLogger(ContextCreator.class.getName());
	
	private static Context<Object> mainContext;
	
	private static Context<Street> streetContext;
	private static Geography<Street> streetProjection;
	
	private static Context<CarAgent> agentContext;
	private static Geography<CarAgent> agentGeography;
	
	private static Context<Intersection> intersectionContext;
	private static Geography<Intersection> intersectionGeography;
	private static Network<Intersection> streetNetwork;

	private int numAgents = 5;
	
	@Override
	public Context<Object> build(Context<Object> context) {
		
		mainContext = context;
		
		// Configure the environment
		String gisDataDir = "./data/gis_data/";
		LOGGER.log(Level.FINE, "Configuring the environment with data from " + gisDataDir);
		
		// Create the Roads - context and geography
		streetContext = new StreetContext();
		streetProjection = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
				"StreetGeography", streetContext,
				new GeographyParameters<Street>(new SimpleAdder<Street>()));
		String roadFile = gisDataDir + "SelectedRoads.shp";
		try {
			readShapefile(Street.class, roadFile, streetProjection, streetContext);
		} catch (MalformedURLException | FileNotFoundException e) {
			LOGGER.log(Level.SEVERE, "", e);
			return null;
		}
		mainContext.addSubContext(streetContext);
		
		intersectionContext = new IntersectionContext();
		mainContext.addSubContext(intersectionContext);
		intersectionGeography = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
				"IntersectionGeography", intersectionContext,
				new GeographyParameters<Intersection>(new SimpleAdder<Intersection>()));
		
		NetworkBuilder<Intersection> builder = new NetworkBuilder<>("StreetNetwork", intersectionContext, false);
		builder.setEdgeCreator(new NetworkdEdgeCreator<Intersection>());
		streetNetwork = builder.buildNetwork();
		//build RoadNetwork
		buildRoadNetwork(streetProjection, intersectionContext, intersectionGeography, streetNetwork);
		
		// Create the Agents - context and geograph
		agentContext = new AgentContext();
		agentGeography = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
				"AgentGeography", agentContext,
				new GeographyParameters<CarAgent>(new SimpleAdder<CarAgent>()));

		for(int i = 0; i < this.numAgents; i++) {
			CarAgent agent = new CarAgent(i);
			agent.setSource(streetContext.getRandomObject());
			agentContext.add(agent);
			agentGeography.move(agent, null);
			agentGeography.move(agent, streetProjection.getGeometry(agent.getSource()).getCentroid());
		}
		
		
		mainContext.addSubContext(agentContext);
		
	
		return mainContext;	
	}
	
	private void buildRoadNetwork(Geography<Street> roadProjection, Context<Intersection> intersectionContext,
			Geography<Intersection> intersectionGeography, Network<Intersection> streetNetwork) {
		// Para poder crear lineas y puntos desde la interseccion de las calles
		GeometryFactory geomFac = new GeometryFactory();
		// Registro de las intersecciones creadas para reutilizar
		HashMap<Coordinate, Intersection> coordMap = new HashMap<Coordinate, Intersection>();
		
		Iterable<Street> roadIt = roadProjection.getAllObjects();
		for (Street street : roadIt) {
			Geometry geo = roadProjection.getGeometry(street);
			Coordinate c1 = geo.getCoordinates()[0]; // Coord 1
			Coordinate c2 = geo.getCoordinates()[geo.getNumPoints() - 1]; //Coord 2
			
			Intersection inter1 = createIntersection(intersectionContext, intersectionGeography, c1, coordMap);
			Intersection inter2 = createIntersection(intersectionContext, intersectionGeography, c2, coordMap);
			
			//Asigno las intersecciones a la calle
			street.addIntersection(inter1);
			street.addIntersection(inter2);
			//Asigno la calle a cada una de las intersecciones
			inter1.addRoad(street);
			inter2.addRoad(street);
			
			//Genero el arco en la red entre ambas intersecciones con el peso de la longitud de la calle
			StreetNetworkEdge<Intersection> edge = new StreetNetworkEdge<Intersection>(inter1, inter2, false, geo.getLength());
			//Asigno el arco a la calle y la calle al arc
			street.setEdge(edge);
			edge.setStreet(street);
			
			// Finalmente añado el arco a la red
			if (!streetNetwork.containsEdge(edge)) {
				streetNetwork.addEdge(edge);
			} 
		};
		
	}
	
	public Intersection createIntersection(Context<Intersection> intersectionContext,
			Geography<Intersection> intersectionGeography, Coordinate coord, HashMap<Coordinate, Intersection> coordMap ) {
		if (coordMap.containsKey(coord)) {
			// A Junction with those coordinates (c1) has been created, get it so we can add an edge to it
			return coordMap.get(coord);
		} else { // Junction does not exit
			Intersection intersection = new Intersection();
			GeometryFactory geomFac = new GeometryFactory();
			intersection.setCoords(coord);
			intersectionContext.add(intersection);
			coordMap.put(coord, intersection);
			Point p1 = geomFac.createPoint(coord);
			intersectionGeography.move(intersection, p1);
			return intersection;
		}
	}

	public <T extends FixedGeography> void readShapefile(Class<T> cl, String shapefileLocation, Geography<T> geog, Context<T> context) 
			throws MalformedURLException, FileNotFoundException {
		File shapefile = null;
		ShapefileLoader<T> loader = null;
		shapefile = new File(shapefileLocation);
		if (!shapefile.exists()) {
			throw new FileNotFoundException("Could not find the given shapefile: " + shapefile.getAbsolutePath());
		}
		loader = new ShapefileLoader<T>(cl, shapefile.toURI().toURL(), geog, context);
		while (loader.hasNext()) {
			loader.next();
		}
		for (T obj : context.getObjects(cl)) {
			obj.setCoords(geog.getGeometry(obj).getCentroid().getCoordinate());
		}
	}
	
	public static Context<Object> getMainContext() {
		return mainContext;
	}

	public static void setMainContext(Context<Object> mainContext) {
		ContextCreator.mainContext = mainContext;
	}

	public static Context<Street> getStreetContext() {
		return streetContext;
	}

	public static void setStreetContext(Context<Street> streetContext) {
		ContextCreator.streetContext = streetContext;
	}

	public static Geography<Street> getStreetProjection() {
		return streetProjection;
	}

	public static void setStreetProjection(Geography<Street> streetProjection) {
		ContextCreator.streetProjection = streetProjection;
	}

	public static Context<CarAgent> getAgentContext() {
		return agentContext;
	}

	public static void setAgentContext(Context<CarAgent> agentContext) {
		ContextCreator.agentContext = agentContext;
	}

	public static Geography<CarAgent> getAgentGeography() {
		return agentGeography;
	}

	public static void setAgentGeography(Geography<CarAgent> agentGeography) {
		ContextCreator.agentGeography = agentGeography;
	}

	public static Context<Intersection> getIntersectionContext() {
		return intersectionContext;
	}

	public static void setIntersectionContext(Context<Intersection> intersectionContext) {
		ContextCreator.intersectionContext = intersectionContext;
	}

	public static Geography<Intersection> getIntersectionGeography() {
		return intersectionGeography;
	}

	public static void setIntersectionGeography(Geography<Intersection> intersectionGeography) {
		ContextCreator.intersectionGeography = intersectionGeography;
	}

	public static Network<Intersection> getStreetNetwork() {
		return streetNetwork;
	}

	public static void setStreetNetwork(Network<Intersection> streetNetwork) {
		ContextCreator.streetNetwork = streetNetwork;
	}

}
