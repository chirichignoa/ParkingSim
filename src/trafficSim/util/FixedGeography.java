package trafficSim.util;

import com.vividsolutions.jts.geom.Coordinate;

public interface FixedGeography {
	Coordinate getCoord();
	void setCoord(Coordinate c);	
}
