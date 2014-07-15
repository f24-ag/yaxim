package de.f24.rooms.messages;

import java.util.List;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

@Element
public class OpenRoomRequest extends RoomsMessage {
	@Element
	private String roomName;
	
	@ElementList(entry="participant", inline=true)
	private List<String> participants;

	@Element(required=false)
	private String logger;

	public String getLogger() {
		return logger;
	}

	public void setLogger(String logger) {
		this.logger = logger;
	}

	public String getRoomName() {
		return roomName;
	}

	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}

	public List<String> getParticipants() {
		return participants;
	}

	public void setParticipants(List<String> participants) {
		this.participants = participants;
	}
}
