package com.sloth.www.green.client.impl;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.google.gson.Gson;
import com.sloth.www.green.client.IGreenHelper;
import com.sloth.www.green.server.GreenConfig;
import com.sloth.www.green.server.monitor.GreenService;
import java.util.HashMap;
import java.util.Map;

public class GreenHelper extends GreenReceiverHelper implements IGreenHelper {

    private static final String TAG = "GreenHelper";

    @Override
    public void openAndCloseService(Context context) {
        if(context != null){
            Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            context.startActivity(intent);
        }
    }

    @Override
    public void resume(Context context) {
        send(context, GreenConfig.FUNCTIONS.RUNNING, true);
    }

    @Override
    public void pause(Context context) {
        send(context, GreenConfig.FUNCTIONS.RUNNING, false);
    }

    @Override
    public void voiceAlarm(Context context, boolean open) {
        send(context, GreenConfig.FUNCTIONS.VOICE, open);
    }

    @Override
    public void voiceAlarm(Context context, String attentionMsg, String successMsg, String failedMsg) {
        Map<String, String> map = new HashMap<>();
        if(!TextUtils.isEmpty(attentionMsg)){
            map.put(GreenConfig.FUNCTIONS.VOICE_CONTENT_1, attentionMsg);
        }
        if(!TextUtils.isEmpty(successMsg)){
            map.put(GreenConfig.FUNCTIONS.VOICE_CONTENT_2, successMsg);
        }
        if(!TextUtils.isEmpty(failedMsg)){
            map.put(GreenConfig.FUNCTIONS.VOICE_CONTENT_3, failedMsg);
        }
        send(context, GreenConfig.FUNCTIONS.VOICE_CONTENT, new Gson().toJson(map));
    }

    @Override
    public void delay(Context context, long delay) {
        send(context, GreenConfig.FUNCTIONS.DELAY, delay);
    }

    @Override
    public void autoRollBackToChatList(Context context, boolean auto) {
        send(context, GreenConfig.FUNCTIONS.ROLL_BACK, auto);
    }

    @Override
    public boolean isServiceRunning(Context context) {
        int accessibilityEnabled = 0;
        final String service = context.getPackageName()  + "/" + GreenService.class.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    context.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
            Log.v(TAG, "accessibilityEnabled = " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Error finding setting, default accessibility to not found: "
                    + e.getMessage());
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            Log.v(TAG, "***ACCESSIBILITY IS ENABLED*** -----------------");
            String settingValue = Settings.Secure.getString(
                    context.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();

                    Log.v(TAG, "-------------- > accessibilityService :: " + accessibilityService + " " + service);
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        Log.v(TAG, "We've found the correct setting - accessibility is switched on!");
                        return true;
                    }
                }
            }
        } else {
            Log.v(TAG, "***ACCESSIBILITY IS DISABLED***");
        }
        return false;
    }

    @Override
    public void register(Context context) {
        registerClientReceiver(context);
    }

    @Override
    public void unRegister(Context context) {
        unRegisterClientReceiver(context);
    }
}
