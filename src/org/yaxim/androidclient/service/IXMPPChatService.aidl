package org.yaxim.androidclient.service;

interface IXMPPChatService {
	void sendMessage(String user, String message);
	boolean isAuthenticated();
	void clearNotifications(String Jid);
	void sendFile(String jabberID, String fileName, long size, String key, String url);
	void openRoom(String parentRoomID, String topic, in String[] participants);
	void sendTaskResponse(String selectedOption);
}