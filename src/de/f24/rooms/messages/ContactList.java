package de.f24.rooms.messages;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ContactList extends RoomsMessage {
	
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
	
	public List<Participant> getContacts() throws JSONException {
		return getParticipantList("contacts");
	}

	public void setParticipants(List<Participant> contacts) throws JSONException {
		setParticipantList("contacts", contacts);
	}

	@Override
	public RoomsMessageType getType() {
		return RoomsMessageType.ContactList;
	}
}
