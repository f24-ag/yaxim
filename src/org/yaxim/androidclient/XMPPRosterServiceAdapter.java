package org.yaxim.androidclient;

import java.util.Collection;

import org.yaxim.androidclient.util.ConnectionState;

import android.os.RemoteException;
import android.util.Log;

import org.yaxim.androidclient.IXMPPRosterCallback;
import org.yaxim.androidclient.service.IXMPPRosterService;

	
public class XMPPRosterServiceAdapter {
	private static final String TAG = "yaxim.XMPPRSAdapter";
	private IXMPPRosterService xmppServiceStub;
	
	public XMPPRosterServiceAdapter(IXMPPRosterService xmppServiceStub) {
		Log.i(TAG, "New XMPPRosterServiceAdapter construced");
		this.xmppServiceStub = xmppServiceStub;
	}
	
	public void setStatusFromConfig() {
		try {
			xmppServiceStub.setStatusFromConfig();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void addRosterItem(String user, String alias, String group) {
		try {
			xmppServiceStub.addRosterItem(user, alias, group);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	public void renameRosterGroup(String group, String newGroup){
		try {
			xmppServiceStub.renameRosterGroup(group, newGroup);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	public void renameRosterItem(String contact, String newItemName){
		try {
			xmppServiceStub.renameRosterItem(contact, newItemName);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	
	public void moveRosterItemToGroup(String user, String group){
		try {
			xmppServiceStub.moveRosterItemToGroup(user, group);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	public void addRosterGroup(String group){
		try {
			xmppServiceStub.addRosterGroup(group);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	public void removeRosterItem(String user) {
		try {
			xmppServiceStub.removeRosterItem(user);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	public void disconnect() {
		try {
			xmppServiceStub.disconnect();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	public void connect() {
		try {
			xmppServiceStub.connect();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void registerUICallback(IXMPPRosterCallback uiCallback) {
		try {
			xmppServiceStub.registerRosterCallback(uiCallback);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

	public void unregisterUICallback(IXMPPRosterCallback uiCallback) {
		try {
			xmppServiceStub.unregisterRosterCallback(uiCallback);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

	public ConnectionState getConnectionState() {
		try {
			return ConnectionState.values()[xmppServiceStub.getConnectionState()];
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return ConnectionState.OFFLINE;
	}

	public String getConnectionStateString() {
		try {
			return xmppServiceStub.getConnectionStateString();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean isAuthenticated() {
		return getConnectionState() == ConnectionState.ONLINE;
	}

	public void sendPresenceRequest(String user, String type) {
		try {
			xmppServiceStub.sendPresenceRequest(user, type);
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

	public void sendRegistrationMessage1(String phoneNumber) {
		try {
			xmppServiceStub.sendRegistrationMessage1(phoneNumber);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void sendRegistrationMessage2(String code, String publicKey) {
		try {
			xmppServiceStub.sendRegistrationMessage2(code, publicKey);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void syncContacts() {
		try {
			xmppServiceStub.syncContacts();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void searchContact(String name) {
		try {
			xmppServiceStub.searchContact(name);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void sendWebToken(String token) {
		try {
			xmppServiceStub.sendWebToken(token);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
}
