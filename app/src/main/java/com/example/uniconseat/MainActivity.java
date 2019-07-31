package com.example.uniconseat;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.linsh.utilseverywhere.Utils;

import java.io.File;
import java.text.SimpleDateFormat;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {
//    public static final String TAG = "MainActivity";
    private final static int REQUEST_IGNORE_BATTERY_CODE = 1001;
    //到期时间
    static String dueTime = "2020-01-01 00:00:00";//目标时间06:25:00
    //授权管理
    String[] allPermissions = new String[]{Manifest.permission.READ_PHONE_STATE};
    public static boolean allGrant;
    static String TAG;//notification用的全局信息
    static int Id;
    static int Length;
    public boolean TipsJumpServiceStarted;
    public static boolean mainActivityStart = false;
    public static boolean update;

    //全局变量
    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
    private boolean login;//是否注册
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mainActivityStart = true;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //检查更新
        SharedPreferences pref1 = getSharedPreferences("checkUpdate",MODE_PRIVATE);
        update = pref1.getBoolean(CommonFunction.systemDelayTime(0,2),false);
        if (!update){
            Editor editor1=pref1.edit();
            editor1.putBoolean(CommonFunction.systemDelayTime(0,2), true);
            editor1.commit();
            Utils.init(getApplicationContext());
            checkUpdate.upgradeApk(getApplicationContext());
        }

        //启动日志记录
        Intent intentLogService = new Intent(getApplicationContext(),LogService.class);
        startService(intentLogService);

//        Intent intentTiming= new Intent(MainActivity.this,TimingTasks.class);
//        startService(intentTiming);

//        Intent intentG= new Intent(MainActivity.this,GuardianService.class);
//        startService(intentG);

//        Intent intentT= new Intent(MainActivity.this,TipsJumpService.class);
//        startService(intentT);


        /* app模板自带程序 */
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        //软件到期检查
        String systemTime = CommonFunction.systemTime();
        if (CommonFunction.timeCompare(systemTime,dueTime) != 3){//
            ToastCustom.passValue(3000,1000,2,0,100);
            ToastCustom.getInstance(MainActivity.this).show("软件使用权已到期，感谢您的使用！", 3000);
            Log.e("dueTime","到期提醒");
            System.exit(0);
        }

        //授权检查
        checkAuthorize();

        //是否开启桥接服务
        SharedPreferences pref = getSharedPreferences("TipsJumpsService",MODE_PRIVATE);
        TipsJumpServiceStarted = pref.getBoolean("TipsJumpsService",false);

        /*是否注册*/
        SharedPreferences loginPreferences = this.getSharedPreferences("login",MODE_PRIVATE);
        login = loginPreferences.getBoolean("login", false);//记得修改为false
        //login = loginPreferences.getBoolean("login", true);//记得修改为false

        //是否首次启动
        SharedPreferences sharedPreferences = this.getSharedPreferences("share",MODE_PRIVATE);
        boolean isFirstRun=sharedPreferences.getBoolean("isFirstRun", true);
        Editor editor=sharedPreferences.edit();
        if(isFirstRun){
            ToastCustom.passValue(2000,1000,2,0,100);
            ToastCustom.getInstance(getApplicationContext()).show("世界如此美丽，感谢相遇！", 2000);
            editor.putBoolean("isFirstRun", false);
            editor.commit();
        }else{
           if(!login){
                Intent intentLogin = new Intent(MainActivity.this,LoginActivity.class);
                startActivity(intentLogin);
           }else {
                Toast.makeText(getApplicationContext(),"欢迎使用联创座位系统",Toast.LENGTH_SHORT).show();
           }
        }

        //悬浮按钮点击事件
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "祝您生活愉快，学业有成！", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        //滑动菜单栏
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        //重新定义个性化内容
        SharedPreferences sharedPerson = getSharedPreferences("CustomSignature",MODE_PRIVATE);
        String nickname = sharedPerson.getString("nickname","Manuel Nathaniel");
        String motto = sharedPerson.getString("motto","厚德 博学 载物");
        String persign = sharedPerson.getString("per_sign","天行健，君子以自强不息；地势坤，君子以厚德载物。");

        View nav_header = getLayoutInflater().inflate(R.layout.nav_header_main, navigationView);
        TextView textNickname = nav_header.findViewById(R.id.headerTitle);
        TextView textMotto = nav_header.findViewById(R.id.textView);
        TextView textPersign = nav_header.findViewById(R.id.personal_signature);
        textNickname.setText(nickname);
        textMotto.setText(motto);
        textPersign.setText(persign);

        homeCard();

    }
    @Override
    public void onResume(){
        checkAuthorize();
        Log.e("onResume","checking");
        super.onResume();
    }
    @Override
    public void onRestart(){
        checkAuthorize();
        Log.e("onRestart","checking");
        super.onRestart();
    }
    @Override
    protected void onDestroy(){
        mainActivityStart = false;
        super.onDestroy();
    }
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //处理此处的操作栏项，只要在AndroidManifest.xml中指定父活动，操作栏将自动处理单击Home/Up按钮的操作。
        int id = item.getItemId();
        switch (id){
            case R.id.action_settings:
                settingDialog();
                break;
            case R.id.action_login:
                if (login){
                    Toast toast = new Toast(getApplicationContext());
                    View layout = LayoutInflater.from(getApplicationContext()).inflate(R.layout.toast, null, false);
                    TextView toast_message = (TextView) layout.findViewById(R.id.message);
                    toast_message.setText("您已完成激活，有效期至2020年01月01日 00:00，请放心使用");
                    toast.setGravity(Gravity.CENTER,0,0);
                    toast.setDuration(Toast.LENGTH_LONG);
                    toast.setView(layout);
                    toast.show();
                }else {
                    Intent intentLogin = new Intent(MainActivity.this,LoginActivity.class);
                    startActivity(intentLogin);
                }
                break;
            case R.id.battery:
                if (Build.VERSION.SDK_INT >= 23) {
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
                            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                            startActivity(intent);
                            ToastCustom.passValue(2000,1000,2,0,100);
                            ToastCustom.getInstance(getApplicationContext()).show("您已开启该权限", 2000);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else {
                    Intent intentSetting =  new Intent(Settings.ACTION_SETTINGS);
                    startActivity(intentSetting);
                }
                break;
            case R.id.act_overDisplay:
                if (Build.VERSION.SDK_INT >= 23) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    startActivity(intent);
                }else {
                    ToastCustom.passValue(2000,1000,2,0,100);
                    ToastCustom.getInstance(MainActivity.this).show("您当前系统不支持该设置", 2000);
                }
                break;
            case R.id.action_help:
                Intent intenthelp = new Intent(MainActivity.this,ScrollingActivity.class);
                startActivity(intenthelp);
                break;
            case R.id.action_feedback:
                feedbackDialog("反馈","是否将错误代码反馈，请选择前往方式，或点击空白处取消");
                break;
                default:
                    break;
        }
        return super.onOptionsItemSelected(item);
    }
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id){
            case R.id.nav_home://个人中心
                if (login){
                    Intent intentMySelf = new Intent(MainActivity.this,MySelfActivity.class);
                    startActivity(intentMySelf);
                    finish();
                    //Toast.makeText(MainActivity.this,"对不起，您暂时无权访问该内容！",Toast.LENGTH_LONG).show();
                }else {
                    loginDialog();
                }
                break;
            case R.id.nav_myresv://我的预约
                if (login){
                    Intent intentMyresv = new Intent(MainActivity.this,MyReserveActivity.class);
                    startActivity(intentMyresv);
                    //Toast.makeText(MainActivity.this,"对不起，您暂时无权访问该内容！",Toast.LENGTH_LONG).show();
                }else {
                    loginDialog();
                }
                break;
            case R.id.nav_gallery://定时任务
                if (login){
                    Intent intentTiming = new Intent(MainActivity.this,TimingTasksActivity.class);
                    startActivity(intentTiming);
                }else {
                    loginDialog();
                }
                break;
            case R.id.nav_slideshow://重新预约
                if (login){
                    Intent intentleave = new Intent(MainActivity.this,HomeActivity.class);
                    startActivity(intentleave);
                }else { loginDialog(); }
                break;
            case R.id.nav_tools://快速抢座
                if (login){
                    Intent intentgrabseat = new Intent(MainActivity.this,GrabSeatActivity.class);
                    startActivity(intentgrabseat);
                }else {
                    loginDialog();
                }
                break;
            case R.id.nav_share://座位查询
                if (login){
                    Intent intentPrivacy = new Intent(getApplicationContext(),PrivacyActivity.class);
                    startActivity(intentPrivacy);
                    //Toast.makeText(MainActivity.this,"对不起，您暂时无权访问该内容！",Toast.LENGTH_LONG).show();
                }else {
                    loginDialog();
                }
                break;
            case R.id.nav_transfer:
                if (login){
                    Intent intentTransfer = new Intent(getApplicationContext(),SeatTransferActivity.class);
                    startActivity(intentTransfer);
                }else {
                    loginDialog();
                }
                break;
            case R.id.nav_send://应用详情
                if (login){
                    openAppDetails("您可前往应用详情页面，了解本应用相关内容，并对部分内容进行权限管理");
                    //ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.REQUEST_INSTALL_PACKAGES}, 1);
                }else {
                    loginDialog();
                }
                break;
                default:
                    break;
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void homeCard(){
        Button function1 = findViewById(R.id.fun_button1);
        Button function2 = findViewById(R.id.fun_button2);
        Button function3 = findViewById(R.id.fun_button3);
        Button function5 = findViewById(R.id.fun_button5);
        Button function6 = findViewById(R.id.fun_button6);
        Button function7 = findViewById(R.id.fun_button7);
        Button function8 = findViewById(R.id.fun_button8);
        Button function9 = findViewById(R.id.fun_button9);
        Button function10 = findViewById(R.id.fun_button10);
        Button function11 = findViewById(R.id.fun_button11);
        Button function12 = findViewById(R.id.fun_button12);

        function1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (login){
                    Intent intentMySelf = new Intent(MainActivity.this,MySelfActivity.class);
                    startActivity(intentMySelf);
                    finish();
                    //Toast.makeText(MainActivity.this,"对不起，您暂时无权访问该内容！",Toast.LENGTH_LONG).show();
                }else {
                    loginDialog();
                }
            }
        });
        function2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (login){
                    Intent intentMyresv = new Intent(MainActivity.this,MyReserveActivity.class);
                    startActivity(intentMyresv);
                    //Toast.makeText(MainActivity.this,"对不起，您暂时无权访问该内容！",Toast.LENGTH_LONG).show();
                }else {
                    loginDialog();
                }
            }
        });
        function3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),"点击左上角或从左边缘滑动出菜单栏",Toast.LENGTH_LONG).show();
            }
        });
        function5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (login){
                    Toast toast = new Toast(getApplicationContext());
                    View layout = LayoutInflater.from(getApplicationContext()).inflate(R.layout.toast, null, false);
                    TextView toast_message = (TextView) layout.findViewById(R.id.message);
                    toast_message.setText("您已完成激活，有效期至2020年01月01日 00:00，请放心使用");
                    toast.setGravity(Gravity.CENTER,0,0);
                    toast.setDuration(Toast.LENGTH_LONG);
                    toast.setView(layout);
                    toast.show();
                }else {
                    Intent intentLogin = new Intent(MainActivity.this,LoginActivity.class);
                    startActivity(intentLogin);
                }
            }
        });
        function6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settingDialog();
            }
        });
        function7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 23) {
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
                            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                            startActivity(intent);
                            ToastCustom.passValue(2000,1000,2,0,100);
                            ToastCustom.getInstance(getApplicationContext()).show("您已开启该权限", 2000);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else {
                    Intent intentSetting =  new Intent(Settings.ACTION_SETTINGS);
                    startActivity(intentSetting);
                }
            }
        });
        function8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 23) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    startActivity(intent);
                }else {
                    ToastCustom.passValue(2000,1000,2,0,100);
                    ToastCustom.getInstance(MainActivity.this).show("您当前系统不支持该设置", 2000);
                }
            }
        });
        function9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intenthelp = new Intent(MainActivity.this,ScrollingActivity.class);
                startActivity(intenthelp);
            }
        });
        function10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                feedbackDialog("反馈","是否将错误代码反馈，请选择前往方式，或点击空白处取消");
            }
        });
        function11.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),"检查更新",Toast.LENGTH_SHORT).show();
                Utils.init(getApplicationContext());
                checkUpdate.upgradeApk(getApplicationContext());
            }
        });
        function12.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Uri photoURI = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", createImageFile());
                checkUpdate.init();
                Log.e("checkUpdate-init","1");
                File file = new File(checkUpdate.getUpdatePath("README.txt"));
                Log.e("file","1");
                File parentFlie = new File(file.getParent());
