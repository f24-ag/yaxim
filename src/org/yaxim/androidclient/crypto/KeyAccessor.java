package org.yaxim.androidclient.crypto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.abstractj.kalium.encoders.Hex;
import org.abstractj.kalium.keys.KeyPair;
import org.yaxim.androidclient.data.RosterProvider;
import org.yaxim.androidclient.data.RosterProvider.KeysConstants;
import org.yaxim.androidclient.data.RosterProvider.ParticipantConstants;
import org.yaxim.androidclient.data.RosterProvider.RoomsConstants;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;
import de.f24.rooms.crypto.EncryptionKeyAccessor;
import de.f24.rooms.messages.Identity;
import de.f24.rooms.messages.Identity.IdentityType;
import de.f24.rooms.messages.Participant;
import de.f24.rooms.messages.RoomConfiguration;

public class KeyAccessor implements EncryptionKeyAccessor {
	public static final String ROOMS_SERVER = "control-client@rooms-dev-vpc.f24.com";
	
	public static final String ROOMS_PUBLIC_KEY = "f448811b9966a3f9e108d788b384e149e8bacff1622ee5085f393d1f8a1b5962";
	
	public static final String NEW_USER = "registration@rooms-dev-vpc.f24.com";

	public static final String NEW_USER_PASSWORD = "xmpp2";

	private final ContentResolver mContentResolver;
    private final Hex hex = new Hex();
	
	public KeyAccessor(final ContentResolver contentResolver) {
		this.mContentResolver = contentResolver;
	}
	
	public void savePublicKey(final String jid, final String resource, final String key) throws Exception {
		final ContentValues values = new ContentValues();
		values.put(KeysConstants.JID, jid);
		values.put(KeysConstants.PUBLIC_KEY, key);
		values.put(KeysConstants.RESOURCE, resource);
		if (mContentResolver.update(RosterProvider.KEYS_URI, values,
				KeysConstants.JID + " = ? AND " + KeysConstants.RESOURCE + " = ?", 
				new String[] { jid, resource }) == 0) {
			mContentResolver.insert(RosterProvider.KEYS_URI, values);
		}
	}

	public void saveKeys(final String jid, final String privateKey, final String publicKey) throws Exception {
		final ContentValues values = new ContentValues();
		values.put(KeysConstants.JID, jid);
		values.put(KeysConstants.PUBLIC_KEY, publicKey);
		values.put(KeysConstants.PRIVATE_KEY, privateKey);
		if (mContentResolver.update(RosterProvider.KEYS_URI, values,
				KeysConstants.JID + " = ?", new String[] { jid }) == 0) {
			mContentResolver.insert(RosterProvider.KEYS_URI, values);
		}
	}

	public KeyPair loadKeys(final String jid) throws Exception {
		KeyPair kp = null;
		final Cursor c = mContentResolver.query(RosterProvider.KEYS_URI, 
				new String[] { KeysConstants.PUBLIC_KEY, KeysConstants.PRIVATE_KEY }, 
				KeysConstants.JID + " = ?", new String[] { jid }, null);
		if (c.moveToNext()) {
			kp = new KeyPair(hex.decode(c.getString(1)), hex.decode(c.getString(0)));
		}
		c.close();
		return kp;
	}

	@Override
	public String getPublicKey(final String fullJid) {
		String pk = null;
		final String[] jidParts = fullJid.split("/");
		final String bareJid = jidParts[0];
		final String resource = jidParts.length > 1 ? jidParts[1] : null;  
		if (KeyAccessor.ROOMS_SERVER.equals(bareJid)) {
			return KeyAccessor.ROOMS_PUBLIC_KEY;
		}
		else {
			String selection = KeysConstants.JID + " = ? ";
			String[] args = new String[] { bareJid }; 
			if (resource != null) {
				selection += " AND " + KeysConstants.RESOURCE + " = ?";
				args = jidParts; 
			}
			final Cursor c = mContentResolver.query(RosterProvider.KEYS_URI, 
					new String[] { KeysConstants.PUBLIC_KEY }, 
					selection, args, null); 
			if (c.moveToNext()) {
				pk = c.getString(0);
			}
			c.close();
		}
		return pk;
	}

	@Override
    public RoomConfiguration getRoomConfiguration( final String roomID ) {

        if ( roomID == null ) {
            return null;
        }
        final RoomConfiguration conf = new RoomConfiguration();
        final List< Participant > participants = new ArrayList< Participant >();

        final Cursor c0 = mContentResolver.query( RosterProvider.ROOMS_URI,
                RoomsConstants.getRequiredColumns().toArray( new String[] {} ),
                RoomsConstants.ID + " = ?", new String[] { roomID }, null );
        if ( c0.moveToNext() ) {
            conf.setRoomId( roomID );
            // conf.setRoomKey( c0.getString( c0.getColumnIndex( RoomsConstants.ROOM_KEY ) ) );
            conf.setRoomName( c0.getString( c0.getColumnIndex( RoomsConstants.NAME ) ) );
        } else {
            return null;
        }
        c0.close();

        final Cursor c1 = mContentResolver.query( RosterProvider.PARTICIPANTS_URI,
				ParticipantConstants.getRequiredColumns().toArray(new String[] {}), 
                ParticipantConstants.ROOM + " = ?", new String[] { roomID }, ParticipantConstants._ID );
        int n = 0;
        while ( c1.moveToNext() ) {
            final String jid = c1.getString( c1.getColumnIndex( ParticipantConstants.JID ) );
            final Participant p =
                    new Participant( jid, c1.getString( c1.getColumnIndex( ParticipantConstants.NAME ) ), null );
            participants.add( p );
			final Cursor c2 = mContentResolver.query(RosterProvider.KEYS_URI, 
                    new String[] { KeysConstants.RESOURCE, KeysConstants.PUBLIC_KEY },
                            KeysConstants.JID + " = ? AND " + KeysConstants.RESOURCE + " IS NOT NULL",
                            new String[] { jid }, KeysConstants._ID );
            if ( c2.moveToNext() ) {
                final Identity i = new Identity( c2.getString( 0 ), c2.getString( 1 ), IdentityType.Mobile, n++ );
                p.setIdentities( Arrays.asList( i ) );
			}
            c2.close();
		}
        c1.close();
        conf.setParticipants( participants );
        conf.setMaxIndex( n );
        Log.i( "Crypto", conf.toString() );
        return conf;
	}

	@Override
	public String getPrivateKey(final String jid) {
		String pk = null;
		final Cursor c = mContentResolver.query(RosterProvider.KEYS_URI, 
			new String[] { KeysConstants.PRIVATE_KEY }, 
			KeysConstants.JID + " = ?", new String[] { jid }, null);
		if (c.moveToNext()) {
			pk = c.getString(0);
		}
		c.close();
		return pk;
	}
}
