package org.yaxim.androidclient.chat;

import java.util.Collection;

import org.yaxim.androidclient.service.IXMPPChatService;

import android.os.RemoteException;
import android.util.Log;

public class XMPPChatServiceAdapter {

	private static final String TAG = "yaxim.XMPPCSAdapter";
	private IXMPPChatService xmppServiceStub;
	private String jabberID;

	public XMPPChatServiceAdapter(IXMPPChatService xmppServiceStub,
			String jabberID) {
		Log.i(TAG, "New XMPPChatServiceAdapter construced");
		this.xmppServiceStub = xmppServiceStub;
		this.jabberID = jabberID;
	}

	public void sendMessage(String user, String message) {
		try {
			Log.i(TAG, "Called sendMessage(): " + jabberID + ": " + message);
			xmppServiceStub.sendMessage(user, message);
		} catch (RemoteException e) {
			Log.e(TAG, "caught RemoteException: " + e.getMessage());
		}
	}
	
	public boolean isServiceAuthenticated() {
		try {
			return xmppServiceStub.isAuthenticated();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void clearNotifications(String Jid) {
		try {
			xmppServiceStub.clearNotifications(Jid);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void sendFile(String jabberID, String fileName, long size, String key, String url) {
		try {
			xmppServiceStub.sendFile(jabberID, fileName, size, key, url);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void openRoom(String parentRoomID, String topic, Collection<String> participantJids) {
		try {
			xmppServiceStub.openRoom(parentRoomID, topic, participantJids.toArray(new String[] {}));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void sendTaskResponse(String selectedOption) {
		try {
			xmppServiceStub.sendTaskResponse(selectedOption);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
}
