package com.example.uniconseat;

import android.annotation.SuppressLint;
import android.app.FragmentManager;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mcsoft.timerangepickerdialog.RangeTimePickerDialog;

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

public class HomeActivity extends BaseActivity implements RangeTimePickerDialog.ISelectedTime{
    static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
    static String str1;//用户名
    static String str2;//密码
    static String str3;//时间
    static String str4;//结束时间
    public final  String Tag = "test";
    Button startButton;
    Button endButton;

    String responseData;
    String ret,act,msg;
    //控制手机振动器
    Vibrator vibrator;

    static String TAG;//notification用的全局信息
    static int Id;
    static int Length;
    private String errorCode;

    //定义一些时间全局变量
    public String nowTime;//系统时间 date + time
    public String start,end,start_time,end_time,resvTime;//开始/结束时间，开始/结束日期+时间，最终设定的预约时间
    public String now,nowHour,nowMinute,resvHour,resvMinute;//当前time，当前小时/分钟，预约小时/分钟
    private String resvEndTime,resvEndHour,resvEndMinute;
    public String[] myResv = new String[5];
    public String prefEndTime,prefHour,prefMinute;//用户所预约座位原来的结束时间
    public String prefStartTime,prefStartHour,prefStartMinute;
    public int ph,pm;////用户所预约座位原来的结束时间整型
    public int psh,psm;
    public int hour,minute;
    public String time;
    public int actok;
    public String ToastText;
    private boolean startSelect = false;
    private boolean endSelect = false;

    private static final int OPENPROGRESS = 10;//打开
    private static final int CLOSEPROGRESS = 0;//关闭
    private static final int SEATERROR = 1;//座位号异常
    private static final int RESPONSEREQUEST = 2;//相应请求
    private static final int GETCREDIT = 3;//获取积分信息
    private static final int ACCESSDATA = 4;//获取座位数据
    private static final int RESERVE = 5;//正在预约
    private static final int CREDITINFO = 6;//积分信息
    private static final int CHECKCOUNTONE = 7,CHECKCOUTTWO = 8,CANCELRESEV = 9, TRANSFER = 11;
    private static final int CHECKTIME = 12,GETORISEAT = 13;
    private static RelativeLayout relativeLayout;
    private static TextView progressTextView;
    private static ProgressBar progressBar;

