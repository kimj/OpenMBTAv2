package com.mentalmachines.openmbta.openmbtav2;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Application;

public class OpenMBTAv2Application extends Application{
	private static OpenMBTAv2Application instance;
	public HttpClient httpClient;
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		instance = this;
		httpClient = new DefaultHttpClient();
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
