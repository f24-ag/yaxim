package de.f24.rooms.messages;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RoomConfiguration extends RoomsMessage {
	
	private List<Participant> getParticipantList(String property) throws JSONException {
		List<Participant> participants = new ArrayList<Participant>();
		for (int i = 0; i < getBody().getJSONArray(property).length(); i++) {
			JSONObject partObject = getBody().getJSONArray(property).getJSONObject(i);
			participants.add(new Participant(partObject.getString("jid"), partObject.getString("name"), partObject.getString("public-key")));
		}
		return participants;
	}
	
	private void setParticipantList(String property, List<Participant> participants) throws JSONException {
		JSONArray partArray = new JSONArray();
		for (Participant p : participants) {
			partArray.put(p.toObject());
		}
		getBody().put(property, partArray);
	}
	
	public String getRoomName() throws JSONException {
		return getBody().getString("name");
	}

	public void setRoomName(String roomName) throws JSONException {
		getBody().put("name", roomName);
	}

	public String getRoomID() throws JSONException {
		return getBody().getString("room-id");
	}

	public void setRoomID(String roomID) throws JSONException {
		getBody().put("room-id", roomID);
	}

	public List<Participant> getParticipants() throws JSONException {
		return getParticipantList("participants");
	}

	public void setParticipants(List<Participant> participants) throws JSONException {
		setParticipantList("participants", participants);
	}

	public List<Participant> getLoggers() throws JSONException {
		return getParticipantList("loggers");
	}

	public void setLoggers(List<Participant> participants) throws JSONException {
		setParticipantList("loggers", participants);
	}

	@Override
	public RoomsMessageType getType() {
		return RoomsMessageType.RoomsConfiguration;
	}
}
