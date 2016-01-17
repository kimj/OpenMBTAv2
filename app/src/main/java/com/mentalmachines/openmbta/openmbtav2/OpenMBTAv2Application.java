package com.mentalmachines.openmbta.openmbtav2;

import android.app.Application;

public class OpenMBTAv2Application extends Application{
	private static OpenMBTAv2Application instance;
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		instance = this;
		// Initialize the singletons so their instances
		// are bound to the application process.
		initSingletons();
	}
	protected void initSingletons()
	{}
	
	public static OpenMBTAv2Application getApplication() {
		return instance;
	}
}
