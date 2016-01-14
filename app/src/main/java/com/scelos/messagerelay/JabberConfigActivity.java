package com.scelos.messagerelay;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

public class JabberConfigActivity extends Activity {
    final private String app = "com.nosedive25.messagerelay";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.jabber_config_activity);

        final String app = "com.nosedive25.messagerelay";
        final SharedPreferences prefs = this.getSharedPreferences(app, Context.MODE_PRIVATE);

        final EditText server = (EditText)findViewById(R.id.server);
        final EditText username = (EditText)findViewById(R.id.username);
        final EditText password = (EditText)findViewById(R.id.password);

        server.setText(prefs.getString(app + ".server", ""));
        username.setText(prefs.getString(app + ".username", ""));
        password.setText(prefs.getString(app + ".password", ""));

        Button save = (Button)findViewById(R.id.save);
        save.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                prefs.edit().putString(app + ".server", server.getText().toString()).apply();
                prefs.edit().putString(app + ".username", username.getText().toString()).apply();
                prefs.edit().putString(app + ".password", password.getText().toString()).apply();
            }
        });

        Intent i = new Intent(this, SMSListener.class);
        this.startService(i);
    }
}