package br.com.mblabs;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

public class Notification {

    public static final String CHANNEL_HIGH = "channel_high";
    public static final String CHANNEL_DEFAULT = "channel_default";

    public static final int IMPORTANCE_HIGH = 4;
    public static final int IMPORTANCE_DEFAULT = 3;

    public static void cancelNotification( final Context context, final int id) {
        NotificationManager notificationManager = (NotificationManager) context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(id);
    }

    public static void cancelNotifications( final Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    public static NotificationCompat.Builder getBuilder( final Context context,  final String channel, final int importance) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager.getNotificationChannel(channel) == null) {
            notificationManager.createNotificationChannel(new NotificationChannel(channel, channel, importance));
        }

        return new NotificationCompat.Builder(context.getApplicationContext(), channel);
    }
}
