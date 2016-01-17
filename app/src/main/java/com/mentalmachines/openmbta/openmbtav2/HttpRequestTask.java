package com.mentalmachines.openmbta.openmbtav2;

import android.os.AsyncTask;

public class HttpRequestTask extends AsyncTask<String, String, String> {
	protected OpenMBTAv2Application app;
	public String xmlResponse;
	protected String doInBackground(String... requestStrings) {
		return null;
	}

	protected void onPostExecute(String result) {
		xmlResponse = result;
	}

	protected void onProgressUpdate(Integer... progress) {}
}