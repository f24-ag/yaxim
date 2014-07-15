package de.f24.rooms.messages;

import java.util.List;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

@Element
public class RoomConfiguration extends RoomsMessage {
	@Element
	private String roomName;
	
	@Element
	private String roomID;
	
	@ElementList(entry="participant", inline=true)
	private List<Participant> participants;

	public String getRoomName() {
		return roomName;
	}

	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}

	public String getRoomID() {
		return roomID;
	}

	public void setRoomID(String roomID) {
		this.roomID = roomID;
	}

	public List<Participant> getParticipants() {
		return participants;
	}

	public void setParticipants(List<Participant> participants) {
		this.participants = participants;
	}
}
