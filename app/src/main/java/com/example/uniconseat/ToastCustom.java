package com.example.uniconseat;

import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


public class ToastCustom {
    private boolean canceled = true;
    private Handler handler;
    private Toast toast;
    private TimeCount time;
    private TextView toast_content;
    private static ToastCustom instance;
    private static int gravityType = 1;
    private static int countDownInterval = 1000;
    private static int delayMillis = 3000;
    private static int xOffset,yOffset;

    public static void passValue(@NonNull int passdelayMillis, @NonNull int passcountDownInterval, @NonNull int passgravityType,
                                 int passValxOffset,int passValyOffset){
        gravityType = passgravityType;
        delayMillis = passdelayMillis;
        countDownInterval = passcountDownInterval;
        xOffset = passValxOffset; yOffset = passValyOffset;
    }
    public static ToastCustom getInstance(Context context) {
        if(instance == null) {
            instance = new ToastCustom(context);
            Log.e("instance","null");
        }
        Log.e("instance","fill");
        return instance;
    }

    public ToastCustom(Context context) {
        this(context, new Handler());
    }

    public ToastCustom(Context context, Handler handler) {
        this.handler = handler;

        View layout = LayoutInflater.from(context).inflate(R.layout.toast, null, false);
        toast_content = (TextView) layout.findViewById(R.id.message);
        if (toast == null) {
            toast = new Toast(context);
        }
        if (gravityType == 1){
            toast.setGravity(Gravity.CENTER, xOffset, yOffset);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(layout);
        }else if (gravityType == 2){
            toast.setGravity(Gravity.BOTTOM,xOffset,yOffset);//相对于参考位置横向偏移和纵向偏移
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(layout);
        }else if (gravityType == 3){
            toast.setGravity(Gravity.TOP,xOffset,yOffset);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(layout);
        }else {
            toast.setGravity(Gravity.BOTTOM, xOffset, yOffset);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(layout);
        }
    }

    /**
     * @param text     要显示的内容
     * @param duration 显示的时间长
     *                 根据LENGTH_MAX进行判断
     *                 如果不匹配，进行系统显示
     *                 如果匹配，永久显示，直到调用hide()
     */
    public void show(String text, int duration) {
        time = new TimeCount(duration, countDownInterval);//1000是消失渐变时间
        toast_content.setText(text);
        if (canceled) {
            time.start();
            canceled = false;
            showUntilCancel();
        }
    }

    /**
     * 隐藏Toast
     */
    public void hide() {
        if (toast != null) {
            toast.cancel();
        }
        canceled = true;
    }

    private void showUntilCancel() {
        if (canceled) {
            return;
        }
        toast.show();
        handler.postDelayed(new Runnable() {
            public void run() {
                showUntilCancel();
            }
        }, delayMillis);//3000
    }

    /**
     * 计时器
     */
    class TimeCount extends CountDownTimer {
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval); // 总时长,计时的时间间隔
        }

        @Override
        public void onFinish() { // 计时完毕时触发
            hide();
        }

        @Override
        public void onTick(long millisUntilFinished) { // 计时过程显示
        }

    }



}
