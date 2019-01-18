package com.mzw.appwidgetdemog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.mzw.appwidgetdemog.activity.LoginActivity;
import com.mzw.appwidgetdemog.activity.SampleActivity;
import com.mzw.appwidgetdemog.adapter.GridAdapter;
import com.mzw.appwidgetdemog.bean.CityBean;
import com.mzw.appwidgetdemog.bean.VitalBean;
import com.mzw.appwidgetdemog.db.sqlites.BaseDaoFactory;
import com.mzw.appwidgetdemog.db.sqlites.IBaseDao;
import com.mzw.appwidgetdemog.tools.ConstantParameter;
import com.mzw.appwidgetdemog.tools.FileUtils;
import com.mzw.appwidgetdemog.tools.GanZhiJIRi;
import com.mzw.appwidgetdemog.tools.Lunar;
import com.mzw.appwidgetdemog.tools.MD5;
import com.mzw.appwidgetdemog.tools.MyDatePickerDialog;
import com.mzw.appwidgetdemog.tools.NotificationsUtils;
import com.mzw.appwidgetdemog.tools.SolarTerms24;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpCompletionHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import okio.Sink;

import static android.app.Notification.PRIORITY_DEFAULT;
import static android.app.Notification.VISIBILITY_SECRET;

/**
 * 主要技术
 * 七牛云对象存储（文件形式）  https://portal.qiniu.com/bucket/mzw-vital/index       021-2070-3921
 * mob短信验证
 * 文件创建、写入、读取、加密、解密、删除
 * 密码MD5加盐 加密
 * 阳历转阴历
 * 自定义时间选择器
 * 通知栏权限
 *
 *
 * 主要功能
 * 桌面日历挂件（可查看上下月）
 * 在挂件中  农历 阳历 特殊节日 高亮提醒。
 * 特殊节日可自定义添加（如生日，纪念日等）
 * 挂件每30分钟刷新一次
 * 挂件刷新时检测今天是否为特殊节日 是：发送通知提醒
 *
 *
 * 内部流程
 * 注册  mob手机验证 -- 七牛云下载用户信息文件 -- 解密 -- 将注册信息添加到文件 -- 加密 -- 上传 -- 注册成功
 * 登陆  七牛云下载用户信息文件 -- 解密 -- 数据校验 -- 登陆成功
 * 特殊节日添加  七牛云下载用户信息文件 -- 解密 -- 选择时间（默认忽略年） -- 添加备注（限两个字） -- 数据添加到文件 -- 加密 -- 上传到云  完成
 * 特殊节日删除  七牛云下载用户信息文件 -- 解密 -- 长按删除 -- 文件加密 -- 上传到云 完成
 */

public class MainActivity extends Activity implements View.OnClickListener {

    private Context mContext;

    private Calendar calendar = Calendar.getInstance();
    private TextView dateTextView;
    private EditText nicknameView;
    private GridView gridView;
    private LinearLayout progressBar_layout;

    private List<VitalBean> mVitalBeanList = new ArrayList<VitalBean>();
    private List<VitalBean> tempVitalBeanList = new ArrayList<VitalBean>();
    private JSONObject mJSONObject;

    private String lunarStr = "";//阴历 月日
    private String solarStr = "";//阳历 月日
    private String userName = "mzw";

    private String SDPath;//SD根目录
    private File srcFile,encFile,decFile;

    private GridAdapter mGridAdapter;

    private String qiniuKey = "";
    /**
     *输入法管理器
     */
    private InputMethodManager mInputMethodManager;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 0://网络数据改变  直接到这里
                    int arg1 = msg.arg1;
                    getBirthday(1);//网络数据改变 重新提取网络数据

