package org.yaxim.androidclient.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.DNSUtil;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.dns.DNSJavaResolver;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.carbons.Carbon;
import org.jivesoftware.smackx.carbons.CarbonManager;
import org.jivesoftware.smackx.entitycaps.EntityCapsManager;
import org.jivesoftware.smackx.entitycaps.cache.SimpleDirectoryPersistentCache;
import org.jivesoftware.smackx.entitycaps.provider.CapsExtensionProvider;
import org.jivesoftware.smackx.forward.Forwarded;
import org.jivesoftware.smackx.packet.DelayInfo;
import org.jivesoftware.smackx.packet.DelayInformation;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.Version;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.ping.packet.Ping;
import org.jivesoftware.smackx.ping.provider.PingProvider;
import org.jivesoftware.smackx.provider.DelayInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.provider.HeaderProvider;
import org.jivesoftware.smackx.provider.HeadersProvider;
import org.jivesoftware.smackx.pubsub.EventElement;
import org.jivesoftware.smackx.pubsub.EventElementType;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.ItemsExtension;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.SimplePayload;
import org.jivesoftware.smackx.pubsub.Subscription;
import org.jivesoftware.smackx.pubsub.provider.AffiliationProvider;
import org.jivesoftware.smackx.pubsub.provider.AffiliationsProvider;
import org.jivesoftware.smackx.pubsub.provider.ConfigEventProvider;
import org.jivesoftware.smackx.pubsub.provider.EventProvider;
import org.jivesoftware.smackx.pubsub.provider.FormNodeProvider;
import org.jivesoftware.smackx.pubsub.provider.ItemProvider;
import org.jivesoftware.smackx.pubsub.provider.ItemsProvider;
import org.jivesoftware.smackx.pubsub.provider.PubSubProvider;
import org.jivesoftware.smackx.pubsub.provider.RetractEventProvider;
import org.jivesoftware.smackx.pubsub.provider.SimpleNodeProvider;
import org.jivesoftware.smackx.pubsub.provider.SubscriptionProvider;
import org.jivesoftware.smackx.pubsub.provider.SubscriptionsProvider;
import org.jivesoftware.smackx.receipts.DeliveryReceipt;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;
import org.jivesoftware.smackx.receipts.ReceiptReceivedListener;
import org.yaxim.androidclient.YaximApplication;
import org.yaxim.androidclient.crypto.Crypto;
import org.yaxim.androidclient.crypto.KeyAccessor;
import org.yaxim.androidclient.data.ChatProvider;
import org.yaxim.androidclient.data.ChatProvider.ChatConstants;
import org.yaxim.androidclient.data.RosterProvider;
import org.yaxim.androidclient.data.RosterProvider.KeysConstants;
import org.yaxim.androidclient.data.RosterProvider.ParticipantConstants;
import org.yaxim.androidclient.data.RosterProvider.RoomsConstants;
import org.yaxim.androidclient.data.RosterProvider.RosterConstants;
import org.yaxim.androidclient.data.YaximConfiguration;
import org.yaxim.androidclient.exceptions.YaximXMPPException;
import org.yaxim.androidclient.util.ConnectionState;
import org.yaxim.androidclient.util.LogConstants;
import org.yaxim.androidclient.util.PreferenceConstants;
import org.yaxim.androidclient.util.StatusMode;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import de.f24.rooms.messages.CompanyAffiliationRequest;
import de.f24.rooms.messages.ContactList;
import de.f24.rooms.messages.FileMessage;
import de.f24.rooms.messages.Identity;
import de.f24.rooms.messages.Identity.IdentityType;
import de.f24.rooms.messages.Invitation;
import de.f24.rooms.messages.Participant;
import de.f24.rooms.messages.PersonalInfo;
import de.f24.rooms.messages.PushRequest;
import de.f24.rooms.messages.RegistrationConfirmation;
import de.f24.rooms.messages.RoomConfiguration;
import de.f24.rooms.messages.RoomsMessage;
import de.f24.rooms.messages.RoomsMessageFactory;
import de.f24.rooms.messages.RoomsMessageType;
import de.f24.rooms.messages.TaskMessage;
import de.f24.rooms.messages.TextMessage;

public class SmackableImp implements Smackable {
	final static private String TAG = "yaxim.SmackableImp";

	final static private int PACKET_TIMEOUT = 30000;

	final static private String[] SEND_OFFLINE_PROJECTION = new String[] {
			ChatConstants._ID, ChatConstants.JID,
			ChatConstants.MESSAGE, ChatConstants.DATE, ChatConstants.PACKET_ID };
	final static private String SEND_OFFLINE_SELECTION =
			ChatConstants.DIRECTION + " = " + ChatConstants.OUTGOING + " AND " +
			ChatConstants.DELIVERY_STATUS + " = " + ChatConstants.DS_NEW;

	static final DiscoverInfo.Identity YAXIM_IDENTITY = new DiscoverInfo.Identity("client",
					YaximApplication.XMPP_IDENTITY_NAME,
					YaximApplication.XMPP_IDENTITY_TYPE);

	static File capsCacheDir = null; ///< this is used to cache if we already initialized EntityCapsCache

	static {
		registerSmackProviders();
		DNSUtil.setDNSResolver(DNSJavaResolver.getInstance());

		// initialize smack defaults before any connections are created
		SmackConfiguration.setPacketReplyTimeout(PACKET_TIMEOUT);
		SmackConfiguration.setDefaultPingInterval(0);
	}

	static void registerSmackProviders() {
		final ProviderManager pm = ProviderManager.getInstance();
		// add IQ handling
		pm.addIQProvider("query","http://jabber.org/protocol/disco#info", new DiscoverInfoProvider());
		pm.addIQProvider("query","http://jabber.org/protocol/disco#items", new DiscoverItemsProvider());
		// add delayed delivery notifications
		pm.addExtensionProvider("delay","urn:xmpp:delay", new DelayInfoProvider());
		pm.addExtensionProvider("x","jabber:x:delay", new DelayInfoProvider());
		// add XEP-0092 Software Version
		pm.addIQProvider("query", Version.NAMESPACE, new Version.Provider());

		// add carbons and forwarding
		pm.addExtensionProvider("forwarded", Forwarded.NAMESPACE, new Forwarded.Provider());
		pm.addExtensionProvider("sent", Carbon.NAMESPACE, new Carbon.Provider());
		pm.addExtensionProvider("received", Carbon.NAMESPACE, new Carbon.Provider());
		// add delivery receipts
		pm.addExtensionProvider(DeliveryReceipt.ELEMENT, DeliveryReceipt.NAMESPACE, new DeliveryReceipt.Provider());
		pm.addExtensionProvider(DeliveryReceiptRequest.ELEMENT, DeliveryReceipt.NAMESPACE, new DeliveryReceiptRequest.Provider());
		// add XMPP Ping (XEP-0199)
		pm.addIQProvider("ping","urn:xmpp:ping", new PingProvider());

		ServiceDiscoveryManager.setDefaultIdentity(YAXIM_IDENTITY);
		
		// XEP-0115 Entity Capabilities
		pm.addExtensionProvider("c", "http://jabber.org/protocol/caps", new CapsExtensionProvider());
		
		 // SHIM
		pm.addExtensionProvider("headers", "http://jabber.org/protocol/shim", new HeadersProvider());
		pm.addExtensionProvider("header", "http://jabber.org/protocol/shim", new HeaderProvider());

		// PubSub
		pm.addIQProvider("pubsub", "http://jabber.org/protocol/pubsub", new PubSubProvider());
        pm.addExtensionProvider("create", "http://jabber.org/protocol/pubsub", new SimpleNodeProvider());
        pm.addExtensionProvider("items", "http://jabber.org/protocol/pubsub", new ItemsProvider());
        pm.addExtensionProvider("item", "http://jabber.org/protocol/pubsub", new ItemProvider());
        pm.addExtensionProvider("subscriptions", "http://jabber.org/protocol/pubsub", new SubscriptionsProvider());
        pm.addExtensionProvider("subscription", "http://jabber.org/protocol/pubsub", new SubscriptionProvider());
        pm.addExtensionProvider("affiliations", "http://jabber.org/protocol/pubsub", new AffiliationsProvider());
        pm.addExtensionProvider("affiliation", "http://jabber.org/protocol/pubsub", new AffiliationProvider());
        pm.addExtensionProvider("options", "http://jabber.org/protocol/pubsub", new FormNodeProvider());
        // PubSub owner
        pm.addIQProvider("pubsub", "http://jabber.org/protocol/pubsub#owner", new PubSubProvider());
        pm.addExtensionProvider("configure", "http://jabber.org/protocol/pubsub#owner", new FormNodeProvider());
        pm.addExtensionProvider("default", "http://jabber.org/protocol/pubsub#owner", new FormNodeProvider());
        // PubSub event
        pm.addExtensionProvider("event", "http://jabber.org/protocol/pubsub#event", new EventProvider());
        pm.addExtensionProvider("configuration", "http://jabber.org/protocol/pubsub#event", new ConfigEventProvider());
        pm.addExtensionProvider("delete", "http://jabber.org/protocol/pubsub#event", new SimpleNodeProvider());
        pm.addExtensionProvider("options", "http://jabber.org/protocol/pubsub#event", new FormNodeProvider());
        pm.addExtensionProvider("items", "http://jabber.org/protocol/pubsub#event", new ItemsProvider());
        pm.addExtensionProvider("item", "http://jabber.org/protocol/pubsub#event", new ItemProvider());
        pm.addExtensionProvider("retract", "http://jabber.org/protocol/pubsub#event", new RetractEventProvider());
        pm.addExtensionProvider("purge", "http://jabber.org/protocol/pubsub#event", new SimpleNodeProvider());
		
		XmppStreamHandler.addExtensionProviders();
	}

