package com.example.uniconseat;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class BackUpService extends Service {
    public BackUpService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }
    @Override//创建服务的时候调用
    public void onCreate(){
        super.onCreate();
        Log.d("myservice","onCreate executed");
        Intent intent = new Intent(this, GrabSeatActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("好帮手")
                .setContentText("后台监控中...")
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.monitor)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setPriority(Notification.PRIORITY_HIGH)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.drawable.monitorlarge))
                .setFullScreenIntent(PendingIntent.getActivities(BackUpService.this,0,
                        new Intent[]{new Intent(BackUpService.this, GrabSeatActivity.class)},
                        PendingIntent.FLAG_CANCEL_CURRENT),false)
                .setContentIntent(pi)
                .build();
        startForeground(10,notification);
    }
    @Override//每次服务启动的时候调用
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.e("启动","后台监控座位服务已启动");


        return super.onStartCommand(intent,flags,startId);
    }
    @Override//服务销毁的时候调用
    public void  onDestroy(){
        super.onDestroy();
        Log.e("onDestroy","后台监控座位服务已销毁");
    }

}
