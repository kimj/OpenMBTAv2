package com.mentalmachines.ttime.objects;

public class Schedule {
	Route route;
	
	class Route{
		String tag;
		String title;
		String scheduleClass;
		String serviceClass;	
		String direction;
		
		Header header;
	}
	
	class Header{
		Stop stop;
	}
	
	class Stop{
		String tag;
		String text;
	}
}