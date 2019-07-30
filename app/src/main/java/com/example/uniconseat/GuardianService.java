package com.example.uniconseat;

import android.app.AlarmManager;
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
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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

//通知的id段 30-39
public class GuardianService extends Service {
    public GuardianService() {
    }
    /* 时间信息 */
    static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static String Url = "http://202.206.242.87/ClientWeb/xcus/ic2/Default.aspx";
    static String targetTime ;//当天的启动时间:06:20:00
    static Date targetDate = null;
    //date格式的启动时间
    static String endTime ;//10分钟后结束检测
    static String errorTime;//到06:28还不能访问服务器，则叫醒用户
    static Date webTargetDate;
    static String webTargetTime;
    static Date webDate;
    static Date webTime;
    static Date defDate;
    static {
        try {
            defDate = dateFormat.parse("1970-01-01 01:00:00");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }//访问网络出错时返回这个时间
    static Date netError;//服务器开放系统时一刻会变成这个时间
    static {
        try {
            netError = dateFormat.parse("1970-01-01 08:00:00");
        }catch (ParseException e){
            e.printStackTrace();
        }
    }
    public long diff,diffSysNet=0;//定时间隔，系统和服务器时间差值 ms

    /* 标志信息 */
    public boolean open = false;
    public Date serverDate;
    public boolean reserveUser1;
    public boolean user1CanSelect = false;
    public int actok;
    public String finalStartTime,finalEndTime,finalStateDevname,ToastText = "";
    static boolean isEndingGuardian = false;//该服务是否可以被结束
    static boolean GuardianServiceStart;//服务是否启动
    static AlarmManager alarmManager;
    static AlarmManager alarmManagerFinal;
    static PendingIntent pi;
    static boolean timingSatrt = false;

    /* 读取到的用户输入信息 */
    public String[] user1 = new String[9];//保存用户信息
    public String[] user2 = new String[9];
    public String user1Name,user1Id,user1Password,user1Room,user1RoomId,user1PrefSeat,user1StartTime,user1EndTime,user1UrlEnd;//用户信息
    public String user2Name,user2Id,user2Password,user2Room,user2RoomId,user2PrefSeat,user2StartTime,user2EndTime,user2UrlEnd;



    static String TAG;//notification用的全局信息
    static int Id;
    static int Length;

    /* 获取的一些权限信息 */
    Vibrator vibrator;
    //电源锁，解决锁屏Service不执行
    private PowerManager.WakeLock wakeLock = null;


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
        return  null;
    }
    @Override//创建服务的时候调用
    public void onCreate(){
        super.onCreate();
        Log.e("Guardian：onCreate","已创建");
        vibrator =  (Vibrator) getSystemService(VIBRATOR_SERVICE);
        readUser1Info();

        //电源锁
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, GuardianService.class.getName());//PARTIAL_WAKE_LOCK
        wakeLock.acquire(30 * 60 * 1000);

