package de.f24.rooms.messages;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class RoomsMessage {
	protected JSONObject jsonMessage;
	
	static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");

	public RoomsMessage() {
		jsonMessage = new JSONObject();
		try {
			JSONObject header = new JSONObject();
			header.put("type", getType().getValue());
			header.put("version", 1);
			header.put("id", UUID.randomUUID().toString());
			header.put("timestamp", dateFormat.format(new Date()));
			jsonMessage.put("header", header);
			jsonMessage.put("body", new JSONObject());
		} catch (JSONException ex) {
			// Do nothing
		}
	}
	
	public abstract RoomsMessageType getType();
	
	public void setJSONMessage(JSONObject message) {
		this.jsonMessage = message;
	}

	public JSONObject getHeader() throws JSONException {
		return jsonMessage.getJSONObject("header");
	}

	public JSONObject getBody() throws JSONException {
		return jsonMessage.getJSONObject("body");
	}

	public List<String> getRecipients() {
		try {
			List<String> receipients = new ArrayList<String>();
			for (int i = 0; i < getHeader().getJSONArray("recipients").length(); i++) {
				receipients.add(getHeader().getJSONArray("recipients")
						.getJSONObject(i).getString("jid"));
			}
			return receipients;
		}
		catch (JSONException ex) {
			return null;
		}
	}

	public void setRecipients(List<String> recipients) throws JSONException {
		JSONArray array = new JSONArray();
		for (String r : recipients) {
			JSONObject recipient = new JSONObject();
			recipient.put("jid", r);
			array.put(recipient);
		}
		getHeader().put("recipients", array);
	}

	public String getSender() throws JSONException {
		return getHeader().getJSONObject("sender").getString("jid");
	}

	public void setSender(String from) throws JSONException {
		JSONObject sender = new JSONObject();
		sender.put("jid", from);
		getHeader().put("sender", sender);
	}
	
	public String toString() {
		return jsonMessage.toString();
	}
}
