package org.yaxim.androidclient.crypto;

import java.util.ArrayList;
import java.util.List;

import org.abstractj.kalium.encoders.Hex;
import org.abstractj.kalium.keys.KeyPair;
import org.yaxim.androidclient.data.RosterProvider;
import org.yaxim.androidclient.data.RosterProvider.KeysConstants;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import de.f24.rooms.crypto.EncryptionKeyAccessor;

public class KeyAccessor implements EncryptionKeyAccessor {

	private ContentResolver mContentResolver;
    private Hex hex = new Hex();
	
	public KeyAccessor(ContentResolver contentResolver) {
		this.mContentResolver = contentResolver;
	}
	
	public void savePublicKey(String jid, String key) throws Exception {
		ContentValues values = new ContentValues();
		values.put(KeysConstants.JID, jid);
		values.put(KeysConstants.PUBLIC_KEY, key);
		if (mContentResolver.update(RosterProvider.KEYS_URI, values,
				KeysConstants.JID + " = ?", new String[] { jid }) == 0) {
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
	public String getPublicKey(String jid) {
		String pk = null;
		if (KeyRetriever.ROOMS_SERVER.equals(jid)) {
			return KeyRetriever.ROOMS_PUBLIC_KEY;
		}
		else if (KeyRetriever.NEW_USER.equals(jid)) {
			return KeyRetriever.NEW_USER_PUBLIC_KEY;
		}
		else {
			Cursor c = mContentResolver.query(RosterProvider.KEYS_URI, 
					new String[] { KeysConstants.PUBLIC_KEY }, 
					KeysConstants.JID + " = ?", new String[] { jid }, null);
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
//		Cursor c = mContentResolver.query(RosterProvider.PARTICIPANTS_URI, 
//				ParticipantConstants., 
//				ParticipantConstants.ROOM + " = ?", new String[] { roomID }, null);
//		List<Participant> participants = new ArrayList<Participant>();
//		while (c.moveToNext()) {
//			Participant p = new Participant();
//			p.setJid(c.getString(c.getColumnIndex(ParticipantConstants.JID)));
//			p.setName(c.getString(c.getColumnIndex(ParticipantConstants.NAME)));
//			participants.add(p);
//		}
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
