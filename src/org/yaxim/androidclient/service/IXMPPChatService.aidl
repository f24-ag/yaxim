package org.yaxim.androidclient.service;

interface IXMPPChatService {
	void sendMessage(String user, String message);
	boolean isAuthenticated();
	void clearNotifications(String Jid);
	String sendFile(String jabberID, String fileName, long size, String key, String url);
	String openRoom(String parentRoomID, String topic, in String[] participants);
	String sendTaskResponse(String selectedOption);
	String sendTask(String roomID, String text, String recipient);
}