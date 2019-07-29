package com.example.uniconseat;

public class addFunction {
    /*  2019年6月21日 8:37 准备修改*/
//    package com.example.automaticseatselection;
//
//import android.app.ActivityManager;
//import android.app.AlertDialog;
//import android.app.Notification;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.app.Service;
//import android.content.ComponentName;
//import android.content.Context;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.content.ServiceConnection;
//import android.content.SharedPreferences;
//import android.graphics.BitmapFactory;
//import android.graphics.Color;
//import android.os.IBinder;
//import android.os.Looper;
//import android.os.Vibrator;
//import android.support.v4.app.NotificationCompat;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.View;
//import android.view.WindowManager;
//import android.widget.AdapterView;
//import android.widget.EditText;
//import android.widget.ImageButton;
//import android.widget.Spinner;
//import android.widget.Toast;
//
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.OutputStreamWriter;
//import java.net.URL;
//import java.net.URLConnection;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//
//import okhttp3.Cookie;
//import okhttp3.CookieJar;
//import okhttp3.HttpUrl;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.Response;
//
//
//    public class GrabSeatActivity extends BaseActivity {
//        static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
//        /*全局通用信息*/
//        static String Url = "http://202.206.242.87/ClientWeb/xcus/ic2/Default.aspx";
//        static Date webTargetDate;
//        static {
//            try {
//                webTargetDate = df.parse(CommonFunction.systemDelayTime(0,2)+ " " + "06:30:00");
//            } catch (ParseException e) {
//                e.printStackTrace();
//            }
//        }
//        static Date webDate;
//        static Date defDate;
//        static {
//            try {
//                defDate = df.parse("1970-07-01 01:00:00");
//            } catch (ParseException e) {
//                e.printStackTrace();
//            }
//        }//date格式的判错时间
//        static String TAG;//notification用的全局信息
//        static int Id;
//        static int Length;
//        static String[] re = new String[4];//用户信誉信息：姓名、班级、信誉积分、受限时间
//        public boolean creditScore = true;//用户信誉是否受限
//
//        //  全局信息传递
//        private static String username,studentid,password,roomid,roomname; //用户信息
//        public String prefSeat,begintime;//首选座位  开始时间
//        public int begin,prefSeatState=0;//开始时间整型，预约指定座位的结果。0：可以进行预约指定座位；1：用户没有指定座位/指定座位输入错误不存在/预约失败
//        private static String[] emptyTitle = new String[500];  //空闲座位信息
//        private static String[] emptyDevid = new String[500];
//        private static String ToastText = "";//预约状态提示信息
//        private static String finalStateTime,finalStateDevname; //最终座位状态信息
//        private Intent ServiceIntent;
//        public boolean Continuousmonitor = true;//判定是否进行了后台持续监控
//        public boolean isServiceStart = false;//服务是否被启动
//        public int serviceMsg = 0;//后台服务传回的结果
//
//        private static int empty_num = 0; //空闲座位数量
//        public static int actok = 0;//表示操作的结果状态，分为5种
//        Vibrator vibrator;//手机振动器
//        private GrabSeat.GrabSeatBinder grabSeatBinder;//绑定服务
//
//        //    url共用字段
//        String urlCommonFirstPara = "http://202.206.242.87/ClientWeb/pro/ajax/device.aspx?byType=devcls&classkind=8&display=fp&md=d&room_id=";
//        String urlCommonSecondPara = "&purpose=&selectOpenAty=&cld_name=default&date=";
//        String urlCommonThirdPara = "&act=get_rsv_sta&_=";
//        //userInfo中分别存储 username studentid password roomid url字段最后部分
//        static String[] userInfo= new String[5];
//
//        @Override
//        protected void onCreate(Bundle savedInstanceState) {
//            super.onCreate(savedInstanceState);
//            setContentView(R.layout.activity_grab_seat);//初始化界面
//            vibrator=(Vibrator)getSystemService(Service.VIBRATOR_SERVICE);//获取系统的Vibrator服务
////        showNotification("123","123",GrabSeatActivity.this,19);
//            spinnerRoomId();//监听阅览室选择框
//            printUserInfo();//显示上次保存的用户信息
//            grabSeat();//监听选座按钮
//            cancelSeat();//中断服务
//        }
//
//        //提交按钮监听事件
//        public void grabSeat(){
//            ImageButton grabButton = findViewById(R.id.grab_button);
//            grabButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    getUserInfoFromScreen();//从屏幕获取用户信息并保存
//                    runGrabPro();//执行选座操作
//                }
//            });
//        }
//        //停止后台服务
//        public void cancelSeat(){
//            ImageButton exitButton = findViewById(R.id.exit_button);
//            exitButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    if (Continuousmonitor & isServiceStart){//服务一经绑定，就会开始运行
//                        //如果同时调用了startService和bindService则必须同时当上述两种条件同时不满足时，服务才会被销毁
//                        if (!grabSeatBinder.stopProcess){
//                            grabSeatBinder.stopParam(true);
//                        }
//                        unbindService(connection);
//                        stopService(ServiceIntent);
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                showDialog("后台监控","监控服务已关闭，感谢您的使用！", com.example.automaticseatselection.GrabSeatActivity.this);
//                            }
//                        });
//                        Log.e("停止服务","serviceservice is stop");
//                    }
//                    else {
//                        Log.e("没有启动服务","serviceservice is stop");
//                    }
//                }
//            });
//        }
//        //阅览室选择Spinner
//        public void spinnerRoomId(){
//            Spinner spinner = (Spinner) findViewById(R.id.roomidspinner);
//            //初始化先前用户的输入值
//            int index = 2;
//            SharedPreferences spinnerDefaultRoomIdpref = getSharedPreferences("userInfo",MODE_PRIVATE);
//            roomid = spinnerDefaultRoomIdpref.getString("roomid","101439231");//101439231是第四阅览室id
//            //  将roomid转化为中文，显示在对应的输入框中
//            if (roomid.equals("100457211")){
//                roomname = "一阅";
//                index = 1;
//            }else if (roomid.equals("100457213")){
//                roomname = "二阅";
//                index = 2;
//            }else if (roomid.equals("101439229")){
//                roomname = "三阅";
//                index = 3;
//            }else if (roomid.equals("101439231")){
//                roomname = "四阅";
//                index = 4;
//            }else if (roomid.equals("101439233")){
//                roomname = "五阅";
//                index = 5;
//            }else if (roomid.equals("100457221")){
//                roomname = "树华A";
//                index = 6;
//            }
//            spinner.setSelection(index);
//            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//                @Override
//                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                    String[] room = getResources().getStringArray(R.array.roomname);
//                    roomname = room[position];
//                    switch (roomname){
//                        case "一阅":
//                            roomid = "100457211";
//                            break;
//                        case "二阅":
//                            roomid = "100457213";
//                            break;
//                        case "三阅":
//                            roomid = "101439229";
//                            break;
//                        case "四阅":
//                            roomid = "101439231";
//                            break;
//                        case "五阅":
//                            roomid = "101439233";
//                            break;
//                        case "树华A":
//                            roomid = "100457221";
//                            break;
//                        default:
//                            Toast.makeText(com.example.automaticseatselection.GrabSeatActivity.this,"重新选择阅览室",Toast.LENGTH_SHORT).show();
//                            break;
//                    }
//                }
//                @Override
//                public void onNothingSelected(AdapterView<?> parent) {
//                    //当所有选项都没有被选择时触发
//                }
//            });
//
//        }
//        //后台持续监控Dialog
//        public void alertDialog(String title,String msg, Context context){
//            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
//            dialog.setTitle(title);
//            dialog.setMessage(msg);
//            dialog.setCancelable(false);
//            dialog.setPositiveButton("知道了", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    Continuousmonitor = true;
//                    ServiceIntent = new Intent(com.example.automaticseatselection.GrabSeatActivity.this,GrabSeat.class);
//                    bindService(ServiceIntent,connection,BIND_AUTO_CREATE);
//                    isServiceStart = true;//服务是否启动的标志位
//                    //该消息为提示信息，只悬停1.5s后消失即可
//                    showNotification0("后台监控","后台监控已启动", com.example.automaticseatselection.GrabSeatActivity.this,2,15);
////                if (Continuousmonitor){
////                    //如果用户确认监控，则绑定服务
////                    ServiceIntent = new Intent(GrabSeatActivity.this,GrabSeat.class);
////                    bindService(ServiceIntent,connection,BIND_AUTO_CREATE);
////                    isServiceStart = true;//服务是否启动的标志位
////                    //该消息为提示信息，只悬停1.5s后消失即可
////                    showNotification0("后台监控","后台监控已启动",GrabSeatActivity.this,2,15);
////                }else {
////                    isServiceStart = false;
////                    Log.e("while循环运行，不持续监测","当前阅览室无座");
////                    runOnUiThread(new Runnable() {
////                        @Override
////                        public void run() {
////                            Toast.makeText(GrabSeatActivity.this,"请选择其他阅览室",Toast.LENGTH_LONG).show();
////                        }
////                    });
////                }
//                    Log.e("点击我知道了","123456");
//                }
//            });
//            dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    Continuousmonitor = false;
//                    Log.e("点击取消","123456");
//                    isServiceStart = false;
//                    Log.e("while循环运行，不持续监测","当前阅览室无座");
//                    if (actok == 6){
//                        if (CommonFunction.regexMatcher("(预约时间不能少于)",ToastText)){
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    showDialogNotCancel("预约提示","请调整预约时间或改日预约", com.example.automaticseatselection.GrabSeatActivity.this);
////                        Toast.makeText(GrabSeatActivity.this,"请选择其他阅览室",Toast.LENGTH_LONG).show();
//                                }
//                            });
//                        }
//                    }else if (empty_num == 0 & (actok != 1 | actok != 2)){
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                showDialogNotCancel("预约提示","当前阅览室无座,请选择其他阅览室", com.example.automaticseatselection.GrabSeatActivity.this);
////                        Toast.makeText(GrabSeatActivity.this,"请选择其他阅览室",Toast.LENGTH_LONG).show();
//                            }
//                        });
//                    }
//
//                }
//            });
//            dialog.show();
//        }
//        //绑定前台GrabSeat服务
//        private ServiceConnection connection = new ServiceConnection() {
//            @Override
//            public void onServiceConnected(ComponentName name, IBinder service) {
//                //服务成功绑定的时候使用
//                grabSeatBinder = (GrabSeat.GrabSeatBinder)service;
//                grabSeatBinder.assignment(userInfo,begintime,prefSeat,roomname);
//                serviceMsg = grabSeatBinder.runService();
//                Log.e("服务已经在运行","操作执行完成");
//            }
//            @Override
//            public void onServiceDisconnected(ComponentName name) {
//                //服务解除绑定的时候使用
//            }
//        };
//        //判断某个服务是否正在运行的方法
//        //@param mContext@param serviceName是包名+服务的类名（例如：net.loonggg.testbackstage.TestService）true代表正在运行，false代表服务没有正在运行
//        public boolean isServiceWork(Context mContext, String serviceName) {
//            boolean isWork = false;
//            ActivityManager myAM = (ActivityManager) mContext
//                    .getSystemService(Context.ACTIVITY_SERVICE);
//            List<ActivityManager.RunningServiceInfo> myList = myAM.getRunningServices(40);
//            if (myList.size() <= 0) {
//                return false;
//            }
//            for (int i = 0; i < myList.size(); i++) {
//                String mName = myList.get(i).service.getClassName().toString();
//                if (mName.equals(serviceName)) {
//                    isWork = true;
//                    break;
//                }
//            }
//            return isWork;
//        }
//        /*显示通知,用户可以手动删除
//        /* 样式0： 只悬停、不振动、不跳转  不回状态栏  */
//        public void showNotification0(String title, String text, Context context, int id,int length){
//            final NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//            //悬停
//            Notification notification2 = new NotificationCompat.Builder(context)
//                    .setContentTitle(title)
//                    .setContentText(text)
//                    .setWhen(System.currentTimeMillis())
//                    .setDefaults(Notification.DEFAULT_SOUND)
//                    .setPriority(Notification.PRIORITY_HIGH)
//                    .setVisibility(Notification.VISIBILITY_PUBLIC)
//                    .setColor(Color.parseColor("#00000000"))
//                    .setAutoCancel(true)
//                    .setSmallIcon(R.drawable.timingtasknotigy)
//                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
//                            R.drawable.seatlock))
//                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
//                            new Intent[]{new Intent(context, com.example.automaticseatselection.GrabSeatActivity.class)},
//                            PendingIntent.FLAG_CANCEL_CURRENT),false)
//                    .build();
//            String tag = id + "float";
//            manager.notify(tag,id,notification2);
//            TAG = tag; Id = id; Length = length;
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    try { Thread.sleep(Length * 100);//Length秒后悬挂式通知消失
//                        manager.cancel(TAG, Id);//按tag id 来清除消息
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }).start();
//        }
//        /* 样式1： 悬停、不振动、不跳转   回状态栏 */
//        public void showNotification1(String title, String text, Context context, int id,int length){
//            final NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//            //普通
//            Notification notification = new NotificationCompat.Builder(context)
//                    .setContentTitle(title)
//                    .setContentText(text)
//                    .setWhen(System.currentTimeMillis())
//                    .setDefaults(Notification.DEFAULT_SOUND)
//                    .setPriority(Notification.PRIORITY_HIGH)
//                    .setVisibility(Notification.VISIBILITY_PUBLIC)
//                    .setColor(Color.parseColor("#00000000"))
//                    .setAutoCancel(true)
//                    .setSmallIcon(R.drawable.timingtasknotigy)
//                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
//                            R.drawable.seatlock))
//
//                    .build();
//            String tag = id + "float";
//            manager.notify(id,notification);
//            //悬停
//            Notification notification2 = new NotificationCompat.Builder(context)
//                    .setContentTitle(title)
//                    .setContentText(text)
//                    .setWhen(System.currentTimeMillis())
//                    .setDefaults(Notification.DEFAULT_SOUND)
//                    .setPriority(Notification.PRIORITY_HIGH)
//                    .setVisibility(Notification.VISIBILITY_PUBLIC)
//                    .setColor(Color.parseColor("#000000"))
//                    .setAutoCancel(true)
//                    .setSmallIcon(R.drawable.timingtasknotigy)
//                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
//                            R.drawable.seatlock))
//                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
//                            new Intent[]{new Intent(context, com.example.automaticseatselection.GrabSeatActivity.class)},
//                            PendingIntent.FLAG_CANCEL_CURRENT),false)
//                    .build();
//            manager.notify(tag,id,notification2);
//            TAG = tag; Id = id; Length = length;
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    try { Thread.sleep(Length * 100);//Length秒后悬挂式通知消失
//                        manager.cancel(TAG, Id);//按tag id 来清除消息
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }).start();
//        }
//        /* 样式2： 悬停、振动、不跳转   回状态栏   */
//        public void showNotification2(String title, String text, Context context, int id,int length){
//            final NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//            Notification notification = new NotificationCompat.Builder(context)
//                    .setContentTitle(title)
//                    .setContentText(text)
//                    .setWhen(System.currentTimeMillis())
//                    .setDefaults(Notification.DEFAULT_SOUND)
//                    .setPriority(Notification.PRIORITY_HIGH)
//                    .setVisibility(Notification.VISIBILITY_PUBLIC)
//                    .setColor(Color.parseColor("#00000000"))
//                    .setAutoCancel(true)
//                    .setSmallIcon(R.drawable.timingtasknotigy)
//                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
//                            R.drawable.seatlock))
//
//                    .build();
//            String tag = id + "float";
//            manager.notify(id,notification);
//
//            Notification notification2 = new NotificationCompat.Builder(context)
//                    .setContentTitle(title)
//                    .setContentText(text)
//                    .setWhen(System.currentTimeMillis())
//                    .setDefaults(Notification.DEFAULT_SOUND)
//                    .setPriority(Notification.PRIORITY_HIGH)
//                    .setVisibility(Notification.VISIBILITY_PUBLIC)
//                    .setColor(Color.parseColor("#000000"))
//                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)//振动
//                    .setAutoCancel(true)
//                    .setSmallIcon(R.drawable.timingtasknotigy)
//                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
//                            R.drawable.seatlock))
//                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
//                            new Intent[]{new Intent(context, com.example.automaticseatselection.GrabSeatActivity.class)},
//                            PendingIntent.FLAG_CANCEL_CURRENT),false)
//                    .build();
//            manager.notify(tag,id,notification2);
//            TAG = tag; Id = id; Length = length;
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    try { Thread.sleep(Length * 100);//Length秒后悬挂式通知消失
//                        manager.cancel(TAG, Id);//按tag id 来清除消息
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }).start();
//        }
//        /* 样式3： 悬停、振动、跳转   回状态栏    */
//        public void showNotification3(String title, String text, Context context, int id ,int length){
//            final NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//            Notification notification = new NotificationCompat.Builder(context)
//                    .setContentTitle(title)
//                    .setContentText(text)
//                    .setWhen(System.currentTimeMillis())
//                    .setDefaults(Notification.DEFAULT_SOUND)
//                    .setPriority(Notification.PRIORITY_HIGH)
//                    .setVisibility(Notification.VISIBILITY_PUBLIC)
//                    .setColor(Color.parseColor("#00000000"))
//                    .setAutoCancel(true)
//                    .setSmallIcon(R.drawable.timingtasknotigy)
//                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
//                            R.drawable.seatlock))
//                    .setContentIntent(PendingIntent.getActivities(context,0x0001,
//                            new Intent[]{new Intent(context, com.example.automaticseatselection.GrabSeatActivity.class)},PendingIntent.FLAG_CANCEL_CURRENT))//跳转
//                    .build();
//            String tag = id + "float";
//            manager.notify(tag,id,notification);
//
//            Notification notification2 = new NotificationCompat.Builder(context)
//                    .setContentTitle(title)
//                    .setContentText(text)
//                    .setWhen(System.currentTimeMillis())
//                    .setDefaults(Notification.DEFAULT_SOUND)
//                    .setPriority(Notification.PRIORITY_HIGH)
//                    .setVisibility(Notification.VISIBILITY_PUBLIC)
//                    .setColor(Color.parseColor("#00000000"))
//                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)//振动
//                    .setAutoCancel(true)
//                    .setSmallIcon(R.drawable.timingtasknotigy)
//                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
//                            R.drawable.seatlock))
//                    .setContentIntent(PendingIntent.getActivities(context,0x0001,
//                            new Intent[]{new Intent(context, com.example.automaticseatselection.GrabSeatActivity.class)},PendingIntent.FLAG_CANCEL_CURRENT))//跳转
//                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
//                            new Intent[]{new Intent(context, com.example.automaticseatselection.GrabSeatActivity.class)},
//                            PendingIntent.FLAG_CANCEL_CURRENT),false)
//                    .build();
//            manager.notify(tag,id,notification2);
//            TAG = tag; Id = id; Length = length;
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    try { Thread.sleep(Length * 100);//Lengths后悬挂式通知消失
//                        manager.cancel(TAG, Id);//按tag id 来清除消息
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }).start();
//        }
//        /* 样式4： 悬挂、振动、跳转    不回状态栏    */
//        public void showNotification4(String title, String text, Context context, int id){
//            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//            Notification notification = new NotificationCompat.Builder(context)
//                    .setContentTitle(title)
//                    .setContentText(text)
//                    .setWhen(System.currentTimeMillis())
//                    .setDefaults(Notification.DEFAULT_SOUND)//默认铃声
//                    .setPriority(Notification.PRIORITY_HIGH)
//                    .setVisibility(Notification.VISIBILITY_PUBLIC)
//                    .setColor(Color.parseColor("#00000000"))
//                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)//振动
//                    .setAutoCancel(true)
//                    .setSmallIcon(R.drawable.grabseatlock)
//                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
//                            R.drawable.seatlock))
//                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
//                            new Intent[]{new Intent(context, com.example.automaticseatselection.GrabSeatActivity.class)},
//                            PendingIntent.FLAG_CANCEL_CURRENT),false)//悬挂跳转
//                    .build();
//            manager.notify(id,notification);
//        }
//        public void showNotification(String title,String text, Context context,int id){
//            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//            Notification notification = new NotificationCompat.Builder(context)
//                    .setContentTitle(title)
//                    .setContentText(text)
//                    .setWhen(System.currentTimeMillis())
//                    .setDefaults(Notification.DEFAULT_SOUND)
//                    .setPriority(Notification.PRIORITY_HIGH)
//                    .setVisibility(Notification.VISIBILITY_PUBLIC)
//                    .setColor(Color.parseColor("#00000000"))
//                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)
//                    .setAutoCancel(true)
//                    .setSmallIcon(R.drawable.grabseatlock)
//                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
//                            R.drawable.seatlock))
//                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
//                            new Intent[]{new Intent(context,GrabSeat.class)},
//                            PendingIntent.FLAG_CANCEL_CURRENT),false)
//                    .setContentIntent(PendingIntent.getActivities(context,0x0001,
//                            new Intent[]{new Intent(context, com.example.automaticseatselection.GrabSeatActivity.class)},PendingIntent.FLAG_CANCEL_CURRENT))
//                    .build();
//            manager.notify(id,notification);
//        }
//        //Dialog提示
//        public void showDialog(String title,String msg, Context context){
////        AlertDialog.Builder alertDialog = new AlertDialog.Builder(TimingTasks.this, R.style.dialog_style);
//            android.support.v7.app.AlertDialog.Builder builder=new android.support.v7.app.AlertDialog.Builder(context,R.style.dialog_style);
//            builder.setIcon(R.drawable.applygreen);
//            builder.setTitle(title);
//            builder.setMessage(msg);
//            builder.setCancelable(true);
//            builder.setPositiveButton("知道了",
//                    new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialogInterface, int i) {
//
//                        }
//                    });
//            android.support.v7.app.AlertDialog dialog=builder.create();
//            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
//            dialog.show();
//        }
//        public void showDialogNotCancel(String title, String msg, Context context){
////        AlertDialog.Builder alertDialog = new AlertDialog.Builder(TimingTasks.this, R.style.dialog_style);
//            android.support.v7.app.AlertDialog.Builder builder=new android.support.v7.app.AlertDialog.Builder(context,R.style.dialog_style);
//            builder.setIcon(R.drawable.applygreen);
//            builder.setTitle(title);
//            builder.setMessage(msg);
//            builder.setCancelable(false);
//            builder.setPositiveButton("知道了",
//                    new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialogInterface, int i) {
//
//                        }
//                    });
//            android.support.v7.app.AlertDialog dialog=builder.create();
//            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
//            dialog.show();
//        }
//
//        //执行选座过程
//        public boolean runGrabPro(){
//            final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    try{    //实现同一个cookie访问
//                        OkHttpClient client = new OkHttpClient.Builder()
//                                .cookieJar(new CookieJar() {
//                                    @Override
//                                    public void saveFromResponse(HttpUrl httpUrl, List<Cookie> list) {
//                                        cookieStore.put(httpUrl.host(), list);
//                                    }
//                                    @Override
//                                    public List<Cookie> loadForRequest(HttpUrl httpUrl) {
//                                        List<Cookie> cookies = cookieStore.get(httpUrl.host());
//                                        return cookies != null ? cookies : new ArrayList<Cookie>();
//                                    }
//                                })
//                                .build();
//
//                        int succeed; String result;
//                        client = getCredit(client);//获取用户的积分信息，以确认是否可以进行预约
//
//                        if (creditScore){//如果积分不为0，则可以进行预约
//                            client = obtainAndParseStatus(client);// 获取并解析座位状态数据
//                            result =loadRsvSta("data");
//                            succeed = parseStatusOfEachSeatAndReserve(result,client);
////                        if (!prefSeat.equals("") & prefSeatState==0){//如果用户指定了座位，先对指定座位进行操作，再遍历其他邻近座位
////                            String[] res = parseSeatId(roomname,prefSeat);
////                            ergodicAllSeatAfterPrefSeat(roomname,res[1],client);
////                        }else {//如果用户没有指定座位，则随机选座
////                            client = obtainAndParseStatus(client);// 获取并解析座位状态数据
////                            result =loadRsvSta("data");
////                            succeed = parseStatusOfEachSeatAndReserve(result,client);
////                        }
//                        }else {//如果积分为0，则显示禁止时间，并退出
//                            tips();//处理结果信息
//                            return;
//                        }
//
//                        //当用户积分满足时才运行以下步骤
//                        if (succeed!=1){//如果用户没有预约，且积分满足预约条件，方可启动后台监控服务
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    alertDialog("后台监控","当前阅览室暂时无座，后台监控会消耗部分流量。如果您仍点击确认，将为您在后台持续监测15分钟，您随时可以选择退出！", com.example.automaticseatselection.GrabSeatActivity.this);
//                                }
//                            });
//                            Log.e("选座提示","运行正确");
//                        }else {//此处表示预约成功通知    该通知必须为悬挂不回收通知，等待用户滑动删除，默认振动+默认铃声
//                            showNotification("座位锁定",ToastText, com.example.automaticseatselection.GrabSeatActivity.this,1);
//                            //vibrator.vibrate(2000);
//                        }
//
//                    }catch (Exception e){
//                        e.printStackTrace();
//                        //访问异常，无法连接服务器！
//                        if (cookieStore.size()==0){
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    Toast.makeText(com.example.automaticseatselection.GrabSeatActivity.this,"访问异常，无法连接服务器！",Toast.LENGTH_SHORT).show();
//                                }
//                            });
//                            vibrator.vibrate(1000);//此处需要修改
//                        }
//                    }
//                }
//            }).start();
//            return true;
//        }
//        //处理预约结果，并提示
//        public void tips(){
//            switch (actok){
//                case 1://预约成功
//                    //该信息重要等级为 HIGH，用户必须与之交互
//                    prefSeatState = 0;//对指定座位的操作状态置0
//                    ToastText = userInfo[0] + "已预约成功!" + finalStateDevname + "开始时间:" + finalStateTime;
//                    break;
//                case 2://已有预约
//                    //该信息重要等级为 LOW，点击空白处可取消Dialog
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            showDialog("预约小提示",ToastText, com.example.automaticseatselection.GrabSeatActivity.this);
//                        }
//                    });
//                    break;
//                case 3://系统未开放
//                    //该信息重要等级为 LOW，点击空白处可取消Dialog
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            showDialog("预约小提示","请在6:30之后再进行预约; 系统当前时间"+df.format(getNetTime()), com.example.automaticseatselection.GrabSeatActivity.this);
//                        }
//                    });
//                    break;
//                case 4://积分不足
//                    //该信息重要等级为 HIGH，用户必须与之交互
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            showDialogNotCancel("预约提示",re[0]+":剩余积分为"+re[2]+",在"+re[3]+"期间被禁止预约", com.example.automaticseatselection.GrabSeatActivity.this);
//                        }
//                    });
//                    break;
//                case 5://预约冲突
//                    //该信息重要等级为 LOW，点击空白处可取消Dialog
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            showDialog("预约小提示","与现有预约存在冲突", com.example.automaticseatselection.GrabSeatActivity.this);
//                        }
//                    });
//                    break;
//                case 6://其他信息
//                    if (CommonFunction.regexMatcher("(参数有误)",ToastText)){
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                Toast.makeText(com.example.automaticseatselection.GrabSeatActivity.this,"座位号不存在",Toast.LENGTH_SHORT).show();
//                            }
//                        });
//                    }else if (CommonFunction.regexMatcher("(未登录)",ToastText)){
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                Toast.makeText(com.example.automaticseatselection.GrabSeatActivity.this,"用户名/密码错误",Toast.LENGTH_SHORT).show();
//                            }
//                        });
//                    }else {
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                showDialog("预约提示",ToastText, com.example.automaticseatselection.GrabSeatActivity.this);
//                            }
//                        });
//                    }
//                    break;
//                default:
//                    break;
//            }
//        }
//        //解析座位id
//        public String[] parseSeatId(String room,String id){
//            //e: room = 四阅      id = 209
//            String[] roomNameArray;
//            String[] idArray;
//            String[] result = new String[3];//四阅-209/order/address
//            String address = "101440034";//四阅-209
//            String ps = room + "-" + id;//如：四阅-209
////        String a = "";//默认座位 四阅-209
//            int order = 0;//座位id在数组中的序号
//            //1. 提出默认阅览室座位id
//            switch (room){
//                case "一阅":
//                    roomNameArray = getResources().getStringArray(R.array.first_reading_room_name);
//                    idArray = getResources().getStringArray(R.array.first_reading_room_id);
//                    break;
//                case "二阅":
//                    roomNameArray = getResources().getStringArray(R.array.second_reading_room_name);
//                    idArray = getResources().getStringArray(R.array.second_reading_room_id);
//                    break;
//                case "三阅":
//                    roomNameArray = getResources().getStringArray(R.array.third_reading_room_name);
//                    idArray = getResources().getStringArray(R.array.third_reading_room_id);
//                    break;
//                case "四阅":
//                    roomNameArray = getResources().getStringArray(R.array.fourth_reading_room_name);
//                    idArray = getResources().getStringArray(R.array.fourth_reading_room_id);
//                    break;
//                case "五阅":
//                    roomNameArray = getResources().getStringArray(R.array.fifth_reading_room_name);
//                    idArray = getResources().getStringArray(R.array.fifth_reading_room_id);
//                    break;
//                case "树华A":
//                    roomNameArray = getResources().getStringArray(R.array.shuhua_a_name);
//                    idArray = getResources().getStringArray(R.array.shuhua_a_id);
//                    break;
//                default:
//                    roomNameArray = getResources().getStringArray(R.array.fourth_reading_room_name);
//                    idArray = getResources().getStringArray(R.array.fourth_reading_room_id);
//                    ps = "四阅-209"; id = "209";
//                    break;
//            }
//            //2.根据座位号检索id
//            for (int index = 0; index < roomNameArray.length; index ++){
//                String rr = roomNameArray[index];
//                Log.e("rr",rr);
//                if (rr.equals(ps)){
//                    order = index;
//                    address = idArray[index];
//                    break;
//                }
//            }
//            result[0] = ps; result[1] = String.valueOf(order); result[2] = address;
//            return result;
//        }
//        //遍历所有座位
//        public int ergodicAllSeatAfterPrefSeat(String room,String order,OkHttpClient client) throws IOException, ParseException {
//            int or = Integer.parseInt(order);//把序号变为int型
//            String[] roomNameArray;//阅览室座位名
//            String[] idArray;//阅览室座位id
//            String ps,address;
//            switch (room){
//                case "一阅":
//                    roomNameArray = getResources().getStringArray(R.array.first_reading_room_name);
//                    idArray = getResources().getStringArray(R.array.first_reading_room_id);
//                    break;
//                case "二阅":
//                    roomNameArray = getResources().getStringArray(R.array.second_reading_room_name);
//                    idArray = getResources().getStringArray(R.array.second_reading_room_id);
//                    break;
//                case "三阅":
//                    roomNameArray = getResources().getStringArray(R.array.third_reading_room_name);
//                    idArray = getResources().getStringArray(R.array.third_reading_room_id);
//                    break;
//                case "四阅":
//                    roomNameArray = getResources().getStringArray(R.array.fourth_reading_room_name);
//                    idArray = getResources().getStringArray(R.array.fourth_reading_room_id);
//                    break;
//                case "五阅":
//                    roomNameArray = getResources().getStringArray(R.array.fifth_reading_room_name);
//                    idArray = getResources().getStringArray(R.array.fifth_reading_room_id);
//                    break;
//                case "树华A":
//                    roomNameArray = getResources().getStringArray(R.array.shuhua_a_name);
//                    idArray = getResources().getStringArray(R.array.shuhua_a_id);
//                    break;
//                default:
//                    roomNameArray = getResources().getStringArray(R.array.fourth_reading_room_name);
//                    idArray = getResources().getStringArray(R.array.fourth_reading_room_id);
//                    break;
//            }
//            int s = 0,r = -1;
//            for (int indey = or; indey < idArray.length + or; indey ++){
//                if (indey>idArray.length){
//                    indey = indey - idArray.length;
//                }
//                ps = roomNameArray[indey];//座位名称
//                finalStateDevname = ps;//最终预约的座位
//                address = idArray[indey];//座位id
//                s = loginAndSetReserve(client,address);//登录并进行预约,处理返回值，并决定是否结束
//                if (s == 1 & indey == or){//如果对指定座位预约成功，则指定座位的操作状态置1
//                    prefSeatState = 1;
//                    r = 1;
//                    tips();
//                    return r;
//                }else if (s == 1 | s == 2 ){//预约成功/已有预约
//                    r = 1;
//                    tips();
//                    return r;
//                }else if (s == 3){//系统未开放
//                    r = 0;
//                    return r;//r=0;
//                }else if (s == 5){//预约冲突，直接跳出
//                    break;
//                }else if (s == 6){
//                    tips();
//                    break;
//                }
//                Log.e(ps,String.valueOf(s));//预约的结果分析/类别归属
//            }
//            if (r != 1){//遍历所有座位后没有成功，则当前时间当前阅览室没有空座
//                r = 0;
//                empty_num = 0;
//            }
//            return r;
//        }
//        //登录并进行预约       reture 1/2/3/4/5/6 对应不同的操作结果
//        public  int loginAndSetReserve(OkHttpClient client,String dev_id) throws IOException, ParseException {
//            //1.    设定预约时间，如果用户没有指定时间，则从40分钟之后开始
//            if (begintime.equals("")){
//                begintime = "40";
//                begin = Integer.parseInt(begintime);
//            }else{
//                begin = Integer.parseInt(begintime);
//            }
//            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//            String reserve = df.format(new Date().getTime()+ begin * 60 * 1000);// 当前时间加40分钟,该时间格式为2019-06-01 18:25
//            String date = reserve.substring(0,10);//日期2019-06-01
//            String time = reserve.substring(11,16);//预约开始时间     例：18:25
//            finalStateTime = time;
//            //2.    日期时间格式整合
//            String start = new StringBuilder().append(CommonFunction.getMatcher("(.*) ", reserve)).append("+")
//                    .append(CommonFunction.getMatcher("(.*):", time)).append("%3A").append(CommonFunction.getMatcher(":(.*)", time))
//                    .toString();
//            String end = CommonFunction.getMatcher("(.*) ",reserve)+"+22%3A00";
//            String start_time = CommonFunction.getMatcher("(.*):",time)+CommonFunction.getMatcher(":(.*)",time);
//            String end_time = "2200";
//            //3.    登录
//            String loginUrl = new StringBuilder().append("http://seat.ysu.edu.cn/ClientWeb/pro/ajax/login.aspx?act=login&id=").append(userInfo[1]).append("&pwd=")
//                    .append(userInfo[2]).append("&role=512&aliuserid=&schoolcode=&wxuserid=&_nocache=1551511783772")
//                    .toString();
//            //      3.1 向服务器发送登录请求，包括student ID, password
//            Request loginRequest = new Request.Builder().url(loginUrl).build();//构建登录请求
//            Response loginResponse = client.newCall(loginRequest).execute();//执行登录请求
//            String loginReturn = loginResponse.body().string();
//
////        parseJSONScore(parseJSONResponse(loginReturn));
////        Log.e("parseJSONUserSc",loginReturn);
//
//            //      3.2向服务器发送选座请求，并接受响应数据
//            //      3.2 向服务器发送选座请求，包括座位id  开始时间和结束时间
//            String setResvUrl = new StringBuilder().append("http://seat.ysu.edu.cn/ClientWeb/pro/ajax/reserve.aspx?dialogid=&dev_id=").append(dev_id)
//                    .append("&lab_id=&kind_id=&room_id=&type=dev&prop=&test_id=&term=&test_name=&start=").append(start).append("&end=")
//                    .append(end).append("&start_time=").append(start_time).append("&end_time=").append(end_time).append("&up_file=&memo=&act=set_resv&_=1544339248168")
//                    .toString();
//            Request setResvRequest = new Request.Builder().url(setResvUrl).build();//构建预约请求
//            Response setRsvResponse = client.newCall(setResvRequest).execute();//执行预约请求
//            //{"ret":0,"act":"set_resv","msg":"请在6:30之后再进行预约","data":null,"ext":null}
//            String setRsvResponseData = setRsvResponse.body().string();//服务器返回的数据
//            Log.e("setRsvResponseData",setRsvResponseData);
//
//            //解析操作状态  预约冲突返回信息示例：{ret: 0, act: "set_resv", msg: "2019-06-04您在【2019年06月04日】已有预约，当日不能再预约", data: null, ext: null}
//            //              预约成功返回的信息
//            String act_ret = CommonFunction.parseSingleLabel(setRsvResponseData,"ret");
//            String act_name = CommonFunction.parseSingleLabel(setRsvResponseData,"act");
//            String act_msg = CommonFunction.parseSingleLabel(setRsvResponseData,"msg");
//            //构建预约冲突时的返回信息
//            String msg_error = date + "您在【" + date.substring(0,4) + "年" + date.substring(5,7) +"月" + date.substring(8,10) + "日】已有预约，当日不能再预约";
//
//            //4.    预约结果分析    除了以下1,2两种状态其他状态均为当前用户没有座位
//            if (act_name.equals("set_resv") & act_msg.equals("操作成功！") & act_ret.equals("1")){//操作状态1：预约成功
//                actok = 1;
//                ToastText = userInfo[0] + "已预约成功!" + finalStateDevname + "开始时间:" + finalStateTime;
//            }else if (act_name.equals("set_resv") & act_msg.equals(msg_error)){//操作状态2：您已有预约，当日不能再预约
//                actok = 2;
//                ToastText = msg_error;
//            }else if (CommonFunction.regexMatcher("(请在6:30之后再进行预约)",act_msg)){//操作状态3：系统未开放
//                actok = 3;
//            }else if (CommonFunction.regexMatcher("(积分不足)",act_msg)){//操作状态4：积分不足，禁止预约
//                actok = 4;
//            }else if (CommonFunction.regexMatcher("(冲突)",act_msg)){//操作状态5：与现有的预约存在冲突
//                actok = 5;
//            }else {//操作状态6：其他情况的预约不成功,打印提示      例：密码或学号错误时出现“未登录或登录超时，session=null 请重新登录” 座位号错误时返回的“参数错误”
//                actok = 6;
//                ToastText = act_msg;
//            }
//            return actok;
//        }
//        //解析每个座位状态信息
//        public int parseStatusOfEachSeatAndReserve(String dataArray,OkHttpClient client) throws JSONException, IOException, ParseException {
//            int state = -1; //表示预约状态。0：无预约；1：有预约
//            JSONArray jsonArray = new JSONArray(dataArray);
//            /*  1. 用户指定了首选座位    */
//            if (!prefSeat.equals("")&prefSeatState==0){
//                Log.e("prefSeat",prefSeat);
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(com.example.automaticseatselection.GrabSeatActivity.this,"检索指定座位预约状态...",Toast.LENGTH_SHORT).show();
//                    }
//                });
//                //1.1 先对指定座位进行操作
//                String[] res = parseSeatId(roomname,prefSeat);
//                Log.e("rooname",roomname);
////                int r = loginAndSetReserve(client,res[2]);
//                int r = 5;
//                Log.e("res",res[2]);
//                if (r == 5 ){//预约冲突
//                    prefSeatState = 1;//指定座位操作失败
//                    showNotification0("选座提示","与现有预约存在冲突", com.example.automaticseatselection.GrabSeatActivity.this,3,15);
//                } else if (r == 1 ){
//                    finalStateDevname = res[0];
//                    tips();
//                    return 1;
//                }else if (r == 2){
//                    tips();
//                    return 0;
//                }
////                for (int indey = 0; indey < jsonArray.length(); indey ++){//检索所有座位的状态并存储
////                    String s = jsonArray.getString(indey);
////                    String title = CommonFunction.parseSingleLabel(s,"title");//座位名称：四阅-102
////                    String devId = CommonFunction.parseSingleLabel(s,"devId");//座位id：
////                    String t = CommonFunction.parseSingleLabel(s,"ts");//座位状态ts="[]"为空闲
////                    if (t.equals("[]")){
////                        t = "free";//空闲
////                    }else {
////                        t = "occupy";//占用
////                    }
////                    SharedPreferences.Editor yuelanshipref = getSharedPreferences(roomname,MODE_PRIVATE).edit();
////                    yuelanshipref.putString(title,devId);//座位名称和座位id
////                    yuelanshipref.putString(title+"-state",t);//座位名称和状态
////                    yuelanshipref.apply();
////                    //Log.e("<item>",title+"</item>");//测试时获取信息
////                    //Log.e("<item>",devId+"</item>");
////                }
////                String key,assignState="",assignId="";//指定座位的名称，座位状态，座位id
////                try {
////                    SharedPreferences preferences = getSharedPreferences(roomname,MODE_PRIVATE);
////                    key = roomname + "-" + prefSeat;
////                    assignState = preferences.getString(key+"-state","");//查找指定座位的状态
////                    assignId = preferences.getString(key,"");//查找指定座位的id
////                }catch (Exception e){
////                    e.printStackTrace();
////                    runOnUiThread(new Runnable() {
////                        @Override
////                        public void run() {
////                            Toast.makeText(GrabSeatActivity.this,"指定座位不存在!",Toast.LENGTH_SHORT).show();
////                        }
////                    });
////                }
////                if (assignState.equals("free")){//座位处于空闲状态
////                    int param = loginAndSetReserve(client,assignId);
////                    if (param ==1 | param == 2){//操作状态1/2，表示用户已有预约，均停止该方法
////                        state = 1;//表示已有座位
////                        return state;
////                    }else if (param==3){//操作状态3,未到系统开放的预约时间
////                        long diffNetAndTarget = webTargetDate.getTime() - getNetTime().getTime();
////                        //如果未到系统开放时间，并且网络时间不等于判错时间，即获取到正确的服务器时间
////
////                    }else {//操作状态5,6
////                        prefSeatState = 0;//对指定座位的操作失败
////                        state = 0;//用户没有预约
////                    }
////                }else {
////                    prefSeatState = 1;//对指定座位的操作失败(2019:06:20 8:38修改),不加此句，系统将不检索非指定座位,该处的值为1时，可同时预约两个座位
////                    runOnUiThread(new Runnable() {
////                        @Override
////                        public void run() {
////                            Toast.makeText(GrabSeatActivity.this,"非空闲状态!",Toast.LENGTH_SHORT).show();
////                        }
////                    });
////                }
//            }
//            Log.e("gongong","gongong");
//            if (prefSeat.equals("") | prefSeatState == 1){
//                showNotification0("选座提示","正在为您查找空闲座位...", com.example.automaticseatselection.GrabSeatActivity.this,3,15);
//                Log.e("解析每个座位的状态","寻找空闲座位");
//
//                for (int index = 0; index < jsonArray.length(); index++){
//                    String seatInfo = jsonArray.getString(index);
//                    String ts = CommonFunction.parseSingleLabel(seatInfo,"ts");
////              "ts":[{"id":null,"start":"2019-06-02 20:00","end":"2019-06-02 22:30","state":"doing","date":null,"name":null,"title":null,"owner":"小三","accno":"100047176","member":"","limit":null,"occupy":true}],
//                    if (ts.equals("[]")){       //ts="[]"表示该座位处于空闲状态
//                        //把空闲座位的Name和id存进数组
//                        String title = CommonFunction.parseSingleLabel(seatInfo,"title");
//                        String dev_id = CommonFunction.parseSingleLabel(seatInfo,"devId");
//                        emptyTitle[empty_num] = title;      emptyDevid[empty_num] =dev_id;
//                        finalStateDevname = title;
////                        int actok = loginAndSetReserve(client,dev_id);//登录并预约，返回操作结果
//                        if (actok ==1 | actok == 2){//操作状态1/2，表示用户存在预约，均停止该方法
//                            state = 1;//表示已有座位
//                            return state;
//                        }else if (actok == 3){//操作状态3,未到系统开放的预约时间
//                            long diffNetAndTarget = webTargetDate.getTime() - getNetTime().getTime();
//                            state = 0;//用户没有座位
//                        }else {//用户不存在预约
//                            state = 0;
//                        }
//                        empty_num ++; //可选座位数量
//                        empty_num = 0;//测试用
//                    }
//                }
//                String[] res = parseSeatId(roomname,prefSeat);
//                state = ergodicAllSeatAfterPrefSeat(roomname,res[1],client);
//                //直到检索完该阅览室的所有座位，用户的预约状态为0，空闲座位为0，则提示该阅览室当前无所需座位
//                if (empty_num == 0){
//                    state = 0;//调试中为1，默认为0
//                    showNotification0("选座提示","当前时间没有空闲座位", com.example.automaticseatselection.GrabSeatActivity.this,3,20);
//                }
//            }
//            return state;
//        }
//        //获取网络时间
//        public Date getNetTime(){
//            try {
//                URL url = new URL(Url);
//                URLConnection uc = url.openConnection();
//                uc.setReadTimeout(5000);
//                uc.setConnectTimeout(5000);
//                uc.connect();
//                long correctTime = uc.getDate();
//                webDate = new Date(correctTime);
//                Log.e("网络时间",df.format(webDate));
//            }catch (Exception e){
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(com.example.automaticseatselection.GrabSeatActivity.this,"请重启手机或重新开启网络!",Toast.LENGTH_SHORT).show();
//                    }
//                });
//                webDate = defDate;
//                Log.e("尝试获取网络时间出错",e.toString());
//            }
//            return webDate;
//        }
//        //从服务器获取座位状态信息（包含解析与保存步骤）
//        public OkHttpClient obtainAndParseStatus(OkHttpClient client) throws IOException {
//            //获取系统时间
//            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//            String rsvTime = df.format(new Date());// 当前时间加40分钟,该时间格式为2019-06-01 18:25
//            String date = rsvTime.substring(0,10);
//            String roomIdUrl = urlCommonFirstPara.concat(userInfo[3]).concat(urlCommonSecondPara)
//                    .concat(date).concat(urlCommonThirdPara).concat(userInfo[4]);//目标阅览室url
//            // 访问服务器座位状态信息
//            Request getRsvSta = new Request.Builder().url(roomIdUrl).build();
//            Response rsvStaRsponse = client.newCall(getRsvSta).execute();
//            String rsvStateData = rsvStaRsponse.body().string();
//            //从服务器返回的数据中解析出座位状态信息，并保存到data
//            String data = parseJSONFromResponse(rsvStateData);
//            Log.e("从服务器获取并保存座位状态信息","成功");
//            return client;
//        }
//        //解析出多个标签的数据,ret,act,msg,data,ext et al.
//        private String parseJSONFromResponse(String jsonData){
//            String result = "";
//            try {
//                JSONObject jsonObject = new JSONObject(jsonData);
//                String ret = jsonObject.getString("ret");
//                String act = jsonObject.getString("act");
//                String msg = jsonObject.getString("msg");
//                String data = jsonObject.getString("data");
//                String ext = jsonObject.getString("ext");
//                saveResvSta(data,"data");
//                Log.v("保存成功","保存成功");
//                result = data;
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//            return  result;
//        }
//        //读取座位状态信息
//        public String loadRsvSta(String st){
//            FileInputStream in = null;
//            BufferedReader reader = null;
//            StringBuilder content = new StringBuilder();
//            try{
//                in = openFileInput(st);
//                reader = new BufferedReader(new InputStreamReader(in));
//                String line = "";
//                while ((line = reader.readLine())!= null){
//                    content.append(line);
//                }
//            }catch (IOException e){
//                e.printStackTrace();
//            }finally {
//                if (reader != null){
//                    try{
//                        reader.close();
//                    }catch (IOException e){
//                        e.printStackTrace();
//                    }
//                }
//            }
//            return content.toString();
//        }
//        //保存座位状态信息
//        public void  saveResvSta(String rsvstainfo,String filename){
//            FileOutputStream out = null;
//            BufferedWriter writer = null;
//            try{
//                out = openFileOutput(filename, Context.MODE_PRIVATE);
//                writer = new BufferedWriter(new OutputStreamWriter(out));
//                writer.write(rsvstainfo);
//            }catch (IOException e){
//                e.printStackTrace();
//            }finally {
//                try {
//                    if (writer != null){
//                        writer.close();
//                    }
//                }catch (IOException e){
//                    e.printStackTrace();
//                }
//            }
//        }
//        //保持信息持久化，从文件中读出用户信息显示到对应位置
//        public void printUserInfo(){
//            // 从保存的userInfo文件中获取信息，还原到界面；如果的没有，就使用默认值
//            SharedPreferences pref = getSharedPreferences("userInfo",MODE_PRIVATE);
//            username = pref.getString("username","");
//            studentid = pref.getString("studentid","1501041000");
//            password = pref.getString("password","");
//            prefSeat = pref.getString("prefseat","");
//            begintime = pref.getString("begintime","40");
//
//
//            EditText usernameEdit = findViewById(R.id.edit_username);
//            usernameEdit.setText(username);
//            EditText studentidEdit = findViewById(R.id.edit_studentid);
//            studentidEdit.setText(studentid);
//            EditText passwordEdit = findViewById(R.id.edit_password);
//            passwordEdit.setText(password);
//            EditText prefseatEdit = findViewById(R.id.edit_prefseat);
//            prefseatEdit.setText(prefSeat);
//            EditText begintimeEdit = findViewById(R.id.edit_begintime);
//            begintimeEdit.setText(begintime);
//        }
//        //获取用户输入的信息,username,studentid,password,roomid
//        public String[] getUserInfoFromScreen(){
//            //  获取用户提交的信息，并对数据进行更新保存
//            EditText usernameEdit = findViewById(R.id.edit_username);
//            userInfo[0] = usernameEdit.getText().toString();
//            EditText studentIdEdit = findViewById(R.id.edit_studentid);
//            userInfo[1] = studentIdEdit.getText().toString();
//            EditText passwordEdit = findViewById(R.id.edit_password);
//            userInfo[2] = passwordEdit.getText().toString();
//            userInfo[3] = roomname;
//            EditText prefseatEdit = findViewById(R.id.edit_prefseat);
//            prefSeat = prefseatEdit.getText().toString();
//            EditText begintimeEdit = findViewById(R.id.edit_begintime);
//            begintime = begintimeEdit.getText().toString();
//
//            //  将用户输入的roomName转化为对应的roomid在程序中运行
//            if (userInfo[0].isEmpty()|userInfo[1].isEmpty()|userInfo[2].isEmpty()||userInfo[3].isEmpty()){
//                Toast.makeText(com.example.automaticseatselection.GrabSeatActivity.this,"请输入完整信息",Toast.LENGTH_SHORT).show();
//            }else if (userInfo[3].equals("一阅")){
//                userInfo[3] = "100457211";
//                userInfo[4] = "1551697412954";
//            }else if (userInfo[3].equals("二阅")){
//                userInfo[3] = "100457213";
//                userInfo[4] = "1551697526282";
//            }else if (userInfo[3].equals("三阅")){
//                userInfo[3] = "101439229";
//                userInfo[4] = "1551696819132";
//            }else if (userInfo[3].equals("四阅")){
//                userInfo[3] = "101439231";
//                userInfo[4] = "1551687360999";
//            }else if (userInfo[3].equals("五阅")){
//                userInfo[3] = "101439233";
//                userInfo[4] = "1551696819132";
//            }else if (userInfo[3].equals("树华A")){
//                userInfo[3] = "100457221";
//                userInfo[4] = "1551699007393";
//            }else {
//                Toast.makeText(com.example.automaticseatselection.GrabSeatActivity.this,"输入错误！请重新输入！",Toast.LENGTH_SHORT).show();
//            }
//            // 保存用户数据
//            SharedPreferences.Editor userInfopref = getSharedPreferences("userInfo",MODE_PRIVATE).edit();
//            userInfopref.putString("username",userInfo[0]);
//            userInfopref.putString("studentid",userInfo[1]);
//            userInfopref.putString("password",userInfo[2]);
//            userInfopref.putString("roomid",userInfo[3]);
//            userInfopref.putString("room",userInfo[4]);
//            userInfopref.putString("prefseat",prefSeat);
//            userInfopref.putString("begintime",begintime);
//            userInfopref.apply();
//            return userInfo;
//        }
//        //解析用户积分信息
//        private String parseJSONResponse(String jsonData){
//            String result = "";
//            try {
//                JSONObject jsonObject = new JSONObject(jsonData);
////            String ret = jsonObject.getString("ret");
////            String act = jsonObject.getString("act");
////            String msg = jsonObject.getString("msg");
////            String ext = jsonObject.getString("ext");
//                String data = jsonObject.getString("data");
//                result = data;
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//            return  result;
//        }
//        public void parseJSONScore(String data){
//            String name = CommonFunction.parseSingleLabel(data,"name");//x姓名
//            String cls = CommonFunction.parseSingleLabel(data,"cls");//班级
//            String credit = CommonFunction.parseSingleLabel(data,"credit");//信誉
//            //credit: [["个人预约制度","0","300","2019-06-19至2019-06-22"]]//信誉积分为0时的返回信息
//            //credit = "[[\"个人预约制度\",\"200\",\"300\",\"\"]]";//信誉积分不为0时的返回信息
////        credit = "[[\"个人预约制度\",\"0\",\"300\",\"2019-06-19至2019-06-22\"]]";//测试用
//            Log.e("credit",credit);
//            String c = CommonFunction.getMatcher("\\[(.*)\\]",credit);
//            c = CommonFunction.getMatcher("\\[(.*)\\]",c);
//            c = CommonFunction.getMatcher("\"(.*)\"",c);
//            String[] r = c.split("\",\"");
//            for (int i = 0;i<r.length;i++ ){
//                Log.e(Integer.toString(i),r[i]);
//            }
//            if (Integer.parseInt(r[1])==0){//姓名+班级+剩余积分+限制时间
//                creditScore = false;
//                re[0] = name; re[1] = cls; re[2] = r[1]; re[3] = r[3];
//            }else {
//                creditScore = true;
//                re[0] = name; re[1] = cls; re[2] = r[1]; re[3] = "";
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(com.example.automaticseatselection.GrabSeatActivity.this,re[0]+"  当前积分:"+re[2]+",正在为您预约",Toast.LENGTH_SHORT).show();
//                    }
//                });
//            }
//        }
//        public OkHttpClient getCredit(OkHttpClient client) throws IOException {
//            //1. 登录
//            String loginUrl = new StringBuilder().append("http://seat.ysu.edu.cn/ClientWeb/pro/ajax/login.aspx?act=login&id=").append(userInfo[1]).append("&pwd=")
//                    .append(userInfo[2]).append("&role=512&aliuserid=&schoolcode=&wxuserid=&_nocache=1551511783772")
//                    .toString();
//            Request loginRequest = new Request.Builder().url(loginUrl).build();//向服务器发送登录请求，包括ID, password
//            Response loginResponse = client.newCall(loginRequest).execute();//执行登录请求
//            String loginReturn = loginResponse.body().string();//得到响应数据
//            Log.e("loginReturn",loginReturn);
//            parseJSONScore(parseJSONResponse(loginReturn));
//            if (!creditScore){//积分不足
//                actok = 4;
//                Log.e("actod","4");
//            }
//            return client;
//        }
//    }

}
