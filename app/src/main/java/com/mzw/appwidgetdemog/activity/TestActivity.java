package com.mzw.appwidgetdemog.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.mzw.appwidgetdemog.R;
import com.mzw.appwidgetdemog.bean.CityBean;
import com.mzw.appwidgetdemog.db.sqlites.BaseDaoFactory;
import com.mzw.appwidgetdemog.db.sqlites.IBaseDao;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 *
 * MOB 短信验证码  ui版 测试
 * Created by think on 2018/12/1.
 */

public class TestActivity extends Activity {

    Context mContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_activity);
        mContext = this;

    }

    public void myClick(View view) {
        Log.i("---mzw---","点击...");

        String s1 = "科尔沁右翼前旗";
        String s2 = "科右前旗";
        IBaseDao<CityBean> mIBaseDao = BaseDaoFactory.getInstance().getBaseDao(CityBean.class);
        CityBean bean = new CityBean();

        mIBaseDao.query(bean);
        getLocation();

    }


    //wd 纬度
//jd 经度
    public void updateVersion(String wd, String jd) {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().url("http://api.map.baidu.com/geocoder?output=json&location=" + wd + "," + jd).build();
        okHttpClient.newCall(request).enqueue(new Callback(){

            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String _response = response.body().string();
                if(!TextUtils.isEmpty(_response) && _response.startsWith("{")) {
                    try {
                        JSONObject json = new JSONObject(_response);
                        if(!json.isNull("result")){
                            JSONObject resultJson = json.getJSONObject("result");
                            if(!resultJson.isNull("addressComponent")){
                                JSONObject addressComponentJson = new JSONObject("addressComponent");
                                if(!addressComponentJson.isNull("province")){//省
                                    String province = addressComponentJson.getString("province");
                                }
                                if(!addressComponentJson.isNull("city")){//市
                                    String city = addressComponentJson.getString("city");
                                }

                                if(!addressComponentJson.isNull("district")){//县
                                    String district = addressComponentJson.getString("district");
                                }

                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }


    public void getLocation() {
        String locationProvider;
        //获取地理位置管理器
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //获取所有可用的位置提供器
        List<String> providers = locationManager.getProviders(true);
        if (providers.contains(LocationManager.GPS_PROVIDER)) {
            //如果是GPS
            locationProvider = LocationManager.GPS_PROVIDER;
        } else if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
            //如果是Network
            locationProvider = LocationManager.NETWORK_PROVIDER;
        } else {
            Toast.makeText(this, "没有可用的位置提供器", Toast.LENGTH_SHORT).show();
            return;
        }
        //获取Location
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("---mzw---", "onCreate: 没有权限 ...");
            return;
        }


        Location location = locationManager.getLastKnownLocation(locationProvider);
        if (location != null) {
            //不为空,显示地理位置经纬度
            showLocation(location);
        }
        //监视地理位置变化
        locationManager.requestLocationUpdates(locationProvider, 3000, 1, locationListener);
    }

    /**
     * 显示地理位置经度和纬度信息
     *
     * @param location
     */
    private void showLocation(Location location) {
        String locationStr = "纬度：" + location.getLatitude() + " , 经度：" + location.getLongitude();
        Log.i("---mzw---","当前坐标：" + locationStr);
        updateVersion(location.getLatitude() + "", location.getLongitude() + "");
    }

    /**
     * LocationListern监听器
     * 参数：地理位置提供器、监听位置变化的时间间隔、位置变化的距离间隔、LocationListener监听器
     */

    LocationListener locationListener = new LocationListener() {

        @Override
        public void onStatusChanged(String provider, int status, Bundle arg2) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }

        @Override
        public void onLocationChanged(Location location) {
            //如果位置发生变化,重新显示
            showLocation(location);
        }
    };

}











