package com.example.uniconseat;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class ScrollingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        Toolbar toolbar = (Toolbar) findViewById(R.id.scrol_toolbar);
        setSupportActionBar(toolbar);
        Button exitbutton = findViewById(R.id.exit_bt);
        String exit2Back = "exit";
        SharedPreferences exitToBack = getSharedPreferences("exitToBack",MODE_PRIVATE);
        exit2Back = exitToBack.getString("exitToBack","exit");
        if (exit2Back.equals("exit")){
            exitbutton.setText("退出");
            exitbutton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedPreferences exitToBack = getSharedPreferences("exitToBack",MODE_PRIVATE);
                    SharedPreferences.Editor exitToBackeditor = exitToBack.edit();
                    exitToBackeditor.putString("exitToBack", "back");
                    exitToBackeditor.commit();
                    ActivityCollector.finishAll();
                    System.exit(0);
                }
            });
        }else{
            exitbutton.setText("返回");
            exitbutton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedPreferences exitToBack = getSharedPreferences("exitToBack",MODE_PRIVATE);
                    SharedPreferences.Editor exitToBackeditor = exitToBack.edit();
                    exitToBackeditor.putString("exitToBack", "back");
                    exitToBackeditor.commit();
                    ActivityCollector.finishAll();
                    onBackPressed();
                }
            });
        }


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.scrol_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                feedbackDialog("反馈","如有疑问，请选择前往方式反馈，或点击空白处取消");

            }
        });

        TextView scrollingText = findViewById(R.id.scrolling_text);
        scrollingText.setText(Html.fromHtml(getResources().getString(R.string.large_text)));
    }
    @Override
    public void onBackPressed(){
        finish();
        if (MainActivity.mainActivityStart){
            super.onBackPressed();
        }else {
            Intent intentMain= new Intent(getApplicationContext(),MainActivity.class);
            startActivity(intentMain);
        }
    }

    //feedback Dialog
    public void feedbackDialog(String Title,String msg){
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(getApplicationContext(),R.style.dialog_style);
        builder.setTitle(Title);
        builder.setMessage(msg);
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
                    ToastCustom.getInstance(getApplicationContext()).show("您尚未安装微信，请前往下载或使用其他方式", 2000);
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
}
