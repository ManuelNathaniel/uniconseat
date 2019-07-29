package com.example.uniconseat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

//通知的id段 20-29
//已用 Creat 20;  Start 22;   网络异常 29；    选座成功 28； 查找空闲座位 23；
public class TimingTasks extends Service {
    /*全局通用信息*/
    static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static String Url = "http://202.206.242.87/ClientWeb/xcus/ic2/Default.aspx";
    static String targetTime = CommonFunction.systemDelayTime(0,2) + " " + "16:29:55";//目标时间16:25:00
    static Date targetDate = null;
    static {
        try {
            targetDate = dateFormat.parse(targetTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }//date格式的启动时间
    static Date webTargetDate;
    static {
        try {
            webTargetDate = dateFormat.parse(CommonFunction.systemDelayTime(0,2)+ " " + "16:30:00");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    static Date webDate;
    static Date webTime;
    static Date defDate;
    static {
        try {
            defDate = dateFormat.parse("1970-07-01 01:00:00");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }//date格式的判错时间
    static String TAG;
    static int Id;
    static int Length;
    static String[] re = new String[4];//用户信誉信息：姓名、班级、信誉积分、受限时间

    /* 读取到的用户输入信息 */
    public String[] user1 = new String[9];//保存用户信息
    public String[] user2 = new String[9];
    public String user1Name,user1Id,user1Password,user1Room,user1RoomId,user1PrefSeat,user1StartTime,user1EndTime,user1UrlEnd;//用户信息
    public String user2Name,user2Id,user2Password,user2Room,user2RoomId,user2PrefSeat,user2StartTime,user2EndTime,user2UrlEnd;
    public String userRoom,userRoomId,userStartTime,userEndTime;
    public boolean user1CanSelect = false; boolean user2CanSelect = false;//如果用户信息不为空则进行选座
    public boolean user1s = false; boolean user1e = false; boolean user1Accept = false;
    public boolean user2s = false; boolean user2e = false; boolean user2Accept = false;
    public boolean creditScore = true;
    public String[] candidateArray = new String[4];

    /* 系统需要返回给用户的结果信息 */
    private static String ToastText = "";//预约状态提示信息
    private static String finalStartTime,finalEndTime,finalStateDevname; //最终座位状态信息
    private static String[] emptyTitle = new String[500];  //空闲座位信息
    private static String[] emptyDevid = new String[500];

    /* 操作过程中一些状态信息（标志） */
    private boolean reserveGoing = false;
    public int actok = 0;//表示操作的结果状态，分为5种
    public int prefSeatState=0;//预约指定座位的结果。0：可以进行预约指定座位；1：用户没有指定座位/指定座位输入错误不存在/预约失败
    private static int empty_num = 0; //空闲座位数量
    private int succeed;
    public boolean sessionNull = false;//会话为空时表示登录失败
    public boolean reserveUser1;
    String errorCode;//错误代码

    static boolean TimingTasksCreate = false;//服务是否启动
    static boolean TimingTasksCommandRunning = false;//onStartCommand是否在运行
    static boolean isEndingTiming = false;//是否可以结束该任务
    /* 获取的一些权限信息 */
    Vibrator vibrator;
    //电源锁，解决锁屏Service不执行
    private PowerManager.WakeLock wakeLock = null;


    //url共用字段
    String urlCommonFirstPara = "http://202.206.242.87/ClientWeb/pro/ajax/device.aspx?byType=devcls&classkind=8&display=fp&md=d&room_id=";
    String urlCommonSecondPara = "&purpose=&selectOpenAty=&cld_name=default&date=";
    String urlCommonThirdPara = "&act=get_rsv_sta&_=";
/*--------------------------------------------------------------------------------------------------------------------------------------
                                                                  重写函数
--------------------------------------------------------------------------------------------------------------------------------------*/
    public TimingTasks() {
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
//        return mBinder;
        return  null;
    }
    @Override//创建服务的时候调用
    public void onCreate(){
        TimingTasksCreate = true;//服务启动
        Log.e("TimingTask","onCreate executed");
        //电源锁
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TipsJumpService.class.getName());//PARTIAL_WAKE_LOCK
        wakeLock.acquire(25 * 60 *1000);
        super.onCreate();

        Intent intent = new Intent(this, TimingTasksActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel("TimingTasks", "定时任务服务", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(mChannel);
            notification = new Notification.Builder(this)
                    .setChannelId("TimingTasks")
                    .setContentTitle("TimingTasks")
                    .setContentText("定时任务运行中...")
                    .setWhen(System.currentTimeMillis())
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .setColor(Color.parseColor("#00000000"))
                    .setSmallIcon(R.drawable.clockgreen128)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.time1282))
                    .setFullScreenIntent(PendingIntent.getActivities(getApplicationContext(),0,
                            new Intent[]{new Intent(getApplicationContext(),TimingTasksActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .setContentIntent(pi)
                    .build();
        }else {
            notification = new NotificationCompat.Builder(this)
                    .setContentTitle("TimingTasks")
                    .setContentText("定时任务运行中...")
                    .setWhen(System.currentTimeMillis())
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .setColor(Color.parseColor("#00000000"))
                    .setSmallIcon(R.drawable.clockgreen128)
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.time1282))
                    .setFullScreenIntent(PendingIntent.getActivities(getApplicationContext(),0,
                            new Intent[]{new Intent(getApplicationContext(),TimingTasksActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .setContentIntent(pi)
                    .build();
        }
        startForeground(20,notification);
    }
    @Override//每次服务启动的时候调用
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.e("TimingTasksStart","onStartCommand");
        TimingTasksCommandRunning = true;//onStartCommand在运行
        isEndingTiming = false;
        timingTask();
        return super.onStartCommand(intent,flags,startId);
    }
    @Override//服务销毁的时候调用
    public void  onDestroy(){
        isEndingTiming = false;
        TimingTasksCommandRunning = false;
        TimingTasksCreate = false;
        //释放电源锁
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
        tips();//除预约成功外，所有消息均在销毁服务后提示
        Log.e("YSUonDestroy","定时任务服务已销毁");
        super.onDestroy();
    }
/*--------------------------------------------------------------------------------------------------------------------------------------
                                                                  提示函数
--------------------------------------------------------------------------------------------------------------------------------------*/
    /*显示通知,用户可以手动删除
    /* 样式0： 只悬停、不振动、不跳转  不回状态栏  */
    public void showNotification0(String title, String text, Context context, int id,int length, String channelid,String channelname){
    final NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    //悬停
    Notification notification2;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        NotificationChannel mChannel = new NotificationChannel(channelid, channelname, NotificationManager.IMPORTANCE_LOW);
        manager.createNotificationChannel(mChannel);
        notification2 = new Notification.Builder(context)
                .setChannelId(channelid)
                .setContentTitle(title)
                .setContentText(text)
                .setWhen(System.currentTimeMillis())
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setColor(Color.parseColor("#00000000"))
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.timingtasknotigy)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.drawable.seat))
                .setFullScreenIntent(PendingIntent.getActivities(context,0,
                        new Intent[]{new Intent(context,TimingTasksActivity.class)},
                        PendingIntent.FLAG_CANCEL_CURRENT),false)
                .build();
    }else {
        notification2 = new NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setContentText(text)
                .setWhen(System.currentTimeMillis())
                .setDefaults(Notification.DEFAULT_SOUND)
                .setPriority(Notification.PRIORITY_HIGH)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setColor(Color.parseColor("#00000000"))
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.timingtasknotigy)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.drawable.seat))
                .setFullScreenIntent(PendingIntent.getActivities(context,0,
                        new Intent[]{new Intent(context,TimingTasksActivity.class)},
                        PendingIntent.FLAG_CANCEL_CURRENT),false)
                .build();
    }
    String tag = id + "float";
    manager.notify(tag,id,notification2);
    TAG = tag; Id = id; Length = length;
    new Thread(new Runnable() {
        @Override
        public void run() {
            try { Thread.sleep(Length * 100);//Length秒后悬挂式通知消失
                manager.cancel(TAG, Id);//按tag id 来清除消息
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }).start();
}
    /* 样式1： 悬停、不振动、不跳转   回状态栏 */
    public void showNotification1(String title, String text, Context context, int id,int length,String channelid,String channelname){
        final NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = null;
        Notification notification2 = null;
        //普通
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(channelid, channelname, NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(mChannel);
            notification = new Notification.Builder(context)
                    .setChannelId(channelid)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setColor(Color.parseColor("#00000000"))
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.timingtasknotigy)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.notify7))
                    .build();
        }else {
            notification = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setColor(Color.parseColor("#00000000"))
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.timingtasknotigy)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.notify7))

                    .build();
        }
        String tag = id + "float";
        manager.notify(id,notification);
        //悬停
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(channelid, channelname, NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(mChannel);
            notification2 = new Notification.Builder(context)
                    .setChannelId(channelid)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setColor(Color.parseColor("#000000"))
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.timingtasknotigy)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.notify7))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,TimingTasksActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .build();
        }else {
            notification2 = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setColor(Color.parseColor("#000000"))
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.timingtasknotigy)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.notify7))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,TimingTasksActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .build();
        }
        manager.notify(tag,id,notification2);
        TAG = tag; Id = id; Length = length;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try { Thread.sleep(Length * 100);//Length秒后悬挂式通知消失
                    manager.cancel(TAG, Id);//按tag id 来清除消息
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    /* 样式2： 悬停、振动、不跳转   回状态栏   */
    public void showNotification2(String title, String text, Context context, int id,int length,String channelid,String channelname){
        final NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = null;
        Notification notification2 = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(channelid, channelname, NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(mChannel);
            notification = new Notification.Builder(context)
                    .setChannelId(channelid)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setColor(Color.parseColor("#00000000"))
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.timingtasknotigy)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.notify7))
                    .build();
        }else {
            notification = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setColor(Color.parseColor("#00000000"))
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.timingtasknotigy)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.notify7))

                    .build();
        }
        String tag = id + "float";
        manager.notify(id,notification);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(channelid, channelname, NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(mChannel);
            notification = new Notification.Builder(context)
                    .setChannelId(channelid)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setColor(Color.parseColor("#000000"))
                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)//振动
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.timingtasknotigy)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.notify7))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,TimingTasksActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .build();
        }else {
            notification2 = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setColor(Color.parseColor("#000000"))
                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)//振动
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.timingtasknotigy)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.notify7))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,TimingTasksActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .build();
        }
        manager.notify(tag,id,notification2);
        TAG = tag; Id = id; Length = length;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try { Thread.sleep(Length * 100);//Length秒后悬挂式通知消失
                    manager.cancel(TAG, Id);//按tag id 来清除消息
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    /* 样式3： 悬停、振动、跳转   回状态栏    */
    public void showNotification3(String title, String text, Context context, int id ,int length,String channelid,String channelname){
        final NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = null;
        Notification notification2 = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(channelid, channelname, NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(mChannel);
            notification = new Notification.Builder(context)
                    .setChannelId(channelid)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setColor(Color.parseColor("#00000000"))
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.timingtasknotigy)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.notify7))
                    .setContentIntent(PendingIntent.getActivities(context,0x0001,
                            new Intent[]{new Intent(context,TimingTasksActivity.class)},PendingIntent.FLAG_CANCEL_CURRENT))//跳转
                    .build();
        }else {
            notification = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setColor(Color.parseColor("#00000000"))
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.timingtasknotigy)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.notify7))
                    .setContentIntent(PendingIntent.getActivities(context,0x0001,
                            new Intent[]{new Intent(context,TimingTasksActivity.class)},PendingIntent.FLAG_CANCEL_CURRENT))//跳转
                    .build();
        }
        String tag = id + "float";
        manager.notify(id,notification);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(channelid, channelname, NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(mChannel);
            notification2 = new Notification.Builder(context)
                    .setChannelId(channelid)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setColor(Color.parseColor("#00000000"))
                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)//振动
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.timingtasknotigy)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.notify7))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,TimingTasksActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .build();
        }else {
            notification2 = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setColor(Color.parseColor("#00000000"))
                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)//振动
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.timingtasknotigy)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.notify7))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,TimingTasksActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .build();
        }
        manager.notify(tag,id,notification2);
        TAG = tag; Id = id; Length = length;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try { Thread.sleep(Length * 100);//Lengths后悬挂式通知消失
                    manager.cancel(TAG, Id);//按tag id 来清除消息
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    /* 样式4： 悬挂、振动、跳转    不回状态栏    */
    public void showNotification4(String title, String text, Context context, int id,String channelid,String channelname){
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = null;
        Notification notification2 = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(channelid, channelname, NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(mChannel);
            notification = new Notification.Builder(context)
                    .setChannelId(channelid)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setColor(Color.parseColor("#00000000"))
                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)//振动
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.grabseatlock)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.notify7))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,TimingTasksActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)//悬挂跳转
                    .build();
        }else {
            notification = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(Notification.DEFAULT_SOUND)//默认铃声
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setColor(Color.parseColor("#00000000"))
                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)//振动
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.grabseatlock)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.notify7))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,TimingTasksActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)//悬挂跳转
                    .build();
        }
        manager.notify(id,notification);
    }
    public void showNotification(String title,String text, Context context,int id,String channelid,String channelname){
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = null;
        Notification notification2 = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(channelid, channelname, NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(mChannel);
            notification = new Notification.Builder(context)
                    .setChannelId(channelid)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setColor(Color.parseColor("#00000000"))
                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.grabseatlock)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.notify7))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,GrabSeat.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .setContentIntent(PendingIntent.getActivities(context,0x0001,
                            new Intent[]{new Intent(context,TimingTasksActivity.class)},PendingIntent.FLAG_CANCEL_CURRENT))
                    .build();
        }else {
            notification = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setColor(Color.parseColor("#00000000"))
                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.grabseatlock)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.notify7))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,GrabSeat.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .setContentIntent(PendingIntent.getActivities(context,0x0001,
                            new Intent[]{new Intent(context,TimingTasksActivity.class)},PendingIntent.FLAG_CANCEL_CURRENT))
                    .build();
        }

        manager.notify(id,notification);
    }

    //Dialog提示
    public void showDialog(String title,String msg, Context context){
        android.support.v7.app.AlertDialog.Builder builder=new android.support.v7.app.AlertDialog.Builder(context,R.style.dialog_style);
        builder.setIcon(R.drawable.tec3);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setCancelable(true);
        builder.setPositiveButton("知道了",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        android.support.v7.app.AlertDialog dialog=builder.create();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.getWindow().setType((WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY));
        }else {
            dialog.getWindow().setType((WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));
        }
        dialog.show();
    }
    public void showDialogNotCancel(String title, String msg, Context context){
        android.support.v7.app.AlertDialog.Builder builder=new android.support.v7.app.AlertDialog.Builder(context,R.style.dialog_style);
        builder.setIcon(R.drawable.applygreen);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("取消",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                         vibrator.cancel();//取消震动
                    }
                });
        android.support.v7.app.AlertDialog dialog=builder.create();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.getWindow().setType((WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY));
        }else {
            dialog.getWindow().setType((WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));
        }
        dialog.show();
    }
    //处理预约结果，并提示
    public void tips(){
        //在操作状态1/2/3处应该应用此函数，用于构建提示信息的具体内容，否则ToaskText=null
        switch (actok){
            case 1://预约成功
                //该信息重要等级为 HIGH，用户必须与之交互
                prefSeatState = 0;//对指定座位的操作状态置0
                ToastText = user1[0] + "已预约成功!" + finalStateDevname + "时间:" + finalStartTime + "--" + finalEndTime;
                break;
            case 2://已有预约
                //该信息重要等级为 LOW，点击空白处可取消Dialog
                Handler handlerThree=new Handler(Looper.getMainLooper());
                handlerThree.post(new Runnable(){
                    public void run(){
                        showDialog("预约小提示",ToastText,TimingTasks.this);
                    }
                });
                break;
            case 3://系统未开放
                //该信息重要等级为 LOW，点击空白处可取消Dialog
                Handler handlerThree3=new Handler(Looper.getMainLooper());
                handlerThree3.post(new Runnable(){
                    public void run(){
                        showDialog("预约小提示","请在6:30之后再进行预约; 预约端当前时间"+dateFormat.format(getNetTime()),TimingTasks.this);
                    }
                });
                break;
            case 4://积分不足
                //该信息重要等级为 HIGH，用户必须与之交互
                Handler handlerThree4=new Handler(Looper.getMainLooper());
                handlerThree4.post(new Runnable(){
                    public void run(){
                        CommonFunction.showDialog("预约提示",re[0]+":剩余积分为"+re[2]+",在"+re[3]+"期间被禁止预约",TimingTasks.this);
                    }
                });
                break;
            case 5://预约冲突
                //该信息重要等级为 LOW，点击空白处可取消Dialog
                Handler handlerThree5=new Handler(Looper.getMainLooper());
                handlerThree5.post(new Runnable(){
                    public void run(){
                        showDialog("预约小提示","与现有预约存在冲突",TimingTasks.this);
                    }
                });
                break;
            case 6://其他信息
                if (CommonFunction.regexMatcher("(参数有误)",ToastText)){
                    Handler handlerThree6=new Handler(Looper.getMainLooper());
                    handlerThree6.post(new Runnable(){
                        public void run(){
                            Toast.makeText(TimingTasks.this,"座位号不存在",Toast.LENGTH_SHORT).show();
                        }
                    });
                }else if (CommonFunction.regexMatcher("(未登录)",ToastText)){
                    Handler handlerThree7=new Handler(Looper.getMainLooper());
                    handlerThree7.post(new Runnable(){
                        public void run(){
                            Toast.makeText(TimingTasks.this,"用户名/密码错误",Toast.LENGTH_SHORT).show();
                        }
                    });
                }else {
                    Handler handlerThree8=new Handler(Looper.getMainLooper());
                    handlerThree8.post(new Runnable(){
                        public void run(){
                            showDialog("预约提示",ToastText,TimingTasks.this);
                        }
                    });
                }
                break;
            default:
                break;
        }
    }
