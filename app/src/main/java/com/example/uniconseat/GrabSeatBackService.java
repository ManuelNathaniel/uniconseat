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
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.annotation.RequiresApi;
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

public class GrabSeatBackService extends Service {
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


    /*   全局信息标志位   */
    public boolean creditScore = true;//用户信誉是否受限
    private static String finalStateTime,finalStateDevname; //最终座位状态信息  finalStateTime在登录预约的时候被赋值  finalStateDevname在解析座位状态的时候被赋值
    private static String ToastText = "";//预约状态提示信息
    public int prefSeatState=0;//预约指定座位的结果。0：可以进行预约指定座位；1：用户没有指定座位/指定座位输入错误不存在/预约失败
    private static int empty_num = 0; //空闲座位数量
    public static int actok = 0;//表示操作的结果状态，分为5种
    public boolean sessionNull = false;//会话为空时表示登录失败

    //  全局信息传递
    private static String username,studentid,password,roomid,roomname; //用户信息
    public String prefSeat,begintime;//首选座位  开始时间
    public int begin;//开始时间整型，
    static String[] userInfo= new String[5];//userInfo中分别存储 username studentid password roomid url字段最后部分

    Vibrator vibrator;//手机振动器
    String endTime;
    public boolean ending = false;//是否到了停止服务的时间
    public boolean sta;//存在本地的座位状态 用来定时启动服务的时候便于访问前一次的结果
    public boolean finalsta = false;//全局可访问的座位状态，以便在销毁活动时及时检测

    // url共用字段
    String urlCommonFirstPara = "http://202.206.242.87/ClientWeb/pro/ajax/device.aspx?byType=devcls&classkind=8&display=fp&md=d&room_id=";
    String urlCommonSecondPara = "&purpose=&selectOpenAty=&cld_name=default&date=";
    String urlCommonThirdPara = "&act=get_rsv_sta&_=";


    public  static  AlarmManager alarmManager;
    public static PendingIntent pi;
    private String[] grabArray = new String[4];
    public static boolean GrabService = false;

