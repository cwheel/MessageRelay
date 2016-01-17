package com.scelos.messagerelay;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.carbons.CarbonManager;

public class SMSContentObserver extends ContentObserver {
    Context c;
    String lastBody;

    final private String app = "com.nosedive25.messagerelay";

    public SMSContentObserver(Handler handler, Context con) {
        super(handler);

        c = con;
        lastBody = "";
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);

        Cursor cur = c.getContentResolver().query(Uri.parse("content://sms"), null, null, null, null);
        cur.moveToNext();

        if (cur.getInt(cur.getColumnIndex("type")) == 2 && !lastBody.equals(cur.getString(cur.getColumnIndex("body")))) {
            SharedPreferences prefs = c.getSharedPreferences(app, Context.MODE_PRIVATE);
            String sender = cur.getString(cur.getColumnIndex("address")).replace("(", "").replace(")","").replace(" ","").replace("-", "");

            lastBody = cur.getString(cur.getColumnIndex("body"));

            AbstractXMPPConnection adminCon = JabberUtils.adminConnection(prefs);
            String tmpPass = JabberUtils.phoneUserPassword();

            System.out.println(prefs.getString(app + ".pass" + sender, tmpPass));

            JabberUtils.registerUser(adminCon, sender, prefs.getString(app + ".pass" + sender, tmpPass), prefs);

            ChatManager chatmanager = ChatManager.getInstanceFor(adminCon);
            CarbonManager cm = CarbonManager.getInstanceFor(adminCon);

            try {
               if (cm.isSupportedByServer()) {
                   cm.enableCarbons();
                   cm.sendCarbonsEnabled(true);
                   cm.setCarbonsEnabled(true);
               }
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
            } catch (XMPPException.XMPPErrorException e) {
                e.printStackTrace();
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            } catch (SmackException e) {
                e.printStackTrace();
            } catch (XMPPException e) {
                e.printStackTrace();
            }

            Chat newChat = chatmanager.createChat(sender + "@" + prefs.getString(app + ".server", ""), new ChatMessageListener() {
                @Override
                public void processMessage(Chat chat, Message message) {

                }
            });

            try {
                newChat.sendMessage(lastBody);
            }
            catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            }
        }

        cur.close();
    }
}
