package de.f24.rooms.messages;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementUnion;
import org.simpleframework.xml.Root;

@Root(name="message")
public class Envelope {
	@ElementUnion({
		@Element(name="openRoomRequest", type=OpenRoomRequest.class),
		@Element(name="invitation", type=Invitation.class),
		@Element(name="roomConfiguration", type=RoomConfiguration.class),
		@Element(name="textMessage", type=TextMessage.class),
		@Element(name="registrationRequest", type=RegistrationRequest.class),
		@Element(name="registration", type=Registration.class),
		@Element(name="registrationConfirmation", type=RegistrationConfirmation.class),
	})
	private RoomsMessage message;

	public Envelope() {
	}
	
	public Envelope(RoomsMessage message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "Envelope [message=" + message + "]";
	}

	public RoomsMessage getMessage() {
		return message;
	}

	public void setMessage(RoomsMessage message) {
		this.message = message;
	}
}
