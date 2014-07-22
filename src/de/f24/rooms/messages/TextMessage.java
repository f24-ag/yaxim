package de.f24.rooms.messages;

import org.json.JSONException;


public class TextMessage extends RoomsMessage {
	public String getText() throws JSONException {
		return getBody().getString("text");
	}

	public void setText(String text) throws JSONException {
		getBody().put("text", text);
	}

	@Override
	public RoomsMessageType getType() {
		return RoomsMessageType.TextMessage;
	}
}
