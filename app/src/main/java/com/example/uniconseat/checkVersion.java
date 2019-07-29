package com.example.uniconseat;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class checkVersion {
    /**
     * 1. 获取当前app的版本号
     * 获取版本号
     *
     * @throws PackageManager.NameNotFoundException
     */
    public static String getVersionName(Context context) throws PackageManager.NameNotFoundException {
        // 获取packagemanager的实例
        PackageManager packageManager = context.getPackageManager();
        // getPackageName()是你当前类的包名，0代表是获取版本信息
        PackageInfo packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
        String version = packInfo.versionName;
        return version;
    }

    /**
     * 2.根据版本号名称判断版本高低
     * 版本号比较
     *0代表相等，1代表version1大于version2，-1代表version1小于version2
     * @param version1
     * @param version2
     * @return
     */
    public static int compareVersion(String version1, String version2) {
        if (version1.equals(version2)) {
            return 0;
        }
        String[] version1Array = version1.split("\\.");
        String[] version2Array = version2.split("\\.");
        int index = 0;
        // 获取最小长度值
        int minLen = Math.min(version1Array.length, version2Array.length);
        int diff = 0;
        // 循环判断每位的大小
        while (index < minLen
                && (diff = Integer.parseInt(version1Array[index])
                - Integer.parseInt(version2Array[index])) == 0) {
            index++;
        }
        if (diff == 0) {
            // 如果位数不一致，比较多余位数
            for (int i = index; i < version1Array.length; i++) {
                if (Integer.parseInt(version1Array[i]) > 0) {
                    return 1;
                }
            }

            for (int i = index; i < version2Array.length; i++) {
                if (Integer.parseInt(version2Array[i]) > 0) {
                    return -1;
                }
            }
            return 0;
        } else {
            return diff > 0 ? 1 : -1;
        }
    }

    /**
     * 从服务器获取版本最新的版本信息
     */
//    private void getVersionInfoFromServer(){
//        //模拟从服务器获取信息  模拟更新王者荣耀
//        versionInfoBean = new VersionInfoBean("1.1.1","http://dlied5.myapp.com/myapp/1104466820/sgame/2017_com.tencent.tmgp.sgame_h162_1.33.1.8_9c4c7f.apk","1.修复若干bug\n\n2.新增图片编辑功能"
//                ,getExternalCacheDir()+"/1.1.1.jpg");
//        SharedPreferences sharedPreferences = getSharedPreferences("data",MODE_PRIVATE);
//        sharedPreferences.edit().putString("url",versionInfoBean.getDownloadUrl()).commit();
//        sharedPreferences.edit().putString("path",versionInfoBean.getPath()).commit();//getExternalCacheDir获取到的路径 为系统为app分配的内存 卸载app后 该目录下的资源也会删除
//        //比较版本信息
//        try {
//            int result = Utils.compareVersion(Utils.getVersionName(this),versionInfoBean.getVersionName());
//            if(result==-1){//不是最新版本
//                showDialog();
//            }else{
//                Toast.makeText(MainActivity.this,"已经是最新版本", Toast.LENGTH_SHORT).show();
//            }
//        } catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
//        }
//
//    }


}
