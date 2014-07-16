package de.f24.rooms.messages;

import org.simpleframework.xml.Element;

@Element
public class RegistrationConfirmation extends RoomsMessage {
	@Element
	String jid;
	
	@Element
	String password;

	public String getJid() {
		return jid;
	}

	public void setJid(String jid) {
		this.jid = jid;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
