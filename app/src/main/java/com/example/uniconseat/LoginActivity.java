package com.example.uniconseat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends BaseActivity {
    String applyCode,verifyCode,authotizationCode,authotizationCodeDelTime;//从屏幕获取到的申请码和授权码
    String serialIMEI,serialIMEIAndTime,secreKey,android_id=null;//算法生成的唯一标识号、授权码、验证码
    public static final int REQUEST_READ_PHONE_STATE = 1;
    public String applyTime,dueTime,applyTimeEncrpy;//申请时间，到期时间
    public static boolean exit2back = false;

    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //读取手机的IMEI序列号
        TelephonyManager TelephonyMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        //检查授权状态
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
            //    return;
        }
        //授权时间
        dueTime = CommonFunction.systemDelayTime(15 * 1000,1);//2019-06-04 22:30
        applyTime = CommonFunction.systemTime();
        //0-Z 1-N 2-W 3-H 4-R 5-V 6-X 7-S 8-G 9-I :-M "-"-L " "-P

        //生成加密后的手机标识
        android_id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.e("android_id",android_id);
        serialIMEI = CommonFunction.getIMEI(LoginActivity.this);
        Log.e("serialIMEI",serialIMEI);
        //0FC13C9B7BF7A294C5A124F665B416EB
        //0FC13C9B7BF7A294C5A124F665B416EB
        //时间加密
        applyTimeEncrpy = CommonFunction.date2Int(applyTime);
        //申请码中添加密时间
        serialIMEIAndTime = serialIMEI.substring(0,7) + applyTimeEncrpy.substring(0,5) +
                serialIMEI.substring(7,13) + applyTimeEncrpy.substring(5,9) +
                serialIMEI.substring(13,22) + applyTimeEncrpy.substring(9,15) +
                serialIMEI.substring(22,32) + applyTimeEncrpy.substring(15,19);

        //显示加密后的申请码、验证码
        EditText applyId = findViewById(R.id.edit_apply);
        applyId.setText(serialIMEIAndTime);
        EditText androidId = findViewById(R.id.edit_verify);
        androidId.setText(android_id);

        ToastCustom.passValue(3000,1000,2,0,100);
        ToastCustom.getInstance(getApplicationContext()).show("15分钟内有效，点击返回，退出程序", 3000);
        //监听按钮
        Button submitButton = findViewById(R.id.submit);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit();
            }
        });
        Button contactButton = findViewById(R.id.act_contact);
        contactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                feedbackDialog();
            }
        });
    }

    //设置按钮监听事件
    public void submit(){
        /**  从屏幕重新获取信息  **/
        //getInfoFromScreen();

        //  获取用户提交的信息，并对数据进行更新保存
        EditText applyEdit = findViewById(R.id.edit_apply);
        applyCode = applyEdit.getText().toString();
        EditText verifyEdit = findViewById(R.id.edit_verify);
        verifyCode = verifyEdit.getText().toString();
        EditText authorizeEdit = findViewById(R.id.edit_authorize);
        authotizationCode = authorizeEdit.getText().toString();

        //日期核验
        //1-7 (1-5) 8-13 (6-9) 14-22 (10-15) 23-32  (16-19)
        //0 1 2 3 4 5 6 (7 8 9 10 11)
        // 12 13 14 15 16 17 (18 19 20 21)
        // 22 23 24 25 26 27 28 29 30 (31 32 33 34 35 36)
        // 37 38 39 40 41 42 43 44 45 46 (47 48 49 50)

        //生成授权码并保存
        secreKey = CommonFunction.generateActivCode(serialIMEI,android_id,applyTime);

        if (authotizationCode.isEmpty()){
            Toast.makeText(LoginActivity.this,"请输入授权码",Toast.LENGTH_SHORT).show();
        }else {
            String catchTime=null;
            try{
                catchTime = authotizationCode.substring(7,12) + authotizationCode.substring(18,22) +
                        authotizationCode.substring(31,37) + authotizationCode.substring(47,51);//捕获加密的时间
                catchTime = CommonFunction.int2Date(catchTime);//解析出真实时间
                Log.e("catchTime",catchTime);
                authotizationCodeDelTime = authotizationCode.substring(0,7) +authotizationCode.substring(12,18)+
                        authotizationCode.substring(22,31) + authotizationCode.substring(37,48);//捕获出授权码
                Log.e("authotizationCodelTime",authotizationCodeDelTime);
            }catch (Exception e){
                Toast.makeText(LoginActivity.this,"非本软件授权码",Toast.LENGTH_SHORT).show();
                return;
            }
            if (CommonFunction.timeCompare(catchTime,dueTime) != 1){//
//                Log.e("applyCode",applyCode);
//                Log.e("serialIMEIAndTime",serialIMEIAndTime);
//                Log.e("verifyCode",verifyCode);
//                Log.e("android_id",android_id);
//                Log.e("authotizationCodeD",authotizationCodeDelTime);
//                Log.e("secreKey",secreKey);

                if (applyCode.equals(serialIMEIAndTime) & verifyCode.equals(android_id) & authotizationCodeDelTime.equals(secreKey)){
                    //更改注册信息
                    SharedPreferences sharedPreferences = this.getSharedPreferences("login",MODE_PRIVATE);
                    SharedPreferences.Editor logineditor = sharedPreferences.edit();
                    logineditor.putBoolean("login", true);
                    logineditor.commit();

                    //切换到Help页面
                    SharedPreferences exitToBack = this.getSharedPreferences("exitToBack",MODE_PRIVATE);
                    SharedPreferences.Editor exitToBackeditor = exitToBack.edit();
                    exitToBackeditor.putString("exitToBack", "exit");
                    exitToBackeditor.commit();

                    Intent intenthelp = new Intent(getApplicationContext(),ScrollingActivity.class);
                    startActivity(intenthelp);
                    ToastCustom.passValue(5000,1000,2,0,100);
                    ToastCustom.getInstance(getApplicationContext()).show("验证成功，请退出程序后重新进入！", 5000);
                    onBackPressed();
                }else {
                    Toast.makeText(LoginActivity.this,"授权码输入错误",Toast.LENGTH_SHORT).show();
                }
            }else {
                Toast.makeText(LoginActivity.this,"授权码已过期",Toast.LENGTH_SHORT).show();
            }
        }
    }
    //feedback Dialog
    public void feedbackDialog(){
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(getApplicationContext(),R.style.dialog_style);
        builder.setTitle("激活");
        builder.setMessage("选择申请方式，或点击空白处取消");
        builder.setCancelable(true);
        builder.setIcon(R.drawable.feedback);
        builder.setPositiveButton("QQ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intentqq = new Intent();
                intentqq.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ComponentName componentName = new ComponentName("com.tencent.mobileqq","com.tencent.mobileqq.activity.SplashActivity");
                intentqq.setComponent(componentName);
                try {
                    startActivity(intentqq);
                }catch (Exception e){
                    ToastCustom.passValue(2000,1000,2,0,100);
                    ToastCustom.getInstance(getApplicationContext()).show("您尚未安装QQ，请前往下载或使用其他方式", 2000);
                }
            }
        });
        builder.setNeutralButton("TIM", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intenttim = new Intent();
                intenttim.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ComponentName componentName = new ComponentName("com.tencent.tim","com.tencent.mobileqq.activity.SplashActivity");
                intenttim.setComponent(componentName);
                try {
                    startActivity(intenttim);
                }catch (Exception e){
                    ToastCustom.passValue(2000,1000,2,0,100);
                    ToastCustom.getInstance(getApplicationContext()).show("您尚未安装TIM，请前往下载或使用其他方式", 2000);
                }
            }
        });

        builder.setNegativeButton("微信", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intentwx = new Intent();
                intentwx.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ComponentName componentName = new ComponentName("com.tencent.mm","com.tencent.mm.ui.LauncherUI");
                intentwx.setComponent(componentName);
                try {
                    startActivity(intentwx);
                }catch (Exception e){
                    ToastCustom.passValue(2000,1000,2,0,100);
                    ToastCustom.getInstance(getApplicationContext()).show("您尚未安装TIM，请前往下载或使用其他方式", 2000);
                }
            }
        });
        android.support.v7.app.AlertDialog dialog=builder.create();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.getWindow().setType((WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY));
        }else {
            dialog.getWindow().setType((WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));
        }
        dialog.show();
        Window dialogWindow = dialog.getWindow();   //获取dialog所属的window
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();   //获取window的布局属性
        dialogWindow.setGravity(Gravity.CENTER);

    }
    @Override
    public void onBackPressed(){
        Toast.makeText(LoginActivity.this,"已退出!",Toast.LENGTH_SHORT).show();
        System.exit(0);
    }
    public void getInfoFromScreen(){
        //  获取用户提交的信息，并对数据进行更新保存
        EditText usernameEdit = findViewById(R.id.edit_apply);
        applyCode = usernameEdit.getText().toString();
        EditText studentIdEdit = findViewById(R.id.edit_authorize);
        authotizationCode = studentIdEdit.getText().toString();
        // 保存用户数据
        try {
            SharedPreferences userCode = getSharedPreferences("userCode",MODE_PRIVATE);
            SharedPreferences.Editor userCodeEditor = userCode.edit();
            userCodeEditor.putString("applyCode",applyCode);
            userCodeEditor.putString("authorizationCode",authotizationCode);
            userCodeEditor.apply();
        }catch (Exception e){
            e.printStackTrace();
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        boolean isGranted = true;
        for (int i : grantResults){
            if (grantResults[i] == PackageManager.PERMISSION_DENIED){
                isGranted = false;
                break;
            }
        }
        if (!isGranted){
            openAppDetails();
        }else {
            finish();
        }
    }
    //来源于CSDN打开 APP 的详情设置
    private void openAppDetails() {
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this,R.style.dialog_style);
        builder.setTitle("设置");
        builder.setMessage("请前往设置中心为该应用开启读取手机身份状态权限，以便完成注册");
        builder.setCancelable(false);
        builder.setIcon(R.drawable.setting);
        builder.setPositiveButton("前往设置中心", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intentSetting =  new Intent(Settings.ACTION_SETTINGS);
                startActivity(intentSetting);
                intentSetting.addCategory(Intent.CATEGORY_DEFAULT);
                intentSetting.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intentSetting.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intentSetting.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(intentSetting);
            }
        });
        builder.setNegativeButton("否", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ToastCustom.passValue(3000,1000,2,0,100);
                ToastCustom.getInstance(getApplicationContext()).show("再次申请，请点击屏幕左上角前往设置中心", 3000);
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
