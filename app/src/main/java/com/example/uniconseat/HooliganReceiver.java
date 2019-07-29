package com.example.uniconseat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class HooliganReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("HooliganReceiver","creat");
        if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            Intent intentHooligan = new Intent(context,HooliganActivity.class);
            intentHooligan.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intentHooligan);
            Log.e("HooliganReceiver","ACTION_SCREEN_OFF");
        } else if(intent.getAction().equals(Intent.ACTION_SCREEN_ON) | intent.getAction().equals(Intent.ACTION_USER_PRESENT)){
            HooliganActivity. killHooligan();
            Log.e("HooliganReceiver","ACTION_SCREEN_ON");
        }
    }
}
