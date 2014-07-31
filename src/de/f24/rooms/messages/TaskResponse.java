package de.f24.rooms.messages;

import org.json.JSONException;


public class TaskResponse extends RoomsMessage {
	public String getAnswer() throws JSONException {
		return getBody().getString("answer");
	}

	public void setText(String answer) throws JSONException {
		getBody().put("answer", answer);
	}
	
	@Override
	public RoomsMessageType getType() {
		return RoomsMessageType.TaskResponse;
	}
}