/*--------------------------------------------------------------------------------------------------------------------------------------
                                                                  执行函数
--------------------------------------------------------------------------------------------------------------------------------------*/
    //定时任务启动
    public void timingTask(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e("TimingTask","执行timingTask");
                final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

                //用户信息是否完整
                readUser1Info();
                if (!user1CanSelect){
                    stopSelf();
                    Log.e("TimingTask","缺少用户信息，已停止");
                }
                ToastText = ""; actok = 0; empty_num = 0; succeed = 0;
                //如果用户已经提供了足够的信息，则启动定时任务
                if (user1CanSelect){
                    Log.e("TimingTask","启动预约");
                    try{    //实现同一个cookie访问
                        OkHttpClient client = new OkHttpClient.Builder()
                                .cookieJar(new CookieJar() {
                                    @Override
                                    public void saveFromResponse(HttpUrl httpUrl, List<Cookie> list) {
                                        cookieStore.put(httpUrl.host(), list);
                                    }
                                    @Override
                                    public List<Cookie> loadForRequest(HttpUrl httpUrl) {
                                        List<Cookie> cookies = cookieStore.get(httpUrl.host());
                                        return cookies != null ? cookies : new ArrayList<Cookie>();
                                    }
                                })
                                .build();

                        //登录是否成功，积分是否满足条件
                        client = getCredit(client);
                        Log.e("TimingTask","获取积分");

                        //处理过程：1.登录成功，积分不为0 --> 第一次预约；2.登录失败/积分为空，--> 退出
                        String result;
                        if (creditScore & !sessionNull){//如果登录成功且积分不为0，则可以进行预约
                            Log.e("TimingTask","满足预约条件，获取座位数据");
                            client = obtainAndParseStatus(client);// 获取并解析座位状态数据
                            result =loadRsvSta("Timingdata");
                            succeed = parseStatusOfEachSeatAndReserve(result,client);
                        }else {//如果登录失败/积分为0，则显示禁止时间，并退出
                            Log.e("TimingTask","登录失败或积分为0");
                            tips();//处理结果信息
                            actok = 0; creditScore = true; sessionNull = false;//标志位恢复
                            TimingTasksCommandRunning = false; succeed = 0;//onStartCommand运行结束
                            isEndingTiming = true;//不能预约，可以结束该服务
                            return;
                        }
                        //是否预约成功：1.是 --> 处理预约结果  2.否 --> 再进行第二次预约
                        //积分满足 且第一次没有预约成功，则进行下面的步骤,连续检测5次
                        //succeed = 0;//测试
                        if (succeed != 1){
                            Log.e("TimingTask","再一次预约");
                                if (actok == 3 ){
                                    succeed = parseStatusOfEachSeatAndReserve(result,client);
                                    empty_num = 0;//每次检测结束座位数量置零
                                }else if (succeed == 5){
                                    client = obtainAndParseStatus(client);
                                    result =loadRsvSta("Timingdata");
                                    succeed = parseStatusOfEachSeatAndReserve(result,client);
                                    empty_num = 0;//每次检测结束座位数量置零
                                }
                        }
                        if (actok == 1 ){//此处表示预约成功通知    该通知必须为悬挂不回收通知，等待用户滑动删除，默认振动+默认铃声
                            tips();
                            showNotification4("座位锁定",dateFormat.format(getNetTime()) + ToastText,TimingTasks.this,29,"channel29","座位锁定");
                            //预约成功后，将预约结果写入.true表示已有座位  不论返回值是预约成功还是已有预约
                            SharedPreferences user1sharedPreferences = getSharedPreferences(user1Id+"M",MODE_PRIVATE);
                            SharedPreferences.Editor editor=user1sharedPreferences.edit();
                            editor.putBoolean(CommonFunction.systemDelayTime(0,2), true);//日期+成功
                            editor.commit();

                            ToastText = ""; empty_num = 0; actok = 0; succeed = 0;
                            TimingTasksCommandRunning = false;//onStartCommand运行结束
                            isEndingTiming = true;//预约成功，可以销毁该服务

                        }else if (actok == 2){//true表示已有座位  不论返回值是预约成功还是已有预约
                            SharedPreferences user1sharedPreferences = getSharedPreferences(user1Id+"M",MODE_PRIVATE);
                            SharedPreferences.Editor editor=user1sharedPreferences.edit();
                            editor.putBoolean(CommonFunction.systemDelayTime(0,2), true);//日期+成功
                            editor.commit();
                            actok = 0; ToastText = ""; empty_num = 0; succeed = 0;
                            TimingTasksCommandRunning = false;//onStartCommand运行结束
                            isEndingTiming = true;//预约成功，可以销毁该服务
                        }else {
                            succeed = 0;
                            TimingTasksCommandRunning = false;//onStartCommand运行结束
                        }

                    }catch (Exception e){
                        e.printStackTrace();
                        errorCode = e.toString();
                        if (cookieStore.size()==0){//访问异常，无法连接服务器！
                            Log.e("YSU访问异常，无法连接服务器","**************************");
                            if (CommonFunction.timeCompare(CommonFunction.systemTime(),CommonFunction.systemDelayTime(0,2)+ " " + "06:30:00")==1){
                                vibrator =  (Vibrator) getSystemService(VIBRATOR_SERVICE);//获得 一个震动的服务
                                long[] pattern = {500, 2000, 500, 2000};
                                vibrator.vibrate(pattern, 0);
                                Handler handlerThree=new Handler(Looper.getMainLooper());
                                handlerThree.post(new Runnable(){
                                    public void run(){
                                        showDialogNotCancel("网络异常","请及时检查！",TimingTasks.this);
                                    }
                                });
                            }
                            //该信息重要等级极高、启动振动、唤醒用户，并给出提示
                            showNotification4("网络异常","无法连接服务器",TimingTasks.this,29,"channel29","网络异常");
                        }else {
                            Handler handlerThree=new Handler(Looper.getMainLooper());
                            handlerThree.post(new Runnable(){
                                public void run(){
                                    showDialogNotCancel("错误代码", errorCode ,TimingTasks.this);
                                }
                            });
                        }
                        SharedPreferences user1sharedPreferences = getSharedPreferences(user1Id+"M",MODE_PRIVATE);
                        SharedPreferences.Editor editor=user1sharedPreferences.edit();
                        editor.putBoolean(CommonFunction.systemDelayTime(0,2), false);//日期+失败
                        editor.commit();
                        TimingTasksCommandRunning = false;//onStartCommand运行结束
                        isEndingTiming = false;//未成功不能被结束
                    }//catch end

                }
            }
        }).start();
    }
