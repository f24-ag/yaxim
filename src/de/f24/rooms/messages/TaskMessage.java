package de.f24.rooms.messages;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;


public class TaskMessage extends RoomsMessage {
	public String getText() throws JSONException {
		return getBody().getString("text");
	}

	public void setText(String text) throws JSONException {
		getBody().put("text", text);
	}
	
	public List<String> getOptions() {
		List<String> options = new ArrayList<String>();
		try {
			for (int i = 0; i < getBody().getJSONArray("options").length(); i++) {
				options.add(getBody().getJSONArray("options").getString(i));
			}
		}
		catch (JSONException ex) {
			
		}
		return options;
	}

	public void setOptions(List<String> options) throws JSONException {
		JSONArray array = new JSONArray();
		for (String l : options) {
			array.put(l);
		}
		getBody().put("options", array);
	}

	@Override
	public RoomsMessageType getType() {
		return RoomsMessageType.Task;
	}
}