                    String text = "";
                    if(arg1 == 1){
                        text = "添加成功";
                        dateTextView.setText("");
                        nicknameView.setText("");
                    }else{
                        text = "删除成功";
                    }
                    Toast.makeText(mContext,text,Toast.LENGTH_SHORT).show();
                    break;
                case 1://网络数据 直接到这里
                    try {
                        //文件解密
                        FileUtils.DecFile(encFile,decFile);
                        //读取文件json
                        String _response = FileUtils.readFile(decFile);
                        if(!TextUtils.isEmpty(_response) && _response.startsWith("{")){
                            //保存到本地
                            ConstantParameter.saveBirthday(mContext,_response.trim());
                            mJSONObject = new JSONObject(_response.trim());
                            jsontoList();
                            mHandler.sendEmptyMessage(2);
                        }else{
                            mJSONObject = null;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mHandler.sendEmptyMessage(2);
                    break;
                case 2://本地数据 直接到这里
                    tempVitalBeanList.clear();
                    tempVitalBeanList.addAll(mVitalBeanList);
                    Log.i("---mzw---","mVitalBeanList.size : " + mVitalBeanList.size());
                    //显示数据
                    if(mGridAdapter != null){
                        while (tempVitalBeanList != null && tempVitalBeanList.size() % 3 != 0){
                            tempVitalBeanList.add(new VitalBean("",""));
                        }
                        mGridAdapter.notifyDataSetChanged();
                        progressBar_layout.setVisibility(View.GONE);
                    }
                    break;
                case -1:
                    String error = (String)msg.obj;
                    Toast.makeText(mContext,TextUtils.isEmpty(error)?"操作失败请重试":error,Toast.LENGTH_SHORT).show();
                    break;
            }
            super.handleMessage(msg);
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        //获取本地用户信息
        userName = ConstantParameter.getUserName(mContext);

        //获取 sd卡路径
        SDPath = FileUtils.getSDPath(mContext);
        //清空目录中文件
        FileUtils.deleteFile(SDPath);
        if(TextUtils.isEmpty(userName)){
            // 登陆
            startActivity(new Intent(mContext,LoginActivity.class));
            this.finish();
        }

        progressBar_layout = findViewById(R.id.progressBar_layout);
        gridView = findViewById(R.id.id_gridView);
        dateTextView = findViewById(R.id.textView4);
        nicknameView = findViewById(R.id.nicknameView);

        dateTextView.setOnClickListener(this);
        findViewById(R.id.id_setting).setOnClickListener(this);
        findViewById(R.id.textView).setOnClickListener(this);
        findViewById(R.id.button).setOnClickListener(this);

        qiniuKey = "mzw"+userName+".by";
        srcFile = new File(SDPath+"/mzw"+userName+".mzw"); //初始文件
        encFile = new File(SDPath+"/mzw"+userName+".by"); //加密文件
        decFile = new File(SDPath+"/mzw"+userName+".mzw"); //解密文件

        mGridAdapter = new GridAdapter(tempVitalBeanList, mContext, getLayoutInflater());
        gridView.setAdapter(mGridAdapter);
        gridView.setOnItemLongClickListener(mOnItemLongClickListener);

        mInputMethodManager=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        nicknameView.addTextChangedListener(textWatcher);


        getBirthday(0);
        checkSettings();
    }