//                Log.e("parentfile","1");
//                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//                intent.setDataAndType(Uri.fromFile(parentFlie), "file/*");
//                //intent.addCategory(Intent.CATEGORY_OPENABLE );
//                intent.addCategory(Intent.CATEGORY_DEFAULT);
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(intent);
//                Log.e("startActivity","1");

                parentFlie = file;
                int length = parentFlie.getPath().length();
                Uri uri = Uri.fromFile(new File(parentFlie.getPath().substring(1,length)));
                Intent installIntent = new Intent(Intent.ACTION_VIEW);
                installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    installIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    uri = FileProvider.getUriForFile(getApplicationContext(), getApplication().getPackageName() + ".provider",parentFlie);
                    installIntent.setDataAndType(uri, "*/*");
                } else {
                    installIntent.setDataAndType(uri, "*/*");
                }
                startActivity(installIntent);

            }
        });
    }

    //授权检查
    public void checkAuthorize(){
        SharedPreferences sharedAuthorize = getSharedPreferences("AuthorizationCenter",MODE_PRIVATE);
        SharedPreferences.Editor authorzieEditor = sharedAuthorize.edit();

        //在其他应用之上显示
        if (Build.VERSION.SDK_INT >= 23) {
            if(!Settings.canDrawOverlays(this)) {
                AuthorizationCenter.overDisplay = false;
                authorzieEditor.putBoolean("overDisplay",false);
                authorzieEditor.commit();
            }else {
                AuthorizationCenter.overDisplay = true;
                authorzieEditor.putBoolean("overDisplay",true);
                authorzieEditor.commit();
            }
        }else {
            AuthorizationCenter.overDisplay = true;
            authorzieEditor.putBoolean("overDisplay",true);
            authorzieEditor.commit();
        }
        //读取手机身份状态
        if (Build.VERSION.SDK_INT >= 23){
            for (String allPermission : allPermissions) {
                if (ActivityCompat.checkSelfPermission(this, allPermission) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(allPermission,allPermission);
                    AuthorizationCenter.phoneState = false;
                    authorzieEditor.putBoolean("phoneState",false);
                    authorzieEditor.commit();
                }else {
                    AuthorizationCenter.phoneState = true;
                    authorzieEditor.putBoolean("phoneState",true);
                    authorzieEditor.commit();
                }
            }
        }else {
            AuthorizationCenter.phoneState = true;
            authorzieEditor.putBoolean("phoneState",true);
            authorzieEditor.commit();
        }
//        if (!AuthorizationCenter.phoneState){
//            AuthorizationCenter.overDisplay = sharedAuthorize.getBoolean("overDisplay",false);
//        }
//        if (!AuthorizationCenter.phoneState){
//            AuthorizationCenter.phoneState = sharedAuthorize.getBoolean("phoneState",false);
//        }
        //结果分析
        if (!AuthorizationCenter.phoneState | !AuthorizationCenter.overDisplay){
            Intent intentAuthorize = new Intent(getApplicationContext(),AuthorizationCenter.class);
            startActivity(intentAuthorize);
            allGrant = false;
            authorzieEditor.putBoolean("allGrant",false);
            authorzieEditor.commit();
        }else {
            allGrant =true;
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,@NonNull int[] grantResults){
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
        }else {
            Intent intentLogin = new Intent(MainActivity.this,LoginActivity.class);
            startActivity(intentLogin);
            Log.e("已授权","正常使用");
        }
    }
    @TargetApi(Build.VERSION_CODES.M)
    public static void isIgnoreBatteryOption(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent();
                String packageName = activity.getPackageName();
                PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    intent.setData(Uri.parse("package:" + packageName));
                    activity.startActivityForResult(intent, REQUEST_IGNORE_BATTERY_CODE);
                    Toast.makeText(activity, "请您开启忽略电池优化~", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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
    //setting Dialog
    public void settingDialog(){
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(MainActivity.this,R.style.dialog_style);
        builder.setTitle("设置");
        builder.setMessage("为保证您的流畅使用和及时得到提醒，您需要在设置->权限管理中手动开启为该应用开启自启动、后台运行、通知栏、悬浮窗等权限，是否前往？");
        builder.setCancelable(false);
        builder.setIcon(R.drawable.setting);
        builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intentSetting =  new Intent(Settings.ACTION_SETTINGS);
                startActivity(intentSetting);
            }
        });
        builder.setNegativeButton("否", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ToastCustom.passValue(3000,1000,2,0,100);
                ToastCustom.getInstance(getApplicationContext()).show("上述权限需要手动开启，如您未予授权，可能造成无法使用", 3000);
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
    //feedback Dialog
    public void feedbackDialog(String Title,String msg){
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(MainActivity.this,R.style.dialog_style);
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
                    ToastCustom.getInstance(MainActivity.this).show("您尚未安装QQ，请前往下载或使用其他方式", 2000);
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
                    ToastCustom.getInstance(MainActivity.this).show("您尚未安装TIM，请前往下载或使用其他方式", 2000);
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
                    ToastCustom.getInstance(MainActivity.this).show("您尚未安装TIM，请前往下载或使用其他方式", 2000);
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
    //显示提示框Dialog
    public void loginDialog(){
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(MainActivity.this,R.style.dialog_style);
        builder.setTitle("激活提示");
        builder.setMessage("您还未激活，是否前往激活？  选择“否”将退出程序");
        builder.setCancelable(false);
        builder.setIcon(R.drawable.notify5);
        builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intentLogin = new Intent(MainActivity.this,LoginActivity.class);
                startActivity(intentLogin);
            }
        });
        builder.setNegativeButton("否", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                System.exit(0);
                android.os.Process.killProcess(android.os.Process.myPid());
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
    //到期检查Dialog
    public void dueDialog(){
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(MainActivity.this,R.style.dialog_style);
        builder.setTitle("软件到期提示");
        builder.setMessage("您的软件已到期，是否要重新激活，选择“否”将退出程序");
        builder.setCancelable(false);
        builder.setIcon(R.drawable.notify5);
        builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                feedbackDialog("重新激活","具体事宜请前往联系");
            }
        });
        builder.setNegativeButton("否", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                System.exit(0);
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });
        android.support.v7.app.AlertDialog dialog=builder.create();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.getWindow().setType((WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY));
        }else {
            dialog.getWindow().setType((WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));
        }
        dialog.show();

    }    //通知
    public void showNotification(String title, String text, Context context, int id,String channelid,String channelname){
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(channelid, channelname, NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(mChannel);
            notification = new Notification.Builder(context)
                    .setChannelId(channelid)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.heart)
                    .setAutoCancel(true)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.guardian1round))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,MainActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .build();
        }else {
            notification = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setColor(Color.parseColor("#00000000"))
                    .setAutoCancel(true)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setSmallIcon(R.drawable.heart)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.guardian1round))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,MainActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .build();
        }
        manager.notify(id,notification);
    }
    public void showNotification0(String title, String text, Context context, int id,int length, String channelid,String channelname){
        final NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //悬停
        Notification notification2;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(channelid, channelname, NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(mChannel);
            notification2 = new Notification.Builder(context)
                    .setChannelId(channelid)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setColor(Color.parseColor("#00000000"))
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.timingtasknotigy)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.seat))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .build();
        }else {
            notification2 = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setColor(Color.parseColor("#00000000"))
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.timingtasknotigy)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.seat))
                    .setFullScreenIntent(PendingIntent.getActivities(context,0,
                            new Intent[]{new Intent(context,GrabSeatActivity.class)},
                            PendingIntent.FLAG_CANCEL_CURRENT),false)
                    .build();
        }
        String tag = id + "float";
        manager.notify(tag,id,notification2);
        TAG = tag; Id = id; Length = length;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try { Thread.sleep(Length * 100);//Length秒后悬挂式通知消失
                    manager.cancel(TAG, Id);//按tag id 来清除消息
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    //版本检查


}
