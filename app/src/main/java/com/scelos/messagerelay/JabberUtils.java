package com.scelos.messagerelay;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.StrictMode;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

public class JabberUtils {
    final static private String app = "com.nosedive25.messagerelay";

    public static void registerUser(AbstractXMPPConnection con, String username, String password, SharedPreferences prefs) {
        org.jivesoftware.smackx.iqregister.AccountManager am = org.jivesoftware.smackx.iqregister.AccountManager.getInstance(con);

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

    public static AbstractXMPPConnection adminConnection(SharedPreferences prefs) {
        return userConnection(prefs.getString(app + ".username", ""), prefs.getString(app + ".password", ""), prefs);
    }

    public static String fullyQualifiedUsername(SharedPreferences prefs) {
        return prefs.getString(app + ".username", "") + "@" + prefs.getString(app + ".server", "");
    }

    public static String phoneUserPassword() {
        SecureRandom random = new SecureRandom();
        return new BigInteger(130, random).toString(32);
    }

    public static AbstractXMPPConnection userConnection(String username, String password, SharedPreferences prefs) {
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
}
