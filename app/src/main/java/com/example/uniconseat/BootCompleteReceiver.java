package com.example.uniconseat;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;


public class BootCompleteReceiver extends BroadcastReceiver {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        //com.linsh.utilseverywhere.Utils.init(context);
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")){
            Intent intentTipsJump = new Intent(context,TipsJumpService.class);
            intentTipsJump.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intentTipsJump);
            } else {
                context.startService(intentTipsJump);
            }
            Log.e("1号reboot已经重启成功-","---------------------");

        }else {
            //开机后启动“跳转服务”，该服务作为其他服务/活动启动的桥接点
            Intent intentTipsJump = new Intent(context,TipsJumpService.class);
            intentTipsJump.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //context.startService(intentTipsJump);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intentTipsJump);
            } else {
                context.startService(intentTipsJump);
            }
            Log.e("2号eboot已经重启成功","****************************");
        }


        Toast.makeText(context,"Boot Complete",Toast.LENGTH_LONG).show();

    }
}
