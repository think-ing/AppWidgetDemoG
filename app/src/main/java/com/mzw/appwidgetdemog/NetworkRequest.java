package com.mzw.appwidgetdemog;

import android.util.Log;

import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.util.Auth;


import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import okhttp3.*;

/**
 * 网络请求
 * Created by think on 2018/11/29.
 */

public class NetworkRequest {

    //七牛云
    private static String accessKey = "lhPn7Wd99CEmkOmmO0zm0akm-Q06AJnOK7X8RICV";
    private static String secretKey = "i1PXpj1BaaTNJf3xvlPfR51A5ETCXAMbo-Fsl0nh";
    //储存空间名称
    private static String bucket = "mzw-vital";
    //域名
    private static String domainOfBucket = "http://piwcjftud.bkt.clouddn.com";


    private static UploadManager uploadManager = new UploadManager();
    private static OkHttpClient okHttpClient = new OkHttpClient();




    //获取私有 访问地址     地址是固定的， 文件名称传进来
    public static String getDownloadUrl(String fileName){
        String encodedFileName = null;
        try {
            encodedFileName = URLEncoder.encode(fileName, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String publicUrl = String.format("%s/%s", domainOfBucket, encodedFileName);

        Auth auth = Auth.create(accessKey, secretKey);
        long expireInSeconds = 10 * 60;//10分钟，可以自定义链接过期时间
        String finalUrl = auth.privateDownloadUrl(publicUrl, expireInSeconds);

        Log.i("---mzw---","finalUrl : " + finalUrl);
        return finalUrl;
    }

    //下载文件
    public static void downloadFile(String url,Callback responseCallback){
        Request request = new Request.Builder().url(url).build();
        okHttpClient.newCall(request).enqueue(responseCallback);
    }

    //上传文件
    public static void uploadingFile(File file, String key, UpCompletionHandler complete,UploadOptions options) {
        //获取凭证
        Auth auth = Auth.create(accessKey, secretKey);
        //以key 覆盖
        String upToken = auth.uploadToken(bucket,key);


        UploadManager uploadManager = new UploadManager();
        uploadManager.put(file, key, upToken,complete,options);
    }

    //获取 地区   mob
    public static void getCitys(Callback responseCallback) {
        Request request = new Request.Builder().url("http://apicloud.mob.com/v1/weather/citys?key=29309d42b2fb0").build();
        okHttpClient.newCall(request).enqueue(responseCallback);
    }
}
