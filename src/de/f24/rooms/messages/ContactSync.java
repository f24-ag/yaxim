package de.f24.rooms.messages;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

public class ContactSync extends RoomsMessage {
	public List<String> getHashes() throws JSONException {
		List<String> values = new ArrayList<String>();
		for (int i = 0; i < getBody().getJSONArray("hashes").length(); i++) {
			values.add(getBody().getJSONArray("hashes").getString(i));
		}
		return values;
	}

	public void setHashes(List<String> hashes) throws JSONException {
		JSONArray array = new JSONArray();
		for (String p : hashes) {
			array.put(p);
		}
		getBody().put("hashes", array);
	}

	@Override
	public RoomsMessageType getType() {
		return RoomsMessageType.ContactSync;
	}
}
