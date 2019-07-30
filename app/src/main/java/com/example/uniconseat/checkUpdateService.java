package com.example.uniconseat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class checkUpdateService extends Service {

    private static final String TAG = "upgrade";
    public static String UPDATE_PATH_MEMORY_DIR;     //从服务器获取的文件在内存中的路径(安装目录中的路径)
    public static String UPDATE_PATH_SDCARD_DIR;     //日志文件在sdcard中的路径
    public static final int SDCARD_TYPE = 0;          //记录类型为存储在SD卡下面
    public static final int MEMORY_TYPE = 1;          //记录类型为存储在内存中
    static String netVersionCode,netVersionName,curVersionName;
    static int curVersionCode;
    static String upTips = "更新了一些bug";
    public static Context context;




    public static final int MEMORY_LOG_FILE_MAX_SIZE = 10 * 1024 * 1024;           //内存中日志文件最大值，10M
    public static final int MEMORY_LOG_FILE_MONITOR_INTERVAL = 10 * 60 * 1000;     //内存中的日志文件大小监控时间间隔，10分钟
    public static final int SDCARD_LOG_FILE_SAVE_DAYS = 7;                         //sd卡中日志文件的最多保存天数


    @SuppressWarnings("unused")
    public static String LOG_SERVICE_LOG_PATH;    //本服务产生的日志，记录日志服务开启失败信息

    public static int CURR_LOG_TYPE = SDCARD_TYPE;    //当前的日志记录类型

    public static String CURR_INSTALL_LOG_NAME;   //如果当前的日志写在内存中，记录当前的日志文件名称

    public static String logServiceLogName = "Log.log";//本服务输出的日志文件名称
    public static SimpleDateFormat myLogSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static OutputStreamWriter writer;


    public static Process process;

    public static PowerManager.WakeLock wakeLock;

    public static LogService.SDStateMonitorReceiver sdStateReceiver; //SDcard状态监测
    public static LogService.LogTaskReceiver logTaskReceiver;

    /* 是否正在监测日志文件大小；
     * 如果当前日志记录在SDcard中则为false
     * 如果当前日志记录在内存中则为true*/
    private static boolean logSizeMoniting = false;
    public checkUpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }
    @Override//创建服务的时候调用
    public void onCreate(){

        super.onCreate();
        Intent intent = new Intent(this, GrabSeatActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = null;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel mChannel = new NotificationChannel("MonitorServer", "监控助手", NotificationManager.IMPORTANCE_HIGH);
//            manager.createNotificationChannel(mChannel);
//            notification = new Notification.Builder(this)
//                    .setChannelId("MonitorServer")
//                    .setContentTitle("监控助手")
//                    .setContentText("运行中...")
//                    .setWhen(System.currentTimeMillis())
//                    .setSmallIcon(R.drawable.monitor)
//                    .setOngoing(true)
//                    .setVisibility(Notification.VISIBILITY_PUBLIC)
//                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
//                            R.drawable.monitorlarge))
//                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
//                            new Intent[]{new Intent(context,GrabSeat.class)},
//                            PendingIntent.FLAG_CANCEL_CURRENT),false)
//                    .setContentIntent(pi)
//                    .build();
//        }else {
//            notification = new NotificationCompat.Builder(this)
//                    .setContentTitle("监控助手")
//                    .setContentText("运行中...")
//                    .setWhen(System.currentTimeMillis())
//                    .setSmallIcon(R.drawable.monitor)
//                    .setDefaults(Notification.DEFAULT_SOUND)
//                    .setPriority(Notification.PRIORITY_HIGH)
//                    .setOngoing(true)
//                    .setVisibility(Notification.VISIBILITY_PUBLIC)
//                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)
//                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
//                            R.drawable.monitorlarge))
//                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
//                            new Intent[]{new Intent(context,GrabSeat.class)},
//                            PendingIntent.FLAG_CANCEL_CURRENT),false)
//                    .setContentIntent(pi)
//                    .build();
//        }
//        startForeground(50,notification);
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override//每次服务启动的时候调用
    public int onStartCommand(Intent intent, int flags, int startId){
        upgradeApk();
        return super.onStartCommand(intent,flags,startId);
    }
    @Override//服务销毁的时候调用
    public void  onDestroy(){

        super.onDestroy();
        Log.e("GrabService","onDestroy");
    }

    public  void upgradeApk(){
        init();
        createLogDir();
        getVersionFromService();
    }

    public  void init(){
        UPDATE_PATH_MEMORY_DIR = getFilesDir().getAbsolutePath() + File.separator + "update";
        LOG_SERVICE_LOG_PATH = UPDATE_PATH_MEMORY_DIR + File.separator + logServiceLogName;
        UPDATE_PATH_SDCARD_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
                +   "UniconSeatUpdate" + File.separator + "update";
        createLogDir();

    /* ******************************************************
    try {
        writer = new OutputStreamWriter(new FileOutputStream(
                LOG_SERVICE_LOG_PATH, true));
    } catch (FileNotFoundException e) {
        Log.e(TAG, e.getMessage(), e);
    }
    * ******************************************************/
//        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
//        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        CURR_LOG_TYPE = getCurrUpdateType();
        Log.i(TAG, "LogService onCreate");
    }


    /**
     * 获取当前应存储在内存中还是存储在SDCard中
     * @return
     */
    public  int getCurrUpdateType(){
        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            return MEMORY_TYPE;
        }else{
            return SDCARD_TYPE;
        }
    }
    /**
     * 创建安装包目录
     */
    private  void createLogDir() {
        File file = new File(UPDATE_PATH_MEMORY_DIR);
        boolean mkOk;
        if (!file.isDirectory()) {
            mkOk = file.mkdirs();
            if (!mkOk) {
                mkOk = file.mkdirs();
            }
        }

        /*
        file = new File(LOG_SERVICE_LOG_PATH);
        if (!file.exists()) {
            try {
                mkOk = file.createNewFile();
                if (!mkOk) {
                    file.createNewFile();
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        */

        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            file = new File(UPDATE_PATH_SDCARD_DIR);
            if (!file.isDirectory()) {
                mkOk = file.mkdirs();
                if (!mkOk) {
                    return;
                }
            }
        }
    }

    /**
     * 根据当前的存储位置得到日志的绝对存储路径
     * @return
     */
    public  String getUpdatePath(){
        createLogDir();
        String logFileName = "output.json";// 日志文件名称
        if(CURR_LOG_TYPE == MEMORY_TYPE){
            CURR_INSTALL_LOG_NAME = logFileName;
            Log.d(TAG, "Upgrade stored in memory, the path is:" + UPDATE_PATH_MEMORY_DIR + File.separator + logFileName);
            return UPDATE_PATH_MEMORY_DIR + File.separator + logFileName;
        }else{
            CURR_INSTALL_LOG_NAME = null;
            Log.d(TAG, "Upgrade stored in SDcard, the path is:"+UPDATE_PATH_SDCARD_DIR + File.separator + logFileName);
            return UPDATE_PATH_SDCARD_DIR + File.separator + logFileName;
        }
    }

    /*
     * 获取当前程序的版本名
     */
    private  String getVersionName() throws Exception{
        //获取packagemanager的实例
        PackageManager packageManager = getPackageManager();
        //getPackageName()是你当前类的包名，0代表是获取版本信息
        PackageInfo packInfo = packageManager.getPackageInfo(getPackageName(), 0);
        Log.e("UPGRADE","版本号"+packInfo.versionCode);
        Log.e("UPGRADE","版本名"+packInfo.versionName);
        return packInfo.versionName;
    }


    /*
     * 获取当前程序的版本号
     */
    private  int getVersionCode() throws Exception{
        //获取packagemanager的实例
        PackageManager packageManager = getPackageManager();
        //getPackageName()是你当前类的包名，0代表是获取版本信息
        PackageInfo packInfo = packageManager.getPackageInfo(getPackageName(), 0);
        Log.e("TAG","版本号"+packInfo.versionCode);
        Log.e("TAG","版本名"+packInfo.versionName);
        return packInfo.versionCode;
    }

    /**
     * 获取服务器上应用程序
     *
     */
    public  void getApkFromService(){
        final String finalUri = "https://github.com/ManuelNathaniel/uniconseat/raw/master/app/release/app-release.apk";
        final ProgressBar pd = null;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(finalUri);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(5000);
                    float all = conn.getContentLength();
                    Log.e("安装包大小",String.valueOf(all));
                    InputStream is = conn.getInputStream();
                    long time = System.currentTimeMillis();//当前时间的毫秒数

                    File file = new File(UPDATE_PATH_MEMORY_DIR + File.separator + "app-release.apk");

                    FileOutputStream fos = new FileOutputStream(file);
                    BufferedInputStream bis = new BufferedInputStream(is);
                    byte[] buffer = new byte[1024];
                    int len;
                    float total = 0;
                    while ((len = bis.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                        total += len;
                        //获取当前下载量
                        float rate = total/all;
                        showNotification("联创座位系统","正在下载新版" + netVersionName + "... " + rate + "%",context,99,"channel99","检查更新");
                    }
                    fos.close();
                    bis.close();
                    is.close();
                    Handler handlerThree=new Handler(Looper.getMainLooper());
                    handlerThree.post(new Runnable(){
                        public void run(){
                            Toast.makeText(context,"下载完成",Toast.LENGTH_SHORT).show();
                        }
                    });
                }catch (IOException e){
                    e.printStackTrace();
                    Handler handlerThree=new Handler(Looper.getMainLooper());
                    handlerThree.post(new Runnable(){
                        public void run(){
                            Toast.makeText(context,"下载出错",Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                //return file;

            }
        }).start();
    }


    /**
     * 版本号比较
     *0代表相等，1代表version1大于version2，-1代表version1小于version2
     * @param version1
     * @param version2
     * @return
     */
    public int compareVersion(String version1, String version2) {
        if (version1.equals(version2)) {
            return 0;
        }
        String[] version1Array = version1.split("\\.");
        String[] version2Array = version2.split("\\.");
        int index = 0;
        // 获取最小长度值
        int minLen = Math.min(version1Array.length, version2Array.length);
        int diff = 0;
        // 循环判断每位的大小
        while (index < minLen
                && (diff = Integer.parseInt(version1Array[index])
                - Integer.parseInt(version2Array[index])) == 0) {
            index++;
        }
        if (diff == 0) {
            // 如果位数不一致，比较多余位数
            for (int i = index; i < version1Array.length; i++) {
                if (Integer.parseInt(version1Array[i]) > 0) {
                    return 1;
                }
            }

            for (int i = index; i < version2Array.length; i++) {
                if (Integer.parseInt(version2Array[i]) > 0) {
                    return -1;
                }
            }
            return 0;
        } else {
            return diff > 0 ? 1 : -1;
        }
    }


    /**
     * 检查版本是否需要更新
     *
     */
    public  void getVersionFromService(){
        final String finalUri = "https://raw.githubusercontent.com/ManuelNathaniel/uniconseat/master/app/release/output.json";
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //1. 登录
                    final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();
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
                    Request loginRequest = new Request.Builder().url(finalUri).build();//向服务器发送登录请求，包括ID, password
                    Response loginResponse = client.newCall(loginRequest).execute();//执行登录请求
                    String loginReturn = loginResponse.body().string();//得到响应数据
                    /***
                     *  [{
                     *  "apkData":{"type":"MAIN","splits":[],"versionCode":3,"versionName":"3.1.0","enabled":true,"outputFile":"app-release.apk","fullName":"release","baseName":"release"},
                     *  "path":"app-release.apk","properties":{}}]
                     * */

                    JSONArray jsonArray = new JSONArray(loginReturn);
                    String output = jsonArray.getString(0);
                    String apkData = CommonFunction.parseSingleLabel(output, "apkData");
                    Log.e("apkData",apkData);

                    JSONObject apkObject = new JSONObject(apkData);
                    netVersionCode = apkObject.getString("versionCode");
                    netVersionName = apkObject.getString("versionName");
                    try {
                        upTips = apkObject.getString("upTips");
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    Log.e("netVersionCode",netVersionCode);
                    Log.e("netVersionName",netVersionName);

                    curVersionCode = getVersionCode();
                    curVersionName = getVersionName();
                    if (Integer.parseInt(netVersionCode) >= curVersionCode){
                        if (compareVersion(curVersionName,netVersionName) == 1){
                            Handler handlerThree=new Handler(Looper.getMainLooper());
                            handlerThree.post(new Runnable(){
                                public void run(){
                                    Toast.makeText(context,"已是最新版",Toast.LENGTH_SHORT).show();
                                }
                            });
                        }else {
                            Handler handlerThree=new Handler(Looper.getMainLooper());
                            handlerThree.post(new Runnable(){
                                public void run(){
                                    alertDialog();
                                }
                            });
                        }
                    }


                }catch (IOException | JSONException e){
                    e.printStackTrace();
                    Handler handlerThree=new Handler(Looper.getMainLooper());
                    handlerThree.post(new Runnable(){
                        public void run(){
                            Toast.makeText(context,"检查出错",Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Handler handlerThree=new Handler(Looper.getMainLooper());
                    handlerThree.post(new Runnable(){
                        public void run(){
                            Toast.makeText(context,"获取版本信息出错",Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    public  void alertDialog(){
        android.support.v7.app.AlertDialog.Builder builder=new android.support.v7.app.AlertDialog.Builder(context,R.style.dialog_style);
        builder.setIcon(R.drawable.upgrade);
        builder.setTitle("联创座位系统");
        builder.setMessage("已发布新版本" + netVersionName + "，新版特性:\n" + upTips + ",是否前往下载？");
        builder.setCancelable(false);
        builder.setPositiveButton("下载", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getApkFromService();
            }
        });
        builder.setNegativeButton("稍后再说", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

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


    //下载进度
    public void showNotification(String title, String text, Context context, int id,String channelid,String channelname){
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
                    .setDefaults(Notification.DEFAULT_LIGHTS)//振动
                    .setAutoCancel(false)
                    .setSmallIcon(R.drawable.update)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.upgrade))
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
                    .setDefaults(Notification.DEFAULT_LIGHTS)//振动
                    .setAutoCancel(false)
                    .setSmallIcon(R.drawable.update)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.upgrade))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)//悬挂跳转
                    .build();
        }
        manager.notify(id,notification);
    }
    public void showNotification3(String title, String text, Context context, int id, String channelid, String channelname){
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
                    .setAutoCancel(false)
                    .setSmallIcon(R.drawable.update)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.upgrade))
                    .setContentIntent(PendingIntent.getActivities(context,0x0001,
                            new Intent[]{new Intent(context,MainActivity.class)},PendingIntent.FLAG_CANCEL_CURRENT))//跳转
                    .build();
        }else {
            notification = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setColor(Color.parseColor("#00000000"))
                    .setAutoCancel(false)
                    .setSmallIcon(R.drawable.update)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.upgrade))
                    .setContentIntent(PendingIntent.getActivities(context,0x0001,
                            new Intent[]{new Intent(context,MainActivity.class)},PendingIntent.FLAG_CANCEL_CURRENT))//跳转
                    .build();
        }
        manager.notify(id,notification);

    }
}
