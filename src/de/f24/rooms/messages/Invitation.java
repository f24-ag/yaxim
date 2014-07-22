package de.f24.rooms.messages;

public class Invitation extends RoomConfiguration {
	public RoomsMessageType getType() {
		return RoomsMessageType.Invitation;
	}
}
