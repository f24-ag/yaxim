package org.yaxim.androidclient.crypto;

import org.abstractj.kalium.keys.KeyPair;
import org.abstractj.kalium.keys.PublicKey;


public interface KeyRetriever {
	public static final String ROOMS_SERVER = "rooms@rooms.f24.com";
	
	public static final String ROOMS_KEY = "f448811b9966a3f9e108d788b384e149e8bacff1622ee5085f393d1f8a1b5962";
	
	public void savePublicKey(String jid, String key) throws Exception;
	public void saveKeys(String jid, KeyPair keyPair) throws Exception;
	public KeyPair loadKeys(String jid) throws Exception;
	public PublicKey loadPublicKey(String jid) throws Exception;
}
