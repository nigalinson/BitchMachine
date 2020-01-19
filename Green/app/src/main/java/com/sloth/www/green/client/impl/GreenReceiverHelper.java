package com.sloth.www.green.client.impl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.sloth.www.green.server.GreenConfig;
import com.sloth.www.green.server.utils.BroadcastUtils;

import androidx.annotation.Nullable;

public class GreenReceiverHelper {

    public class GreenClientReceiver extends BroadcastReceiver {

        public GreenClientReceiver() {

        }

        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }

    private GreenClientReceiver receiver = new GreenClientReceiver();

    void registerClientReceiver(Context context){
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(GreenConfig.ACTION_CLIENT);
        context.registerReceiver(receiver, iFilter);
    }

    void unRegisterClientReceiver(Context context){
        context.unregisterReceiver(receiver);
    }

    void send(Context context, String key, @Nullable Object value){
        if(context == null){
            return;
        }

        BroadcastUtils.send(context, GreenConfig.ACTION_SERVER, key, value);
    }

}
