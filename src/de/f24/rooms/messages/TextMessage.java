package de.f24.rooms.messages;

import org.simpleframework.xml.Element;

@Element
public class TextMessage extends RoomsMessage {
	@Element
	private String text;

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}
