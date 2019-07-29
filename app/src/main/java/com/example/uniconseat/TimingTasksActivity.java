package com.example.uniconseat;

import android.annotation.SuppressLint;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.mcsoft.timerangepickerdialog.RangeTimePickerDialog;

import java.text.SimpleDateFormat;
import java.util.Random;

public class TimingTasksActivity extends AppCompatActivity implements RangeTimePickerDialog.ISelectedTime{
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//定义日日期的格式
    public String[] user1 = new String[9];//保存用户信息
    public String[] user2 = new String[9];
    public String user1Name,user1Id,user1Password,user1Room,user1RoomId,user1PrefSeat,user1StartTime,user1EndTime;//用户信息
    public String candidate1,candidate2,candidate3,candidate4;//候选座位
    public String user2Name,user2Id,user2Password,user2Room,user2RoomId,user2PrefSeat,user2StartTime,user2EndTime;
    public String userRoom,userRoomId,userStartTime,userEndTime,prefSeat,roomId;//双用户共用信息
    public boolean user1s = false; boolean user1e = false; boolean user1Accept = false;
    public boolean user2s = false; boolean user2e = false; boolean user2Accept = false;
    public boolean TipsJumpServiceStart;
    public String[] candidateArray = new String[4];
    private static final int SEATERROR = 1;//座位号异常


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timing_tasks);

        //如果桥接服务设定为启动，则启动
        SharedPreferences pref = getSharedPreferences("TipsJumpsService",MODE_PRIVATE);
        TipsJumpServiceStart = pref.getBoolean("TipsJumpsService",false);
        Log.e("TimpsJumpServiceStart",String.valueOf(TipsJumpService.TimpsJumpServiceStart));

        final SwitchCompat longRunTips = findViewById(R.id.longRunTips);
        if (readUser1Info()){
            //设定开关设置
            longRunTips.setChecked(TipsJumpService.TimpsJumpServiceStart);
        }

        //启动公告滚动条
        AutoScrollTextView autoScrollTextView = (AutoScrollTextView)findViewById(R.id.tipsend);
        autoScrollTextView.init(getWindowManager());
        autoScrollTextView.startScroll();

        //监听switch开关
        longRunTips.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (readUser1Info()){
                    if (isChecked){
                        Log.e("TimpsJumpServiceStart",String.valueOf(TipsJumpService.TimpsJumpServiceStart));
                        if (!TipsJumpService.TimpsJumpServiceStart){
                            Intent intentTipsJump = new Intent(TimingTasksActivity.this,TipsJumpService.class);
                            startService(intentTipsJump);
                            SharedPreferences.Editor timingPref = getSharedPreferences("TipsJumpsService",MODE_PRIVATE).edit();
                            timingPref.putBoolean("TipsJumpsService",true);
                            timingPref.apply();
                        }
                        ToastCustom.passValue(3000,1000,2,0,100);
                        ToastCustom.getInstance(TimingTasksActivity.this).show("您已开启定时任务服务", 3000);

                    }else {
                        Log.e("TimpsJumpServiceStart",String.valueOf(TipsJumpService.TimpsJumpServiceStart));
                        if (TipsJumpService.TimpsJumpServiceStart){
                            Intent intentTipsJump = new Intent(TimingTasksActivity.this,TipsJumpService.class);
                            stopService(intentTipsJump);
                            //取消闹钟
                            if (TipsJumpService.alarmManager != null){
                                TipsJumpService.alarmManager.cancel(TipsJumpService.pi);
                            }
                            SharedPreferences.Editor timingPref = getSharedPreferences("TipsJumpsService",MODE_PRIVATE).edit();
                            timingPref.putBoolean("TipsJumpsService",false);
                            timingPref.apply();
                        }
                        ToastCustom.passValue(3000,1000,2,0,100);
                        ToastCustom.getInstance(TimingTasksActivity.this).show("您已关闭定时任务服务", 3000);
                    }
                }else {
                    if (isChecked){
                        longRunTips.setChecked(false);
                    }
                    Toast.makeText(TimingTasksActivity.this,"请输入完整信息",Toast.LENGTH_SHORT).show();
                }

            }
        });

        //悬浮按钮点击事件
        FloatingActionButton fab = findViewById(R.id.comfab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, R.string.timing_float, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        Spinner user1spinner = findViewById(R.id.user1roomidspinner);
        spinnerRoomId(user1spinner,"user1Info");
        //Spinner user2spinner = findViewById(R.id.user2roomidspinner);
        //spinnerRoomId(user2spinner,"user2Info");

        printUserInfo("user1Info");
        //printUserInfo("user2Info");

        user1Monitor();
        //user2Monitor();
        user1Save();
        //user2Save();
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

    //监听用户1的保存按钮
    public void user1Save(){
        Button user1save = findViewById(R.id.saveuser1_button);
        user1save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                user1s = false;
                user1e = false;
                if (getUser1InfoFromScreen()){
                    Toast.makeText(TimingTasksActivity.this,"保存成功",Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
    //监听用户2的保存按钮
    public void user2Save(){
        Button user2save = findViewById(R.id.saveuser2_button);
        user2save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                user2s = false;
                user2e = false;
                if (getUser2InfoFromScreen()){
                    Toast.makeText(TimingTasksActivity.this,"保存成功",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    //监听用户1的时间选择
    public void user1Monitor(){
        //两个按钮只能点击一个
        Button user1stime = findViewById(R.id.user1starttime);
        Button user1etime = findViewById(R.id.user1endtime);

        user1stime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (user1e){

                }else {
                    showCustomDialogTimePicker();
                    user1s = true;
                    user1Accept = true;
                }
            }

        });
        user1etime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (user1s){

                }else {
                    showCustomDialogTimePicker();
                    user1e = true;
                    user1Accept = true;
                }
            }
        });
    }
    //监听用户2的时间选择
    public void user2Monitor(){
        //两个按钮只能点击一个
        Button user2stime = findViewById(R.id.user2starttime);
        Button user2etime = findViewById(R.id.user2endtime);
        user2stime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (user2e){
                }else {
                    showCustomDialogTimePicker();
                    user2s = true;
                    user2Accept = true;
                }
            }

        });
        user2etime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (user2s){
                }else {
                    showCustomDialogTimePicker();
                    user2e = true;
                    user2Accept = true;
                }
            }
        });
    }
    //监听阅览室选择Spinner
    public void spinnerRoomId(Spinner spinner, final String filename){
        int index = 4;
        SharedPreferences spinnerDefaultRoomIdpref = getSharedPreferences(filename,MODE_PRIVATE);
        userRoomId = spinnerDefaultRoomIdpref.getString("roomid","101439231");//101439231是第四阅览室id
        //  将roomid转化为中文，显示在对应的输入框中
        switch (userRoomId){
            case "100457211":
                userRoom = "一阅";
                index = 1;
                break;
            case "100457213":
                userRoom = "二阅";
                index = 2;
                break;
            case "101439229":
                userRoom = "三阅";
                index = 3;
                break;
            case "101439231":
                userRoom = "四阅";
                index = 4;
                break;
            case "101439233":
                userRoom = "五阅";
                index = 5;
                break;
            case "100457221":
                userRoom = "树华A";
                index = 6;
                break;
                default:break;
        }
        spinner.setSelection(index);//赋予历史值
        //监听spinner
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] room = getResources().getStringArray(R.array.roomname);
                userRoom = room[position];
                switch (userRoom){
                    case "一阅":
                        userRoomId = "100457211";
                        break;
                    case "二阅":
                        userRoomId = "100457213";
                        break;
                    case "三阅":
                        userRoomId = "101439229";
                        break;
                    case "四阅":
                        userRoomId = "101439231";
                        break;
                    case "五阅":
                        userRoomId = "101439233";
                        break;
                    case "树华A":
                        userRoomId = "100457221";
                        break;
                    default:
                        Toast.makeText(TimingTasksActivity.this,"重新选择阅览室",Toast.LENGTH_SHORT).show();
                        break;
                }
                switch (filename){
                    case "user1Info":
                        user1Room = userRoom; user1RoomId = userRoomId;
                        break;
                    case "user2Info":
                        user2Room = userRoom; user2RoomId = userRoomId;
                        break;
                    default:
                        break;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //当所有选项都没有被选择时触发
            }
        });
        switch (filename){
            case "user1Info":
                user1Room = userRoom; user1RoomId = userRoomId;
                break;
            case "user2Info":
                user2Room = userRoom; user2RoomId = userRoomId;
                break;
                default:
                    break;
        }
    }
    //时间选择窗口
    @Override
    public void onSelectedTime(int hourStart, int minuteStart, int hourEnd, int minuteEnd) {
        // Use parameters provided by Dialog
        //如果开始时间小于7:00
        if (hourStart < 7){
            hourStart = 7;
            minuteStart = 40;
            userStartTime = "0" + hourStart + ":" + minuteStart;
            Toast.makeText(TimingTasksActivity.this,"时间设定不得小于7:00，已自动为您更改",Toast.LENGTH_SHORT).show();
        }else if (hourStart < 10){//hour<10
            if (minuteStart < 10){//minute & hour < 10
                userStartTime = "0" + hourStart + ":" + "0" + minuteStart;
            }else {//hour < 10  minute > 10
                userStartTime = "0" + hourStart + ":" + minuteStart;
            }
        }else {//hour > 10
            if (minuteStart < 10){//hour > 10  minute < 10
                userStartTime = hourStart + ":" + "0" + minuteStart;
            }else {//hour > 10  minute > 10
                userStartTime = hourStart + ":" + minuteStart;
            }
        }
        if (hourEnd < 7){
            minuteEnd = 40;
            userEndTime = "0" + hourEnd + ":" + minuteEnd;
            Toast.makeText(TimingTasksActivity.this,"时间设定不得小于7:00，已为您自动更改",Toast.LENGTH_SHORT).show();
        }else if (hourEnd < 10){//hour<10
            if (minuteEnd < 10){//minute & hour < 10
                userEndTime = "0" + hourEnd + ":" + "0" + minuteEnd;
            }else {//hour < 10  minute > 10
                userEndTime = "0" + hourEnd + ":" + minuteEnd;
            }
        }else {//hour > 10
            if (minuteEnd < 10){//hour > 10  minute < 10
                userEndTime = hourEnd + ":" + "0" + minuteEnd;
            }else {//hour > 10  minute > 10
                userEndTime = hourEnd + ":" + minuteEnd;
            }
        }


        if (user1Accept){
            user1Accept = false;
            Button user1stime = findViewById(R.id.user1starttime);
            Button user1etime = findViewById(R.id.user1endtime);
            user1stime.setText(userStartTime);
            user1etime.setText(userEndTime);
            user1StartTime = userStartTime;
            user1EndTime = userEndTime;
        }else if (user2Accept){
            user2Accept = false;
            Button user2stime = findViewById(R.id.user2starttime);
            Button user2etime = findViewById(R.id.user2endtime);
            user2stime.setText(userStartTime);
            user2etime.setText(userEndTime);
            user2StartTime = userStartTime;
            user2EndTime = userEndTime;
        }
    }
    public void showCustomDialogTimePicker() {
        // Create an instance of the dialog fragment and show it
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

        dialog.setInitialStartClock(8,00);//初始化开始时间
        dialog.setInitialEndClock(22,30);//初始化结束时间
        FragmentManager fragmentManager = getFragmentManager();
        dialog.show(fragmentManager, "");
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
    //自动填充座位号
    public void autoFillDialog(String title,String msg, Context context){
        android.support.v7.app.AlertDialog.Builder builder=new android.support.v7.app.AlertDialog.Builder(context,R.style.dialog_style);
        builder.setIcon(R.drawable.applygreen);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String[] r = parseSeatId(user1Room,user1PrefSeat,"user1");//检索座位号是否有误,如果有则给出更改提示
                EditText prefseatEdit = findViewById(R.id.edit_user1prefseat);
                prefseatEdit.setText(user1PrefSeat);//更新显示
                user1[5] = user1PrefSeat;//传值更改
                SharedPreferences.Editor user1Infopref = getSharedPreferences("user1Info",MODE_PRIVATE).edit();//保存
                user1Infopref.putString("prefseat",user1[5]);
                user1Infopref.apply();
            }
        });
        builder.setNegativeButton("否", new DialogInterface.OnClickListener() {
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
    //解析座位id
    public String[] parseSeatId(String room,String id,String user){
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
            Log.e("rr",rr);
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
            showDialog("座位号","已自动更改为"+result[0],TimingTasksActivity.this);
        }
        if (user.equals("user1")){
            user1[5] = user1PrefSeat = result[0].substring(3,6);//自动更改并显示
            Message message = new Message();
            message.what = SEATERROR;
            handler.sendMessage(message);
        }else if (user.equals("user2")){
            user2PrefSeat = result[0].substring(3,6);//自动更改并显示
        }
        return result;
    }
    //Handl处理消息
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @Override
        public void  handleMessage(Message msg){
            if (msg.what == SEATERROR) {
                EditText prefSeatEdit = findViewById(R.id.edit_user1prefseat);
                prefSeatEdit.setText(user1PrefSeat);
            }
            super.handleMessage(msg);
        }
    };
    //从日期中提取时间
    public String subTime(String date){
        return date.substring(11,16);
    }
    //保持信息持久化，从文件中读出用户信息显示到对应位置
    public void printUserInfo(String userFileName){
        // 从保存的userInfo文件中获取信息，还原到界面；如果没有，就使用默认值
        String[] userinfo = new String[9];
        String username,userid,userpassword,room,roomid,prefseat,starttime,endtime,urlend;
        if (userFileName.equals("user1Info")){
            userinfo = user1;
        }else {
            userinfo = user2;
        }
        SharedPreferences pref = getSharedPreferences(userFileName,MODE_PRIVATE);
        username = pref.getString("username","");
        userid = pref.getString("userid","");
        userpassword = pref.getString("userpassword","");
        room = pref.getString("room","");
        roomid = pref.getString("roomid","");
        prefseat = pref.getString("prefseat","");
        starttime = pref.getString("starttime","");
        endtime = pref.getString("endtime","");
        urlend = pref.getString("urlend","");
        candidate1 = pref.getString("candidate1","");
        candidate2 = pref.getString("candidate2","");
        candidate3 = pref.getString("candidate3","");
        candidate4 = pref.getString("candidate4","");

        if (userFileName.equals("user1Info")){
            EditText usernameEdit = findViewById(R.id.edit_user1name);//读出并初始化界面用户名
            usernameEdit.setText(username);
            EditText studentIdEdit = findViewById(R.id.edit_user1id);//读出并初始化界面的用户id
            studentIdEdit.setText(userid);
            EditText passwordEdit = findViewById(R.id.edit_user1password);//读出并初始化界面用户密码
            passwordEdit.setText(userpassword);
            EditText prefseatEdit = findViewById(R.id.edit_user1prefseat);//读出并初始化界面用户首选座位
            prefseatEdit.setText(prefseat);
            Button user1starttime =  findViewById(R.id.user1starttime);//初始化按钮上的开始时间
            user1starttime.setText(starttime);
            Button user1endtime = findViewById(R.id.user1endtime);//初始化按钮上的结束时间
            user1endtime.setText(endtime);
            EditText candidate1Edit = findViewById(R.id.candidate1);
            candidate1Edit.setText(candidate1);
            EditText candidate2Edit = findViewById(R.id.candidate2);
            candidate2Edit.setText(candidate2);
            EditText candidate3Edit = findViewById(R.id.candidate3);
            candidate3Edit.setText(candidate3);
            EditText candidate4Edit = findViewById(R.id.candidate4);
            candidate4Edit.setText(candidate4);
        }else {
            EditText usernameEdit = findViewById(R.id.edit_user2name);//读出并初始化界面用户名
            usernameEdit.setText(username);
            EditText studentIdEdit = findViewById(R.id.edit_user2id);//读出并初始化界面的用户id
            studentIdEdit.setText(userid);
            EditText passwordEdit = findViewById(R.id.edit_user2password);//读出并初始化界面用户密码
            passwordEdit.setText(userpassword);
            EditText prefseatEdit = findViewById(R.id.edit_user2prefseat);//读出并初始化界面用户首选座位
            prefseatEdit.setText(prefseat);
            Button user2starttime =  findViewById(R.id.user2starttime);//初始化按钮上的开始时间
            user2starttime.setText(starttime);
            Button user2endtime = findViewById(R.id.user2endtime);//初始化按钮上的结束时间
            user2endtime.setText(endtime);
        }
    }
    //获取用户输入的信息,username,studentid,password,roomid
    public boolean getUser1InfoFromScreen(){
        //  获取用户提交的信息，并对数据进行更新保存
        EditText usernameEdit = findViewById(R.id.edit_user1name);
        user1[0] = user1Name = usernameEdit.getText().toString();
        EditText studentIdEdit = findViewById(R.id.edit_user1id);
        user1[1] = user1Id = studentIdEdit.getText().toString();
        EditText passwordEdit = findViewById(R.id.edit_user1password);
        user1[2] = user1Password = passwordEdit.getText().toString();
        user1[3] = user1Room;
        user1[4] = user1RoomId;
        EditText prefseatEdit = findViewById(R.id.edit_user1prefseat);
        user1[5] = user1PrefSeat = prefseatEdit.getText().toString();
        Button user1stime = findViewById(R.id.user1starttime);
        Button user1etime = findViewById(R.id.user1endtime);
        user1StartTime = user1stime.getText().toString();
        user1EndTime = user1etime.getText().toString();
        user1[6] = user1StartTime;
        user1[7] = user1EndTime;
        EditText candidate1Edit = findViewById(R.id.candidate1);
        candidateArray[0] = candidate1 = candidate1Edit.getText().toString();
        EditText candidate2Edit = findViewById(R.id.candidate2);
        candidateArray[1] = candidate2 = candidate2Edit.getText().toString();
        EditText candidate3Edit = findViewById(R.id.candidate3);
        candidateArray[2] = candidate3 = candidate3Edit.getText().toString();
        EditText candidate4Edit = findViewById(R.id.candidate4);
        candidateArray[3] = candidate4 = candidate4Edit.getText().toString();

        candidateArray[0] = candidate1;
        candidateArray[1] = candidate2;
        candidateArray[2] = candidate3;
        candidateArray[3] = candidate4;


        //  将用户输入的roomName转化为对应的roomid在程序中运行
        if (user1[0].isEmpty()|user1[1].isEmpty()|user1[2].isEmpty() | user1[3].isEmpty() | user1[6].isEmpty() | user1[7].isEmpty()){
            Toast.makeText(TimingTasksActivity.this,"请输入完整信息",Toast.LENGTH_SHORT).show();
        }else if (user1[3].equals("一阅")){
            user1[4] = "100457211";
            user1[8] = "1551697412954";
        }else if (user1[3].equals("二阅")){
            user1[4] = "100457213";
            user1[8] = "1551697526282";
        }else if (user1[3].equals("三阅")){
            user1[4] = "101439229";
            user1[8] = "1551696819132";
        }else if (user1[3].equals("四阅")){
            user1[4] = "101439231";
            user1[8] = "1551687360999";
        }else if (user1[3].equals("五阅")){
            user1[4] = "101439233";
            user1[8] = "1551696819132";
        }else if (user1[3].equals("树华A")){
            user1[4] = "100457221";
            user1[8] = "1551699007393";
        }else {
            Toast.makeText(TimingTasksActivity.this,"输入错误！请重新输入！",Toast.LENGTH_SHORT).show();
        }

        if (!user1[0].isEmpty() & !user1[1].isEmpty() & !user1[2].isEmpty() & !user1[3].isEmpty() & !user1[6].isEmpty() & !user1[7].isEmpty()){
            if (user1[5].isEmpty()){
                autoFillDialog("座位号","座位为空，是否需要随机指定？",TimingTasksActivity.this);
            }else {
                parseSeatId(user1[3],user1[5],"user1");
            }

            // 保存用户数据
            SharedPreferences.Editor user1Infopref = getSharedPreferences("user1Info",MODE_PRIVATE).edit();
            user1Infopref.putString("username",user1[0]);
            user1Infopref.putString("userid",user1[1]);
            user1Infopref.putString("userpassword",user1[2]);
            user1Infopref.putString("room",user1[3]);
            user1Infopref.putString("roomid",user1[4]);
            user1Infopref.putString("prefseat",user1[5]);
            user1Infopref.putString("starttime",user1[6]);
            user1Infopref.putString("endtime",user1[7]);
            user1Infopref.putString("urlend",user1[8]);
            user1Infopref.putString("candidate1",candidateArray[0]);
            user1Infopref.putString("candidate2",candidateArray[1]);
            user1Infopref.putString("candidate3",candidateArray[2]);
            user1Infopref.putString("candidate4",candidateArray[3]);
            user1Infopref.apply();
            return true;
        }else {
            return false;
        }
    }
    public boolean getUser2InfoFromScreen(){
        //  获取用户提交的信息，并对数据进行更新保存
        EditText usernameEdit = findViewById(R.id.edit_user2name);
        user2[0] = user2Name = usernameEdit.getText().toString();
        EditText studentIdEdit = findViewById(R.id.edit_user2id);
        user2[1] = user2Id = studentIdEdit.getText().toString();
        EditText passwordEdit = findViewById(R.id.edit_user2password);
        user2[2] = user2Password = passwordEdit.getText().toString();
        user2[3] = user2Room;
        //user2[4] = user2RoomId;
        EditText prefseatEdit = findViewById(R.id.edit_user2prefseat);
        user2[5] = user2PrefSeat = prefseatEdit.getText().toString();
        Button user2stime = findViewById(R.id.user2starttime);
        Button user2etime = findViewById(R.id.user2endtime);
        user2StartTime = user2stime.getText().toString();
        user2EndTime = user2stime.getText().toString();
        user2[6] = user2StartTime;
        user2[7] = user2EndTime;

        //  将用户输入的roomName转化为对应的roomid在程序中运行
        if (user2[0].isEmpty()|user2[1].isEmpty()|user2[2].isEmpty()||user2[3].isEmpty() | !user2[6].isEmpty() | !user2[7].isEmpty()){
            Toast.makeText(TimingTasksActivity.this,"请输入完整信息",Toast.LENGTH_SHORT).show();
        }else if (user2[3].equals("一阅")){
            user2[4] = "100457211";
            user2[8] = "1551697412954";
        }else if (user2[3].equals("二阅")){
            user2[4] = "100457213";
            user2[8] = "1551697526282";
        }else if (user2[3].equals("三阅")){
            user2[4] = "101439229";
            user2[8] = "1551696819132";
        }else if (user2[3].equals("四阅")){
            user2[4] = "101439231";
            user2[8] = "1551687360999";
        }else if (user2[3].equals("五阅")){
            user2[4] = "101439233";
            user2[8] = "1551696819132";
        }else if (user2[3].equals("树华A")){
            user2[4] = "100457221";
            user2[8] = "1551699007393";
        }else {
            Toast.makeText(TimingTasksActivity.this,"输入错误！请重新输入！",Toast.LENGTH_SHORT).show();
        }

        if (!user2[0].isEmpty() | !user2[1].isEmpty() | !user2[2].isEmpty()| !user2[3].isEmpty() | !user2[6].isEmpty() | !user2[7].isEmpty()){
            // 保存用户数据
            SharedPreferences.Editor user2Infopref = getSharedPreferences("user2Info",MODE_PRIVATE).edit();
            user2Infopref.putString("username",user2[0]);
            user2Infopref.putString("userid",user2[1]);
            user2Infopref.putString("userpassword",user2[2]);
            user2Infopref.putString("room",user2[3]);
            user2Infopref.putString("roomid",user2[4]);
            user2Infopref.putString("prefseat",user2[5]);
            user2Infopref.putString("starttime",user2[6]);
            user2Infopref.putString("endtime",user2[7]);
            user2Infopref.apply();
            return true;
        }else {
            return false;
        }

    }
    public boolean readUser1Info(){
        String checkName,checkId,checkPassword,checkRoom,checkRoomId,checkPrefSeat,checkST,checkET,checkUrlEnd;
        SharedPreferences pref = getSharedPreferences("user1Info",MODE_PRIVATE);
        checkName = pref.getString("username","");
        checkId = pref.getString("userid","");
        checkPassword = pref.getString("userpassword","");
        checkRoom = pref.getString("room","");
        checkRoomId = pref.getString("roomid","");
        checkPrefSeat = pref.getString("prefseat","");
        checkST = pref.getString("starttime","");
        checkET = pref.getString("endtime","");
        checkUrlEnd = pref.getString("urlend","");

        Log.e("checkId",checkId);
        Log.e("checkpassword",checkPassword);
        Log.e("checkroo,",checkRoom);
        return  !checkId.isEmpty() & !checkPassword.isEmpty() & !checkRoom.isEmpty();
    }
}
