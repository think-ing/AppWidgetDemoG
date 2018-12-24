package com.mzw.appwidgetdemog.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mzw.appwidgetdemog.MainActivity;
import com.mzw.appwidgetdemog.NetworkRequest;
import com.mzw.appwidgetdemog.R;
import com.mzw.appwidgetdemog.bean.UserBean;
import com.mzw.appwidgetdemog.bean.VitalBean;
import com.mzw.appwidgetdemog.tools.ConstantParameter;
import com.mzw.appwidgetdemog.tools.FileUtils;
import com.mzw.appwidgetdemog.tools.MD5;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;
import cn.smssdk.gui.RegisterPage;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import okio.Sink;

/**
 * Created by think on 2018/11/29.
 */

public class LoginActivity extends Activity implements View.OnClickListener {

    private LinearLayout linearLayout;

    private EditText username_view;
    private EditText password_view;
    private TextView register_view;
    private TextView submit_view;

    private Context mContext;

    private String username,password;
    private String SDPath;//SD根目录
    private File srcFile,encFile,decFile;


    private List<UserBean> mUserBeanList = new ArrayList<UserBean>();


    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 1:
                    try {
                        //文件解密
                        FileUtils.DecFile(encFile,decFile);
                        //读取文件json
                        String _response = FileUtils.readFile(decFile);
                        if(!TextUtils.isEmpty(_response) && _response.startsWith("{")){
                            //转jaon
                            JSONObject mJSONObject = new JSONObject(_response.trim());
                            Iterator<String> krys = mJSONObject.keys();
                            //转为list
                            while(krys.hasNext()) {
                                String k = krys.next();
                                String v = mJSONObject.getString(k);
                                mUserBeanList.add(new UserBean(k,v));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case 2:
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
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);
        mContext = this;
        //获取 sd卡路径
        SDPath = FileUtils.getSDPath(mContext);

        username_view = findViewById(R.id.id_username_view);
        password_view = findViewById(R.id.id_password_view);
        register_view = findViewById(R.id.id_register_view);
        submit_view = findViewById(R.id.id_submit_view);
        linearLayout = findViewById(R.id.id_layout);

        findViewById(R.id.id_find_pass_view).setOnClickListener(this);
        register_view.setOnClickListener(this);
        submit_view.setOnClickListener(this);

        srcFile = new File(SDPath+"/"+ConstantParameter.USER_INFO_KEY+".mzw"); //初始文件
        encFile = new File(SDPath+"/"+ConstantParameter.USER_INFO_KEY+".by"); //加密文件
        decFile = new File(SDPath+"/"+ConstantParameter.USER_INFO_KEY+".mzw"); //解密文件

        username = getIntent().getStringExtra("phone");
        username_view.setText(username);

        getUserInfo();
    }

    //获取 用户信息
    private void getUserInfo() {
        String fileUrl = NetworkRequest.getDownloadUrl(ConstantParameter.USER_INFO_KEY+".by");
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
                    //将文件保存到本地
//                    File dest = new File(SDPath,ConstantParameter.USER_INFO_KEY+".by");
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

    @Override
    public void onClick(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0); //强制隐藏键盘
        switch (v.getId()){
            case R.id.id_register_view:
                //注册      手机号验证
                sendCode(1);
                break;
             case R.id.id_find_pass_view:
                //找回密码  手机号验证
                sendCode(2);
                break;

            case R.id.id_submit_view:
                boolean b = false;
                username = username_view.getText().toString().trim();
                password = MD5.encrypt(password_view.getText().toString().trim());

                for (UserBean bean : mUserBeanList) {
                    if(bean.username.equals(username) && bean.password.equals(password)){
                        b = true;
                        ConstantParameter.saveUserName(mContext, username, password);
                        startActivity(new Intent(mContext,MainActivity.class));
                        FileUtils.deleteFile(SDPath);
                        this.finish();
                    }
                }
                if(!b){
                    new AlertDialog.Builder(mContext).setTitle("登陆失败").setMessage("用户或密码不正确！！")
                            .setPositiveButton("确定",null)
                            .create().show();
                }

                break;
        }
    }



    @Override
    protected void onDestroy() {
        //推出时 删除缓存文件
        FileUtils.deleteFile(SDPath);
        super.onDestroy();
    }


    //mob  短信验证  UI，发送，验证 都是由 mob完成
    public void sendCode(final int sign) {
        RegisterPage page = new RegisterPage();
        //如果使用我们的ui，没有申请模板编号的情况下需传null
        page.setTempCode(null);
        page.setRegisterCallback(new EventHandler() {
            public void afterEvent(int event, int result, Object data) {
                Log.i("---mzw---","event："+event + " , result："+result);
                if (result == SMSSDK.RESULT_COMPLETE) {
                    // 处理成功的结果
                    HashMap<String,Object> phoneMap = (HashMap<String, Object>) data;
                    String country = (String) phoneMap.get("country"); // 国家代码，如“86”
                    String phone = (String) phoneMap.get("phone"); // 手机号码，如“13800138000”
                    // TODO 利用国家代码和手机号码进行后续的操作
                    Log.i("---mzw---","country: " + country);
                    Log.i("---mzw---","phone: " + phone);

                    //进入注册最后一步   填写密码
                    Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                    intent.putExtra("phone",phone);
                    intent.putExtra("sign",sign);
                    Pair<View,String> pairLayout = new Pair<View, String>(linearLayout,"layout");

//                    if (Build.VERSION.SDK_INT >= 22) {
//                        ActivityOptionsCompat options = ActivityOptionsCompat
//                                .makeSceneTransitionAnimation(LoginActivity.this,pairLayout);
//                        startActivity(intent, options.toBundle());
//                    } else {
                        startActivity(intent);
//                    }


                } else{
                    // TODO 处理错误的结果
                    Log.i("---mzw---","错误...");
                    String str = "";
                    switch (result){
                        case 600:str = "请求太频繁";break;
                        case 601:str = "短信发送受限";break;
                        case 602:str = "无法发送此地区短信";break;
                        case 603:str = "请填写正确的手机号码";break;
                        case 604:str = "暂不支持此国家";break;
                        case 605:str = "没有权限连接服务端";break;
                        case 606:str = "无权访问该接口";break;
                        case 607:str = "Contet-Length错误";break;
                        case 608:str = "AppKey为空";break;
                        case 609:str = "Sign为空";break;
                        case 610:str = "UserAgent为空";break;
                        case 611:str = "AppSecret为空";break;
                    }
                }
            }
        });
        page.show(mContext);
    }
}
