package de.f24.rooms.messages;

import org.json.JSONException;


public class ContactSearch extends RoomsMessage {
	public String getQuery() throws JSONException {
		return getBody().getString("query");
	}

	public void setQuery(String query) throws JSONException {
		getBody().put("query", query);
	}

	@Override
	public RoomsMessageType getType() {
		return RoomsMessageType.ContactSearch;
	}
}
