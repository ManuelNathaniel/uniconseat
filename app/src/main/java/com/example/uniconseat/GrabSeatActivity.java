package com.example.uniconseat;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
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


public class GrabSeatActivity extends BaseActivity {
    static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
    /*全局通用信息*/
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
    static String TAG;//notification用的全局信息
    static int Id;
    static int Length;
    static String[] re = new String[4];//用户信誉信息：姓名、班级、信誉积分、受限时间
    private static final int OPENPROGRESS = 10;
    private static final int CLOSEPROGRESS = 0;
    private static final int SEATERROR = 1;//座位号异常
    private static final int RESPONSEREQUEST = 2;//相应请求
    private static final int GETCREDIT = 3;//获取积分信息
    private static final int ACCESSDATA = 4;//获取座位数据
    private static final int RESERVE = 5;//正在预约
    private static final int CREDITINFO = 6;//积分信息
    private static final int PARSEDATA = 7;//解析座位信息
    public static String[] grabArray = new String[4];//候选座位


    /*   全局信息标志位   */
    public boolean creditScore = true;//用户信誉是否受限
    public boolean Continuousmonitor = true;//判定是否进行了后台持续监控
    public boolean isServiceStart = false;//服务是否被启动
    private static String finalStateTime,finalStateDevname; //最终座位状态信息  finalStateTime在登录预约的时候被赋值  finalStateDevname在解析座位状态的时候被赋值
    private static String ToastText = "";//预约状态提示信息
    public int prefSeatState=0;//预约指定座位的结果。0：可以进行预约指定座位；1：用户没有指定座位/指定座位输入错误不存在/预约失败
    private static int empty_num = 0; //空闲座位数量
    public static int actok = 0;//表示操作的结果状态，分为5种
    public boolean sessionNull = false;//会话为空时表示登录失败

    //  全局信息传递
    private static String username,studentid,password,roomid,roomname; //用户信息
    public String prefSeat,begintime,backtime;//首选座位  开始时间
    public int begin;//开始时间整型，
    private static String[] emptyTitle = new String[500];  //空闲座位信息
    private static String[] emptyDevid = new String[500];
    private Intent ServiceIntent;
    public int serviceMsg = 0;//后台服务传回的结果
    static String[] userInfo= new String[5];//userInfo中分别存储 username studentid password roomid url字段最后部分
    public String errorCode;

    Vibrator vibrator;//手机振动器
    private GrabSeat.GrabSeatBinder grabSeatBinder;//绑定服务
    private RelativeLayout relativeLayout;
    private TextView progressTextView;
    private ProgressBar progressBar;

    // url共用字段
    String urlCommonFirstPara = "http://202.206.242.87/ClientWeb/pro/ajax/device.aspx?byType=devcls&classkind=8&display=fp&md=d&room_id=";
    String urlCommonSecondPara = "&purpose=&selectOpenAty=&cld_name=default&date=";
    String urlCommonThirdPara = "&act=get_rsv_sta&_=";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grab_seat);//初始化界面
        vibrator=(Vibrator)getSystemService(Service.VIBRATOR_SERVICE);//获取系统的Vibrator服务

        //进度条
        relativeLayout = findViewById(R.id.progressBar);
        progressTextView = findViewById(R.id.progresstips);
        progressBar = findViewById(R.id.progress);

