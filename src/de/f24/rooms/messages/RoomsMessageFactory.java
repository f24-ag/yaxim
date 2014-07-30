package de.f24.rooms.messages;

import org.json.JSONException;
import org.json.JSONObject;

public class RoomsMessageFactory {
	public static RoomsMessage getRoomsMessage(RoomsMessageType type) {
		switch (type) {
		case Invitation:
			return new Invitation();
		case OpenRoomRequest:
			return new OpenRoomRequest();
		case PersonalInfo:
			return new PersonalInfo();
		case Registration:
			return new Registration();
		case RegistrationConfirmation:
			return new RegistrationConfirmation();
		case RegistrationRequest:
			return new RegistrationRequest();
		case RoomsConfiguration:
			return new RoomConfiguration();
		case TextMessage:
			return new TextMessage();
		case PushRequest:
			return new PushRequest();
		case ContactSync:
			return new ContactSync();
		case ContactList:
			return new ContactList();
		case ContactSearch:
			return new ContactSearch();
		case File:
			return new FileMessage();
		default:
			return null;
		}
	}
	
	public static RoomsMessage getRoomsMessage(String json) {
		try {
			JSONObject jsonObject = new JSONObject(json);
			int type = jsonObject.getJSONObject("header").getInt("type");
			RoomsMessage roomsMessage = getRoomsMessage(RoomsMessageType.ofType(type));
			roomsMessage.setJSONMessage(jsonObject);
			return roomsMessage;
		}
		catch (JSONException ex) {
			return null;
		}
	}
}
