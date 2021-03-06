package org.yaxim.androidclient;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;
import org.yaxim.androidclient.IXMPPRosterCallback.Stub;
import org.yaxim.androidclient.crypto.KeyAccessor;
import org.yaxim.androidclient.data.ChatProvider;
import org.yaxim.androidclient.data.ChatProvider.ChatConstants;
import org.yaxim.androidclient.data.RosterProvider;
import org.yaxim.androidclient.data.RosterProvider.ParticipantConstants;
import org.yaxim.androidclient.data.RosterProvider.RoomsConstants;
import org.yaxim.androidclient.data.RosterProvider.RosterConstants;
import org.yaxim.androidclient.data.YaximConfiguration;
import org.yaxim.androidclient.dialogs.AddRosterItemDialog;
import org.yaxim.androidclient.dialogs.FirstStartDialog;
import org.yaxim.androidclient.dialogs.GroupNameView;
import org.yaxim.androidclient.preferences.AccountPrefs;
import org.yaxim.androidclient.preferences.MainPrefs;
import org.yaxim.androidclient.service.IXMPPRosterService;
import org.yaxim.androidclient.service.XMPPService;
import org.yaxim.androidclient.util.ConnectionState;
import org.yaxim.androidclient.util.PreferenceConstants;
import org.yaxim.androidclient.util.StatusMode;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.Window;
import com.nullwire.trace.ExceptionHandler;

public class MainWindow extends SherlockFragmentActivity {

	private static final String TAG = "yaxim.MainWindow";

	private YaximConfiguration mConfig;

	private Intent xmppServiceIntent;
	private ServiceConnection xmppServiceConnection;
	private XMPPRosterServiceAdapter serviceAdapter;
	private Stub rosterCallback;

	private ActionBar actionBar;
	private String mTheme;
	
	private final Handler mainHandler = new Handler();
	
	private RosterTabFragment rosterTab;
	private RoomsTabFragment roomsTab;
	
