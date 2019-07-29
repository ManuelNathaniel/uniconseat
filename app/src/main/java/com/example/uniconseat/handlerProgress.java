package com.example.uniconseat;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class handlerProgress {
    private static final int OPENPROGRESS = 10;//打开
    private static final int CLOSEPROGRESS = 0;//关闭
    private static final int SEATERROR = 1;//座位号异常
    private static final int RESPONSEREQUEST = 2;//相应请求
    private static final int GETCREDIT = 3;//获取积分信息
    private static final int ACCESSDATA = 4;//获取座位数据
    private static final int RESERVE = 5;//正在预约
    private static final int CREDITINFO = 6;//积分信息
    private static final int CHECKCOUNTONE = 7,CHECKCOUTTWO = 8,CANCELRESEV = 9, TRANSFER = 11;
    private static final int CHECKTIME = 12,GETORISEAT = 13;
    private static RelativeLayout relativeLayout;
    private static TextView progressTextView;
    private static ProgressBar progressBar;

    //handle传递消息
    public static void handlerProgress(RelativeLayout relativeLayout1,TextView textView, int id){
        relativeLayout = relativeLayout1;
        progressTextView = textView;
        Message message = new Message();
        switch (id){
            case 0:
                message.what = CLOSEPROGRESS;
                break;
            case 10:
                message.what = OPENPROGRESS;
                break;
            case 1:
                message.what = SEATERROR;
                break;
            case 2:
                message.what = RESPONSEREQUEST;
                break;
            case 3:
                message.what = GETCREDIT;
                break;
            case 4:
                message.what = ACCESSDATA;
                break;
            case 5:
                message.what = RESERVE;
                break;
            case 6:
                message.what = CREDITINFO;
                break;
            case 7:
                message.what = CHECKCOUNTONE;
                break;
            case 8:
                message.what = CHECKCOUTTWO;
                break;
            case 9:
                message.what = CANCELRESEV;
                break;
            case 11:
                message.what = TRANSFER;
                break;
            case 12:
                message.what = CHECKTIME;
                break;
            case 13:
                message.what = GETORISEAT;
                break;
            default:
                break;
        }
        handler.sendMessage(message);
    }

    //Handl处理消息
    @SuppressLint("HandlerLeak")
    private static  Handler handler = new Handler(){
        @Override
        public void  handleMessage(Message msg){
            switch (msg.what) {
                case CLOSEPROGRESS:
                    //关闭视图0
                    relativeLayout.setVisibility(View.GONE);
                    break;
                case  OPENPROGRESS:
                    //打开视图10
                    relativeLayout.setVisibility(View.VISIBLE);
                    relativeLayout.bringToFront();
                    break;
                case SEATERROR:
                    //1
                    progressTextView.setText("座位号异常");
                    break;
                case RESPONSEREQUEST:
                    //2
                    progressTextView.setText("正在响应您的请求");
                    break;
                case GETCREDIT:
                    //3
                    progressTextView.setText("获取积分信息");
                    break;
                case ACCESSDATA:
                    //4
                    progressTextView.setText("获取座位数据");
                    break;
                case RESERVE:
                    //5
                    progressTextView.setText("正在预约，请稍后");
                    break;
                case CREDITINFO:
                    //6
                    progressTextView.setText("积分满足条件");
                    break;
                case CHECKCOUNTONE:
                    //7
                    progressTextView.setText("核对账户一");
                    break;
                case CHECKCOUTTWO:
                    //8
                    progressTextView.setText("核对账户二");
                    break;
                case CANCELRESEV:
                    //9
                    progressTextView.setText("取消当前预约");
                    break;
                case TRANSFER:
                    //11
                    progressTextView.setText("正在转接，账户二正在预约");
                    break;
                case CHECKTIME:
                    //12
                    progressTextView.setText("正在核对时间信息");
                    break;
                case 13:
                    progressTextView.setText("正在获取原始座位信息");
                    break;

            }
            super.handleMessage(msg);
        }
    };
}
