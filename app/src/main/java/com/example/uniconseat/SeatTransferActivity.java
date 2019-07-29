package com.example.uniconseat;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SeatTransferActivity extends AppCompatActivity {
    private String source_id = "",source_pwd="",target_id ="",target_pwd ="";
    private EditText sourceIdEdit,sourcePwdEdit,targetIdEdit,targetPwdEdit;
    private Button transferButton;
    private int actok;
    private String ToastText,resvTime;
    private String[] myResv = new String[6];
    private String prefStartTime,prefStartHour,prefStartMinute,prefEndTime,prefEndHour,prefEndMinute;
    private int psh,psm,peh,pem;
    private String currenTime,currentHour,currentMinute;
    private int ch,cm;
    private String start,end,start_time,end_time;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seat_transfer);

        //进度条
        relativeLayout = findViewById(R.id.progressBar);
        progressTextView = findViewById(R.id.progresstips);
        progressBar = findViewById(R.id.progress);

        sourceIdEdit = findViewById(R.id.edit_source_id);
        sourcePwdEdit = findViewById(R.id.edit_source_pwd);
        targetIdEdit = findViewById(R.id.edit_target_id);
        targetPwdEdit = findViewById(R.id.edit_target_pwd);
        transferButton = findViewById(R.id.transferButton);

        SharedPreferences pref = getSharedPreferences("transferSeat",MODE_PRIVATE);
        source_id = pref.getString("sourceid","");
        source_pwd = pref.getString("sourcepwd","");
        target_id = pref.getString("targetid","");
        target_pwd = pref.getString("targetpwd","");

        sourceIdEdit.setText(source_id);
        sourcePwdEdit.setText(source_pwd);
        targetIdEdit.setText(target_id);
        targetPwdEdit.setText(target_pwd);

        transferButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkInfo();
            }
        });

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
    private void checkInfo(){
        source_id = sourceIdEdit.getText().toString();
        source_pwd = sourcePwdEdit.getText().toString();
        target_id = targetIdEdit.getText().toString();
        target_pwd = targetPwdEdit.getText().toString();

        // 保存用户数据
        SharedPreferences.Editor userInfopref = getSharedPreferences("transferSeat",MODE_PRIVATE).edit();
        userInfopref.putString("sourceid",source_id);
        userInfopref.putString("sourcepwd",source_pwd);
        userInfopref.putString("targetid",target_id);
        userInfopref.putString("targetpwd",target_pwd);
        userInfopref.apply();

        if (!source_id.equals("") & !source_pwd.equals("") & !target_id.equals("") & !target_pwd.equals("")){
            transfer();
        }else {
            ToastCustom.passValue(2000,1000,2,0,100);
            ToastCustom.getInstance(getApplicationContext()).show("请确保以上信息的完整性", 2000);
        }
    }
    private void transfer(){
        final HashMap<String, List<Cookie>> cookieStoreh = new HashMap<>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
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
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(getApplicationContext(),"正在响应您的请求，请稍后",Toast.LENGTH_SHORT).show();
//                        }
//                    });
                    handlerProgress(10);
                    handlerProgress(2);
                    actok = 0; ToastText = "";
                   handlerProgress(7);
                    //1. 账户一登录
                    String loginUrl = new StringBuilder().append("http://seat.ysu.edu.cn/ClientWeb/pro/ajax/login.aspx?act=login&id=").append(source_id).append("&pwd=")
                            .append(source_pwd).append("&role=512&aliuserid=&schoolcode=&wxuserid=&_nocache=1551511783772")
                            .toString();
                    Request loginRequest = new Request.Builder().url(loginUrl).build();//向服务器发送登录请求，包括ID, password
                    Response loginResponse = client.newCall(loginRequest).execute();//执行登录请求
                    String loginReturn = loginResponse.body().string();//得到响应数据

                    //2. 账户一登录结果分析
                    parseResponseMsg(loginReturn);//登录结果只有两种 登录成功 和 登录失败

                    if (actok != 7 ){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showDialog("操作异常","账户一"+ToastText,SeatTransferActivity.this);
                            }
                        });
                        return;
                    }
                   handlerProgress(13);
                    myResv = getMyReserve(source_id,source_pwd,client,getApplicationContext());
                    if (myResv[1].equals("no") & myResv[4].equals("no")){
                       handlerProgress(10);
                        return;//如果之前没有，直接结束，并提示
                    }else {
                        prefStartTime = myResv[4].substring(6,11);
                        prefEndTime = myResv[4].substring(12,17);

                        prefEndHour = CommonFunction.getMatcher("(.*):",prefEndTime);
                        prefEndMinute = CommonFunction.getMatcher(":(.*)",prefEndTime);
                        peh = Integer.parseInt(prefEndHour);
                        pem = Integer.parseInt(prefEndMinute);

                        prefStartHour = CommonFunction.getMatcher("(.*):",prefStartTime);
                        prefStartMinute = CommonFunction.getMatcher(":(.*)",prefStartTime);
                        psh = Integer.parseInt(prefStartHour);
                        psm = Integer.parseInt(prefStartMinute);
                    }

                    //3.账户二登录
                   handlerProgress(8);
                    String loginUrl2 = new StringBuilder().append("http://seat.ysu.edu.cn/ClientWeb/pro/ajax/login.aspx?act=login&id=").append(target_id).append("&pwd=")
                            .append(target_pwd).append("&role=512&aliuserid=&schoolcode=&wxuserid=&_nocache=1551511783772")
                            .toString();
                    Request loginRequest2 = new Request.Builder().url(loginUrl2).build();//向服务器发送登录请求，包括ID, password
                    Response loginResponse2 = client.newCall(loginRequest2).execute();//执行登录请求
                    String loginReturn2 = loginResponse2.body().string();//得到响应数据
                    //4.账户二登录结果分析
                    parseResponseMsg(loginReturn2);//登录结果只有两种 登录成功 和 登录失败
                    if (actok != 7 ){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showDialog("操作异常","账户二"+ToastText,SeatTransferActivity.this);
                            }
                        });
                        return;
                    }

                    //5. 判断预约是否开始，时间是否合理
                    handlerProgress(12);
                    String nowTime = CommonFunction.systemTime();
                    currenTime = nowTime.substring(11,16);
                    currentHour = CommonFunction.getMatcher("(.*):",currenTime);
                    currentMinute = CommonFunction.getMatcher(":(.*)",currenTime);
                    ch = Integer.parseInt(currentHour);
                    cm = Integer.parseInt(currentMinute);
                    if ( ( (ch - psh)*60 + cm - psm ) < 0 ){//距离预约开始时间还有10分钟
                        start = CommonFunction.getMatcher("(.*) ",nowTime) + "+" + prefStartHour +"%3A"+ prefStartMinute;
                        start_time = prefStartHour + prefStartMinute;
                        end = CommonFunction.getMatcher("(.*) ",nowTime) + "+" + prefEndHour + "%3A" + prefEndMinute;//原始结束时间
                        end_time = prefEndHour + prefEndMinute;
                        resvTime = prefStartHour + ":" + prefStartMinute;
                    }else if ( ( (ch - peh)*60 + cm - pem ) > 70 ){
                        //已经开始，距离结束时间大于1小时
                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        nowTime = df.format(new Date().getTime()+ 10 * 60 * 1000);
                        currenTime = nowTime.substring(11,16);
                        currentHour = CommonFunction.getMatcher("(.*):",currenTime);
                        currentMinute = CommonFunction.getMatcher(":(.*)",currenTime);
                        ch = Integer.parseInt(currentHour);
                        cm = Integer.parseInt(currentMinute);
                        //10分钟后开始
                        start = CommonFunction.getMatcher("(.*) ",nowTime) + "+" + currentHour +"%3A"+ currentMinute;
                        start_time = currentHour + currentMinute;
                        end = CommonFunction.getMatcher("(.*) ",nowTime) + "+" + prefEndHour + "%3A" + prefEndMinute;//原始结束时间
                        end_time = prefEndHour + prefEndMinute;
                        resvTime = currentHour + ":" + currentMinute;
                    }else {
                        handlerProgress(0);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showDialog("操作异常","账户一的剩余可使用时间不能少于70分钟",SeatTransferActivity.this);
                            }
                        });
                        return;
                    }

                    //6. 开始取消当前预约
                    handlerProgress(9);
                    loginRequest = new Request.Builder().url(loginUrl).build();//向服务器发送登录请求，包括ID, password
                    loginResponse = client.newCall(loginRequest).execute();//执行登录请求

                    Request request3 = new Request.Builder()
                            .url("http://seat.ysu.edu.cn/ClientWeb/pro/ajax/reserve.aspx?act=resv_leave&type=2&resv_id="+myResv[5]+"&_nocache=1551683616407")
                            .build();
                    client.newCall(request3).execute();//取消现有预约

                    Request request4 = new Request.Builder()
                            .url("http://seat.ysu.edu.cn/ClientWeb/pro/ajax/reserve.aspx?act=del_resv&id="+myResv[5]+"&_nocache=1551683616407")
                            .build();
                    client.newCall(request4).execute();

                    //7. 账户二登录并接收
                   handlerProgress(11);
                    loginRequest2 = new Request.Builder().url(loginUrl2).build();//向服务器发送登录请求，包括ID, password
                    loginResponse2 = client.newCall(loginRequest2).execute();//执行登录请求

                    Request request5 = new Request.Builder()
                            .url("http://seat.ysu.edu.cn/ClientWeb/pro/ajax/reserve.aspx?dialogid=&dev_id="+myResv[2]+"&lab_id=&kind_id=&room_id=&type=dev&prop=&test_id=" +
                                    "&term=&test_name=&start="+start+"&end="+end+"&start_time="+start_time+"&end_time="+end_time+"&up_file=&memo=&act=set_resv&_=1544339248168")
                            .build();
                    Response response1 = client.newCall(request5).execute();
                    String responseData1 = response1.body().string();
                    Log.e("responseData1",responseData1);

                    parseResponseMsg(responseData1);
                    tips();

                }catch (final Exception e){
                    //访问异常，无法连接服务器！
                    handlerProgress( 0);
                    if (cookieStoreh.size()==0){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ToastCustom.passValue(2000,1000,2,0,100);
                                ToastCustom.getInstance(getApplicationContext()).show("访问异常，无法连接服务器！", 2000);
                            }
                        });
                    }else {
                        Handler handlerThree=new Handler(Looper.getMainLooper());
                        handlerThree.post(new Runnable(){
                            public void run(){
                                showDialogNotCancel("错误代码", e.toString() ,getApplicationContext());
                            }
                        });
                    }
                }
            }
        }).start();
    }
    public String[] getMyReserve(String myid, String mypwd, OkHttpClient client, final Context context) throws IOException, JSONException {//必须在子线程中运行，同意cookie访问
        //1. 登录并获取我的预约信息
        String[] myResv = new String[]{"no","no","no","no","no","no"};//owner devName devId labName timeDesc
        String owner = ""; String devName = ""; String devId = ""; String labName = ""; String timeDesc = ""; String id = "";
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
            id = jsonObjectSingle.getString("id");
            myResv[0] = owner; myResv[1] = devName; myResv[2] = devId; myResv[3] = labName; myResv[4] = timeDesc;myResv[5] = id;
        }
        return myResv;
    }
    //处理预约结果，并提示
    public void tips(){
        handlerProgress(0);
        //在操作状态1/2/3处应该应用此函数，用于构建提示信息的具体内容，否则ToaskText=null
        switch (actok){
            case 1://预约成功
                //该信息重要等级为 HIGH，用户必须与之交互
                showNotification4("座位锁定",ToastText,getApplicationContext(),1,"channnel1","座位锁定");
                handlerProgress(0);
                break;
            case 2://已有预约
                //该信息重要等级为 LOW，点击空白处可取消Dialog
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showDialog("预约小提示",ToastText,getApplicationContext());
                    }
                });
                handlerProgress(0);
                break;
            case 3://系统未开放
                //该信息重要等级为 LOW，点击空白处可取消Dialog
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showDialog("预约小提示","请在6:30之后再进行预约; 预约端当前时间",getApplicationContext());
                    }
                });
                handlerProgress(0);
                break;
            case 4://积分不足
                //该信息重要等级为 HIGH，用户必须与之交互
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showDialogNotCancel("预约提示",ToastText ,getApplicationContext());
                    }
                });
                break;
            case 5://预约冲突
                //该信息重要等级为 LOW，点击空白处可取消Dialog
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showDialog("预约小提示","与现有预约存在冲突",getApplicationContext());
                    }
                });
                handlerProgress(0);
                break;
            case 6://预约时间少于1小时
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showDialog("预约小提示",ToastText,getApplicationContext());
                    }
                });
                handlerProgress(0);
                break;
            case 7://登录成功
                break;
            case 8://预约时间大于15小时
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showDialog("预约小提示",ToastText,getApplicationContext());
                    }
                });
                handlerProgress(0);
                break;
            case 9://其他信息
                if (CommonFunction.regexMatcher("(参数有误)",ToastText)){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),"座位号不存在",Toast.LENGTH_SHORT).show();
                        }
                    });
                }else if (CommonFunction.regexMatcher("(未登录)",ToastText)){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),"用户名/密码错误",Toast.LENGTH_SHORT).show();
                        }
                    });
                }else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showDialog("预约提示",ToastText,getApplicationContext());
                        }
                    });
                }
                handlerProgress(0);
                break;
            default:
                break;
        }
        actok = 0; //ToastText = "";
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
            ToastText = "账户二" + "已预约成功!" + myResv[1] + "开始时间:" + resvTime;
        }else if (act_name.equals("set_resv") & act_msg.equals("已有预约")){//操作状态2：您已有预约，当日不能再预约
            actok = 2;
            ToastText = act_msg;
        }else if (CommonFunction.regexMatcher("(请在6:30之后再进行预约)",act_msg)){//操作状态3：系统未开放
            actok = 3;
            ToastText = act_msg;
        }else if (CommonFunction.regexMatcher("(积分不足)",act_msg)){//操作状态4：积分不足，禁止预约
            actok = 4;
            ToastText = act_msg;
        }else if (CommonFunction.regexMatcher("(冲突)",act_msg)){//操作状态5：与现有的预约存在冲突
            actok = 5;
            ToastText = act_msg;
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
                            new Intent[]{new Intent(context,SeatTransferActivity.class)},
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
                            new Intent[]{new Intent(context,SeatTransferActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)//悬挂跳转
                    .build();
        }
        manager.notify(id,notification);
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
                case 13:
                    progressTextView.setText("正在获取原始座位信息");
                    break;

            }
            super.handleMessage(msg);
        }
    };
}