    /**
     * 获取数据
     * 本地查找  null  网络下载
     * 到七牛云下载加密文件
     * 文件解密
     * 读取文件json
     * 保存到本地
     * 转为list
     *
     * sign 是否去网络 取数据（本地有就不去，新增或删除要去）
     */
    private void getBirthday(int sign) {
        progressBar_layout.setVisibility(View.VISIBLE);
        mVitalBeanList.clear();
        if(sign == 0){
            //查找本地
            String v = ConstantParameter.getBirthday(mContext);
            if(!TextUtils.isEmpty(v) && v.startsWith("{")){
                try {
                    mJSONObject = new JSONObject(v.trim());
                    jsontoList();
                    mHandler.sendEmptyMessage(2);
                    return;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        String fileUrl = NetworkRequest.getDownloadUrl(qiniuKey);
        //到七牛云下载加密文件
        NetworkRequest.downloadFile(fileUrl,new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 下载失败
                e.printStackTrace();
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
//                Log.i("---mzw---","response : " + response);
                Sink sink = null;
                BufferedSink bufferedSink = null;
                try {
//                    File dest = new File(SDPath,qiniuKey);
                    sink = Okio.sink(encFile);
                    bufferedSink = Okio.buffer(sink);
                    bufferedSink.writeAll(response.body().source());

                    bufferedSink.close();
                    mHandler.sendEmptyMessage(1);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if(bufferedSink != null){
                        bufferedSink.close();
                    }
                }
            }
        });
    }

    /**
     * 上传文件
     * 转为json
     * 写入文档文件
     * 文件加密
     * 上传到七牛云
     * 删除原文件和加密文件
     * @param arg1
     */
    private void putBirthday(int arg1){
//        progressBar_layout.setVisibility(View.VISIBLE);
        final int finalArg = arg1;
        try {
            //写入文档文件
            FileUtils.writeFile(srcFile,mJSONObject.toString());
//            String str = "{\"0320\":\"她爸\",\"0806\":\"老爸\",\"0808\":\"媳妇\",\"0815\":\"阿斌\",\"0824\":\"哥哥\",\"0915\":\"侄子\",\"1006\":\"她妈\",\"1018\":\"她儿\",\"1126\":\"老妈\",\"1219\":\"\uD83D\uDC97\"}";
//            FileUtils.writeFile(srcFile,str);
            //文件加密
            FileUtils.EncFile(srcFile,encFile);
            //将加密文件上传到七牛云
            NetworkRequest.uploadingFile(encFile,qiniuKey,
                    new UpCompletionHandler() {
                        @Override
                        public void complete(String key, ResponseInfo info, JSONObject response) {
                            Log.i("---mzw---","    info : " + info);
                            Log.i("---mzw---","response : " + response);
                            //官方没有 明确 错误代码  不好判断  就直接返回成功
                            Message msg = new Message();
                            String error = "";
                            if(response != null){
                                if(response.isNull("error")){
                                    //上传成功
                                    //删除原文件和加密文件
//                                    srcFile.delete();
//                                    encFile.delete();

                                    msg.what = 0;
                                    msg.arg1 = finalArg;
                                    mHandler.sendMessage(msg);
                                    return;
                                }else{
                                    try {
                                        error = response.getString("error");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            Log.i("---mzw---", "上传失败...");
                            msg.what = -1;
                            msg.obj = error;
                            mHandler.sendMessage(msg);
                        }
                    }, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void jsontoList() throws JSONException{
        if(mJSONObject != null){
            Iterator<String> krys = mJSONObject.keys();
            //转为list
            while(krys.hasNext()) {
                String date = krys.next();
                String name = mJSONObject.getString(date);
                mVitalBeanList.add(new VitalBean(date,name));
            }
        }
        Collections.sort(mVitalBeanList,new Comparator<VitalBean>(){
            public int compare(VitalBean arg0, VitalBean arg1) {
                return arg0.date.compareTo(arg1.date);
            }
        });
    }
    private void toJson() {//封装成 json 存储
        mJSONObject = new JSONObject();
        Collections.sort(mVitalBeanList,new Comparator<VitalBean>(){
            public int compare(VitalBean arg0, VitalBean arg1) {
                return arg0.date.compareTo(arg1.date);
            }
        });
        for(int i = 0; i < mVitalBeanList.size(); i++) {
            VitalBean mVitalBean = mVitalBeanList.get(i);
            try {
                if(!TextUtils.isEmpty(mVitalBean.date)){
                    mJSONObject.put(mVitalBean.date, mVitalBean.name);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    //选择时间
    public void showDatePickerDialog(Activity activity, int themeResId, final TextView tv, final EditText ev,Calendar calendar) {
        // 直接创建一个DatePickerDialog对话框实例，并将它显示出来
        new MyDatePickerDialog(activity
                ,  themeResId
                // 绑定监听器(How the parent is notified that the date is set.)
                ,new MyDatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day, String lunarStr, String str) {
                // 此处得到选择的时间，可以进行你想要的操作
                MainActivity.this.lunarStr = str;
                String _month = ""+month;
                String _day = ""+day;
                if(month < 10){
                    _month = "0"+month;
                }
                if(day < 10){
                    _day = "0"+day;
                }
                MainActivity.this.solarStr = _month+_day;
                tv.setText(year+ "年" + month+ "月" + day + "日 【" + lunarStr + "】");

                ev.setText("");
                ev.setFocusable(true);//设置输入框可聚集
                ev.setFocusableInTouchMode(true);//设置触摸聚焦
                ev.requestFocus();//请求焦点
                ev.findFocus();//获取焦点
                //强制键盘 隐藏 弹出 （）
                mInputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
//                mInputMethodManager.showSoftInput(ev,0);//显示输入法
            }
        }
                ,calendar
                ,true
                ,true
                ,true).show();
    }

    // 检查 设置  （是否开启通知栏权限）
    private void checkSettings() {
        if (!NotificationsUtils.isNotificationEnabled(this)) {
            final AlertDialog dialog = new AlertDialog.Builder(this).create();
            dialog.show();

            View view = View.inflate(this, R.layout.dialog, null);
            dialog.setContentView(view);

            TextView context = (TextView) view.findViewById(R.id.tv_dialog_context);
            context.setText("检测到您没有打开通知权限！！！\n是否去打开？？");

            TextView confirm = (TextView) view.findViewById(R.id.btn_confirm);
            confirm.setText("是");
            confirm.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dialog.cancel();
                    Intent localIntent = new Intent();
                    localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (Build.VERSION.SDK_INT >= 9) {
                        localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                        localIntent.setData(Uri.fromParts("package", MainActivity.this.getPackageName(), null));
                    } else if (Build.VERSION.SDK_INT <= 8) {
                        localIntent.setAction(Intent.ACTION_VIEW);

                        localIntent.setClassName("com.android.settings",
                                "com.android.settings.InstalledAppDetails");

                        localIntent.putExtra("com.android.settings.ApplicationPkgName",
                                MainActivity.this.getPackageName());
                    }
                    startActivity(localIntent);
                }
            });

            TextView cancel = (TextView) view.findViewById(R.id.btn_off);
            cancel.setText("否");
            cancel.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dialog.cancel();
                }
            });
        }
    }


    private AdapterView.OnItemLongClickListener mOnItemLongClickListener = new AdapterView.OnItemLongClickListener(){
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
            VitalBean bean = mVitalBeanList.get(position);
            if(!TextUtils.isEmpty(bean.date)){
                new AlertDialog.Builder(MainActivity.this).setTitle("删除").setMessage("你确定？？")
                        .setPositiveButton("确定",
                                new DialogInterface.OnClickListener(){
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mVitalBeanList.remove(position);
                                        toJson();
                                        putBirthday(0);//删除
                                    }
                                })
                        .setNegativeButton("取消",null)
//                            .setNeutralButton("取消",null)
                        .create().show();
                return true;
            }else{
                return false;
            }

        }
    };


    @Override
    public void onClick(View v) {
//        sendNotification(mContext,"title","content");
//        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//        imm.hideSoftInputFromWindow(v.getWindowToken(), 0); //强制隐藏键盘
        switch (v.getId()){
            case R.id.id_setting:
                //设置
                Intent mIntent = new Intent(mContext,SampleActivity.class);
                mIntent.putExtra("sign",1);
                startActivity(mIntent);
                break;
             case R.id.textView4:
                //主题 样式  0 -- 5
                //选择时间
                showDatePickerDialog(MainActivity.this,3,dateTextView,nicknameView,calendar);
                break;

            case R.id.textView:
                //查询 特殊节日
                try {
                    test();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                getBirthday(1);
                break;
            case R.id.button:
                //添加 特殊节日  每个账号最多50个
                String nickname = nicknameView.getText().toString().trim();
                if(TextUtils.isEmpty(lunarStr)){
                    Toast.makeText(mContext,"请添加时间！！！",Toast.LENGTH_SHORT).show();
                    return;
                }

                if(TextUtils.isEmpty(nickname)){
                    Toast.makeText(mContext,"请添加备注！！！",Toast.LENGTH_SHORT).show();
                    return;
                }

                if(mVitalBeanList.size() > 50){
                    Toast.makeText(mContext,"添加数量已达上线！！！",Toast.LENGTH_LONG).show();
                    return;
                }else{
                    mVitalBeanList.add(new VitalBean(lunarStr,nickname));
                    toJson();
                    putBirthday(1);//添加
                }
                break;
        }
    }



    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }
        @Override
        public void afterTextChanged(Editable s) {
            if(!TextUtils.isEmpty(s.toString()) && s.toString().length() >= 2){
//                nicknameView.setFocusable(false);//设置输入框不可聚焦,即失去焦点和光标
                if(mInputMethodManager.isActive()){
                    mInputMethodManager.hideSoftInputFromWindow(nicknameView.getWindowToken(),0);//隐藏输入法
                }
            }
        }
    };
    @Override
    protected void onDestroy() {
        //退出时 删除缓存文件
        FileUtils.deleteFile(SDPath);
        super.onDestroy();
    }