//        relativeLayout.setVisibility(View.VISIBLE);
//        relativeLayout.bringToFront();
//        progressTextView.bringToFront();
//        progressBar.bringToFront();

        //启动公告滚动条
        AutoScrollTextView autoTips2 = (AutoScrollTextView)findViewById(R.id.tip2);
        autoTips2.init(getWindowManager());
        autoTips2.startScroll();

        AutoScrollTextView autoTips3 = (AutoScrollTextView)findViewById(R.id.tip3);
        autoTips3.init(getWindowManager());
        autoTips3.startScroll();

        AutoScrollTextView autoTips4 = (AutoScrollTextView)findViewById(R.id.tip4);
        autoTips4.init(getWindowManager());
        autoTips4.startScroll();

        //悬浮按钮点击事件
        FloatingActionButton fab = findViewById(R.id.comfab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, R.string.grab_action, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        spinnerRoomId();//监听阅览室选择框
        printUserInfo();//显示上次保存的用户信息
        grabSeat();//监听选座按钮
        grabSave();//保存数据按钮
        cancelSeat();//中断服务
    }

    @Override
    public void onBackPressed(){
        if (MainActivity.mainActivityStart){
            super.onBackPressed();
        }else {
            Intent intentMain= new Intent(getApplicationContext(),MainActivity.class);
            startActivity(intentMain);
        }
    }

    //提交按钮监听事件
    public void grabSeat(){
        ImageButton grabButton = findViewById(R.id.grab_button);
        grabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //getUserInfoFromScreen();//从屏幕获取用户信息并保存
                if (getUserInfoFromScreen()){
                    if (prefSeat.isEmpty()){
                        //选座操作在此对话框函数中
                        autoFillDialog("座位号","座位为空，是否需要随机指定？",GrabSeatActivity.this);
                    }else {
                        runGrabPro();//执行选座操作
                    }
                }
            }
        });
    }
    //保存数据
    public void grabSave() {
        ImageButton grabSaveButton = findViewById(R.id.grab_save_button);
        grabSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //getUserInfoFromScreen();//从屏幕获取用户信息并保存
                if (getUserInfoFromScreen()){
                    if (prefSeat.isEmpty()){
                        //选座操作在此对话框函数中
                        autoFillDialog("座位号","座位为空，是否需要随机指定？",GrabSeatActivity.this);
                    }else {
                        Toast.makeText(getApplicationContext(),"保存成功",Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
    //停止后台服务
    public void cancelSeat(){
            ImageButton exitButton = findViewById(R.id.exit_button);
            exitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ((Continuousmonitor & isServiceStart) | GrabSeatBackService.GrabService){//服务一经绑定，就会开始运行
                        //如果同时调用了startService和bindService则必须同时当上述两种条件同时不满足时，服务才会被销毁
//                        if (!grabSeatBinder.stopProcess){
//                            grabSeatBinder.stopParam(true);
//                        }
//                        unbindService(connection);
//                        stopService(ServiceIntent);
                        if (GrabSeatBackService.alarmManager != null){
                            GrabSeatBackService.alarmManager.cancel(GrabSeatBackService.pi);
                        }
                        Intent intent = new Intent(GrabSeatActivity.this,GrabSeatBackService.class);
                        stopService(intent);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showDialog("后台监控","监控服务已关闭，感谢您的使用！",GrabSeatActivity.this);
                            }
                        });
                        Log.e("YSU停止服务","serviceservice is stop");
                    }
                    else {
                        if (GrabSeatBackService.alarmManager != null){
                            GrabSeatBackService.alarmManager.cancel(GrabSeatBackService.pi);
                        }
                        ToastCustom.passValue(2000,1000,2,0,100);
                        ToastCustom.getInstance(GrabSeatActivity.this).show("您还没有启动监控服务", 2000);
                        Log.e("YSU没有启动服务","serviceservice is stop");
                    }
                }
            });
        }
    //阅览室选择Spinner
    public void spinnerRoomId(){
        Spinner spinner = (Spinner) findViewById(R.id.roomidspinner);
        //初始化先前用户的输入值
        int index = 2;
        SharedPreferences spinnerDefaultRoomIdpref = getSharedPreferences("userInfo",MODE_PRIVATE);
        roomid = spinnerDefaultRoomIdpref.getString("roomid","101439231");//101439231是第四阅览室id
        //  将roomid转化为中文，显示在对应的输入框中
        if (roomid.equals("100457211")){
            roomname = "一阅";
            index = 1;
        }else if (roomid.equals("100457213")){
            roomname = "二阅";
            index = 2;
        }else if (roomid.equals("101439229")){
            roomname = "三阅";
            index = 3;
        }else if (roomid.equals("101439231")){
            roomname = "四阅";
            index = 4;
        }else if (roomid.equals("101439233")){
            roomname = "五阅";
            index = 5;
        }else if (roomid.equals("100457221")){
            roomname = "树华A";
            index = 6;
        }
        spinner.setSelection(index);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] room = getResources().getStringArray(R.array.roomname);
                roomname = room[position];
                switch (roomname){
                    case "一阅":
                        roomid = "100457211";
                        break;
                    case "二阅":
                        roomid = "100457213";
                        break;
                    case "三阅":
                        roomid = "101439229";
                        break;
                    case "四阅":
                        roomid = "101439231";
                        break;
                    case "五阅":
                        roomid = "101439233";
                        break;
                    case "树华A":
                        roomid = "100457221";
                        break;
                    default:
                        Toast.makeText(GrabSeatActivity.this,"重新选择阅览室",Toast.LENGTH_SHORT).show();
                        break;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //当所有选项都没有被选择时触发
            }
        });

    }
    //后台持续监控Dialog
    public void alertDialog(String title,String msg, Context context){
        android.support.v7.app.AlertDialog.Builder builder=new android.support.v7.app.AlertDialog.Builder(context,R.style.dialog_style);
        builder.setIcon(R.drawable.applygreen);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Continuousmonitor = true;//持续监控标志
                isServiceStart = true;//服务是否启动标志
//                ServiceIntent = new Intent(GrabSeatActivity.this,GrabSeat.class);
//                bindService(ServiceIntent,connection,BIND_AUTO_CREATE);
                String endTime = df.format(new Date().getTime()+ Integer.parseInt(backtime) * 60 * 1000);
                SharedPreferences.Editor starttime = getSharedPreferences("endTime",MODE_PRIVATE).edit();
                starttime.putString("endTime",endTime);
                starttime.putBoolean("state",false);//写入结束时间和是否有座位
                starttime.apply();

                Intent intent = new Intent(GrabSeatActivity.this,GrabSeatBackService.class);
                startService(intent);
                //该消息为提示信息，只悬停1.5s后消失即可
                showNotification0("后台监控","后台监控已启动",GrabSeatActivity.this,2,15,"channel15","后台监控");
                Log.e("YSU点击我知道了","*************");
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Continuousmonitor = false;//持续监控标志
                Log.e("YSU点击取消","-------------");
                isServiceStart = false;//服务是否启动标志
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showDialogNotCancel("预约提示","当前阅览室无座,请选择其他阅览室",GrabSeatActivity.this);
                    }
                });

                //全局标志恢复
                actok = 0; prefSeatState = 0; ToastText = ""; empty_num = 0; finalStateDevname = ""; finalStateTime = ""; creditScore = true;
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
    //绑定前台GrabSeat服务
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //服务成功绑定的时候使用
            grabSeatBinder = (GrabSeat.GrabSeatBinder)service;
            grabSeatBinder.assignment(userInfo,begintime,prefSeat,roomname);//所有用户信息、开始时间、首选座位、阅览室
            serviceMsg = grabSeatBinder.runService();
            Log.e("YSU服务已经在运行","操作执行完成");
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            //服务解除绑定的时候使用
        }
    };
    //判断某个服务是否正在运行的方法
    //@param mContext@param serviceName是包名+服务的类名（例如：net.loonggg.testbackstage.TestService）true代表正在运行，false代表服务没有正在运行
    public boolean isServiceWork(Context mContext, String serviceName) {
        boolean isWork = false;
        ActivityManager myAM = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> myList = myAM.getRunningServices(40);
        if (myList.size() <= 0) {
            return false;
        }
        for (int i = 0; i < myList.size(); i++) {
            String mName = myList.get(i).service.getClassName().toString();
            if (mName.equals(serviceName)) {
                isWork = true;
                break;
            }
        }
        return isWork;
    }
