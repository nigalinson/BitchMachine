package com.sloth.www.green.pipe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class GreenServerReceiver extends BroadcastReceiver {

    private Callback callback;

    public GreenServerReceiver(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(callback != null){
            callback.requestState();
        }
    }

    interface Callback{
        void requestState();
    }

}
