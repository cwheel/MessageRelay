package com.scelos.messagerelay;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class JabberConfigActivity extends Activity {

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
    }
}
