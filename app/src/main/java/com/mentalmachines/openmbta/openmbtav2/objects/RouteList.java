package com.mentalmachines.openmbta.openmbtav2.objects;

import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;


@Root(name="body")
public class RouteList {
	@Attribute
	String copyright;
	
	@ElementList(inline=true)
	List<Route> body = new ArrayList<Route>();

}

