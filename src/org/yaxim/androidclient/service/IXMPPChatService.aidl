package org.yaxim.androidclient.service;

interface IXMPPChatService {
	void sendMessage(String user, String message);
	boolean isAuthenticated();
	void clearNotifications(String Jid);
	void sendFile(String jid, String fileName);
	void openRoom(String parentRoomID, String topic, in String[] participants);
}