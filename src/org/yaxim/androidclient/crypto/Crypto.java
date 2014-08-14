package org.yaxim.androidclient.crypto;

import java.io.InputStream;
import java.io.OutputStream;

import org.jivesoftware.smack.util.Base64;

import de.f24.rooms.crypto.EncryptionException;
import de.f24.rooms.crypto.EncryptionType;
import de.f24.rooms.crypto.RoomsCrypto;
import de.f24.rooms.messages.RoomsMessage;

public class Crypto {
	private final KeyAccessor keyAccessor;
	
	private final RoomsCrypto roomsCrypto;
	
	public KeyAccessor getKeyAccessor() {
		return keyAccessor;
	}

	public String[] generateKeys(final String jid) throws Exception {
		final String keys[] = roomsCrypto.generateKeyPair(jid);
		keyAccessor.saveKeys(jid, keys[0], keys[1]);
		return keys;
	}
	
	public String hash(final String value) {
		 return roomsCrypto.hash(value);
	}

	public void encryptStream(final InputStream in, final OutputStream out, final String symmtericKey) throws Exception {
		roomsCrypto.encryptStream(in, out, symmtericKey);
	}

	public void decryptStream(final InputStream in, final OutputStream out, final String symmetricKey) throws Exception {
		roomsCrypto.decryptStream(in, out, symmetricKey);
	}

    public String encryptMessage( final RoomsMessage envelope ) throws EncryptionException {

        final byte[] cipher =
                roomsCrypto.encryptMessage( envelope, EncryptionType.PublicKey, envelope.getRecipients(), envelope
                        .getRoomId() );
		return Base64.encodeBytes(cipher);
	}

	public RoomsMessage decryptMessage(final String encryptedMessage, final String myJID, final String senderJID, final String roomID) throws EncryptionException {
		final byte[] cipher = Base64.decode(encryptedMessage);
        return roomsCrypto.decryptMessage( cipher, senderJID, roomID );
	}

	public String generateSymmetricKey() {
		return roomsCrypto.generateSymmetricKey();
	}
	
	public String encryptSymmetrically(final String data, final String key) throws EncryptionException {
		return roomsCrypto.encryptSymmetrically(data, key);
	}

	public String decryptSymmetrically(final String data, final String key) throws EncryptionException {
		return roomsCrypto.decryptSymmetrically(data, key);
	}

    public void init( final String myJid ) throws EncryptionException {

        this.roomsCrypto.init( myJid );
    }

    public Crypto( final KeyAccessor keyAccessor ) {
		this.keyAccessor = keyAccessor;
		this.roomsCrypto = new RoomsCrypto(keyAccessor);
	}
}