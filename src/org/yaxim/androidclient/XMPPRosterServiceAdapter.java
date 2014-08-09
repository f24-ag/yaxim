package org.yaxim.androidclient;

import java.util.Collection;
import java.util.List;

import org.yaxim.androidclient.service.IXMPPRosterService;
import org.yaxim.androidclient.util.ConnectionState;

import android.os.RemoteException;
import android.util.Log;

	
public class XMPPRosterServiceAdapter {
	private static final String TAG = "yaxim.XMPPRSAdapter";
	private final IXMPPRosterService xmppServiceStub;
	
	public XMPPRosterServiceAdapter(final IXMPPRosterService xmppServiceStub) {
		Log.i(TAG, "New XMPPRosterServiceAdapter construced");
		this.xmppServiceStub = xmppServiceStub;
	}
	
	public void setStatusFromConfig() {
		try {
			xmppServiceStub.setStatusFromConfig();
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
	}

	public void addRosterItem(final String user, final String alias, final String group) {
		try {
			xmppServiceStub.addRosterItem(user, alias, group);
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
	}
	
	public void renameRosterGroup(final String group, final String newGroup){
		try {
			xmppServiceStub.renameRosterGroup(group, newGroup);
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
	}
	
	public void renameRosterItem(final String contact, final String newItemName){
		try {
			xmppServiceStub.renameRosterItem(contact, newItemName);
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
	}
	
	
	public void moveRosterItemToGroup(final String user, final String group){
		try {
			xmppServiceStub.moveRosterItemToGroup(user, group);
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
	}
	
	public void addRosterGroup(final String group){
		try {
			xmppServiceStub.addRosterGroup(group);
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
	}
	
	public void removeRosterItem(final String user) {
		try {
			xmppServiceStub.removeRosterItem(user);
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
	}
	
	public void disconnect() {
		try {
			xmppServiceStub.disconnect();
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
	}
	
	public void connect() {
		try {
			xmppServiceStub.connect();
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
	}

	public void registerUICallback(final IXMPPRosterCallback uiCallback) {
		try {
			xmppServiceStub.registerRosterCallback(uiCallback);
		} catch (final RemoteException e) {
			e.printStackTrace();
		} catch (final NullPointerException e) {
			e.printStackTrace();
		}
	}

	public void unregisterUICallback(final IXMPPRosterCallback uiCallback) {
		try {
			xmppServiceStub.unregisterRosterCallback(uiCallback);
		} catch (final RemoteException e) {
			e.printStackTrace();
		} catch (final NullPointerException e) {
			e.printStackTrace();
		}
	}

	public ConnectionState getConnectionState() {
		try {
			return ConnectionState.values()[xmppServiceStub.getConnectionState()];
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
		return ConnectionState.OFFLINE;
	}

	public String getConnectionStateString() {
		try {
			return xmppServiceStub.getConnectionStateString();
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean isAuthenticated() {
		return getConnectionState() == ConnectionState.ONLINE;
	}

	public void sendPresenceRequest(final String user, final String type) {
		try {
			xmppServiceStub.sendPresenceRequest(user, type);
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
	}
	
	public void openRoom(final String parentRoomID, final String topic, final Collection<String> participantJids) {
		try {
			xmppServiceStub.openRoom(parentRoomID, topic, participantJids.toArray(new String[] {}));
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
	}

	public void sendRegistrationMessage1(final String phoneNumber) {
		try {
			xmppServiceStub.sendRegistrationMessage1(phoneNumber);
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
	}

	public void sendRegistrationMessage2(final String code, final String publicKey) {
		try {
			xmppServiceStub.sendRegistrationMessage2(code, publicKey);
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
	}

	public void syncContacts() {
		try {
			xmppServiceStub.syncContacts();
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
	}

	public void searchContact(final String name) {
		try {
			xmppServiceStub.searchContact(name);
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
	}

	public void sendWebToken(final String token) {
		try {
			xmppServiceStub.sendWebToken(token);
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
	}

    public void setCompanies( final List< String > selectedKeys ) {

        try {
            xmppServiceStub.setCompanies( selectedKeys.toArray( new String[] {} ) );
        } catch ( final RemoteException e ) {
            e.printStackTrace();
        }
    }
}
