package com.example.uniconseat;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

//通知id 40-49
public class TipsJumpService extends Service {
    /* 时间节点信息 */
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static String targetTime;//该时间为GuardianService可触发时间段的开始时间6:20-6:45
    private static String endTime,screenTime;
    private long diff;
    private String tomorrowStartSelf,tomorrowZero,reStartSelfTime,tomorrowStartGuard;
    private Date tomorrow;

    /* 读取到的用户输入信息 */
    public String[] user1 = new String[9];//保存用户信息
    public String[] user2 = new String[9];
    public String user1Name,user1Id,user1Password,user1Room,user1RoomId,user1PrefSeat,user1StartTime,user1EndTime,user1UrlEnd;//用户信息
    public String user2Name,user2Id,user2Password,user2Room,user2RoomId,user2PrefSeat,user2StartTime,user2EndTime,user2UrlEnd;
    public boolean user1CanSelect = false; boolean user2CanSelect = false;//如果用户信息不为空则进行选座

    /* 标志位信息 */
    static boolean TimpsJumpServiceStart;//服务是否启动
    public boolean TipsJumpServiceStarted;//服务是否被switch按钮启动
    public static AlarmManager alarmManager;
    public static PendingIntent pi;
    public static boolean reserveUser1;
    public static boolean isRegisterReceiver = false;//监听屏幕的广播是否注册

    //电源锁，解决锁屏Service不执行
    private PowerManager.WakeLock wakeLock = null;

    IntentFilter intentFilter = new IntentFilter();
    BroadcastReceiver receiver = new HooliganReceiver();

    public TipsJumpService() {
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }
    @SuppressLint("InvalidWakeLockTag")
    @Override//创建服务的时候调用
    public void onCreate(){
        TimpsJumpServiceStart = true;
        isRegisterReceiver = false;
        Log.e("TipsJumpService已创建","onCreate");
        super.onCreate();
        //电源锁
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TipsJumpService.class.getName());//PARTIAL_WAKE_LOCK
        wakeLock.acquire();

