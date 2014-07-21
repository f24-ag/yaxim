package org.yaxim.androidclient.dialogs;

import org.abstractj.kalium.keys.KeyPair;
import org.yaxim.androidclient.MainWindow;
import org.yaxim.androidclient.R;
import org.yaxim.androidclient.XMPPRosterServiceAdapter;
import org.yaxim.androidclient.YaximApplication;
import org.yaxim.androidclient.util.PreferenceConstants;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class FirstStartDialog extends AlertDialog implements DialogInterface.OnClickListener, TextWatcher {

	private MainWindow mainWindow;
	private Button mOkButton;
	private EditText mSMSCode;
	private EditText mUserName;
	private XMPPRosterServiceAdapter mServiceAdapter;

	public FirstStartDialog(MainWindow mainWindow,
			XMPPRosterServiceAdapter serviceAdapter) {
		super(mainWindow);
		this.mainWindow = mainWindow;
		this.mServiceAdapter = serviceAdapter;

		setTitle(R.string.StartupDialog_Title);

		LayoutInflater inflater = (LayoutInflater) mainWindow
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View group = inflater.inflate(R.layout.firststartdialog, null, false);
		setView(group);

		setButton(BUTTON_POSITIVE, mainWindow.getString(android.R.string.ok), this);

		mSMSCode = (EditText) group.findViewById(R.id.StartupDialog_SMSCode);
		mUserName = (EditText) group.findViewById(R.id.StartupDialog_UserName);

		mSMSCode.addTextChangedListener(this);
		mUserName.addTextChangedListener(this);
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mOkButton = getButton(BUTTON_POSITIVE);
		mOkButton.setEnabled(false);
	}


	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
		case BUTTON_POSITIVE:
			verifyAndSavePreferences();
			try {
				KeyPair keyPair = YaximApplication.getApp(getContext()).mCrypto.generateKeys("tmp");
				mServiceAdapter.sendRegistrationMessage2(mSMSCode.getText().toString(), keyPair.getPublicKey().toString());
			} catch (Exception e) {
				Log.e("Registration", "Registration failed", e);
				Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
			}
			break;
		}
	}

	private void verifyAndSavePreferences() {
		String name = mUserName.getText().toString();
		String code = mSMSCode.getText().toString();
		savePreferences(name, code);
		cancel();
	}

	private void updateDialog() {
		boolean is_ok = true;
		Editable code = mSMSCode.getText();
		if (code.length() != 4) {
			mSMSCode.setError(mainWindow.getString(R.string.Global_JID_malformed));
			is_ok = false;
		}
		if (mUserName.length() == 0)
			is_ok = false;
		mOkButton.setEnabled(is_ok);
	}

	public void afterTextChanged(Editable s) {
		updateDialog();
	}

	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}

	private void savePreferences(String name, String code) {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(mainWindow);
		Editor editor = sharedPreferences.edit();

		editor.putString(PreferenceConstants.NAME, name);
		editor.putString(PreferenceConstants.SMS_CODE, code);
		editor.commit();
	}
}
