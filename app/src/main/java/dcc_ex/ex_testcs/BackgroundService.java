package dcc_ex.ex_testcs;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class BackgroundService  extends Service {
    private NotificationManager mNM;

    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = R.string.background_service_started;

    public class LocalBinder extends Binder {
        BackgroundService getService() {
            return BackgroundService.this;
        }
    }

    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, R.string.background_service_started,Toast.LENGTH_SHORT).show();
        Log.d("EX-TestCS", "BackgroundService: onStartCommand: Received start id " + startId + ": " + intent);
//        showNotification();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(NOTIFICATION);

        Toast.makeText(this, R.string.background_service_stopped, Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IBinder mBinder = new LocalBinder();

//    private void showNotification() {
//        Log.d("EX-TestCS", "showNotification()");
//
//        // The PendingIntent to launch our activity if the user selects this notification
//        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
//                new Intent(this, ex_testcs.class), PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
//
//        // Set the info for the views that show in the notification panel.
//        Notification notification = new Notification.Builder(this)
//                .setSmallIcon(R.drawable.ic_launcher_small)  // the status icon
//                .setTicker(getText(R.string.background_service_started))  // the status text
//                .setWhen(System.currentTimeMillis())  // the time stamp
//                .setContentTitle(getText(R.string.background_service_label))  // the label of the entry
//                .setContentText(getText(R.string.background_service_started))  // the contents of the entry
//                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
//                .build();
//
//        // Send the notification.
//        mNM.notify(NOTIFICATION, notification);
//    }
}