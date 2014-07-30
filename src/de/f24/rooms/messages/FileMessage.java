package de.f24.rooms.messages;

import org.json.JSONException;


public class FileMessage extends RoomsMessage {
	public String getFileID() throws JSONException {
		return getBody().getString("file-id");
	}

	public void setFileID(String value) throws JSONException {
		getBody().put("file-id", value);
	}

	public String getDownloadLink() throws JSONException {
		return getBody().getString("download-link");
	}

	public void setDownloadLink(String value) throws JSONException {
		getBody().put("download-link", value);
	}

	public String getMimeType() throws JSONException {
		return getBody().getString("mime-type");
	}

	public void setMimeType(String value) throws JSONException {
		getBody().put("mime-type", value);
	}

	public long getSize() throws JSONException {
		return getBody().getLong("size");
	}

	public void setSize(long value) throws JSONException {
		getBody().put("size", value);
	}

	public String getFilename() throws JSONException {
		return getBody().getString("filename");
	}

	public void setFilename(String value) throws JSONException {
		getBody().put("filename", value);
	}

	public String getDescription() throws JSONException {
		return getBody().getString("description");
	}

	public void setDescription(String value) throws JSONException {
		getBody().put("description", value);
	}

	public String getKey() throws JSONException {
		return getBody().getString("key");
	}

	public void setKey(String value) throws JSONException {
		getBody().put("key", value);
	}

	@Override
	public RoomsMessageType getType() {
		return RoomsMessageType.File;
	}
}
