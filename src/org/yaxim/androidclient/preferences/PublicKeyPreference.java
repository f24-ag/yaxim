package org.yaxim.androidclient.preferences;

import org.abstractj.kalium.keys.KeyPair;
import org.yaxim.androidclient.YaximApplication;
import org.yaxim.androidclient.crypto.KeyRetriever;
import org.yaxim.androidclient.util.PreferenceConstants;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

public class PublicKeyPreference extends DialogPreference {
	
	private KeyRetriever keyRetriever;

	public PublicKeyPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		keyRetriever = YaximApplication.getApp(context).mCrypto.getKeyRetriever();
		this.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				readPublicKey();
				return true;
			}
		});
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (DialogInterface.BUTTON_POSITIVE == which) {
			String JID = this.getSharedPreferences().getString(PreferenceConstants.JID, "");
			try {
				KeyPair keyPair = keyRetriever.loadKeys(JID);
				ClipboardManager ClipMan = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
				ClipData clip = ClipData.newPlainText("key", keyPair.getPublicKey().toString());
			    ClipMan.setPrimaryClip(clip);
			}
			catch (Exception ex) {
				ex.printStackTrace();
				Toast.makeText(getContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
			}
		}
	}
	
	@Override
	protected void showDialog(android.os.Bundle state) {
		readPublicKey();
		super.showDialog(state);
	}
	
	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		readPublicKey();
	}
	
	private void readPublicKey() {
		final String JID = PublicKeyPreference.this.getSharedPreferences().getString(PreferenceConstants.JID, "");
		try {
			KeyPair keyPair = keyRetriever.loadKeys(JID);
			setDialogMessage(keyPair.getPublicKey().toString());
		}
		catch (Exception ex) {
			setDialogMessage("");
		}
	}
}
