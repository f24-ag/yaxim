package de.f24.rooms.messages;

import org.simpleframework.xml.Element;

@Element
public class Invitation extends RoomsMessage {
	@Element
	private RoomConfiguration roomConfiguration;

	public RoomConfiguration getRoomConfiguration() {
		return roomConfiguration;
	}

	public void setRoomConfiguration(RoomConfiguration roomsConfiguration) {
		this.roomConfiguration = roomsConfiguration;
	}
}