        //前台服务信息
        Intent intent = new Intent(this, TimingTasksActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Notification notification = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel("TipsJumpService", "桥接服务", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(mChannel);
            notification = new Notification.Builder(this)
                    .setChannelId("TipsJumpService")
                    .setContentTitle("TipsJumpService")
                    .setContentText("桥接服务...")
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.jump)
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.jump128))
                    .setFullScreenIntent(PendingIntent.getActivities(getApplicationContext(),0,
                            new Intent[]{new Intent(getApplicationContext(),TimingTasksActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .build();
        }else {
            notification = new NotificationCompat.Builder(this)
                    .setContentTitle("TipsJumpService")
                    .setContentText("桥接服务...")
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.jump)
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.jump128))
                    .setFullScreenIntent(PendingIntent.getActivities(getApplicationContext(),0,
                            new Intent[]{new Intent(getApplicationContext(),TimingTasksActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .build();
        }
        startForeground(40,notification);
    }
    @Override//每次服务启动的时候调用
    public int onStartCommand(Intent intent, int flags, int startId){
        Date reStartSelfDate = null,targetDate,endDate,tomorrowZeroDate,tomoStartSelfDate = null,tomoStartGuardDate;
//        Log.e("YSU跳转服务启动","onStartCommand");
        //计算启动的时间信息，当天启动自身的时间、第二天启动自身的时间；当天服务结束时间、第二天服务结束时间；当天启动守护服务时间，第二天启动守护服务时间
        Calendar currentTimeAddOneDay = Calendar.getInstance();
        targetDate = currentTimeAddOneDay.getTime();
        reStartSelfTime = dateFormat.format(targetDate).substring(0,10) + " " + "06:15:00";
        targetTime = dateFormat.format(targetDate).substring(0,10) + " " + "06:20:00";
        endTime = dateFormat.format(targetDate).substring(0,10) + " " + "06:45:00";
        screenTime = dateFormat.format(targetDate).substring(0,10) + " " + "06:00:00";
        currentTimeAddOneDay.add(Calendar.DAY_OF_MONTH,1);
        tomorrow = currentTimeAddOneDay.getTime();
        tomorrowZero = dateFormat.format(tomorrow).substring(0,10) + " " + "00:00:00";
        tomorrowStartSelf = dateFormat.format(tomorrow).substring(0,10) + " " + "06:15:00";
        tomorrowStartGuard = dateFormat.format(tomorrow).substring(0,10) + " " + "06:20:00";

        try {
            reStartSelfDate = dateFormat.parse(reStartSelfTime);
            targetDate = dateFormat.parse(targetTime);
            endDate = dateFormat.parse(endTime);
            tomorrowZeroDate = dateFormat.parse(tomorrowZero);
            tomoStartSelfDate = dateFormat.parse(tomorrowStartSelf);
            tomoStartGuardDate = dateFormat.parse(tomorrowStartGuard);
        } catch (ParseException e) {
            e.printStackTrace();
        }

//        Log.e("当天启动自己的时间",reStartSelfTime);
//        Log.e("当天启动守护服务的时间",targetTime);
//        Log.e("当天守护服务触发的结束时间",endTime);
//        Log.e("第二天启动自己的时间",tomorrowStartSelf);
//        Log.e("明天启动守护服务的时间",tomorrowStartGuard);

        //服务启动标志位
        TimpsJumpServiceStart = true;
        Intent intent1 = new Intent(this,TipsJumpService.class);
        pi = PendingIntent.getService(this,0,intent1,0);

        //用户当天是否预约成功标志
        SharedPreferences sharedPreferences1 = getSharedPreferences(user1Id+"M",MODE_PRIVATE);
        reserveUser1 = sharedPreferences1.getBoolean(CommonFunction.systemDelayTime(0,2), false);
//        Log.e("reserveUser1",String.valueOf(reserveUser1));

        //1. 是否已经被启动
        SharedPreferences pref = getSharedPreferences("TipsJumpsService",MODE_PRIVATE);
        TipsJumpServiceStarted = pref.getBoolean("TipsJumpsService",false);
        Log.e("TipsJumpService是否start",String.valueOf(TipsJumpServiceStarted));

        //2. 是否已经注册
        SharedPreferences loginPreferences = getSharedPreferences("login",MODE_PRIVATE);
        boolean login =loginPreferences.getBoolean("login", false);

        //3. 用户信息是否完整
        readUser1Info();
        readUser2Info();

        //4. 获取定时服务
        int sysTomoZero = CommonFunction.timeCompare(CommonFunction.systemTime(),tomorrowZero);//是当天还是第二天 00:00
        int sysRestartSelf = CommonFunction.timeCompare(CommonFunction.systemTime(),reStartSelfTime);//是否该启动自己 6:15
        int sysTarget = CommonFunction.timeCompare(CommonFunction.systemTime(),targetTime); //启动守护服务前还是后 6:20
        int sysEnd = CommonFunction.timeCompare(CommonFunction.systemTime(),endTime);//守护服务出发时间是否一过 6:40
        int sysTomorrowSelf = CommonFunction.timeCompare(CommonFunction.systemTime(),tomorrowStartSelf);//第二天启动自身的时间 6:15
        int sysScreen =  CommonFunction.timeCompare(CommonFunction.systemTime(),screenTime);//屏幕常亮开始时间 6:00

//        Utils.init(getApplicationContext());
//        IntentUtils.gotoHome();
        //是否在当天的6:00-6:40之间
        if (sysScreen != 3 & sysEnd == 3){
            if (!isRegisterReceiver){
                brightScreen();//点亮屏幕，启动常亮activity，动态注册广播
            }
        }

        //5. 当信息完整且经过注册之后才提供相应的服务
        if ( (user1CanSelect | user2CanSelect) & login){
            if (sysRestartSelf != 3 & sysTomorrowSelf == 3){//Today 6:15  =< sys < tomorrow 6:15
                if (sysTarget == 3){//Today 6:15 =< sys < 6:20,today
                    diff = targetDate.getTime() - CommonFunction.systemTimeDate().getTime();
                    if (diff > 2 * 60 * 1000) diff = diff - 60 * 1000;
                    Log.e("TipsJump：系统时间6:15-6:20",String.valueOf(diff));

                }else if (sysEnd != 1){//today 6:20 =< sys =< 6:45,today
                    diff = 60 * 1000;
                    if ( !reserveUser1 & !GuardianService.GuardianServiceStart){
                        Intent intentGuardian = new Intent(TipsJumpService.this,GuardianService.class);
                        intentGuardian.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startService(intentGuardian);
                        Log.e("TipsJump：系统时间6:20-6:45","没有座位没有启动守护服务"+String.valueOf(diff));

                    }else if (reserveUser1){
                        diff = 60 * 60 * 1000;
                        Log.e("TipsJump：系统时间6:20-6:40","预约成功"+String.valueOf(diff));
                    }

                }else {//today 6:40 < sys < 6:15,tomorrow
                    //diff = tomoStartSelfDate.getTime() - CommonFunction.systemTimeDate().getTime();
                    diff = 60 * 60 * 1000;
                    Log.e("TipsJump：系统时间超过6:45",String.valueOf(diff));

                    //停止未关闭的服务
                    if (GuardianService.GuardianServiceStart){
                        Log.e("TipsJump：定时任务超时","即将关闭GuardianService");
                        if (GuardianService.alarmManager != null){
                            GuardianService.alarmManager.cancel(GuardianService.pi);
                        }
                        if (GuardianService.alarmManagerFinal != null){
                            GuardianService.alarmManagerFinal.cancel(GuardianService.pi);
                        }
                        Intent intentGuardian = new Intent(TipsJumpService.this,GuardianService.class);
                        stopService(intentGuardian);
                    }
                    if (TimingTasks.TimingTasksCreate){
                        Log.e("TipsJump：定时任务超时","即将关闭TimingTasks");
                        Intent intentTiming = new Intent(TipsJumpService.this,TimingTasks.class);
                        stopService(intentTiming);
                    }
                    //释放监听屏幕的注册
                    if (isRegisterReceiver){
                        Log.e("TipsJump：定时任务超时","释放监听屏幕注册");
                        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
                        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
                        intentFilter.addAction(Intent.ACTION_USER_PRESENT);
                        try {
                            unregisterReceiver(receiver);
                        }catch (Exception e){
                            e.printStackTrace();
                        }

                    }
                }
            }else {//Toady < 6:15
                diff = reStartSelfDate.getTime() - CommonFunction.systemTimeDate().getTime();
                if (diff > 20 * 60 * 1000)  diff = diff - 60 * 1000;
                Log.e("TipsJump：系统时间在6:15之前",String.valueOf(diff));
            }
        }else if (!login){
            ToastCustom.passValue(3000,1000,2,0,100);
            ToastCustom.getInstance(TipsJumpService.this).show("您还未注册，请点击主页右上角要是进行注册", 3000);
        }else {//如果信息不完整，但已经注册，则提供座位监控服务
            Log.e("TipsJump：用户信息缺失","停止服务");
            ToastCustom.passValue(3000,1000,2,0,100);
            ToastCustom.getInstance(TipsJumpService.this).show("用户信息不完整，已停止桥接服务", 3000);
            showNotification("服务关闭","用户信息不完整，已停止定时任务服务",TipsJumpService.this,45,"channel45","桥接服务");
            stopSelf();   //测试时注释掉
        }

        //6. 任务完成，关闭其他服务
        if (reserveUser1){
            if (GuardianService.GuardianServiceStart){
                Log.e("TipsJump：任务完成","即将关闭GuardianService");
                GuardianService.alarmManagerFinal.cancel(GuardianService.pi);
                GuardianService.alarmManager.cancel(GuardianService.pi);
                Intent intentGuardian = new Intent(TipsJumpService.this,GuardianService.class);
                stopService(intentGuardian);

            }
            if (TimingTasks.TimingTasksCreate){
                Intent intentTiming = new Intent(TipsJumpService.this,TimingTasks.class);
                stopService(intentTiming);
                Log.e("TipsJump：任务完成","即将关闭TimingTasks");
            }
            //释放监听屏幕的注册
            if (isRegisterReceiver){
                Log.e("TipsJump：任务完成","释放监听屏幕注册");
                intentFilter.addAction(Intent.ACTION_SCREEN_ON);
                intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
                intentFilter.addAction(Intent.ACTION_USER_PRESENT);
                try {
                    unregisterReceiver(receiver);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

        //设定闹钟
        if (user1CanSelect & login){
            Log.e("TipsJump：设置定时",String.valueOf(diff));
            long triggerAtTime = SystemClock.elapsedRealtime() + Math.abs(diff);    //下一次启动的时间
            Log.e("TipsJump：设置定时",String.valueOf(triggerAtTime));
            alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
            }else {
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
            }
        }
        return super.onStartCommand(intent,flags,startId);
    }
    @Override//服务销毁的时候调用
    public void  onDestroy(){
        TimpsJumpServiceStart = false;
        //停止未关闭的服务
        if (GuardianService.GuardianServiceStart){
            Log.e("TipsJump：onDestroy","即将关闭GuardianService");
            if (GuardianService.alarmManager != null){
                GuardianService.alarmManager.cancel(GuardianService.pi);
            }
            if (GuardianService.alarmManagerFinal != null){
                GuardianService.alarmManagerFinal.cancel(GuardianService.pi);
            }

            Intent intentGuardian = new Intent(TipsJumpService.this,GuardianService.class);
            stopService(intentGuardian);

        }
        if (TimingTasks.TimingTasksCreate){
            Log.e("TipsJump：onDestroy","即将关闭TimingTasks已关闭");
            Intent intentTiming = new Intent(TipsJumpService.this,TimingTasks.class);
            stopService(intentTiming);

        }
        //释放电源锁
        if (wakeLock != null) {
            Log.e("TipsJump：onDestroy","释放电源锁");
            wakeLock.release();
            wakeLock = null;
        }
        //释放监听屏幕的注册
        if (isRegisterReceiver){
            Log.e("TipsJump：onDestroy","释放监听屏幕的注册");
            intentFilter.addAction(Intent.ACTION_SCREEN_ON);
            intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
            intentFilter.addAction(Intent.ACTION_USER_PRESENT);
            try {
                unregisterReceiver(receiver);
            }catch (Exception e){
                e.printStackTrace();
            }

        }
        super.onDestroy();
        Log.e("TipsJump：onDestroy","跳转服务已销毁");
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
    public void readUser2Info(){
        SharedPreferences pref2 = getSharedPreferences("user2Info",MODE_PRIVATE);
        user2Name = pref2.getString("username","");
        user2Id = pref2.getString("userid","");
        user2Password = pref2.getString("userpassword","");
        user2Room = pref2.getString("room","");
        user2RoomId = pref2.getString("roomid","");
        user2PrefSeat = pref2.getString("prefseat","");
        user2StartTime = pref2.getString("starttime","");
        user2EndTime = pref2.getString("endtime","");
        user2UrlEnd = pref2.getString("urlend","");

        user2[0] = user2Name;
        user2[1] = user2Id;
        user2[2] = user2Password;
        user2[3] = user2Room;
        user2[4] = user2RoomId;
        user2[5] = user2PrefSeat;
        user2[6] = user2StartTime;
        user2[7] = user2EndTime;
        user2[8] = user2UrlEnd;

        user2CanSelect = !user2[1].isEmpty() & !user2[2].isEmpty() & !user2[3].isEmpty();
//        user2CanSelect = !user2[1].equals("") & !user2[2].equals("") & !user2[3].equals("");
    }
    public void showNotification(String title, String text, Context context, int id, String channelid, String channelname){
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
                    .setSmallIcon(R.drawable.timingtasknotigy)
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
                            R.drawable.seatlock))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,GrabSeat.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .setContentIntent(PendingIntent.getActivities(context,0x0001,
                            new Intent[]{new Intent(context,TimingTasksActivity.class)},PendingIntent.FLAG_CANCEL_CURRENT))
                    .build();
        }

        manager.notify(id,notification);
    }
    public void brightScreen(){
        //屏幕锁屏：
        KeyguardManager mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        boolean flag = mKeyguardManager.inKeyguardRestrictedInputMode();
        //屏幕是否亮屏：
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //true为打开，false为关闭
        boolean ifOpen = powerManager.isScreenOn();

        if (!ifOpen | flag){
            PowerManager.WakeLock wl;
            wl = powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK,TipsJumpService.class.getName());
            wl.acquire(); // 点亮屏幕
            wl.release(); // 释放
            //启动屏幕常亮活动
            Intent intentHooligan = new Intent(this,HooliganActivity.class);
            intentHooligan.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentHooligan);

        }
        Log.e("TipsJump：屏幕是否亮屏",String.valueOf(ifOpen));
        Log.e("TipsJump：屏幕是否锁屏",String.valueOf(flag));
        //动态注册监听屏幕广播

        receiver = new HooliganReceiver();

        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(receiver,intentFilter);

        isRegisterReceiver = true;
    }
}
