package com.mentalmachines.ttime.objects.gtfs;

public class TransitCalendarDates {

	public String service_id;
	public String date;
	public String exception_type;

	public void FromString(String str){
		String[] temp;
		temp = str.split(",");
        
		this.service_id = temp[0];
        this.date = temp[1];
        this.exception_type = temp[2];
		
	}
}
