package org.yaxim.androidclient;

import java.net.URI;
import java.net.URISyntaxException;

import org.jboss.aerogear.android.Callback;
import org.jboss.aerogear.android.unifiedpush.PushConfig;
import org.jboss.aerogear.android.unifiedpush.PushRegistrar;
import org.jboss.aerogear.android.unifiedpush.Registrations;
import org.yaxim.androidclient.crypto.Crypto;
import org.yaxim.androidclient.crypto.KeyAccessor;
import org.yaxim.androidclient.data.YaximConfiguration;

import android.app.Application;
import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;
import de.duenndns.ssl.MemorizingTrustManager;

public class YaximApplication extends Application {
	// identity name and type, see:
	// http://xmpp.org/registrar/disco-categories.html
	public static final String XMPP_IDENTITY_NAME = "yaxim";
	public static final String XMPP_IDENTITY_TYPE = "phone";

    private final String VARIANT_ID       = "883131fa-bde6-42e1-828f-9dec7d9c0b77"; // Android
    private final String SECRET           = "8b027d63-8e00-4573-b031-c6d547935c05"; // Application
    private final String GCM_SENDER_ID    = "284187628423"; // Google

    // the URL which points to your UnifiedPush Server (e.g. "http://SERVER:PORT/CONTEXT/")
    private final String UNIFIED_PUSH_URL = "https://ag-yeswecan.rhcloud.com/ag-push";

    // MTM is needed globally for both the backend (connect)
	// and the frontend (display dialog)
	public MemorizingTrustManager mMTM;

	private YaximConfiguration mConfig;

	public Crypto mCrypto;

	public YaximApplication() {
		super();
	}

	@Override
	public void onCreate() {
		mMTM = new MemorizingTrustManager(this);
		mConfig = new YaximConfiguration(PreferenceManager
				.getDefaultSharedPreferences(this));
		//mCrypto = new Crypto(new FileKeyRetriever(this));
		mCrypto = new Crypto(new KeyAccessor(getContentResolver()));
	}
	
	public static YaximApplication getApp(Context ctx) {
		return (YaximApplication)ctx.getApplicationContext();
	}

	public static YaximConfiguration getConfig(Context ctx) {
		return getApp(ctx).mConfig;
	}

	public void registerForGCM(Context ctx, String jid){
        Registrations registrations = new Registrations();
        try {
            PushConfig config = new PushConfig(new URI(UNIFIED_PUSH_URL), GCM_SENDER_ID);  // 2
            config.setVariantID(VARIANT_ID);
            config.setSecret(SECRET);
            config.setAlias(jid);
            PushRegistrar push = registrations.push("unifiedpush", config);  // 3
    	    push.register(ctx, new Callback<Void>() {   // 2
    	        private static final long serialVersionUID = 1L;

    	        @Override
    	        public void onSuccess(Void ignore) {
    	        	Log.i("PushApplication", "Registration Succeeded!");
    	        }

    	        @Override
    	        public void onFailure(Exception exception) {
    	            Log.e("PushApplication", exception.getMessage(), exception);
    	        }
    	    });
        } 
        catch (URISyntaxException e) {
        	Log.e("tag", e.getMessage(), e);
        }
	}
}

