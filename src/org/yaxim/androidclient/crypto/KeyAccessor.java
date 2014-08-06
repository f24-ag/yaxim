package org.yaxim.androidclient.crypto;

import java.util.ArrayList;
import java.util.List;

import org.abstractj.kalium.encoders.Hex;
import org.abstractj.kalium.keys.KeyPair;
import org.yaxim.androidclient.data.RosterProvider;
import org.yaxim.androidclient.data.RosterProvider.KeysConstants;
import org.yaxim.androidclient.data.RosterProvider.ParticipantConstants;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import de.f24.rooms.crypto.EncryptionKeyAccessor;

public class KeyAccessor implements EncryptionKeyAccessor {
	public static final String ROOMS_SERVER = "control-client@rooms-dev-vpc.f24.com";
	
	public static final String ROOMS_PUBLIC_KEY = "f448811b9966a3f9e108d788b384e149e8bacff1622ee5085f393d1f8a1b5962";
	
	public static final String NEW_USER = "registration@rooms-dev-vpc.f24.com";

	public static final String NEW_USER_PASSWORD = "xmpp2";

	private ContentResolver mContentResolver;
    private Hex hex = new Hex();
	
	public KeyAccessor(ContentResolver contentResolver) {
		this.mContentResolver = contentResolver;
	}
	
	public void savePublicKey(String jid, String resource, String key) throws Exception {
		ContentValues values = new ContentValues();
		values.put(KeysConstants.JID, jid);
		values.put(KeysConstants.PUBLIC_KEY, key);
		values.put(KeysConstants.RESOURCE, resource);
		if (mContentResolver.update(RosterProvider.KEYS_URI, values,
				KeysConstants.JID + " = ? AND " + KeysConstants.RESOURCE + " = ?", 
				new String[] { jid, resource }) == 0) {
			mContentResolver.insert(RosterProvider.KEYS_URI, values);
		}
	}

	public void saveKeys(String jid, String privateKey, String publicKey) throws Exception {
		ContentValues values = new ContentValues();
		values.put(KeysConstants.JID, jid);
		values.put(KeysConstants.PUBLIC_KEY, publicKey);
		values.put(KeysConstants.PRIVATE_KEY, privateKey);
		if (mContentResolver.update(RosterProvider.KEYS_URI, values,
				KeysConstants.JID + " = ?", new String[] { jid }) == 0) {
			mContentResolver.insert(RosterProvider.KEYS_URI, values);
		}
	}

	public KeyPair loadKeys(String jid) throws Exception {
		KeyPair kp = null;
		Cursor c = mContentResolver.query(RosterProvider.KEYS_URI, 
				new String[] { KeysConstants.PUBLIC_KEY, KeysConstants.PRIVATE_KEY }, 
				KeysConstants.JID + " = ?", new String[] { jid }, null);
		if (c.moveToNext()) {
			kp = new KeyPair(hex.decode(c.getString(1)), hex.decode(c.getString(0)));
		}
		c.close();
		return kp;
	}

	@Override
	public String getPublicKey(String fullJid) {
		String pk = null;
		String[] jidParts = fullJid.split("/");
		String bareJid = jidParts[0];
		String resource = jidParts.length > 1 ? jidParts[1] : null;  
		if (KeyAccessor.ROOMS_SERVER.equals(bareJid)) {
			return KeyAccessor.ROOMS_PUBLIC_KEY;
		}
		else {
			Cursor c = mContentResolver.query(RosterProvider.KEYS_URI, 
					new String[] { KeysConstants.PUBLIC_KEY }, 
					KeysConstants.JID + " = ? AND " + KeysConstants.RESOURCE + " = ?", new String[] { bareJid, resource }, null);
			if (c.moveToNext()) {
				pk = c.getString(0);
			}
			c.close();
		}
		return pk;
	}

	@Override
	public List<String> getKeysOfParticipants(String roomID) {
		List<String> keys = new ArrayList<String>();
		Cursor c = mContentResolver.query(RosterProvider.PARTICIPANTS_URI, 
				ParticipantConstants.getRequiredColumns().toArray(new String[] {}), 
				ParticipantConstants.ROOM + " = ?", new String[] { roomID }, null);
		while (c.moveToNext()) {
			String jid = c.getString(c.getColumnIndex(ParticipantConstants.JID));
			Cursor c2 = mContentResolver.query(RosterProvider.KEYS_URI, 
					new String[] { KeysConstants.PUBLIC_KEY }, 
					KeysConstants.JID + " = ?", new String[] { jid }, null);
			while (c2.moveToNext()) {
				keys.add(c.getString(0));
			}
		}
		return keys;
	}

	@Override
	public String getPrivateKey(String jid) {
		String pk = null;
		Cursor c = mContentResolver.query(RosterProvider.KEYS_URI, 
			new String[] { KeysConstants.PRIVATE_KEY }, 
			KeysConstants.JID + " = ?", new String[] { jid }, null);
		if (c.moveToNext()) {
			pk = c.getString(0);
		}
		c.close();
		return pk;
	}

	@Override
	public String getRoomKey(String roomID) {
		return null; // TODO!
	}
}
