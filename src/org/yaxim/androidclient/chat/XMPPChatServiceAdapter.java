package org.yaxim.androidclient.chat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

import org.json.JSONObject;
import org.yaxim.androidclient.YaximApplication;
import org.yaxim.androidclient.crypto.Crypto;
import org.yaxim.androidclient.service.IXMPPChatService;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.RemoteException;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.ResponseHeaderOverrides;

public class XMPPChatServiceAdapter {

	private static final String TAG = "yaxim.XMPPCSAdapter";
	private static final String AWS_KEY1 = "c879011b7c5df02fa694657dafa1b3ca7f70202b5304a45299ca3f183ec4149012409fed5f3a378a95653a72cac44e86eec9ed61381c4f09e7336f67";
	private static final String AWS_KEY2 = "d076901f824ecf6f0351868eca56594d6f6e0c6089af5fadbe48ac95340561b1b60385cdb890af644946ecc3a5a7d289c7830af030cac4cb41c0f24badf3ea4ef5d7b627602fb0b239bd7e221791affa";
	private final IXMPPChatService xmppServiceStub;
	private final String jabberID;

	public XMPPChatServiceAdapter(final IXMPPChatService xmppServiceStub,
			final String jabberID) {
		Log.i(TAG, "New XMPPChatServiceAdapter construced");
		this.xmppServiceStub = xmppServiceStub;
		this.jabberID = jabberID;
	}

	public void sendMessage(final String user, final String message) {
		try {
			Log.i(TAG, "Called sendMessage(): " + jabberID + ": " + message);
			xmppServiceStub.sendMessage(user, message);
		} catch (final RemoteException e) {
			Log.e(TAG, "caught RemoteException: " + e.getMessage());
		}
	}
	
	public boolean isServiceAuthenticated() {
		try {
			return xmppServiceStub.isAuthenticated();
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void clearNotifications(final String Jid) {
		try {
			xmppServiceStub.clearNotifications(Jid);
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
	}

	public void sendFile(final String jabberID, final String fileName, final long size, final String key, final String url, final String mimeType) {
		try {
			xmppServiceStub.sendFile(jabberID, fileName, size, key, url, mimeType);
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
	}

	public void openRoom(final String parentRoomID, final String topic, final Collection<String> participantJids) {
		try {
			xmppServiceStub.openRoom(parentRoomID, topic, participantJids.toArray(new String[] {}));
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
	}

	public void sendTaskResponse(final String selectedOption) {
		try {
			xmppServiceStub.sendTaskResponse(selectedOption);
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
	}

	public void sendTask(final String roomID, final String text, final String recipient) {
		try {
			xmppServiceStub.sendTask(roomID, text, recipient);
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
	}
	
	public String downloadFile(final JSONObject fileInfo, final Context context) throws Exception {
		InputStream input = null;
        ByteArrayOutputStream output = null;
        HttpURLConnection connection = null;
        final Crypto crypto = YaximApplication.getApp(context).mCrypto;
        try {
            final String downloadLink = fileInfo.getString("download-link");
            final String filename = fileInfo.getString("filename");
            final String encryptionKey = fileInfo.getString("key");
            final String mimeType = fileInfo.getString("mime-type");
            final URL url = new URL(downloadLink);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new Exception("Server returned HTTP " + connection.getResponseCode()
                        + " " + connection.getResponseMessage());
            }

            input = connection.getInputStream();
            output = new ByteArrayOutputStream();
            final byte data[] = new byte[4096];
            int count;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }
            final File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            final String fullName = dir.getAbsolutePath() + File.separator + filename;
            final FileOutputStream decryptedFile = new FileOutputStream(fullName);
            crypto.decryptStream(new ByteArrayInputStream(output.toByteArray()), decryptedFile, encryptionKey);
            return fullName;
        } 
        catch (final Exception e) {
            throw e;
        } 
        finally {
            try {
                if (output != null) {
                    output.close();
                }
                if (input != null) {
                    input.close();
                }
            } 
            catch (final IOException ignored) {
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
	}
	
	public String uploadFile(final Context context, final String to, final String selectedFile) throws Exception {
        final Crypto crypto = YaximApplication.getApp(context).mCrypto;
        final String key1 = crypto.decryptSymmetrically(AWS_KEY1, TAG);
        final String key2 = crypto.decryptSymmetrically(AWS_KEY2, TAG);
		final File file = new File(selectedFile);
		
		final FileInputStream in = new FileInputStream(selectedFile);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final String key = crypto.generateSymmetricKey();
		final String fileID = UUID.randomUUID().toString();

		crypto.encryptStream(in, out, key);
		
		final AmazonS3Client s3Client = new AmazonS3Client(new BasicAWSCredentials(key1, key2));
		final ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(out.size());

		final String mimeType = getMimeTypeFromFile(file, null);

		metadata.setContentType(mimeType);
		final PutObjectRequest por = new PutObjectRequest("encrypted-file-storage", fileID, new ByteArrayInputStream(out.toByteArray()), metadata); 
		s3Client.putObject(por);
		
		final ResponseHeaderOverrides override = new ResponseHeaderOverrides();
		override.setContentType(mimeType);
		final GeneratePresignedUrlRequest urlRequest = new GeneratePresignedUrlRequest("encrypted-file-storage", fileID);
		urlRequest.setExpiration(new Date(System.currentTimeMillis() + 360000000));
		urlRequest.setResponseHeaders(override);
		final URL url = s3Client.generatePresignedUrl(urlRequest);
		
		sendFile(to, file.getName(), file.length(), key, url.toString(), mimeType);
		return url.toString();
	}
	
	private String getMimeTypeFromFile(final File file, final String fallback) {
		final Uri uri = Uri.fromFile(file);
		final MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
		final String ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
		String mime = mimeTypeMap.getMimeTypeFromExtension(ext);
		
		if (mime == null) {
			mime = fallback;
		}
		
		return mime;
	}

    public void kickParticipant( final String mWithJabberID, final String selectedParticipant ) {

        try {
            xmppServiceStub.kickParticipant( mWithJabberID, selectedParticipant );
        } catch ( final RemoteException e ) {
            e.printStackTrace();
        }
    }

    public void inviteParticipant( final String mWithJabberID, final String selectedParticipant ) {

        try {
            xmppServiceStub.inviteParticipant( mWithJabberID, selectedParticipant );
        } catch ( final RemoteException e ) {
            e.printStackTrace();
        }
    }
}
