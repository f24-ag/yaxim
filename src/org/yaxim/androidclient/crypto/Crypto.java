package org.yaxim.androidclient.crypto;

import static org.abstractj.kalium.SodiumConstants.SECRETKEY_BYTES;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;

import org.abstractj.kalium.SodiumConstants;
import org.abstractj.kalium.crypto.Box;
import org.abstractj.kalium.crypto.Hash;
import org.abstractj.kalium.crypto.Random;
import org.abstractj.kalium.crypto.SecretBox;
import org.abstractj.kalium.crypto.Util;
import org.abstractj.kalium.encoders.Hex;
import org.abstractj.kalium.keys.KeyPair;
import org.abstractj.kalium.keys.PublicKey;
import org.jivesoftware.smack.util.Base64;

import de.f24.rooms.messages.RoomsMessage;
import de.f24.rooms.messages.RoomsMessageFactory;

public class Crypto {
	private static final int BUFFER_SIZE = 1048576;
	
	private KeyRetriever keyRetriever;
	private Hash hash;
	private Hex hex;
	private Random random;

	public KeyRetriever getKeyRetriever() {
		return keyRetriever;
	}

	public KeyPair generateKeys(String jid) throws Exception {
		KeyPair keyPair = new KeyPair();
		System.out.println(keyPair.getPrivateKey().toString());
		System.out.println(keyPair.getPublicKey().toString());
		keyRetriever.saveKeys(jid, keyPair);
		return keyPair;
	}
	
	public String hash(String value) {
		 return hash.sha256(value, hex);
	}

	public void encryptStream(InputStream in, OutputStream out, String symmtericKey) throws Exception {
		byte[] nonce = random.randomBytes(SodiumConstants.NONCE_BYTES);
		out.write(nonce);

		SecretBox box = new SecretBox(symmtericKey, hex);
		byte[] buffer = new byte[BUFFER_SIZE];
		int count;
		while ((count = in.read(buffer)) > 0) {
			byte[] encrypted = box.encrypt(nonce, Util.slice(buffer, 0, count));
			out.write(encrypted);
		}
	}

	public void decryptStream(InputStream in, OutputStream out, String symmetricKey) throws Exception {
		byte[] nonce = new byte[SodiumConstants.NONCE_BYTES];
		in.read(nonce);

		SecretBox box = new SecretBox(symmetricKey, hex);
		byte[] buffer = new byte[BUFFER_SIZE + SodiumConstants.BOXZERO_BYTES];
		int count;
		while ((count = in.read(buffer)) > 0) {
			out.write(box.decrypt(nonce, Util.slice(buffer, 0, count)));
		}
	}

	public String encryptMessage(RoomsMessage envelope) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			String message = envelope.toString();
			output.write(intToByteArray(2)); // version
			output.write(intToByteArray(0)); // metadata
			output.write(intToByteArray(0)); // metadata

			output.write(intToByteArray(envelope.getRecipients().size())); // number of keys
			byte[] nonce = random.randomBytes(SodiumConstants.NONCE_BYTES);
			output.write(nonce); // nonce

			KeyPair myKeys = keyRetriever.loadKeys(envelope.getSender());
			output.write(myKeys.getPublicKey().toBytes()); // sender's public key

			byte[] symmetricKey = random.randomBytes(SECRETKEY_BYTES);
			for (String receipient : envelope.getRecipients()) {
				PublicKey otherKey = keyRetriever.loadPublicKey(receipient);
				Box box = new Box(otherKey, myKeys.getPrivateKey());
				byte[] ciphertext = box.encrypt(nonce, symmetricKey);
				output.write(ciphertext); // encrypted symmetric key 
			}
			
			SecretBox secretBox = new SecretBox(symmetricKey);
			byte[] encryptedMessage = secretBox.encrypt(nonce,
					message.getBytes("UTF-8"));
			output.write(encryptedMessage); // message
			
			return Base64.encodeBytes(output.toByteArray());
		} catch (Exception ex) {
			ex.printStackTrace();
			return "Encryption failed";
		}
	}

	private byte[] readBytes(InputStream input, int no) throws IOException {
		byte[] array = new byte[no];
		input.read(array, 0, no);
		return array;
	}

	private byte[] intToByteArray(int a) {
		byte[] ret = new byte[4];
		ret[3] = (byte) (a & 0xFF);
		ret[2] = (byte) ((a >> 8) & 0xFF);
		ret[1] = (byte) ((a >> 16) & 0xFF);
		ret[0] = (byte) ((a >> 24) & 0xFF);
		return ret;
	}

	private int byteArrayToInt(byte[] b) {
		return b[3] & 0xFF | (b[2] & 0xFF) << 8 | (b[1] & 0xFF) << 16
				| (b[0] & 0xFF) << 24;
	}

	public RoomsMessage decryptMessage(String encryptedMessage, String myJID) {
		ByteArrayInputStream input = new ByteArrayInputStream(Base64.decode(encryptedMessage));
		try {
			int version = byteArrayToInt(readBytes(input, 4));
			if (version != 2) {
				throw new UnsupportedEncodingException("Version " + version + " is not supported");
			}
			readBytes(input, 8); // Metadata
	        int noKeys = byteArrayToInt(readBytes(input, 4));
	        byte[] nonce = readBytes(input, SodiumConstants.NONCE_BYTES);
	        PublicKey otherKey = new PublicKey(readBytes(input, SECRETKEY_BYTES));
			KeyPair myKeys = keyRetriever.loadKeys(myJID);
			Box box = new Box(otherKey, myKeys.getPrivateKey());
			byte[] symmetricKey = null;
			for (int i = 0; i < noKeys; i++) {
				byte[] encKey = readBytes(input, 48);
				if (symmetricKey == null) {
					try {
						symmetricKey = box.decrypt(nonce, encKey);
					}
					catch (Exception ex) {
						// Do nothing...
					}
				}
			}
			if (symmetricKey == null) {
				throw new InvalidKeyException();
			}

			SecretBox secretBox = new SecretBox(symmetricKey);
			byte[] encrytedMessage = new byte[input.available()];
			input.read(encrytedMessage);
			byte[] decryptedData = secretBox.decrypt(nonce, encrytedMessage);
			String strMessage = new String(decryptedData, "UTF-8"); 
			return RoomsMessageFactory.getRoomsMessage(strMessage);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public String generateSymmetricKey() {
		byte[] symmetricKey = random.randomBytes(SECRETKEY_BYTES);
		return hex.encode(symmetricKey);
	}

	public Crypto(KeyRetriever keyRetriever) {
		this.keyRetriever = keyRetriever;
		hash = new Hash();
		hex = new Hex();
		random = new Random();
	}
}