	private final YaximConfiguration mConfig;
	private ConnectionConfiguration mXMPPConfig;
	private XmppStreamHandler.ExtXMPPConnection mXMPPConnection;
	private XmppStreamHandler mStreamHandler;
	private Thread mConnectingThread;
	private final Object mConnectingThreadMutex = new Object();
	private PubSubManager pubSub;

	private ConnectionState mRequestedState = ConnectionState.OFFLINE;
	private ConnectionState mState = ConnectionState.OFFLINE;
	private String mLastError;
	
	private XMPPServiceCallback mServiceCallBack;
	private Roster mRoster;
	private RosterListener mRosterListener;
	private PacketListener mPacketListener;
	private PacketListener mPresenceListener;
	private ConnectionListener mConnectionListener;

	private final ContentResolver mContentResolver;

	private final AlarmManager mAlarmManager;
	private PacketListener mPongListener;
	private String mPingID;
	private long mPingTimestamp;

	private PendingIntent mPingAlarmPendIntent;
	private PendingIntent mPongTimeoutAlarmPendIntent;
	private static final String PING_ALARM = "org.yaxim.androidclient.PING_ALARM";
	private static final String PONG_TIMEOUT_ALARM = "org.yaxim.androidclient.PONG_TIMEOUT_ALARM";
	private final Intent mPingAlarmIntent = new Intent(PING_ALARM);
	private final Intent mPongTimeoutAlarmIntent = new Intent(PONG_TIMEOUT_ALARM);
	private final Service mService;

	private final PongTimeoutAlarmReceiver mPongTimeoutAlarmReceiver = new PongTimeoutAlarmReceiver();
	private final BroadcastReceiver mPingAlarmReceiver = new PingAlarmReceiver();
	private final Crypto crypto;

	public SmackableImp(final YaximConfiguration config,
			final ContentResolver contentResolver,
			final Service service) {
		this.mConfig = config;
		this.mContentResolver = contentResolver;
		this.mService = service;
		this.mAlarmManager = (AlarmManager)mService.getSystemService(Context.ALARM_SERVICE);
		this.crypto = YaximApplication.getApp(mService).mCrypto;
	}
		
	// this code runs a DNS resolver, might be blocking
	private synchronized void initXMPPConnection() {
		// allow custom server / custom port to override SRV record
		if (mConfig.customServer.length() > 0) {
            mXMPPConfig = new ConnectionConfiguration(mConfig.customServer,
					mConfig.port, mConfig.server);
        }
        else {
            mXMPPConfig = new ConnectionConfiguration(mConfig.server); // use SRV
        }
		mXMPPConfig.setReconnectionAllowed(false);
		mXMPPConfig.setSendPresence(false);
		mXMPPConfig.setCompressionEnabled(false); // disable for now
		mXMPPConfig.setDebuggerEnabled(mConfig.smackdebug);
		if (mConfig.require_ssl) {
            this.mXMPPConfig.setSecurityMode(ConnectionConfiguration.SecurityMode.required);
        }

		// register MemorizingTrustManager for HTTPS
		try {
			final SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, new X509TrustManager[] { YaximApplication.getApp(mService).mMTM },
					new java.security.SecureRandom());
			this.mXMPPConfig.setCustomSSLContext(sc);
		} catch (final java.security.GeneralSecurityException e) {
			debugLog("initialize MemorizingTrustManager: " + e);
		}

		this.mXMPPConnection = new XmppStreamHandler.ExtXMPPConnection(mXMPPConfig);
		this.mStreamHandler = new XmppStreamHandler(mXMPPConnection, mConfig.smackdebug);
		mStreamHandler.addAckReceivedListener(new XmppStreamHandler.AckReceivedListener() {
			@Override
            public void ackReceived(final long handled, final long total) {
				gotServerPong("" + handled);
			}
		});
		mConfig.reconnect_required = false;

