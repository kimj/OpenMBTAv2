package com.mentalmachines.openmbta.openmbtav2;

import android.os.AsyncTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;

public class HttpRequestTask extends AsyncTask<String, String, String> {
	protected OpenMBTAv2Application app;
	public String xmlResponse;
	
	protected String doInBackground(String... requestStrings) {
		app = (OpenMBTAv2Application) OpenMBTAv2Application.getApplication();
	    HttpClient client = new DefaultHttpClient();
        HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000); //Timeout Limit
        HttpResponse response;
        
        HttpEntity entity;
        String URL = requestStrings[0];
        try {
        	HttpGet get = new HttpGet(URL);
            response = client.execute(get);
            /*Checking response */
            if(response != null){
            	entity = response.getEntity();
                xmlResponse = EntityUtils.toString(entity);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
		return xmlResponse;
	}

	protected void onProgressUpdate(Integer... progress) {
		// TODO You are on the GUI thread, and the first element in 
		// the progress parameter contains the last progress
		// published from doInBackground, so update your GUI
	}

	protected void onPostExecute(String result) {
		xmlResponse = result;
	}
}
