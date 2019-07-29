package com.example.uniconseat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
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

//通知的id段 10-19
public class GrabSeat extends Service {
    public GrabSeat() {
    }
    /*全局通用信息*/
    static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
    static String Url = "http://202.206.242.87/ClientWeb/xcus/ic2/Default.aspx";
    static Date webTargetDate;
    static {
        try {
            webTargetDate = df.parse(CommonFunction.systemDelayTime(0,2)+ " " + "06:30:00");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    static Date webDate;
    static Date defDate;
    static {
        try {
            defDate = df.parse("1970-07-01 01:00:00");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }//date格式的判错时间


    //  全局信息传递
    private String roomname; //用户信息
    private String finalStateTime,finalStateDevname; //最终座位状态信息
    private String ToastText = "";//预约状态提示信息
    public String prefSeat,begintime;//首选座位  开始时间
    public int begin,prefSeatState=0;//开始时间整型，预约指定座位的结果。0：可以进行预约指定座位；1：用户没有指定座位/指定座位输入错误不存在/预约失败
    private  String[] emptyTitle = new String[500];  //空闲座位信息
    private  String[] emptyDevid = new String[500];
    private  int empty_num = 0; //空闲座位数量
    public int actok = 0;//表示操作的结果状态，分为3种


    String seatUrl = "http://202.206.242.87/ClientWeb/xcus/ic2/Default.aspx" ;

    //    url共用字段
    String urlCommonFirstPara = "http://202.206.242.87/ClientWeb/pro/ajax/device.aspx?byType=devcls&classkind=8&display=fp&md=d&room_id=";
    String urlCommonSecondPara = "&purpose=&selectOpenAty=&cld_name=default&date=";
    String urlCommonThirdPara = "&act=get_rsv_sta&_=";
    String[] userInfo = new String[5];

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
        return mBinder;
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
                .setOngoing(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.drawable.monitorlarge))
                .setFullScreenIntent(PendingIntent.getActivities(GrabSeat.this,0,
                        new Intent[]{new Intent(GrabSeat.this,GrabSeat.class)},
                        PendingIntent.FLAG_CANCEL_CURRENT),false)
                .setContentIntent(pi)
                .build();
        startForeground(10,notification);
    }
    @Override//每次服务启动的时候调用
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.e("YSU启动","后台监控座位服务已启动");

        return super.onStartCommand(intent,flags,startId);
    }
    @Override//服务销毁的时候调用
    public void  onDestroy(){
        super.onDestroy();
        Log.e("YSUonDestroy","后台监控座位服务已销毁");
    }

    private GrabSeatBinder mBinder = new GrabSeatBinder();
    class GrabSeatBinder extends Binder{
        private GrabSeat.GrabSeatBinder grabSeatBinder;//绑定服务
        private OkHttpClient clients;
        private String result;
        private int succeed;
        public boolean stopProcess = false;//是否终止前台服务进程
        int Id;
        int Length;
        String TAG;
        String[] re = new String[4];//用户信誉信息：姓名、班级、信誉积分、受限时间
        public boolean creditScore = true;//用户信誉是否受限


        //把活动中的值传递进来，供服务使用
        public void assignment(String[] userInfoa, String begintimea, String prefSeata, String roomnamea){
            userInfo = userInfoa;
            begintime = begintimea;
            prefSeat = prefSeata;
            roomname = roomnamea;
            Log.e("YSUmsg",roomname+prefSeat+begintime);
            Log.e("YSUmsg",userInfo[0]+userInfo[1]+userInfo[2]);
        }
        //把是否中断服务的参数传进来
        public void stopParam(boolean stop){
            stopProcess = stop;
            Log.e("YSUSTOP传值完毕","true");
        }

