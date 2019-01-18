package com.mzw.appwidgetdemog.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 常量参数
 * Created by think on 2018/11/10.
 */

public class ConstantParameter {


    //集合名称，一个用户只能拥有5个自定义的table   特殊节日
    public static final String MOB_TABLE = "mzw_festival";
    private static final String birthday_k = "birthday";


    public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd");
    public static SimpleDateFormat sdf_a = new SimpleDateFormat("yyyy-MM-dd");
    public static SimpleDateFormat sdf_b = new SimpleDateFormat("yyyy年MM月dd日");
    // 做 比较使用  指定时间是否为今天   今月   今年
    public static SimpleDateFormat sdf_toyear = new SimpleDateFormat("yyyy");
    public static SimpleDateFormat sdf_tomonth = new SimpleDateFormat("yyyyMM");
    public static SimpleDateFormat sdf_today = new SimpleDateFormat("yyyyMMdd");

    public static SimpleDateFormat sdf_year = new SimpleDateFormat("yyyy");
    public static SimpleDateFormat sdf_month = new SimpleDateFormat("MM");
    public static SimpleDateFormat sdf_day = new SimpleDateFormat("dd");
    public static SimpleDateFormat sdf_hhmm = new SimpleDateFormat("HHmm");//做通知 id 用




    //桌面挂件点击事件  日
    public static final String itemLayout = "com.mzw.appwidgetdemob.itemLayout";
    //桌面挂件点击事件  上月
    public static final String month_previous = "com.mzw.appwidgetdemob.month_previous";
    //桌面挂件点击事件  下月
    public static final String month_next = "com.mzw.appwidgetdemob.month_next";
    //返回今天
    public static final String back_today = "com.mzw.appwidgetdemob.back_today";
    //不再发送通知提示
    public static final String BIRTHDAY_REMIND = "com.mzw.appwidgetdemob.birthday_remind";

    //挂件 样式
    public static final String WIDGET_BACKGROUND = "com.mzw.appwidgetdemob.widget_background";


    //七牛云 个人用户文件名称
    public static final String USER_INFO_KEY = "mzwUserInfo";


    public static String getBirthday_k(Context mContext) {
        return birthday_k + "_" + getUserName(mContext);
    }

    //获取登陆名称
    public static String getUserName(Context mContext) {
        SharedPreferences mSharedPreferences = mContext.getSharedPreferences(USER_INFO_KEY, Context.MODE_PRIVATE);
        return mSharedPreferences.getString("username","");
    }

    //保存登陆信息
    public static void saveUserName(Context mContext,String userName,String password) {
        SharedPreferences mSharedPreferences = mContext.getSharedPreferences(USER_INFO_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = mSharedPreferences.edit();
        edit.putString("username",userName);
        edit.putString("password",password);
        edit.commit();
    }

    //登陆校验
    public static boolean login(Context mContext,String _userName,String _password) {
        SharedPreferences mSharedPreferences = mContext.getSharedPreferences(USER_INFO_KEY, Context.MODE_PRIVATE);
        String username = mSharedPreferences.getString("username","");
        String password = mSharedPreferences.getString("password","");
        if(username.equals(_userName) && password.equals(_password)){
            return true;
        }
        return false;
    }

    //保存特殊节日
    public static void saveBirthday(Context mContext,String v) {
        SharedPreferences mSharedPreferences = mContext.getSharedPreferences(ConstantParameter.MOB_TABLE, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = mSharedPreferences.edit();
        edit.putString(ConstantParameter.getBirthday_k(mContext),v);
        edit.commit();
    }
    //获取本地特殊节日
    public static String getBirthday(Context mContext) {
        SharedPreferences mSharedPreferences = mContext.getSharedPreferences(ConstantParameter.MOB_TABLE, Context.MODE_PRIVATE);
        String v = mSharedPreferences.getString(ConstantParameter.getBirthday_k(mContext),"");
        return v;
    }
    //解析特殊节日
    public static String[] getFestivalBirthday(Context mContext) {
        String[] festivalBirthday = null;
        try {
            SharedPreferences mSharedPreferences = mContext.getSharedPreferences(ConstantParameter.MOB_TABLE, Context.MODE_PRIVATE);
            String v = mSharedPreferences.getString(ConstantParameter.getBirthday_k(mContext),"");
            if(!TextUtils.isEmpty(v) && v.startsWith("{")){
                JSONObject vJson = new JSONObject(v);
                festivalBirthday = new String[vJson.length()];
                Iterator<String> krys = vJson.keys();

                int index = 0;
                while(krys.hasNext()) {
                    String date = krys.next();
                    String name = vJson.getString(date);
                    festivalBirthday[index] = date+" "+name;
                    index++;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return festivalBirthday;
    }

    //关闭特殊节日提醒
    public static void saveBirthdayRemind(Context mContext,int i) {
        SharedPreferences mSharedPreferences = mContext.getSharedPreferences(ConstantParameter.BIRTHDAY_REMIND, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = mSharedPreferences.edit();
        edit.putInt("birthdayRemind",i);
        edit.commit();
    }
    //特殊节日提醒标识  默认提醒   -1 不提醒  0 提醒
    public static int getBirthdayRemind(Context mContext) {
        SharedPreferences mSharedPreferences = mContext.getSharedPreferences(ConstantParameter.BIRTHDAY_REMIND, Context.MODE_PRIVATE);
        return mSharedPreferences.getInt("birthdayRemind",0);
    }


    //桌面挂件 背景
    public static void saveWidgetBackground(Context mContext,String mBackgroundColor,int mProgress) {
        SharedPreferences mSharedPreferences = mContext.getSharedPreferences(ConstantParameter.WIDGET_BACKGROUND, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = mSharedPreferences.edit();
        edit.putString("mBackgroundColor",mBackgroundColor);
        edit.putInt("mProgress",mProgress);
        edit.commit();
    }

    //桌面挂件 背景
    public static Map getWidgetBackground(Context mContext) {
        Map<String,Object> map = new HashMap<String,Object>();
        SharedPreferences mSharedPreferences = mContext.getSharedPreferences(ConstantParameter.WIDGET_BACKGROUND, Context.MODE_PRIVATE);
        map.put("mBackgroundColor",mSharedPreferences.getString("mBackgroundColor","#000000"));
        map.put("mProgress",mSharedPreferences.getInt("mProgress",66));
        return map;
    }

    /*
    保存 MAP
        map24 --> 24节气
        map3 --> 3伏 3九
     */
    public static void saveMap(Context mContext,String name, Map<String,String> map) {
        Gson gson = new Gson();
        String strJson  = gson.toJson(map);
        SharedPreferences mSharedPreferences = mContext.getSharedPreferences(name, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = mSharedPreferences.edit();
        edit.putString("map",strJson);
        edit.commit();
    }
    public static Map getMap(Context mContext,String name,Map<String,String> map) {
        SharedPreferences mSharedPreferences = mContext.getSharedPreferences(name, Context.MODE_PRIVATE);

        String strJson = mSharedPreferences.getString(name,null);
        if (strJson == null){
            return map;
        }
        Gson gson = new Gson();
        map = gson.fromJson(strJson,new TypeToken<Map<String,String> >(){}.getType());
        return map;
    }


}