    //获取地区   mob  想做天气  但是地区表要做映射   放弃了
    private void getCitys() {
        NetworkRequest.getCitys(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        // 下载失败
                        e.printStackTrace();
                    }
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String _response = response.body().string();
//                        Log.i("---mzw---","getCitys : " + _response);
                        IBaseDao<CityBean> mIBaseDao = BaseDaoFactory.getInstance().getBaseDao(CityBean.class);
                        mIBaseDao.deleteAll();//清空表

                        int x = 0;
                        if(!TextUtils.isEmpty(_response) && _response.startsWith("{")) try {
                            JSONObject jsonObject = new JSONObject(_response);
                            if (!jsonObject.isNull("msg")) {
                                if ("success".equals(jsonObject.getString("msg")) && !jsonObject.isNull("result")) {
                                    JSONArray jsonArray = jsonObject.getJSONArray("result");
                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        //省
                                        JSONObject jsonProvince = (JSONObject) jsonArray.get(i);
                                        if (jsonProvince != null && !jsonProvince.isNull("province") && !jsonProvince.isNull("city")) {
                                            String province = jsonProvince.getString("province");
                                            String provinceId = MD5.encrypt(province);
//                                            CityBean provinceBean = new CityBean(provinceId, province, "","1");
//                                            CityBean provinceBean = new CityBean(province, "", "");
//                                            mIBaseDao.insert(provinceBean);//插入到数据库

                                            JSONArray jsonProvinceArray = jsonProvince.getJSONArray("city");
                                            for (int j = 0; j < jsonProvinceArray.length(); j++) {
                                                //市
                                                JSONObject jsonCity = (JSONObject) jsonProvinceArray.get(j);
                                                if (jsonCity != null && !jsonCity.isNull("district") && !jsonCity.isNull("city")) {
                                                    String city = jsonCity.getString("city");
                                                    String cityId = MD5.encrypt(province+city);
//                                                    CityBean cityBean = new CityBean(cityId, city, provinceId,"2");
//                                                    CityBean cityBean = new CityBean(province, city, "");
//                                                    mIBaseDao.insert(cityBean);//插入到数据库

                                                    JSONArray jsonDistrictArray = jsonCity.getJSONArray("district");
                                                    if (jsonDistrictArray != null) {
                                                        for (int k = 0; k < jsonDistrictArray.length(); k++) {
                                                            x++;
                                                            //区县
                                                            JSONObject jsonDistrict = (JSONObject) jsonDistrictArray.get(k);
                                                            String district = jsonDistrict.getString("district");
                                                            String districtId = MD5.encrypt(province+city+district);
//                                                            CityBean districtBean = new CityBean(districtId, district, cityId,"3");
                                                            CityBean districtBean = new CityBean(province, city, district);
                                                            mIBaseDao.insert(districtBean);//插入到数据库

                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Log.i("---mzw---","====" + x);
                    }
                });
    }


    private List<Address> getAddress(Location location) {
        List<Address> result = null;
        try {
            if (location != null) {
                Geocoder gc = new Geocoder(MainActivity.this, Locale.getDefault());
                result = gc.getFromLocation(location.getLatitude(),
                        location.getLongitude(), 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }




    private void test() throws ParseException {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        Lunar lunar = new Lunar(calendar);

        Log.i("---mzw---"," " + lunar.cyclical());




        //第一步  获取24节气 集合
        Map<String ,String> map24 =  SolarTerms24.solarTermToMap(calendar.get(Calendar.YEAR));
        /*
        第二步  获取三伏 集合
            初伏 从夏至那天 开始 循环找到第三个庚日
            中伏 从夏至那天 开始 循环找到第四个庚日
            末伏 从立秋那天 开始 循环找到第一个庚日
        */
        //三伏  三九   集合
        Map<String ,String> map3 = new HashMap<>();
        int i = 0;
        boolean b = true;

        String xiaZhi = map24.get("夏至");
        Date xiaZhiDate = ConstantParameter.sdf_b.parse(xiaZhi);
        calendar.setTime(xiaZhiDate);
        do{
            GanZhiJIRi ganZhi = new GanZhiJIRi(calendar);
            if(ganZhi.toString().startsWith("庚")){
                i += 1;
                if(i == 3){
                    map3.put(ConstantParameter.sdf_b.format(calendar.getTime()),"初伏");
                }
                if(i == 4){
                    map3.put(ConstantParameter.sdf_b.format(calendar.getTime()),"中伏");
                    b = false;
                }
            }
            calendar.add(Calendar.DATE, 1);
        }while (b);

        String liQiu = map24.get("立秋");
        Date liQiuDate = ConstantParameter.sdf_b.parse(liQiu);
        calendar.setTime(liQiuDate);
        do{
            GanZhiJIRi ganZhi = new GanZhiJIRi(calendar);
            if(ganZhi.toString().startsWith("庚")){
                map3.put(ConstantParameter.sdf_b.format(calendar.getTime()),"末伏");
                b = false;
            }
            calendar.add(Calendar.DATE, 1);
        }while (b);


        /*
        第三步  获取三九 集合
            从冬至那天 开始 循环  冬至为 一九   每9天加一九 （如 冬至后第十天是二九，第十八天是三九 ... 一直到九九）
            一九二九不出手，三九四九冰上走，五九六九沿河看柳，七九河开，八九雁来，九九归一九，犁牛遍地走。
         */
        String dongZhi = map24.get("冬至");
        Date dongZhiDate = ConstantParameter.sdf_b.parse(dongZhi);
        calendar.setTime(dongZhiDate);

        String sanJiu[] = {"一", "二", "三", "四", "五", "六", "七", "八", "九"};
        i = 0;
        while (i <= 80){
            if(i % 9 == 0){
                map3.put(ConstantParameter.sdf_b.format(calendar.getTime()),sanJiu[i / 9]+"九");
                map3.put(ConstantParameter.sdf_b.format(calendar.getTime()),"第一天");
            }else{
                map3.put(ConstantParameter.sdf_b.format(calendar.getTime()),"第"+sanJiu[i%9]+"天");
            }
            calendar.add(Calendar.DATE, 1);
            i += 1;
        }

        ConstantParameter.saveMap(mContext,"map24",map24);
        ConstantParameter.saveMap(mContext,"map3",map3);


    }

}