    //通知段id 50-59
    public GrabSeatBackService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        // throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }
    @Override//创建服务的时候调用
    public void onCreate(){
        Log.e("GrabService","启动");
        GrabService = true;
        super.onCreate();
        Intent intent = new Intent(this, GrabSeatActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel("MonitorServer", "监控助手", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(mChannel);
            notification = new Notification.Builder(this)
                    .setChannelId("MonitorServer")
                    .setContentTitle("监控助手")
                    .setContentText("运行中...")
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.monitor)
                    .setOngoing(true)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.monitorlarge))
                    .setFullScreenIntent(PendingIntent.getActivities(GrabSeatBackService.this,0,
                            new Intent[]{new Intent(GrabSeatBackService.this,GrabSeat.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .setContentIntent(pi)
                    .build();
        }else {
            notification = new NotificationCompat.Builder(this)
                    .setContentTitle("监控助手")
                    .setContentText("运行中...")
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.monitor)
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setOngoing(true)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.monitorlarge))
                    .setFullScreenIntent(PendingIntent.getActivities(GrabSeatBackService.this,0,
                            new Intent[]{new Intent(GrabSeatBackService.this,GrabSeat.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .setContentIntent(pi)
                    .build();
        }
        startForeground(50,notification);

        SharedPreferences pref = getSharedPreferences("userInfo",MODE_PRIVATE);
        grabArray[0] = pref.getString("grab1","219");
        grabArray[1] = pref.getString("grab2","320");
        grabArray[2] = pref.getString("grab3","");
        grabArray[3] = pref.getString("grab4","");
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override//每次服务启动的时候调用
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.e("GrabService：start","运行");

        SharedPreferences endTimePref = getSharedPreferences("endTime",MODE_PRIVATE);
        endTime = endTimePref.getString("endTime","");
        sta = endTimePref.getBoolean("state",false);

        int en = CommonFunction.timeCompare(CommonFunction.systemTime(),endTime);//时间比较  1 超过结束时间 2 已到结束时间 3 未到结束时间

        if (en == 3 & !sta){
            Log.e("GrabService：运行","going");
            backMonitor();
            ending = false;
            int maxDiff = 100;//最大检测间隔20s 单位100ms
            Random a = new Random();
            int aa = a.nextInt(maxDiff);
            Log.e("GrabService：随机间隔",Integer.toString(aa));
            Log.e("GrabService：当前时间",CommonFunction.systemTime());
            long triggerAtTime = SystemClock.elapsedRealtime() + aa * 100;    //下一次启动的时间

            try {
                Log.e("GrabService：下次启动时间",df.format(CommonFunction.longToDate(triggerAtTime,"yyyy-MM-dd HH:mm:ss")));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            /*  获取定时服务  */
            alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            Intent intent1 = new Intent(this,GrabSeatBackService.class);
            pi = PendingIntent.getService(this,0,intent1,0);
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
        }else {
            Log.e("GrabService","结束");
            ending = true;
            stopSelf();
        }

        return super.onStartCommand(intent,flags,startId);
    }
    @Override//服务销毁的时候调用
    public void  onDestroy(){
        GrabService = false;
        SharedPreferences endTimePref1 = getSharedPreferences("endTime",MODE_PRIVATE);
        sta = endTimePref1.getBoolean("state",false);
        if (ending & !sta & !finalsta ){//在结束的时候还没预约到座位
            Log.e("GrabService","close");
            showNotification3("后台监控","当前阅览室无空闲座位，请切换阅览室",GrabSeatBackService.this,55,50,"channel55","后台监控");
        }else if(finalsta | sta){
            SharedPreferences.Editor starttime = getSharedPreferences("endTime",MODE_PRIVATE).edit();
            starttime.putBoolean("state",true);//写入结束时间和是否有座位
            starttime.apply();
        }
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent1 = new Intent(this,GrabSeatBackService.class);
        PendingIntent pi = PendingIntent.getService(this,0,intent1,0);
        alarmManager.cancel(pi);
        super.onDestroy();
        Log.e("GrabService","onDestroy");
    }

    public void backMonitor(){
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

                    getUserInfoFromScreen();
                    int succeed; String result;
//通过检测登录排除操作状态7，通过积分信息排除操作状态4，通过检测个人预约排除操作状态2；通过有无座位余量排除状态0；
//                    client = getCredit(client);//获取用户的积分信息，以确认是否可以进行预约.该过程会给出操作状态4/7/9的判断

                    empty_num = 0;
                    if (!ending & !sta){//如果积分不为0，则可以进行预约
//                         如果已经运行该部分内容，则操作状态4(积分不足)、操作状态6(其他异常)和操作状态7(登录成功)在接下来不必考虑，只考虑操作状态0/1/2/3/5/6/8
                        client = obtainAndParseStatus(client);// 获取并解析座位状态数据
                        result =loadRsvSta("currentSeat");
                        succeed = parseStatusOfEachSeatAndReserve(result,client);
                        empty_num = 0;
                        //actok = 0; succeed = 0;
                    }else {//如果积分为0，则显示禁止时间，并退出
                        //此处为操作状态4/6
                        tips();//处理结果信息
                        actok = 0; creditScore = true; sessionNull = false;//标志位恢复
                        return;
                    }
                    //当用户积分满足时才运行以下步骤，此处只存在操作状态0/1/3，操作状态5在执行过程以判断
                    if (succeed!=1 ){//如果用户没有预约，且积分满足预约条件，方可启动后台监控服务
                        //此部分只存在0/3/6操作状态
                        switch (actok){
                            case 0:
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
                    }else {//此处表示操作状态1   预约成功通知    该通知必须为悬挂不回收通知，等待用户滑动删除，默认振动+默认铃声
                        SharedPreferences.Editor starttime = getSharedPreferences("endTime",MODE_PRIVATE).edit();
                        starttime.putBoolean("state",true);//写入结束时间和是否有座位
                        starttime.apply();
                        showNotification4("座位锁定",ToastText,GrabSeatBackService.this,1,"channel1","座位锁定");
                        finalsta = true;
                        ToastText = "";
                        vibrator =  (Vibrator) getSystemService(VIBRATOR_SERVICE);//获得 一个震动的服务
                        vibrator.vibrate(2500);
                    }
                    empty_num = 0;
                }catch (Exception e){
                    e.printStackTrace();
                    //访问异常，无法连接服务器！
                    if (cookieStore.size()==0){
                        Handler handlerThree=new Handler(Looper.getMainLooper());
                        handlerThree.post(new Runnable(){
                            public void run(){
                                Toast.makeText(GrabSeatBackService.this,"访问异常，无法连接服务器！",Toast.LENGTH_SHORT).show();
                            }
                        });
                        vibrator =  (Vibrator) getSystemService(VIBRATOR_SERVICE);//获得 一个震动的服务
                        vibrator.vibrate(2000);
                    }
                    empty_num = 0;
                }
            }
        }).start();
    }

/*-------------------------------------------------------------------------------------------------------------------------------------------------
                                                                  操作函数
-------------------------------------------------------------------------------------------------------------------------------------------------*/
    //获取我的座位状态信息2019.6.22
    public String[] getMyReserve(String myid, String mypwd, OkHttpClient client, final Context context) throws IOException, JSONException {//必须在子线程中运行，同意cookie访问
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
            Log.e("GrabService:解析我的预约信息",data);

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("GrabService:解析出错","err");
        }

        if (data == null){//解析出错
            Handler handlerThree=new Handler(Looper.getMainLooper());
            handlerThree.post(new Runnable(){
                public void run(){
                    showDialog("预约小提示","获取座位信息出错",context);

                }
            });
        }else if (data.equals("[]")){//没有预约{"ret":1,"act":"set_resv","msg":"操作成功！","data":null,"ext":null}
            Log.e("GrabService","没有预约");
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
        //直到检索完该阅览室的所有座位，用户的预约状态为0，空闲座位为0，则提示该阅览室当前无所需座位
        if (empty_num == 0){
            actok = 0;
            Handler handlerThree=new Handler(Looper.getMainLooper());
            handlerThree.post(new Runnable(){
                public void run(){
                    Toast.makeText(GrabSeatBackService.this,"当前时间内所选阅览室无座", Toast.LENGTH_SHORT).show();
                }
            });
            //showNotification0("选座提示","当前时间内所选阅览室无座",GrabSeatBackService.this,3,30,"channel3","选座提示");
            return 0;
        }else {
            Handler handlerThree=new Handler(Looper.getMainLooper());
            handlerThree.post(new Runnable(){
                public void run(){
                    Toast.makeText(GrabSeatBackService.this,"当前阅览室座位： "+ empty_num + " / " + jsonArray.length(), Toast.LENGTH_SHORT).show();
                }
            });
            /*  1. 用户指定了首选座位    */
            //四阅-209/order/address
            String[] res = new String[3];
            if (!prefSeat.equals("") & prefSeatState==0){
                //此处对指定座位只存在操作状态1/3/5/6/8，如果是5则直接到下一步继续检索，如果是3，则退出，并给出提示
                Log.e("GrabService：首选座位号",prefSeat);
                //1.1 先对指定座位进行操作
                res = parseSeatId(roomname,prefSeat);
                Log.e("GrabService：指定阅览室",roomname);
                int r = loginAndSetReserve(client,res[2]);
                Log.e("GrabService：首选座位id",res[2]);

                if (r != 1 & r != 2 & r != 3){
                    for (int i = 0; i < grabArray.length; i++){
                        if (grabArray[i].equals("")){
                            Log.e("第" + i +"个候选座位为空","no");
                        }else {
                            res = parseSeatId(roomname,grabArray[i]);
                            r = loginAndSetReserve(client,res[2]);
                            if (r == 1 | r == 2){
                                break;
                            }
                        }
                    }
                }

                if (r == 5 ){//预约冲突
                    prefSeatState = 1;//指定座位操作失败
                    showNotification0("选座提示","指定座位已有预约,正在为您查找其他座位",GrabSeatBackService.this,3,15,"channel3","选座提示");
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
                Log.e("GrabService","寻找空闲座位");
                if (prefSeat.equals("")){//如果没有指定，则随机生成
                    Random a = new Random();
                    int aa = a.nextInt(jsonArray.length());
                    res[1] = String.valueOf(aa);
                    Log.e("GrabService：随机座位",Integer.toString(aa));
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
                }else if (s == 1){//预约成功/已有预约
                    r = 1;
                    tips();
                    return r;
                }else if (s == 3 | s == 6 | s == 8){//未开放、时间短于1小时、时间长于15小时都直接退出
                    return 0;
                }else if (s == 5){//系统未开放
                    continue;
                }
                Log.e(ps,String.valueOf(s));//预约的结果分析/类别归属
            }
        }
        return r;
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
            prefSeat = result[0].substring(3,6);//自动更改并显示
            showDialog("座位号异常","已自动更改为"+result[0],GrabSeatBackService.this);
        }
        return result;
    }
    //登录并进行预约       reture 1/2/3/4/5/6 对应不同的操作结果
    public  int loginAndSetReserve(OkHttpClient client,String dev_id) throws IOException {
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
        String end = CommonFunction.getMatcher("(.*) ",reserve)+"+22%3A00";//2019-06-04+22%3A30
        String start_time = CommonFunction.getMatcher("(.*):",time)+CommonFunction.getMatcher(":(.*)",time);//start_time=1128
        String end_time = "2200";//end_time=2230
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
        Log.e("GrabService：预约参数",setRsvResponseData);

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
                Handler handlerThree=new Handler(Looper.getMainLooper());
                handlerThree.post(new Runnable(){
                    public void run(){
                        showDialog("预约小提示",ToastText,GrabSeatBackService.this);
                    }
                });
                break;
            case 3://系统未开放
                //该信息重要等级为 LOW，点击空白处可取消Dialog
                final String cu  = df.format(getNetTime());
                Handler handlerThree3=new Handler(Looper.getMainLooper());
                handlerThree3.post(new Runnable(){
                    public void run(){
                        showDialog("预约小提示","请在6:30之后再进行预约; 预约端当前时间"+ cu,GrabSeatBackService.this);
                    }
                });
                break;
            case 4://积分不足
                //该信息重要等级为 HIGH，用户必须与之交互
                Handler handlerThree4=new Handler(Looper.getMainLooper());
                handlerThree4.post(new Runnable(){
                    public void run(){
                        showDialogNotCancel("预约提示",re[0]+":剩余积分为"+re[2]+",在"+re[3]+"期间被禁止预约",GrabSeatBackService.this);
                    }
                });
                break;
            case 5://预约冲突
                //该信息重要等级为 LOW，点击空白处可取消Dialog
                Handler handlerThree5=new Handler(Looper.getMainLooper());
                handlerThree5.post(new Runnable(){
                    public void run(){
                        showDialog("预约小提示","与现有预约存在冲突",GrabSeatBackService.this);
                    }
                });
                break;
            case 6://预约时间少于1小时
                Handler handlerThree6=new Handler(Looper.getMainLooper());
                handlerThree6.post(new Runnable(){
                    public void run(){
                        showDialog("预约小提示",ToastText,GrabSeatBackService.this);
                    }
                });
                break;
            case 7://登录成功
                break;
            case 8://预约时间大于15小时
                Handler handlerThree8=new Handler(Looper.getMainLooper());
                handlerThree8.post(new Runnable(){
                    public void run(){
                        showDialog("预约小提示",ToastText,GrabSeatBackService.this);
                    }
                });
                break;
            case 9://其他信息
                if (CommonFunction.regexMatcher("(参数有误)",ToastText)){
                    Handler handlerThree9=new Handler(Looper.getMainLooper());
                    handlerThree9.post(new Runnable(){
                        public void run(){
                            Toast.makeText(GrabSeatBackService.this,"座位号不存在",Toast.LENGTH_SHORT).show();
                        }
                    });
                }else if (CommonFunction.regexMatcher("(未登录)",ToastText)){
                    Handler handlerThree10=new Handler(Looper.getMainLooper());
                    handlerThree10.post(new Runnable(){
                        public void run(){
                            Toast.makeText(GrabSeatBackService.this,"用户名/密码错误",Toast.LENGTH_SHORT).show();
                        }
                    });

                }else {
                    Handler handlerThree11=new Handler(Looper.getMainLooper());
                    handlerThree11.post(new Runnable(){
                        public void run(){
                            showDialog("预约提示",ToastText,GrabSeatBackService.this);
                        }
                    });
                }
                break;
            default:
                break;
        }
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
                .setSmallIcon(R.drawable.monitor)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.drawable.monitorlarge))
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
                .setSmallIcon(R.drawable.monitor)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.drawable.monitorlarge))
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
                    .setSmallIcon(R.drawable.monitor)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.monitorlarge))
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
                    .setSmallIcon(R.drawable.monitor)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.monitorlarge))

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
                    .setSmallIcon(R.drawable.monitor)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.monitorlarge))
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
                    .setSmallIcon(R.drawable.monitor)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.monitorlarge))
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
                    .setSmallIcon(R.drawable.monitor)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.monitorlarge))
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
                    .setSmallIcon(R.drawable.monitor)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.monitorlarge))

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
                    .setSmallIcon(R.drawable.monitor)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.monitorlarge))
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
                    .setSmallIcon(R.drawable.monitor)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.monitorlarge))
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
                    .setSmallIcon(R.drawable.monitor)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.monitorlarge))
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
                    .setSmallIcon(R.drawable.monitor)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.monitorlarge))
                    .setContentIntent(PendingIntent.getActivities(context,0x0001,
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},PendingIntent.FLAG_CANCEL_CURRENT))//跳转
                    .build();
        }
        String tag = id + "float";
        manager.notify(tag,id,notification);
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
                    .setSmallIcon(R.drawable.monitor)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.monitorlarge))
                    .setContentIntent(PendingIntent.getActivities(context,0x0001,
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},PendingIntent.FLAG_CANCEL_CURRENT))//跳转
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
                    .setSmallIcon(R.drawable.monitor)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.monitorlarge))
                    .setContentIntent(PendingIntent.getActivities(context,0x0001,
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},PendingIntent.FLAG_CANCEL_CURRENT))//跳转
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
                    .setSmallIcon(R.drawable.monitor)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.monitorlarge))
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
                    .setSmallIcon(R.drawable.monitor)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.monitorlarge))
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
                            R.drawable.seatlock))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,GrabSeat.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .setContentIntent(PendingIntent.getActivities(context,0x0001,
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},PendingIntent.FLAG_CANCEL_CURRENT))
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
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},PendingIntent.FLAG_CANCEL_CURRENT))
                    .build();
        }

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
            Log.e("YSU网络时间",df.format(webDate));
        }catch (Exception e){
            Handler handlerThree=new Handler(Looper.getMainLooper());
            handlerThree.post(new Runnable(){
                public void run(){
                    Toast.makeText(GrabSeatBackService.this,"请重启手机或重新开启网络!",Toast.LENGTH_SHORT).show();
                }
            });
            webDate = defDate;
            Log.e("YSU尝试获取网络时间出错",e.toString());
        }
        return webDate;
    }
    //从服务器获取座位状态信息（包含解析与保存步骤）
    public OkHttpClient obtainAndParseStatus(OkHttpClient client) throws IOException {
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
    //获取用户输入的信息,username,studentid,password,roomid
    public String[] getUserInfoFromScreen(){
        // 保存用户数据
        SharedPreferences userInfopref = getSharedPreferences("userInfo",MODE_PRIVATE);
        username = userInfopref.getString("username","");
        studentid = userInfopref.getString("studentid","");
        password = userInfopref.getString("password","");
        roomid = userInfopref.getString("roomid","");
        userInfo[4] = userInfopref.getString("room","");
        prefSeat = userInfopref.getString("prefseat","");
        begintime = userInfopref.getString("begintime","");
        //userInfo中分别存储 username studentid password roomid url字段最后部分
        userInfo[0] = username;
        userInfo[1] = studentid;
        userInfo[2] = password;
        userInfo[3] = roomid;

        if (userInfo[4].equals("100457211")){
            roomname = "一阅";
        }else if (roomid.equals("100457213")){
            roomname = "二阅";
        }else if (roomid.equals("101439229")){
            roomname = "三阅";
        }else if (roomid.equals("101439231")){
            roomname = "四阅";
        }else if (roomid.equals("101439233")){
            roomname = "五阅";
        }else if (roomid.equals("100457221")){
            roomname = "树华A";
        }

        for (int i = 0; i < 4; i ++){
            Log.e(String.valueOf(i),userInfo[i]);
        }
        Log.e("begin",begintime);Log.e("pref",prefSeat);
        return userInfo;
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
//            Handler handlerThree=new Handler(Looper.getMainLooper());
//            handlerThree.post(new Runnable(){
//                public void run(){
//                    Toast.makeText(GrabSeatBackService.this,re[0]+"  当前积分:"+re[2],Toast.LENGTH_SHORT).show();
//                }
//            });
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
        parseResponseMsg(loginReturn);//登录结果只有两种 登录成功 和 登录失败
        if (actok == 9){//登录失败
            sessionNull = true;
        }else if (actok == 7){//登录成功
            //解析积分
            parseJSONScore(parseJSONResponse(loginReturn));
        }
        actok = 0;
        return client;
    }
}
