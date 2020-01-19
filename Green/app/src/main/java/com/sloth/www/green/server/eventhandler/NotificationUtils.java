package com.sloth.www.green.server.eventhandler;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import com.sloth.www.green.R;
import com.sloth.www.green.ui.SettingsActivity;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * 适配 8.0 的通知管理器
 * 发送本地通知
 * created by nigal at 2018/6/19
 */
public class NotificationUtils extends ContextWrapper {

    private NotificationManager manager;
    public static final String id = "channel_1";
    public static final String name = "channel_name_1";

    private Context context;

    public NotificationUtils(Context context) {
        super(context);
        this.context = context;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH);
        getManager().createNotificationChannel(channel);
    }

    private NotificationManager getManager() {
        if (manager == null) {
            manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
        return manager;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Notification.Builder getChannelNotification(String title, String content) {
        Intent intent;
        PendingIntent ma;
        intent = new Intent(context, SettingsActivity.class);
        ma = PendingIntent.getActivity(context, 0, intent, 0);

        return new Notification.Builder(getApplicationContext(), id)
                .setContentIntent(ma)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.mipmap.pig)
                .setAutoCancel(true);
    }

    public NotificationCompat.Builder getNotification_25(String title, String content) {
        Intent intent;
        PendingIntent ma;
        intent = new Intent(context, SettingsActivity.class);
        ma = PendingIntent.getActivity(context, 0, intent, 0);

        return new NotificationCompat.Builder(getApplicationContext())
                .setContentIntent(ma)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.mipmap.pig)
                .setAutoCancel(true);
    }

    public void sendNotification(String title, String content) {
        if (Build.VERSION.SDK_INT >= 26) {
            createNotificationChannel();
            Intent intent;
            PendingIntent ma;
            intent = new Intent(context, SettingsActivity.class);
            ma = PendingIntent.getActivity(context, 0, intent, 0);

            Notification notification = getChannelNotification
                    (title, content)
                    .setContentIntent(ma)
                    .build();

            getManager().notify(1, notification);
        } else {
            Notification notification = getNotification_25(title, content).build();
            getManager().notify(1, notification);
        }
    }

    public Notification prepareNotification(String title, String content) {
        if (Build.VERSION.SDK_INT >= 26) {
            createNotificationChannel();

            Notification notification = getChannelNotification
                    (title, content)
                    .build();

            return notification;
        } else {
            return getNotification_25(title, content).build();
        }
    }

    private static final String CHECK_OP_NO_THROW = "checkOpNoThrow";
    private static final String OP_POST_NOTIFICATION = "OP_POST_NOTIFICATION";

    public static boolean isNotificationEnabled(Context context) {

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            return true;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

}