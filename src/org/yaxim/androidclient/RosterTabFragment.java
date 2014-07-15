package org.yaxim.androidclient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.yaxim.androidclient.data.ChatProvider;
import org.yaxim.androidclient.data.ChatProvider.ChatConstants;
import org.yaxim.androidclient.data.RosterProvider;
import org.yaxim.androidclient.data.RosterProvider.RosterConstants;
import org.yaxim.androidclient.data.YaximConfiguration;
import org.yaxim.androidclient.util.SimpleCursorTreeAdapter;
import org.yaxim.androidclient.util.StatusMode;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

public class RosterTabFragment extends SherlockFragment {
	private static final String TAG = "yaxim.RosterFragment";

	private YaximConfiguration mConfig;
	private HashMap<String, Boolean> mGroupsExpanded = new HashMap<String, Boolean>();
	private ExpandableListView listView;
	private RosterExpListAdapter rosterListAdapter;

	private ContentResolver mContentResolver;
	private ContentObserver mRosterObserver;
	private ContentObserver mChatObserver;
	private MainWindow mainWindow;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		mConfig = YaximApplication.getConfig(this.getActivity());
		super.onCreate(savedInstanceState);
		mainWindow = (MainWindow)getActivity();
		mContentResolver = mainWindow.getContentResolver();
		mRosterObserver = new RosterObserver();
		mChatObserver = new ChatObserver();
		mainWindow.getContentResolver().registerContentObserver(RosterProvider.CONTENT_URI,
				true, mRosterObserver);
		mainWindow.getContentResolver().registerContentObserver(ChatProvider.CONTENT_URI,
				true, mChatObserver);
	}
	
	@Override
	public void onDestroy() {
		if (mainWindow != null) {
			mainWindow.getContentResolver().unregisterContentObserver(mRosterObserver);
			mainWindow.getContentResolver().unregisterContentObserver(mChatObserver);
		}
		super.onDestroy();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		storeExpandedState();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		updateRoster();
	}
	
	@Override
	public void onConfigurationChanged(android.content.res.Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.d(TAG, "onConfigurationChanged");
		listView.requestFocus();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.rostertab, container, false);
		listView = (ExpandableListView)view.findViewById(R.id.rosterList);
		rosterListAdapter = new RosterExpListAdapter(getActivity());
		listView.setAdapter(rosterListAdapter);
		listView.setOnChildClickListener(new OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				return RosterTabFragment.this.onChildClick(parent, v, groupPosition, childPosition, id);
			}
		});
		registerForContextMenu(listView);
		return view;
	}
	
	public void updateRoster() {
		loadUnreadCounters();
		rosterListAdapter.requery();
		restoreGroupsExpanded();
	}

	String getPackedItemRow(long packedPosition, String rowName) {
		int flatPosition = listView.getFlatListPosition(
				packedPosition);
		Cursor c = (Cursor) listView.getItemAtPosition(
				flatPosition);
		return c.getString(c.getColumnIndex(rowName));
	}

	void setupContenView() {
		registerForContextMenu(listView);
		listView.requestFocus();

		listView.setOnGroupClickListener(
				new ExpandableListView.OnGroupClickListener() {
					public boolean onGroupClick(ExpandableListView parent,
							View v, int groupPosition, long id) {
						groupClicked = true;
						return false;
					}
				});
		listView.setOnGroupCollapseListener(
				new ExpandableListView.OnGroupCollapseListener() {
					public void onGroupCollapse(int groupPosition) {
						handleGroupChange(groupPosition, false);
					}
				});
		listView.setOnGroupExpandListener(
				new ExpandableListView.OnGroupExpandListener() {
					public void onGroupExpand(int groupPosition) {
						handleGroupChange(groupPosition, true);
					}
				});
	}

	// need this to workaround unwanted OnGroupCollapse/Expand events
	boolean groupClicked = false;

	void handleGroupChange(int groupPosition, boolean isExpanded) {
		String groupName = getGroupName(groupPosition);
		if (groupClicked) {
			Log.d(TAG, "group status change: " + groupName + " -> "
					+ isExpanded);
			mGroupsExpanded.put(groupName, isExpanded);
			groupClicked = false;
			// } else {
			// if (!mGroupsExpanded.containsKey(name))
			// restoreGroupsExpanded();
		}
	}

	// store mGroupsExpanded into prefs (this is a hack, but SQLite /
	// content providers suck wrt. virtual groups)
	public void storeExpandedState() {
		SharedPreferences.Editor prefedit = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
		for (HashMap.Entry<String, Boolean> item : mGroupsExpanded.entrySet()) {
			prefedit.putBoolean("expanded_" + item.getKey(), item.getValue());
		}
		prefedit.commit();
	}

	// get the name of a roster group from the cursor
	public String getGroupName(int groupId) {
		return getPackedItemRow(
				ExpandableListView.getPackedPositionForGroup(groupId),
				RosterConstants.GROUP);
	}

	public void restoreGroupsExpanded() {
		if (getActivity() != null) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
			for (int count = 0; count < rosterListAdapter.getGroupCount(); count++) {
				String name = getGroupName(count);
				if (!mGroupsExpanded.containsKey(name))
					mGroupsExpanded.put(name, prefs.getBoolean("expanded_" + name, true));
				Log.d(TAG, "restoreGroupsExpanded: " + name + ": " + mGroupsExpanded.get(name));
				if (mGroupsExpanded.get(name))
					listView.expandGroup(count);
				else
					listView.collapseGroup(count);
			}
		}
	}

	private HashMap<String, Integer> mUnreadCounters = new HashMap<String, Integer>();
	private void loadUnreadCounters() {
		final String[] PROJECTION = new String[] { ChatConstants.JID, "count(*)" };
		final String SELECTION = ChatConstants.DIRECTION + " = " + ChatConstants.INCOMING + " AND " +
			ChatConstants.DELIVERY_STATUS + " = " + ChatConstants.DS_NEW +
			") GROUP BY (" + ChatConstants.JID; // hack!

		Cursor c = mContentResolver.query(ChatProvider.CONTENT_URI,
				PROJECTION, SELECTION, null, null);
		mUnreadCounters.clear();
		if(c!=null){
			while (c.moveToNext())
				mUnreadCounters.put(c.getString(0), c.getInt(1));
			c.close();
		}
	}

	private class ChatObserver extends ContentObserver {
		public ChatObserver() {
			super(mainWindow.getHandler());
		}
		public void onChange(boolean selfChange) {
			updateRoster();
		}
	}

	public class RosterExpListAdapter extends SimpleCursorTreeAdapter {

		public RosterExpListAdapter(Context context) {
			super(context, /* cursor = */ null, 
					R.layout.maingroup_row, GROUPS_FROM, GROUPS_TO,
					R.layout.mainchild_row,
					new String[] {
						RosterConstants.ALIAS,
						RosterConstants.STATUS_MESSAGE,
						RosterConstants.STATUS_MODE
					},
					new int[] {
						R.id.roster_screenname,
						R.id.roster_statusmsg,
						R.id.roster_icon
					});
		}

		public void requery() {
			String selectWhere = null;
			if (!mConfig.showOffline)
				selectWhere = OFFLINE_EXCLUSION;

			String[] query = GROUPS_QUERY_COUNTED;
			if(!mConfig.enableGroups) {
				query = GROUPS_QUERY_CONTACTS_DISABLED;
			}
			Cursor cursor = mContentResolver.query(RosterProvider.GROUPS_URI,
					query, selectWhere, null, RosterConstants.GROUP);
			Cursor oldCursor = getCursor();
			changeCursor(cursor);
			mainWindow.stopManagingCursor(oldCursor);
		}

		@Override
		protected Cursor getChildrenCursor(Cursor groupCursor) {
			// Given the group, we return a cursor for all the children within that group
			String selectWhere;
			int idx = groupCursor.getColumnIndex(RosterConstants.GROUP);
			String groupname = groupCursor.getString(idx);
			String[] args = null;

			if(!mConfig.enableGroups) {
				selectWhere = mConfig.showOffline ? "" : OFFLINE_EXCLUSION;
			} else {
				selectWhere = mConfig.showOffline ? "" : OFFLINE_EXCLUSION + " AND ";
				selectWhere += RosterConstants.GROUP + " = ?";
				args = new String[] { groupname };
			}
			return mContentResolver.query(RosterProvider.CONTENT_URI, ROSTER_QUERY,
				selectWhere, args, null);
		}

		@Override
		protected void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded) {
			super.bindGroupView(view, context, cursor, isExpanded);
			if (cursor.getString(cursor.getColumnIndexOrThrow(RosterConstants.GROUP)).length() == 0) {
				TextView groupname = (TextView)view.findViewById(R.id.groupname);
				groupname.setText(mConfig.enableGroups ? R.string.default_group : R.string.all_contacts_group);
			}
		}

		@Override
		protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
			super.bindChildView(view, context, cursor, isLastChild);
			
			TextView statusmsg = (TextView)view.findViewById(R.id.roster_statusmsg);
			boolean hasStatus = statusmsg.getText() != null && statusmsg.getText().length() > 0;
			statusmsg.setVisibility(hasStatus ? View.VISIBLE : View.GONE);

			String jid = cursor.getString(cursor.getColumnIndex(RosterConstants.JID));
			TextView unreadmsg = (TextView)view.findViewById(R.id.roster_unreadmsg_cnt);
			Integer count = mUnreadCounters.get(jid);
			if (count == null)
				count = 0;
			unreadmsg.setText(count.toString());
			unreadmsg.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
			unreadmsg.bringToFront();
		}

		 protected void setViewImage(ImageView v, String value) {
			 try {
				 int presenceMode = Integer.parseInt(value);
				 v.setImageResource(getIconForPresenceMode(presenceMode));
			 }
			 catch (Exception ex) {
				 v.setImageResource(getIconForPresenceMode(0));
			 }
		 }

		private int getIconForPresenceMode(int presenceMode) {
			MainWindow mainWindow = (MainWindow)getActivity();
			if (!mainWindow.isConnected()) // override icon if we are offline
				presenceMode = 0;
			return StatusMode.values()[presenceMode].getDrawableId();
		}
	}

	private class RosterObserver extends ContentObserver {
		public RosterObserver() {
			super(mainWindow.getHandler());
		}
		public void onChange(boolean selfChange) {
			Log.d(TAG, "RosterObserver.onChange: " + selfChange);
			// work around race condition in ExpandableListView, which collapses
			// groups rand-f**king-omly
			if (rosterListAdapter != null)
				mainWindow.getHandler().postDelayed(new Runnable() {
					public void run() {
						restoreGroupsExpanded();
					}}, 100);
		}
	}

	private static final String OFFLINE_EXCLUSION =
			RosterConstants.STATUS_MODE + " != " + StatusMode.offline.ordinal();
	private static final String countAvailableMembers =
			"SELECT COUNT() FROM " + RosterProvider.TABLE_ROSTER + " inner_query" +
					" WHERE inner_query." + RosterConstants.GROUP + " = " +
					RosterProvider.QUERY_ALIAS + "." + RosterConstants.GROUP +
					" AND inner_query." + OFFLINE_EXCLUSION;
	private static final String countMembers =
			"SELECT COUNT() FROM " + RosterProvider.TABLE_ROSTER + " inner_query" +
					" WHERE inner_query." + RosterConstants.GROUP + " = " +
					RosterProvider.QUERY_ALIAS + "." + RosterConstants.GROUP;
	private static final String[] GROUPS_QUERY = new String[] {
		RosterConstants._ID,
		RosterConstants.GROUP,
	};
	private static final String[] GROUPS_QUERY_COUNTED = new String[] {
		RosterConstants._ID,
		RosterConstants.GROUP,
		"(" + countAvailableMembers + ") || '/' || (" + countMembers + ") AS members"
	};

	final String countAvailableMembersTotals =
			"SELECT COUNT() FROM " + RosterProvider.TABLE_ROSTER + " inner_query" +
					" WHERE inner_query." + OFFLINE_EXCLUSION;
	final String countMembersTotals =
			"SELECT COUNT() FROM " + RosterProvider.TABLE_ROSTER;
	final String[] GROUPS_QUERY_CONTACTS_DISABLED = new String[] {
			RosterConstants._ID,
			"'' AS " + RosterConstants.GROUP,
			"(" + countAvailableMembersTotals + ") || '/' || (" + countMembersTotals + ") AS members"
	};

	private static final String[] GROUPS_FROM = new String[] {
		RosterConstants.GROUP,
		"members"
	};
	private static final int[] GROUPS_TO = new int[] {
		R.id.groupname,
		R.id.members
	};
	private static final String[] ROSTER_QUERY = new String[] {
		RosterConstants._ID,
		RosterConstants.JID,
		RosterConstants.ALIAS,
		RosterConstants.STATUS_MODE,
		RosterConstants.STATUS_MESSAGE,
	};

	public List<String> getRosterGroups() {
		// we want all, online and offline
		List<String> list = new ArrayList<String>();
		Cursor cursor = mContentResolver.query(RosterProvider.GROUPS_URI, GROUPS_QUERY,
					null, null, RosterConstants.GROUP);
		int idx = cursor.getColumnIndex(RosterConstants.GROUP);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			list.add(cursor.getString(idx));
			cursor.moveToNext();
		}
		cursor.close();
		return list;
	}

	public List<String[]> getRosterContacts() {
		// we want all, online and offline
		List<String[]> list = new ArrayList<String[]>();
		Cursor cursor = mContentResolver.query(RosterProvider.CONTENT_URI, ROSTER_QUERY,
					null, null, RosterConstants.ALIAS);
		int JIDIdx = cursor.getColumnIndex(RosterConstants.JID);
		int aliasIdx = cursor.getColumnIndex(RosterConstants.ALIAS);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			String jid = cursor.getString(JIDIdx);
			String alias = cursor.getString(aliasIdx);
			if ((alias == null) || (alias.length() == 0)) alias = jid;
			list.add(new String[] { jid, alias });
			cursor.moveToNext();
		}
		cursor.close();
		return list;
	}

	boolean isChild(long packedPosition) {
		int type = ExpandableListView.getPackedPositionType(packedPosition);
		return (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD);
	}
	
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {

		MainWindow mainWindow = (MainWindow)getActivity();
		long packedPosition = ExpandableListView.getPackedPositionForChild(groupPosition, childPosition);
		Cursor c = (Cursor)listView.getItemAtPosition(listView.getFlatListPosition(packedPosition));
		String userJid = c.getString(c.getColumnIndexOrThrow(RosterConstants.JID));
		String userName = c.getString(c.getColumnIndexOrThrow(RosterConstants.ALIAS));
		Intent i = mainWindow.getIntent();
		if (i.getAction() != null && i.getAction().equals(Intent.ACTION_SEND)) {
			// delegate ACTION_SEND to child window and close self
			mainWindow.startChatActivity(userJid, userName, i.getStringExtra(Intent.EXTRA_TEXT));
			mainWindow.finish();
		} 
		else {
			StatusMode s = StatusMode.values()[c.getInt(c.getColumnIndexOrThrow(RosterConstants.STATUS_MODE))];
			if (s == StatusMode.subscribe)
				mainWindow.rosterAddRequestedDialog(userJid,
					c.getString(c.getColumnIndexOrThrow(RosterConstants.STATUS_MESSAGE)));
			else
				mainWindow.startChatActivity(userJid, userName, null);
		}

		return true;
	}
	
	/** Context Menü **/
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		ExpandableListView.ExpandableListContextMenuInfo info;

		try {
			info = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
		} catch (ClassCastException e) {
			Log.e(TAG, "bad menuinfo: ", e);
			return;
		}

		long packedPosition = info.packedPosition;
		boolean isChild = isChild(packedPosition);

		// get the entry name for the item
		String menuName;
		if (isChild) {
			getActivity().getMenuInflater().inflate(R.menu.roster_item_contextmenu, menu);
			menuName = String.format("%s (%s)",
				getPackedItemRow(packedPosition, RosterConstants.ALIAS),
				getPackedItemRow(packedPosition, RosterConstants.JID));
		} 
		else {
			menuName = getPackedItemRow(packedPosition, RosterConstants.GROUP);
			if (menuName.equals(""))
				return; // no options for default menu
			getActivity().getMenuInflater().inflate(R.menu.roster_group_contextmenu, menu);
		}

		menu.setHeaderTitle(getString(R.string.roster_contextmenu_title, menuName));
	}
}