package de.f24.rooms.messages;

import org.json.JSONException;


public class RegistrationConfirmation extends RoomsMessage {
	public String getJid() throws JSONException {
		return getBody().getString("jid");
	}

	public void setJid(String jid) throws JSONException {
		getBody().put("jid", jid);
	}

	public String getPassword() throws JSONException {
		return getBody().getString("password");
	}

	public void setPassword(String password) throws JSONException {
		getBody().put("password", password);
	}

	@Override
	public RoomsMessageType getType() {
		return RoomsMessageType.RegistrationConfirmation;
	}
}
