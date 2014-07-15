package org.yaxim.androidclient.crypto;

import org.abstractj.kalium.encoders.Hex;
import org.abstractj.kalium.keys.KeyPair;
import org.abstractj.kalium.keys.PublicKey;
import org.yaxim.androidclient.data.RosterProvider;
import org.yaxim.androidclient.data.RosterProvider.KeysConstants;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;

public class DBKeyRetriever implements KeyRetriever {

	private ContentResolver mContentResolver;
    private Hex hex = new Hex();
	
	public DBKeyRetriever(ContentResolver contentResolver) {
		this.mContentResolver = contentResolver;
	}
	
	@Override
	public void savePublicKey(String jid, String key) throws Exception {
		ContentValues values = new ContentValues();
		values.put(KeysConstants.JID, jid);
		values.put(KeysConstants.PUBLIC_KEY, key);
		if (mContentResolver.update(RosterProvider.KEYS_URI, values,
				KeysConstants.JID + " = ?", new String[] { jid }) == 0) {
			mContentResolver.insert(RosterProvider.KEYS_URI, values);
		}
	}

	@Override
	public void saveKeys(String jid, KeyPair keyPair) throws Exception {
		ContentValues values = new ContentValues();
		values.put(KeysConstants.JID, jid);
		values.put(KeysConstants.PUBLIC_KEY, keyPair.getPublicKey().toString());
		values.put(KeysConstants.PRIVATE_KEY, keyPair.getPrivateKey().toString());
		if (mContentResolver.update(RosterProvider.KEYS_URI, values,
				KeysConstants.JID + " = ?", new String[] { jid }) == 0) {
			mContentResolver.insert(RosterProvider.KEYS_URI, values);
		}
	}

	@Override
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
	public PublicKey loadPublicKey(String jid) throws Exception {
		PublicKey pk = null;
		if (KeyRetriever.ROOMS_SERVER.equals(jid)) {
			return new PublicKey(hex.decode(KeyRetriever.ROOMS_KEY));
		}
		else {
			Cursor c = mContentResolver.query(RosterProvider.KEYS_URI, 
					new String[] { KeysConstants.PUBLIC_KEY }, 
					KeysConstants.JID + " = ?", new String[] { jid }, null);
			if (c.moveToNext()) {
				pk = new PublicKey(hex.decode(c.getString(0)));
			}
			c.close();
		}
		return pk;
	}

}
