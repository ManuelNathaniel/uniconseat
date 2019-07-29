package com.example.uniconseat;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class BaseActivity extends AppCompatActivity {
    IntentFilter intentFilter = new IntentFilter();
    BroadcastReceiver receiver = new HooliganReceiver();
    @Override
    protected void onCreate(Bundle saveInstanceState){

        super.onCreate(saveInstanceState);
        Log.e("BaseActivity",getClass().getSimpleName());
        ActivityCollector.addActivity(this);
        //动态注册监听锁屏/解锁的广播
//        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
//        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
//        intentFilter.addAction(Intent.ACTION_USER_PRESENT);
//        registerReceiver(receiver,intentFilter);
        Log.e("动态注册监听锁屏","ok");
    }
    @Override
    protected void onDestroy(){


        super.onDestroy();
        ActivityCollector.removeActivity(this);

//        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
//        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
//        intentFilter.addAction(Intent.ACTION_USER_PRESENT);
//        unregisterReceiver(receiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,@NonNull int[] grantResults){
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}

