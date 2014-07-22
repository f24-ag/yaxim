package de.f24.rooms.messages;

import org.json.JSONException;


public class PersonalInfo extends RoomsMessage {
	public String getName() throws JSONException {
		return getBody().getString("name");
	}

	public void setName(String name) throws JSONException {
		getBody().put("name", name);
	}

	@Override
	public RoomsMessageType getType() {
		return RoomsMessageType.PersonalInfo;
	}
}
