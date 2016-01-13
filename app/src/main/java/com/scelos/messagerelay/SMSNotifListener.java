package com.scelos.messagerelay;

import java.math.BigInteger;
import java.security.SecureRandom;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.StrictMode;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.telephony.SmsManager;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

import java.io.IOException;

public class SMSNotifListener extends NotificationListenerService {

    Context context;
    final String app = "com.nosedive25.messagerelay";

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        SharedPreferences prefs = this.getSharedPreferences(app, Context.MODE_PRIVATE);

        if (!prefs.getString(app + ".server", "").equals("")
                && !prefs.getString(app + ".username", "").equals("")
                && !prefs.getString(app + ".password", "").equals("")
                && sbn.getPackageName().equals("com.google.android.apps.messaging")) {

            Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);

            if (cursor.moveToFirst()) {
                AbstractXMPPConnection adminCon = adminConnection();
                String phoneNum = cursor.getString(2);
                String tmpPass = phoneUserPassword();

                registerUser(adminCon, phoneNum, prefs.getString(app + ".pass" + phoneNum, tmpPass));

                AbstractXMPPConnection senderCon = userConnection(phoneNum, prefs.getString(app + ".pass" + phoneNum, tmpPass));
                ChatManager chatmanager = ChatManager.getInstanceFor(senderCon);
                Chat newChat = chatmanager.createChat(fullyQualifiedUsername(), new ChatMessageListener() {
                    @Override
                    public void processMessage(Chat chat, Message message) {
                        SmsManager sms = SmsManager.getDefault();
                        sms.sendTextMessage(message.getTo().split("@")[0], null, message.getBody(), null, null);
                    }
                });

                try {
                    newChat.sendMessage(cursor.getString(12));
                }
                catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                }

                adminCon.disconnect();
            }

            cursor.close();
        }
    }

    private void registerUser(AbstractXMPPConnection con, String username, String password) {
        org.jivesoftware.smackx.iqregister.AccountManager am = org.jivesoftware.smackx.iqregister.AccountManager.getInstance(con);
        SharedPreferences prefs = this.getSharedPreferences(app, Context.MODE_PRIVATE);

        prefs.edit().putString(app + ".pass" + username, password).apply();

        try {
            am.sensitiveOperationOverInsecureConnection(true);
            am.createAccount(username, password);
        } catch (XMPPException e) {
            //System.out.println("User probably already exists, shouldn't be an issue.");
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        }
    }

    private AbstractXMPPConnection adminConnection() {
        SharedPreferences prefs = this.getSharedPreferences(app, Context.MODE_PRIVATE);
        return userConnection(prefs.getString(app + ".server", ""), prefs.getString(app + ".password", ""));
    }

    private String fullyQualifiedUsername() {
        SharedPreferences prefs = this.getSharedPreferences(app, Context.MODE_PRIVATE);
        return prefs.getString(app + ".username", "") + "@" + prefs.getString(app + ".server", "");
    }

    private String phoneUserPassword() {
        SecureRandom random = new SecureRandom();
        return new BigInteger(130, random).toString(32);
    }

    private AbstractXMPPConnection userConnection(String username, String password) {
        SharedPreferences prefs = this.getSharedPreferences(app, Context.MODE_PRIVATE);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        XMPPTCPConnectionConfiguration.Builder config = XMPPTCPConnectionConfiguration.builder();
        config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);

        config.setHost(prefs.getString(app + ".server", ""));
        config.setServiceName(prefs.getString(app + ".server", ""));
        config.setSendPresence(true);
        config.setDebuggerEnabled(true);

        AbstractXMPPConnection con = new XMPPTCPConnection(config.build());

        try {
            con.connect();
            con.login(username, password);
        } catch (SmackException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XMPPException e) {
            e.printStackTrace();
        }

        return con;
    }
}