/*--------------------------------------------------------------------------------------------------------------------------------------
                                                                  操作模块
--------------------------------------------------------------------------------------------------------------------------------------*/
    //登录并进行预约
    public  int loginAndSetReserve(OkHttpClient client,String dev_id,String user) throws IOException {
        //0.1. 准备工作。检查用户信息
        String reserve = dateFormat.format(new Date());// 时间格式为2019-06-01 18:25
        String date = reserve.substring(0,10);//日期2019-06-01
        String startTime,endTime;//预约时间,默认为08:00-22:00

        if (!user1StartTime.isEmpty()){
            startTime = user1StartTime;//预约开始时间
        }else {
            startTime = "08:00";
        }
        finalStartTime = startTime;
        if (!user1EndTime.isEmpty()){
            endTime = user1EndTime;
        }else {
            endTime = "22:30";
        }
        finalEndTime = endTime;

        //0.2. 准备工作。预约信息整合
        String startDateAndTime = new StringBuilder().append(CommonFunction.getMatcher("(.*) ", reserve)).append("+")
                .append(CommonFunction.getMatcher("(.*):", startTime)).append("%3A").append(CommonFunction.getMatcher(":(.*)", startTime))
                .toString();
        String endDateAndTime = CommonFunction.getMatcher("(.*) ",reserve) + "+" + CommonFunction.getMatcher("(.*):", endTime)
                + "%3A" + CommonFunction.getMatcher(":(.*)", endTime);//"+22%3A00"
        String start_time = CommonFunction.getMatcher("(.*):",startTime)+CommonFunction.getMatcher(":(.*)",startTime);
        String end_time = CommonFunction.getMatcher("(.*):",endTime)+CommonFunction.getMatcher(":(.*)",endTime);

        //1. 登录
        String loginUrl = new StringBuilder().append("http://seat.ysu.edu.cn/ClientWeb/pro/ajax/login.aspx?act=login&id=").append(user1[1]).append("&pwd=")
                .append(user1[2]).append("&role=512&aliuserid=&schoolcode=&wxuserid=&_nocache=1551511783772")
                .toString();
        Request loginRequest = new Request.Builder().url(loginUrl).build();//向服务器发送登录请求，包括ID, password
        Response loginResponse = client.newCall(loginRequest).execute();//执行登录请求
        String loginReturn = loginResponse.body().string();//得到响应数据
        Log.e("TimingTask：登录参数",loginReturn);

        //2. 选座
        String setResvUrl = new StringBuilder().append("http://seat.ysu.edu.cn/ClientWeb/pro/ajax/reserve.aspx?dialogid=&dev_id=").append(dev_id)
                .append("&lab_id=&kind_id=&room_id=&type=dev&prop=&test_id=&term=&test_name=&start=").append(startDateAndTime).append("&end=")
                .append(endDateAndTime).append("&start_time=").append(start_time).append("&end_time=").append(end_time).append("&up_file=&memo=&act=set_resv&_=1544339248168")
                .toString();
        Request setResvRequest = new Request.Builder().url(setResvUrl).build();//准备请求
        Response setRsvResponse = client.newCall(setResvRequest).execute();//执行请求
        String setRsvResponseData = setRsvResponse.body().string();//得到响应数据
        Log.e("TimingTask：预约参数",setRsvResponseData);
        actok = parseResponseMsg(setRsvResponseData,user);

        return actok;
    }
    //解析预约结果
    public int parseResponseMsg(String data,String user){
        String[] userInfo = new String[9];
        if (user.equals("user1")){
            userInfo = user1;
        }else {
            userInfo = user2;
        }
        //1.    解析操作状态  预约冲突返回信息示例：{ret: 0, act: "set_resv", msg: "2019-06-04您在【2019年06月04日】已有预约，当日不能再预约", data: null, ext: null}
        //              预约成功返回的信息：{ret: 1, act: "set_resv", msg: "操作成功！", data: null, ext: null}
        String act_ret = CommonFunction.parseSingleLabel(data,"ret");
        String act_name = CommonFunction.parseSingleLabel(data,"act");
        String act_msg = CommonFunction.parseSingleLabel(data,"msg");
        //构建预约冲突时的返回信息
        //String reserve = dateFormat.format(new Date());// 当前时间加40分钟,该时间格式为2019-06-01 18:25
        //String date = reserve.substring(0,10);//日期2019-06-01
        //String msg_error = date + "您在【" + date.substring(0,4) + "年" + date.substring(5,7) +"月" + date.substring(8,10) + "日】已有预约，当日不能再预约";

        //2.    预约结果分析    除了以下1,2两种状态其他状态均为当前用户没有座位
        if (act_name.equals("set_resv") & act_msg.equals("操作成功！") & act_ret.equals("1")){//操作状态1：预约成功
            actok = 1;
            ToastText = userInfo[0] + "已预约成功!" + finalStateDevname + "时间:" + finalStartTime + "--" + finalEndTime;
        }else if (act_name.equals("set_resv") & act_msg.equals("已有预约")){//操作状态2：您已有预约，当日不能再预约
            actok = 2;
            ToastText = act_msg;
        }else if (CommonFunction.regexMatcher("(请在6:30之后再进行预约)",act_msg)){//操作状态3：系统未开放
            actok = 3;
        }else if (CommonFunction.regexMatcher("(积分不足)",act_msg)){//操作状态4：积分不足，禁止预约
            actok = 4;
        }else if (CommonFunction.regexMatcher("(冲突)",act_msg)){//操作状态5：与现有的预约存在冲突
            actok = 5;
        }else if (CommonFunction.regexMatcher("(ok)",act_msg)){//操作状态7：专为登录会话返回值设置
            actok = 7;
        }else {//操作状态6：其他情况的预约不成功,打印提示
            // 例：密码或学号错误时出现“未登录或登录超时，session=null 请重新登录” 座位号错误时返回的“参数错误”
            actok = 6;
            ToastText = act_msg;
        }
        return actok;
    }
    //解析座位id
    public String[] parseSeatId(String room,String id){
        //e: room = 四阅      id = 209
        String[] roomNameArray;
        String[] idArray;
        final String[] result = new String[3];//四阅-209/order/address
        String address = "101440034";//四阅-209
        String ps = room + "-" + id;//如：四阅-209
        //String a = "";//默认座位 四阅-209
        int order = 0;//座位id在数组中的序号
        //1. 提出默认阅览室座位id
        switch (room){
            case "一阅":
                roomNameArray = getResources().getStringArray(R.array.first_reading_room_name);
                idArray = getResources().getStringArray(R.array.first_reading_room_id);
                break;
            case "二阅":
                roomNameArray = getResources().getStringArray(R.array.second_reading_room_name);
                idArray = getResources().getStringArray(R.array.second_reading_room_id);
                break;
            case "三阅":
                roomNameArray = getResources().getStringArray(R.array.third_reading_room_name);
                idArray = getResources().getStringArray(R.array.third_reading_room_id);
                break;
            case "四阅":
                roomNameArray = getResources().getStringArray(R.array.fourth_reading_room_name);
                idArray = getResources().getStringArray(R.array.fourth_reading_room_id);
                break;
            case "五阅":
                roomNameArray = getResources().getStringArray(R.array.fifth_reading_room_name);
                idArray = getResources().getStringArray(R.array.fifth_reading_room_id);
                break;
            case "树华A":
                roomNameArray = getResources().getStringArray(R.array.shuhua_a_name);
                idArray = getResources().getStringArray(R.array.shuhua_a_id);
                break;
            default:
                roomNameArray = getResources().getStringArray(R.array.fourth_reading_room_name);
                idArray = getResources().getStringArray(R.array.fourth_reading_room_id);
                ps = "四阅-209"; id = "209";
                break;
        }
        //2.根据座位号检索id
        int f = 0;
        for (int index = 0; index < roomNameArray.length; index ++){
            String rr = roomNameArray[index];
            if (rr.equals(ps)){
                order = index;
                address = idArray[index];
                f = 0;//表示所选座位存在
                break;
            }else {
                Random randdiff = new Random();
                order = randdiff.nextInt(roomNameArray.length);
                f = 1;//所选座位不存在，自动随机更改
            }
        }
        if (f == 0){
            result[0] = ps; result[1] = String.valueOf(order); result[2] = address;
        }else {
            result[0] = roomNameArray[order];
            result[1] = String.valueOf(order);
            result[2] = idArray[order];
            user1PrefSeat = result[0].substring(3,6);//自动更改并显示
            Handler handlerThree=new Handler(Looper.getMainLooper());
            handlerThree.post(new Runnable(){
                public void run(){
                    showDialog("座位号异常","已自动更改为"+result[0],TimingTasks.this);
                }
            });
        }
        return result;
    }
    //遍历所有座位
    public int ergodicAllSeatAfterPrefSeat(String room,String order,OkHttpClient client,int[] storeState) throws IOException, ParseException {
        int or = Integer.parseInt(order);//把序号变为int型
        String[] roomNameArray;//阅览室座位名
        String[] idArray;//阅览室座位id
        String ps,address;
        switch (room){
            case "一阅":
                roomNameArray = getResources().getStringArray(R.array.first_reading_room_name);
                idArray = getResources().getStringArray(R.array.first_reading_room_id);
                break;
            case "二阅":
                roomNameArray = getResources().getStringArray(R.array.second_reading_room_name);
                idArray = getResources().getStringArray(R.array.second_reading_room_id);
                break;
            case "三阅":
                roomNameArray = getResources().getStringArray(R.array.third_reading_room_name);
                idArray = getResources().getStringArray(R.array.third_reading_room_id);
                break;
            case "四阅":
                roomNameArray = getResources().getStringArray(R.array.fourth_reading_room_name);
                idArray = getResources().getStringArray(R.array.fourth_reading_room_id);
                break;
            case "五阅":
                roomNameArray = getResources().getStringArray(R.array.fifth_reading_room_name);
                idArray = getResources().getStringArray(R.array.fifth_reading_room_id);
                break;
            case "树华A":
                roomNameArray = getResources().getStringArray(R.array.shuhua_a_name);
                idArray = getResources().getStringArray(R.array.shuhua_a_id);
                break;
            default:
                roomNameArray = getResources().getStringArray(R.array.fourth_reading_room_name);
                idArray = getResources().getStringArray(R.array.fourth_reading_room_id);
                break;
        }

        int s,r = 0;
        for (int indey = or; indey < idArray.length + or; indey ++){
            if (indey>=idArray.length){
                indey = indey - idArray.length;
            }
            if (storeState[indey] == 1){
                ps = roomNameArray[indey];//座位名称
                finalStateDevname = ps;//最终预约的座位
                address = idArray[indey];//座位id
                s = loginAndSetReserve(client,address,"user1");//登录并进行预约,处理返回值，s = actok
                if (s == 1){//如果对指定座位预约成功，则指定座位的操作状态置1
                    r = 1;
                    tips();//在操作状态1/2/3处应该应用此函数，用于构建提示信息的具体内容，否则ToaskText=null
                    return r;
                }else if (s == 2 ){//预约成功/已有预约
                    r = 1;
                    return r;
                }else if (s == 3){//系统未开放
                    return r;//r=0;
                }else if (s == 5){//预约冲突，检索下一个座位
                    continue;
                }else if (s == 6){
                    break;
                }
                Log.e("TimingTask：预约结果分析" + ps,String.valueOf(s));//预约的结果分析/类别归属
            }
        }
        return r;
    }
    //解析每个座位状态信息
    public int parseStatusOfEachSeatAndReserve(String dataArray,OkHttpClient client) throws JSONException, IOException, ParseException {
        int state = -1; //表示预约状态。0：无预约；1：有预约
        final JSONArray jsonArray = new JSONArray(dataArray);//获取到的座位状态数据
        int[] storeState = new int[jsonArray.length()];//存储每个座位的状态 不可预约：1；可预约：0.

        //0. 座位余量查询 empty_numm
        empty_num = 0;
        for (int indey = 0; indey < jsonArray.length(); indey ++){//检索所有座位的状态并存储
            String s = jsonArray.getString(indey);
            String t = CommonFunction.parseSingleLabel(s,"ts");//座位状态ts="[]"为空闲
            if (t.equals("[]")){//空闲
                storeState[indey] = 1;
                empty_num ++ ;
            }else{
                storeState[indey] = 0;
            }
        }
        //座位余量提示
        Handler handlerThree=new Handler(Looper.getMainLooper());
        handlerThree.post(new Runnable(){
            public void run(){
                Toast.makeText(TimingTasks.this,"当前阅览室座位余量   " + empty_num + "/" + jsonArray.length(),Toast.LENGTH_SHORT).show();
            }
        });
        Log.e("TimingTask:座位余量"+CommonFunction.systemTime(),String.valueOf(empty_num));

        //是否有余量  1.否 --> 操作状态为0 --> 退出  2.是 --> 预约
        if (empty_num == 0){
            actok = 0;
            return 0;
        }else {
            //是否指定了首选座位 1.是 --> 访问首选座位状态，并预约  2.否 --> 随机指定座位并开始预约

            // 1.首选座位是否预约成功. 1.是 --> 提示并结束  2.否 --> 检索其他座位
            //获取首选座位在该阅览室座位中的序号 res[3] = {"四阅-209","12","101440034"} ps(prefSeat)/order/address
            String[] res = new String[3];
            int r = 0;
            if (!user1PrefSeat.equals("") & prefSeatState==0){
                //四阅-209/order/address
                Log.e("TimingTask","检索指定座位");
                res = parseSeatId(user1[3],user1[5]);       Log.e("TimingTask：指定阅览室",user1[3]);
                r = loginAndSetReserve(client,res[2],"user1");      Log.e("TimingTask：指定座位id",res[2]);
                //启动候选座位预约
                if (r != 1 & r != 2 & r != 3){
                    for (int i = 0; i < candidateArray.length; i++){
                        if (candidateArray[i].equals("")){
                            Log.e("第" + i +"个候选座位为空","no");
                        }else {
                            res = parseSeatId(user1[3],candidateArray[i]);
                            r = loginAndSetReserve(client,res[2],"user1");
                            if (r == 1 | r == 2){
                                break;
                            }
                        }
                    }
                }
                if (r == 5 ){//预约冲突
                    prefSeatState = 1;//指定座位操作失败
                } else if (r == 1 ){
                    finalStateDevname = res[0]; //finalStateTime在登录预约的时候被赋值  finalStateDevname在解析座位状态的时候被赋值
                    tips();//将预约座位的详细信息赋值给ToastText
                    return 1;
                }else if (r == 2){
                    return 1;
                }
            }
            //  2. 用户没有指定首选座位，或首选座位未能预约成功
            if (user1PrefSeat.equals("") | prefSeatState==1 ){
                Log.e("TimingTask","正在查找其他座位");
                if (user1PrefSeat.equals("")){//如果没有指定，则随机生成
                    Random a = new Random();
                    int aa = a.nextInt(jsonArray.length());
                    res[1] = String.valueOf(aa);
                    Log.e("TimingTask：随机座位",Integer.toString(aa));
                }
                state = ergodicAllSeatAfterPrefSeat(user1[3],res[1],client,storeState);//从指定座位开始，按顺序遍历所有座位，直至遍历到当前座位结束，并返回预约状态
            }
        }
        empty_num = 0;
        return state;
    }
    //获取网络时间
    public Date getNetTime(){
        try {
            URL url = new URL(Url);
            URLConnection uc = url.openConnection();
            uc.setReadTimeout(5000);
            uc.setConnectTimeout(5000);
            uc.connect();
            long correctTime = uc.getDate();
            webDate = new Date(correctTime);
            Log.e("TimingTask：网络时间",dateFormat.format(webDate));
        }catch (Exception e){
            Handler handlerThree7=new Handler(Looper.getMainLooper());
            handlerThree7.post(new Runnable(){
                public void run(){
                    Toast.makeText(TimingTasks.this,"请重启手机或重新开启网络",Toast.LENGTH_SHORT).show();
                }
            });
            webDate = defDate;
            Log.e("TimingTask：获取网络时间出错",e.toString());
        }
        return webDate;
    }
    //从服务器获取座位状态信息（包含解析与保存步骤）
    public OkHttpClient obtainAndParseStatus(OkHttpClient client) throws IOException {
        //1.    设定预约时间，如果用户没有指定时间，则从40分钟之后开始
        String rsvTime = dateFormat.format(new Date());// 当前时间加40分钟,该时间格式为2019-06-01 18:25
        String date = rsvTime.substring(0,10);//日期2019-06-01
        //2.    日期时间格式整合    user1[6] = user1StartTime;   user1[7] = user1EndTime;
        String fr_start = new StringBuilder().append(CommonFunction.getMatcher("(.*):", user1[6])).append("%3A")
                .append(CommonFunction.getMatcher(":(.*)", user1[6]))
                .toString();
        String fr_end = new StringBuilder().append(CommonFunction.getMatcher("(.*):", user1[7])).append("%3A")
                .append(CommonFunction.getMatcher(":(.*)", user1[7]))
                .toString();;

        //3.    获取系统时间
        //http://202.206.242.87/ClientWeb/pro/ajax/device.aspx?byType=devcls&classkind=8&display=fp&md=d&room_id=100457213
        // &purpose=&selectOpenAty=&cld_name=default&date=2019-06-21&fr_start=14%3A00&fr_end=21%3A00&act=get_rsv_sta&_=1561082040042
        String roomIdUrl = urlCommonFirstPara.concat(user1[4]).concat(urlCommonSecondPara)
                .concat(date).concat("&fr_start=").concat(fr_start).concat("&fr_end=").concat(fr_end)
                .concat(urlCommonThirdPara).concat(user1[8]);//目标阅览室url

        //4.     访问服务器座位状态信息
        Request getRsvSta = new Request.Builder().url(roomIdUrl).build();
        Response rsvStaRsponse = client.newCall(getRsvSta).execute();
        String rsvStateData = rsvStaRsponse.body().string();
        //5.    从服务器返回的数据中解析出座位状态信息，并保存到data
        String data = parseJSONFromResponse(rsvStateData);
        Log.e("TimingTask","获取座位状态数据");
        return client;
    }
    //解析出多个标签的数据,ret,act,msg,data,ext et al.
    private String parseJSONFromResponse(String jsonData){
        String result = "";
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            String ret = jsonObject.getString("ret");
            String act = jsonObject.getString("act");
            String msg = jsonObject.getString("msg");
            String data = jsonObject.getString("data");
            String ext = jsonObject.getString("ext");
            saveResvSta(data,"Timingdata");
            Log.v("保存成功","保存成功");
            result = data;
        }catch (Exception e){
            e.printStackTrace();
        }
        return  result;
    }
    //读取座位状态信息
    public String loadRsvSta(String st){
        FileInputStream in = null;
        BufferedReader reader = null;
        StringBuilder content = new StringBuilder();
        try{
            in = openFileInput(st);
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine())!= null){
                content.append(line);
            }
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            if (reader != null){
                try{
                    reader.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
        return content.toString();
    }
    //保存座位状态信息
    public void  saveResvSta(String rsvstainfo,String filename){
        FileOutputStream out = null;
        BufferedWriter writer = null;
        try{
            out = openFileOutput(filename, Context.MODE_PRIVATE);
            writer = new BufferedWriter(new OutputStreamWriter(out));
            writer.write(rsvstainfo);
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            try {
                if (writer != null){
                    writer.close();
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
    //读取用户设定的选座信息：姓名、id、密码、阅览室、阅览室id（系统自动识别）、首选座位号、开始时间、结束时间、阅览室url
    public void readUser1Info(){
        SharedPreferences pref = getSharedPreferences("user1Info",MODE_PRIVATE);
        user1Name = pref.getString("username","");
        user1Id = pref.getString("userid","");
        user1Password = pref.getString("userpassword","");
        user1Room = pref.getString("room","");
        user1RoomId = pref.getString("roomid","");
        user1PrefSeat = pref.getString("prefseat","");
        user1StartTime = pref.getString("starttime","");
        user1EndTime = pref.getString("endtime","");
        user1UrlEnd = pref.getString("urlend","");
        candidateArray[0] = pref.getString("candidate1","");
        candidateArray[1] = pref.getString("candidate2","");
        candidateArray[2] = pref.getString("candidate3","");
        candidateArray[3] = pref.getString("candidate4","");

        user1[0] = user1Name;
        user1[1] = user1Id;
        user1[2] = user1Password;
        user1[3] = user1Room;
        user1[4] = user1RoomId;
        user1[5] = user1PrefSeat;
        user1[6] = user1StartTime;
        user1[7] = user1EndTime;
        user1[8] = user1UrlEnd;

        user1CanSelect = !user1[1].isEmpty() & !user1[2].isEmpty() & !user1[3].isEmpty();
    }

    //解析用户积分信息
    private String parseJSONResponse(String jsonData){
        String result = "";
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            String data = jsonObject.getString("data");
            result = data;
        }catch (Exception e){
            e.printStackTrace();
        }
        return  result;
    }
    public void parseJSONScore(String data){
        String name = CommonFunction.parseSingleLabel(data,"name");//班级
        String cls = CommonFunction.parseSingleLabel(data,"cls");//姓名
        String credit = CommonFunction.parseSingleLabel(data,"credit");//信誉
        //credit: [["个人预约制度","0","300","2019-06-19至2019-06-22"]]//信誉积分为0时的返回信息
        //credit = "[[\"个人预约制度\",\"200\",\"300\",\"\"]]";//信誉积分不为0时的返回信息
//        credit = "[[\"个人预约制度\",\"0\",\"300\",\"2019-06-19至2019-06-22\"]]";//测试用
        Log.e("TimingTask：积分",credit);
        String c = CommonFunction.getMatcher("\\[(.*)\\]",credit);
        c = CommonFunction.getMatcher("\\[(.*)\\]",c);
        c = CommonFunction.getMatcher("\"(.*)\"",c);
        String[] r = c.split("\",\"");
        for (int i = 0;i<r.length;i++ ){
            Log.e(Integer.toString(i),r[i]);
        }
        if (Integer.parseInt(r[1])==0){//姓名+班级+剩余积分+限制时间
            creditScore = false;
            actok = 4;
            re[0] = name; re[1] = cls; re[2] = r[1]; re[3] = r[3];
        }else {
            creditScore = true;
            re[0] = name; re[1] = cls; re[2] = r[1]; re[3] = "";

        }
    }
    public OkHttpClient getCredit(OkHttpClient client) throws IOException {
        //1. 登录
        String loginUrl = new StringBuilder().append("http://seat.ysu.edu.cn/ClientWeb/pro/ajax/login.aspx?act=login&id=").append(user1[1]).append("&pwd=")
                .append(user1[2]).append("&role=512&aliuserid=&schoolcode=&wxuserid=&_nocache=1551511783772")
                .toString();
        Request loginRequest = new Request.Builder().url(loginUrl).build();//向服务器发送登录请求，包括ID, password
        Response loginResponse = client.newCall(loginRequest).execute();//执行登录请求
        String loginReturn = loginResponse.body().string();//得到响应数据
        Log.e("TimingTask-getCredit：登录",loginReturn);
        //解析登录结果
        parseResponseMsg(loginReturn,"user1");
        if (actok == 6){
            sessionNull = true;
        }else if (actok == 4){
            creditScore = false;
            String data  = parseJSONResponse(loginReturn);
            parseJSONScore(data);
        }else {
            String data2 = parseJSONResponse(loginReturn);
            parseJSONScore(data2);
        }
        return client;
    }
}
