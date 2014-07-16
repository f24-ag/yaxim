package org.yaxim.androidclient.crypto;

import org.abstractj.kalium.keys.KeyPair;
import org.abstractj.kalium.keys.PublicKey;


public interface KeyRetriever {
	public static final String ROOMS_SERVER = "rooms@rooms.f24.com";
	
	public static final String ROOMS_PUBLIC_KEY = "f448811b9966a3f9e108d788b384e149e8bacff1622ee5085f393d1f8a1b5962";
	
	public static final String NEW_USER = "newuser@rooms.f24.com";

	public static final String NEW_USER_PASSWORD = "xmpp1";

	public static final String NEW_USER_PUBLIC_KEY = "ca5b506f2f59914f824b56602c0d94e1b5d5e142c037e303dcd7bbf1d4320662";

	public static final String NEW_USER_PRIVATE_KEY = "87235237dfb46dbdb1993738a356e5fcb8b58f3d2a3a0da1f076b1af8987fe4e";

	public void savePublicKey(String jid, String key) throws Exception;
	public void saveKeys(String jid, KeyPair keyPair) throws Exception;
	public KeyPair loadKeys(String jid) throws Exception;
	public PublicKey loadPublicKey(String jid) throws Exception;
}
