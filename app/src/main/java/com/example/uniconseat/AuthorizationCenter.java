package com.example.uniconseat;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class AuthorizationCenter extends AppCompatActivity {
    public static boolean overDisplay = false;//在其他应用之上显示
    public static boolean phoneState = false;//读取手机身份状态
    public static boolean ignoreBattery = false;
    private final static int REQUEST_IGNORE_BATTERY_CODE = 1001;
    private boolean login;
    //到期时间
    static String dueTime = "2020-01-01 00:00:00";//目标时间06:25:00
    //授权管理
    String[] allPermissions = new String[]{Manifest.permission.READ_PHONE_STATE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authorization_center);


        final Activity activity = getParent();

        SharedPreferences sharedAuthorize = getSharedPreferences("AuthorizationCenter",MODE_PRIVATE);
        overDisplay = sharedAuthorize.getBoolean("overDisplay",false);
        phoneState = sharedAuthorize.getBoolean("phoneState",false);

        //find布局中控件的id
        final Button overButton = findViewById(R.id.over_display);
        Button phoneButton = findViewById(R.id.phone_state);
        Button ignoreButton = findViewById(R.id.ignore_battery);
        //ignoreButton.setTextColor(0x88888888);

        //在其他应用之上显示的权限检查
        if (Build.VERSION.SDK_INT >= 23) {
            if(!Settings.canDrawOverlays(this)) {
                overDisplay = false;
            }else {
                overDisplay = true;
            }
        }else{
            overDisplay = true;
        }

        //更新按钮显示的文字
        //是否注册
        SharedPreferences loginPreferences = getSharedPreferences("login",MODE_PRIVATE);
        login =loginPreferences.getBoolean("login", false);//记得修改为false
        if (overDisplay){
            overButton.setText("完成");
            overButton.setTextColor(0x88ff0000);
        }
        if (phoneState & overDisplay & login ){
            phoneButton.setText("完成");
            phoneButton.setTextColor(0x88ff0000);
        }else if (phoneState & !overDisplay){
            phoneButton.setText("继续");
            phoneButton.setTextColor(0x88ff0000);
        }else if (phoneState & overDisplay & !login){
            phoneButton.setText("激活");
            phoneButton.setTextColor(0x88ff0000);
        }

        //监听显示在其他应用之上按钮
        overButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (overDisplay){
                    ToastCustom.passValue(2000,1000,2,0,100);
                    ToastCustom.getInstance(getApplicationContext()).show("您已完成相应授权", 2000);
                }else {
                    if (Build.VERSION.SDK_INT >= 23) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                        startActivity(intent);
                    }else {
                        ToastCustom.passValue(2000,1000,2,0,100);
                        ToastCustom.getInstance(getApplicationContext()).show("您设备的当前系统不支持该选项", 2000);
                    }
                }
            }
        });
        //监听读取手机身份状态按钮
        phoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (phoneState){
                    //是否注册
                    SharedPreferences loginPreferences = getSharedPreferences("login",MODE_PRIVATE);
                    login =loginPreferences.getBoolean("login", false);//记得修改为false
                    if (login & overDisplay){
                        ToastCustom.passValue(2000,1000,2,0,100);
                        ToastCustom.getInstance(getApplicationContext()).show("您已可正常使用", 2000);
                    }else if (login & !overDisplay){
                        ToastCustom.passValue(2000,1000,2,0,100);
                        ToastCustom.getInstance(getApplicationContext()).show("您还有授权没有完成，请前往", 2000);
                    }
                    if (!login & overDisplay){
                        Intent intentLogin = new Intent(getApplicationContext(),LoginActivity.class);
                        startActivity(intentLogin);
                        ToastCustom.passValue(2000,1000,2,0,100);
                        ToastCustom.getInstance(getApplicationContext()).show("您已完成相应授权，现在前往注册", 2000);
                    }
                }else {
                    applyPermission();
                }
            }
        });
        //忽略电池优化按钮
        ignoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //电池优化检查
                showDialogNotCancel(getApplicationContext());
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
    @Override
    public void onResume(){
        checkAuthorize();
        //是否注册
        SharedPreferences loginPreferences = getSharedPreferences("login",MODE_PRIVATE);
        login =loginPreferences.getBoolean("login", false);//记得修改为false
        SharedPreferences sharedAuthorize = getSharedPreferences("AuthorizationCenter",MODE_PRIVATE);
        overDisplay = sharedAuthorize.getBoolean("overDisplay",false);
        phoneState = sharedAuthorize.getBoolean("phoneState",false);
        Button overButton = findViewById(R.id.over_display);
        Button phoneButton = findViewById(R.id.phone_state);
        if (overDisplay){
            overButton.setText("完成");
            overButton.setTextColor(0x88ff0000);
        }
        if (phoneState & overDisplay & login ){
            phoneButton.setText("完成");
            phoneButton.setTextColor(0x88ff0000);
        }else if (phoneState & !overDisplay){
            phoneButton.setText("继续");
            phoneButton.setTextColor(0x88ff0000);
        }else if (phoneState & overDisplay & !login){
            phoneButton.setText("激活");
            phoneButton.setTextColor(0x88ff0000);
        }
        super.onResume();
    }
    @Override
    public void onRestart(){
        checkAuthorize();
        //是否注册
        SharedPreferences loginPreferences = getSharedPreferences("login",MODE_PRIVATE);
        login =loginPreferences.getBoolean("login", false);//记得修改为false
        SharedPreferences sharedAuthorize = getSharedPreferences("AuthorizationCenter",MODE_PRIVATE);
        overDisplay = sharedAuthorize.getBoolean("overDisplay",false);
        phoneState = sharedAuthorize.getBoolean("phoneState",false);
        Button overButton = findViewById(R.id.over_display);
        Button phoneButton = findViewById(R.id.phone_state);
        Button ignoreButton = findViewById(R.id.ignore_battery);
        if (overDisplay){
            overButton.setText("完成");
            overButton.setTextColor(0x88ff0000);
        }
        if (phoneState & overDisplay & login ){
            phoneButton.setText("完成");
            phoneButton.setTextColor(0x88ff0000);
        }else if (phoneState & !overDisplay){
            phoneButton.setText("继续");
            phoneButton.setTextColor(0x88ff0000);
        }else if (phoneState & overDisplay & !login){
            phoneButton.setText("激活");
            phoneButton.setTextColor(0x88ff0000);
        }
        super.onRestart();
    }

    public void showDialogNotCancel(Context context){
        android.support.v7.app.AlertDialog.Builder builder=new android.support.v7.app.AlertDialog.Builder(context,R.style.dialog_style);
        builder.setIcon(R.drawable.applygreen);
        builder.setTitle("忽略电池优化");
        builder.setMessage("开启忽略电池优化使用，该应用将被加入白名单，耗电量会提高，对于6.0以上系统，这是必须的；否则将影响定时任务的使用");
        builder.setCancelable(false);
        builder.setPositiveButton("前往",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        isIgnoreBatteryOption(getParent());
                    }
                });
        builder.setNegativeButton("返回", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ToastCustom.passValue(2000,1000,2,0,100);
                ToastCustom.getInstance(getApplicationContext()).show("拒绝该授权，定时任务将无法工作，您可前往左上角“电池”进行管理。", 2000);
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

    //授权检查
    public void checkAuthorize(){
        SharedPreferences sharedAuthorize = getSharedPreferences("AuthorizationCenter",MODE_PRIVATE);
        SharedPreferences.Editor authorzieEditor = sharedAuthorize.edit();
        //在其他应用之上显示
        if (Build.VERSION.SDK_INT >= 23) {
            if(!Settings.canDrawOverlays(this)) {
                overDisplay = false;
                authorzieEditor.putBoolean("overDisplay",false);
                authorzieEditor.commit();
            }else {
                overDisplay = true;
                authorzieEditor.putBoolean("overDisplay",true);
                authorzieEditor.commit();
            }
        }else {
            overDisplay = true;
            authorzieEditor.putBoolean("overDisplay",true);
            authorzieEditor.commit();
        }
        //读取手机身份状态
        if (Build.VERSION.SDK_INT >= 23){
            for (String allPermission : allPermissions) {
                if (ActivityCompat.checkSelfPermission(this, allPermission) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(allPermission,allPermission);
                    phoneState = false;
                    authorzieEditor.putBoolean("phoneState",false);
                    authorzieEditor.commit();
                }else {
                    phoneState = true;
                    authorzieEditor.putBoolean("phoneState",true);
                    authorzieEditor.commit();
                }
            }
        }else {
            phoneState = true;
            authorzieEditor.putBoolean("phoneState",true);
            authorzieEditor.commit();
        }
        if (!phoneState | !overDisplay){
            MainActivity.allGrant = false;
            authorzieEditor.putBoolean("allGrant",false);
            authorzieEditor.commit();
        }else {
            MainActivity.allGrant =true;
            authorzieEditor.putBoolean("allGrant",true);
            authorzieEditor.commit();
        }
    }
    //授权管理
    public void applyPermission(){
        if (Build.VERSION.SDK_INT >= 23){
            for (String allPermission : allPermissions) {
                if (ActivityCompat.checkSelfPermission(this, allPermission) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(allPermission,allPermission);
                    ActivityCompat.requestPermissions(this, allPermissions, 1);
                }
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        boolean isGrantdeAll = false;
        for (int index = 0; index < grantResults.length; index ++){
            if (grantResults[index] == PackageManager.PERMISSION_GRANTED){
                isGrantdeAll = true;
                Log.e(permissions[index],"已授权");
            }else {
                isGrantdeAll = false;
                Log.e(permissions[index],"拒绝授权");
            }
        }
        if (!isGrantdeAll){
            openAppDetails("应用需要悬浮窗、自启动、锁屏显示、手机身份等权限，请到应用详情页开启，或点击启动页左上角设置前往设置中心");
        }
//        else {
//            Intent intentLogin = new Intent(getApplicationContext(),LoginActivity.class);
//            startActivity(intentLogin);
//            Log.e("已授权","正常使用");
//        }
    }
    //来源于CSDN /**  打开 APP 的详情设置   **/
    private void openAppDetails(String msg) {
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this,R.style.dialog_style);
        builder.setMessage(msg);
        builder.setPositiveButton("前往应用详情页", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setData(Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(intent);
            }
        });
        builder.setNegativeButton("取消", null);
        android.support.v7.app.AlertDialog dialog=builder.create();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.getWindow().setType((WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY));
        }else {
            dialog.getWindow().setType((WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));
        }
        dialog.show();
    }
    @TargetApi(Build.VERSION_CODES.M)
    public void isIgnoreBatteryOption(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent();
                String packageName = getPackageName();
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    startActivity(intent);
                    ToastCustom.passValue(2000,1000,2,0,100);
                    ToastCustom.getInstance(getApplicationContext()).show("请您开启忽略电池优化", 2000);
                }else {
                    ToastCustom.passValue(2000,1000,2,0,100);
                    ToastCustom.getInstance(getApplicationContext()).show("您已开启该权限", 2000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else {
            ToastCustom.passValue(2000,1000,2,0,100);
            ToastCustom.getInstance(getApplicationContext()).show("您设备的当前系统不支持该选项", 2000);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if (requestCode == REQUEST_IGNORE_BATTERY_CODE) {
                Log.d("Hello World!","开启省电模式成功");
            }
        }else if (resultCode == RESULT_CANCELED) {
            if (requestCode == REQUEST_IGNORE_BATTERY_CODE) {
                Toast.makeText(this, "请用户开启忽略电池优化~", Toast.LENGTH_LONG).show();
            }
        }
    }
}
