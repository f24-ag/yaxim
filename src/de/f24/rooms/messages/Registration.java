package de.f24.rooms.messages;

import org.json.JSONException;

public class Registration extends RoomsMessage {
	public String getConfirmationCode() throws JSONException {
		return getBody().getString("pin");
	}

	public void setConfirmationCode(String confirmationCode)
			throws JSONException {
		getBody().put("put", confirmationCode);
	}

	public String getPublicKey() throws JSONException {
		return getBody().getString("public-key");
	}

	public void setPublicKey(String publicKey) throws JSONException {
		getBody().put("public-key", publicKey);
	}

	@Override
	public RoomsMessageType getType() {
		return RoomsMessageType.Registration;
	}
}
