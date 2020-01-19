package com.sloth.www.green.server.utils;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.Nullable;

public class BroadcastUtils {

    public static void send(Context context, String action, String key, @Nullable Object value){
        Intent it = new Intent(action);
        it.putExtra("key", key);

        if(value != null){
            if(value instanceof String){
                it.putExtra("value", (String)value);
            }else if(value instanceof Integer){
                it.putExtra("value", (Integer)value);
            }else if(value instanceof Long){
                it.putExtra("value", (Long)value);
            }else if(value instanceof Boolean){
                it.putExtra("value", (Boolean)value);
            }else{
                throw new RuntimeException("暂不支持该格式");
            }
        }

        context.sendBroadcast(it);
    }

    public static String key(Intent intent){
        return intent.getStringExtra("key");
    }

    public static String valueString(Intent intent){
        return intent.getStringExtra("value");
    }

    public static int valuInt(Intent intent){
        return intent.getIntExtra("value", -1);
    }

    public static long valueLong(Intent intent){
        return intent.getLongExtra("value", -1L);
    }

    public static boolean valueBoolean(Intent intent){
        return intent.getBooleanExtra("value", false);
    }
}
