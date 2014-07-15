package org.yaxim.androidclient.preferences;

import org.yaxim.androidclient.R;
import org.yaxim.androidclient.YaximApplication;
import org.yaxim.androidclient.crypto.Crypto;
import org.yaxim.androidclient.util.PreferenceConstants;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.widget.Toast;

public class PrivateKeyPreference extends DialogPreference {
	private Crypto crypto;
	
	public PrivateKeyPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		crypto = YaximApplication.getApp(context).mCrypto;
	    setDialogMessage(R.string.account_privateKeyGenerateWarning);
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (DialogInterface.BUTTON_POSITIVE == which) {
			String JID = this.getSharedPreferences().getString(PreferenceConstants.JID, "");
			try {
				crypto.generateKeys(JID);
				Toast.makeText(getContext(), R.string.account_privateKeyGenerateSuccess, Toast.LENGTH_LONG).show();
			}
			catch (Exception ex) {
				ex.printStackTrace();
				Toast.makeText(getContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
			}
		}
	}
}
