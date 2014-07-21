package de.f24.rooms.messages;

import org.simpleframework.xml.Element;

@Element
public class PersonalInfo extends RoomsMessage {
	@Element
	String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
