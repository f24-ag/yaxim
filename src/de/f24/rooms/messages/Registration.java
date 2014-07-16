package de.f24.rooms.messages;

import org.simpleframework.xml.Element;

@Element
public class Registration extends RoomsMessage {
	@Element
	String confirmationCode;
	
	@Element
	String publicKey;

	public String getConfirmationCode() {
		return confirmationCode;
	}

	public void setConfirmationCode(String confirmationCode) {
		this.confirmationCode = confirmationCode;
	}

	public String getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
	}
}
