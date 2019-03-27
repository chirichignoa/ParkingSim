package trafficSim.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.ArrayUtils;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.referencing.GeodeticCalculator;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import repast.simphony.context.Context;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.gis.util.GeometryUtil;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;
import repast.simphony.space.gis.ShapefileLoader;
import repast.simphony.space.gis.SimpleAdder;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
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

	private int numAgents = 1;
	
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
		// String roadFile = gisDataDir + "SelectedRoads.shp";
		String roadFile = gisDataDir + "SelectedRoads.shp";
		try {
			loadFeatures(roadFile, streetContext, streetProjection);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "", e);
			return null;
		}
		mainContext.addSubContext(streetContext);
		
		intersectionContext = new IntersectionContext();
		mainContext.addSubContext(intersectionContext);
		intersectionGeography = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
				"IntersectionGeography", intersectionContext,
				new GeographyParameters<Intersection>(new SimpleAdder<Intersection>()));
		
		NetworkBuilder<Intersection> builder = new NetworkBuilder<>("StreetNetwork", intersectionContext, true);
		builder.setEdgeCreator(new NetworkdEdgeCreator<Intersection>());
		streetNetwork = builder.buildNetwork();
		//build RoadNetwork
		buildRoadNetwork(streetProjection, intersectionContext, intersectionGeography, streetNetwork);
		
//		checkRoadNetwork(streetNetwork);
				
		// Create the Agents - context and geograph
		agentContext = new AgentContext();
		agentGeography = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
				"AgentGeography", agentContext,
				new GeographyParameters<CarAgent>(new SimpleAdder<CarAgent>()));

		for(int i = 0; i < this.numAgents; i++) {
			Street source = streetContext.getRandomObject();
			CarAgent agent = new CarAgent(i, source);
			agentContext.add(agent);
//			agentGeography.move(agent, null);
			agentGeography.move(agent, streetProjection.getGeometry(agent.getSource()).getCentroid());
		}
		
		
		mainContext.addSubContext(agentContext);
		
	
		return mainContext;	
	}
	
