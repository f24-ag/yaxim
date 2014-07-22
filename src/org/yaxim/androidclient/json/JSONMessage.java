package org.yaxim.androidclient.json;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONMessage extends JSONObject {
	public JSONMessage() {
		super();
		try {
			this.put("header", new JSONObject());
			this.put("body", new JSONObject());
		}
		catch (JSONException ex) {
			ex.printStackTrace();
		}
	}
	
	public JSONMessage(String message) throws JSONException {
		super(message);
	}
	
	public JSONObject getHeader() {
		try {
			return getJSONObject("header");
		} 
		catch (JSONException ex) {
			return null;
		}
	}

	public JSONObject getBody() throws JSONException {
		return getJSONObject("body");
	}
	
	public String getSender() throws JSONException {
		return getHeader().getString("sender");
	}
	
	public void setSender(String jid) throws JSONException {
		getHeader().put("sender", jid);
	}
	
	public List<String> getRecipients() {
		List<String> receipients = new ArrayList<String>();
		try {
			for (int i = 0; i < getHeader().getJSONArray("receipients").length(); i++) {
				receipients.add(getHeader().getJSONArray("receipients").getString(i));
			}
		} 
		catch (JSONException ex) {
			ex.printStackTrace();
		}
		return receipients;
	}
}
