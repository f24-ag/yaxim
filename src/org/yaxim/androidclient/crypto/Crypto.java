package org.yaxim.androidclient.crypto;

import java.io.InputStream;
import java.io.OutputStream;

import org.abstractj.kalium.keys.KeyPair;
import org.jivesoftware.smack.util.Base64;

import de.f24.rooms.crypto.EncryptionException;
import de.f24.rooms.crypto.EncryptionKeyAccessor;
import de.f24.rooms.crypto.EncryptionType;
import de.f24.rooms.crypto.RoomsCrypto;
import de.f24.rooms.messages.RoomsMessage;

public class Crypto {
	private KeyAccessor keyAccessor;
	
	private RoomsCrypto roomsCrypto;
	
	public KeyAccessor getKeyAccessor() {
		return keyAccessor;
	}

	public String[] generateKeys(String jid) throws Exception {
		String keys[] = roomsCrypto.generateKeyPair(jid);
		keyAccessor.saveKeys(jid, keys[0], keys[1]);
		return keys;
	}
	
	public String hash(String value) {
		 return roomsCrypto.hash(value);
	}

	public void encryptStream(InputStream in, OutputStream out, String symmtericKey) throws Exception {
		roomsCrypto.encryptStream(in, out, symmtericKey);
	}

	public void decryptStream(InputStream in, OutputStream out, String symmetricKey) throws Exception {
		roomsCrypto.decryptStream(in, out, symmetricKey);
	}

	public String encryptMessage(RoomsMessage envelope) throws EncryptionException {
		byte[] cipher = roomsCrypto.encryptMessage(envelope, EncryptionType.PublicKey);
		return Base64.encodeBytes(cipher);
	}

	public RoomsMessage decryptMessage(String encryptedMessage, String myJID, String senderJID, String roomID) throws EncryptionException {
		byte[] cipher = Base64.decode(encryptedMessage);
		return roomsCrypto.decryptMessage(cipher, myJID, senderJID, roomID);
	}

	public String generateSymmetricKey() {
		return roomsCrypto.generateSymmetricKey();
	}
	
	public String encryptSymmetrically(String data, String key) throws EncryptionException {
		return roomsCrypto.encryptSymmetrically(data, key);
	}

	public String decryptSymmetrically(String data, String key) throws EncryptionException {
		return roomsCrypto.decryptSymmetrically(data, key);
	}

	public Crypto(KeyAccessor keyAccessor) {
		this.keyAccessor = keyAccessor;
		this.roomsCrypto = new RoomsCrypto(keyAccessor);
	}
}