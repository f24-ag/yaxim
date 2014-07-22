package de.f24.rooms.messages;

import org.json.JSONException;


public class RegistrationRequest extends RoomsMessage {
	public String getPhoneNumber() throws JSONException {
		return getBody().getString("phone");
	}

	public void setPhoneNumber(String phoneNumber) throws JSONException {
		getBody().put("phone", phoneNumber);
	}

	@Override
	public RoomsMessageType getType() {
		return RoomsMessageType.RegistrationRequest;
	}
}