/*-------------------------------------------------------------------------------------------------------------------------------------------------
                                                                  提示信息
-------------------------------------------------------------------------------------------------------------------------------------------------*/
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
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},
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
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},
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
                            R.drawable.seatlock))
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
                            R.drawable.seatlock))

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
                            R.drawable.seatlock))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},
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
                            R.drawable.seatlock))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},
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
                            R.drawable.seatlock))
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
                            R.drawable.seatlock))

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
                    .setColor(Color.parseColor("#000000"))
                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)//振动
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.timingtasknotigy)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.seatlock))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},
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
                            R.drawable.seatlock))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},
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
                            R.drawable.seatlock))
                    .setContentIntent(PendingIntent.getActivities(context,0x0001,
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},PendingIntent.FLAG_CANCEL_CURRENT))//跳转
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
                            R.drawable.seatlock))
                    .setContentIntent(PendingIntent.getActivities(context,0x0001,
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},PendingIntent.FLAG_CANCEL_CURRENT))//跳转
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
                            R.drawable.seatlock))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},
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
                            R.drawable.seatlock))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},
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
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},
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
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)//悬挂跳转
                    .build();
        }
        manager.notify(id,notification);
    }
    public void showNotification(String title,String text, Context context,int id,String channelid,String channelname){
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
                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.grabseatlock)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.seatlock))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,GrabSeat.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
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
                    .build();
        }

        manager.notify(id,notification);
    }
    //Dialog提示
    public void showDialog(String title,String msg, Context context){
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
    //自动选取座位号
    public void autoFillDialog(String title,String msg, Context context){
        android.support.v7.app.AlertDialog.Builder builder=new android.support.v7.app.AlertDialog.Builder(context,R.style.dialog_style);
        builder.setIcon(R.drawable.applygreen);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String[] r = parseSeatId(roomname,prefSeat,0);//检索座位号是否有误,如果有则给出更改提示
                EditText prefseatEdit = findViewById(R.id.edit_prefseat);
                prefseatEdit.setText(r[0].substring(3,6));//更新显示

                SharedPreferences.Editor userInfopref = getSharedPreferences("userInfo",MODE_PRIVATE).edit();
                userInfopref.putString("prefseat",prefSeat);
                userInfopref.apply();
                runGrabPro();//执行选座操作
            }
        });
        builder.setNegativeButton("否", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                runGrabPro();//执行选座操作
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
/*-------------------------------------------------------------------------------------------------------------------------------------------------
                                                                  执行流程
-------------------------------------------------------------------------------------------------------------------------------------------------*/
/*{"ret":0,"act":"set_resv","msg":"2019-06-22参数错误，预约开始时间必须小于预约结束时间","data":null,"ext":null}*/
    /*  执行选座，
    1. 1.1 getCredit 函数判断用户的积分信息。 在该函数中会向服务器发送登录请求，解析返回的信息，判断是否登录成功，
            1.1.1 actok = 9；如果没有登录成功，直接结束在dialog中显示msg提醒用户
            1.1.2 actok = 7；如果登录成功，积分是否满足条件：是，继续；否，退出，actok = 4，积分不足，弹出Dialog，并结束
    2. 2.1 getMyReserve 获取用户的座位状态信息
            2.1.1 actok = 2；已有预约，直接结束，给出现有的预约信息
            2.1.2 没有预约，继续
    3. 3.1 obtainAndParseStatus 获取并解析座位状态信息
            3.1.1 该函数获取用户指定时间往后的座位状态信息（结束时间22:25），是String格式
            3.1.2 从当中获取data下的数据，保存下来

       3.2 loadRsvSta 载入刚才保存的信息
       3.3 parseStatusOfEachSeatAndReserve 解析每个座位的状态信息，并进行预约
            3.3.1 查询座位余量，从data中解析出ts下的信息，ts="[]"时该座位为空闲，如果余量为0，弹出提示信息并结束；余量不为0，则继续
            3.3.2 用户是否指定了首选座位。
                （1）指定了首选座位。根据本地数据，检索指定座位在列表中的排序，根据此排序，访问座位列表中的该座位的状态，
                        若空闲，则预约；否则，改变指定座位操作状态prefSeatState=1不可操作，选取其他座位
                （2）没有指定首选座位/首选座位不可预约。
                        首选座位不可预约：根据指定座位在座位列表中的位置，以此为起点先后检索空闲座位，并预约。
                        没有指定首选座位：根据指定阅览室的座位数量，随机生成一个序号，检索该序号对应座位的状态，并以此为起点向后检索，并预约
    4. tips 根据不同情况下操作状态，给出对应的提示信息

        对指定座位预约时：actok = 5，预约冲突，改变指定座位不可操作，访问其他座位
                          actok = 1，将最终预约的座位赋值给全局变量，显示通知，并结束
                          actok = 3；不到开放时间，显示Dialog，并结束
                          actok = 6/8；预约时长受限，显示Dialog，并结束
        在遍历所有座位时：如果对指定座位操作成功，则ackok = 1; prefSeatState = 1;显示通知，并结束。
                          如果对其他座位操作成功 actok = 1；显示通知，并结束
                          如果访问的座位预约冲突 actok = 5；跳出继续下一个座位
                          如果未开放、预约时长受限 actok = 3/6/8，直接退出
    5. 结束预约过程，判断未能预约成功的原因  actok = 0/3/6/8，
                            0：empty_num = 0; actok = 0； 提示直接结束，提示当前阅览室无座；询问是否进行后台监控
                            3：Dialog提示并结束
                            6：Dialog提示并结束
                            8：Dialog提示并结束
    */
    //执行选座过程
    public boolean runGrabPro(){
        final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();
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

                    handlerProgress(10);
                    handlerProgress(2);

                    //showNotification3("快速抢座","正在响应您的请求，请稍后",getApplicationContext(),4,20,"channel4","选座提示");
                    int succeed; String result;
                    ToastText = ""; actok = 0; empty_num = 0; creditScore = true; sessionNull = false;
                    //通过检测登录排除操作状态7，通过积分信息排除操作状态4，通过检测个人预约排除操作状态2；通过有无座位余量排除状态0；
                    handlerProgress(3);
                    client = getCredit(client);//获取用户的积分信息，以确认是否可以进行预约.该过程会给出操作状态4/7/9的判断

                    if (creditScore & !sessionNull){//如果积分不为0，则可以进行预约
                        // 如果已经运行该部分内容，则操作状态4(积分不足)、操作状态6(其他异常)和操作状态7(登录成功)在接下来不必考虑，只考虑操作状态0/1/2/3/5/6/8
                        //访问座位状态
                        String[] myResv = getMyReserve(userInfo[1],userInfo[2],client,GrabSeatActivity.this);////owner devName devId labName timeDesc
                        if (!myResv[1].equals("no") & !myResv[4].equals("no")){//状态2
                            handlerProgress(0);
                            actok = 2;
                            ToastText = myResv[0] + "，您已有预约，当日不能再预约！" + myResv[1] + "(" + myResv[3] + ")，" + "时间:" + myResv[4];
                            tips();
                            return;//直接结束
                        }else {//此处只存在操作状态0/1/3/5/6/8
                            //加进度条
                            client = obtainAndParseStatus(client);// 获取并解析座位状态数据
                            result =loadRsvSta("currentSeat");
                            succeed = parseStatusOfEachSeatAndReserve(result,client);
                        }
                    }else {//如果积分为0，则显示禁止时间，并退出
                        //此处为操作状态4/6
                        handlerProgress(0);
                        tips();//处理结果信息
                        actok = 0; creditScore = true; sessionNull = false; empty_num = 0;//标志位恢复
                        return;
                    }
                    handlerProgress(0);
                    //当用户积分满足时才运行以下步骤，此处只存在操作状态0/1/3，操作状态5在执行过程以判断
                    if (succeed!=1 ){//如果用户没有预约，且积分满足预约条件，方可启动后台监控服务
                        //此部分只存在0/3/6操作状态
                        switch (actok){
                            case 0:
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        alertDialog("后台监控","当前阅览室暂时无座，后台监控会消耗部分流量。如果您仍点击确认，将为您在后台持续监测" + backtime + "分钟，您随时可以选择退出！",GrabSeatActivity.this);
                                    }
                                });
                                Log.e("YSU选座提示","运行正确");
                                break;
                            case 3:
                                tips();
                                return;//未到开放时间直接退出
                            case 6:
                                tips();
                                break;
                            case 8:
                                tips();
                                break;
                                default:
                                    break;
                        }
                        //
                    }else {//此处表示操作状态1   预约成功通知    该通知必须为悬挂不回收通知，等待用户滑动删除，默认振动+默认铃声
                        showNotification4("座位锁定",ToastText,GrabSeatActivity.this,1,"channnel1","座位锁定");
                        ToastText = ""; empty_num = 0;  actok = 0; succeed = 0;
                        //vibrator.vibrate(2000);
                    }
                }catch (Exception e){
                    handlerProgress(0);
                    e.printStackTrace();
                    errorCode = e.toString();
                    //访问异常，无法连接服务器！
                    if (cookieStore.size()==0){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ToastCustom.passValue(2000,1000,2,0,100);
                                ToastCustom.getInstance(GrabSeatActivity.this).show("访问异常，无法连接服务器！", 2000);
                            }
                        });
                        vibrator =  (Vibrator) getSystemService(VIBRATOR_SERVICE);//获得 一个震动的服务
                        vibrator.vibrate(2000);
                    }else {
                        Handler handlerThree=new Handler(Looper.getMainLooper());
                        handlerThree.post(new Runnable(){
                            public void run(){
                                showDialogNotCancel("错误代码", errorCode ,GrabSeatActivity.this);
                            }
                        });
                    }
                }
            }
        }).start();
        return true;
    }

