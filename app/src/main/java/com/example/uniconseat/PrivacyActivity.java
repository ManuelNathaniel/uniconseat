package com.example.uniconseat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
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

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class PrivacyActivity extends BaseActivity {
    private String userRoom,userRoomId,seat,urlForth;
    private String start,end,owner;
    private String[] re = new String[3];
    private OkHttpClient client;
    private static final int SEATISEMPTY = 0;
    private static final int DISPLAYPERSON1 = 1;
    private static final int DISPLAYPERSON2 = 2;
    private static final int DISPLAYPERSON3 = 3;
    // url共用字段
    private String urlCommonFirstPara = "http://202.206.242.87/ClientWeb/pro/ajax/device.aspx?byType=devcls&classkind=8&display=fp&md=d&room_id=";
    private String urlCommonSecondPara = "&purpose=&selectOpenAty=&cld_name=default&date=";
    private String urlCommonThirdPara = "&act=get_rsv_sta&_=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy);
        Spinner optionRoom = findViewById(R.id.option_room);
        spinnerRoomId(optionRoom);
        CardView display = findViewById(R.id.card_diplay);
        CardView display2 = findViewById(R.id.card_diplay2);
        CardView display3 = findViewById(R.id.card_diplay3);
        display.setVisibility(INVISIBLE);
        display2.setVisibility(INVISIBLE);
        display3.setVisibility(INVISIBLE);

        findYou();
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
    //Handl处理消息
    @SuppressLint("HandlerLeak")
    private  Handler handler = new Handler(){
        @Override
        public void  handleMessage(Message msg){
            CardView display = findViewById(R.id.card_diplay);
            CardView display2 = findViewById(R.id.card_diplay2);
            CardView display3 = findViewById(R.id.card_diplay3);
            switch (msg.what){
                case SEATISEMPTY:
                    display.setVisibility(VISIBLE);
                    TextView displayTs = findViewById(R.id.display_ts1);
                    displayTs.setVisibility(VISIBLE);
                    displayTs.setText("当前座位处于空闲状态");
                    break;
                case DISPLAYPERSON1:
                    display.setVisibility(VISIBLE);
                    TextView displayTs1 = findViewById(R.id.display_ts1);
                    displayTs1.setText(String.format(getResources().getString(R.string.ts1),owner,start,end));
                    displayTs1.setVisibility(VISIBLE);
                    Log.e("owner",owner);
                    break;
                case DISPLAYPERSON2:
                    display2.setVisibility(VISIBLE);
                    TextView displayTs2 = findViewById(R.id.display_ts2);
                    displayTs2.setText(String.format(getResources().getString(R.string.ts2),owner,start,end));
                    displayTs2.setVisibility(VISIBLE);
                    break;
                case DISPLAYPERSON3:
                    display3.setVisibility(VISIBLE);
                    TextView displayTs3 = findViewById(R.id.display_ts3);
                    displayTs3.setText(String.format(getResources().getString(R.string.ts3),owner,start,end));
                    displayTs3.setVisibility(VISIBLE);
                    break;
                    default:
                        break;
            }
            super.handleMessage(msg);
        }
    };
    //提交监听
    public void findYou(){
        Button optionButton = findViewById(R.id.option_button);
        optionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText optionSeat = findViewById(R.id.option_seat);
                seat = optionSeat.getText().toString();
                Log.e("seat",seat);
                if (seat.isEmpty()){
                    Toast.makeText(getApplicationContext(),"请输入具体的座位号",Toast.LENGTH_SHORT).show();
                }else {
                    re = parseSeatId(userRoom,seat,1);
                    parseAndDisplay();
                }
            }
        });
    }
    //监听阅览室选择Spinner
    public void spinnerRoomId(Spinner spinner){
        int index = 4;
        final SharedPreferences spinnerDefaultRoomIdpref = getSharedPreferences("findyou",MODE_PRIVATE);
        userRoomId = spinnerDefaultRoomIdpref.getString("roomid","101439231");//101439231是第四阅览室id
        //  将roomid转化为中文，显示在对应的输入框中
        switch (userRoomId){
            case "100457211":
                userRoom = "一阅";
                urlForth = "1551697412954";
                index = 1;
                break;
            case "100457213":
                userRoom = "二阅";
                urlForth = "1551697526282";
                index = 2;
                break;
            case "101439229":
                userRoom = "三阅";
                urlForth = "1551696819132";
                index = 3;
                break;
            case "101439231":
                userRoom = "四阅";
                urlForth = "1551687360999";
                index = 4;
                break;
            case "101439233":
                userRoom = "五阅";
                urlForth = "1551696819132";
                index = 5;
                break;
            case "100457221":
                userRoom = "树华A";
                urlForth = "1551699007393";
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
                        Toast.makeText(getApplicationContext(),"重新选择阅览室",Toast.LENGTH_SHORT).show();
                        break;
                }
                SharedPreferences.Editor spinnerEditor =  spinnerDefaultRoomIdpref.edit();
                spinnerEditor.putString("room",userRoom);
                spinnerEditor.putString("roomid",userRoomId);
                spinnerEditor.commit();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //当所有选项都没有被选择时触发
            }
        });

    }
    //获取座位信息
    private void parseAndDisplay(){
        CardView display = findViewById(R.id.card_diplay);
        CardView display2 = findViewById(R.id.card_diplay2);
        CardView display3 = findViewById(R.id.card_diplay3);
        display.setVisibility(INVISIBLE);
        display2.setVisibility(INVISIBLE);
        display3.setVisibility(INVISIBLE);
        final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {//实现同一个cookie访问
                    client = new OkHttpClient.Builder()
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
                    String currentStatus =obtainAndParseStatus(client);
                    String data = parseJSONFromResponse(currentStatus);

                    //"ts":[{"id":null,"start":"2019-06-02 14:00","end":"2019-06-02 22:20","state":"doing","date":null,"name":null,
                    // "title":null,"owner":"张小三","accno":"100047230","member":"","limit":null,"occupy":true}],

                    JSONArray jsonArray = new JSONArray(data);
                    String targetSeat = jsonArray.getString(Integer.parseInt(re[1]));
                    String ts = CommonFunction.parseSingleLabel(targetSeat,"ts");

                    if (ts.equals("[]")){
                        Message message = new Message();
                        message.what = SEATISEMPTY;
                        handler.sendMessage(message);
                    }else {
                        JSONArray jsonArray1 = new JSONArray(ts);
                        int length = jsonArray1.length();
                        for (int i = 0; i < length; i++){
                            String person = jsonArray1.getString(i);
                            start = CommonFunction.parseSingleLabel(person,"start").substring(11,16);
                            end = CommonFunction.parseSingleLabel(person,"end").substring(11,16);
                            owner = CommonFunction.parseSingleLabel(person,"owner");
                            switch (i){
                                case 0:
                                    Message message = new Message();
                                    message.what = DISPLAYPERSON1;
                                    handler.sendMessage(message);
                                    break;
                                case 1:
                                    Message message2 = new Message();
                                    message2.what = DISPLAYPERSON2;
                                    handler.sendMessage(message2);
                                    break;
                                case 2:
                                    Message message3 = new Message();
                                    message3.what = DISPLAYPERSON3;
                                    handler.sendMessage(message3);
                                    break;
                                    default:
                                        break;
                            }
                            if (i>2){
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(),"仅显示前三条预约信息",Toast.LENGTH_LONG).show();
                                    }
                                });
                                break;
                            }
                            Thread.sleep(20);
                        }
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),"查询完毕",Toast.LENGTH_LONG).show();
                        }
                    });
                }catch (Exception e){
                    Log.e("e",e.toString());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),"访问出错，请检查网络",Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
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
            seat = result[0].substring(3,6);//自动更改并显示
            SharedPreferences.Editor userInfopref = getSharedPreferences("findyou",MODE_PRIVATE).edit();
            userInfopref.putString("seat",seat);
            userInfopref.apply();
            switch (idx){
                case 0://主线程
                    EditText prefseatEdit = findViewById(R.id.option_seat);
                    prefseatEdit.setText(seat);//更新显示
                    showDialog("座位号异常","已自动将其更改为"+ result[0],PrivacyActivity.this);
                    break;
                case 1:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showDialog("座位号异常","已自动将其更改为"+ result[0],PrivacyActivity.this);
                        }
                    });
                    break;
                default:
                    break;
            }
        }
        return result;
    }
    //从服务器获取座位状态信息（包含解析与保存步骤）
    public String obtainAndParseStatus(OkHttpClient client) throws IOException {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String reserve = df.format(new Date());// 当前时间加40分钟,该时间格式为2019-06-01 18:25
        String date = reserve.substring(0,10);//日期2019-06-01
        String time = reserve.substring(11,16);//预约开始时间     例：18:25
        //2.    日期时间格式整合
//        String fr_start = new StringBuilder().append(CommonFunction.getMatcher("(.*):", time)).append("%3A")
//                .append(CommonFunction.getMatcher(":(.*)", time))
//                .toString();
        String fr_start = "07%3A00";
        String fr_end = "22%3A30";

        //http://202.206.242.87/ClientWeb/pro/ajax/device.aspx?byType=devcls&classkind=8&display=fp&md=d&room_id=100457213
        // &purpose=&selectOpenAty=&cld_name=default&date=2019-06-21&fr_start=14%3A00&fr_end=21%3A00&act=get_rsv_sta&_=1561082040042
        String roomIdUrl = urlCommonFirstPara.concat(userRoomId).concat(urlCommonSecondPara)
                .concat(date).concat("&fr_start=").concat(fr_start).concat("&fr_end=").concat(fr_end)
                .concat(urlCommonThirdPara).concat(urlForth);//目标阅览室url
        // 访问服务器座位状态信息
        Request getRsvSta = new Request.Builder().url(roomIdUrl).build();
        Response rsvStaRsponse = client.newCall(getRsvSta).execute();
        String rsvStateData = rsvStaRsponse.body().string();
        //从服务器返回的数据中解析出座位状态信息，并保存到data
        //parseJSONFromResponse(rsvStateData);
        //Log.e("YSU从服务器获取并保存座位状态信息","成功");
        return rsvStateData;
    }
    //解析出多个标签的数据,ret,act,msg,data,ext et al.
    private String parseJSONFromResponse(String jsonData){
        String result = "";
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            String data = jsonObject.getString("data");
            //saveResvSta(data,"data");
            Log.v("保存成功","保存成功");
            result = data;
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),"解析出错",Toast.LENGTH_LONG).show();
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
}
