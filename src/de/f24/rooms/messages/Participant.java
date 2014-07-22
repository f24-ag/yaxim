package de.f24.rooms.messages;

import org.json.JSONException;
import org.json.JSONObject;


public class Participant {
	private String jid;

	private String name;

	private String publicKey;
	
	public Participant() {
	}
	
	public Participant(String jid, String name, String publicKey) {
		this.jid = jid;
		this.name = name;
		this.publicKey = publicKey;
	}

	@Override
	public String toString() {
		return name + " [" + jid + "]";
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
	
	public JSONObject toObject() throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put("jid", jid);
		obj.put("name", name);
		obj.put("public-key", publicKey);
		return obj;
	}
}