    // url共用字段
    String urlCommonFirstPara = "http://202.206.242.87/ClientWeb/pro/ajax/device.aspx?byType=devcls&classkind=8&display=fp&md=d&room_id=";
    String urlCommonSecondPara = "&purpose=&selectOpenAty=&cld_name=default&date=";
    String urlCommonThirdPara = "&act=get_rsv_sta&_=";
/*----------------------------------------------------------------------------------------------------------------------------------------------------------------*/
/*----------------------------------------------------------------------------------------------------------------------------------------------------------------*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        //获取系统的Vibrator服务
        vibrator=(Vibrator)getSystemService(Service.VIBRATOR_SERVICE);
        //进度条
        relativeLayout = findViewById(R.id.progressBar);
        progressTextView = findViewById(R.id.progresstips);
        progressBar = findViewById(R.id.progress);

        //启动公告滚动条
        AutoScrollTextView autoScrollTextView = (AutoScrollTextView)findViewById(R.id.leaveTips);
        autoScrollTextView.init(getWindowManager());
        autoScrollTextView.startScroll();

        //悬浮按钮点击事件
        FloatingActionButton fab = findViewById(R.id.comfab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, R.string.home_tips1, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        //寻找布局中的空间的id
        startButton = findViewById(R.id.starttime);
        endButton = findViewById(R.id.endtime);

        //获取用户的历史记录并显示到屏幕
        SharedPreferences pref = getSharedPreferences("leaveData",MODE_PRIVATE);
        str1 = pref.getString("Username","1501041000");
        str2 = pref.getString("Password","");
        str3 = pref.getString("Time","");
        str4 = pref.getString("endtime","22:30");

        EditText editText = findViewById(R.id.editText);
        editText.setText(str1);
        EditText editText2 = findViewById(R.id.editText2);
        editText2.setText(str2);
        startButton.setText(str3);
        endButton.setText(str4);

        Log.e("str1",str1);
        Log.e("str2",str2);
        Log.e("str3",str3);
        Log.e("str4",str4);

        //系统当前时间
        nowTime = CommonFunction.systemTime();
        Log.e("nowTime",nowTime);

        //从系统当前时间中解析出hour 和 minute       str3 =  "08:00"   start_time = 0800
        now = CommonFunction.getMatcher("(.*):", nowTime.substring(11,16))+CommonFunction.getMatcher(":(.*)",nowTime.substring(11,16));
        nowHour = now.substring(0,2);       Log.e("nowHour",nowHour);
        nowMinute = now.substring(2,4);     Log.e("nowMinute",nowMinute);

        //监听时间选择事件（用户进行了时间选择）
        timeSelectMonitor();

        //监听运行按钮响应事件（用户没有进行选择时间的操作）
        imageButtonMonitor();
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
    //时间选择按钮两个,每次选择只能点击一个
    public void timeSelectMonitor(){
        //两个按钮只能点击一个
        Log.e("用户点击了按钮","用户点击了按钮");
        Button starttime = findViewById(R.id.starttime);
        Button endtime = findViewById(R.id.endtime);

        starttime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (endSelect){
                    //默认为false，如果为true，则另一个按钮不能被点击
                }else {
                    showCustomDialogTimePicker();
                    endSelect = false;
                }
            }

        });
        endtime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (startSelect){

                }else {
                    showCustomDialogTimePicker();
                    startSelect = false;
                }
            }
        });
    }
    //时间选择窗口
    @Override
    public void onSelectedTime(int hourStart, int minuteStart, int hourEnd, int minuteEnd) {
        // Use parameters provided by Dialog
        //如果开始时间小于7:00
        Log.e("str3",str3);
        if (hourStart < 7){
            hourStart = 7;
            minuteStart = 40;
            str3 = "0" + hourStart + ":" + minuteStart;
            Toast.makeText(HomeActivity.this,"时间设定不得小于7:00，已自动为您更改",Toast.LENGTH_SHORT).show();
        }else if (hourStart < 10){//hour<10
            if (minuteStart < 10){//minute & hour < 10
                str3 = "0" + hourStart + ":" + "0" + minuteStart;
            }else {//hour < 10  minute > 10
                str3 = "0" + hourStart + ":" + minuteStart;
            }
        }else {//hour > 10
            if (minuteStart < 10){//hour > 10  minute < 10
                str3 = hourStart + ":" + "0" + minuteStart;
            }else {//hour > 10  minute > 10
                str3 = hourStart + ":" + minuteStart;
                Log.e("str3",str3);
            }
        }

        if (hourEnd < 7){
            minuteEnd = 40;
            str4 = "0" + hourEnd + ":" + minuteEnd;
            Toast.makeText(HomeActivity.this,"时间设定不得小于7:00，已为您自动更改",Toast.LENGTH_SHORT).show();
        }else if (hourEnd < 10){//hour<10
            if (minuteEnd < 10){//minute & hour < 10
                str4 = "0" + hourEnd + ":" + "0" + minuteEnd;
            }else {//hour < 10  minute > 10
                str4 = "0" + hourEnd + ":" + minuteEnd;
            }
        }else {//hour > 10
            if (minuteEnd < 10){//hour > 10  minute < 10
                str4 = hourEnd + ":" + "0" + minuteEnd;
            }else {//hour > 10  minute > 10
                str4 = hourEnd + ":" + minuteEnd;
            }
        }

        if (startSelect){
            startSelect = false;
        }else {
            endSelect = false;
        }
        //设置并显示

        startButton.setText(str3);
        endButton.setText(str4);
        resvTime = str3;
        resvEndTime = str4;

        //判断是否正确
        resvHour = CommonFunction.getMatcher("(.*):",resvTime);
        resvMinute = CommonFunction.getMatcher(":(.*)",resvTime);
        Log.e("resvTime",resvTime);
        if ( ( (Integer.parseInt(resvHour) - Integer.parseInt(nowHour)) * 60 + Integer.parseInt(resvMinute) - Integer.parseInt(nowMinute) ) <= 0 ){
            String reserve = df.format(new Date().getTime()+ 20 * 60 * 1000);
            str3 = resvTime = reserve.substring(11,16);//预约开始时间
            startButton.setText(resvTime);
            Log.e("resvTime",resvTime);

            resvHour = CommonFunction.getMatcher("(.*):",resvTime);
            resvMinute = CommonFunction.getMatcher(":(.*)",resvTime);
            ToastCustom.passValue(2000,1000,3,0,100);
            ToastCustom.getInstance(getApplicationContext()).show("时间选择错误，已自动更改为20分钟后"+ resvTime, 2000);
        }
        //时间选择完毕后监听运行按钮
        imageButtonMonitor();
    }
    public void showCustomDialogTimePicker() {
        // Create an instance of the dialog fragment and show it
        Calendar calendar = Calendar.getInstance();
        RangeTimePickerDialog dialog = new RangeTimePickerDialog();
        dialog.newInstance();//默认方法
        dialog.setIs24HourView(true);//把时间设置为24小时格式
        dialog.setRadiusDialog(20);//设置对话半径
        dialog.setTextTabStart("Start");//设置选项卡开始文本
        dialog.setTextTabEnd("End");//设置选项卡结束文本
        dialog.setTextBtnPositive("Accept");//设置正按钮文本
        dialog.setTextBtnNegative("Close");//设置负按钮文本
        dialog.setValidateRange(true);//如果要验证范围时间（开始时间<结束时间），则设置为true。如果要随时选择，请设置为false
        dialog.setMessageErrorRangeTime("请重新选择");//设置选择结束时间大于开始时间时出现的消息错误（仅当“validateRange”为真时）
        dialog.setColorBackgroundHeader(R.color.colorPrimary);//设置标题对话框的背景颜色
        dialog.setColorBackgroundTimePickerHeader(R.color.colorPrimary);//设置标题timePicker的背景颜色
        dialog.setColorTextButton(R.color.colorPrimaryDark);//设置按钮文本颜色
        dialog.enableMinutes(true);//启用或禁用分钟时钟
        dialog.setStartTabIcon(R.drawable.ic_access_time_black_24dp);//更改开始选项卡图标的方法
        dialog.setEndTabIcon(R.drawable.ic_timelapse_black_24dp);//更改结束选项卡图标的方法
//        dialog.setMinimumSelectedTimeInMinutes(0,false);
        dialog.setInitialOpenedTab(RangeTimePickerDialog.InitialOpenedTab.START_CLOCK_TAB);//选择打开时选择了哪个选项卡的方法（START_CLOCK_TAB或END_CLOCK_TAB）

        dialog.setInitialStartClock(calendar.get(Calendar.HOUR_OF_DAY),calendar.get(Calendar.MINUTE)+40);//初始化开始时间
        dialog.setInitialEndClock(22,30);//初始化结束时间
        FragmentManager fragmentManager = getFragmentManager();
        dialog.show(fragmentManager, "");
    }

    //监听运行按钮
    public void imageButtonMonitor(){
        ImageButton button1 = findViewById(R.id.button);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = findViewById(R.id.editText);
                EditText editText2 = findViewById(R.id.editText2);

                //如果开始时间为空，用户没有点击选择时间的按钮
                if (str3.isEmpty()){
                    String reserve = df.format(new Date().getTime()+ 20 * 60 * 1000);
                    str3 = resvTime = reserve.substring(11,16);//预约开始时间
                    startButton.setText(resvTime);
                    Log.e("如果开始时间为空resvTime",resvTime);
                    resvHour = CommonFunction.getMatcher("(.*):",resvTime);
                    resvMinute = CommonFunction.getMatcher(":(.*)",resvTime);
                    ToastCustom.passValue(2000,1000,1,0,100);
                    ToastCustom.getInstance(getApplicationContext()).show("时间选择错误，已自动更改为20分钟后"+ resvTime, 2000);
                }

                str1 = editText.getText().toString();
                str2 = editText2.getText().toString();
                str3 = startButton.getText().toString();
                str4 = endButton.getText().toString();

                SharedPreferences.Editor editor = getSharedPreferences("leaveData",MODE_PRIVATE).edit();
                editor.putString("Username",str1);
                editor.putString("Password",str2);
                editor.putString("endtime",str4);
                editor.apply();
                Run_click(v);
            }
        });
    }
    //运行按钮响应过程
    public boolean Run_click(View view){
        final HashMap<String, List<Cookie>> cookieStoreh = new HashMap<>();
        Log.e("fadklfdj","线程开启");
        new Thread(new Runnable(){
            @Override
            public void run(){
                try{               //实现同一个cookie访问
                    final OkHttpClient client = new OkHttpClient.Builder()
                            .cookieJar(new CookieJar() {
                                @Override
                                public void saveFromResponse(HttpUrl httpUrl, List<Cookie> list) {
                                    cookieStoreh.put(httpUrl.host(), list);
                                }
                                @Override
                                public List<Cookie> loadForRequest(HttpUrl httpUrl) {
                                    List<Cookie> cookies = cookieStoreh.get(httpUrl.host());
                                    return cookies != null ? cookies : new ArrayList<Cookie>();
                                }
                            })
                            .build();
                    //showNotification0("重新预约","正在响应您的请求，请稍后",getApplicationContext(),4,20,"channel4","选座提示");
                    handlerProgress(10);
                    handlerProgress(2);
                    //1. 登录
                    String loginUrl = new StringBuilder().append("http://seat.ysu.edu.cn/ClientWeb/pro/ajax/login.aspx?act=login&id=").append(str1).append("&pwd=")
                            .append(str2).append("&role=512&aliuserid=&schoolcode=&wxuserid=&_nocache=1551511783772")
                            .toString();
                    Request loginRequest = new Request.Builder().url(loginUrl).build();//向服务器发送登录请求，包括ID, password
                    Response loginResponse = client.newCall(loginRequest).execute();//执行登录请求
                    String loginReturn = loginResponse.body().string();//得到响应数据


                    //2. 登录结果分析
                    parseResponseMsg(loginReturn);//登录结果只有两种 登录成功 和 登录失败
                    if (actok != 7 ){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showDialog("操作异常",ToastText,HomeActivity.this);
                            }
                        });
                        return;
                    }

                    //3. 获取我的座位的时间信息
                    handlerProgress(13);
                    myResv = getMyReserve(str1,str2,client,HomeActivity.this);
                    if (myResv[1].equals("no") & myResv[4].equals("no")){
                        handlerProgress(0);
                        return;//如果之前没有，直接结束，并提示
                    }else {//如果有座位则重新预约
                        //2.    根据预约时长的限制，判断设定的时间是否可以预约   "timeDesc":"06/22 15:00-22:30"
                        handlerProgress(12);
                        prefStartTime = myResv[4].substring(6,11);
                        prefEndTime = myResv[4].substring(12,17);

                        prefHour = CommonFunction.getMatcher("(.*):",prefEndTime);
                        prefMinute = CommonFunction.getMatcher(":(.*)",prefEndTime);
                        ph = Integer.parseInt(prefHour);
                        pm = Integer.parseInt(prefMinute);

                        prefStartHour = CommonFunction.getMatcher("(.*):",prefStartTime);
                        prefStartMinute = CommonFunction.getMatcher(":(.*)",prefStartTime);
                        psh = Integer.parseInt(prefStartHour);
                        psm = Integer.parseInt(prefStartMinute);
                    }

                    //4. 当前预约时间到原始预约时间是否大于1小时
                    if ( ( ( ph - Integer.parseInt(resvHour) )* 60 + pm - Integer.parseInt(resvMinute ) ) > 60){
                        start = CommonFunction.getMatcher("(.*) ",nowTime) + "+" +CommonFunction.getMatcher("(.*):",resvTime)+"%3A"+CommonFunction.getMatcher(":(.*)",resvTime);
                        start_time = CommonFunction.getMatcher("(.*):",resvTime) + CommonFunction.getMatcher(":(.*)",resvTime);
                        end = CommonFunction.getMatcher("(.*) ",nowTime) + "+" + prefHour + "%3A" + prefMinute;//原始结束时间
                        end_time = prefHour + prefMinute;
                    }else {
                        //预约时间少于1小时，谨慎预约
                        handlerProgress(0);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ToastCustom.passValue(2000,1000,1,0,100);
                                ToastCustom.getInstance(getApplicationContext()).show("当前时间到原始预约时间" + prefEndTime + "小于1小时，系统已自动退出", 2000);
                            }
                        });
                        return;
                    }
                    //重新预约
                    runRefresh(client);
                }catch(Exception e){
                    e.printStackTrace();
                    errorCode = e.toString();
                    handlerProgress(0);
                    //如果没有正确执行，提示用户核对输入信息和用户当前时间是否可预约
                    if (cookieStoreh.size()==0){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(HomeActivity.this,"服务器异常，登录失败！",Toast.LENGTH_SHORT).show();
                            }
                        });
                    }else {
                        Handler handlerThree=new Handler(Looper.getMainLooper());
                        handlerThree.post(new Runnable(){
                            public void run(){
                                showDialogNotCancel("错误代码", errorCode ,HomeActivity.this);
                            }
                        });
                    }
                }
            }
        }).start();

        return true;
    }
    //执行重新选座过程
    public void runRefresh(OkHttpClient client) throws IOException {
        //执行登录请求
        handlerProgress(9);
        Request request1 = new Request.Builder()
                .url("http://seat.ysu.edu.cn/ClientWeb/pro/ajax/login.aspx?act=login&id=" + str1 + "&pwd=" + str2 + "&role=512&aliuserid=&schoolcode=&wxuserid=&_nocache=1551511783772")
                .build();
//        http://seat.ysu.edu.cn/ClientWeb/pro/ajax/login.aspx?act=login&id=150104100017&pwd=131216&role=512&aliuserid=&schoolcode=&wxuserid=&_nocache=1551511783772
        //获取座位预约座位状态
        client.newCall(request1).execute();
        Request request2 = new Request.Builder()
                .url("http://seat.ysu.edu.cn/ClientWeb/pro/ajax/reserve.aspx?stat_flag=9&act=get_my_resv&_nocache=1551577219949")
                .build();
        Response response = client.newCall(request2).execute();
        responseData = response.body().string();           //得到第二次访问返回的数据
        Log.e("YSU座位状态信息",responseData);

        //如果返回数据中data不为空，则表示已经有预约座位；从中解析出已有座位的数据
        if(!CommonFunction.getMatcher("data\":(.*),\"ext",responseData).equals("[]")){
            String resv_id = CommonFunction.getMatcher("id\":\"(\\d+)\",\"group",responseData);
            Log.e(Tag,resv_id);
            String resv_devid = CommonFunction.getMatcher("devId\":\"(\\d+)\",\"devName",responseData);
            Log.e(Tag,resv_devid);
            Request request3 = new Request.Builder()
                    .url("http://seat.ysu.edu.cn/ClientWeb/pro/ajax/reserve.aspx?act=resv_leave&type=2&resv_id="+resv_id+"&_nocache=1551683616407")
                    .build();
            client.newCall(request3).execute();//取消现有预约

            Request request4 = new Request.Builder()
                    .url("http://seat.ysu.edu.cn/ClientWeb/pro/ajax/reserve.aspx?act=del_resv&id="+resv_id+"&_nocache=1551683616407")
                    .build();
            client.newCall(request4).execute();

            handlerProgress(5);
            Request request5 = new Request.Builder()
                    .url("http://seat.ysu.edu.cn/ClientWeb/pro/ajax/reserve.aspx?dialogid=&dev_id="+resv_devid+"&lab_id=&kind_id=&room_id=&type=dev&prop=&test_id=" +
                            "&term=&test_name=&start="+start+"&end="+end+"&start_time="+start_time+"&end_time="+end_time+"&up_file=&memo=&act=set_resv&_=1544339248168")
                    .build();
            Response response1 = client.newCall(request5).execute();
            String responseData1 = response1.body().string();
            Log.e("responseData1",responseData1);

            ret = CommonFunction.parseSingleLabel(responseData1,"ret");
            act = CommonFunction.parseSingleLabel(responseData1,"act");
            msg = CommonFunction.parseSingleLabel(responseData1,"msg");

            if (act.equals("set_resv") & msg.equals("操作成功！") & ret.equals("1")){
                handlerProgress(0);
//                            vibrator.vibrate(2000);
                showNotification("预约成功","预约成功,开始时间"+resvTime,HomeActivity.this,6,"channel6","重新预约");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastCustom.passValue(4000,1000,3,0,100);
                        ToastCustom.getInstance(getApplicationContext()).show("预约成功,开始时间" + resvTime, 4000);
                    }
                });
            }else {
                handlerProgress(0);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        CommonFunction.showDialog("预约小提示",msg,HomeActivity.this);
                    }
                });
            }
        }
    }


    //handle传递消息
    public void handlerProgress( int id){
        Message message = new Message();
        switch (id){
            case 0:
                message.what = CLOSEPROGRESS;
                break;
            case 10:
                message.what = OPENPROGRESS;
                Log.e("进入","ok");
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
                message.what = CHECKCOUNTONE;
                break;
            case 8:
                message.what = CHECKCOUTTWO;
                break;
            case 9:
                message.what = CANCELRESEV;
                break;
            case 11:
                message.what = TRANSFER;
                break;
            case 12:
                message.what = CHECKTIME;
                break;
            case 13:
                message.what = GETORISEAT;
                break;
            default:
                break;
        }
        handler.sendMessage(message);
    }

    //Handl处理消息
    @SuppressLint("HandlerLeak")
    private  Handler handler = new Handler(){
        @Override
        public void  handleMessage(Message msg){
            switch (msg.what) {
                case CLOSEPROGRESS:
                    //关闭视图0
                    relativeLayout.setVisibility(View.GONE);
                    break;
                case  OPENPROGRESS:
                    //打开视图10
                    relativeLayout.setVisibility(View.VISIBLE);
                    relativeLayout.bringToFront();
                    break;
                case SEATERROR:
                    //1
                    progressTextView.setText("座位号异常");
                    break;
                case RESPONSEREQUEST:
                    //2
                    Log.e("处理","ok");
                    progressTextView.setText("正在响应您的请求");
                    break;
                case GETCREDIT:
                    //3
                    progressTextView.setText("获取积分信息");
                    break;
                case ACCESSDATA:
                    //4
                    progressTextView.setText("获取座位数据");
                    break;
                case RESERVE:
                    //5
                    progressTextView.setText("正在预约，请稍后");
                    break;
                case CREDITINFO:
                    //6
                    progressTextView.setText("积分满足条件");
                    break;
                case CHECKCOUNTONE:
                    //7
                    progressTextView.setText("核对账户一");
                    break;
                case CHECKCOUTTWO:
                    //8
                    progressTextView.setText("核对账户二");
                    break;
                case CANCELRESEV:
                    //9
                    progressTextView.setText("取消当前预约");
                    break;
                case TRANSFER:
                    //11
                    progressTextView.setText("正在转接，账户二正在预约");
                    break;
                case CHECKTIME:
                    //12
                    progressTextView.setText("正在核对时间信息");
                    break;
                case GETORISEAT:
                    progressTextView.setText("正在获取原始座位信息");
                    break;

            }
            super.handleMessage(msg);
        }
    };
/*----------------------------------------------------------------------------------------------------------------------------------------------------------------*/
/*----------------------------------------------------------------------------------------------------------------------------------------------------------------*/
    //整型时间换成字符串，在需要补零的地方补零  赋值给str3
    public void intToString(int hourStart, int minuteStart){
        if (hourStart < 7){
            hourStart = 7;
            minuteStart = 40;
            str3= "0" + hourStart + ":" + minuteStart;
            Toast.makeText(HomeActivity.this,"时间设定不得小于7:00，已自动为您更改",Toast.LENGTH_SHORT).show();
        }else if (hourStart < 10){//hour<10
            if (minuteStart < 10){//minute & hour < 10
                str3 = "0" + hourStart + ":" + "0" + minuteStart;
            }else {//hour < 10  minute > 10
                str3 = "0" + hourStart + ":" + minuteStart;
            }
        }else {//hour > 10
            if (minuteStart < 10){//hour > 10  minute < 10
                str3 = hourStart + ":" + "0" + minuteStart;
            }else {//hour > 10  minute > 10
                str3 = hourStart + ":" + minuteStart;
            }
        }
    }
    //核验过程
    public void checkAllProgress(OkHttpClient client) throws IOException, JSONException {
        myResv = getMyReserve(str1,str2,client,HomeActivity.this);
        if (myResv[1].equals("") & myResv[4].equals("")){
            return;//如果之前没有直接结束，并提示
        }else {//如果有座位则重新预约
            //1.    检查设定的时间是否合乎逻辑
            checkTime();
            //2.    根据预约时长的限制，判断设定的时间是否可以预约   "timeDesc":"06/22 15:00-22:30"
            prefEndTime = myResv[4].substring(12,17);
            ph = Integer.parseInt(CommonFunction.getMatcher("(.*):",prefEndTime));
            pm = Integer.parseInt(CommonFunction.getMatcher(":(.*)",prefEndTime));
            //3.    计算当前时间到原始预约结束时间是否大于1小时
            isReserve(client);
        }
    }
    //核验选择的时间是否合乎逻辑
    public void checkTime(){
        //获取系统时间  2019-06-01 08:00
        nowTime = CommonFunction.systemTime();
        //str3 =  "08:00"   start_time = 0800
        //判断输入的时间是否符合要求，是否在当前时间之后
        now = CommonFunction.getMatcher("(.*):", nowTime.substring(11,16))+CommonFunction.getMatcher(":(.*)",nowTime.substring(11,16));
        nowHour = now.substring(0,2);
        nowMinute = now.substring(2,4);
        resvHour = start_time.substring(0,2);
        resvMinute = start_time.substring(2,4);

        //时间是否符合要求
        if (Integer.parseInt(resvHour)>=Integer.parseInt(nowHour) & Integer.parseInt(resvHour)< 24){//小时是否符合要求
            if (Integer.parseInt(resvMinute)>=Integer.parseInt(nowMinute) & Integer.parseInt(resvMinute)<60){//分钟是否符合要求
                //小时和时间均符合要求
                start = CommonFunction.getMatcher("(.*) ",nowTime)+"+"+CommonFunction.getMatcher("(.*):",str3)+"%3A"+CommonFunction.getMatcher(":(.*)",str3);
                start_time = CommonFunction.getMatcher("(.*):",str3)+CommonFunction.getMatcher(":(.*)",str3);
                resvTime = str3;
            }else{
                //分钟小于当前分钟，判断用户设定的小时是否大于当前小时
                if (Integer.parseInt(resvHour)==Integer.parseInt(nowHour)){
                    //用户输入的时间中小时为当前小时，分钟小于当前分钟 则 从当前时间延后20分钟开始
                    start = CommonFunction.getMatcher("(.*) ",nowTime)+"+"+ resvHour +"%3A"+ (Integer.parseInt(nowMinute)+20);
                    start_time = nowHour + (Integer.parseInt(nowMinute) + 20);
                    resvTime = nowHour + ":" + (Integer.parseInt(nowMinute) + 20);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(HomeActivity.this,"分钟错误，预约时间设定为20分钟之后！",Toast.LENGTH_SHORT).show();
                        }
                    });
                }else{
                    //用户输入时间中小时大于当前小时，则不改变分钟
                    start = CommonFunction.getMatcher("(.*) ",nowTime)+"+"+CommonFunction.getMatcher("(.*):",str3)+"%3A"+CommonFunction.getMatcher(":(.*)",str3);
                    start_time = CommonFunction.getMatcher("(.*):",str3)+CommonFunction.getMatcher(":(.*)",str3);
                    resvTime = str3;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(HomeActivity.this,"预约时间："+ resvTime,Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }else {//输入的信息完全不符合要求
            start = CommonFunction.getMatcher("(.*) ",nowTime)+"+"+ nowHour +"%3A"+ (Integer.parseInt(nowMinute)+20);
            start_time = nowHour + (Integer.parseInt(nowMinute) + 20);
            resvTime = nowHour + ":" + (Integer.parseInt(nowMinute) + 20);
            Toast.makeText(HomeActivity.this,"时间错误，预约时间设定为20分钟之后！",Toast.LENGTH_SHORT).show();
        }
    }
    //该时间是否可预约
    public void isReserve(OkHttpClient client) throws IOException, JSONException {
        if (Integer.parseInt(resvHour)>=22 | (Integer.parseInt(resvHour)==21 & Integer.parseInt(resvMinute)>30)){//此处表明所选时间在21:30之后，受1小时限制，不能预约
            Toast.makeText(HomeActivity.this,"预约时间不能少于1小时,",Toast.LENGTH_SHORT).show();
        }else if ( ((ph-Integer.parseInt(resvHour)) * 60 + pm - Integer.parseInt(resvMinute)) <= 60){
            //此处说明设定的时间不在21:30之后，不受预约时长的限制，应该判断设定时间到原来的结束时间是否小于1小时
            //如果时长小于1小时，计算当前座位，座位的剩余时间是否小于1小时
            if (( (22-ph) * 60 + 30 - pm ) <= 60){
               //剩余时长小于1小时，则直接延长到闭馆时间，计算是否够1个小时
                int diff = 60 - ( (ph - Integer.parseInt(resvHour) * 60) + pm - Integer.parseInt(resvMinute));
                if (( (22 - Integer.parseInt(resvHour)) * 60 + 30 - Integer.parseInt(resvMinute) ) >= 60 ){
                    //结束时间更改为22:30时满足预约条件，开始预约
                    start = CommonFunction.getMatcher("(.*) ",nowTime)+"+"+CommonFunction.getMatcher("(.*):",resvTime)+"%3A"+CommonFunction.getMatcher(":(.*)",resvTime);
                    start_time = CommonFunction.getMatcher("(.*):",resvTime)+CommonFunction.getMatcher(":(.*)",resvTime);
                    end = CommonFunction.getMatcher("(.*) ",nowTime)+"+22%3A30";//默认结束时间
                    end_time = "2230";

                    //选择的时间显示出来
                }else {//更改到闭关仍不满足条件，则只能先前调整

                }
            }else {
                //座位剩余时长超过1小时，检查该1小时内是否有人预约
                //      获取座位状态信息
                String[] res = parseSeatId(myResv[1].substring(0,2),myResv[1].substring(3,6));//四阅-209/order/address
                client = obtainAndParseStatus(client);// 获取并解析座位状态数据
                String dataArray =loadRsvSta("data");
                JSONArray jsonArray = new JSONArray(dataArray);//获取到的座位状态数据
                int[] storeState = new int[jsonArray.length()];//存储每个座位的状态 不可预约：1；可预约：0.

                for (int indey = 0; indey < jsonArray.length(); indey ++){//检索所有座位的状态并存储
                    String s = jsonArray.getString(indey);
                    String t = CommonFunction.parseSingleLabel(s,"ts");//座位状态ts="[]"为空闲
                    if (t.equals("[]")){//空闲
                        storeState[indey] = 1;
                    }else{
                        storeState[indey] = 0;
                    }
                }//状态解析完毕
                //      分析结果
                if (storeState[Integer.parseInt(res[1])] == 0){
                    //剩余时间没有人预约，则直接预约一定满足1小时条件
                    start = CommonFunction.getMatcher("(.*) ",nowTime)+"+"+CommonFunction.getMatcher("(.*):",resvTime)+"%3A"+CommonFunction.getMatcher(":(.*)",resvTime);
                    start_time = CommonFunction.getMatcher("(.*):",resvTime)+CommonFunction.getMatcher(":(.*)",resvTime);
                    end = CommonFunction.getMatcher("(.*) ",nowTime)+"+22%3A30";//默认结束时间
                    end_time = "2230";
//                    Button timeButton = findViewById(R.id.timeButton);
//                    timeButton.setText(resvTime);//选择的时间显示出来
                }else {
                    //剩余时间有人预约，不能向后延长更改，只能向前延长更改
                    //计算当前时间到结束时间
                    if ( ((ph - Integer.parseInt(nowHour)) *  60 + pm - Integer.parseInt(nowMinute)) >= 90 ){
                        //大于90则继续 余留15分钟让用户在一定时长内有机会释放座位，避免因为不能及时到馆而导致违约
                        resvMinute = String.valueOf(Integer.parseInt(resvMinute) + 15);
                        if (Integer.parseInt(resvMinute) >= 60){
                            resvHour = String.valueOf(Integer.parseInt(resvHour) + 1);
                        }
                        if (Integer.parseInt(resvMinute) < 10){
                            resvMinute = "0" + resvMinute;
                        }
                        if (Integer.parseInt(resvHour) < 10){
                            resvHour = "0" + resvHour;
                        }
                        resvTime = resvHour + resvMinute;
                        start = CommonFunction.getMatcher("(.*) ",nowTime)+"+"+CommonFunction.getMatcher("(.*):",resvTime)+"%3A"+CommonFunction.getMatcher(":(.*)",resvTime);
                        start_time = CommonFunction.getMatcher("(.*):",resvTime)+CommonFunction.getMatcher(":(.*)",resvTime);
                        end = CommonFunction.getMatcher("(.*) ",nowTime)+prefHour + "%3A" + prefEndTime;//默认结束时间
                        end_time = prefHour + prefEndTime;
                    }else {
                        String reservetime = df.format(new Date().getTime()+ 20 * 60 * 1000);// 当前时间20分钟后,该时间格式为2019-06-01 18:25
                        resvTime = reservetime.substring(11,16);//预约开始时间     例：18:25
                        start = CommonFunction.getMatcher("(.*) ",nowTime)+"+"+CommonFunction.getMatcher("(.*):",resvTime)+"%3A"+CommonFunction.getMatcher(":(.*)",resvTime);
                        start_time = CommonFunction.getMatcher("(.*):",resvTime)+CommonFunction.getMatcher(":(.*)",resvTime);
                        end = CommonFunction.getMatcher("(.*) ",nowTime)+prefHour + "%3A" + prefEndTime;//默认结束时间
                        end_time = prefHour + prefEndTime;
                        final OkHttpClient finalClient = client;
//                        Button timeButton = findViewById(R.id.timeButton);
//                        timeButton.setText(resvTime);//选择的时间显示出来
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                alertDialog("预约提示",prefEndTime + "以后已有人预约,当前时间到上次预约结束时间已不足90分钟，是否要继续重新预约，开始时间:" + resvTime,
                                        HomeActivity.this, finalClient);
                            }
                        });
                    }
                }

            }
        }else {//设定时间到之前的结束时间不小于1小时，则直接进行预约
            start = CommonFunction.getMatcher("(.*) ",nowTime)+"+"+CommonFunction.getMatcher("(.*):",resvTime)+"%3A"+CommonFunction.getMatcher(":(.*)",resvTime);
            start_time = CommonFunction.getMatcher("(.*):",resvTime)+CommonFunction.getMatcher(":(.*)",resvTime);
            end = CommonFunction.getMatcher("(.*) ",nowTime)+prefHour + "%3A" + prefEndTime;//默认结束时间
            end_time = prefHour + prefEndTime;
        }
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
        String reserve = df.format(new Date().getTime());// 当前时间加40分钟,该时间格式为2019-06-01 18:25
        String date = reserve.substring(0,10);//日期2019-06-01
        String msg_error = date + "您在【" + date.substring(0,4) + "年" + date.substring(5,7) +"月" + date.substring(8,10) + "日】已有预约，当日不能再预约";

        //2.    预约结果分析    除了以下1,2两种状态其他状态均为当前用户没有座位
        if (act_name.equals("set_resv") & act_msg.equals("操作成功！") & act_ret.equals("1")){//操作状态1：预约成功
            actok = 1;
            ToastText = "已预约成功!" + "开始时间:" + resvTime;
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
            ToastText = act_msg;
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
    //从服务器获取座位状态信息（包含解析与保存步骤）
    public OkHttpClient obtainAndParseStatus(OkHttpClient client) throws IOException {
        //1.    设定预约时间，如果用户没有指定时间，则从40分钟之后开始
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String reserve = df.format(new Date());// 当前时间加40分钟,该时间格式为2019-06-01 18:25
        String date = reserve.substring(0,10);//日期2019-06-01
        //2.    日期时间格式整合
        String fr_start = prefHour  + "%3A" + ( prefMinute + 1 );
        String fr_end = "22%3A30";

        //3.    获取系统时间
        /*SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String rsvTime = df.format(new Date());// 当前时间加40分钟,该时间格式为2019-06-01 18:25
        String date = rsvTime.substring(0,10);*/
        //http://202.206.242.87/ClientWeb/pro/ajax/device.aspx?byType=devcls&classkind=8&display=fp&md=d&room_id=100457213
        // &purpose=&selectOpenAty=&cld_name=default&date=2019-06-21&fr_start=14%3A00&fr_end=21%3A00&act=get_rsv_sta&_=1561082040042
        String roomIdUrl = urlCommonFirstPara.concat(str1).concat(urlCommonSecondPara)
                .concat(date).concat("&fr_start=").concat(fr_start).concat("&fr_end=").concat(fr_end)
                .concat(urlCommonThirdPara).concat(str2);//目标阅览室url
        // 访问服务器座位状态信息
        Request getRsvSta = new Request.Builder().url(roomIdUrl).build();
        Response rsvStaRsponse = client.newCall(getRsvSta).execute();
        String rsvStateData = rsvStaRsponse.body().string();
        //从服务器返回的数据中解析出座位状态信息，并保存到data
        parseJSONFromResponse(rsvStateData);
        Log.e("YSU从服务器获取并保存座位状态信息","成功");
        return client;
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
        }
        return result;
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

/*----------------------------------------------------------------------------------------------------------------------------------------------------------------*/
/*----------------------------------------------------------------------------------------------------------------------------------------------------------------*/
    //通知栏提示
    /* 样式3： 悬停、振动、跳转   回状态栏    */
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
    public void showNotification3(String title, String text, Context context, int id ,int length,String channelid, String channelname){
        final NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(channelid, channelname, NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(mChannel);
            notification = new NotificationCompat.Builder(context)
                    .setChannelId(channelid)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.timingtasknotigy)
                    .setAutoCancel(true)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.seatlock))
                    .setContentIntent(PendingIntent.getActivities(context,0x0001,
                            new Intent[]{new Intent(context,HomeActivity.class)},PendingIntent.FLAG_CANCEL_CURRENT))//跳转
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
                            new Intent[]{new Intent(context,HomeActivity.class)},PendingIntent.FLAG_CANCEL_CURRENT))//跳转
                    .build();
        }
        String tag = id + "float";
        manager.notify(tag,id,notification);

        Notification notification2 = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel mChannel = new NotificationChannel(channelid, channelname, NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(mChannel);
            notification2 = new NotificationCompat.Builder(context)
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
                    .setContentIntent(PendingIntent.getActivities(context,0x0001,
                            new Intent[]{new Intent(context,HomeActivity.class)},PendingIntent.FLAG_CANCEL_CURRENT))//跳转
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,HomeActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),true)
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
                    .setContentIntent(PendingIntent.getActivities(context,0x0001,
                            new Intent[]{new Intent(context,HomeActivity.class)},PendingIntent.FLAG_CANCEL_CURRENT))//跳转
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,HomeActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),true)
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
    //后台持续监控Dialog
    public void alertDialog(String title, String msg, Context context, final OkHttpClient client){
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(context,R.style.dialog_style);
        builder.setIcon(R.drawable.applygreen);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    runRefresh(client);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        CommonFunction.showDialog("预约小提示","您可取消当前座位，前往快速抢座页面",HomeActivity.this);
                    }
                });
            }
        });
//        android.support.v7.app.AlertDialog dialog= builder.create();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            dialog.getWindow().setType((WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY));
//        }else {
//            dialog.getWindow().setType((WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));
//        }
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
    private void parseJSONWithJSONObject(String jsonData){
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            String ret = jsonObject.getString("ret");
            String act = jsonObject.getString("act");
            String msg = jsonObject.getString("msg");
//            String data = jsonObject.getString("data");
            String ext = jsonObject.getString("ext");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
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
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showDialog("预约小提示","没有已生效或即将生效的预约",context);
                }
            });
        }else{//座位信息
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
        return myResv;
    }
}