        //前台状态栏
        Intent intent = new Intent(this, TimingTasksActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel("GuardianService", "守护服务", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(mChannel);
            notification = new Notification.Builder(this)
                    .setChannelId("GuardianService")
                    .setContentTitle("GuardianService")
                    .setContentText("监控中...")
                    .setWhen(System.currentTimeMillis())
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .setSmallIcon(R.drawable.heart)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.guardian1round))
                    .setFullScreenIntent(PendingIntent.getActivities(getApplicationContext(),0,
                            new Intent[]{new Intent(getApplicationContext(),TimingTasksActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .build();
        }else {
            notification = new NotificationCompat.Builder(this)
                    .setContentTitle("GuardianService")
                    .setContentText("监控中...")
                    .setWhen(System.currentTimeMillis())
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .setSmallIcon(R.drawable.heart)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.guardian1round))
                    .setFullScreenIntent(PendingIntent.getActivities(getApplicationContext(),0,
                            new Intent[]{new Intent(getApplicationContext(),TimingTasksActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .build();
        }
        startForeground(30,notification);
    }
    @Override//每次服务启动的时候调用
    public int onStartCommand(Intent intent, int flags, int startId){
        /* 在早晨6:20启动该服务（守护服务），设定监测的结束时间为6:40。预约系统开放时间是6:30，服务器可以访问的时间要早于6:30，启动登录预约（定时任务服务）时间为6:29:55。
        如果在系统时间到达6:40，检测到用户还没有座位，则启动振动，弹出检查提示，确认是否关闭振动*/

        GuardianServiceStart = true;
        Log.e("Guardian：start","启动");

        //定义时间信息
        Calendar currentTimeAddOneDay = Calendar.getInstance();
        targetDate = currentTimeAddOneDay.getTime();
        targetTime = dateFormat.format(targetDate).substring(0,10) + " " + "06:20:00";
        webTargetTime = dateFormat.format(targetDate).substring(0,10)+ " " + "06:29:45";
        endTime = dateFormat.format(targetDate).substring(0,10) + " " + "06:45:00";
        errorTime = dateFormat.format(targetDate).substring(0,10) + " " + "06:29:00";
        currentTimeAddOneDay.add(Calendar.DAY_OF_MONTH,1);
        Date tomorrow = currentTimeAddOneDay.getTime();
        String tomorrowTime = dateFormat.format(tomorrow).substring(0,10) + " " + "06:20:00";
        try {
            webTargetDate = dateFormat.parse(webTargetTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        //定义alarm
        alarmManagerFinal = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent1 = new Intent(this,GuardianService.class);
        pi = PendingIntent.getService(this,0,intent1,0);


        /*判断客户端系统数时间与  启动定时服务时间  结束定时服务时间的大小关系
         * 情况1：还没到启动时间，守护服务继续定时监控时间
         * 情况2：刚到开始时间/已超过开始时间还没到结束时间，判断用户是否有座
         * 情况3：已经超过了结束时间，如果还没有预约到座位，就强制振动叫醒用户*/

        /** 根据保存到变量值查看用户是否已经有座  **/
        SharedPreferences user1sharedPreferences = getSharedPreferences(user1Id+"M",MODE_PRIVATE);
        reserveUser1 = user1sharedPreferences.getBoolean(CommonFunction.systemDelayTime(0,2), false);

        if (TimingTasks.isEndingTiming){
            reserveUser1 = TimingTasks.isEndingTiming;
        }

        Log.e("Guardian：检索赋值","TimingTasks.isEndingTiming = "+ reserveUser1);

        /** 1 系统时间 > 启动时间； 2 系统时间 = 启动时间； 3 系统时间 < 启动时间**/
        //      过了/刚好/没到
        diff = 2000;  int sysAndTarget;  int sysAndEnd;
        sysAndTarget = CommonFunction.timeCompare(CommonFunction.systemTime(),targetTime);/** 系统时间和目标时间比较 系统时间的结束预约时间比较 **/
        sysAndEnd = CommonFunction.timeCompare(CommonFunction.systemTime(),endTime);

        if ( sysAndTarget == 3){//系统时间小于启动服务的时间
            diff = targetDate.getTime() - CommonFunction.systemTimeDate().getTime();
            if (diff > 2 * 60 * 1000) diff = diff - 60 * 1000;
            long triggerAtTime = SystemClock.elapsedRealtime() + Math.abs(diff);    //下一次启动的时间

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
            }else {
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
            }

            Log.e("Guardian：未到6:20",String.valueOf(diff/1000) + "s");
        }else if (sysAndTarget == 2 | ( sysAndTarget == 1 & sysAndEnd == 3) ){//启动时间 =< 系统时间  < 结束时间
            //1. 如果服务器没有开启则继续访问，如果开启了但是没有座位，继续访问；服务器开放+有座位了，如果还在运行，就停止

            if (!open){//没开放
                diff = webTargetDate.getTime() - CommonFunction.systemTimeDate().getTime();
                if (diff > 5 * 60 * 1000) diff = diff - 2 * 60 * 1000;
                serverOpen();
                long triggerAtTime = SystemClock.elapsedRealtime() + Math.abs(diff);    //下一次启动的时间
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
                }else {
                    alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
                }

                Log.e("Guardian：服务器没有开放","下一次访问" + diff/1000 + "s");

            }else if (!reserveUser1){//开放了没座
                /** 每隔一秒检查一次，如果定时任务没有结束，就不再重新启动start方法，如果结束了就重新启动start方法 **/
                if (!TimingTasks.TimingTasksCreate | !TimingTasks.TimingTasksCommandRunning){
                    serverOpen();
                    Log.e("Guardian：系统已开放，用户是否有座位",String.valueOf(reserveUser1));
                }
                long triggerAtTime = SystemClock.elapsedRealtime() + Math.abs(4 * 500);    //下一次启动的时间
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
                }else {
                    alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
                }

            }else {//有座了
                if (TimingTasks.TimingTasksCreate){//如果有座之后还没有被销毁，则将其销毁
                    Log.e("Guardian：用户已有座位","停止定时服务");
                    Intent intentTimingTasks1 = new Intent(GuardianService.this,TimingTasks.class);
                    stopService(intentTimingTasks1);
                }
                if (alarmManager != null){
                    Log.e("Guardian：用户已有座位","取消定时器");
                    alarmManager.cancel(pi);
                }
            }
            if (TimingTasks.TimingTasksCreate & TimingTasks.isEndingTiming){
                Intent intentTimingTasks1 = new Intent(GuardianService.this,TimingTasks.class);
                stopService(intentTimingTasks1);
                Log.e("GuardianService定时结束",String.valueOf(diff));
                if (alarmManager != null){
                    alarmManager.cancel(pi);
                }
            }
        }else if (CommonFunction.timeCompare(CommonFunction.systemTime(),tomorrowTime)==3 &
                CommonFunction.timeCompare(CommonFunction.systemTime(),endTime) == 1){
            //系统时间 > 关闭服务时间， 小于第二天的启动时间 如果还没有预约成功则开启振动

            SharedPreferences sharedPreferences = getSharedPreferences(user1Id+"M",MODE_PRIVATE);
            reserveUser1 = sharedPreferences.getBoolean(CommonFunction.systemDelayTime(0,2), false);//记得修改为false
            Log.e("Guardian：服务超时,用户是否有座",String.valueOf(reserveUser1));

            if (!reserveUser1){//如果依旧没有座位，则开启振动
                Log.e("Guardian：服务超时","振动");
                vibrator =  (Vibrator) getSystemService(VIBRATOR_SERVICE);//获得 一个震动的服务
                long[] pattern = {500, 2000, 500, 2000};
                vibrator.vibrate(pattern, 0);
                Handler handlerThree=new Handler(Looper.getMainLooper());
                handlerThree.post(new Runnable(){
                    public void run(){
                        showDialogNotCancel("预约异常","喔唷，您的网页失踪了，网络似乎出了问题",GuardianService.this);
                    }
                });
            }
        }


        if ( (sysAndTarget == 2 | ( sysAndTarget == 1 & sysAndEnd == 3)) & open){
            if (!TimingTasks.TimingTasksCommandRunning & !reserveUser1){
                long triggerAtTime = SystemClock.elapsedRealtime() + Math.abs(2 * 1000);    //下一次启动的时间
                Intent intentp = new Intent(getApplicationContext(),GuardianService.class);
                pi = PendingIntent.getService(getApplicationContext(),1,intentp,0);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    alarmManagerFinal.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
                }else {
                    alarmManagerFinal.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
                }
                Log.e("Guardian：开启一个新的2s定时器",String.valueOf(diff));
            }
        }
        return super.onStartCommand(intent,flags,startId);
    }
    @Override//服务销毁的时候调用
    public void  onDestroy(){
        GuardianServiceStart = false;
        alarmManager.cancel(pi);
        alarmManagerFinal.cancel(pi);
        //释放电源锁
        if (wakeLock != null) {
            wakeLock.release();//?
            wakeLock = null;
        }
        super.onDestroy();
        Log.e("YSU守护服务销毁","定时任务服务已销毁");
    }

    //开启一个线程检测服务器是否可以访问
    public void serverOpen(){
        final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();
        new Thread(new Runnable() {
            @Override
            public void run() {//实现同一个cookie访问
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
                //获取网站服务器时间
                if (!open){
                    //在没有开放的时候获取网络时间判断是否开放，否则将直接计算
                    Log.e("serverOpen","获取服务器时间");
                    serverDate = getNetTime();
                }else {
                    SharedPreferences user1sharedPreferences = getSharedPreferences("diffSysNet",MODE_PRIVATE);
                    diffSysNet = user1sharedPreferences.getLong("diffSysNet",0);
                    try {
                        Log.e("serverOpen","计算服务器时间");
                        serverDate = CommonFunction.longToDate(CommonFunction.systemTimeDate().getTime() + diffSysNet,"yyyy-MM-dd HH:mm:ss");
                    } catch (ParseException e) {
                        e.printStackTrace();
                        Log.e("serverOpen","计算失败，获取服务器时间");
                        serverDate = getNetTime();
                    }
                }

                Log.e("serverOpen：系统时间",CommonFunction.systemTime());
                Log.e("serverOpen：errorTime",errorTime);

                //  从服务器得到的时间等于错误时间表示服务器没有开放
                if (CommonFunction.timeCompare(dateFormat.format(serverDate),dateFormat.format(defDate)) == 2 ){
                    open = false; Log.e("serverOpen：系统是否开放",String.valueOf(open));
                }else {
                    open = true; Log.e("serverOpen：系统是否开放",String.valueOf(open));
                }

                // 如果到了06:29:00还不能访问，则叫醒用户
                if (CommonFunction.timeCompare(CommonFunction.systemTime(),errorTime) == 2  & !open){
                    Log.e("serverOpen：6:29","不能访问网络");
                    //获得 一个震动的服务
                    long[] pattern = {500, 2000, 500, 2000};
                    vibrator.vibrate(pattern, 0);
                    Handler handlerThree=new Handler(Looper.getMainLooper());
                    handlerThree.post(new Runnable(){
                        public void run(){
                            showDialogNotCancel("网络异常","(G)无法访问服务器，请及时检查！",GuardianService.this);
                        }
                    });
                    return;
                }

                if (!open & CommonFunction.timeCompare(CommonFunction.systemTime(),webTargetTime) != 3){
                    open = true;
                }

                //3. 如果系统开放了，计算到定时任务启动还有多长时间
                if (open){
                    if (CommonFunction.timeCompare(dateFormat.format(serverDate),dateFormat.format(netError)) == 2){
                        SharedPreferences user1sharedPreferences = getSharedPreferences("diffSysNet",MODE_PRIVATE);
                        diffSysNet = user1sharedPreferences.getLong("diffSysNet",0);
                    }else {
                        diffSysNet = CommonFunction.systemTimeDate().getTime() - serverDate.getTime();
                        SharedPreferences user1sharedPreferences = getSharedPreferences("diffSysNet",MODE_PRIVATE);
                        SharedPreferences.Editor editor=user1sharedPreferences.edit();
                        editor.putLong("diffSysNet", diffSysNet);//时间差
                        editor.commit();
                    }


//                    String[] myResv = new String[]{"no","no","no","no","no"};////owner devName devId labName timeDesc
//                    try {
//                        myResv = getMyReserve(user1Id,user1Password,client);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                    if (!myResv[1].equals("no") & !myResv[4].equals("no")){
//                        return;//已有座位
//                    }

                    diff = webTargetDate.getTime() - serverDate.getTime();//06:29:45
                    if (diff < 0){
                        diff = 2 * 1000;
                        Log.e("serverOpen","重新计算定时");
                    }

                    long derror = Math.abs(CommonFunction.systemTimeDate().getTime() - serverDate.getTime());
                    if (derror > 2 * 60 * 1000){//两个时间的误差在2分钟
                        Log.e("serverOpen：误差超过两分钟","实际时间：" + dateFormat.format(CommonFunction.systemTimeDate().getTime() + diffSysNet) +
                                "系统与服务器实际时间差：" + String.valueOf(diffSysNet));
                        diff = 2 * 1000;
                    }else {
                        diff = 2 * 1000;
                        Log.e("serverOpen：服务器时间正常",String.valueOf(diff));
                    }

                    if (diff >= 60 * 1000){
                        diff = diff - 50 * 1000;
                        long h = diff / 3600/1000; long m = diff/1000 % 3600 /60; long s = diff/1000 % 60;
                        long triggerAtTime = SystemClock.elapsedRealtime() + Math.abs(diff);    //下一次启动的时间
//                        showNotification("守护服务","服务器当前时间" + dateFormat.format(serverDate) + "，距离定时任务启动时间还有" + h + "时" + m + "分" + s + "秒",
//                                getApplicationContext(),35,"channel35","定时守护");

                        Intent intent1 = new Intent(getApplicationContext(),GuardianService.class);
                        pi = PendingIntent.getService(getApplicationContext(),0,intent1,0);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
                        }else {
                            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
                        }

                        Log.e("serverOpen：定时启动 > 60s", dateFormat.format(serverDate) + h + "时" + m + "分" + s + "秒");
                        Log.e("serverOpen：定时启动 > 60s",String.valueOf(diff));

                    }else if (diff > 5 * 1000 ){
                        long h = diff / 3600/1000; long m = diff % 3600/1000 /60; long s = diff/1000 % 60;
//                        showNotification("守护服务","服务器当前时间" + dateFormat.format(serverDate) + "，距离定时任务启动时间还有" + h + "时" + m + "分" + s + "秒",
//                                getApplicationContext(),35,"channel35","定时守护");

                        long triggerAtTime = SystemClock.elapsedRealtime() + Math.abs(diff);    //下一次启动的时间
                        Intent intent1 = new Intent(getApplicationContext(),GuardianService.class);
                        pi = PendingIntent.getService(getApplicationContext(),0,intent1,0);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
                        }else {
                            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
                        }

                        Log.e("serverOpen：定时启动",dateFormat.format(serverDate) + "剩余" + h + "时" + m + "分" + s + "秒");
                        Log.e("serverOpen：定时启动",String.valueOf(diff));
                    }else {
                        diff = 4 * 500;
                        long triggerAtTime = SystemClock.elapsedRealtime() + Math.abs(diff);    //下一次启动的时间
                        Intent intent1 = new Intent(getApplicationContext(),GuardianService.class);
                        pi = PendingIntent.getService(getApplicationContext(),0,intent1,0);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
                        }else {
                            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
                        }

                        Log.e("serverOpen：定时启动",dateFormat.format(serverDate) + diff/ 1000 + "s后");
                        Log.e("serverOpen：定时启动",String.valueOf(diff));
                    }

                    //服务时间在06:29:45， 或者  系统时间在 06:29:45之后 06:40:00之前
                    if (CommonFunction.timeCompare(dateFormat.format(serverDate),dateFormat.format(webTargetDate)) == 2 |
                            (CommonFunction.timeCompare(dateFormat.format(serverDate),endTime) == 3
                                    & CommonFunction.timeCompare(dateFormat.format(serverDate),dateFormat.format(webTargetDate)) == 1) ){
                        //如果在此期间，没有预约成功并且定时任务没有被创建或者没有再运行都创建它
                        if ( (!TimingTasks.TimingTasksCreate | !TimingTasks.TimingTasksCommandRunning) & !reserveUser1){
                            Log.e("serverOpen：服务器已开放","即将启动TimingTasks");
                            Intent intentTimgTask = new Intent(GuardianService.this,TimingTasks.class);
                            startService(intentTimgTask);

                            timingSatrt = true;
                            long triggerAtTime = SystemClock.elapsedRealtime() + Math.abs(4 * 500);    //下一次启动的时间
                            Intent intent1 = new Intent(getApplicationContext(),GuardianService.class);
                            pi = PendingIntent.getService(getApplicationContext(),0,intent1,0);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
                            }else {
                                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
                            }
                            Log.e("serverOpen：6:29:45-6:45","下次启动" + diff);
                        }
                    }
                }
            }
        }).start();
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
        //String reserve = dateFormat.format(new Date());// 当前时间加40分钟,该时间格式为2019-06-01 20:25
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
    //获取我的座位状态信息2019.6.22
    public String[] getMyReserve(String myid, String mypwd, OkHttpClient client) throws IOException, JSONException {
        //1. 登录并获取我的预约信息
        String[] myResv = new String[]{"no","no","no","no","no"};//owner devName devId labName timeDesc
        String owner = ""; String devName = ""; String devId = ""; String labName = ""; String timeDesc = "";
        Request loginRequest = new Request.Builder()
                .url("http://seat.ysu.edu.cn/ClientWeb/pro/ajax/login.aspx?act=login&id=" + myid + "&pwd=" + mypwd + "&role=512&aliuserid=&schoolcode=&wxuserid=&_nocache=1551511783772")
                .build();
        client.newCall(loginRequest).execute();
        Request getMyResvRequest = new Request.Builder()
                .url("http://seat.ysu.edu.cn/ClientWeb/pro/ajax/reserve.aspx?stat_flag=9&act=get_my_resv&_nocache=1551577219949")
                .build();
        Response response = client.newCall(getMyResvRequest).execute();
        String myResvState = response.body().string();//得到座位状态数据

        //2. 解析获得的预约信息
        String data = null;
        try {
            JSONObject jsonObject = new JSONObject(myResvState);
            data = jsonObject.getString("data");
            Log.e("getMyReserve：解析我的预约",data);

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("getMyReserve：解析出错",e.toString());
        }

        if (data == null){//解析出错
            Log.e("getMyReserve","data=null");
        }else if (data.equals("[]")){//没有预约{"ret":1,"act":"set_resv","msg":"操作成功！","data":null,"ext":null}
            Log.e("getMyReserve","没有预约");
        }else {
            //座位信息
            /* {"ret":1,"act":"get_my_resv","msg":"ok","data":[{"id":"117161236","group":100008063,"name":"","title":null,"detail":null,"owner":"张三",
            "ownerAccno":"100008063","dept":null,"start":"2019-06-22 15:00","end":"2019-06-22 22:30","starts":null,"ends":null,"timeDesc":"06/22 15:00-22:30",
            "occur":"2019-06-22 13:33","ltch":0,"status":4482,"state":"<span class='orange uni_trans'>预约成功</span>",
            "states":"<span style='color:green' class='uni_trans'>预约成功</span>,<span style='color:orange' class='uni_trans'>未生效</span>,<span style='color:green' class='uni_trans'>审核通过</span>",
            "prop":null,"allDay":false,"islong":false,"szmemo":null,"szCardNo":"00036200","devId":"101440095","devName":"四阅-510","kindId":"100457197","kindName":null,
            "groupId":"100008063","groupName":null,"members":"个人预约","roomId":"101439231","roomName":"第四阅览室","labId":"101439226","labName":"六楼","campus":null,
            "devDept":null,"org":null,"orger":null,"contact":null,"phone":null,"minUser":0,"maxUser":0,"atyId":null,"atyName":null,"testId":null,"testName":null,"planId":null,
            "planName":null,"teacher":null,"teacherAccno":null,"actSN":1}],"ext":null}*/
            data = data.substring(1,data.length());
            JSONObject jsonObjectSingle = new JSONObject(data);
            owner = jsonObjectSingle.getString("owner");
            devName = jsonObjectSingle.getString("devName");
            devId = jsonObjectSingle.getString("devId");
            labName = jsonObjectSingle.getString("labName");
            timeDesc = jsonObjectSingle.getString("timeDesc");
            myResv[0] = owner; myResv[1] = devName; myResv[2] = devId; myResv[3] = labName; myResv[4] = timeDesc;
        }
        //myResv[0] = owner; myResv[1] = devName; myResv[2] = devId; myResv[3] = labName; myResv[4] = timeDesc;
        return myResv;
    }

    //获取网络时间
    public Date getNetTime(){
        try {
            //http://seat.ysu.edu.cn/clientweb/m/ic2/default.aspx
            URL url = new URL(Url);
            URLConnection uc = url.openConnection();
            uc.setReadTimeout(2500);
            uc.setConnectTimeout(2500);
            uc.connect();
            long correctTime = uc.getDate();
            webDate = new Date(correctTime);
            Log.e("getNetTime：网络时间",dateFormat.format(webDate));
        }catch (Exception e){
            Handler handlerThree7=new Handler(Looper.getMainLooper());
            handlerThree7.post(new Runnable(){
                public void run(){
                    Toast.makeText(GuardianService.this,"请重启手机或重新开启网络",Toast.LENGTH_SHORT).show();
                }
            });
            webDate = defDate;//返回错误时间 1970-01-01 01:00:00
            Log.e("getNetTime：获取网络时间出错",e.toString());
        }
        return webDate;
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
        }        dialog.show();
    }

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
                            R.drawable.seatlock))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,TimingTasks.class)},
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
                            R.drawable.seatlock))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,TimingTasks.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)//悬挂跳转
                    .build();
        }
        manager.notify(id,notification);
    }
    //登录并进行预约
    public  int loginAndSetReserve(OkHttpClient client,String dev_id,String user) throws IOException {
        //0.1. 准备工作。检查用户信息
        String reserve = dateFormat.format(new Date());// 时间格式为2019-06-01 20:25
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
        Log.e("loginAndSetReserve：登录参数",loginReturn);


        //2. 选座
        String setResvUrl = new StringBuilder().append("http://seat.ysu.edu.cn/ClientWeb/pro/ajax/reserve.aspx?dialogid=&dev_id=").append(dev_id)
                .append("&lab_id=&kind_id=&room_id=&type=dev&prop=&test_id=&term=&test_name=&start=").append(startDateAndTime).append("&end=")
                .append(endDateAndTime).append("&start_time=").append(start_time).append("&end_time=").append(end_time).append("&up_file=&memo=&act=set_resv&_=1544339248168")
                .toString();
        Request setResvRequest = new Request.Builder().url(setResvUrl).build();//准备请求
        Response setRsvResponse = client.newCall(setResvRequest).execute();//执行请求
        String setRsvResponseData = setRsvResponse.body().string();//得到响应数据
        Log.e("loginAndSetReserve：预约参数",setRsvResponseData);
        actok = parseResponseMsg(setRsvResponseData,user);

        return actok;
    }
    //显示通知,用户可以手动删除
    public void showNotification(String title, String text, Context context, int id,String channelid,String channelname){
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(channelid, channelname, NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(mChannel);
            notification = new Notification.Builder(context)
                    .setChannelId(channelid)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setColor(Color.parseColor("#00000000"))
                    .setAutoCancel(true)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setSmallIcon(R.drawable.heart)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.guardian1round))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,TimingTasksActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .build();
        }else {
            notification = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setColor(Color.parseColor("#00000000"))
                    .setAutoCancel(true)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setSmallIcon(R.drawable.heart)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.guardian1round))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,TimingTasksActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .build();
        }
        manager.notify(id,notification);
    }
    public void showNotification2(String title, String text, Context context, int id,String channelid,String channelname){
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(channelid, channelname, NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(mChannel);
            notification = new Notification.Builder(context)
                    .setChannelId(channelid)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setColor(Color.parseColor("#00000000"))
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.heart)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.guardian1round))
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
                    .setColor(Color.parseColor("#00000000"))
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.heart)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.guardian1round))
                    .setContentIntent(PendingIntent.getActivities(context,0x0001,
                            new Intent[]{new Intent(context,TimingTasksActivity.class)},PendingIntent.FLAG_CANCEL_CURRENT))
                    .build();
        }
        manager.notify(id,notification);
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
                            new Intent[]{new Intent(context,TimingTasks.class)},PendingIntent.FLAG_CANCEL_CURRENT))//跳转
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
                            new Intent[]{new Intent(context,TimingTasks.class)},PendingIntent.FLAG_CANCEL_CURRENT))//跳转
                    .build();
        }
        String tag = id + "float";
        manager.notify(tag,id,notification);
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
                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)//振动
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.timingtasknotigy)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.notify7))
                    .setContentIntent(PendingIntent.getActivities(context,0x0001,
                            new Intent[]{new Intent(context,TimingTasks.class)},PendingIntent.FLAG_CANCEL_CURRENT))//跳转
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,TimingTasks.class)},
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
                    .setContentIntent(PendingIntent.getActivities(context,0x0001,
                            new Intent[]{new Intent(context,TimingTasks.class)},PendingIntent.FLAG_CANCEL_CURRENT))//跳转
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,TimingTasks.class)},
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
//            showDialog("座位号异常","已自动更改为"+result[0],GuardianService.this);
        }
        return result;
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

}
