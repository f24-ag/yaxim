package org.yaxim.androidclient.service;

import org.yaxim.androidclient.exceptions.YaximXMPPException;
import org.yaxim.androidclient.util.ConnectionState;

import de.f24.rooms.messages.RoomsMessage;


public interface Smackable {
	boolean doConnect(boolean create_account) throws YaximXMPPException;
	boolean isAuthenticated();
	void requestConnectionState(ConnectionState new_state);
	void requestConnectionState(ConnectionState new_state, boolean create_account);
	ConnectionState getConnectionState();
	String getLastError();

	void addRosterItem(String user, String alias, String group) throws YaximXMPPException;
	void removeRosterItem(String user) throws YaximXMPPException;
	void renameRosterItem(String user, String newName) throws YaximXMPPException;
	void moveRosterItemToGroup(String user, String group) throws YaximXMPPException;
	void renameRosterGroup(String group, String newGroup);
	void sendPresenceRequest(String user, String type);
	void addRosterGroup(String group);
	
	void setStatusFromConfig();
	void sendMessage(String user, String message);
	void sendServerPing();
	
	void registerCallback(XMPPServiceCallback callBack);
	void unRegisterCallback();
	
	String getNameForJID(String jid, String roomID);
	void sendFile(String jid, String fileName);
	String sendControlMessage(RoomsMessage request);
	String sendRoomMessage(String roomID, RoomsMessage request);
}
