package de.f24.rooms.messages;

import org.simpleframework.xml.Element;

@Element
public class Participant {
	@Element
	private String jid;

	@Element
	private String name;

	@Element
	private String publicKey;

	@Override
	public String toString() {
		return "Participant [jid=" + jid + ", name=" + name + ", publicKey="
				+ publicKey + "]";
	}

	public String getJid() {
		return jid;
	}

	public void setJid(String jid) {
		this.jid = jid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
	}
}