	private BroadcastReceiver pushReceiver;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		Log.i(TAG, getString(R.string.build_version));
		mConfig = YaximApplication.getConfig(this);
		mTheme = mConfig.theme;
		setTheme(mConfig.getTheme());
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_ACTION_BAR);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, ActionBar.DISPLAY_SHOW_TITLE);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setHomeButtonEnabled(true);
		registerCrashReporter();
		
	    actionBar.addTab(actionBar.newTab().setText(R.string.rooms_rooms)
	              .setTabListener(new MainTabListener<RoomsTabFragment>(this, "tab2", RoomsTabFragment.class)));		
	    actionBar.addTab(actionBar.newTab().setText(R.string.rooms_contacts)
	              .setTabListener(new MainTabListener<RosterTabFragment>(this, "tab1", RosterTabFragment.class)));		

		showFirstStartUpDialogIfPrefsEmpty();
		
		registerXMPPService();
		createUICallback();
		setContentView(R.layout.main);
        getContentResolver().registerContentObserver( RosterProvider.COMPANIES_URI, true, new CompaniesObserver() );
	}

	public int getStatusActionIcon() {
		final boolean showOffline = !isConnected() || isConnecting()
					|| getStatusMode() == null;

		if (showOffline) {
			return StatusMode.offline.getDrawableId();
		}

		return getStatusMode().getDrawableId();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (serviceAdapter != null) {
            serviceAdapter.unregisterUICallback(rosterCallback);
        }

		YaximApplication.getApp(this).mMTM.unbindDisplayActivity(this);
		unbindXMPPService();
		
		NotifyingHandler.unregisterDynamicBroadcastReceiver(this, pushReceiver);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mConfig.theme.equals(mTheme) == false) {
			// restart
			final Intent restartIntent = new Intent(this, MainWindow.class);
			restartIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(restartIntent);
			finish();
		}
		final FragmentManager fragmentManager = this.getSupportFragmentManager();
		rosterTab = (RosterTabFragment)fragmentManager.findFragmentByTag("tab1");
		roomsTab = (RoomsTabFragment)fragmentManager.findFragmentByTag("tab2");
		
		displayOwnStatus();
		bindXMPPService();

		YaximApplication.getApp(this).mMTM.bindDisplayActivity(this);

		// handle imto:// intent after restoring service connection
		mainHandler.post(new Runnable() {
			@Override
            public void run() {
				 handleJabberIntent();
			}});
		// handle SEND action
		handleSendIntent();
		
		pushReceiver = NotifyingHandler.registerDynamicBroadcastReceiver(this);
	}

	public void handleSendIntent() {
		final Intent intent = getIntent();
		final String action = intent.getAction();
		if ((action != null) && (action.equals(Intent.ACTION_SEND))) {
			showToastNotification(R.string.chooseContact);
			setTitle(R.string.chooseContact);
		}
	}

	public boolean isConnected() {
		return serviceAdapter != null && serviceAdapter.isAuthenticated();
	}
	
	public boolean isConnecting() {
		return serviceAdapter != null && serviceAdapter.getConnectionState() == ConnectionState.CONNECTING;
	}

	void doMarkAllAsRead(final String JID) {
		final ContentValues values = new ContentValues();
		values.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_SENT_OR_READ);

		getContentResolver().update(ChatProvider.CONTENT_URI, values,
				ChatProvider.ChatConstants.JID + " = ? AND "
						+ ChatConstants.DIRECTION + " = " + ChatConstants.INCOMING + " AND "
						+ ChatConstants.DELIVERY_STATUS + " = " + ChatConstants.DS_NEW,
				new String[]{JID});
	}

	void removeChatHistory(final String JID) {
		getContentResolver().delete(ChatProvider.CONTENT_URI,
				ChatProvider.ChatConstants.JID + " = ?", new String[] { JID });
	}

	void removeChatHistoryDialog(final String JID, final String userName) {
		new AlertDialog.Builder(this)
			.setTitle(R.string.deleteChatHistory_title)
			.setMessage(getString(R.string.deleteChatHistory_text, userName, JID))
			.setPositiveButton(android.R.string.yes,
					new DialogInterface.OnClickListener() {
						@Override
                        public void onClick(final DialogInterface dialog, final int which) {
							removeChatHistory(JID);
						}
					})
			.setNegativeButton(android.R.string.no, null)
			.create().show();
	}

	void removeRosterItemDialog(final String JID, final String userName) {
		new AlertDialog.Builder(this)
			.setTitle(R.string.deleteRosterItem_title)
			.setMessage(getString(R.string.deleteRosterItem_text, userName, JID))
			.setPositiveButton(android.R.string.yes,
					new DialogInterface.OnClickListener() {
						@Override
                        public void onClick(final DialogInterface dialog, final int which) {
							serviceAdapter.removeRosterItem(JID);
						}
					})
			.setNegativeButton(android.R.string.no, null)
			.create().show();
	}

	boolean addToRosterDialog(final String jid) {
		if (serviceAdapter != null && serviceAdapter.isAuthenticated()) {
			new AddRosterItemDialog(this, serviceAdapter, jid).show();
			return true;
		} else {
			showToastNotification(R.string.Global_authenticate_first);
			return false;
		}
	}

	void rosterAddRequestedDialog(final String jid, final String message) {
		new AlertDialog.Builder(this)
			.setTitle(R.string.subscriptionRequest_title)
			.setMessage(getString(R.string.subscriptionRequest_text, jid, message))
			.setPositiveButton(android.R.string.yes,
					new DialogInterface.OnClickListener() {
						@Override
                        public void onClick(final DialogInterface dialog, final int which) {
							serviceAdapter.sendPresenceRequest(jid, "subscribed");
							addToRosterDialog(jid);
						}
					})
			.setNegativeButton(android.R.string.no, 
					new DialogInterface.OnClickListener() {
						@Override
                        public void onClick(final DialogInterface dialog, final int which) {
							serviceAdapter.sendPresenceRequest(jid, "unsubscribed");
						}
					})
			.create().show();
	}

	abstract class EditOk {
		abstract public void ok(String result);
	}

	void editTextDialog(final int titleId, final CharSequence message, final String text,
			final EditOk ok) {
		final LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		final View layout = inflater.inflate(R.layout.edittext_dialog,
		                               (ViewGroup) findViewById(R.id.layout_root));

		final TextView messageView = (TextView) layout.findViewById(R.id.text);
		messageView.setText(message);
		final EditText input = (EditText) layout.findViewById(R.id.editText);
		input.setTransformationMethod(android.text.method.SingleLineTransformationMethod.getInstance());
		input.setText(text);
		new AlertDialog.Builder(this)
			.setTitle(titleId)
			.setView(layout)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
                        public void onClick(final DialogInterface dialog, final int which) {
							final String newName = input.getText().toString();
							if (newName.length() != 0) {
                                ok.ok(newName);
                            }
						}})
			.setNegativeButton(android.R.string.cancel, null)
			.create().show();
	}

	void renameRosterItemDialog(final String JID, final String userName) {
		editTextDialog(R.string.RenameEntry_title,
				getString(R.string.RenameEntry_summ, userName, JID),
				userName, new EditOk() {
					@Override
                    public void ok(final String result) {
						serviceAdapter.renameRosterItem(JID, result);
					}
				});
	}

	void renameRosterGroupDialog(final String groupName) {
		editTextDialog(R.string.RenameGroup_title,
				getString(R.string.RenameGroup_summ, groupName),
				groupName, new EditOk() {
					@Override
                    public void ok(final String result) {
						serviceAdapter.renameRosterGroup(groupName, result);
					}
				});
	}
	
	void moveRosterItemToGroupDialog(final String jabberID) {
		final LayoutInflater inflater = (LayoutInflater)getSystemService(
			      LAYOUT_INFLATER_SERVICE);
		final View group = inflater.inflate(R.layout.moverosterentrytogroupview, null, false);
		final GroupNameView gv = (GroupNameView)group.findViewById(R.id.moverosterentrytogroupview_gv);
		gv.setGroupList(rosterTab.getRosterGroups());
		new AlertDialog.Builder(this)
			.setTitle(R.string.MoveRosterEntryToGroupDialog_title)
			.setView(group)
			.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
                    public void onClick(final DialogInterface dialog, final int which) {
						Log.d(TAG, "new group: " + gv.getGroupName());
						serviceAdapter.moveRosterItemToGroup(jabberID,
								gv.getGroupName());
					}
				})
			.setNegativeButton(android.R.string.cancel, null)
			.create().show();
	}

	@Override
    public boolean onContextItemSelected(final MenuItem item) {
		if (item.getMenuInfo() instanceof ExpandableListContextMenuInfo) {
			return applyMenuContextChoice(item);
		}
		else {
			return applyRoomsMenuContextChoice(item);
		}
	}

	private boolean applyRoomsMenuContextChoice(final MenuItem item) {
		final int itemID = item.getItemId();
		if (getActiveTab() != null) {
			switch (itemID) {
			case R.id.rooms_close:
				final int position = ((AdapterContextMenuInfo)item.getMenuInfo()).position;
				final String roomID = roomsTab.getRoomId(position);
				getContentResolver().delete(RosterProvider.PARTICIPANTS_URI, ParticipantConstants.ROOM + " = ?", new String[] { roomID });
				getContentResolver().delete(RosterProvider.ROOMS_URI, RoomsConstants.ID + " = ?", new String[] { roomID });
				return true;
			}
		}
		return false;
	}
	
	private boolean applyMenuContextChoice(final MenuItem item) {

		final ExpandableListContextMenuInfo contextMenuInfo = (ExpandableListContextMenuInfo) item
				.getMenuInfo();
		final long packedPosition = contextMenuInfo.packedPosition;
		if (rosterTab == null) {
			rosterTab = (RosterTabFragment)getActiveTab();
		}

		if (rosterTab != null && rosterTab.isChild(packedPosition)) {

			final String userJid = rosterTab.getPackedItemRow(packedPosition, RosterConstants.JID);
			final String userName = rosterTab.getPackedItemRow(packedPosition, RosterConstants.ALIAS);
			Log.d(TAG, "action for contact " + userName + "/" + userJid);

			final int itemID = item.getItemId();

			switch (itemID) {
			case R.id.roster_contextmenu_contact_open_chat:
				startChatActivity(userJid, userName, null);
				return true;

			case R.id.roster_contextmenu_contact_mark_all_as_read:
				doMarkAllAsRead(userJid);
				return true;

			case R.id.roster_contextmenu_contact_delmsg:
				removeChatHistoryDialog(userJid, userName);
				return true;

			case R.id.roster_contextmenu_contact_delete:
				if (!isConnected()) { showToastNotification(R.string.Global_authenticate_first); return true; }
				removeRosterItemDialog(userJid, userName);
				return true;

			case R.id.roster_contextmenu_contact_rename:
				if (!isConnected()) { showToastNotification(R.string.Global_authenticate_first); return true; }
				renameRosterItemDialog(userJid, userName);
				return true;

			case R.id.roster_contextmenu_contact_request_auth:
				if (!isConnected()) { showToastNotification(R.string.Global_authenticate_first); return true; }
				serviceAdapter.sendPresenceRequest(userJid, "subscribe");
				return true;

			case R.id.roster_contextmenu_contact_change_group:
				if (!isConnected()) { showToastNotification(R.string.Global_authenticate_first); return true; }
				moveRosterItemToGroupDialog(userJid);
				return true;
				
			}
		} else {

			final int itemID = item.getItemId();
			final String seletedGroup = rosterTab.getPackedItemRow(packedPosition, RosterConstants.GROUP);
			Log.d(TAG, "action for group " + seletedGroup);

			switch (itemID) {
			case R.id.roster_contextmenu_group_rename:
				if (!isConnected()) { showToastNotification(R.string.Global_authenticate_first); return true; }
				renameRosterGroupDialog(seletedGroup);
				return true;

			}
		}
		return false;
	}

	public void startChatActivity(final String user, final String userName, final String message) {
		final Intent chatIntent = new Intent(this,
				org.yaxim.androidclient.chat.ChatWindow.class);
		final Uri userNameUri = Uri.parse(user);
		chatIntent.setData(userNameUri);
		chatIntent.putExtra(org.yaxim.androidclient.chat.ChatWindow.INTENT_EXTRA_USERNAME, userName);
		if (message != null) {
			chatIntent.putExtra(org.yaxim.androidclient.chat.ChatWindow.INTENT_EXTRA_MESSAGE, message);
		}
		startActivity(chatIntent);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getSupportMenuInflater().inflate(R.menu.roster_options, menu);
		return true;
	}

	void setMenuItem(final Menu menu, final int itemId, final int iconId, final CharSequence title) {
		final com.actionbarsherlock.view.MenuItem item = menu.findItem(itemId);
		if (item == null) {
            return;
        }
		item.setIcon(iconId);
		item.setTitle(title);
	}

	@Override
	public boolean onPrepareOptionsMenu(final Menu menu) {
		setMenuItem(menu, R.id.menu_connect, getConnectDisconnectIcon(),
				getConnectDisconnectText());
		//setMenuItem(menu, R.id.menu_show_hide, getShowHideMenuIcon(),
		//		getShowHideMenuText());
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final com.actionbarsherlock.view.MenuItem item) {
		return applyMainMenuChoice(item);
	}

	public StatusMode getStatusMode() {
		return StatusMode.fromString(mConfig.statusMode);
	}

	public void setAndSaveStatus(final StatusMode statusMode, final String message) {
		final SharedPreferences.Editor prefedit = PreferenceManager
				.getDefaultSharedPreferences(this).edit();
		// do not save "offline" to prefs, or else!
		if (statusMode != StatusMode.offline) {
            prefedit.putString(PreferenceConstants.STATUS_MODE, statusMode.name());
        }
		if (!message.equals(mConfig.statusMessage)) {
			final List<String> smh = new ArrayList<String>(java.util.Arrays.asList(mConfig.statusMessageHistory));
			if (!smh.contains(message)) {
                smh.add(message);
            }
			final String smh_joined = android.text.TextUtils.join("\036", smh);
			prefedit.putString(PreferenceConstants.STATUS_MESSAGE_HISTORY, smh_joined);
		}
		prefedit.putString(PreferenceConstants.STATUS_MESSAGE, message);
		prefedit.commit();

		displayOwnStatus();

		// check if we are connected and want to go offline
		final boolean needToDisconnect = (statusMode == StatusMode.offline) && isConnected();
		// check if we want to reconnect
		final boolean needToConnect = (statusMode != StatusMode.offline) &&
				serviceAdapter.getConnectionState() == ConnectionState.OFFLINE;

		if (needToConnect || needToDisconnect) {
            toggleConnection();
        } else if (isConnected()) {
            serviceAdapter.setStatusFromConfig();
        }
	}

	private void displayOwnStatus() {
		// This and many other things like it should be done with observer
		actionBar.setIcon(getStatusActionIcon());

		if (mConfig.statusMessage.equals("")) {
			actionBar.setSubtitle(null);
		} else {
			actionBar.setSubtitle(mConfig.statusMessage);
		}
	}

	private void aboutDialog() {
		final LayoutInflater inflater = (LayoutInflater)getSystemService(
			      LAYOUT_INFLATER_SERVICE);
		final View about = inflater.inflate(R.layout.aboutview, null, false);
		String versionTitle = getString(R.string.AboutDialog_title);
		try {
			final PackageInfo pi = getPackageManager()
						.getPackageInfo(getPackageName(), 0);
			versionTitle += " v" + pi.versionName;
		} catch (final NameNotFoundException e) {
		}

		// fix translator-credits: hide if unset, format otherwise
		final TextView tcv = (TextView)about.findViewById(R.id.translator_credits);
		if (tcv.getText().equals("translator-credits")) {
            tcv.setVisibility(View.GONE);
        }

		new AlertDialog.Builder(this)
			.setTitle(versionTitle)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setView(about)
			.setPositiveButton(android.R.string.ok, null)
			.setNeutralButton(R.string.AboutDialog_Vote, new DialogInterface.OnClickListener() {
				@Override
                public void onClick(final DialogInterface dialog, final int item) {
					final Intent market = new Intent(Intent.ACTION_VIEW,
						Uri.parse("market://details?id=" + getPackageName()));
					market.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
					try {
						startActivity(market);
					} catch (final Exception e) {
						// do not crash
						Log.e(TAG, "could not go to market: " + e);
					}
				}
			})
			.create().show();
	}

	private boolean applyMainMenuChoice(final com.actionbarsherlock.view.MenuItem item) {

		final int itemID = item.getItemId();
		final String name = mConfig.jabberID;

		switch (itemID) {
		case R.id.menu_connect:
			toggleConnection();
			return true;

		case R.id.menu_add_friend:
			addContact();
			return true;

		case R.id.menu_add_room:
			addRoomDialog1();
			return true;

		case R.id.menu_show_hide:
			setOfflineContactsVisibility(!mConfig.showOffline);
			rosterTab.updateRoster();
			return true;

		case android.R.id.home:
		case R.id.menu_status:
			//new ChangeStatusDialog(this, StatusMode.fromString(mConfig.statusMode),
			//		mConfig.statusMessage, mConfig.statusMessageHistory).show();
			return false;

		case R.id.menu_exit:
			PreferenceManager.getDefaultSharedPreferences(this).edit().
				putBoolean(PreferenceConstants.CONN_STARTUP, false).commit();
			stopService(xmppServiceIntent);
			finish();
			return true;

		case R.id.menu_settings:
			startActivity(new Intent(this, MainPrefs.class));
			return true;

		case R.id.menu_about:
			aboutDialog();
			return true;
			
		case R.id.menu_register:
			registerDialog1();
			return true;
			
		case R.id.menu_sync:
			syncAddressBook();
			return true;

		case R.id.menu_web_login:
			generateWebLoginToken();
			return true;
		}

		return false;

	}

	/** Sets if all contacts are shown in the roster or online contacts only. */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB) // required for Sherlock's invalidateOptionsMenu */
	private void setOfflineContactsVisibility(final boolean showOffline) {
		PreferenceManager.getDefaultSharedPreferences(this).edit().
			putBoolean(PreferenceConstants.SHOW_OFFLINE, showOffline).commit();
		invalidateOptionsMenu();
	}

	private void updateConnectionState(final ConnectionState cs) {
		Log.d(TAG, "updateConnectionState: " + cs);
		displayOwnStatus();
		final Fragment activeTab = getActiveTab();
		if (activeTab == null || activeTab.getView() == null) {
			return;
		}
		final TextView mConnectingText = (TextView)activeTab.getView().findViewById(R.id.error_view);
		boolean spinTheSpinner = false;
		switch (cs) {
		case CONNECTING:
		case DISCONNECTING:
			spinTheSpinner = true;
            //$FALL-THROUGH$
		case DISCONNECTED:
		case RECONNECT_NETWORK:
		case RECONNECT_DELAYED:
		case OFFLINE:
			if (cs == ConnectionState.OFFLINE) {
                mConnectingText.setText(R.string.conn_offline);
            } else {
                mConnectingText.setText(serviceAdapter.getConnectionStateString());
            }
			mConnectingText.setVisibility(View.VISIBLE);
			setSupportProgressBarIndeterminateVisibility(spinTheSpinner);
			break;
		case ONLINE:
			mConnectingText.setVisibility(View.GONE);
			setSupportProgressBarIndeterminateVisibility(false);
		}
	}
	
	public void startConnection(final boolean create_account) {
		xmppServiceIntent.putExtra("create_account", create_account);
		startService(xmppServiceIntent);
	}

	// this function changes the prefs to keep the connection
	// according to the requested state
	private void toggleConnection() {
		if (!mConfig.jid_configured) {
			startActivity(new Intent(this, AccountPrefs.class));
			return;
		}
		final boolean oldState = isConnected() || isConnecting();

		PreferenceManager.getDefaultSharedPreferences(this).edit().
			putBoolean(PreferenceConstants.CONN_STARTUP, !oldState).commit();
		if (oldState) {
			serviceAdapter.disconnect();
			stopService(xmppServiceIntent);
		} else {
            startConnection(false);
        }
	}

	private int getConnectDisconnectIcon() {
		if (isConnected() || isConnecting()) {
			return R.drawable.ic_menu_unplug;
		}
		return R.drawable.ic_menu_plug;
	}

	private String getConnectDisconnectText() {
		if (isConnected() || isConnecting()) {
			return getString(R.string.Menu_disconnect);
		}
		return getString(R.string.Menu_connect);
	}

	private void registerXMPPService() {
		Log.i(TAG, "called startXMPPService()");
		xmppServiceIntent = new Intent(this, XMPPService.class);
		xmppServiceIntent.setAction("org.yaxim.androidclient.XMPPSERVICE");

		xmppServiceConnection = new ServiceConnection() {

			@Override
            @TargetApi(Build.VERSION_CODES.HONEYCOMB) // required for Sherlock's invalidateOptionsMenu */
			public void onServiceConnected(final ComponentName name, final IBinder service) {
				Log.i(TAG, "called onServiceConnected()");
				serviceAdapter = new XMPPRosterServiceAdapter(
						IXMPPRosterService.Stub.asInterface(service));
				serviceAdapter.registerUICallback(rosterCallback);
				Log.i(TAG, "getConnectionState(): "
						+ serviceAdapter.getConnectionState());
				invalidateOptionsMenu();	// to load the action bar contents on time for access to icons/progressbar
				final ConnectionState cs = serviceAdapter.getConnectionState();
				updateConnectionState(cs);
				
				if (rosterTab != null) {
					rosterTab.updateRoster();
				}

				// when returning from prefs to main activity, apply new config
				if (mConfig.reconnect_required && cs == ConnectionState.ONLINE) {
					// login config changed, force reconnection
					serviceAdapter.disconnect();
					serviceAdapter.connect();
				} else if (mConfig.presence_required && isConnected()) {
                    serviceAdapter.setStatusFromConfig();
                }
			}

			@Override
            public void onServiceDisconnected(final ComponentName name) {
				Log.i(TAG, "called onServiceDisconnected()");
			}
		};
	}

	private void unbindXMPPService() {
		try {
			unbindService(xmppServiceConnection);
		} catch (final IllegalArgumentException e) {
			Log.e(TAG, "Service wasn't bound!");
		}
	}

	private void bindXMPPService() {
		bindService(xmppServiceIntent, xmppServiceConnection, BIND_AUTO_CREATE);
	}

	private void createUICallback() {
		rosterCallback = new IXMPPRosterCallback.Stub() {
			@Override
			public void connectionStateChanged(final int connectionstate)
						throws RemoteException {
				mainHandler.post(new Runnable() {
					@Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB) // required for Sherlock's invalidateOptionsMenu */
					public void run() {
						final ConnectionState cs = ConnectionState.values()[connectionstate];
						//Log.d(TAG, "connectionStatusChanged: " + cs);
						updateConnectionState(cs);
						invalidateOptionsMenu();
					}
				});
			}
		};
	}

	private void showFirstStartUpDialogIfPrefsEmpty() {
		Log.i(TAG, "showFirstStartUpDialogIfPrefsEmpty, JID: "
						+ mConfig.jabberID);
		if (mConfig.jabberID.length() < 3 || mConfig.jabberID.equals(KeyAccessor.NEW_USER)) {
			// load preference defaults
			PreferenceManager.setDefaultValues(this, R.layout.mainprefs, false);
			PreferenceManager.setDefaultValues(this, R.layout.accountprefs, false);

			// prevent a start-up with empty JID
			final SecureRandom random = new SecureRandom();
			final String ressource = new BigInteger(64, random).toString(32);
			final SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(this);
			prefs.edit().putBoolean(PreferenceConstants.CONN_STARTUP, true)
				.putString(PreferenceConstants.JID, KeyAccessor.NEW_USER)
				.putString(PreferenceConstants.PASSWORD, KeyAccessor.NEW_USER_PASSWORD)
				.putString(PreferenceConstants.RESSOURCE, ressource)
				.commit();

			// show welcome dialog
			registerDialog1();
		}
	}

	public static Intent createIntent(final Context context) {
		final Intent i = new Intent(context, MainWindow.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return i;
	}

	protected void showToastNotification(final int message) {
		final Toast tmptoast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
		tmptoast.show();
	}

	private void registerCrashReporter() {
		if (mConfig.reportCrash) {
			ExceptionHandler.register(this, "http://duenndns.de/yaxim-crash/");
		}
	}
	
	public void handleJabberIntent() {
		final Intent intent = getIntent();
		final String action = intent.getAction();
		final Uri data = intent.getData();
		if ((action != null) && (action.equals(Intent.ACTION_SENDTO))
				&& data != null && data.getHost().equals("jabber")) {
			final String jid = data.getPathSegments().get(0);
			Log.d(TAG, "handleJabberIntent: " + jid);

			final List<String[]> contacts = rosterTab.getRosterContacts();
			for (final String[] c : contacts) {
				if (jid.equalsIgnoreCase(c[0])) {
					// found it
					startChatActivity(c[0], c[1], null);
					finish();
					return;
				}
			}
			// did not find in roster, try to add
			if (!addToRosterDialog(jid)) {
                finish();
            }
		}
	}
	
	Handler getHandler() {
		return mainHandler;
	}
	
	public List<String> getRosterGroups() {
		return rosterTab.getRosterGroups();
	}
	
	public Fragment getActiveTab() {
		final FragmentManager fragmentManager = this.getSupportFragmentManager();
		rosterTab = (RosterTabFragment)fragmentManager.findFragmentByTag("tab1");
		roomsTab = (RoomsTabFragment)fragmentManager.findFragmentByTag("tab2");
		return rosterTab != null && rosterTab.isVisible() ? rosterTab : roomsTab;
	}
	
	private void addRoomDialog1() {
		final List<String> jids = new ArrayList<String>();
		final List<String> names = new ArrayList<String>();
		final Set<String> selectedJids = new HashSet<String>();
		final Cursor c = getContentResolver().query(RosterProvider.CONTENT_URI, new String[] { RosterConstants.JID, RosterConstants.ALIAS }, 
				null, null, RosterConstants.ALIAS);
		while (c.moveToNext()) {
			jids.add(c.getString(0));
			names.add(c.getString(1));
		}
		new AlertDialog.Builder(this)
			.setTitle(R.string.rooms_participants)
			.setMultiChoiceItems(names.toArray(new String[]{}), null, new DialogInterface.OnMultiChoiceClickListener() {
               @Override
               public void onClick(final DialogInterface dialog, final int which, final boolean isChecked) {
                   if (isChecked) {
                       selectedJids.add(jids.get(which));
                   } else {
                       selectedJids.remove(jids.get(which));
                   }
               }
           })
		.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
                    public void onClick(final DialogInterface dialog, final int which) {
						addRoomDialog2(selectedJids);
					}
				})
		.setNegativeButton(android.R.string.cancel, null)
		.create().show();
	}
	
	private void addRoomDialog2(final Set<String> participants) {
		final EditText input = new EditText(this);
		new AlertDialog.Builder(this)
		.setTitle(R.string.rooms_title)
		.setView(input)
		.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
                    public void onClick(final DialogInterface dialog, final int which) {
						openRoom(input.getText().toString(), participants);
					}
				})
		.setNegativeButton(android.R.string.cancel, null)
		.create().show();
	}

	public void openRoom(final String title, final Set<String> participants) {
		serviceAdapter.openRoom(null, title, participants);
	}
	
	private void registerDialog1() {
		final EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_PHONE);
		new AlertDialog.Builder(this)
		.setTitle(R.string.rooms_phoneNumber)
		.setView(input)
		.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
                    public void onClick(final DialogInterface dialog, final int which) {
						try {
							serviceAdapter.sendRegistrationMessage1(input.getText().toString());
							new FirstStartDialog(MainWindow.this, serviceAdapter).show();
						}
						catch (final Exception ex) {
							Toast.makeText(MainWindow.this, "Registration failed", Toast.LENGTH_SHORT).show();
						}
					}
				})
		.setNegativeButton(android.R.string.cancel, null)
		.create().show();
	}
	
	private void syncAddressBook() {

        final AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setMessage( R.string.rooms_sync_confirm ).setPositiveButton( android.R.string.yes,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick( final DialogInterface dialog, final int which ) {

                        serviceAdapter.syncContacts();
                    }
                } )
                .setNegativeButton( android.R.string.no, null ).create().show();
	}
	
	private void addContact() {
		final EditText input = new EditText(this);
		new AlertDialog.Builder(this)
		.setTitle(R.string.rooms_search_contact)
		.setView(input)
		.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
                    public void onClick(final DialogInterface dialog, final int which) {
						serviceAdapter.searchContact(input.getText().toString());
					}
				})
		.setNegativeButton(android.R.string.cancel, null)
		.create().show();
	}
	
	private void generateWebLoginToken() {
		final SecureRandom random = new SecureRandom();
		final String token = new BigInteger(24, random).toString(32);
		serviceAdapter.sendWebToken(token);

		final AlertDialog dialog = new AlertDialog.Builder(this)
			.setTitle(R.string.Menu_webLogin)
			.setMessage(token)
			.setPositiveButton(android.R.string.ok, null)
			.create();
		dialog.show();
		
		final TextView messageText = (TextView)dialog.findViewById(android.R.id.message);
		messageText.setTextSize(50);
		messageText.setGravity(Gravity.CENTER);
	}

    class CompaniesObserver extends ContentObserver {
        public CompaniesObserver() {
            super( getHandler() );
        }

        @Override
        public void onChange( final boolean selfChange ) {

            final List< String > names = new ArrayList< String >();
            final List< String > keys = new ArrayList< String >();
            final List< String > selectedKeys = new ArrayList< String >();
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( MainWindow.this );
            try {
                final JSONObject jsonObject = new JSONObject( prefs.getString( PreferenceConstants.COMPANIES, "" ) );
                final boolean[] selectedIndexes = new boolean[jsonObject.getJSONArray( "companies" ).length()];
                for (int i = 0; i < jsonObject.getJSONArray( "companies" ).length(); i++) {
                    final JSONObject company = jsonObject.getJSONArray( "companies" ).getJSONObject( i );
                    names.add( company.getString( "name" ) );
                    keys.add(company.getString( "key" ));
                    selectedKeys.add( company.getString( "key" ) );
                    selectedIndexes[i] = true;
                }
                new AlertDialog.Builder( MainWindow.this )
                        .setTitle( R.string.rooms_companies )
                        .setMultiChoiceItems( names.toArray( new String[] {} ), selectedIndexes,
                                new DialogInterface.OnMultiChoiceClickListener() {
                                    @Override
                                    public void onClick( final DialogInterface dialog, final int which,
                                            final boolean isChecked ) {
                                        if ( isChecked ) {
                                            selectedKeys.add( keys.get( which ) );
                                        } else {
                                            selectedKeys.remove( keys.get( which ) );
                                        }
                                    }
                                } )
                        .setPositiveButton( android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick( final DialogInterface dialog, final int which ) {

                                        prefs.edit().remove( PreferenceConstants.COMPANIES ).commit();
                                        serviceAdapter.setCompanies( selectedKeys );
                                        syncAddressBook();
                                    }
                                } )
                        .setNegativeButton( android.R.string.cancel, null )
                        .create().show();
            } catch ( final Exception ex ) {
                Log.w( ex.getMessage(), ex );
            }
        }
    }

}
