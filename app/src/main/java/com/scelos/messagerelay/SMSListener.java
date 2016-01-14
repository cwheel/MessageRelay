package com.scelos.messagerelay;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

public class SMSListener extends Service {
    private BroadcastReceiver smsReceived;
    final private String app = "com.nosedive25.messagerelay";

    @Override
    public void onCreate() {
        smsReceived = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SharedPreferences prefs = getApplicationContext().getSharedPreferences(app, Context.MODE_PRIVATE);

                Bundle mssgBundle = intent.getExtras();
                Object[] pdus = (Object[])mssgBundle.get("pdus");

                if (!prefs.getString(app + ".server", "").equals("") && !prefs.getString(app + ".username", "").equals("") && !prefs.getString(app + ".password", "").equals("")) {
                    SmsMessage mssg = SmsMessage.createFromPdu((byte[]) pdus[0]);

                    AbstractXMPPConnection adminCon = adminConnection();
                    String phoneNum = mssg.getOriginatingAddress();
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
                        newChat.sendMessage(mssg.getMessageBody());
                    }
                    catch (SmackException.NotConnectedException e) {
                        e.printStackTrace();
                    }

                    adminCon.disconnect();
                }
            }
        };

        IntentFilter recFilter = new IntentFilter();
        recFilter.addAction("android.provider.Telephony.SMS_RECEIVED");

        registerReceiver(smsReceived, recFilter);
    }

    private void registerUser(AbstractXMPPConnection con, String username, String password) {
        org.jivesoftware.smackx.iqregister.AccountManager am = org.jivesoftware.smackx.iqregister.AccountManager.getInstance(con);
        SharedPreferences prefs = this.getSharedPreferences(app, Context.MODE_PRIVATE);

        prefs.edit().putString(app + ".pass" + username, password).apply();

        try {
            am.sensitiveOperationOverInsecureConnection(true);
            am.createAccount(username, password);
        } catch (XMPPException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        }
    }

    private AbstractXMPPConnection adminConnection() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(app, Context.MODE_PRIVATE);
        return userConnection(prefs.getString(app + ".username", ""), prefs.getString(app + ".password", ""));
    }

    private String fullyQualifiedUsername() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(app, Context.MODE_PRIVATE);
        return prefs.getString(app + ".username", "") + "@" + prefs.getString(app + ".server", "");
    }

    private String phoneUserPassword() {
        SecureRandom random = new SecureRandom();
        return new BigInteger(130, random).toString(32);
    }

    private AbstractXMPPConnection userConnection(String username, String password) {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(app, Context.MODE_PRIVATE);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        XMPPTCPConnectionConfiguration.Builder config = XMPPTCPConnectionConfiguration.builder();
        config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);

        config.setHost(prefs.getString(app + ".server", ""));
        config.setServiceName(prefs.getString(app + ".server", ""));
        config.setSendPresence(true);
        config.setDebuggerEnabled(true);

        AbstractXMPPConnection con = new XMPPTCPConnection(config.build());
        Presence pres = new Presence(Presence.Type.available);
        pres.setMode(Presence.Mode.available);

        try {
            con.connect();
            con.login(username, password);

            con.sendStanza(pres);
        } catch (SmackException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XMPPException e) {
            e.printStackTrace();
        }

        return con;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(smsReceived);
    }
}
