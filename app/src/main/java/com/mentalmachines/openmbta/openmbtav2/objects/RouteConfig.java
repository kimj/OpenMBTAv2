package com.mentalmachines.openmbta.openmbtav2.objects;

import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

public class RouteConfig {
	@Attribute
	String copyright;
	
	@ElementList(inline=true)
	public List<Route> route = new ArrayList<Route>();
	
	@Element(name="route")
	public static class Route{
		@Attribute
		String tag;
		@Attribute
		String title;
		@Attribute
		String color;
		@Attribute
		String oppositeColor;
		@Attribute
		Double latMin, latMax, lonMin, lonMax;
	
		@ElementList(entry="stop", inline=true)
		List<StopFull> stop = new ArrayList<StopFull>();
		@ElementList(inline=true)
		List<Direction> direction = new ArrayList<Direction>();
		@ElementList(inline=true)
		List<Path> path = new ArrayList<Path>();
		
	}
	
	static class StopFull {
		@Attribute
		String tag;
		@Attribute
		String title;
		@Attribute
		Double lat, lon;
		@Attribute(required=false)
		String stopId;
	}
	
	static class Direction{
		@Attribute
		String tag;
		@Attribute
		String title;
		@Attribute
		String name;
		@Attribute
		String useForUI;
		
		@ElementList(entry="stop", inline=true)
		List<StopTag> stopTags;
	}

	static class StopTag{
		@Attribute
		String tag;
	}
	
	static class Path{
		@ElementList(entry="point", inline=true)
		List<Point> points;
		
	}
	
	static class Point{
		@Attribute
		Double lat, lon;
	}
}