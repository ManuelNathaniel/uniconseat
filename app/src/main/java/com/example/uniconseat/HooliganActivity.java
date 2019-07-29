package com.example.uniconseat;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public class HooliganActivity extends Activity {
    private static HooliganActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        Window window = getWindow();
        window.setGravity(Gravity.START | Gravity.TOP);
        WindowManager.LayoutParams params = window.getAttributes();
        params.x = 0;
        params.y = 0;
        params.height = 1;
        params.width = 1;
        window.setAttributes(params);
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        Toast.makeText(getApplicationContext(),"点亮屏幕",Toast.LENGTH_SHORT).show();

        Log.e("HooliganActivity","onCreate");
    }

    /**
     * 开启保活页面
     */
//    public static void startHooligan() {
//        Intent intent = new Intent(BaseActivity.getAppContext(),HooliganActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);
//    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        instance = null;

        Log.e("onDestroy","ok");
    }

    /**
     * 关闭保活页面
     */
    public static void killHooligan() {
        if(instance != null) {
            instance.finish();
        }
        Log.e("killHooligan","finish");
    }
}