/*-------------------------------------------------------------------------------------------------------------------------------------------------
                                                                  操作函数
-------------------------------------------------------------------------------------------------------------------------------------------------*/
   //handle传递消息
    public void handlerProgress(int id){
        Message message = new Message();
        switch (id){
            case 0:
                message.what = CLOSEPROGRESS;
                break;
            case 10:
                message.what = OPENPROGRESS;
                break;
            case 1:
                message.what = SEATERROR;
                break;
            case 2:
                message.what = RESPONSEREQUEST;
                break;
            case 3:
                message.what = GETCREDIT;
                break;
            case 4:
                message.what = ACCESSDATA;
                break;
            case 5:
                message.what = RESERVE;
                break;
            case 6:
                message.what = CREDITINFO;
                break;
            case 7:
                message.what = PARSEDATA;
                break;
                default:
                    break;
        }
        handler.sendMessage(message);
    }
    //获取我的座位状态信息2019.6.22
    public String[] getMyReserve(String myid, String mypwd, OkHttpClient client, final Context context) throws IOException, JSONException {
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
            Log.e("YSU解析我的预约信息",data);

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("YSU解析我的预约信息出错","err");
        }

        if (data == null){//解析出错
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showDialog("预约小提示","获取座位信息出错",context);
                }
            });
        }else if (data.equals("[]")){//没有预约{"ret":1,"act":"set_resv","msg":"操作成功！","data":null,"ext":null}
            Log.e("YSU","no");
        }else if (!data.equals("[]")){//座位信息
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
    //解析每个座位状态信息
    public int parseStatusOfEachSeatAndReserve(String dataArray,OkHttpClient client) throws JSONException, IOException {
        handlerProgress(7);
        int state = -1; //表示预约状态。0：无预约；1：有预约
        final JSONArray jsonArray = new JSONArray(dataArray);//获取到的座位状态数据
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
        //直到检索完该阅览室的所有座位，用户的预约状态为0，空闲座位为0，则提示该阅览室当前无所需座位
        if (empty_num == 0){
            handlerProgress(0);
            actok = 0;
            return 0;
        }else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ToastCustom.passValue(2000,1000,2,0,100);
                    ToastCustom.getInstance(GrabSeatActivity.this).show("当前阅览室座位： "+ empty_num + " / " + jsonArray.length(), 2000);
                }
            });
            /*  1. 用户指定了首选座位    */
            //四阅-209/order/address
            String[] res = new String[3];
            int r = 0;
            if (!prefSeat.equals("") & prefSeatState==0){//此处对指定座位只存在操作状态1/3/5/6/8，如果是5则直接到下一步继续检索，如果是3，则退出，并给出提示
                Log.e("GrabActivity：首选座位",prefSeat);
                //1.1 先对指定座位进行操作
                res = parseSeatId(roomname,prefSeat,1);
                Log.e("GrabActivity：指定阅览室",roomname);
                r = loginAndSetReserve(client,res[2]);
                Log.e("GrabActivity：指定座位id",res[2]);

                if (r != 1 & r != 2 & r != 3){
                    for (int i = 0; i < grabArray.length; i++){
                        if (grabArray[i].equals("")){
                            Log.e("第" + i +"个候选座位为空","no");
                        }else {
                            res = parseSeatId(roomname,grabArray[i],1);
                            r = loginAndSetReserve(client,res[2]);
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
                    tips();//在操作状态1/2处应该应用此函数，用于构建提示信息的具体内容，否则ToaskText=null
                    return 1;
                }else if (r == 3){
                    return 0;
                }else if (r == 6 | r == 8){//包含预约时间不少于1小时和不大于15小时
                    return 0;
                }
            }

            /*  2. 用户没有指定首选座位，或首选座位未能预约成功    */
            if ((prefSeat.equals("") | prefSeatState == 1)){//操作状态只存在1/3/5/6/8
                prefSeatState = 0;
                Log.e("GrabActivity","寻找空闲座位");
                if (prefSeat.equals("")){//如果没有指定，则随机生成
                    Random a = new Random();
                    int aa = a.nextInt(jsonArray.length());
                    res[1] = String.valueOf(aa);
                    Log.e("GrabActivity：随机座位",Integer.toString(aa));
                }
                //获取首选座位在该阅览室座位中的序号 res[3] = {"四阅-209","12","101440034"} ps(prefSeat)/order/address
                state = ergodicAllSeatAfterPrefSeat(roomname,res[1],client,storeState);//从指定座位开始，按顺序遍历所有座位，直至遍历到当前座位结束，并返回预约状态
            }
        }
        return state;
    }
    //遍历所有座位
    public int ergodicAllSeatAfterPrefSeat(String room,String order,OkHttpClient client,int[] storeState) throws IOException{
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
                s = loginAndSetReserve(client,address);//登录并进行预约,处理返回值，s = actok
                if (s == 1 & indey == or){//如果对指定座位预约成功，则指定座位的操作状态置1
                    prefSeatState = 1;
                    r = 1;
                    tips();//在操作状态1/2/3处应该应用此函数，用于构建提示信息的具体内容，否则ToaskText=null
                    return r;
                }else if (s == 1){//预约成功
                    r = 1;
                    tips();
                    return r;
                }else if (s == 3 | s == 6 | s == 8){//未开放、时间短于1小时、时间长于15小时都直接退出
                    return 0;
                }else if (s == 5){//预约冲突
                   continue;
                }
                Log.e(ps,String.valueOf(s));//预约的结果分析/类别归属
            }
        }
        return r;
    }
    //解析座位id
    public String[] parseSeatId(String room,String id, int idx){
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

                f = 1;//所选座位不存在，自动随机更改
            }
        }
        if (f == 0){
            result[0] = ps; result[1] = String.valueOf(order); result[2] = address;
        }else {
            Random randdiff = new Random();
            order = randdiff.nextInt(roomNameArray.length);
            result[0] = roomNameArray[order];
            result[1] = String.valueOf(order);
            result[2] = idArray[order];

            if (idx == 0 | idx == 1){
                prefSeat = result[0].substring(3,6);//自动更改并显示

                Message message = new Message();
                message.what = SEATERROR;
                handler.sendMessage(message);
            }


            switch (idx){
                case 0://主线程
                    EditText prefseatEdit = findViewById(R.id.edit_prefseat);
                    prefseatEdit.setText(prefSeat);//更新显示
                    showDialog("座位号异常","已自动将其更改为"+ result[0],GrabSeatActivity.this);
                    break;
                case 1:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showDialog("座位号异常","已自动将其更改为"+ result[0],GrabSeatActivity.this);
                        }
                    });
                    break;
                case 2:
                    grabArray[0] = result[0].substring(3,6);
                    EditText candidate1Edit = findViewById(R.id.grab1);
                    candidate1Edit.setText(grabArray[0]);
                    ToastCustom.passValue(1000,1000,2,0,100);
                    ToastCustom.getInstance(GrabSeatActivity.this).show("座位号异常，已完成更改", 1000);
                    break;
                case 3:
                    grabArray[1] = result[0].substring(3,6);
                    EditText candidate2Edit = findViewById(R.id.grab2);
                    candidate2Edit.setText(grabArray[1]);
                    ToastCustom.passValue(1000,1000,2,0,100);
                    ToastCustom.getInstance(GrabSeatActivity.this).show("座位号异常，已完成更改", 1000);
                    break;
                case 4:
                    grabArray[2] = result[0].substring(3,6);
                    EditText candidate3Edit = findViewById(R.id.grab3);
                    candidate3Edit.setText(grabArray[2]);
                    ToastCustom.passValue(1000,1000,2,0,100);
                    ToastCustom.getInstance(GrabSeatActivity.this).show("座位号异常，已完成更改", 1000);
                    break;
                case 5:
                    grabArray[3] = result[0].substring(3,6);
                    EditText candidate4Edit = findViewById(R.id.grab4);
                    candidate4Edit.setText(grabArray[3]);
                    ToastCustom.passValue(2000,1000,2,0,100);
                    ToastCustom.getInstance(GrabSeatActivity.this).show("座位号异常，已完成更改", 2000);
                    break;
                    default:
                        break;
            }


        }
        return result;
    }
    //Handl处理消息
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @Override
        public void  handleMessage(Message msg){
            switch (msg.what) {
                case CLOSEPROGRESS:
                    relativeLayout.setVisibility(View.GONE);
                    break;
                case  OPENPROGRESS:
                    relativeLayout.setVisibility(View.VISIBLE);
                    relativeLayout.bringToFront();

                    break;
                case SEATERROR:
                    EditText prefSeatEdit = findViewById(R.id.edit_prefseat);
                    prefSeatEdit.setText(prefSeat);
                    SharedPreferences.Editor userInfopref = getSharedPreferences("userInfo", MODE_PRIVATE).edit();
                    userInfopref.putString("prefseat", prefSeat);
                    userInfopref.apply();
                    break;
                case RESPONSEREQUEST:
                    progressTextView.setText("正在响应您的请求");
                    break;
                case GETCREDIT:
                    progressTextView.setText("获取积分信息");
                    break;
                case ACCESSDATA:
                    progressTextView.setText("获取座位数据");
                    break;
                case RESERVE:
                    progressTextView.setText("正在预约，请稍后");
                    break;
                case CREDITINFO:
                    progressTextView.setText(String.format(getResources().getString(R.string.progresstips),re[0],re[2]));
                    break;
                case PARSEDATA:
                    progressTextView.setText("解析数据....");
                    break;
            }
            super.handleMessage(msg);
        }
    };
    //登录并进行预约       reture 1/2/3/4/5/6 对应不同的操作结果
    public  int loginAndSetReserve(OkHttpClient client,String dev_id) throws IOException {
        handlerProgress(5);
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
        String end = CommonFunction.getMatcher("(.*) ",reserve)+"+22%3A30";//2019-06-04+22%3A30
        String start_time = CommonFunction.getMatcher("(.*):",time)+CommonFunction.getMatcher(":(.*)",time);//start_time=1128
        String end_time = "2230";//end_time=2230
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
        Request setResvRequest = new Request.Builder().url(setResvUrl).build();//构建预约请求
        Response setRsvResponse = client.newCall(setResvRequest).execute();//执行预约请求
        String setRsvResponseData = setRsvResponse.body().string();//服务器返回的数据       //{"ret":0,"act":"set_resv","msg":"请在6:30之后再进行预约","data":null,"ext":null}
        Log.e("GrabActivity：预约参数",setRsvResponseData);

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
            empty_num = 0;
            ToastText = userInfo[0] + "已预约成功!" + finalStateDevname + "开始时间:" + finalStateTime;
        }else if (act_name.equals("set_resv") & act_msg.equals("已有预约")){//操作状态2：您已有预约，当日不能再预约
            actok = 2;
            empty_num = 0;
            ToastText = act_msg;
        }else if (CommonFunction.regexMatcher("(请在6:30之后再进行预约)",act_msg)){//操作状态3：系统未开放
            actok = 3;
        }else if (CommonFunction.regexMatcher("(积分不足)",act_msg)){//操作状态4：积分不足，禁止预约
            actok = 4;
        }else if (CommonFunction.regexMatcher("(冲突)",act_msg)){//操作状态5：与现有的预约存在冲突
            actok = 5;
        }else if (CommonFunction.regexMatcher("(预约时间不能少于)",act_msg)){//操作状态6：预约时间太短
            actok = 6;
            ToastText = act_msg;
        }else if (CommonFunction.regexMatcher("(ok)",act_msg)&act_name.equals("login")){//操作状态7：专为登录会话返回值设置
            actok = 7;
        }else if (CommonFunction.regexMatcher("(15小时)",act_msg)){//操作状态8：预约时长超过15小时
            actok = 8;
            ToastText = act_msg;
        } else {//操作状态9：其他情况的预约不成功,打印提示
            // 例：密码或学号错误时出现“未登录或登录超时，session=null 请重新登录” 座位号错误时返回的“参数错误”
            actok = 9;
            ToastText = act_msg;
        }
        return actok;
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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showDialog("预约小提示",ToastText,GrabSeatActivity.this);
                    }
                });
                break;
            case 3://系统未开放
                //该信息重要等级为 LOW，点击空白处可取消Dialog
                final String cu  = df.format(getNetTime());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showDialog("预约小提示","请在6:30之后再进行预约; 预约端当前时间"+ cu,GrabSeatActivity.this);
                    }
                });
                break;
            case 4://积分不足
                //该信息重要等级为 HIGH，用户必须与之交互
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showDialogNotCancel("预约提示",re[0]+":剩余积分为"+re[2]+",在"+re[3]+"期间被禁止预约",GrabSeatActivity.this);
                    }
                });
                break;
            case 5://预约冲突
                //该信息重要等级为 LOW，点击空白处可取消Dialog
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showDialog("预约小提示","与现有预约存在冲突",GrabSeatActivity.this);
                    }
                });
                break;
            case 6://预约时间少于1小时
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showDialog("预约小提示",ToastText,GrabSeatActivity.this);
                    }
                });
                break;
            case 7://登录成功
                break;
            case 8://预约时间大于15小时
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showDialog("预约小提示",ToastText,GrabSeatActivity.this);
                    }
                });
                break;
            case 9://其他信息
                if (CommonFunction.regexMatcher("(参数有误)",ToastText)){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(GrabSeatActivity.this,"座位号不存在",Toast.LENGTH_SHORT).show();
                        }
                    });
                }else if (CommonFunction.regexMatcher("(未登录)",ToastText)){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(GrabSeatActivity.this,"用户名/密码错误",Toast.LENGTH_SHORT).show();
                        }
                    });
                }else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showDialog("预约提示",ToastText,GrabSeatActivity.this);
                        }
                    });
                }
                break;
            default:
                break;
        }
        actok = 0; empty_num = 0; //ToastText = "";
    }

