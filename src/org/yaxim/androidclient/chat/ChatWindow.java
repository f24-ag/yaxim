package org.yaxim.androidclient.chat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.yaxim.androidclient.MainWindow;
import org.yaxim.androidclient.NotifyingHandler;
import org.yaxim.androidclient.R;
import org.yaxim.androidclient.YaximApplication;
import org.yaxim.androidclient.crypto.KeyAccessor;
import org.yaxim.androidclient.data.ChatProvider;
import org.yaxim.androidclient.data.ChatProvider.ChatConstants;
import org.yaxim.androidclient.data.RosterProvider;
import org.yaxim.androidclient.data.RosterProvider.ParticipantConstants;
import org.yaxim.androidclient.data.RosterProvider.RoomsConstants;
import org.yaxim.androidclient.data.YaximConfiguration;
import org.yaxim.androidclient.service.IXMPPChatService;
import org.yaxim.androidclient.service.XMPPService;
import org.yaxim.androidclient.util.StatusMode;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.Window;

import de.f24.rooms.messages.RoomsMessageType;

@SuppressWarnings("deprecation") /* recent ClipboardManager only available since API 11 */
public class ChatWindow extends SherlockListActivity implements OnKeyListener,
		TextWatcher {

	public static final String INTENT_EXTRA_USERNAME = ChatWindow.class.getName() + ".username";
	public static final String INTENT_EXTRA_MESSAGE = ChatWindow.class.getName() + ".message";
	
	private static final String TAG = "yaxim.ChatWindow";
	private static final String[] PROJECTION_FROM = new String[] {
			ChatProvider.ChatConstants._ID, ChatProvider.ChatConstants.DATE,
			ChatProvider.ChatConstants.DIRECTION, ChatProvider.ChatConstants.SENDER,
			ChatProvider.ChatConstants.MESSAGE, ChatProvider.ChatConstants.DELIVERY_STATUS, 
			ChatProvider.ChatConstants.TYPE, ChatProvider.ChatConstants.EXTRA_DATA };

	private static final int[] PROJECTION_TO = new int[] { R.id.chat_date,
			R.id.chat_from, R.id.chat_message };
	
	private static final int DELAY_NEWMSG = 2000;

	private ContentObserver mContactObserver = new ContactObserver();
	private ImageView mStatusMode;
	private TextView mTitle;
	private TextView mSubTitle;
	private Button mSendButton = null;
	private EditText mChatInput = null;
	private String mWithJabberID = null;
	private String mUserScreenName = null;
	private Intent mServiceIntent;
	private ServiceConnection mServiceConnection;
	private XMPPChatServiceAdapter mServiceAdapter;
	private int mChatFontSize;
	private YaximConfiguration mConfig;
	private Map<String, String> participants;
	private BroadcastReceiver pushReceiver;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		mConfig = YaximApplication.getConfig(this);
		setTheme(mConfig.getTheme());
		super.onCreate(savedInstanceState);
	
		mChatFontSize = Integer.valueOf(YaximApplication.getConfig(this).chatFontSize);

		requestWindowFeature(Window.FEATURE_ACTION_BAR);
		setContentView(R.layout.mainchat);
		
		getContentResolver().registerContentObserver(RosterProvider.CONTENT_URI,
				true, mContactObserver);

		ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);

		registerForContextMenu(getListView());
		setContactFromUri();
		registerXMPPService();
		setSendButton();
		setUserInput();
		
		String titleUserid;
		if (mUserScreenName != null) {
			titleUserid = mUserScreenName;
		} else {
			titleUserid = mWithJabberID;
		}

		if (mWithJabberID.indexOf('@') == -1) { // Room
			participants = new HashMap<String, String>();
			Cursor c = getContentResolver().query(RosterProvider.PARTICIPANTS_URI, 
					ParticipantConstants.getRequiredColumns().toArray(new String[] {}), 
					ParticipantConstants.ROOM + " = ?", new String[] { mWithJabberID }, null);
			while (c.moveToNext()) {
				participants.put(c.getString(c.getColumnIndex(ParticipantConstants.JID)), c.getString(c.getColumnIndex(ParticipantConstants.NAME)));
			}
			c.close();
			
			c = getContentResolver().query(RosterProvider.ROOMS_URI, new String[] { RoomsConstants.NAME }, 
					RoomsConstants.ID + " = ?", new String[] { mWithJabberID }, null);
			if (c.moveToNext()) {
				titleUserid = c.getString(0);
			}
			c.close();
		}
		
		setCustomTitle(titleUserid);
		setChatWindowAdapter();
		getListView().setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (view.getTag(R.id.TAG_CHAT_ROW_EXTRA_DATA) != null) { // Extra data in JSON format
					try {
						JSONObject extraData = (JSONObject)view.getTag(R.id.TAG_CHAT_ROW_EXTRA_DATA);
						if (extraData.has("text")) { // HACK!!!
							int _id = (Integer)view.getTag(R.id.TAG_CHAT_ROW_ID);
							showTaskDialog(_id, extraData);
						}
						else {
							new DownloadFileTask(ChatWindow.this).execute(extraData);
						}
					}
					catch (Exception ex) {
						Log.e(TAG, ex.getMessage());
					}
				}
			}
		});
	}

	private void setCustomTitle(String title) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.chat_action_title, null);
		mStatusMode = (ImageView)layout.findViewById(R.id.action_bar_status);
		mTitle = (TextView)layout.findViewById(R.id.action_bar_title);
		mSubTitle = (TextView)layout.findViewById(R.id.action_bar_subtitle);
		mTitle.setText(title);

		setTitle(null);
		getSupportActionBar().setCustomView(layout);
		getSupportActionBar().setDisplayShowCustomEnabled(true);
	}

	private void setChatWindowAdapter() {
		String selection = ChatConstants.JID + "='" + mWithJabberID + "'";
		Cursor cursor = managedQuery(ChatProvider.CONTENT_URI, PROJECTION_FROM,
				selection, null, null);
		ListAdapter adapter = new ChatWindowAdapter(cursor, PROJECTION_FROM,
				PROJECTION_TO, mWithJabberID, mUserScreenName);

		setListAdapter(adapter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateContactStatus();
		
		pushReceiver = NotifyingHandler.registerDynamicBroadcastReceiver(this);
	}
	
	@Override
	protected void onPause() {
		NotifyingHandler.unregisterDynamicBroadcastReceiver(this, pushReceiver);
		
		super.onPause();
	}
	

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus)
			bindXMPPService();
		else
			unbindXMPPService();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (hasWindowFocus()) unbindXMPPService();
		getContentResolver().unregisterContentObserver(mContactObserver);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.chat_options, menu);
		return true;
	}
	
	private void registerXMPPService() {
		Log.i(TAG, "called startXMPPService()");
		mServiceIntent = new Intent(this, XMPPService.class);
		Uri chatURI = Uri.parse(mWithJabberID);
		mServiceIntent.setData(chatURI);
		mServiceIntent.setAction("org.yaxim.androidclient.XMPPSERVICE");

		mServiceConnection = new ServiceConnection() {

			public void onServiceConnected(ComponentName name, IBinder service) {
				Log.i(TAG, "called onServiceConnected()");
				mServiceAdapter = new XMPPChatServiceAdapter(
						IXMPPChatService.Stub.asInterface(service),
						mWithJabberID);
				
				mServiceAdapter.clearNotifications(mWithJabberID);
				updateContactStatus();
			}

			public void onServiceDisconnected(ComponentName name) {
				Log.i(TAG, "called onServiceDisconnected()");
			}

		};
	}

	private void unbindXMPPService() {
		try {
			unbindService(mServiceConnection);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Service wasn't bound!");
		}
	}

	private void bindXMPPService() {
		bindService(mServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}

	private void setSendButton() {
		mSendButton = (Button) findViewById(R.id.Chat_SendButton);
		View.OnClickListener onSend = getOnSetListener();
		mSendButton.setOnClickListener(onSend);
		mSendButton.setEnabled(false);
	}

	private void setUserInput() {
		Intent i = getIntent();
		mChatInput = (EditText) findViewById(R.id.Chat_UserInput);
		mChatInput.addTextChangedListener(this);
		if (i.hasExtra(INTENT_EXTRA_MESSAGE)) {
			mChatInput.setText(i.getExtras().getString(INTENT_EXTRA_MESSAGE));
		}
	}

	private void setContactFromUri() {
		Intent i = getIntent();
		mWithJabberID = i.getDataString().toLowerCase();
		if (i.hasExtra(INTENT_EXTRA_USERNAME)) {
			mUserScreenName = i.getExtras().getString(INTENT_EXTRA_USERNAME);
		} else {
			mUserScreenName = mWithJabberID;
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		View target = ((AdapterContextMenuInfo)menuInfo).targetView;
		TextView from = (TextView)target.findViewById(R.id.chat_from);
		getMenuInflater().inflate(R.menu.chat_contextmenu, menu);
		if (!from.getText().equals(getString(R.string.chat_from_me))) {
			menu.findItem(R.id.chat_contextmenu_resend).setEnabled(false);
		}
	}

	private CharSequence getMessageFromContextMenu(MenuItem item) {
		View target = ((AdapterContextMenuInfo)item.getMenuInfo()).targetView;
		TextView message = (TextView)target.findViewById(R.id.chat_message);
		return message.getText();
	}

	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.chat_contextmenu_copy_text:
			ClipboardManager cm = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
			cm.setText(getMessageFromContextMenu(item));
			return true;
		case R.id.chat_contextmenu_resend:
			sendMessage(getMessageFromContextMenu(item).toString());
			Log.d(TAG, "resend!");
			return true;
		default:
			return super.onContextItemSelected((android.view.MenuItem) item);
		}
	}
	

	private View.OnClickListener getOnSetListener() {
		return new View.OnClickListener() {

			public void onClick(View v) {
				sendMessageIfNotNull();
			}
		};
	}

	private void sendMessageIfNotNull() {
		if (mChatInput.getText().length() >= 1) {
			sendMessage(mChatInput.getText().toString());
		}
	}

	private void sendMessage(String message) {
		mChatInput.setText(null);
		mSendButton.setEnabled(false);
		mServiceAdapter.sendMessage(mWithJabberID, message);
		if (!mServiceAdapter.isServiceAuthenticated())
			showToastNotification(R.string.toast_stored_offline);
	}

	private void markAsReadDelayed(final int id, final int delay) {
		new Thread() {
			@Override
			public void run() {
				try { Thread.sleep(delay); } catch (Exception e) {}
				markAsRead(id);
			}
		}.start();
	}
	
	private void markAsRead(int id) {
		Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY
			+ "/" + ChatProvider.TABLE_NAME + "/" + id);
		Log.d(TAG, "markAsRead: " + rowuri);
		ContentValues values = new ContentValues();
		values.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_SENT_OR_READ);
		getContentResolver().update(rowuri, values, null, null);
	}

	class ChatWindowAdapter extends SimpleCursorAdapter {
		String mScreenName, mJID;

		ChatWindowAdapter(Cursor cursor, String[] from, int[] to,
				String JID, String screenName) {
			super(ChatWindow.this, android.R.layout.simple_list_item_1, cursor,
					from, to);
			mScreenName = screenName;
			mJID = JID;
		}
		
		public View getFileDownloadView(String from, String date, String extraData) {
			LayoutInflater inflater = getLayoutInflater();
			View row = inflater.inflate(R.layout.chatfilerow, null);
			((TextView) row.findViewById(R.id.chat_date)).setText(date);
			((TextView) row.findViewById(R.id.chat_from)).setText(from);
			try {
				JSONObject fileInfo = new JSONObject(extraData);
				((TextView) row.findViewById(R.id.fileNameText)).setText(fileInfo.getString("filename"));
				((TextView) row.findViewById(R.id.fileSizeText)).setText(fileInfo.getString("size"));
				((TextView) row.findViewById(R.id.fileDescriptionText)).setText(fileInfo.getString("description"));
				row.setTag(R.id.TAG_CHAT_ROW_EXTRA_DATA, fileInfo);
			}
			catch (Exception ex) {
				Log.e(TAG, ex.getMessage());
			}
			return row;
		}

		public View getTaskView(RoomsMessageType type, String from, String date, String text, String extraData) {
			LayoutInflater inflater = getLayoutInflater();
			View row = inflater.inflate(R.layout.taskrow, null);
			((TextView) row.findViewById(R.id.chat_date)).setText(date);
			((TextView) row.findViewById(R.id.chat_from)).setText(from);
			try {
				JSONObject taskInfo = new JSONObject(extraData);
				((TextView) row.findViewById(R.id.taskText)).setText(text);
				row.setTag(R.id.TAG_CHAT_ROW_EXTRA_DATA, taskInfo);
			}
			catch (Exception ex) {
				Log.e(TAG, ex.getMessage());
			}
			row.setBackgroundColor(type == RoomsMessageType.Task ? 0xffffcccc : 0xffccffcc);
			return row;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Cursor cursor = this.getCursor();
			cursor.moveToPosition(position);

			long dateMilliseconds = cursor.getLong(cursor
					.getColumnIndex(ChatProvider.ChatConstants.DATE));

			int _id = cursor.getInt(cursor
					.getColumnIndex(ChatProvider.ChatConstants._ID));
			String date = getDateString(dateMilliseconds);
			String message = cursor.getString(cursor
					.getColumnIndex(ChatProvider.ChatConstants.MESSAGE));
			int intType = cursor.getInt(cursor
					.getColumnIndex(ChatProvider.ChatConstants.TYPE));
			RoomsMessageType type = RoomsMessageType.values()[intType];
			String sender = cursor.getString(cursor
					.getColumnIndex(ChatProvider.ChatConstants.SENDER));
			boolean from_me = mConfig.jabberID.equalsIgnoreCase(sender);
			String from = sender;
			if (sender.equals(mJID)){
				from = mScreenName;
			}
			if (participants != null && participants.get(from) != null) {
				from = participants.get(from);
			}
			int delivery_status = cursor.getInt(cursor
					.getColumnIndex(ChatProvider.ChatConstants.DELIVERY_STATUS));

			if (type == RoomsMessageType.File) {
				String extraData = cursor.getString(cursor.getColumnIndex(ChatProvider.ChatConstants.EXTRA_DATA));
				return getFileDownloadView(from, date, extraData);
			}
			else if (type == RoomsMessageType.Task || type == RoomsMessageType.TaskResponse) {
				String extraData = cursor.getString(cursor.getColumnIndex(ChatProvider.ChatConstants.EXTRA_DATA));
				View taskView = getTaskView(type, from, date, message, extraData);
				taskView.setTag(R.id.TAG_CHAT_ROW_ID, _id);
				return taskView;
			}
			
			View row = null;
			ChatItemWrapper wrapper = null;
			if (convertView != null && convertView.getTag() instanceof ChatItemWrapper) {
				row = convertView;
			}

			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.chatrow, null);
				wrapper = new ChatItemWrapper(row, ChatWindow.this);
				row.setTag(wrapper);
			} else {
				wrapper = (ChatItemWrapper) row.getTag();
			} 

			if (!from_me && delivery_status == ChatConstants.DS_NEW) {
				markAsReadDelayed(_id, DELAY_NEWMSG);
			}

			if (wrapper != null) {
				wrapper.populateFrom(date, from_me, from, message, delivery_status);
			}

			return row;
		}
	}

	private String getDateString(long milliSeconds) {
		SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date(milliSeconds);
		return dateFormater.format(date);
	}

	public class ChatItemWrapper {
		private TextView mDateView = null;
		private TextView mFromView = null;
		private TextView mMessageView = null;
		private ImageView mIconView = null;

		private final View mRowView;
		private ChatWindow chatWindow;

		ChatItemWrapper(View row, ChatWindow chatWindow) {
			this.mRowView = row;
			this.chatWindow = chatWindow;
		}

		void populateFrom(String date, boolean from_me, String from, String message,
				int delivery_status) {
//			Log.i(TAG, "populateFrom(" + from_me + ", " + from + ", " + message + ")");
			getDateView().setText(date);
			TypedValue tv = new TypedValue();
			if (from_me) {
				getTheme().resolveAttribute(R.attr.ChatMsgHeaderMeColor, tv, true);
				getDateView().setTextColor(tv.data);
				getFromView().setText(getString(R.string.chat_from_me));
				getFromView().setTextColor(tv.data);
			} else {
				getTheme().resolveAttribute(R.attr.ChatMsgHeaderYouColor, tv, true);
				getDateView().setTextColor(tv.data);
				getFromView().setText(from + ":");
				getFromView().setTextColor(tv.data);
			}
			switch (delivery_status) {
			case ChatConstants.DS_NEW:
				ColorDrawable layers[] = new ColorDrawable[2];
				getTheme().resolveAttribute(R.attr.ChatNewMessageColor, tv, true);
				layers[0] = new ColorDrawable(tv.data);
				if (from_me) {
					// message stored for later transmission
					getTheme().resolveAttribute(R.attr.ChatStoredMessageColor, tv, true);
					layers[1] = new ColorDrawable(tv.data);
				} else {
					layers[1] = new ColorDrawable(0x00000000);
				}
				TransitionDrawable backgroundColorAnimation = new
					TransitionDrawable(layers);
				int l = mRowView.getPaddingLeft();
				int t = mRowView.getPaddingTop();
				int r = mRowView.getPaddingRight();
				int b = mRowView.getPaddingBottom();
				mRowView.setBackgroundDrawable(backgroundColorAnimation);
				mRowView.setPadding(l, t, r, b);
				backgroundColorAnimation.setCrossFadeEnabled(true);
				backgroundColorAnimation.startTransition(DELAY_NEWMSG);
				getIconView().setImageResource(R.drawable.ic_chat_msg_status_queued);
				break;
			case ChatConstants.DS_SENT_OR_READ:
				getIconView().setImageResource(R.drawable.ic_chat_msg_status_unread);
				mRowView.setBackgroundColor(0x00000000); // default is transparent
				break;
			case ChatConstants.DS_ACKED:
				getIconView().setImageResource(R.drawable.ic_chat_msg_status_ok);
				mRowView.setBackgroundColor(0x00000000); // default is transparent
				break;
			case ChatConstants.DS_FAILED:
				getIconView().setImageResource(R.drawable.ic_chat_msg_status_failed);
				mRowView.setBackgroundColor(0x30ff0000); // default is transparent
				break;
			}
			if (from.equals(KeyAccessor.ROOMS_SERVER)) {
				mRowView.setBackgroundColor(0x300000ff); // default is transparent
			}
			getMessageView().setText(message);
			getMessageView().setTextSize(TypedValue.COMPLEX_UNIT_SP, chatWindow.mChatFontSize);
			getDateView().setTextSize(TypedValue.COMPLEX_UNIT_SP, chatWindow.mChatFontSize*2/3);
			getFromView().setTextSize(TypedValue.COMPLEX_UNIT_SP, chatWindow.mChatFontSize*2/3);
		}
		
		TextView getDateView() {
			if (mDateView == null) {
				mDateView = (TextView) mRowView.findViewById(R.id.chat_date);
			}
			return mDateView;
		}

		TextView getFromView() {
			if (mFromView == null) {
				mFromView = (TextView) mRowView.findViewById(R.id.chat_from);
			}
			return mFromView;
		}

		TextView getMessageView() {
			if (mMessageView == null) {
				mMessageView = (TextView) mRowView
						.findViewById(R.id.chat_message);
			}
			return mMessageView;
		}

		ImageView getIconView() {
			if (mIconView == null) {
				mIconView = (ImageView) mRowView
						.findViewById(R.id.iconView);
			}
			return mIconView;
		}

	}

	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN
				&& keyCode == KeyEvent.KEYCODE_ENTER) {
			sendMessageIfNotNull();
			return true;
		}
		return false;

	}

	public void afterTextChanged(Editable s) {
		if (mChatInput.getText().length() >= 1) {
			mChatInput.setOnKeyListener(this);
			mSendButton.setEnabled(true);
		}
	}

	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		// TODO Auto-generated method stub
	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

	private void showToastNotification(int message) {
		Toast toastNotification = Toast.makeText(this, message,
				Toast.LENGTH_SHORT);
		toastNotification.show();
	}

	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		File file = new File(Environment.getExternalStorageDirectory().getPath());
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, MainWindow.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		case R.id.menu_send_file:
			Intent fileIntent = new Intent(getApplicationContext(), FileExplore.class);
			fileIntent.setAction(android.content.Intent.ACTION_VIEW);
			fileIntent.setDataAndType(Uri.fromFile(file), "*/*");
			startActivityForResult(fileIntent, R.id.menu_send_file);
			return true;
		case R.id.menu_groupchat:
			startGroupChatStep1();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (data == null || requestCode != R.id.menu_send_file) {
			return;
		}
		String selectedFile = data.getStringExtra("selectedFile");
		long ms = new Date().getTime();
		if (selectedFile != null) {
			switch (requestCode) {
			case R.id.menu_send_file:
				new UploadFileTask(this).execute(selectedFile);
				break;
			}
		}
		Log.i(TAG, "Es hat " + (new Date().getTime() - ms) + " ms gedauert");
	}
	
	private void startGroupChatStep1() {
		final List<String> jids = new ArrayList<String>();
		final List<String> names = new ArrayList<String>();
		Cursor c = getContentResolver().query(RosterProvider.PARTICIPANTS_URI, new String[] {  ParticipantConstants._ID, ParticipantConstants.JID, ParticipantConstants.NAME }, 
				ParticipantConstants.ROOM + " = ? and " + ParticipantConstants.JID + " != ?", 
				new String[] { mWithJabberID, mConfig.jabberID }, ParticipantConstants.NAME);
		while (c.moveToNext()) {
			jids.add(c.getString(1));
			names.add(c.getString(2));
		}
		new AlertDialog.Builder(this)
			.setTitle(R.string.rooms_taskRecipient)
			.setSingleChoiceItems(names.toArray(new String[]{}), -1, null)
			.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
						String selectedParticipant = jids.get(selectedPosition);
						startTaskStep2(selectedParticipant);
					}
				})
		.setNegativeButton(android.R.string.cancel, null)
		.create().show();
	}
	
	private void startTaskStep2(final String participant) {
		final EditText input = new EditText(this);
		new AlertDialog.Builder(this)
		.setTitle(R.string.rooms_taskTitle)
		.setView(input)
		.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						mServiceAdapter.sendTask(mWithJabberID, input.getText().toString(), participant);
					}
				})
		.setNegativeButton(android.R.string.cancel, null)
		.create().show();
	}

	private static final String[] STATUS_QUERY = new String[] {
		RosterProvider.RosterConstants.STATUS_MODE,
		RosterProvider.RosterConstants.STATUS_MESSAGE,
	};
	
	private void updateContactStatus() {
		Cursor cursor = getContentResolver().query(RosterProvider.CONTENT_URI, STATUS_QUERY,
					RosterProvider.RosterConstants.JID + " = ?", new String[] { mWithJabberID }, null);
		int MODE_IDX = cursor.getColumnIndex(RosterProvider.RosterConstants.STATUS_MODE);
		int MSG_IDX = cursor.getColumnIndex(RosterProvider.RosterConstants.STATUS_MESSAGE);

		if (cursor.getCount() == 1) {
			cursor.moveToFirst();
			int status_mode = cursor.getInt(MODE_IDX);
			String status_message = cursor.getString(MSG_IDX);
			Log.d(TAG, "contact status changed: " + status_mode + " " + status_message);
			mSubTitle.setVisibility((status_message != null && status_message.length() != 0)?
					View.VISIBLE : View.GONE);
			mSubTitle.setText(status_message);
			if (mServiceAdapter == null || !mServiceAdapter.isServiceAuthenticated())
				status_mode = 0; // override icon if we are offline
			mStatusMode.setImageResource(StatusMode.values()[status_mode].getDrawableId());
		}
		cursor.close();
	}

	private class ContactObserver extends ContentObserver {
		public ContactObserver() {
			super(new Handler());
		}

		public void onChange(boolean selfChange) {
			Log.d(TAG, "ContactObserver.onChange: " + selfChange);
			updateContactStatus();
		}
	}
	
	class UploadFileTask extends AsyncTask<String, Void, String> {
		private ProgressDialog spinner;
		private Exception ex;
		
		public UploadFileTask(Context ctx) {
			super();
			spinner = new ProgressDialog(ctx);			
		    spinner.setMessage(ctx.getString(R.string.rooms_uploading));
		}
		
		@Override
		protected void onPreExecute() {
		    spinner.show();
		}

		@Override
	    protected String doInBackground(String... selectedFiles) {
			try {
				return mServiceAdapter.uploadFile(ChatWindow.this, mWithJabberID, selectedFiles[0]);
			}
			catch (Exception ex) {
				this.ex = ex;
				Log.w(TAG, ex.getMessage(), ex);
				return null;
			}
	    }

	    protected void onPostExecute(String url) {
	    	spinner.dismiss();
	    	if (url == null) {
	    		Toast.makeText(getBaseContext(), ex != null ? ex.getMessage() : "Error", Toast.LENGTH_LONG).show();
	    	}
	    	else {
	    		Log.i(TAG, url);
	    	}
	    }
	}

	class DownloadFileTask extends AsyncTask<JSONObject, Void, String> {
		private ProgressDialog spinner;
		private Exception ex;
		private String mimeType;
		
		public DownloadFileTask(Context ctx) {
			super();
			spinner = new ProgressDialog(ctx);			
		    spinner.setMessage(ctx.getString(R.string.rooms_downloading));
		}
		
		@Override
		protected void onPreExecute() {
		    spinner.show();
		}

		@Override
	    protected String doInBackground(JSONObject... fileInfo) {
			try {
				mimeType = fileInfo[0].getString("mime-type");
				return mServiceAdapter.downloadFile(fileInfo[0], ChatWindow.this);
			}
			catch (Exception ex) {
				this.ex = ex;
				return null;
			}
		}

	    protected void onPostExecute(String uri) {
	    	spinner.dismiss();
	    	if (uri == null) {
	    		Toast.makeText(getBaseContext(), ex != null ? ex.getMessage() : "Error", Toast.LENGTH_LONG).show();
	    	}
	    	else {
	    		Log.i(TAG, uri);
	    		Intent intent = new Intent();
	    		File file = new File(uri);
	            intent.setAction(android.content.Intent.ACTION_VIEW);
	            intent.setDataAndType(Uri.fromFile(file), mimeType);
	            startActivity(intent); 
	    	}
	    }
	}
	
	private void showTaskDialog(final int id, JSONObject taskData) throws Exception {
		
		final List<String> options = new ArrayList<String>();
		final String text = taskData.getString("text"); 
		for(int i = 0; i < taskData.getJSONArray("options").length(); i++){
		    options.add(taskData.getJSONArray("options").getString(i));
		}		
		new AlertDialog.Builder(this)
			.setTitle(text)
			.setSingleChoiceItems(options.toArray(new String[]{}), 0, new DialogInterface.OnClickListener() {
           @Override
           public void onClick(DialogInterface dialog, int which) {
           }
       })
       .setPositiveButton(android.R.string.ok,
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
					String selectedOption = options.get(selectedPosition);
					ContentValues values = new ContentValues();
					values.put(ChatConstants.TYPE, RoomsMessageType.TaskResponse.ordinal());
					values.put(ChatConstants.MESSAGE, text + " [" + selectedOption + "]");
					getContentResolver().update(ChatProvider.CONTENT_URI, values,
							ChatProvider.ChatConstants._ID + " = ?", new String[]{ String.valueOf(id) });
					mServiceAdapter.sendTaskResponse(selectedOption);
				}
			})
		.setNegativeButton(android.R.string.cancel, null)
		.create().show();
	}
}