//	private void checkRoadNetwork(Network<Intersection> streetNetwork) {
////		Iterable<Intersection> streetIt = streetNetwork.getNodes();
////		for(Intersection s: streetIt) {
////			System.out.println(s.toString());
////			System.out.println(streetNetwork.getAdjacent(s));
////			
////		}
//		Iterable<RepastEdge<Intersection>> edges = streetNetwork.getEdges();
//		for(RepastEdge<Intersection> e : edges) {
//			System.out.println("Source => " + e.getSource());
//			System.out.println("Target => " + e.getTarget());
//			System.out.println("IsDirected => " + e.isDirected());
//		}
//	}
	
	private void buildRoadNetwork(Geography<Street> roadProjection, Context<Intersection> intersectionContext,
			Geography<Intersection> intersectionGeography, Network<Intersection> streetNetwork) {
		// Para poder crear lineas y puntos desde la interseccion de las calles
		GeometryFactory geomFac = new GeometryFactory();
		// Registro de las intersecciones creadas para reutilizar
		HashMap<Coordinate, Intersection> coordMap = new HashMap<Coordinate, Intersection>();
		
		Iterable<Street> roadIt = roadProjection.getAllObjects();
		for (Street street : roadIt) {
			if(!street.getName().equals("")) {

				generateIntersections(street, coordMap);
			}
		}
		System.out.println("coordMap -> " + coordMap.size());
	}
	
	private void generateIntersections(Street street, HashMap<Coordinate, Intersection> coordMap) {
		Intersection previousIntersection = null;
		Coordinate[] coords = street.getCoords();
		// Iterate over the street coords, creating a interection for each one
		if(street.getDirection().equals("S") || street.getDirection().equals("O")) {
			ArrayUtils.reverse(coords);
		}
		for(Coordinate c: coords) {
			Intersection intersection = createIntersection(intersectionContext, intersectionGeography, c, coordMap);
			street.addIntersection(intersection);
			intersection.addRoad(street);
			if(previousIntersection != null) {
				//Chequear para que lado va la calle
				// TODO: hacer lo del sentido
				StreetNetworkEdge<Intersection> edge = new StreetNetworkEdge<>(previousIntersection,
																				intersection,
																				street.getOneWay().equals("T"),
																				calculateDistance(previousIntersection,intersection));
				edge.setStreet(street);
				intersection.addPrevious(previousIntersection);
				previousIntersection.addNext(intersection);
				// Finalmente añado el arco a la red
				if (!streetNetwork.containsEdge(edge)) {
					streetNetwork.addEdge(edge);
				} 
				if(street.getOneWay().equals("F")) {
					edge = new StreetNetworkEdge<>(intersection,
							previousIntersection,
							street.getOneWay().equals("T"),
							calculateDistance(intersection, previousIntersection));
					edge.setStreet(street);
					if (!streetNetwork.containsEdge(edge)) {
						streetNetwork.addEdge(edge);
					} 
				}
				
			}
			previousIntersection = intersection;
		}
	}
	
	private double calculateDistance(Intersection previousIntersection, Intersection intersection) {
		GeodeticCalculator calculator = new GeodeticCalculator(ContextCreator.getStreetProjection().getCRS());
		calculator.setStartingGeographicPoint(previousIntersection.getCoord().x, previousIntersection.getCoord().y);
		calculator.setDestinationGeographicPoint(intersection.getCoord().x, intersection.getCoord().y);
		return Math.abs(calculator.getOrthodromicDistance());
	}

	public Intersection createIntersection(Context<Intersection> intersectionContext,
			Geography<Intersection> intersectionGeography, Coordinate coord, HashMap<Coordinate, Intersection> coordMap ) {
		if (coordMap.containsKey(coord)) {
			// A Junction with those coordinates (c1) has been created, get it so we can add an edge to it
			return coordMap.get(coord);
		} else { // Junction does not exit
			Intersection intersection = new Intersection();
			GeometryFactory geomFac = new GeometryFactory();
			intersection.setCoord(coord);
			intersectionContext.add(intersection);
//			intersection.setNext(null);
			coordMap.put(coord, intersection);
			Point p1 = geomFac.createPoint(coord);
			intersectionGeography.move(intersection, p1);
			return intersection;
		}
	}
	
	private List<SimpleFeature> loadFeaturesFromShapefile(String filename){
		System.out.println("Extracting features");
		URL url = null;
		try {
			url = new File(filename).toURL();
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}

		List<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		// Try to load the shapefile
		SimpleFeatureIterator fiter = null;
		ShapefileDataStore store = null;
		store = new ShapefileDataStore(url);

		try {
			fiter = store.getFeatureSource().getFeatures().features();

			while(fiter.hasNext()){
				features.add(fiter.next());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			fiter.close();
			store.dispose();
		}
		
		return features;
	}
	
	/**
	 * Loads features from the specified shapefile.  The appropriate type of agents
	 * will be created depending on the geometry type in the shapefile
	 * 
	 * @param filename the name of the shapefile from which to load agents
	 * @param context the context
	 * @param geography the geography
	 */
	private void loadFeatures (String filename, Context context, Geography geography){

		List<SimpleFeature> features = loadFeaturesFromShapefile(filename);
		String name;
		// For each feature in the file
		for (SimpleFeature feature : features){
			Geometry geom = (Geometry)feature.getDefaultGeometry();

			if (!geom.isValid()){
				System.out.println("Invalid geometry: " + feature.getID());
			}
			MultiLineString line = (MultiLineString)feature.getDefaultGeometry();
			geom = (LineString)line.getGeometryN(0);
			Coordinate[] coords = line.getCoordinates();
			name = (String)feature.getAttribute("name");
			if(!name.equals("")) {
				Street street = new Street(Integer.parseInt((String)feature.getAttribute("osm_id")));
				street.setName(name);
				street.setOneWay((String)feature.getAttribute("oneway"));
				street.setDirection((String)feature.getAttribute("direction"));
				street.setCoords(coords);
				context.add(street);
				geography.move(street, geom);
			}
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
