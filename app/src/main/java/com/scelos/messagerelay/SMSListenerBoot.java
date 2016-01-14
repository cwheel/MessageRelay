package com.scelos.messagerelay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SMSListenerBoot extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, SMSListener.class);
        context.startService(i);
    }
}
