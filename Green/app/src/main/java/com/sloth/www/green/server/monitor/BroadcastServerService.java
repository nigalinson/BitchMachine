package com.sloth.www.green.server.monitor;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.sloth.www.green.server.GreenConfig;

public abstract class BroadcastServerService extends AccessibilityService {

    GreenServerReceiver receiver = new GreenServerReceiver();

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    void registerReceiver() {
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(GreenConfig.ACTION_SERVER);
        registerReceiver(receiver, iFilter);
    }


    public class GreenServerReceiver extends BroadcastReceiver {

        public GreenServerReceiver() { }

        @Override
        public void onReceive(Context context, Intent intent) {
            String key = intent.getStringExtra("key");
            achieveBroadcast(key, intent);
        }
    }

    protected abstract void achieveBroadcast(String key, Intent intent);

}
