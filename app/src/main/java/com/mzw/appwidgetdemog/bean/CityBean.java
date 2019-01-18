package com.mzw.appwidgetdemog.bean;

import android.text.TextUtils;
import android.util.Log;

import com.mzw.appwidgetdemog.db.annotaions.DBField;
import com.mzw.appwidgetdemog.db.annotaions.DBTable;

/**
 * 天气 使用  放弃了
 * Created by think on 2018/12/9.
 */
@DBTable("tb_city")
public class CityBean {

//    @DBField("_id")
//    public String id;
//
//    @DBField("_city_name")
//    public String cityName; //名称
//
//    @DBField("_parent_id")
//    public String parentId; //父级id
//
//    @DBField("_level")
//    public String level; //等级   1省 2市 3县
//
    public CityBean() {
    }
//
//    public CityBean(String id, String cityName, String parentId,String level) {
//        this.id = id;
//        this.cityName = cityName; //getCityName(cityName);
//        this.parentId = parentId;
//        this.level = level;
//    }

//    //因为 天气网 地区都是简称，所以要插入%  GPS定位在百度取地区名称   模糊查询
//    private String getCityName(String cityName) {
//        StringBuffer sb = new StringBuffer();
//        sb.append("%");
//        if(!TextUtils.isEmpty(cityName)){
//            for(int i = 0; i < cityName.length(); i++){
//                sb.append(cityName.substring(i,i+1));
//                sb.append("%");
//            }
//        }
//        Log.i("---mzw---","getCityName:" + sb.toString().trim());
//        return sb.toString().trim();
//    }










    @DBField("_a")
    public String a;//省

    @DBField("_b")
    public String b;//市

    @DBField("_c")
    public String c;//县


    public CityBean(String a, String b, String c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }
}