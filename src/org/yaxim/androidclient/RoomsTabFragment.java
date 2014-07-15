package org.yaxim.androidclient;

import org.yaxim.androidclient.data.RosterProvider;
import org.yaxim.androidclient.data.RosterProvider.RoomsConstants;
import org.yaxim.androidclient.util.StatusMode;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.actionbarsherlock.app.SherlockFragment;

public class RoomsTabFragment extends SherlockFragment {
	private static final String TAG = "yaxim.RoomsFragment";

	private RoomsListAdapter roomsListAdapter;
	private RoomsObserver mRoomsObserver;
	private MainWindow mainWindow;
	private ListView listView;
	private ContentResolver mContentResolver;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mainWindow = (MainWindow)getActivity();
		mRoomsObserver = new RoomsObserver();
		mainWindow.getContentResolver().registerContentObserver(RosterProvider.ROOMS_URI,
				true, mRoomsObserver);
	}
	
	private Cursor getCursor() {
		return mContentResolver.query(RosterProvider.ROOMS_URI, new String[] {
				RoomsConstants._ID,
				RoomsConstants.ID,
				RoomsConstants.NAME,
				RoomsConstants.STATUS,
				RoomsConstants.TOPIC,
			}, "", null, RoomsConstants.CREATED + " desc");
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mContentResolver = getActivity().getContentResolver();
		View view = inflater.inflate(R.layout.roomstab, container, false);
		listView = (ListView)view.findViewById(R.id.roomsList);
		
		roomsListAdapter = new RoomsListAdapter(getActivity(), getCursor());
		listView.setAdapter(roomsListAdapter);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				MainWindow mainWindow = (MainWindow)getActivity();
				Cursor c = (Cursor)listView.getItemAtPosition(position);
				String roomID = c.getString(c.getColumnIndexOrThrow(RoomsConstants.ID));
				String roomName = c.getString(c.getColumnIndexOrThrow(RoomsConstants.NAME));
				Intent i = mainWindow.getIntent();
				if (i != null) {
					mainWindow.startChatActivity(roomID, roomName, null);
				}
			}
		});
		registerForContextMenu(listView);
		return view;
	}
	
	/** Context Menü **/
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		getActivity().getMenuInflater().inflate(R.menu.room_contextmenu, menu);
		menu.setHeaderTitle(getString(R.string.rooms_rooms));
	}
	
	String getRoomId(int position) {
		Cursor c = (Cursor) listView.getItemAtPosition(position);
		return c.getString(c.getColumnIndex(RoomsConstants.ID));
	}
	
	@SuppressWarnings("deprecation")
	class RoomsListAdapter extends SimpleCursorAdapter {

		public RoomsListAdapter(Context context, Cursor c) {
			super(context, R.layout.mainchild_row, c, 
					new String[] {
						RoomsConstants.NAME,
						RoomsConstants.TOPIC
					},
					new int[] {
						R.id.roster_screenname,
						R.id.roster_statusmsg
					});
		}
		
		public void requery() {
			Cursor cursor = RoomsTabFragment.this.getCursor();
			Cursor oldCursor = getCursor();
			changeCursor(cursor);
			mainWindow.stopManagingCursor(oldCursor);
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			super.bindView(view, context, cursor);
			ImageView imageView = (ImageView)view.findViewById(R.id.roster_icon);
			int status = cursor.getInt(cursor.getColumnIndex(RoomsConstants.STATUS));
			imageView.setImageResource(status == StatusMode.chat.ordinal() ? R.drawable.ic_status_chat : R.drawable.ic_status_dnd);
		}
	}
	
	private class RoomsObserver extends ContentObserver {
		public RoomsObserver() {
			super(mainWindow.getHandler());
		}
		
		public void onChange(boolean selfChange) {
			Log.d(TAG, "RoomsObserver.onChange: " + selfChange);
			roomsListAdapter.requery();
		}
	}
}
