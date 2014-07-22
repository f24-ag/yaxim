package de.f24.rooms.messages;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

public class OpenRoomRequest extends RoomsMessage {
	public List<String> getLoggers() throws JSONException {
		List<String> values = new ArrayList<String>();
		for (int i = 0; i < getBody().getJSONArray("loggers").length(); i++) {
			values.add(getBody().getJSONArray("loggers").getString(i));
		}
		return values;
	}

	public void setLoggers(List<String> loggers) throws JSONException {
		JSONArray array = new JSONArray();
		for (String l : loggers) {
			array.put(l);
		}
		getBody().put("loggers", array);
	}

	public String getRoomName() throws JSONException {
		return getBody().getString("name");
	}

	public void setRoomName(String roomName) throws JSONException {
		getBody().put("name", roomName);
	}

	public List<String> getParticipants() throws JSONException {
		List<String> values = new ArrayList<String>();
		for (int i = 0; i < getBody().getJSONArray("participants").length(); i++) {
			values.add(getBody().getJSONArray("participants").getString(i));
		}
		return values;
	}

	public void setParticipants(List<String> participants) throws JSONException {
		JSONArray array = new JSONArray();
		for (String p : participants) {
			array.put(p);
		}
		getBody().put("participants", array);
	}

	public void setParticipantList(List<Participant> participants) throws JSONException {
		JSONArray array = new JSONArray();
		for (Participant p : participants) {
			array.put(p.getJid());
		}
		getBody().put("participants", array);
	}

	@Override
	public RoomsMessageType getType() {
		return RoomsMessageType.OpenRoomRequest;
	}
}
