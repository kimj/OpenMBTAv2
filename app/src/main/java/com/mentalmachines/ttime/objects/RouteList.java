package com.mentalmachines.ttime.objects;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;


@Root(name="body")
public class RouteList {
	@Attribute
	String copyright;
	
	@ElementList(inline=true)
	List<Route> body = new ArrayList<Route>();

}