        //运行服务，执行选座操作
        public int runService(){
            final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();
            Log.e("YSU全局cookie","msg");
            new Thread(new Runnable() {
                @Override
                public void run() {
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
                        Log.e("计算时间","msg");
                        //获取服务开始时间，并计算服务应该结束的时间
                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String endTime = df.format(new Date().getTime()+ 15 * 60 * 1000);
                        //开始第一次选座
                        clients = obtainAndParseStatus(client);// 获取并解析座位状态数据
                        result =loadRsvSta("data");
                        succeed = parseStatusOfEachSeatAndReserve(result,clients);

                        //如果选座失败，并且不停止服务时运行,则监测15分钟
                        while (succeed!=1 & !stopProcess){//选座没有成功
                            //判断是时间是否到了15分钟，如果没有，则随机定义x秒之后再次访问服务器
                            String startTime = CommonFunction.systemTime();//获取服务运行的时间
                            if (CommonFunction.timeCompare(startTime,endTime)==3){//1 结束时间小于开始时间 2 开始时间与结束时间相同 3 结束时间大于开始时间
                                Random randdiff = new Random();
                                int i = randdiff.nextInt(500);
                                Log.e("我是随机数",Integer.toString(i));
                                Log.e("diff = "+ Math.abs(i),"**************************");
                                long triggerAtTime = SystemClock.elapsedRealtime() + Math.abs(i);    //下一次启动的时间
                                Log.e("重新计算定时启动时间",String.valueOf(triggerAtTime));

                                /*  获取定时服务  */
//                                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
//                                Intent intent1 = new Intent(GrabSeat.this,GrabSeat.class);
//                                PendingIntent pi = PendingIntent.getService(GrabSeat.this,0,intent1,0);
//                                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
//                                break;
                                repeat();
//                                tips();
                            }else {
                                showNotification2("后台服务","后后台服务已关闭，未预约到座位！",GrabSeat.this,19,50);
                                stopSelf();
                            }
                        }
                        if (succeed == 1)
                        {
                            showNotification("座位锁定",ToastText,GrabSeat.this,12);
                        }
                        Log.e("好了好了，我完成了","-------------------");
                    }catch (Exception e){
                        e.printStackTrace();
                        //访问异常，无法连接服务器！
                        Log.e("YSU访问异常，无法连接服务器！",e.toString());
                        if (cookieStore.size()==0){
                            Handler handlerThreec=new Handler(Looper.getMainLooper());
                            handlerThreec.post(new Runnable(){
                                public void run(){
                                    Toast.makeText(GrabSeat.this,"后台服务未启动,请检查网络...",Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                }
            }).start();
            return succeed;
        }
        //定时监测
        int repeat() throws IOException, JSONException {
            clients = obtainAndParseStatus(clients);
            result =loadRsvSta("data");
            succeed = parseStatusOfEachSeatAndReserve(result,clients);
            Log.e("YSU定时监测","返回操作结果");
            return  succeed;
        }

        /*显示通知,用户可以手动删除*/
        /* 样式0： 只悬停、不振动、不跳转  不回状态栏  */
        public void showNotification0(String title, String text, Context context, int id,int length){
            final NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            //悬停
            Notification notification2 = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setColor(Color.parseColor("#00000000"))
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.monitor)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.monitorlarge))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .build();
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
        public void showNotification1(String title, String text, Context context, int id,int length){
            final NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            //普通
            Notification notification = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setColor(Color.parseColor("#00000000"))
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.monitor)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.monitorlarge))

                    .build();
            String tag = id + "float";
            manager.notify(id,notification);
            //悬停
            Notification notification2 = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setColor(Color.parseColor("#000000"))
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.monitor)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.monitorlarge))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .build();
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
        public void showNotification2(String title, String text, Context context, int id,int length){
            final NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            Notification notification = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setColor(Color.parseColor("#00000000"))
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.monitor)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.monitorlarge))

                    .build();
            String tag = id + "float";
            manager.notify(id,notification);

            Notification notification2 = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setColor(Color.parseColor("#000000"))
                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)//振动
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.monitor)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.monitorlarge))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .build();
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
        public void showNotification3(String title, String text, Context context, int id ,int length){
            final NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            Notification notification = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setColor(Color.parseColor("#00000000"))
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.monitor)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.monitorlarge))
                    .setContentIntent(PendingIntent.getActivities(context,0x0001,
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},PendingIntent.FLAG_CANCEL_CURRENT))//跳转
                    .build();
            String tag = id + "float";
            manager.notify(tag,id,notification);

            Notification notification2 = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setColor(Color.parseColor("#00000000"))
                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)//振动
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.monitor)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.monitorlarge))
                    .setContentIntent(PendingIntent.getActivities(context,0x0001,
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},PendingIntent.FLAG_CANCEL_CURRENT))//跳转
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .build();
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
        public void showNotification4(String title, String text, Context context, int id){
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            Notification notification = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(Notification.DEFAULT_SOUND)//默认铃声
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setColor(Color.parseColor("#00000000"))
                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)//振动
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.monitor)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.monitorlarge))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)//悬挂跳转
                    .build();
            manager.notify(id,notification);
        }
        /*后台服务选定座位*/
        public void showNotification(String title,String text, Context context,int id){
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            Notification notification = new NotificationCompat.Builder(context)
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
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},PendingIntent.FLAG_CANCEL_CURRENT))
                    .build();
            manager.notify(id,notification);
        }
        //Dialog提示
        public void showDialog(String title,String msg, Context context){
//        AlertDialog.Builder alertDialog = new AlertDialog.Builder(TimingTasks.this, R.style.dialog_style);
            android.support.v7.app.AlertDialog.Builder builder=new android.support.v7.app.AlertDialog.Builder(context,R.style.dialog_style);
            builder.setIcon(R.drawable.applygreen);
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
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            dialog.show();
        }
        public void showDialogNotCancel(String title, String msg, Context context){
//        AlertDialog.Builder alertDialog = new AlertDialog.Builder(TimingTasks.this, R.style.dialog_style);
            android.support.v7.app.AlertDialog.Builder builder=new android.support.v7.app.AlertDialog.Builder(context,R.style.dialog_style);
            builder.setIcon(R.drawable.applygreen);
            builder.setTitle(title);
            builder.setMessage(msg);
            builder.setCancelable(false);
            builder.setPositiveButton("知道了",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });
            android.support.v7.app.AlertDialog dialog=builder.create();
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            dialog.show();
        }
        //处理预约结果，并提示
        public void tips(){
            //在操作状态1/2/3处应该应用此函数，用于构建提示信息的具体内容，否则ToaskText=null
            switch (actok){
                case 1://预约成功
                    //该信息重要等级为 HIGH，用户必须与之交互
                    prefSeatState = 0;//对指定座位的操作状态置0
                    ToastText = userInfo[0] + "已预约成功!" + finalStateDevname + "开始时间:" + finalStateTime;
                    break;
                case 2://已有预约
                    //该信息重要等级为 LOW，点击空白处可取消Dialog
                    Handler handlerThree=new Handler(Looper.getMainLooper());
                    handlerThree.post(new Runnable(){
                        public void run(){
                            showDialog("预约小提示",ToastText,GrabSeat.this);
                        }
                    });
                    break;
                case 3://系统未开放
                    //该信息重要等级为 LOW，点击空白处可取消Dialog
                    Handler handlerThreec=new Handler(Looper.getMainLooper());
                    handlerThreec.post(new Runnable(){
                        public void run(){
                            showDialog("预约小提示","请在6:30之后再进行预约; 预约端当前时间"+df.format(getNetTime()),GrabSeat.this);
                        }
                    });
                    break;
                case 4://积分不足
                    //该信息重要等级为 HIGH，用户必须与之交互
                    Handler handlerThreed=new Handler(Looper.getMainLooper());
                    handlerThreed.post(new Runnable(){
                        public void run(){
                            showDialogNotCancel("预约提示",re[0]+":剩余积分为"+re[2]+",在"+re[3]+"期间被禁止预约",GrabSeat.this);
                        }
                    });
                    break;
                case 5://预约冲突
                    //该信息重要等级为 LOW，点击空白处可取消Dialog
                    Handler handlerThreee=new Handler(Looper.getMainLooper());
                    handlerThreee.post(new Runnable(){
                        public void run(){
                            showDialog("预约小提示","与现有预约存在冲突",GrabSeat.this);
                        }
                    });
                    break;
                case 6://其他信息
                    if (CommonFunction.regexMatcher("(参数有误)",ToastText)){
                        Handler handlerThreef=new Handler(Looper.getMainLooper());
                        handlerThreef.post(new Runnable(){
                            public void run(){
                                Toast.makeText(GrabSeat.this,"座位号不存在",Toast.LENGTH_SHORT).show();
                            }
                        });
                    }else if (CommonFunction.regexMatcher("(未登录)",ToastText)){
                        Handler handlerThreeg=new Handler(Looper.getMainLooper());
                        handlerThreeg.post(new Runnable(){
                            public void run(){
                                Toast.makeText(GrabSeat.this,"用户名/密码错误",Toast.LENGTH_SHORT).show();
                            }
                        });
                    }else {
                        Handler handlerThreeh=new Handler(Looper.getMainLooper());
                        handlerThreeh.post(new Runnable(){
                            public void run(){
                                showDialog("预约提示",ToastText,GrabSeat.this);
                            }
                        });
                    }
                    break;
                default:
                    break;
            }
        }
        //解析座位id
        public String[] parseSeatId(String room,String id){
            //e: room = 四阅      id = 209
            String[] roomNameArray;
            String[] idArray;
            String[] result = new String[3];//四阅-209/order/address
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
                Log.e("YSUrr",rr);
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
                prefSeat = result[0].substring(3,6);//自动更改并显示
            }
            return result;
        }
        //遍历所有座位
        public int ergodicAllSeatAfterPrefSeat(String room,String order,OkHttpClient client,int[] storeState) throws IOException {
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
            int s ,r = 0;
            for (int indey = or; indey < idArray.length + or; indey ++){
                if (indey>=idArray.length){
                    indey = indey - idArray.length;
                }
                if (storeState[indey] == 1){
                    ps = roomNameArray[indey];//座位名称
                    finalStateDevname = ps;//最终预约的座位
                    address = idArray[indey];//座位id
                    s = loginAndSetReserve(client,address);//登录并进行预约,处理返回值，并决定是否结束
                    if (s == 1 & indey == or){//如果对指定座位预约成功，则指定座位的操作状态置1
                        prefSeatState = 1;
                        r = 1;
                        tips();//在操作状态1/2/3处应该应用此函数，用于构建提示信息的具体内容，否则ToaskText=null
                        return r;
                    }else if (s == 1 | s == 2 ){//预约成功/已有预约
                        r = 1;
                        tips();
                        return r;
                    }else if (s == 3){//系统未开放
                        tips();
                        return r;//r=0;
                    }else if (s == 5){//预约冲突，检索下一个座位
                        continue;
                    }else if (s == 6){
                        //tips();
                        break;
                    }
                    Log.e(ps,String.valueOf(s));
                }
            }
            return r;
        }
        //登录并进行预约   reture 1/2/3/4/5/6 对应不同的操作结果
        int loginAndSetReserve(OkHttpClient client, String dev_id) throws IOException {
            //1.    设定预约时间，如果用户没有指定时间，则从40分钟之后开始
            if (begintime.equals("")){
                begintime = "40";
                begin = Integer.parseInt(begintime);
            }else{
                begin = Integer.parseInt(begintime);
            }
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String reserve = df.format(new Date().getTime()+ begin * 60 * 1000);// 当前时间加40分钟,该时间格式为2019-06-01 18:25
            String date = reserve.substring(0,10);//日期2019-06-01
            String time = reserve.substring(11,16);//预约开始时间     例：18:25
            finalStateTime = time;
            //2.    日期时间格式整合
            String start = new StringBuilder().append(CommonFunction.getMatcher("(.*) ", reserve)).append("+")
                    .append(CommonFunction.getMatcher("(.*):", time)).append("%3A").append(CommonFunction.getMatcher(":(.*)", time))
                    .toString();//2019-06-04+11%3A28
            String end = CommonFunction.getMatcher("(.*) ",reserve)+"+22%3A20";//2019-06-04+22%3A30
            String start_time = CommonFunction.getMatcher("(.*):",time)+CommonFunction.getMatcher(":(.*)",time);//start_time=1128
            String end_time = "2220";//end_time=2230
            //3.    登录
            String loginUrl = new StringBuilder().append("http://seat.ysu.edu.cn/ClientWeb/pro/ajax/login.aspx?act=login&id=").append(userInfo[1]).append("&pwd=")
                    .append(userInfo[2]).append("&role=512&aliuserid=&schoolcode=&wxuserid=&_nocache=1551511783772")
                    .toString();
            //      3.1 向服务器发送登录请求，包括student ID, password
            Request loginRequest = new Request.Builder().url(loginUrl).build();//构建登录请求
            Response loginResponse = client.newCall(loginRequest).execute();//执行登录请求
            //String loginReturn = loginResponse.body().string();

            //      3.2 向服务器发送选座请求，包括座位id  开始时间和结束时间，并接受响应数据
            String setResvUrl = new StringBuilder().append("http://seat.ysu.edu.cn/ClientWeb/pro/ajax/reserve.aspx?dialogid=&dev_id=").append(dev_id)
                    .append("&lab_id=&kind_id=&room_id=&type=dev&prop=&test_id=&term=&test_name=&start=").append(start).append("&end=")
                    .append(end).append("&start_time=").append(start_time).append("&end_time=").append(end_time).append("&up_file=&memo=&act=set_resv&_=1544339248168")
                    .toString();
            Request setResvRequest = new Request.Builder().url(setResvUrl).build();
            Response setRsvResponse = client.newCall(setResvRequest).execute();
            //{"ret":0,"act":"set_resv","msg":"请在6:30之后再进行预约","data":null,"ext":null}
            String setRsvResponseData = setRsvResponse.body().string();//服务器返回的数据

            //解析操作状态  预约冲突返回信息示例：{ret: 0, act: "set_resv", msg: "2019-06-04您在【2019年06月04日】已有预约，当日不能再预约", data: null, ext: null}
            //              预约成功返回的信息
            String act_ret = CommonFunction.parseSingleLabel(setRsvResponseData,"ret");
            String act_name = CommonFunction.parseSingleLabel(setRsvResponseData,"act");
            String act_msg = CommonFunction.parseSingleLabel(setRsvResponseData,"msg");
            //构建预约冲突时的返回信息
            String msg_error = date + "您在【" + date.substring(0,4) + "年" + date.substring(5,7) +"月" + date.substring(8,10) + "日】已有预约，当日不能再预约";

            //5.    预约结果分析    除了以下1,2两种状态其他状态均为当前用户没有座位
            actok = parseResponseMsg(setRsvResponseData);
            return actok;
        }
        //解析预约结果
        public int parseResponseMsg(String data){
            //1.    解析操作状态  预约冲突返回信息示例：{ret: 0, act: "set_resv", msg: "2019-06-04您在【2019年06月04日】已有预约，当日不能再预约", data: null, ext: null}
            //              预约成功返回的信息：{ret: 1, act: "set_resv", msg: "操作成功！", data: null, ext: null}
            String act_ret = CommonFunction.parseSingleLabel(data,"ret");
            String act_name = CommonFunction.parseSingleLabel(data,"act");
            String act_msg = CommonFunction.parseSingleLabel(data,"msg");
            //构建预约冲突时的返回信息
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String reserve = df.format(new Date().getTime()+ begin * 60 * 1000);// 当前时间加40分钟,该时间格式为2019-06-01 18:25
            String date = reserve.substring(0,10);//日期2019-06-01
            String msg_error = date + "您在【" + date.substring(0,4) + "年" + date.substring(5,7) +"月" + date.substring(8,10) + "日】已有预约，当日不能再预约";

            //2.    预约结果分析    除了以下1,2两种状态其他状态均为当前用户没有座位
            if (act_name.equals("set_resv") & act_msg.equals("操作成功！") & act_ret.equals("1")){//操作状态1：预约成功
                actok = 1;
                ToastText = userInfo[0] + "已预约成功!" + finalStateDevname + "开始时间:" + finalStateTime;
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
        //解析每个座位状态信息
        int parseStatusOfEachSeatAndReserve(String dataArray, OkHttpClient client) throws JSONException, IOException {
            int state = -1; //表示预约状态。0：无预约；1：有预约
            JSONArray jsonArray = new JSONArray(dataArray);
            int[] storeState = new int[jsonArray.length()];//存储每个座位的状态 不可预约：1；可预约：0.
            //0. 座位余量查询 empty_numm
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

            /*  1. 用户指定了首选座位    */
            String[] res = new String[3];
            if (!prefSeat.equals("") & prefSeatState==0 & empty_num != 0) {
                Log.e("YSUprefSeat", prefSeat);
                //showNotification0("选座提示", "检索指定座位预约状态", GrabSeat.this, 3, 15);
                //1.1 先对指定座位进行操作
                res = parseSeatId(roomname, prefSeat);
                Log.e("YSUrooname", roomname);
                int r = loginAndSetReserve(client,res[2]);

                Log.e("YSUres", res[2]);
                if (r == 5) {//预约冲突
                    prefSeatState = 1;//指定座位操作失败
                    showNotification0("选座提示", "与现有预约存在冲突", GrabSeat.this, 3, 15);
                } else if (r == 1) {
                    finalStateDevname = res[0];
                    tips();//在操作状态1/2处应该应用此函数，用于构建提示信息的具体内容，否则ToaskText=null
                    return 1;
                } else if (r == 2) {
                    tips();
                    return 0;
                }
            }

            /*  2. 用户没有指定首选座位，或首选座位未能预约成功    */
            if ((prefSeat.equals("") | prefSeatState==1) & empty_num != 0){
                Log.e("YSU解析每个座位的状态","寻找空闲座位");
                if (prefSeat.equals("")){//如果没有指定，则随机生成
                    Random a = new Random();
                    int aa = a.nextInt(jsonArray.length());
                    res[1] = String.valueOf(aa);
                    Log.e("YSU我是随机数",Integer.toString(aa));
                }
                //获取首选座位在该阅览室座位中的序号 res[3] = {"四阅-209","12","101440034"} ps(prefSeat)/order/address
                //res = parseSeatId(roomname,prefSeat);
                state = ergodicAllSeatAfterPrefSeat(roomname,res[1],client,storeState);
                //从指定座位开始，按顺序遍历所有座位，直至遍历到当前座位结束，并返回预约状态
            }
            //直到检索完该阅览室的所有座位，用户的预约状态为0，空闲座位为0，则提示该阅览室当前无所需座位
            if (empty_num == 0){
                state = 0;//调试中为1，默认为0
            }
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
                Log.e("YSU网络时间",df.format(webDate));
            }catch (Exception e){
                Handler handlerThreef=new Handler(Looper.getMainLooper());
                handlerThreef.post(new Runnable(){
                    public void run(){
                        Toast.makeText(GrabSeat.this,"请重启手机或重新开启网络",Toast.LENGTH_SHORT).show();
                    }
                });
                webDate = defDate;
                Log.e("YSU尝试获取网络时间出错",e.toString());
            }
            return webDate;
        }
        //从服务器获取座位状态信息（包含解析与保存步骤）
         OkHttpClient obtainAndParseStatus(OkHttpClient client) throws IOException {
             //1.    设定预约时间，如果用户没有指定时间，则从40分钟之后开始
             if (begintime.equals("")){
                 begintime = "40";
                 begin = Integer.parseInt(begintime);
             }else{
                 begin = Integer.parseInt(begintime);
             }
             SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
             String reserve = df.format(new Date().getTime()+ begin * 60 * 1000);// 当前时间加40分钟,该时间格式为2019-06-01 18:25
             String date = reserve.substring(0,10);//日期2019-06-01
             String time = reserve.substring(11,16);//预约开始时间     例：18:25
             //2.    日期时间格式整合
             String fr_start = new StringBuilder().append(CommonFunction.getMatcher("(.*):", time)).append("%3A")
                     .append(CommonFunction.getMatcher(":(.*)", time))
                     .toString();
             String fr_end = "22%3A00";
             //3.    获取系统时间
            /*SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String rsvTime = df.format(new Date());// 当前时间加40分钟,该时间格式为2019-06-01 18:25
            String date = rsvTime.substring(0,10);*/
             String roomIdUrl = urlCommonFirstPara.concat(userInfo[3]).concat(urlCommonSecondPara)
                     .concat(date).concat("&fr_start=").concat(fr_start).concat("&fr_end=").concat(fr_end)
                     .concat(urlCommonThirdPara).concat(userInfo[4]);//目标阅览室url
            // 访问服务器座位状态信息
            Request getRsvSta = new Request.Builder().url(roomIdUrl).build();
            Response rsvStaRsponse = client.newCall(getRsvSta).execute();
            String rsvStateData = rsvStaRsponse.body().string();
            //从服务器返回的数据中解析出座位状态信息，并保存到data
            parseJSONFromResponse(rsvStateData);
             Log.e("YSU从服务器获取并保存座位状态信息","成功");
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
                saveResvSta(data,"data");
                result = data;
            }catch (Exception e){
                e.printStackTrace();
            }
            return  result;
        }
        //读取座位状态信息
        String loadRsvSta(String st){
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
        void  saveResvSta(String rsvstainfo, String filename){
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
        //解析用户积分信息
        private String parseJSONResponse(String jsonData){
            String result = "";
            try {
                JSONObject jsonObject = new JSONObject(jsonData);
//            String ret = jsonObject.getString("ret");
//            String act = jsonObject.getString("act");
//            String msg = jsonObject.getString("msg");
//            String ext = jsonObject.getString("ext");
                String data = jsonObject.getString("data");
                result = data;
            }catch (Exception e){
                e.printStackTrace();
            }
            return  result;
        }
        public void parseJSONScore(String data){
            String name = CommonFunction.parseSingleLabel(data,"name");//x姓名
            String cls = CommonFunction.parseSingleLabel(data,"cls");//班级
            String credit = CommonFunction.parseSingleLabel(data,"credit");//信誉
            //credit: [["个人预约制度","0","300","2019-06-19至2019-06-22"]]//信誉积分为0时的返回信息
            //credit = "[[\"个人预约制度\",\"200\",\"300\",\"\"]]";//信誉积分不为0时的返回信息
//        credit = "[[\"个人预约制度\",\"0\",\"300\",\"2019-06-19至2019-06-22\"]]";//测试用
            Log.e("YSUcredit",credit);
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
                Handler handlerThreec=new Handler(Looper.getMainLooper());
                handlerThreec.post(new Runnable(){
                    public void run(){
                        Toast.makeText(GrabSeat.this,re[0]+"  当前积分:"+re[2],Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
        public OkHttpClient getCredit(OkHttpClient client) throws IOException {
            //1. 登录
            String loginUrl = new StringBuilder().append("http://seat.ysu.edu.cn/ClientWeb/pro/ajax/login.aspx?act=login&id=").append(userInfo[1]).append("&pwd=")
                    .append(userInfo[2]).append("&role=512&aliuserid=&schoolcode=&wxuserid=&_nocache=1551511783772")
                    .toString();
            Request loginRequest = new Request.Builder().url(loginUrl).build();//向服务器发送登录请求，包括ID, password
            Response loginResponse = client.newCall(loginRequest).execute();//执行登录请求
            String loginReturn = loginResponse.body().string();//得到响应数据
            Log.e("YSUloginReturn",loginReturn);
            parseJSONScore(parseJSONResponse(loginReturn));
            if (!creditScore){//积分不足
                actok = 4;
                Log.e("YSUactod","4");
            }
            return client;
        }
    }


}
