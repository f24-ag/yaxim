package de.f24.rooms.messages;

import org.simpleframework.xml.Element;

@Element
public class RegistrationRequest extends RoomsMessage {
	@Element
	String phoneNumber;

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
}
