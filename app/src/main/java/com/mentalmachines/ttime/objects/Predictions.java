package com.mentalmachines.ttime.objects;


public class Predictions {
	/*@Attribute
	String agencyTitle;
	@Attribute
	String routeTag;
	@Attribute
	Integer routeCode;
	@Attribute
	String routeTitle;
	@Attribute
	String stopTitle;

	@Element(name="direction")
	List<Direction> direction;	

	@Element(name="message")
	Message message;

	class Direction {
		@Element(name="title")	
		String title; 
		@ElementList(entry="direction", inline=true)	
		List<Prediction> prediction;
	}

	class Prediction {
		@Attribute
		Integer seconds;
		@Attribute
		Integer minutes;
		@Attribute
		String epochTime;
		@Attribute
		Boolean isDeparture;
		@Attribute
		String dirTag;
		@Attribute
		Integer block;
	}*/

	class Message {
		String text;
	}
}
