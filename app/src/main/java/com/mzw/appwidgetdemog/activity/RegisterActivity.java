package com.mzw.appwidgetdemog.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import com.mzw.appwidgetdemog.tools.ConstantParameter;
import com.mzw.appwidgetdemog.tools.FileUtils;
import com.mzw.appwidgetdemog.tools.MD5;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpCompletionHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import okio.Sink;

/**
 * Created by think on 2018/11/29.
 */

public class RegisterActivity  extends Activity implements View.OnClickListener {

    private LinearLayout linearLayout;
    private TextView username_view;
    private EditText password_view;
    private TextView submit_view;

    private Context mContext;

    private String username, password;
    private int sign = 1;// 1注册    2找回密码

    private String SDPath;//SD根目录
    private File srcFile,encFile,decFile;

    private List<UserBean> mUserBeanList = new ArrayList<UserBean>();
    private JSONObject mJSONObject;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    //注册成功
                    ConstantParameter.saveUserName(mContext, username, password);
                    startActivity(new Intent(mContext, MainActivity.class));
                    RegisterActivity.this.finish();
                    break;
                case 1:
                    int sign = msg.arg1;
                    try {
                        //文件解密
                        FileUtils.DecFile(encFile,decFile);
                        //读取文件json
                        String _response = FileUtils.readFile(decFile);
                        if(!TextUtils.isEmpty(_response) && _response.startsWith("{")){
                            //转jaon
                            mJSONObject = new JSONObject(_response.trim());
                            Iterator<String> krys = mJSONObject.keys();
                            //转为list
                            while(krys.hasNext()) {
                                String k = krys.next();
                                String v = mJSONObject.getString(k);
                                mUserBeanList.add(new UserBean(k,v));
                            }
                        }else{
                            mJSONObject = new JSONObject();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if(sign == 1){
                        rigisterNetwork(username,password);
                    }
                    break;
                case -1:
                    Toast.makeText(mContext, "操作失败请重试...", Toast.LENGTH_SHORT).show();
                    break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);
        mContext = this;

        //获取 sd卡路径
        SDPath = FileUtils.getSDPath(mContext);

        srcFile = new File(SDPath+"/"+ConstantParameter.USER_INFO_KEY+".mzw"); //初始文件
        encFile = new File(SDPath+"/"+ConstantParameter.USER_INFO_KEY+".by"); //加密文件
        decFile = new File(SDPath+"/"+ConstantParameter.USER_INFO_KEY+".mzw"); //解密文件

        linearLayout = findViewById(R.id.id_layout);
        username_view = findViewById(R.id.id_username_view);
        password_view = findViewById(R.id.id_password_view);
        submit_view = findViewById(R.id.id_submit_view);

        submit_view.setOnClickListener(this);

        sign = getIntent().getIntExtra("sign",1);
        username = getIntent().getStringExtra("phone");
        username_view.setText(username);
        if(sign == 1){
            submit_view.setText("注  册");
        }else{
            submit_view.setText("提  交");
        }
        //下载 用户信息文件
        getUserInfo(0);
    }

    @Override
    public void onClick(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0); //强制隐藏键盘
        boolean b = true;
        password = MD5.encrypt(password_view.getText().toString().trim());
        if(TextUtils.isEmpty(username) || TextUtils.isEmpty(password)){
            return;
        }

        if(sign == 1){
            for (UserBean bean : mUserBeanList) {
                if(bean.username.equals(username)){
                    b = false;
                    new AlertDialog.Builder(mContext).setTitle("注册失败").setMessage("用户已存在！！")
                            .setPositiveButton("去登陆",new DialogInterface.OnClickListener(){
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                    intent.putExtra("phone",username);
                                    Pair<View,String> pairLayout = new Pair<View, String>(linearLayout,"layout");

                                    if (Build.VERSION.SDK_INT >= 22) {
                                        ActivityOptionsCompat options = ActivityOptionsCompat
                                                .makeSceneTransitionAnimation(RegisterActivity.this,pairLayout);
                                        startActivity(intent, options.toBundle());
                                    } else {
                                        startActivity(intent);
                                    }
                                }
                            })
                            .create().show();
                    break;
                }
            }
        }


        /**
         * 为了避免 并发出现错误， 在上传文件前重新下载文件
         * 解析后立刻上传
         */
        if(b){
            getUserInfo(1);
        }
    }

    //上传 用户信息
    private void rigisterNetwork(String username, String password) {

        try {
            //将数据添加到 json
            mJSONObject.put(username,password);
            //将json写入文档文件
            FileUtils.writeFile(srcFile,mJSONObject.toString());
            //文件加密
            FileUtils.EncFile(srcFile,encFile);

            //将加密文件上传到七牛云
            NetworkRequest.uploadingFile(encFile,ConstantParameter.USER_INFO_KEY+".by",
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
                                    mHandler.sendEmptyMessage(0);
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

    //获取 用户信息   1 下载后上传文件，  0不上传
    private void getUserInfo(final int sign) {
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
//                    File dest = new File(SDPath,ConstantParameter.USER_INFO_KEY+".by");
                    sink = Okio.sink(encFile);
                    bufferedSink = Okio.buffer(sink);
                    bufferedSink.writeAll(response.body().source());

                    bufferedSink.close();

                    Message msg = new Message();
                    msg.arg1 = sign;
                    msg.what = 1;
                    mHandler.sendMessage(msg);

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
    protected void onDestroy() {
        //推出时 删除缓存文件
        FileUtils.deleteFile(SDPath);
        super.onDestroy();
    }
}