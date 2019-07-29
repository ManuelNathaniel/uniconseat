package com.example.uniconseat;

import android.Manifest;
import android.app.Activity;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TimePicker;

import org.json.JSONObject;

import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.TELEPHONY_SERVICE;

public class CommonFunction {
    static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static int hour, minute;

    //时间选择器
    public static String showTimePickerDialog(Activity activity, Calendar calendar) {
        // Calendar c = Calendar.getInstance();
        // 创建一个TimePickerDialog实例，并把它显示出来
        // 解释一哈，Activity是context的子类
        final String[] s = new String[1];
        new TimePickerDialog(activity, android.R.style.Theme_DeviceDefault_Light_Dialog,
                // 绑定监听器
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minuteOfH) {
                        s[0] = hourOfDay + ":" + minuteOfH;
                    }
                }
                // 设置初始时间
                , calendar.get(Calendar.HOUR_OF_DAY)
                , calendar.get(Calendar.MINUTE)
                // true表示采用24小时制
                , true).show();
        return s[0];
    }

    //显示提示框Dialog
    public static void showDialog(String title, String msg, Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.dialog_style);
        builder.setIcon(R.drawable.tec3);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("我知道了",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        android.support.v7.app.AlertDialog dialog = builder.create();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.getWindow().setType((WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY));
        } else {
            dialog.getWindow().setType((WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));
        }
        dialog.show();
    }

    /** 字符匹配 **/

    //正则表达式匹配字符
    public static String getMatcher(String regex, String source) {
        String result = "";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(source);
        if (matcher.find()) {
            result = matcher.group(1);
        } else {
            result = "匹配错误";
        }
        return result;
    }

    //正则表达式返回false/true
    public static boolean regexMatcher(String regex, String source) {
        String result = "";
        boolean succeed = false;
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(source);
        if (matcher.find()) {
            result = matcher.group(1);
            succeed = true;
        } else {
            result = "匹配错误";
            succeed = false;
        }
        return succeed;
    }

    /** 获取时间信息 **/

    //系统时间字符串格式
    public static String systemTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        String nowTime = df.format(new Date());// new Date()为获取当前系统时间
        return nowTime;
    }

    //系统时间延时x毫秒    delay 单位ms index 表示要返回的类型 1-年月日时分秒  2-年月日  3-时分秒
    public static String systemDelayTime(int delay, int index) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateAndTime = df.format(new Date().getTime() + delay);// 当前时间加40分钟,该时间格式为2019-06-01 18:25
        String date = dateAndTime.substring(0, 10);//日期2019-06-01
        String time = dateAndTime.substring(11, 16);//预约开始时间
        String result = "";
        switch (index) {
            case 1:
                result = dateAndTime;
                break;
            case 2:
                result = date;
                break;
            case 3:
                result = time;
            default:
                break;
        }
        return result;
    }

    //系统时间Date格式
    public static Date systemTimeDate() {
//        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date nowTime = new Date(System.currentTimeMillis());
        return nowTime;
    }

    //时间比较  1 结束时间小于开始时间 2 开始时间与结束时间相同 3 结束时间大于开始时间
    // 1 超过结束时间 2 刚好到结束时间  3 未到结束时间
    public static int timeCompare(String startTime, String endTime) {
        int i = 0;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try {
            Date date1 = dateFormat.parse(startTime);//开始时间
            Date date2 = dateFormat.parse(endTime);//结束时间
            //
            if (date2.getTime() < date1.getTime()) {//结束时间小于开始时间
                i = 1;
            } else if (date2.getTime() == date1.getTime()) { //结束时间与开始时间相同
                i = 2;
            } else if (date2.getTime() > date1.getTime()) {//结束时间大于开始时间
                i = 3;
            }
        } catch (Exception e) {

        }
        return i;
    }

    //获取网络时间
    public static Date getNetTime(String targetUrl) throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        Date webDate;
        try {
            URL url = new URL(targetUrl);
            URLConnection uc = url.openConnection();
            uc.setReadTimeout(5000);
            uc.setConnectTimeout(5000);
            uc.connect();
            long correctTime = uc.getDate();
            webDate = new Date(correctTime);
            Log.e("网络时间",df.format(webDate));
        }catch (Exception e){
            webDate = df.parse("1970-07-01 01:00:00");
            Log.e("尝试获取网络时间出错",e.toString());
        }
        return webDate;
    }
    //计算系统和网络的时间差
    public static String diffSysAndNet(String targetUrl) throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        Date webDate;
        String strDiff;
        long second;
        try {
            URL url = new URL(targetUrl);
            URLConnection uc = url.openConnection();
            uc.setReadTimeout(5000);
            uc.setConnectTimeout(5000);
            uc.connect();
            long correctTime = uc.getDate();
            webDate = new Date(correctTime);
            Date sysDate = systemTimeDate();
            long diff = webDate.getTime()-sysDate.getTime();
            strDiff = CommonFunction.longToString(diff,"yyyy-MM-dd HH:mm:ss");
            strDiff = "s" + strDiff;//通过解析第一个字母判断，获取的时间差是哪种情况下获得的
            second = diff/1000;
            Log.e("网络时间",df.format(webDate));
            Log.e("系统时间",df.format(sysDate));
            Log.e("时间差ms",String.valueOf(diff));
            Log.e("时间差s",String.valueOf(second));
            Log.e("毫秒网络sys",String.valueOf(sysDate.getTime()));
            Log.e("毫秒网络web",String.valueOf(webDate.getTime()));
            Log.e("时间差",strDiff);
        }catch (Exception e){
            long diff = 0;
            strDiff = CommonFunction.longToString(diff,"yyyy-MM-dd HH:mm:ss");
            strDiff = "f" + strDiff;
            Log.e("尝试获取网络时间出错",e.toString());
            Log.e("时间差",strDiff);
        }
        return strDiff;
    }


    //解析出某个属性下的数据
    public static String parseSingleLabel(String inputdata, String label) {
        try {
            JSONObject jsonObject = new JSONObject(inputdata);
            label = jsonObject.getString(label);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return label;
    }

    /** 设备信息获取 **/
    //获取IMEI号用来生成序列号为软件在该设备上进行注册
    public static String getSecretkey(String serialIMEI) {
        String letter = "AuwCeEHF7lpqGdIv9GfiKh5g6kL4B1zM1gN-OnZ0bPr2QxRS3tTs8UVaWmoXDYcy";//随机字母串63个
        String secreKey = "R";//特有字符
        String strOfIMEI;//源于IMEI码中的一个字符
        String strOfLetter;//源于随机字母串中的一个字符

        // 随机决定当前位置是字母还是数字2 9 6 5 13   7 4 12  10 9 3 8
        //激活码构成：读取设备的IMEI号，根据下述计算过程进行转换
        //转换过程：激活码长度为19位，1为特有字母 + 18位生成字符。其中第一位是特有字符，为R。以后的18位，根据从

        int[] randnum = new int[18];
        randnum[0] = 24;
        randnum[1] = 18;
        randnum[2] = 21;
        randnum[3] = 5;
        randnum[4] = 13;
        randnum[5] = 7;
        randnum[6] = 4;
        randnum[7] = 12;
        randnum[8] = 10;
        randnum[9] = 15;
        randnum[10] = 3;
        randnum[11] = 22;
        randnum[12] = 6;
        randnum[13] = 8;
        randnum[14] = 9;
        randnum[15] = 20;
        randnum[16] = 25;
        randnum[17] = 2;
        //序号为双数该位置为数字，序号为单数该位置为字母
        int order = 0;//从IMEI中提取到的数字，用来判断第i个位置是从IMEI中选取字符还是从预定义的字符串中选取字符
        for (int i = 0; i < 18; i++) {
            strOfIMEI = serialIMEI.substring(order, order + 1);
            order = Integer.parseInt(strOfIMEI);
            if (order / 2 == 0) {
                //这一位从序列号中选择
                secreKey = secreKey.concat(strOfIMEI);
            } else {
                //这一位从给定的字母中选择
                strOfLetter = letter.substring(order * 4 + randnum[i], order * 4 + randnum[i] + 1);
                secreKey = secreKey.concat(strOfLetter);
            }
        }
        return secreKey;
    }
    //获取设备SN号
    public static String getDeviceSN() {
        String serialNumber = android.os.Build.SERIAL;
        return serialNumber;
    }
    //获取设备唯一标识
    public static String getIMEI(Context context) {
        TelephonyManager TelephonyMgr = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
//            return TODO;
        }
        String szImei = TelephonyMgr.getDeviceId();

        String m_szDevIDShort = "35" + //we make this look like a valid IMEI
                Build.BOARD.length()%10 + Build.BRAND.length()%10 +
                Build.CPU_ABI.length()%10 + Build.DEVICE.length()%10 +
                Build.DISPLAY.length()%10 + Build.HOST.length()%10 +
                Build.ID.length()%10 + Build.MANUFACTURER.length()%10 +
                Build.MODEL.length()%10 + Build.PRODUCT.length()%10 +
                Build.TAGS.length()%10 + Build.TYPE.length()%10 + Build.USER.length()%10 ; //13 digits

        String m_szAndroidID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        WifiManager wm = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        String m_szWLANMAC = wm.getConnectionInfo().getMacAddress();

        BluetoothAdapter m_BluetoothAdapter = BluetoothAdapter.getDefaultAdapter();; // Local Bluetooth adapter
        String m_szBTMAC = m_BluetoothAdapter.getAddress();

        String m_szLongID = szImei + m_szDevIDShort + m_szAndroidID+ m_szWLANMAC + m_szBTMAC;
        // compute md5
        MessageDigest m = null;
        try {
            m = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        m.update(m_szLongID.getBytes(),0,m_szLongID.length());
        // get md5 bytes
        byte p_md5Data[] = m.digest();
        // create a hex string
        String m_szUniqueID = new String();
        for (int i=0;i<p_md5Data.length;i++) {
            int b =  (0xFF & p_md5Data[i]);
            // if it is a single digit, make sure it have 0 in front (proper padding)
            if (b <= 0xF)
                m_szUniqueID+="0";
            // add number to string
            m_szUniqueID+=Integer.toHexString(b);
        }   // hex string to uppercase
        m_szUniqueID = m_szUniqueID.toUpperCase();
        return m_szUniqueID;
    }
    //生成唯一激活码
    public static String generateActivCode(String uniqueIdentifyCode, String android_id,String date){
        int[] uniqueCodeInt = byte2Int(uniqueIdentifyCode);//32 digits
        String letter = "AuwCeEHF7lpqGdIv9GfiKh5g6kL4B1zM1gN-OnZ0bPr2QxRS3tTs8UVaWmoXDYcy";//随机字母串63个
        String secreKey = "R";//特有字符
        String strOfId;//源于android_id中的一个字符
        String strOfLetter;//源于随机字母串中的一个字符
        // 随机决定当前位置是字母还是数字2 9 6 5 13   7 4 12  10 9 3 8
        //激活码构成：读取设备的IMEI号，根据下述计算过程进行转换
        //转换过程：激活码长度为19位，1为特有字母 + 18位生成字符。其中第一位是特有字符，为R。以后的18位，根据从

        int[] randnum = new int[]{24, 18, 21, 5, 13, 7, 4, 12,
                                    10, 15, 3, 22, 6, 8, 9, 20};
        int orderOfIMEI = 0;
        for (int i = 0; i < uniqueCodeInt.length; i ++){
          orderOfIMEI = uniqueCodeInt[i];
          if (orderOfIMEI/2 == 0){
              if (android_id == null){
                  int a = randnum[orderOfIMEI];
                  strOfLetter = letter.substring(a,a+1);
                  secreKey = secreKey.concat(strOfLetter);
                 // Log.e("android_idsecreKey",secreKey);
              }else {
                  strOfId =  android_id.substring(orderOfIMEI,orderOfIMEI+1);
                  secreKey = secreKey.concat(strOfId);
                  //Log.e("android_id",secreKey);
              }
          }else {
              int a = randnum[orderOfIMEI];
              strOfLetter = letter.substring(a,a+1);
              secreKey = secreKey.concat(strOfLetter);
              //Log.e("secreKey",secreKey);
          }
        }
        //0FC13C9B7BF7A294C5A124F665B416EB
//        String date = date2Int(systemTime());//开始日期码 2019-07-01 19:36:40

        //R9KHbEH WZNIL v5G5KG ZSLZ ChvdHFCbh WPWZMZ dKeeF5dbel NMNW
        //0-7
        //R9KHbEH Lv5G5KG
        //R9KHbEHv5G5KGChvdHFCbhdKeeF5dbel5
        //R9KHbEHv5G5KGChvdHFCbhdKeeF5dbel

        int length1 = date.length();
        int length2 = secreKey.length();
        // 1 + 32 +19
        //1-7 (1-5) 8-13 (6-9) 14-22 (10-15) 23-32  (16-19)
        //0 1 2 3 4 5 6 (7 8 9 10 11)
        // 12 13 14 15 16 17 (18 19 20 21)
        // 22 23 24 25 26 27 28 29 30 (31 32 33 34 35 36)
        // 37 38 39 40 41 42 43 44 45 46 47 (48 49 50 51)
        date = date2Int(date);
//        secreKey = secreKey.substring(0,7) + date.substring(0,5) +
//                secreKey.substring(7,13) + date.substring(5,9) +
//                secreKey.substring(13,22) + date.substring(9,15) +
//                secreKey.substring(22,32) + date.substring(15,19);
        return secreKey;
    }

    /** 数据类型转换 **/
    public static String bytes2String(byte[] data){
        String getString = "";
        for (byte datum : data) {
            getString += String.format("%02X", datum);
        }
        return getString;
    }
    public  static int[] byte2Int(String inputDate){
        //把输入进来的32位16进制数据转化为10进制，转化为32位10进制整形数
        //0FC13C9B7BF7A294C5A124F665B416EB
        int inLength = inputDate.length();//输入数据的长度
        int[] str2Dec = new int[inLength];//转化为的10进制整型数据
        int a;
        for (int index = 0; index < inLength; index ++ ){
            String s = inputDate.substring(index,index+1);//下标
            switch (s){
                case "A":
                    a = 10;
                    break;
                case "B":
                    a = 11;
                    break;
                case "C":
                    a = 12;
                    break;
                case "D":
                    a = 13;
                    break;
                case "E":
                    a = 14;
                    break;
                case "F":
                    a = 15;
                    break;
                    default:
                        a = Integer.parseInt(s);
                        break;
            }
            //0-Z 1-N 2-W 3-H 4-R 5-V 6-X 7-S 8-G 9-I :-M "-"-L " "-P
            //Log.e("转化后",String.valueOf(a));
            str2Dec[index] = a;
        }
        return str2Dec;
    }
    public static String date2Int(String currentTime){
        //日期转码
        //0-Z 1-N 2-W 3-H 4-R 5-V 6-X 7-S 8-G 9-I :-M "-"-L " "-P
        String result = "";
        for (int i = 0; i < currentTime.length(); i++){
            String s = currentTime.substring(i,i+1);
            String r = "";
            switch (s){
                case "0":
                    result = result.concat("Z");
                    break;
                case "1":
                    result = result.concat("N");
                    break;
                case "2":
                    result = result.concat("W");
                    break;
                case "3":
                    result = result.concat("H");
                    break;
                case "4":
                    result = result.concat("R");
                    break;
                case "5":
                    result = result.concat("V");
                    break;
                case "6":
                    result = result.concat("X");
                    break;
                case "7":
                    result = result.concat("S");
                    break;
                case "8":
                    result = result.concat("G");
                    break;
                case "9":
                    result = result.concat("I");
                    break;
                case "-":
                    result = result.concat("L");
                    break;
                case ":":
                    result = result.concat("M");
                    break;
                case " ":
                    result = result.concat("P");
                    break;
                    default:
                        result = result.concat("Z");
                        break;
            }
        }
        return result;
    }
    public static String int2Date(String catchTime){
        String result = "";
        for (int i = 0; i < catchTime.length(); i++){
            String s = catchTime.substring(i,i+1);
            switch (s){
                case "Z":
                    result = result.concat("0");
                    break;
                case "N":
                    result = result.concat("1");
                    break;
                case "W":
                    result = result.concat("2");
                    break;
                case "H":
                    result = result.concat("3");
                    break;
                case "R":
                    result = result.concat("4");
                    break;
                case "V":
                    result = result.concat("5");
                    break;
                case "X":
                    result = result.concat("6");
                    break;
                case "S":
                    result = result.concat("7");
                    break;
                case "G":
                    result = result.concat("8");
                    break;
                case "I":
                    result = result.concat("9");
                    break;
                case "L":
                    result = result.concat("-");
                    break;
                case "M":
                    result = result.concat(":");
                    break;
                case "P":
                    result = result.concat(" ");
                    break;
                default:
                    result = result.concat("Z");
                    break;
            }
        }
        return result;
    }

    /** 时间格式的转换 **/
    // formatType格式为yyyy-MM-dd HH:mm:ss//yyyy年MM月dd日 HH时mm分ss秒      data Date类型的时间
    public static String dateToString(Date data, String formatType) {
        return new SimpleDateFormat(formatType).format(data);
    }
    // currentTime要转换的long类型的时间     formatType要转换的string类型的时间格式
    public static String longToString(long currentTime, String formatType) throws ParseException {
        Date date = longToDate(currentTime, formatType); // long类型转成Date类型
        String strTime = dateToString(date, formatType); // date类型转成String
        return strTime;
    }
    // strTime要转换的string类型的时间，formatType要转换的格式yyyy-MM-dd HH:mm:ss//yyyy年MM月dd日 HH时mm分ss秒，
    // strTime的时间格式必须要与formatType的时间格式相同
    public static Date stringToDate(String strTime, String formatType) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat(formatType);
        Date date = null;
        date = formatter.parse(strTime);
        return date;
    }
    // currentTime要转换的long类型的时间     formatType要转换的时间格式yyyy-MM-dd HH:mm:ss//yyyy年MM月dd日 HH时mm分ss秒
    public static Date longToDate(long currentTime, String formatType) throws ParseException {
        Date dateOld = new Date(currentTime); // 根据long类型的毫秒数生命一个date类型的时间
        String sDateTime = dateToString(dateOld, formatType); // 把date类型的时间转换为string
        Date date = stringToDate(sDateTime, formatType); // 把String类型转换为Date类型
        return date;
    }
    // strTime要转换的String类型的时间   formatType时间格式  strTime的时间格式和formatType的时间格式必须相同
    public static long stringToLong(String strTime, String formatType) throws ParseException {
        Date date = stringToDate(strTime, formatType); // String类型转成date类型
        if (date == null) {
            return 0;
        } else {
            long currentTime = dateToLong(date); // date类型转成long类型
            return currentTime;
        }
    }
    // date要转换的date类型的时间
    public static long dateToLong(Date date) {
        return date.getTime();
    }

    /** 错误提示Dialog **/
    public void errorDialogNotCancel(String title, String msg, Context context){
        android.support.v7.app.AlertDialog.Builder builder=new android.support.v7.app.AlertDialog.Builder(context,R.style.dialog_style);
        builder.setIcon(R.drawable.error);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("知道了",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

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