/*-------------------------------------------------------------------------------------------------------------------------------------------------
                                                                  状态函数
-------------------------------------------------------------------------------------------------------------------------------------------------*/
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
            Log.e("GrabActivity;网络时间",df.format(webDate));
        }catch (Exception e){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(GrabSeatActivity.this,"请重启手机或重新开启网络!",Toast.LENGTH_SHORT).show();
                }
            });
            webDate = defDate;
            Log.e("GrabActivity：获取网络时间出错",e.toString());
        }
        return webDate;
    }
    //从服务器获取座位状态信息（包含解析与保存步骤）
    public OkHttpClient obtainAndParseStatus(OkHttpClient client) throws IOException {
        handlerProgress(4);
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
        String fr_end = "22%3A30";

        //3.    获取系统时间
        /*SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String rsvTime = df.format(new Date());// 当前时间加40分钟,该时间格式为2019-06-01 18:25
        String date = rsvTime.substring(0,10);*/
        //http://202.206.242.87/ClientWeb/pro/ajax/device.aspx?byType=devcls&classkind=8&display=fp&md=d&room_id=100457213
        // &purpose=&selectOpenAty=&cld_name=default&date=2019-06-21&fr_start=14%3A00&fr_end=21%3A00&act=get_rsv_sta&_=1561082040042
        String roomIdUrl = urlCommonFirstPara.concat(userInfo[3]).concat(urlCommonSecondPara)
                .concat(date).concat("&fr_start=").concat(fr_start).concat("&fr_end=").concat(fr_end)
                .concat(urlCommonThirdPara).concat(userInfo[4]);//目标阅览室url
        // 访问服务器座位状态信息
        Request getRsvSta = new Request.Builder().url(roomIdUrl).build();
        Response rsvStaRsponse = client.newCall(getRsvSta).execute();
        String rsvStateData = rsvStaRsponse.body().string();
        //从服务器返回的数据中解析出座位状态信息，并保存到data
        parseJSONFromResponse(rsvStateData);
        Log.e("GrabActivity","获取座位状态数据");
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
            saveResvSta(data,"currentSeat");
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
    //保持信息持久化，从文件中读出用户信息显示到对应位置
    public void printUserInfo(){
        // 从保存的userInfo文件中获取信息，还原到界面；如果的没有，就使用默认值
        SharedPreferences pref = getSharedPreferences("userInfo",MODE_PRIVATE);
        username = pref.getString("username","");
        studentid = pref.getString("studentid","1501041000");
        password = pref.getString("password","");
        prefSeat = pref.getString("prefseat","");
        begintime = pref.getString("begintime","40");
        backtime = pref.getString("backtime","15");

        grabArray[0] = pref.getString("grab1","219");
        grabArray[1] = pref.getString("grab2","320");
        grabArray[2] = pref.getString("grab3","");
        grabArray[3] = pref.getString("grab4","");

        EditText usernameEdit = findViewById(R.id.edit_username);
        usernameEdit.setText(username);
        EditText studentidEdit = findViewById(R.id.edit_studentid);
        studentidEdit.setText(studentid);
        EditText passwordEdit = findViewById(R.id.edit_password);
        passwordEdit.setText(password);
        EditText prefseatEdit = findViewById(R.id.edit_prefseat);
        prefseatEdit.setText(prefSeat);
        EditText begintimeEdit = findViewById(R.id.edit_begintime);
        begintimeEdit.setText(begintime);
        EditText backTimeEdit = findViewById(R.id.back_edit_time);
        backTimeEdit.setText(backtime);

        EditText candidate1Edit = findViewById(R.id.grab1);
        candidate1Edit.setText(grabArray[0]);
        EditText candidate2Edit = findViewById(R.id.grab2);
        candidate2Edit.setText(grabArray[1]);
        EditText candidate3Edit = findViewById(R.id.grab3);
        candidate3Edit.setText(grabArray[2]);
        EditText candidate4Edit = findViewById(R.id.grab4);
        candidate4Edit.setText(grabArray[3]);
    }
    //获取用户输入的信息,username,studentid,password,roomid
    public boolean getUserInfoFromScreen(){
        //  获取用户提交的信息，并对数据进行更新保存
        EditText usernameEdit = findViewById(R.id.edit_username);
        userInfo[0] = usernameEdit.getText().toString();
        EditText studentIdEdit = findViewById(R.id.edit_studentid);
        userInfo[1] = studentIdEdit.getText().toString();
        EditText passwordEdit = findViewById(R.id.edit_password);
        userInfo[2] = passwordEdit.getText().toString();
        userInfo[3] = roomname;
        EditText prefseatEdit = findViewById(R.id.edit_prefseat);
        prefSeat = prefseatEdit.getText().toString();
        EditText begintimeEdit = findViewById(R.id.edit_begintime);
        begintime = begintimeEdit.getText().toString();
        EditText backTimeEdit = findViewById(R.id.back_edit_time);
        backtime = backTimeEdit.getText().toString();

        EditText candidate1Edit = findViewById(R.id.grab1);
        grabArray[0] = candidate1Edit.getText().toString();
        EditText candidate2Edit = findViewById(R.id.grab2);
        grabArray[1] = candidate2Edit.getText().toString();
        EditText candidate3Edit = findViewById(R.id.grab3);
        grabArray[2] = candidate3Edit.getText().toString();
        EditText candidate4Edit = findViewById(R.id.grab4);
        grabArray[3] = candidate4Edit.getText().toString();



        //  将用户输入的roomName转化为对应的roomid在程序中运行
        if (backtime.isEmpty()){
            backtime = "15";
        }
        if (userInfo[0].isEmpty()|userInfo[1].isEmpty()|userInfo[2].isEmpty()||userInfo[3].isEmpty()){
            ToastCustom.passValue(2000,1000,2,0,100);
            ToastCustom.getInstance(GrabSeatActivity.this).show("请输入完整信息", 2000);
            return false;
        }
        if (!prefSeat.isEmpty()){
            for (int i = 0; i < grabArray.length; i ++){
                if (!grabArray[i].isEmpty()){
                    parseSeatId(roomname,grabArray[i],i+2);
                }
            }
        }else {
            for (String s : grabArray) {
                if (!s.isEmpty()) {
                    ToastCustom.passValue(2000, 1000, 2, 0, 100);
                    ToastCustom.getInstance(GrabSeatActivity.this).show("首选座位为空时，候选座位无效，将随机预约", 2000);
                    break;
                }
            }
        }

        switch (userInfo[3]) {
            case "一阅":
                userInfo[3] = "100457211";
                userInfo[4] = "1551697412954";
                break;
            case "二阅":
                userInfo[3] = "100457213";
                userInfo[4] = "1551697526282";
                break;
            case "三阅":
                userInfo[3] = "101439229";
                userInfo[4] = "1551696819132";
                break;
            case "四阅":
                userInfo[3] = "101439231";
                userInfo[4] = "1551687360999";
                break;
            case "五阅":
                userInfo[3] = "101439233";
                userInfo[4] = "1551696819132";
                break;
            case "树华A":
                userInfo[3] = "100457221";
                userInfo[4] = "1551699007393";
                break;
            default:
                ToastCustom.passValue(2000, 1000, 2, 0, 100);
                ToastCustom.getInstance(GrabSeatActivity.this).show("输入错误！请重新输入", 2000);
                break;
        }


        // 保存用户数据
        SharedPreferences.Editor userInfopref = getSharedPreferences("userInfo",MODE_PRIVATE).edit();
        userInfopref.putString("username",userInfo[0]);
        userInfopref.putString("studentid",userInfo[1]);
        userInfopref.putString("password",userInfo[2]);
        userInfopref.putString("roomid",userInfo[3]);
        userInfopref.putString("room",userInfo[4]);
        userInfopref.putString("prefseat",prefSeat);
        userInfopref.putString("begintime",begintime);
        userInfopref.putString("backtime",backtime);
        userInfopref.putString("grab1",grabArray[0]);
        userInfopref.putString("grab2",grabArray[1]);
        userInfopref.putString("grab3",grabArray[2]);
        userInfopref.putString("grab4",grabArray[3]);
        userInfopref.apply();
        return true;
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
        Log.e("GrabActivity：积分信息",credit);
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
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ToastCustom.passValue(2000,1000,2,0,100);
                    ToastCustom.getInstance(GrabSeatActivity.this).show(re[0]+"  当前积分:"+re[2], 2000);
                }
            });
            handlerProgress(6);

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
        Log.e("GrabActivity：登录获取积分",loginReturn);
        parseResponseMsg(loginReturn);//登录结果只有两种 登录成功 和 登录失败
        if (actok == 9){//登录失败
            sessionNull = true;
        }else if (actok == 7){//登录成功
            //解析积分
            parseJSONScore(parseJSONResponse(loginReturn));
        }
        return client;
    }
}
