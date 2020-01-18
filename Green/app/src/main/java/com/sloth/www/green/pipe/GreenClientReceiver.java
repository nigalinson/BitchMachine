package com.sloth.www.green.pipe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class GreenClientReceiver extends BroadcastReceiver {

    private Callback callback;

    public GreenClientReceiver(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        callback.state(intent.getBooleanExtra("opening", false));
    }

    public interface Callback{
        void state(boolean opening);
    }

}
