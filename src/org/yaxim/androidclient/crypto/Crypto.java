package org.yaxim.androidclient.crypto;

import static org.abstractj.kalium.SodiumConstants.SECRETKEY_BYTES;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.security.InvalidKeyException;
import java.util.Date;

import org.abstractj.kalium.SodiumConstants;
import org.abstractj.kalium.crypto.Box;
import org.abstractj.kalium.crypto.Random;
import org.abstractj.kalium.crypto.SecretBox;
import org.abstractj.kalium.crypto.Util;
import org.abstractj.kalium.encoders.Hex;
import org.abstractj.kalium.keys.KeyPair;
import org.abstractj.kalium.keys.PublicKey;
import org.simpleframework.xml.core.Persister;

import android.util.Log;
import de.f24.rooms.messages.Envelope;
import de.f24.rooms.messages.TextMessage;

public class Crypto {
	private KeyRetriever keyRetriever;
	
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
	
	public String encrypt(String data, String myJabberID, String otherJabberID) throws Exception {
        Hex hex = new Hex();
        Random random = new Random(); 
		KeyPair myKeys = keyRetriever.loadKeys(myJabberID);
		PublicKey otherKey = keyRetriever.loadPublicKey(otherJabberID);
		byte[] nonce = random.randomBytes(SodiumConstants.NONCE_BYTES);

		Box myBox = new Box(otherKey, myKeys.getPrivateKey());
        byte[] ciphertext = myBox.encrypt(nonce, data.getBytes("UTF-8"));
		
		return hex.encode(nonce) + hex.encode(ciphertext);
	}

	public void encrypt(InputStream in, OutputStream out, String myJabberID, String otherJabberID) throws Exception {
        Random random = new Random(); 
		KeyPair myKeys = keyRetriever.loadKeys(myJabberID);
		PublicKey otherKey = keyRetriever.loadPublicKey(myJabberID);
		byte[] nonce = random.randomBytes(SodiumConstants.NONCE_BYTES);
		out.write(nonce);
		
		long m = new Date().getTime();
		Box myBox = new Box(otherKey, myKeys.getPrivateKey());
		byte[] buffer = new byte[1048576];
		int count;
		while ((count = in.read(buffer)) > 0) {
			byte[] encrypted = myBox.encrypt(nonce, Util.slice(buffer, 0, count)); 
			out.write(encrypted);
		}
		Log.i("Crypto", "Dateiverschlüsselung hat " + (new Date().getTime() - m) + " ms gedauert");
	}

	public String decrypt(String encData, String myJabberID, String otherJabberID) throws Exception {
        Hex hex = new Hex();
		KeyPair myKeys = keyRetriever.loadKeys(myJabberID);
		PublicKey otherKey = keyRetriever.loadPublicKey(otherJabberID);
		String nonce = encData.substring(0, SodiumConstants.NONCE_BYTES * 2);
		String data = encData.substring(SodiumConstants.NONCE_BYTES * 2);

		Box myBox = new Box(otherKey, myKeys.getPrivateKey());
        byte[] decryptedText = myBox.decrypt(hex.decode(nonce), hex.decode(data));
		
		return new String(decryptedText, "UTF-8");
	}

	public void decrypt(InputStream in, OutputStream out, String myJabberID, String otherJabberID) throws Exception {
		KeyPair myKeys = keyRetriever.loadKeys(myJabberID);
		PublicKey otherKey = keyRetriever.loadPublicKey(myJabberID);
		byte[] nonce = new byte[SodiumConstants.NONCE_BYTES];
		in.read(nonce);
		
		long m = new Date().getTime();
		Box myBox = new Box(otherKey, myKeys.getPrivateKey());
		byte[] buffer = new byte[1048576 + SodiumConstants.BOXZERO_BYTES];
		int count;
		while ((count = in.read(buffer)) > 0) {
			out.write(myBox.decrypt(nonce, Util.slice(buffer, 0, count)));
		}
		Log.i("Crypto", "Dateientschlüsselung hat " + (new Date().getTime() - m) + " ms gedauert");
	}

	public String encryptEnvelope(Envelope envelope) {
		try {
	        Random random = new Random(); 
	        Hex hex = new Hex();
			Persister p = new Persister();
			StringWriter w = new StringWriter();
			p.write(envelope, w);
			String message = w.toString();
			StringBuilder out = new StringBuilder();
			
			byte[] nonce = random.randomBytes(SodiumConstants.NONCE_BYTES);
			out.append(hex.encode(nonce));
			out.append('\n');

			byte[] symmetricKey = random.randomBytes(SECRETKEY_BYTES);
			SecretBox secretBox = new SecretBox(symmetricKey);
			byte[] encryptedMessage = secretBox.encrypt(nonce, message.getBytes("UTF-8"));
			out.append(hex.encode(encryptedMessage));
			out.append('\n');
			
			KeyPair myKeys = keyRetriever.loadKeys(envelope.getMessage().getFrom());
			out.append(hex.encode(myKeys.getPublicKey().toBytes()));
			out.append('\n');
			
			//PublicKey roomsKey = keyRetriever.loadPublicKey(KeyRetriever.ROOMS_SERVER);
			long m = new Date().getTime();
			for (String recipient : envelope.getMessage().getRecipients()) {
				PublicKey otherKey = keyRetriever.loadPublicKey(recipient);
				Box box = new Box(otherKey, myKeys.getPrivateKey());
		        byte[] ciphertext = box.encrypt(nonce, symmetricKey);
				out.append(hex.encode(ciphertext));
				out.append('\n');
			} 
			
			// Bloat begin
//			Box box = new Box(roomsKey, myKeys.getPrivateKey());
//			for (int i = 0; i < 996; i++ ) {
//		        byte[] ciphertext = box.encrypt(nonce, symmetricKey);
//				out.append(hex.encode(ciphertext));
//				out.append('\n');
//			}
			Log.i("Crypto", "Verschlüsselung hat " + (new Date().getTime() - m) + " ms gedauert");
			// Bloat end
			return out.toString();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return "Encryption failed";
		}
	}
	
	public Envelope decryptEnvelope(String encryptedMessage, String myJID) {
		String[] parts = encryptedMessage.split("\n");
		try {
	        Hex hex = new Hex();
			KeyPair myKeys = keyRetriever.loadKeys(myJID);

			long m = new Date().getTime();
	        byte[] nonce = hex.decode(parts[0]);
	        PublicKey otherKey = new PublicKey(hex.decode(parts[2]));
			Box box = new Box(otherKey, myKeys.getPrivateKey());
			byte[] symmetricKey = null;
			for (int i = 3; i < parts.length; i++) {
				try {
					symmetricKey = box.decrypt(nonce, hex.decode(parts[i]));
					// break;
				}
				catch (Exception ex) {
					// Do nothing...
				}
			}
			if (symmetricKey == null) {
				throw new InvalidKeyException();
			}
			Log.i("Crypto", "Entschlüsselung hat " + (new Date().getTime() - m) + " ms gedauert");
			SecretBox secretBox = new SecretBox(symmetricKey);
			byte[] decryptedData = secretBox.decrypt(nonce, hex.decode(parts[1]));
			String message = new String(decryptedData, "UTF-8"); 
			
			Persister persister = new Persister();
			Envelope env = persister.read(Envelope.class, message);
			if (env.getMessage() instanceof TextMessage) {
				TextMessage tm = (TextMessage) env.getMessage();
				tm.setText(tm.getText());
			}
			return env;
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	public Crypto(KeyRetriever keyRetriever) {
		this.keyRetriever = keyRetriever;
	}
}