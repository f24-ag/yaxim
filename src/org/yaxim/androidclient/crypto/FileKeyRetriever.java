package org.yaxim.androidclient.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import org.abstractj.kalium.encoders.Hex;
import org.abstractj.kalium.keys.KeyPair;
import org.abstractj.kalium.keys.PublicKey;

import android.content.Context;

public class FileKeyRetriever implements KeyRetriever {

	private Context context;
	
	public FileKeyRetriever(Context context) {
		this.context = context;
	}
	
    private byte[] readFile(File file) throws IOException {        
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            throw new IOException("File is too large!");
        }
        byte[] bytes = new byte[(int)length];
        int offset = 0;
        int numRead = 0;
        InputStream is = new FileInputStream(file);
        try {
            while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
                offset += numRead;
            }
        } finally {
            is.close();
        }
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file "+file.getName());
        }
        return bytes;
    }	

	@Override
	public void savePublicKey(String jid, String key) throws Exception {
		PublicKey publicKey = new PublicKey(key);
		FileWriter out = new FileWriter(new File(context.getFilesDir(), jid + ".public"));
		out.write(publicKey.toString());
		out.close();
	}

	@Override
	public KeyPair loadKeys(String jid) throws Exception {
        Hex hex = new Hex();
		String privateKeyHex = new String(readFile(new File(context.getFilesDir(), jid + ".private")));
		String publicKeyHex = new String(readFile(new File(context.getFilesDir(), jid + ".public")));
		return new KeyPair(hex.decode(privateKeyHex), hex.decode(publicKeyHex));
	}

	@Override
	public PublicKey loadPublicKey(String jid) throws Exception {
        Hex hex = new Hex();
		if (KeyRetriever.ROOMS_SERVER.equals(jid)) {
			return new PublicKey(hex.decode(KeyRetriever.ROOMS_PUBLIC_KEY));
		}
		else {
			String publicKeyHex = new String(readFile(new File(context.getFilesDir(), jid + ".public")));
			return new PublicKey(hex.decode(publicKeyHex));
		}
	}

	@Override
	public void saveKeys(String jid, KeyPair keyPair) throws Exception {
		FileWriter out = new FileWriter(new File(context.getFilesDir(), jid + ".public"));
		out.write(keyPair.getPublicKey().toString());
		out.close();
		out = new FileWriter(new File(context.getFilesDir(), jid + ".private"));
		out.write(keyPair.getPrivateKey().toString());
		out.close();
	}
}
