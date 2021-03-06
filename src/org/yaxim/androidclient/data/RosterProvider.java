package org.yaxim.androidclient.data;

import java.util.ArrayList;

import org.yaxim.androidclient.util.LogConstants;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Handler;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

public class RosterProvider extends ContentProvider {

	public static final String AUTHORITY = "org.yaxim.androidclient.provider.Roster";
	public static final String TABLE_ROSTER = "roster";
	public static final String TABLE_GROUPS = "groups";
	public static final String TABLE_ROOMS = "rooms";
	public static final String TABLE_PARTICIPANTS = "participants";
	public static final String TABLE_KEYS = "keys";
    public static final String TABLE_COMPANIES = "companies";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + TABLE_ROSTER);
	public static final Uri GROUPS_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + TABLE_GROUPS);
	public static final Uri ROOMS_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + TABLE_ROOMS);
	public static final Uri PARTICIPANTS_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + TABLE_PARTICIPANTS);
	public static final Uri KEYS_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + TABLE_KEYS);
    public static final Uri COMPANIES_URI = Uri.parse( "content://" + AUTHORITY
            + "/" + TABLE_COMPANIES );
	public static final String QUERY_ALIAS = "main_result";

	private static final UriMatcher URI_MATCHER = new UriMatcher(
			UriMatcher.NO_MATCH);
	private static final int CONTACTS = 1;
	private static final int CONTACT_ID = 2;
	private static final int GROUPS = 3;
	private static final int GROUP_MEMBERS = 4;
	private static final int ROOMS = 5;
	private static final int ROOM_ID = 6;
	private static final int PARTICIPANTS = 7;
	private static final int PARTICIPANT_ID = 8;
	private static final int KEYS = 9;
	private static final int KEYS_ID = 10;

	static {
		URI_MATCHER.addURI(AUTHORITY, "roster", CONTACTS);
		URI_MATCHER.addURI(AUTHORITY, "roster/#", CONTACT_ID);
		URI_MATCHER.addURI(AUTHORITY, "groups", GROUPS);
		URI_MATCHER.addURI(AUTHORITY, "groups/*", GROUP_MEMBERS);
		URI_MATCHER.addURI(AUTHORITY, "rooms", ROOMS);
		URI_MATCHER.addURI(AUTHORITY, "rooms/#", ROOM_ID);
		URI_MATCHER.addURI(AUTHORITY, "participants", PARTICIPANTS);
		URI_MATCHER.addURI(AUTHORITY, "participants/#", PARTICIPANT_ID);
		URI_MATCHER.addURI(AUTHORITY, "keys", KEYS);
		URI_MATCHER.addURI(AUTHORITY, "keys/#", KEYS_ID);
	}

	private static final String TAG = "yaxim.RosterProvider";

	private final Runnable mNotifyChange = new Runnable() {
		@Override
        public void run() {
			Log.d(TAG, "notifying change");
			getContext().getContentResolver().notifyChange(CONTENT_URI, null);
			getContext().getContentResolver().notifyChange(GROUPS_URI, null);
			getContext().getContentResolver().notifyChange(ROOMS_URI, null);
		}
	};
	private final Handler mNotifyHandler = new Handler();

	private SQLiteOpenHelper mOpenHelper;


	public RosterProvider() {
	}

	@Override
	public int delete(final Uri url, String where, final String[] whereArgs) {
		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		String segment;
		switch (URI_MATCHER.match(url)) {

		case CONTACTS:
			count = db.delete(TABLE_ROSTER, where, whereArgs);
			break;

		case CONTACT_ID:
			segment = url.getPathSegments().get(1);
			if (TextUtils.isEmpty(where)) {
				where = "_id=" + segment;
			} else {
				where = "_id=" + segment + " AND (" + where + ")";
			}

			count = db.delete(TABLE_ROSTER, where, whereArgs);
			break;

		case ROOMS:
			count = db.delete(TABLE_ROOMS, where, whereArgs);
			break;

		case ROOM_ID:
			segment = url.getPathSegments().get(1);
			if (TextUtils.isEmpty(where)) {
				where = "_id=" + segment;
			} else {
				where = "_id=" + segment + " AND (" + where + ")";
			}
			count = db.delete(TABLE_ROOMS, where, whereArgs);
			break;

		case PARTICIPANTS:
			count = db.delete(TABLE_PARTICIPANTS, where, whereArgs);
			break;

		case PARTICIPANT_ID:
			segment = url.getPathSegments().get(1);
			if (TextUtils.isEmpty(where)) {
				where = "_id=" + segment;
			} else {
				where = "_id=" + segment + " AND (" + where + ")";
			}
			count = db.delete(TABLE_PARTICIPANTS, where, whereArgs);
			break;

		case KEYS:
			count = db.delete(TABLE_KEYS, where, whereArgs);
			break;

		default:
			throw new IllegalArgumentException("Cannot delete from URL: " + url);
		}

		getContext().getContentResolver().notifyChange(GROUPS_URI, null);
		notifyChange();

		return count;
	}

	@Override
	public String getType(final Uri url) {
		final int match = URI_MATCHER.match(url);
		switch (match) {
		case CONTACTS:
			return RosterConstants.CONTENT_TYPE;
		case CONTACT_ID:
			return RosterConstants.CONTENT_ITEM_TYPE;
		case ROOMS:
			return RoomsConstants.CONTENT_TYPE;
		case ROOM_ID:
			return RoomsConstants.CONTENT_ITEM_TYPE;
		case PARTICIPANTS:
			return ParticipantConstants.CONTENT_TYPE;
		case PARTICIPANT_ID:
			return ParticipantConstants.CONTENT_ITEM_TYPE;
		case KEYS:
			return ParticipantConstants.CONTENT_TYPE;
		case KEYS_ID:
			return ParticipantConstants.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URL");
		}
	}

	@Override
	public Uri insert(final Uri url, final ContentValues initialValues) {
		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		final ContentValues values = (initialValues != null) ? new ContentValues(
				initialValues) : new ContentValues();

		if (URI_MATCHER.match(url) == CONTACTS) {
			for (final String colName : RosterConstants.getRequiredColumns()) {
				if (values.containsKey(colName) == false) {
					throw new IllegalArgumentException("Missing column: " + colName);
				}
			}
			final long rowId = db.insert(TABLE_ROSTER, RosterConstants.JID, values);
			if (rowId < 0) {
				throw new SQLException("Failed to insert row into " + url);
			}
			final Uri noteUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
			notifyChange();
			return noteUri;
		}
		else if (URI_MATCHER.match(url) == ROOMS) {
			for (final String colName : RoomsConstants.getRequiredColumns()) {
				if (values.containsKey(colName) == false) {
					throw new IllegalArgumentException("Missing column: " + colName);
				}
			}
			final long rowId = db.insert(TABLE_ROOMS, RoomsConstants.ID, values);
			if (rowId < 0) {
				throw new SQLException("Failed to insert row into " + url);
			}
			final Uri noteUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
			notifyChange();
			return noteUri;
		} 
		else if (URI_MATCHER.match(url) == PARTICIPANTS) {
			for (final String colName : ParticipantConstants.getRequiredColumns()) {
				if (values.containsKey(colName) == false) {
					throw new IllegalArgumentException("Missing column: " + colName);
				}
			}
			final long rowId = db.insert(TABLE_PARTICIPANTS, ParticipantConstants.JID, values);
			if (rowId < 0) {
				throw new SQLException("Failed to insert row into " + url);
			}
			final Uri noteUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
			notifyChange();
			return noteUri;
		} 
		else if (URI_MATCHER.match(url) == KEYS) {
			final long rowId = db.insert(TABLE_KEYS, KeysConstants.JID, values);
			if (rowId < 0) {
				throw new SQLException("Failed to insert row into " + url);
			}
			final Uri noteUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
			notifyChange();
			return noteUri;
		} 
		else {
			throw new IllegalArgumentException("Cannot insert into this uri: " + url);
		}
	}

	@Override
	public boolean onCreate() {
		mOpenHelper = new RosterDatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(final Uri url, final String[] projectionIn, final String selection,
			final String[] selectionArgs, final String sortOrder) {

		final SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
		final int match = URI_MATCHER.match(url);
		String groupBy = null;

		switch (match) {

		case GROUPS:
			qBuilder.setTables(TABLE_ROSTER + " " + QUERY_ALIAS);
			groupBy = RosterConstants.GROUP;
			break;

		case GROUP_MEMBERS:
			qBuilder.setTables(TABLE_ROSTER + " " + QUERY_ALIAS);
			qBuilder.appendWhere(RosterConstants.GROUP + "=");
			qBuilder.appendWhere(url.getPathSegments().get(1));
			break;

		case CONTACTS:
			qBuilder.setTables(TABLE_ROSTER + " " + QUERY_ALIAS);
			break;

		case CONTACT_ID:
			qBuilder.setTables(TABLE_ROSTER + " " + QUERY_ALIAS);
			qBuilder.appendWhere("_id=");
			qBuilder.appendWhere(url.getPathSegments().get(1));
			break;

		case ROOMS:
			qBuilder.setTables(TABLE_ROOMS + " " + QUERY_ALIAS);
			break;

		case ROOM_ID:
			qBuilder.setTables(TABLE_ROOMS + " " + QUERY_ALIAS);
			qBuilder.appendWhere("_id=");
			qBuilder.appendWhere(url.getPathSegments().get(1));
			break;

		case PARTICIPANTS:
			qBuilder.setTables(TABLE_PARTICIPANTS + " " + QUERY_ALIAS);
			break;

		case PARTICIPANT_ID:
			qBuilder.setTables(TABLE_PARTICIPANTS + " " + QUERY_ALIAS);
			qBuilder.appendWhere("_id=");
			qBuilder.appendWhere(url.getPathSegments().get(1));
			break;

		case KEYS:
			qBuilder.setTables(TABLE_KEYS + " " + QUERY_ALIAS);
			break;

		case KEYS_ID:
			qBuilder.setTables(TABLE_KEYS + " " + QUERY_ALIAS);
			qBuilder.appendWhere("_id=");
			qBuilder.appendWhere(url.getPathSegments().get(1));
			break;

		default:
			throw new IllegalArgumentException("Unknown URL " + url);
		}

		String orderBy;
		if (TextUtils.isEmpty(sortOrder) && match == CONTACTS) {
			orderBy = RosterConstants.DEFAULT_SORT_ORDER;
		} else {
			orderBy = sortOrder;
		}

		final SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		final Cursor ret = qBuilder.query(db, projectionIn, selection, selectionArgs,
				groupBy, null, orderBy);

		if (ret == null) {
			infoLog("RosterProvider.query: failed");
		} else {
			ret.setNotificationUri(getContext().getContentResolver(), url);
		}

		return ret;
	}

	@Override
	public int update(final Uri url, final ContentValues values, final String where,
			final String[] whereArgs) {
		int count;
		long rowId = 0;
		final int match = URI_MATCHER.match(url);
		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		String segment;

		switch (match) {
		case CONTACTS:
			count = db.update(TABLE_ROSTER, values, where, whereArgs);
			break;
		case CONTACT_ID:
			segment = url.getPathSegments().get(1);
			rowId = Long.parseLong(segment);
			count = db.update(TABLE_ROSTER, values, "_id=" + rowId, whereArgs);
			break;
		case ROOMS:
			count = db.update(TABLE_ROOMS, values, where, whereArgs);
			break;
		case ROOM_ID:
			segment = url.getPathSegments().get(1);
			rowId = Long.parseLong(segment);
			count = db.update(TABLE_ROSTER, values, "_id=" + rowId, whereArgs);
			break;
		case PARTICIPANTS:
			count = db.update(TABLE_PARTICIPANTS, values, where, whereArgs);
			break;
		case PARTICIPANT_ID:
			segment = url.getPathSegments().get(1);
			rowId = Long.parseLong(segment);
			count = db.update(TABLE_PARTICIPANTS, values, "_id=" + rowId, whereArgs);
			break;
		case KEYS:
			count = db.update(TABLE_KEYS, values, where, whereArgs);
			break;
		case KEYS_ID:
			segment = url.getPathSegments().get(1);
			rowId = Long.parseLong(segment);
			count = db.update(TABLE_KEYS, values, "_id=" + rowId, whereArgs);
			break;
		default:
			throw new UnsupportedOperationException("Cannot update URL: " + url);
		}

		notifyChange();

		return count;

	}

	/* delay change notification, cancel previous attempts.
	 * this implements rate throttling on fast update sequences
	 */
	long last_notify = 0;
	private void notifyChange() {
		mNotifyHandler.removeCallbacks(mNotifyChange);
		final long ts = System.currentTimeMillis();
		if (ts > last_notify + 500) {
            mNotifyChange.run();
        } else {
            mNotifyHandler.postDelayed(mNotifyChange, 200);
        }
		last_notify = ts;
	}

	private static void infoLog(final String data) {
		if (LogConstants.LOG_INFO) {
			Log.i(TAG, data);
		}
	}

	private static class RosterDatabaseHelper extends SQLiteOpenHelper {

		private static final String DATABASE_NAME = "roster.db";
		private static final int DATABASE_VERSION = 2;

		public RosterDatabaseHelper(final Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(final SQLiteDatabase db) {
			infoLog("creating new roster table");

			db.execSQL("CREATE TABLE " + TABLE_ROSTER + " ("
					+ RosterConstants._ID
					+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ RosterConstants.JID + " TEXT UNIQUE ON CONFLICT REPLACE, "
					+ RosterConstants.ALIAS
					+ " TEXT, " + RosterConstants.STATUS_MODE + " INTEGER, "
					+ RosterConstants.STATUS_MESSAGE + " TEXT, "
					+ RosterConstants.GROUP + " TEXT);");
			db.execSQL("CREATE INDEX idx_roster_group ON " + TABLE_ROSTER
				        + " (" + RosterConstants.GROUP + ")");
			db.execSQL("CREATE INDEX idx_roster_alias ON " + TABLE_ROSTER
				        + " (" + RosterConstants.ALIAS + ")");
			db.execSQL("CREATE INDEX idx_roster_status ON " + TABLE_ROSTER
				        + " (" + RosterConstants.STATUS_MODE + ")");
			createRoomsTable(db);
			createParticipantsTable(db);
			createKeysTable(db);
		}
		
		private void createRoomsTable(final SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TABLE_ROOMS + " ("
					+ RosterConstants._ID
					+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ RoomsConstants.ID + " TEXT UNIQUE ON CONFLICT REPLACE, "
					+ RoomsConstants.NAME+ " TEXT, " 
					+ RoomsConstants.CREATED + " LONG, "
					+ RoomsConstants.OWNER + " TEXT, "
					+ RoomsConstants.TOPIC + " TEXT, "
					+ RoomsConstants.STATUS + " INTEGER, "
					+ RoomsConstants.LOGGER + " TEXT);");
			db.execSQL("CREATE INDEX idx_rooms_id ON " + TABLE_ROOMS
				        + " (" + RoomsConstants.ID + ")");
		}

		private void createParticipantsTable(final SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TABLE_PARTICIPANTS + " ("
					+ ParticipantConstants._ID
					+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ ParticipantConstants.JID + " TEXT, "
					+ ParticipantConstants.ROOM + " TEXT, " 
					+ ParticipantConstants.NAME + " TEXT);");
			db.execSQL("CREATE INDEX idx_room_id ON " + TABLE_PARTICIPANTS
				        + " (" + ParticipantConstants.ROOM + ")");
		}

		private void createKeysTable(final SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TABLE_KEYS + " ("
					+ KeysConstants._ID
					+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ KeysConstants.JID + " TEXT, "
					+ KeysConstants.RESOURCE + " TEXT, "
					+ KeysConstants.PUBLIC_KEY + " TEXT, " 
					+ KeysConstants.PRIVATE_KEY + " TEXT);");
			db.execSQL("CREATE INDEX idx_jid_id ON " + TABLE_PARTICIPANTS
				        + " (" + KeysConstants.JID + ")");
		}

		@Override
		public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
			infoLog("onUpgrade: from " + oldVersion + " to " + newVersion);
			switch (oldVersion) {
			case 1:
				db.execSQL("ALTER TABLE " + TABLE_KEYS + " ADD " + KeysConstants.RESOURCE + " TEXT");
				break;
			default:
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_GROUPS);
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROSTER);
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROOMS);
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_PARTICIPANTS);
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_KEYS);
				onCreate(db);
			}
		}
	}

	public static final class RosterConstants implements BaseColumns {

		private RosterConstants() {
		}

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.yaxim.roster";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.yaxim.roster";

		public static final String JID = "jid";
		public static final String ALIAS = "alias";
		public static final String STATUS_MODE = "status_mode";
		public static final String STATUS_MESSAGE = "status_message";
		public static final String GROUP = "roster_group";

		public static final String DEFAULT_SORT_ORDER = STATUS_MODE + " DESC, " + ALIAS + " COLLATE NOCASE";

		public static ArrayList<String> getRequiredColumns() {
			final ArrayList<String> tmpList = new ArrayList<String>();
			tmpList.add(JID);
			tmpList.add(ALIAS);
			tmpList.add(STATUS_MODE);
			tmpList.add(STATUS_MESSAGE);
			tmpList.add(GROUP);
			return tmpList;
		}

	}

	public static final class RoomsConstants implements BaseColumns {

		private RoomsConstants() {
		}

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.yaxim.rooms";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.yaxim.rooms";

		public static final String ID = "node_id";
		public static final String NAME = "name";
		public static final String CREATED = "created";
		public static final String OWNER = "owner";
		public static final String TOPIC = "topic";
		public static final String LOGGER = "logger";
		public static final String STATUS = "status";

		public static final String DEFAULT_SORT_ORDER = CREATED + " DESC, " + NAME + " COLLATE NOCASE";

		public static ArrayList<String> getRequiredColumns() {
			final ArrayList<String> tmpList = new ArrayList<String>();
			tmpList.add(ID);
			tmpList.add(NAME);
			tmpList.add(CREATED);
			tmpList.add(OWNER);
			tmpList.add(TOPIC);
			tmpList.add(LOGGER);
			tmpList.add(STATUS);
			return tmpList;
		}

	}

	public static final class ParticipantConstants implements BaseColumns {

		private ParticipantConstants() {
		}

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.yaxim.participants";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.yaxim.participants";

		public static final String JID = "jid";
		public static final String ROOM = "room_id";
		public static final String NAME = "name";

		public static ArrayList<String> getRequiredColumns() {
			final ArrayList<String> tmpList = new ArrayList<String>();
			tmpList.add(JID);
			tmpList.add(ROOM);
			tmpList.add(NAME);
			return tmpList;
		}
	}

	public static final class KeysConstants implements BaseColumns {

		private KeysConstants() {
		}

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.yaxim.participants";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.yaxim.participants";

		public static final String JID = "jid";
		public static final String PUBLIC_KEY = "public_key";
		public static final String PRIVATE_KEY = "private_key";
		public static final String RESOURCE = "resource";

		public static ArrayList<String> getRequiredColumns() {
			final ArrayList<String> tmpList = new ArrayList<String>();
			tmpList.add(JID);
			tmpList.add(PUBLIC_KEY);
			tmpList.add(PRIVATE_KEY);
			return tmpList;
		}
	}
}
