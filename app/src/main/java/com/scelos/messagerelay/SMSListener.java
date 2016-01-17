package com.scelos.messagerelay;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;

public class SMSListener extends Service {
    final private String app = "com.nosedive25.messagerelay";
    private BroadcastReceiver smsReceived;

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

                    AbstractXMPPConnection adminCon = JabberUtils.adminConnection(prefs);
                    String phoneNum = mssg.getOriginatingAddress();
                    String tmpPass = JabberUtils.phoneUserPassword();

                    JabberUtils.registerUser(adminCon, phoneNum, prefs.getString(app + ".pass" + phoneNum, tmpPass), prefs);

                    AbstractXMPPConnection senderCon = JabberUtils.userConnection(phoneNum, prefs.getString(app + ".pass" + phoneNum, tmpPass), prefs);
                    ChatManager chatmanager = ChatManager.getInstanceFor(senderCon);
                    Chat newChat = chatmanager.createChat(JabberUtils.fullyQualifiedUsername(prefs), new ChatMessageListener() {
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

        ContentResolver contentResolver = getApplicationContext().getContentResolver();
        contentResolver.registerContentObserver(Uri.parse("content://sms"), true, new SMSContentObserver(new Handler(), getApplicationContext()));
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