		initServiceDiscovery();
		pubSub = new PubSubManager(mXMPPConnection);
	}

	// blocking, run from a thread!
	@Override
    public boolean doConnect(final boolean create_account) throws YaximXMPPException {
		mRequestedState = ConnectionState.ONLINE;
		updateConnectionState(ConnectionState.CONNECTING);
		if (mXMPPConnection == null || mConfig.reconnect_required) {
            initXMPPConnection();
        }
		tryToConnect(create_account);
		// actually, authenticated must be true now, or an exception must have
		// been thrown.
		if (isAuthenticated()) {
			registerMessageListener();
			registerPresenceListener();
			registerPongListener();
			sendOfflineMessages();
			// we need to "ping" the service to let it know we are actually
			// connected, even when no roster entries will come in
			updateConnectionState(ConnectionState.ONLINE);
		} else {
            throw new YaximXMPPException("SMACK connected, but authentication failed");
        }
		return true;
	}

	// BLOCKING, call on a new Thread!
	private void updateConnectingThread(final Thread new_thread) {
		synchronized(mConnectingThreadMutex) {
			if (mConnectingThread == null) {
				mConnectingThread = new_thread;
			} else {
                try {
                	Log.d(TAG, "updateConnectingThread: old thread is still running, killing it.");
                	mConnectingThread.interrupt();
                	mConnectingThread.join(50);
                } catch (final InterruptedException e) {
                	Log.d(TAG, "updateConnectingThread: failed to join(): " + e);
                } finally {
                	mConnectingThread = new_thread;
                }
            }
		}
	}
	private void finishConnectingThread() {
		synchronized(mConnectingThreadMutex) {
			mConnectingThread = null;
		}
	}

	/** Non-blocking, synchronized function to connect/disconnect XMPP.
	 * This code is called from outside and returns immediately. The actual work
	 * is done on a background thread, and notified via callback.
	 * @param new_state The state to transition into. Possible values:
	 * 	OFFLINE to properly close the connection
	 * 	ONLINE to connect
	 * 	DISCONNECTED when network goes down
	 * @param create_account When going online, try to register an account.
	 */
	@Override
	public synchronized void requestConnectionState(final ConnectionState new_state, final boolean create_account) {
		Log.d(TAG, "requestConnState: " + mState + " -> " + new_state + (create_account ? " create_account!" : ""));
		mRequestedState = new_state;
		if (new_state == mState) {
            return;
        }
		switch (new_state) {
		case ONLINE:
			switch (mState) {
			case RECONNECT_DELAYED:
				// TODO: cancel timer
			case RECONNECT_NETWORK:
			case OFFLINE:
				// update state before starting thread to prevent race conditions
				updateConnectionState(ConnectionState.CONNECTING);

				// register ping (connection) timeout handler: 2*PACKET_TIMEOUT(30s) + 3s
				registerPongTimeout(2*PACKET_TIMEOUT + 3000, "connection");

				new Thread() {
					@Override
					public void run() {
						updateConnectingThread(this);
						try {
							doConnect(create_account);
						} catch (final IllegalArgumentException e) {
							// this might happen when DNS resolution in ConnectionConfiguration fails
							onDisconnected(e);
						} catch (final YaximXMPPException e) {
							onDisconnected(e);
						} finally {
							mAlarmManager.cancel(mPongTimeoutAlarmPendIntent);
							finishConnectingThread();
						}
					}
				}.start();
				break;
			case CONNECTING:
			case DISCONNECTING:
				// ignore all other cases
				break;
			}
			break;
		case DISCONNECTED:
			// spawn thread to do disconnect
			if (mState == ConnectionState.ONLINE) {
				// update state before starting thread to prevent race conditions
				updateConnectionState(ConnectionState.DISCONNECTING);

				// register ping (connection) timeout handler: PACKET_TIMEOUT(30s)
				registerPongTimeout(PACKET_TIMEOUT, "forced disconnect");

				new Thread() {
					@Override
                    public void run() {
						updateConnectingThread(this);
						mStreamHandler.quickShutdown();
						onDisconnected("forced disconnect completed");
						finishConnectingThread();
						//updateConnectionState(ConnectionState.OFFLINE);
					}
				}.start();
			}
			break;
		case OFFLINE:
			switch (mState) {
			case CONNECTING:
			case ONLINE:
				// update state before starting thread to prevent race conditions
				updateConnectionState(ConnectionState.DISCONNECTING);

				// register ping (connection) timeout handler: PACKET_TIMEOUT(30s)
				registerPongTimeout(PACKET_TIMEOUT, "manual disconnect");

				// spawn thread to do disconnect
				new Thread() {
					@Override
                    public void run() {
						updateConnectingThread(this);
						mXMPPConnection.shutdown();
						mStreamHandler.close();
						mAlarmManager.cancel(mPongTimeoutAlarmPendIntent);
						// we should reset XMPPConnection the next time
						mConfig.reconnect_required = true;
						finishConnectingThread();
						// reconnect if it was requested in the meantime
						if (mRequestedState == ConnectionState.ONLINE) {
                            requestConnectionState(ConnectionState.ONLINE);
                        }
					}
				}.start();
				break;
			case DISCONNECTING:
				break;
			case RECONNECT_DELAYED:
				// TODO: clear timer
			case RECONNECT_NETWORK:
				updateConnectionState(ConnectionState.OFFLINE);
			}
			break;
		case RECONNECT_NETWORK:
		case RECONNECT_DELAYED:
			switch (mState) {
			case DISCONNECTED:
			case RECONNECT_NETWORK:
			case RECONNECT_DELAYED:
				updateConnectionState(new_state);
				break;
			default:
				throw new IllegalArgumentException("Can not go from " + mState + " to " + new_state);
			}
		}
	}
	@Override
	public void requestConnectionState(final ConnectionState new_state) {
		requestConnectionState(new_state, false);
	}

	@Override
	public ConnectionState getConnectionState() {
		return mState;
	}

	// called at the end of a state transition
	private synchronized void updateConnectionState(final ConnectionState new_state) {
		if (new_state == ConnectionState.ONLINE || new_state == ConnectionState.CONNECTING) {
            mLastError = null;
        }
		Log.d(TAG, "updateConnectionState: " + mState + " -> " + new_state + " (" + mLastError + ")");
		if (new_state == mState) {
            return;
        }
		mState = new_state;
		if (mServiceCallBack != null) {
            mServiceCallBack.connectionStateChanged();
        }
	}
	private void initServiceDiscovery() {
		// register connection features
		final ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(mXMPPConnection);

		// init Entity Caps manager with storage in app's cache dir
		try {
			if (capsCacheDir == null) {
				capsCacheDir = new File(mService.getCacheDir(), "entity-caps-cache");
				capsCacheDir.mkdirs();
				EntityCapsManager.setPersistentCache(new SimpleDirectoryPersistentCache(capsCacheDir));
			}
		} catch (final java.io.IOException e) {
			Log.e(TAG, "Could not init Entity Caps cache: " + e.getLocalizedMessage());
		}

		// reference PingManager, set ping flood protection to 10s
		PingManager.getInstanceFor(mXMPPConnection).setPingMinimumInterval(10*1000);

		// set Version for replies
		final String app_name = mService.getString(org.yaxim.androidclient.R.string.app_name);
		final String build_revision = mService.getString(org.yaxim.androidclient.R.string.build_revision);
		Version.Manager.getInstanceFor(mXMPPConnection).setVersion(
				new Version(app_name, build_revision, "Android"));

		// reference DeliveryReceiptManager, add listener
		final DeliveryReceiptManager dm = DeliveryReceiptManager.getInstanceFor(mXMPPConnection);
		dm.enableAutoReceipts();
		dm.addReceiptReceivedListener(new ReceiptReceivedListener() { // DOES NOT WORK IN CARBONS
			@Override
            public void onReceiptReceived(final String fromJid, final String toJid, final String receiptId) {
				Log.d(TAG, "got delivery receipt for " + receiptId);
				changeMessageDeliveryStatus(receiptId, ChatConstants.DS_ACKED);
			}});
	}

	@Override
    public void addRosterItem(final String user, final String alias, final String group)
			throws YaximXMPPException {
		tryToAddRosterEntry(user, alias, group);
	}

	@Override
    public void removeRosterItem(final String user) throws YaximXMPPException {
		debugLog("removeRosterItem(" + user + ")");

		// tryToRemoveRosterEntry(user);
		deleteRosterEntryFromDB(user);
		mServiceCallBack.rosterChanged();
	}

	@Override
    public void renameRosterItem(final String user, final String newName)
			throws YaximXMPPException {
		final RosterEntry rosterEntry = mRoster.getEntry(user);

		if (!(newName.length() > 0) || (rosterEntry == null)) {
			throw new YaximXMPPException("JabberID to rename is invalid!");
		}
		rosterEntry.setName(newName);
	}

	@Override
    public void addRosterGroup(final String group) {
		mRoster.createGroup(group);
	}

	@Override
    public void renameRosterGroup(final String group, final String newGroup) {
		final RosterGroup groupToRename = mRoster.getGroup(group);
		groupToRename.setName(newGroup);
	}

	@Override
    public void moveRosterItemToGroup(final String user, final String group)
			throws YaximXMPPException {
		tryToMoveRosterEntryToGroup(user, group);
	}

	@Override
    public void sendPresenceRequest(final String user, final String type) {
		// HACK: remove the fake roster entry added by handleIncomingSubscribe()
		if ("unsubscribed".equals(type)) {
            deleteRosterEntryFromDB(user);
        }
		final Presence response = new Presence(Presence.Type.valueOf(type));
		response.setTo(user);
		mXMPPConnection.sendPacket(response);
	}
	
	private void onDisconnected(final String reason) {
		unregisterPongListener();
		mLastError = reason;
		updateConnectionState(ConnectionState.DISCONNECTED);
	}
	private void onDisconnected(Throwable reason) {
		Log.e(TAG, "onDisconnected: " + reason);
		reason.printStackTrace();
		// iterate through to the deepest exception
		while (reason.getCause() != null) {
            reason = reason.getCause();
        }
		onDisconnected(reason.getLocalizedMessage());
	}

	private void tryToConnect(final boolean create_account) throws YaximXMPPException {
		try {
			if (mXMPPConnection.isConnected()) {
				try {
					mStreamHandler.quickShutdown(); // blocking shutdown prior to re-connection
				} catch (final Exception e) {
					debugLog("conn.shutdown() failed: " + e);
				}
			}
			registerRosterListener();
			final boolean need_bind = !mStreamHandler.isResumePossible();

			mXMPPConnection.connect(need_bind);
			// the following should not happen as of smack 3.3.1
			if (!mXMPPConnection.isConnected()) {
				throw new YaximXMPPException("SMACK connect failed without exception!");
			}
			if (mConnectionListener != null) {
                mXMPPConnection.removeConnectionListener(mConnectionListener);
            }
			mConnectionListener = new ConnectionListener() {
				@Override
                public void connectionClosedOnError(final Exception e) {
					onDisconnected(e);
				}
				@Override
                public void connectionClosed() {
					// TODO: fix reconnect when we got kicked by the server or SM failed!
					//onDisconnected(null);
					updateConnectionState(ConnectionState.OFFLINE);
				}
				@Override
                public void reconnectingIn(final int seconds) { }
				@Override
                public void reconnectionFailed(final Exception e) { }
				@Override
                public void reconnectionSuccessful() { }
			};
			mXMPPConnection.addConnectionListener(mConnectionListener);

			// SMACK auto-logins if we were authenticated before
			if (!mXMPPConnection.isAuthenticated()) {
				if (create_account) {
					Log.d(TAG, "creating new server account...");
					final AccountManager am = new AccountManager(mXMPPConnection);
					am.createAccount(mConfig.userName, mConfig.password);
				}
				mXMPPConnection.login(mConfig.userName, mConfig.password,
						mConfig.ressource);
			}
			Log.d(TAG, "SM: can resume = " + mStreamHandler.isResumePossible() + " needbind=" + need_bind);
			if (need_bind) {
				mStreamHandler.notifyInitialLogin();
				setStatusFromConfig();
			}

		} catch (final YaximXMPPException e) {
			throw e;
		} catch (final Exception e) {
			// actually we just care for IllegalState or NullPointer or XMPPEx.
			throw new YaximXMPPException("tryToConnect failed", e);
		}
	}

	private void tryToMoveRosterEntryToGroup(final String userName, final String groupName)
			throws YaximXMPPException {

		final RosterGroup rosterGroup = getRosterGroup(groupName);
		final RosterEntry rosterEntry = mRoster.getEntry(userName);

		removeRosterEntryFromGroups(rosterEntry);

		if (groupName.length() == 0) {
            return;
        } else {
			try {
				rosterGroup.addEntry(rosterEntry);
			} catch (final XMPPException e) {
				throw new YaximXMPPException("tryToMoveRosterEntryToGroup", e);
			}
		}
	}

	private RosterGroup getRosterGroup(final String groupName) {
		RosterGroup rosterGroup = mRoster.getGroup(groupName);

		// create group if unknown
		if ((groupName.length() > 0) && rosterGroup == null) {
			rosterGroup = mRoster.createGroup(groupName);
		}
		return rosterGroup;

	}

	private void removeRosterEntryFromGroups(final RosterEntry rosterEntry)
			throws YaximXMPPException {
		final Collection<RosterGroup> oldGroups = rosterEntry.getGroups();

		for (final RosterGroup group : oldGroups) {
			tryToRemoveUserFromGroup(group, rosterEntry);
		}
	}

	private void tryToRemoveUserFromGroup(final RosterGroup group,
			final RosterEntry rosterEntry) throws YaximXMPPException {
		try {
			group.removeEntry(rosterEntry);
		} catch (final XMPPException e) {
			throw new YaximXMPPException("tryToRemoveUserFromGroup", e);
		}
	}

	private void tryToRemoveRosterEntry(final String user) throws YaximXMPPException {
		try {
			final RosterEntry rosterEntry = mRoster.getEntry(user);

			if (rosterEntry != null) {
				// first, unsubscribe the user
				final Presence unsub = new Presence(Presence.Type.unsubscribed);
				unsub.setTo(rosterEntry.getUser());
				mXMPPConnection.sendPacket(unsub);
				// then, remove from roster
				mRoster.removeEntry(rosterEntry);
			}
		} catch (final XMPPException e) {
			throw new YaximXMPPException("tryToRemoveRosterEntry", e);
		}
	}

	private void tryToAddRosterEntry(final String user, final String alias, final String group)
			throws YaximXMPPException {
		try {
			if (mRoster != null) {
				mRoster.createEntry(user, alias, new String[] { group });
			}
		} catch (final XMPPException e) {
			throw new YaximXMPPException("tryToAddRosterEntry", e);
		}
	}

	private void removeOldRosterEntries() {
		Log.d(TAG, "removeOldRosterEntries()");
		final Collection<RosterEntry> rosterEntries = mRoster.getEntries();
		final StringBuilder exclusion = new StringBuilder(RosterConstants.JID + " NOT IN (");
		boolean first = true;
		for (final RosterEntry rosterEntry : rosterEntries) {
			updateRosterEntryInDB(rosterEntry);
			if (first) {
                first = false;
            } else {
                exclusion.append(",");
            }
			exclusion.append("'").append(rosterEntry.getUser()).append("'");
		}
		exclusion.append(")");
		final int count = mContentResolver.delete(RosterProvider.CONTENT_URI, exclusion.toString(), null);
		Log.d(TAG, "deleted " + count + " old roster entries");
	}

	// HACK: add an incoming subscription request as a fake roster entry
	private void handleIncomingSubscribe(final Presence request) {
		final ContentValues values = new ContentValues();

		values.put(RosterConstants.JID, request.getFrom());
		values.put(RosterConstants.ALIAS, request.getFrom());
		values.put(RosterConstants.GROUP, "");

		values.put(RosterConstants.STATUS_MODE, getStatusInt(request));
		values.put(RosterConstants.STATUS_MESSAGE, request.getStatus());
		
		final Uri uri = mContentResolver.insert(RosterProvider.CONTENT_URI, values);
		debugLog("handleIncomingSubscribe: faked " + uri);
	}

	@Override
    public void setStatusFromConfig() {
		// TODO: only call this when carbons changed, not on every presence change
		CarbonManager.getInstanceFor(mXMPPConnection).sendCarbonsEnabled(mConfig.messageCarbons);

		final Presence presence = new Presence(Presence.Type.available);
		final Mode mode = Mode.valueOf(mConfig.statusMode);
		presence.setMode(mode);
		presence.setStatus(mConfig.statusMessage);
		presence.setPriority(mConfig.priority);
		mXMPPConnection.sendPacket(presence);
		mConfig.presence_required = false;
	}

	public void sendOfflineMessages() {
		final Cursor cursor = mContentResolver.query(ChatProvider.CONTENT_URI,
				SEND_OFFLINE_PROJECTION, SEND_OFFLINE_SELECTION,
				null, null);
		final int      _ID_COL = cursor.getColumnIndexOrThrow(ChatConstants._ID);
		final int      JID_COL = cursor.getColumnIndexOrThrow(ChatConstants.JID);
		final int      MSG_COL = cursor.getColumnIndexOrThrow(ChatConstants.MESSAGE);
		final int       TS_COL = cursor.getColumnIndexOrThrow(ChatConstants.DATE);
		final int PACKETID_COL = cursor.getColumnIndexOrThrow(ChatConstants.PACKET_ID);
		final ContentValues mark_sent = new ContentValues();
		mark_sent.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_SENT_OR_READ);
		while (cursor.moveToNext()) {
			final int _id = cursor.getInt(_ID_COL);
			final String toJID = cursor.getString(JID_COL);
			final String message = cursor.getString(MSG_COL);
			String packetID = cursor.getString(PACKETID_COL);
			final long ts = cursor.getLong(TS_COL);
			Log.d(TAG, "sendOfflineMessages: " + toJID + " > " + message);
			final Message newMessage = new Message(toJID, Message.Type.chat);
			newMessage.setBody(message);
			final DelayInformation delay = new DelayInformation(new Date(ts));
			newMessage.addExtension(delay);
			newMessage.addExtension(new DelayInfo(delay));
			newMessage.addExtension(new DeliveryReceiptRequest());
			if ((packetID != null) && (packetID.length() > 0)) {
				newMessage.setPacketID(packetID);
			} else {
				packetID = newMessage.getPacketID();
				mark_sent.put(ChatConstants.PACKET_ID, packetID);
			}
			final Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY
				+ "/" + ChatProvider.TABLE_NAME + "/" + _id);
			mContentResolver.update(rowuri, mark_sent,
						null, null);
			mXMPPConnection.sendPacket(newMessage);		// must be after marking delivered, otherwise it may override the SendFailListener
		}
		cursor.close();
	}

	public static void sendOfflineMessage(final ContentResolver cr, final String toJID, final String message, final String myJID) {
		final ContentValues values = new ContentValues();
		values.put(ChatConstants.DIRECTION, ChatConstants.OUTGOING);
		values.put(ChatConstants.JID, toJID);
		values.put(ChatConstants.MESSAGE, message);
		values.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_NEW);
		values.put(ChatConstants.DATE, System.currentTimeMillis());
		values.put(ChatConstants.SENDER, myJID);
		values.put(ChatConstants.TYPE, RoomsMessageType.TextMessage.ordinal());

		cr.insert(ChatProvider.CONTENT_URI, values);
	}

	public void sendReceipt(final String toJID, final String id) {
		Log.d(TAG, "sending XEP-0184 ack to " + toJID + " id=" + id);
		final Message ack = new Message(toJID, Message.Type.normal);
		ack.addExtension(new DeliveryReceipt(id));
		mXMPPConnection.sendPacket(ack);
	}

	@Override
    public void sendMessage(final String toJID, final String message) {
		final Message newMessage = new Message(toJID, Message.Type.chat);
		
		try {
			final boolean isRoomMessage = toJID.indexOf('@') == -1;
            final TextMessage textMsg = RoomsMessageFactory.getRoomsMessage( RoomsMessageType.TextMessage );
			textMsg.setSender(mConfig.jabberID);
			textMsg.setText(message);
			
			if (isRoomMessage) {
				final List<String> recipients = new ArrayList<String>();
				for (final Participant p : getRoomParticipants(toJID)) {
					for (final Identity i : p.getIdentities()) {
						recipients.add(p.getJid() + "/" + i.getResource());
					}
				}
				textMsg.setRecipients(recipients);
                textMsg.setRoomId( toJID );
				final String encryptedMsg = crypto.encryptMessage(textMsg);
				final String payload = "<body>" + encryptedMsg + "</body>";
				final LeafNode roomNode = pubSub.getNode(toJID);
				roomNode.send(new PayloadItem<SimplePayload>("msg_" + System.currentTimeMillis(), 
						new SimplePayload(null, null, payload)));
				
				// Send push request
                final PushRequest pushRequest = RoomsMessageFactory.getRoomsMessage( RoomsMessageType.PushRequest );
				recipients.remove(mConfig.jabberID); // Do not push to myself 
				pushRequest.setParticipants(recipients);
				sendControlMessage(pushRequest);
			} 
			else {  // One-to-one message
				try {
					textMsg.setRecipients(Arrays.asList(toJID));
					final String encryptedMsg = crypto.encryptMessage(textMsg);
					newMessage.setBody(encryptedMsg);
				}
				catch (final Exception ex) {
					ex.printStackTrace();
					newMessage.setBody(message);
				}
				newMessage.addExtension(new DeliveryReceiptRequest());
			}
			
			if (isAuthenticated()) {
				if (!isRoomMessage) {
					addChatMessageToDB(ChatConstants.OUTGOING, toJID, message, ChatConstants.DS_SENT_OR_READ,
						System.currentTimeMillis(), newMessage.getPacketID(), toJID, RoomsMessageType.TextMessage, null);
					mXMPPConnection.sendPacket(newMessage);
				}
			} 
			else {
				// send offline -> store to DB
				addChatMessageToDB(ChatConstants.OUTGOING, toJID, message, ChatConstants.DS_NEW,
						System.currentTimeMillis(), newMessage.getPacketID(), toJID, RoomsMessageType.TextMessage, null);
			}
		}
		catch (final Exception ex) {
			Log.e(TAG, "Failed to publish pubsub message", ex);
		}
	}

	@Override
    public boolean isAuthenticated() {
		if (mXMPPConnection != null) {
			return (mXMPPConnection.isConnected() && mXMPPConnection
					.isAuthenticated());
		}
		return false;
	}

	@Override
    public void registerCallback(final XMPPServiceCallback callBack) {
		this.mServiceCallBack = callBack;
		mService.registerReceiver(mPingAlarmReceiver, new IntentFilter(PING_ALARM));
		mService.registerReceiver(mPongTimeoutAlarmReceiver, new IntentFilter(PONG_TIMEOUT_ALARM));
	}

	@Override
    public void unRegisterCallback() {
		debugLog("unRegisterCallback()");
		// remove callbacks _before_ tossing old connection
		try {
			mXMPPConnection.getRoster().removeRosterListener(mRosterListener);
			mXMPPConnection.removePacketListener(mPacketListener);
			mXMPPConnection.removePacketListener(mPresenceListener);

			mXMPPConnection.removePacketListener(mPongListener);
			unregisterPongListener();
		} catch (final Exception e) {
			// ignore it!
			e.printStackTrace();
		}
		requestConnectionState(ConnectionState.OFFLINE);
		setStatusOffline();
		mService.unregisterReceiver(mPingAlarmReceiver);
		mService.unregisterReceiver(mPongTimeoutAlarmReceiver);
		this.mServiceCallBack = null;
	}
	
	@Override
    public String getNameForJID(final String jid, final String roomID) {
		if (roomID != null) {
			for (final Participant p : getRoomParticipants(roomID)) {
				if (p.getJid().equals(jid)) {
					return p.getName();
				}
			}
		}
		if (jid.startsWith("control-client")) {
			return "ROOMS"; // Hardcoded nickname for control client
		}
		if (this.mRoster != null && null != this.mRoster.getEntry(jid) && null != this.mRoster.getEntry(jid).getName() && this.mRoster.getEntry(jid).getName().length() > 0) {
			return this.mRoster.getEntry(jid).getName();
		} else {
			return jid;
		}			
	}

	private void setStatusOffline() {
		final ContentValues values = new ContentValues();
		values.put(RosterConstants.STATUS_MODE, StatusMode.offline.ordinal());
		mContentResolver.update(RosterProvider.CONTENT_URI, values, null, null);
	}

	private void registerRosterListener() {
		// flush roster on connecting.
		mRoster = mXMPPConnection.getRoster();
		mRoster.setSubscriptionMode(Roster.SubscriptionMode.manual);

		if (mRosterListener != null) {
            mRoster.removeRosterListener(mRosterListener);
        }

		mRosterListener = new RosterListener() {
			private boolean first_roster = true;

			@Override
            public void entriesAdded(final Collection<String> entries) {
				debugLog("entriesAdded(" + entries + ")");

				final ContentValues[] cvs = new ContentValues[entries.size()];
				int i = 0;
				for (final String entry : entries) {
					final RosterEntry rosterEntry = mRoster.getEntry(entry);
					cvs[i++] = getContentValuesForRosterEntry(rosterEntry);
				}
				mContentResolver.bulkInsert(RosterProvider.CONTENT_URI, cvs);
				// when getting the roster in the beginning, remove remains of old one
				if (first_roster) {
					removeOldRosterEntries();
					first_roster = false;
					mServiceCallBack.rosterChanged();
				}
				debugLog("entriesAdded() done");
			}

			@Override
            public void entriesDeleted(final Collection<String> entries) {
				debugLog("entriesDeleted(" + entries + ")");

				for (final String entry : entries) {
					deleteRosterEntryFromDB(entry);
				}
				mServiceCallBack.rosterChanged();
			}

			@Override
            public void entriesUpdated(final Collection<String> entries) {
				debugLog("entriesUpdated(" + entries + ")");

				for (final String entry : entries) {
					final RosterEntry rosterEntry = mRoster.getEntry(entry);
					updateRosterEntryInDB(rosterEntry);
				}
				mServiceCallBack.rosterChanged();
			}

			@Override
            public void presenceChanged(final Presence presence) {
				debugLog("presenceChanged(" + presence.getFrom() + "): " + presence);

				final String jabberID = getBareJID(presence.getFrom());
				final RosterEntry rosterEntry = mRoster.getEntry(jabberID);
				updateRosterEntry(rosterEntry);
				mServiceCallBack.rosterChanged();
			}
		};
		mRoster.addRosterListener(mRosterListener);
	}

	private String getBareJID(final String from) {
		final String[] res = from.split("/");
		return res[0].toLowerCase();
	}

	public boolean changeMessageDeliveryStatus(final String packetID, final int new_status) {
		final ContentValues cv = new ContentValues();
		cv.put(ChatConstants.DELIVERY_STATUS, new_status);
		final Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/"
				+ ChatProvider.TABLE_NAME);
		return mContentResolver.update(rowuri, cv,
				ChatConstants.PACKET_ID + " = ? AND " +
				ChatConstants.DELIVERY_STATUS + " != " + ChatConstants.DS_ACKED + " AND " +
				ChatConstants.DIRECTION + " = " + ChatConstants.OUTGOING,
				new String[] { packetID }) > 0;
	}

	/** Check the server connection, reconnect if needed.
	 *
	 * This function will try to ping the server if we are connected, and try
	 * to reestablish a connection otherwise.
	 */
	@Override
    public void sendServerPing() {
		if (mXMPPConnection == null || !mXMPPConnection.isAuthenticated()) {
			debugLog("Ping: requested, but not connected to server.");
			requestConnectionState(ConnectionState.ONLINE, false);
			return;
		}
		if (mPingID != null) {
			debugLog("Ping: requested, but still waiting for " + mPingID);
			return; // a ping is still on its way
		}

		if (mStreamHandler.isSmEnabled()) {
			debugLog("Ping: sending SM request");
			mPingID = "" + mStreamHandler.requestAck();
		} else {
			final Ping ping = new Ping();
			ping.setType(Type.GET);
			ping.setTo(mConfig.server);
			mPingID = ping.getPacketID();
			debugLog("Ping: sending ping " + mPingID);
			mXMPPConnection.sendPacket(ping);
		}

		// register ping timeout handler: PACKET_TIMEOUT(30s) + 3s
		registerPongTimeout(PACKET_TIMEOUT + 3000, mPingID);
	}

	private void gotServerPong(final String pongID) {
		final long latency = System.currentTimeMillis() - mPingTimestamp;
		if (pongID != null && pongID.equals(mPingID)) {
            Log.i(TAG, String.format("Ping: server latency %1.3fs",
						latency/1000.));
        } else {
            Log.i(TAG, String.format("Ping: server latency %1.3fs (estimated)",
						latency/1000.));
        }
		mPingID = null;
		mAlarmManager.cancel(mPongTimeoutAlarmPendIntent);
	}

	/** Register a "pong" timeout on the connection. */
	private void registerPongTimeout(final long wait_time, final String id) {
		mPingID = id;
		mPingTimestamp = System.currentTimeMillis();
		debugLog(String.format("Ping: registering timeout for %s: %1.3fs", id, wait_time/1000.));
		mAlarmManager.set(AlarmManager.RTC_WAKEUP,
				System.currentTimeMillis() + wait_time,
				mPongTimeoutAlarmPendIntent);
	}

	/**
	 * BroadcastReceiver to trigger reconnect on pong timeout.
	 */
	private class PongTimeoutAlarmReceiver extends BroadcastReceiver {
		@Override
        public void onReceive(final Context ctx, final Intent i) {
			debugLog("Ping: timeout for " + mPingID);
			onDisconnected("Ping timeout");
		}
	}

	/**
	 * BroadcastReceiver to trigger sending pings to the server
	 */
	private class PingAlarmReceiver extends BroadcastReceiver {
		@Override
        public void onReceive(final Context ctx, final Intent i) {
				sendServerPing();
		}
	}

	/**
	 * Registers a smack packet listener for IQ packets, intended to recognize "pongs" with
	 * a packet id matching the last "ping" sent to the server.
	 *
	 * Also sets up the AlarmManager Timer plus necessary intents.
	 */
	private void registerPongListener() {
		// reset ping expectation on new connection
		mPingID = null;

		if (mPongListener != null) {
            mXMPPConnection.removePacketListener(mPongListener);
        }

		mPongListener = new PacketListener() {

			@Override
			public void processPacket(final Packet packet) {
				if (packet == null) {
                    return;
                }

				gotServerPong(packet.getPacketID());
			}

		};

		mXMPPConnection.addPacketListener(mPongListener, new PacketTypeFilter(IQ.class));
		mPingAlarmPendIntent = PendingIntent.getBroadcast(mService.getApplicationContext(), 0, mPingAlarmIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);
		mPongTimeoutAlarmPendIntent = PendingIntent.getBroadcast(mService.getApplicationContext(), 0, mPongTimeoutAlarmIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);
		mAlarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, 
				System.currentTimeMillis() + AlarmManager.INTERVAL_FIFTEEN_MINUTES, AlarmManager.INTERVAL_FIFTEEN_MINUTES, mPingAlarmPendIntent);
	}
	
	private void unregisterPongListener() {
		mAlarmManager.cancel(mPingAlarmPendIntent);
		mAlarmManager.cancel(mPongTimeoutAlarmPendIntent);
	}

	private void registerMessageListener() {
		// do not register multiple packet listeners
		if (mPacketListener != null) {
            mXMPPConnection.removePacketListener(mPacketListener);
        }

		final PacketTypeFilter filter = new PacketTypeFilter(Message.class);

		mPacketListener = new PacketListener() {
			@Override
            public void processPacket(final Packet packet) {
				try {
				if (packet instanceof Message) {
					final Message msg = (Message) packet;

					final String fromJID = getBareJID(msg.getFrom());
					final int direction = ChatConstants.INCOMING;
					final Carbon cc = CarbonManager.getCarbon(msg);

					// extract timestamp
					long ts;
					DelayInfo timestamp = (DelayInfo)msg.getExtension("delay", "urn:xmpp:delay");
					if (timestamp == null) {
                        timestamp = (DelayInfo)msg.getExtension("x", "jabber:x:delay");
                    }
					if (cc != null) {
                        timestamp = cc.getForwarded().getDelayInfo();
                    }
					if (timestamp != null) {
                        ts = timestamp.getStamp().getTime();
                    } else {
                        ts = System.currentTimeMillis();
                    }
					
					// Extract pubsub info
					final EventElement eventElement = (EventElement)msg.getExtension("http://jabber.org/protocol/pubsub#event");
					if (eventElement != null && eventElement.getEventType() == EventElementType.items) {
						final ItemsExtension itemsExt = (ItemsExtension)eventElement.getEvent();
						for (final Item item : (List<Item>)itemsExt.getItems()) {
							processPubSubItem(item, new Date(), itemsExt.getNode().substring(itemsExt.getNode().lastIndexOf('/')+1));
						}
						return;
					}

					// display error inline
					if (msg.getType() == Message.Type.error) {
						if (changeMessageDeliveryStatus(msg.getPacketID(), ChatConstants.DS_FAILED)) {
                            mServiceCallBack.messageError(fromJID, msg.getError().toString(), (cc != null));
                        }
						return; // we do not want to add errors as "incoming messages"
					}
					
					// ignore empty messages
					if (msg.getBody() == null) {
						Log.d(TAG, "empty message.");
						return;
					}

					// carbons are old. all others are new
					int is_new = (cc == null) ? ChatConstants.DS_NEW : ChatConstants.DS_SENT_OR_READ;
					if (msg.getType() == Message.Type.error){
						is_new = ChatConstants.DS_FAILED;
					}
					
					processMessage(direction, fromJID, msg, is_new, ts);
				}
				} catch (final Exception e) {
					// SMACK silently discards exceptions dropped from processPacket :(
					Log.e(TAG, "failed to process packet:");
					e.printStackTrace();
				}
			}
		};

		mXMPPConnection.addPacketListener(mPacketListener, filter);
	}

	protected void processMessage(final int direction, final String fromJID, final Message msg, final int is_new, final long ts) {

		if (msg.getBody() == null) {
			return;
		}
		try {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( mService );
			final RoomsMessage roomsMessage = crypto.decryptMessage(msg.getBody(), mConfig.jabberID, fromJID, null);
			if (roomsMessage == null) {
				return; 
			}
			else if (roomsMessage instanceof TextMessage) {
				final TextMessage textMessage = (TextMessage)roomsMessage;
				final boolean isNew = addChatMessageToDB(direction, fromJID, textMessage.getText(), is_new, ts, msg.getPacketID(), textMessage.getSender(), RoomsMessageType.TextMessage, null);
				if (direction == ChatConstants.INCOMING && isNew) {
					mServiceCallBack.newMessage(fromJID, null, textMessage.getText(), false);
				}
			}
			else if (roomsMessage instanceof Invitation) {
				final Invitation invitation = (Invitation)roomsMessage;
				final ContentValues values = new ContentValues();
				values.put(RoomsConstants.ID, invitation.getRoomID());
				values.put(RoomsConstants.NAME, invitation.getRoomName());
				values.put(RoomsConstants.STATUS, StatusMode.chat.ordinal());
				values.put(RoomsConstants.CREATED, new Date().getTime());
				values.put(RoomsConstants.TOPIC, "");
				values.put(RoomsConstants.OWNER, invitation.getSender());
				values.put(RoomsConstants.LOGGER, "");
				upsertRoom(values, invitation.getRoomID());
				
				upsertParticipants(invitation.getRoomID(), invitation.getParticipants());
				for (final Participant p : invitation.getParticipants()) {
					for (final Identity identity : p.getIdentities()) {
						crypto.getKeyAccessor().savePublicKey(p.getJid(), identity.getResource(), identity.getPublicKey());
					}
				}
				mServiceCallBack.rosterChanged();

				final LeafNode roomNode = pubSub.getNode(invitation.getRoomID());
				final Subscription sub = roomNode.subscribe(mConfig.jabberID);
				for (final org.jivesoftware.smackx.pubsub.Item item : roomNode.getItems(sub.getId())) {
					processPubSubItem(item, new Date(), invitation.getRoomID());
				}
			}
			else if (roomsMessage instanceof RegistrationConfirmation) {
				final RegistrationConfirmation confirm = (RegistrationConfirmation)roomsMessage;
				prefs.edit().putString(PreferenceConstants.JID, confirm.getBareJid())
					.putString(PreferenceConstants.PASSWORD, confirm.getPassword())
					.putString(PreferenceConstants.RESSOURCE, confirm.getResource())
					.putBoolean(PreferenceConstants.CONN_STARTUP, true)
					.commit();
				final ContentValues values = new ContentValues();
				values.put(KeysConstants.JID, confirm.getBareJid());
                YaximApplication.getApp( mService ).mCrypto.init( mConfig.jabberID );
				mContentResolver.update(RosterProvider.KEYS_URI, values, KeysConstants.JID + " = ?", new String[] { KeyAccessor.NEW_USER });
				mXMPPConnection.disconnect();
				requestConnectionState(ConnectionState.ONLINE);
				
				// Update info 
				final PersonalInfo message = new PersonalInfo();
				message.setName(prefs.getString(PreferenceConstants.NAME, ""));
				sendControlMessage(message);
				
				// Register for push
				YaximApplication.getApp(mService).registerForGCM(mService, confirm.getBareJid());
			}
            else if ( roomsMessage instanceof CompanyAffiliationRequest ) {
                prefs.edit().putString( PreferenceConstants.COMPANIES, roomsMessage.getBody().toString() ).commit();
                mContentResolver.notifyChange( RosterProvider.COMPANIES_URI, null ); // Notify frontend
            }
			else if (roomsMessage instanceof ContactList) {
				final ContactList contactList = (ContactList)roomsMessage;
				for (final Participant p : contactList.getContacts()) {
					for (final Identity identity : p.getIdentities()) {
						final ContentValues values = new ContentValues();
						values.put(RosterConstants.JID, p.getJid());
						values.put(RosterConstants.ALIAS, p.getName());
						values.put(RosterConstants.STATUS_MODE, StatusMode.offline.ordinal());
						values.put(RosterConstants.STATUS_MESSAGE, "");
						values.put(RosterConstants.GROUP, "");
						upsertRoster(values, p.getJid());
						crypto.getKeyAccessor().savePublicKey(p.getJid(), null, identity.getPublicKey());
					}
				}
				mServiceCallBack.rosterChanged();
			}
		}
		catch (final Exception ex) {
			Log.e(TAG, ex.getMessage(), ex);
		}
	}
	
	private void processPubSubItem(final org.jivesoftware.smackx.pubsub.Item item, final Date publishDate, final String roomID) {
		final SimplePayload payload = ((PayloadItem<SimplePayload>)item).getPayload();
		final String xml = payload.toXML();
		Log.d(TAG, xml);
		final String encryptedMessage = xml.substring(xml.indexOf('>') + 1, xml.lastIndexOf('<'));
		try {
			final RoomsMessage roomsMessage = crypto.decryptMessage(encryptedMessage, mConfig.jabberID, null, roomID);
			if (roomsMessage != null) {
				if (roomsMessage instanceof RoomConfiguration) { // Room configuration changed
					final RoomConfiguration configuration = (RoomConfiguration)roomsMessage;
					final ContentValues values = new ContentValues();
					values.put(RoomsConstants.ID, configuration.getRoomID());
					values.put(RoomsConstants.NAME, configuration.getRoomName());
					values.put(RoomsConstants.STATUS, StatusMode.chat.ordinal());
					values.put(RoomsConstants.CREATED, publishDate.getTime());
					values.put(RoomsConstants.TOPIC, "");
					values.put(RoomsConstants.OWNER, configuration.getSender());
					values.put(RoomsConstants.LOGGER, "");
					upsertRoom(values, configuration.getRoomID());
					
					upsertParticipants(configuration.getRoomID(), configuration.getParticipants());
					for (final Participant p : configuration.getParticipants()) {
						for (final Identity identity : p.getIdentities()) {
							crypto.getKeyAccessor().savePublicKey(p.getJid(), identity.getResource(), identity.getPublicKey());
						}
					}
					mServiceCallBack.rosterChanged();
				}
				else if (roomsMessage instanceof TextMessage) {
					final TextMessage textMessage = (TextMessage)roomsMessage;
					final String text = textMessage.getText();
					if (addChatMessageToDB(ChatConstants.INCOMING, roomID, text, 1, publishDate.getTime(), item.getId(), textMessage.getSender(), RoomsMessageType.TextMessage, null) 
							&& !textMessage.getSender().equals(mConfig.jabberID)){
						mServiceCallBack.newMessage(textMessage.getSender(), roomID, text, false);
					}
				}
				else if (roomsMessage instanceof FileMessage) {
					final FileMessage fileMessage = (FileMessage)roomsMessage;
					if (addChatMessageToDB(ChatConstants.INCOMING, roomID, fileMessage.getDescription(), 1, publishDate.getTime(), item.getId(), fileMessage.getSender(), RoomsMessageType.File, fileMessage.getBody().toString())
							&& !fileMessage.getSender().equals(mConfig.jabberID)) {
						mServiceCallBack.newMessage(fileMessage.getSender(), roomID, fileMessage.getDescription(), false);
					}
				}
				else if (roomsMessage instanceof TaskMessage) {
					final TaskMessage task = (TaskMessage)roomsMessage;
					if (addChatMessageToDB(ChatConstants.INCOMING, roomID, task.getText(), 1, publishDate.getTime(), item.getId(), task.getSender(), RoomsMessageType.Task, task.getBody().toString())
							&& !task.getSender().equals(mConfig.jabberID)) {
						mServiceCallBack.newMessage(task.getSender(), roomID, task.getText(), false);
					}
				}
			}
		}
		catch (final Exception ex) {
			Log.e(TAG, ex.getMessage(), ex);
		}
	}

	private void registerPresenceListener() {
		// do not register multiple packet listeners
		if (mPresenceListener != null) {
            mXMPPConnection.removePacketListener(mPresenceListener);
        }

		mPresenceListener = new PacketListener() {
			@Override
            public void processPacket(final Packet packet) {
				try {
					final Presence p = (Presence) packet;
					switch (p.getType()) {
					case subscribe:
						handleIncomingSubscribe(p);
						break;
					case unsubscribe:
						break;
					}
				} catch (final Exception e) {
					// SMACK silently discards exceptions dropped from processPacket :(
					Log.e(TAG, "failed to process presence:");
					e.printStackTrace();
				}
			}
		};

		mXMPPConnection.addPacketListener(mPresenceListener, new PacketTypeFilter(Presence.class));
	}

	private boolean addChatMessageToDB(final int direction, final String JID,
			final String message, final int delivery_status, final long ts, final String packetID, final String sender, final RoomsMessageType type, final String extraData) {
		final ContentValues values = new ContentValues();
		values.put(ChatConstants.DIRECTION, direction);
		values.put(ChatConstants.JID, JID);
		values.put(ChatConstants.MESSAGE, message);
		values.put(ChatConstants.DELIVERY_STATUS, delivery_status);
		values.put(ChatConstants.DATE, ts);
		values.put(ChatConstants.PACKET_ID, packetID);
		values.put(ChatConstants.SENDER, sender);
		values.put(ChatConstants.TYPE, type.ordinal());
		values.put(ChatConstants.EXTRA_DATA, extraData);

		if (mContentResolver.update(ChatProvider.CONTENT_URI, values,
				ChatConstants.PACKET_ID + " = ?", new String[] { packetID }) == 0) {
			mContentResolver.insert(ChatProvider.CONTENT_URI, values);
			return true;
		}
		return false;
	}

	private ContentValues getContentValuesForRosterEntry(final RosterEntry entry) {
		final ContentValues values = new ContentValues();

		values.put(RosterConstants.JID, entry.getUser());
		values.put(RosterConstants.ALIAS, getName(entry));

		final Presence presence = mRoster.getPresence(entry.getUser());
		values.put(RosterConstants.STATUS_MODE, getStatusInt(presence));
		values.put(RosterConstants.STATUS_MESSAGE, presence.getStatus());
		values.put(RosterConstants.GROUP, getGroup(entry.getGroups()));

		return values;
	}

	private void deleteRosterEntryFromDB(final String jabberID) {
		final int count = mContentResolver.delete(RosterProvider.CONTENT_URI,
				RosterConstants.JID + " = ?", new String[] { jabberID });
		debugLog("deleteRosterEntryFromDB: Deleted " + count + " entries");
	}

	private void updateRosterEntryInDB(final RosterEntry entry) {
		upsertRoster(getContentValuesForRosterEntry(entry), entry.getUser());
	}

	private void updateRosterEntry(final RosterEntry entry) {
		mContentResolver.update(RosterProvider.CONTENT_URI, getContentValuesForRosterEntry(entry),
				RosterConstants.JID + " = ?", new String[] { entry.getUser() });
	}

	private void upsertRoster(final ContentValues values, final String jid) {
		if (mContentResolver.update(RosterProvider.CONTENT_URI, values,
				RosterConstants.JID + " = ?", new String[] { jid }) == 0) {
			mContentResolver.insert(RosterProvider.CONTENT_URI, values);
		}
	}

	private void upsertRoom(final ContentValues values, final String roomID) {
		if (mContentResolver.update(RosterProvider.ROOMS_URI, values,
				RoomsConstants.ID + " = ?", new String[] { roomID }) == 0) {
			mContentResolver.insert(RosterProvider.ROOMS_URI, values);
		}
	}
	
	private void upsertParticipants(final String roomID, final List<Participant> participants) {
		mContentResolver.delete(RosterProvider.PARTICIPANTS_URI, 
				ParticipantConstants.ROOM + " = ?", new String[] { roomID });
		final ContentValues values = new ContentValues();
		values.put(ParticipantConstants.ROOM, roomID);
		for (final Participant p : participants) {
			values.put(ParticipantConstants.JID, p.getJid());
			values.put(ParticipantConstants.NAME, p.getName());
			mContentResolver.insert(RosterProvider.PARTICIPANTS_URI, values);
		}
	}
	
	private List<Participant> getRoomParticipants(final String roomID) {  
		final Cursor c = mContentResolver.query(RosterProvider.PARTICIPANTS_URI, 
				ParticipantConstants.getRequiredColumns().toArray(new String[] {}), 
				ParticipantConstants.ROOM + " = ?", new String[] { roomID }, null);
		final List<Participant> participants = new ArrayList<Participant>();
		while (c.moveToNext()) {
			final String jid = c.getString(c.getColumnIndex(ParticipantConstants.JID));
			final String name = c.getString(c.getColumnIndex(ParticipantConstants.NAME));
			final Participant p = new Participant(jid, name, null);
			final Cursor c2 = mContentResolver.query(RosterProvider.KEYS_URI, 
					new String[] { KeysConstants.RESOURCE, KeysConstants.PUBLIC_KEY }, 
					KeysConstants.JID + " = ? AND " + KeysConstants.RESOURCE + " is not null", 
					new String[] { jid }, null);
			while (c2.moveToNext()) {
				final String resource = c2.getString(c2.getColumnIndex(KeysConstants.RESOURCE));
				final String publicKey = c2.getString(c2.getColumnIndex(KeysConstants.PUBLIC_KEY));
				p.addIdentity(resource, publicKey, IdentityType.Mobile);
			}
			participants.add(p);
		}
		return participants;
	}

	private String getGroup(final Collection<RosterGroup> groups) {
		for (final RosterGroup group : groups) {
			return group.getName();
		}
		return "";
	}

	private String getName(final RosterEntry rosterEntry) {
		String name = rosterEntry.getName();
		if (name != null && name.length() > 0) {
			return name;
		}
		name = StringUtils.parseName(rosterEntry.getUser());
		if (name.length() > 0) {
			return name;
		}
		return rosterEntry.getUser();
	}

	private StatusMode getStatus(final Presence presence) {
		if (presence.getType() == Presence.Type.subscribe) {
            return StatusMode.subscribe;
        }
		if (presence.getType() == Presence.Type.available) {
			if (presence.getMode() != null) {
				return StatusMode.valueOf(presence.getMode().name());
			}
			return StatusMode.available;
		}
		return StatusMode.offline;
	}

	private int getStatusInt(final Presence presence) {
		return getStatus(presence).ordinal();
	}

	private void debugLog(final String data) {
		if (LogConstants.LOG_DEBUG) {
			Log.d(TAG, data);
		}
	}

	@Override
	public String getLastError() {
		return mLastError;
	}

	@Override
	public void sendFile(final String jid, final String fileName) {
		// TODO Auto-generated method stub
	}

	@Override
	public String sendControlMessage(final RoomsMessage message) {
		try {
			message.setSender(mConfig.jabberID);
			message.setRecipients(Arrays.asList(KeyAccessor.ROOMS_SERVER));
            final String encryptedMsg = crypto.encryptMessage( message );
			final Message newMessage = new Message(KeyAccessor.ROOMS_SERVER, Message.Type.chat);
			newMessage.setBody(encryptedMsg);
	
			if (isAuthenticated()) {
				addChatMessageToDB(ChatConstants.OUTGOING, KeyAccessor.ROOMS_SERVER, encryptedMsg, ChatConstants.DS_SENT_OR_READ,
					System.currentTimeMillis(), newMessage.getPacketID(), KeyAccessor.ROOMS_SERVER, message.getType(), null);
				mXMPPConnection.sendPacket(newMessage);
			} 
			else {
				// send offline -> store to DB
				addChatMessageToDB(ChatConstants.OUTGOING, KeyAccessor.ROOMS_SERVER, encryptedMsg, ChatConstants.DS_NEW,
						System.currentTimeMillis(), newMessage.getPacketID(), KeyAccessor.ROOMS_SERVER, message.getType(), null);
			}
			return newMessage.getPacketID();
		}
		catch (final Exception ex) {
			Log.e(TAG, "Failed to send control message", ex);
			return null;
		}
	}

	@Override
	public String sendRoomMessage(final String roomID, final RoomsMessage message) {
		try {
			final String msgID = "msg_" + System.currentTimeMillis();
			message.setSender(mConfig.jabberID);
			if (message.getRecipients() == null || message.getRecipients().isEmpty()) {
				final List<String> recipients = new ArrayList<String>();
				for (final Participant p : getRoomParticipants(roomID)) {
					for (final Identity i : p.getIdentities()) {
						recipients.add(p.getJid() + "/" + i.getResource());
					}
				}
				message.setRecipients(recipients);
			}
            message.setRoomId( roomID );
			final String encryptedMsg = crypto.encryptMessage(message);
			final String payload = "<body>" + encryptedMsg + "</body>";
			final LeafNode roomNode = pubSub.getNode(roomID);
			roomNode.send(new PayloadItem<SimplePayload>(msgID, new SimplePayload(null, null, payload)));
			return msgID;
		}
		catch (final Exception ex) {
			Log.e(TAG, "Failed to send control message", ex);
			return null;
		}
	}
}
