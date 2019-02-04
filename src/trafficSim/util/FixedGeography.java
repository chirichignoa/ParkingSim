package trafficSim.util;

import com.vividsolutions.jts.geom.Coordinate;

public interface FixedGeography {
	Coordinate getCoords();
	void setCoords(Coordinate c);	
}
