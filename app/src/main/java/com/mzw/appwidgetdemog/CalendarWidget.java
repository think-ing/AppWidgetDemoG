package com.mzw.appwidgetdemog;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.mzw.appwidgetdemog.activity.SampleActivity;
import com.mzw.appwidgetdemog.bean.*;
import com.mzw.appwidgetdemog.tools.*;

import java.util.*;

import static android.app.Notification.PRIORITY_DEFAULT;
import static android.app.Notification.VISIBILITY_SECRET;


/**
 * 日历挂件
 * Implementation of App Widget functionality.
 */
public class CalendarWidget extends AppWidgetProvider {

    private List<DateBean> mDateList;
    private Date receive_date;


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        Log.i("---mzw---","============onUpdate===========");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        Lunar lunar = new Lunar(calendar);
        String content = lunar.birthday(context);
        String title = lunar.toString();
        if(!TextUtils.isEmpty(content)){
            //今天是生日
            Log.i("---mzw---","今天是" + content + "的生日");
//            sendNotification(context,"今天是重要的日子：" + str);
            if(ConstantParameter.getBirthdayRemind(context) == 0){//如果人为关闭提醒 则不在提醒
                sendNotification(context,title,content);
            }
        }else{
            //今天不是特殊节日则打开提醒
            ConstantParameter.saveBirthdayRemind(context,0);
        }
        getData(context,new Date());
        for (int appWidgetId : appWidgetIds) {
            drawWidget(context, appWidgetId);
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        getData(context,new Date());
        drawWidget(context, appWidgetId);
    }

    @Override
    public void onEnabled(final Context context) {
        Log.i("---mzw---","============onEnabled===========");
        // Enter relevant functionality for when the first widget is created
        Intent mIntent = new Intent(context,SampleActivity.class);
        mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(mIntent);
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        ConstantParameter.saveWidgetBackground(context,"#000000",66);
    }

    private void redrawWidgets(Context context) {
        int[] appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(
                new ComponentName(context, CalendarWidget.class));
        for (int appWidgetId : appWidgetIds) {
            drawWidget(context, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //接收广播处理
        String action = intent.getAction();
        Log.i("---mzw---","action1 : "+action);
        Date _receive_date = new Date(intent.getLongExtra("receive_date", 0));
        Log.i("---mzw---","_receive_date : "+ConstantParameter.sdf_a.format(_receive_date));
        Date mDate = new Date();
        if(ConstantParameter.WIDGET_BACKGROUND.equals(action)){
            //设置
//            sign = 1;
//            mBackgroundColor = intent.getStringExtra("mBackgroundColor");
//            mProgress = intent.getIntExtra("mProgress",66);
//            if(TextUtils.isEmpty(mBackgroundColor)){
//                mBackgroundColor = "#000000";
//            }
        }else if(ConstantParameter.BIRTHDAY_REMIND.equals(action)){
            //关闭通知
            NotificationManager notiManager = (NotificationManager)context.getSystemService(context.NOTIFICATION_SERVICE);
            int notifyId = intent.getIntExtra("notifyId",0);
            if(notifyId != 0){
                notiManager.cancel(notifyId);
            }
            //不再提醒
            ConstantParameter.saveBirthdayRemind(context,-1);
        }else if(ConstantParameter.back_today.equals(action)){
            //回到今天
            mDate = new Date();
        }else if(ConstantParameter.month_previous.equals(action)){
            //上一月
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(_receive_date);
            calendar.add(Calendar.MONTH, -1);
            mDate = calendar.getTime();
        }else if(ConstantParameter.month_next.equals(action)){
            //下一月
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(_receive_date);
            calendar.add(Calendar.MONTH, 1);
            mDate = calendar.getTime();
        }else{
            //点击 天
//            //判断点击的是否是 本月
//            if(ConstantParameter.sdf_tomonth.format(_receive_date).equals(ConstantParameter.sdf_tomonth.format(new Date()))){
//                //是 本月
//            }else{
//                //不是本月\
//            }
            mDate = _receive_date;
        }
        getData(context,mDate);
        redrawWidgets(context);
        super.onReceive(context, intent);
    }

    //获取数据
    private void getData(Context context,Date d) {
        Log.i("---mzw---","getData: "+ConstantParameter.sdf_a.format(d));
        mDateList = new ArrayList<DateBean>();
        receive_date = d;
        if(receive_date == null){
            receive_date = new Date();
        }
        Date date = DateUtil.getMonthStart(receive_date);
        Date monthEnd = DateUtil.getMonthEnd(receive_date);
        while (!date.after(monthEnd)) {
            Calendar calendar= Calendar.getInstance();
            calendar.setTime(date);
            Lunar lunar = new Lunar(calendar);
            mDateList.add(new DateBean(date,ConstantParameter.sdf_year.format(date),ConstantParameter.sdf_month.format(date),ConstantParameter.sdf_day.format(date),lunar.toString(),1,lunar.festival(context)));
            date = DateUtil.getNext(date);
        }

        //如果一号不是周一，取前几天到周一
        while(!"星期一".equals(DateUtil.dateToWeek(mDateList.get(0).date))){
            Date date1 = DateUtil.getPrevious(mDateList.get(0).date);
            Calendar calendar= Calendar.getInstance();
            calendar.setTime(date1);
            Lunar lunar = new Lunar(calendar);
            mDateList.add(0,new DateBean(date1, ConstantParameter.sdf_year.format(date1),ConstantParameter.sdf_month.format(date1),ConstantParameter.sdf_day.format(date1),lunar.toString(),1,lunar.festival(context)));
        }
        //如果月末不是周日，取后几天到周日
        while(!"星期日".equals(DateUtil.dateToWeek(mDateList.get(mDateList.size()-1).date))){
            Date date2 = DateUtil.getNext(mDateList.get(mDateList.size()-1).date);
            Calendar calendar= Calendar.getInstance();
            calendar.setTime(date2);
            Lunar lunar = new Lunar(calendar);
            mDateList.add(new DateBean(date2,ConstantParameter.sdf_year.format(date2),ConstantParameter.sdf_month.format(date2),ConstantParameter.sdf_day.format(date2),lunar.toString(),1,lunar.festival(context)));
        }

        while(mDateList != null && mDateList.size() < 42){
            Date date2 = DateUtil.getNext(mDateList.get(mDateList.size()-1).date);
            Calendar calendar= Calendar.getInstance();
            calendar.setTime(date2);
            Lunar lunar = new Lunar(calendar);
            mDateList.add(new DateBean(date2,ConstantParameter.sdf_year.format(date2),ConstantParameter.sdf_month.format(date2),ConstantParameter.sdf_day.format(date2),lunar.toString(),1,lunar.festival(context)));
        }
    }

    //填充布局
    private void drawWidget(Context context, int appWidgetId) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        RemoteViews mRemoteView = new RemoteViews(context.getPackageName(), R.layout.calendar_widget);

        //背景
        Map<String,Object> map = ConstantParameter.getWidgetBackground(context);
        String mBackgroundColor = (String)map.get("mBackgroundColor");
        int mProgress = (int)map.get("mProgress");

        int colorInt = Color.parseColor(mBackgroundColor);
        int red = (colorInt & 0xff0000) >> 16;
        int green = (colorInt & 0x00ff00) >> 8;
        int blue = (colorInt & 0x0000ff);
        int _mBackgroundColor = Color.argb(mProgress,red,green,blue);
        mRemoteView.setInt(R.id.id_relativeLayout,"setBackgroundColor",_mBackgroundColor);


        mRemoteView.removeAllViews(R.id.id_calendar);
        RemoteViews mRowView = null;
        RemoteViews mDayView = null;
        for(int i =0; i < mDateList.size(); i++){
            DateBean mDateBean = mDateList.get(i);

            if (mRowView == null || i % 7 == 0){
                mRowView = new RemoteViews(context.getPackageName(), R.layout.calendar_row);
            }
            mDayView = new RemoteViews(context.getPackageName(), R.layout.calendar_day);

            //获取当前时间 用来区分当天，当月，其他月
            //当月
            if(ConstantParameter.sdf_year.format(receive_date).equals(mDateBean.yangli_year) &&
                    ConstantParameter.sdf_month.format(receive_date).equals(mDateBean.yangli_month)){
                mDayView.setTextColor(R.id.id_yangli_text, context.getResources().getColor(R.color.this_month_text));
                mDayView.setTextColor(R.id.id_yinli_text, context.getResources().getColor(R.color.this_month_text));
            }else{
                //其他
                mDayView.setTextColor(R.id.id_yangli_text, context.getResources().getColor(R.color.other_month_text));
                mDayView.setTextColor(R.id.id_yinli_text, context.getResources().getColor(R.color.other_month_text));
            }

            //将农历拆分为 月 和 日
            String yinli_month = mDateBean.yinli.substring(0,2);
            String yinli_day = mDateBean.yinli.substring(2,4);
            //如果是初一 则显示月分
            if("初一".equals(yinli_day)){
                mDayView.setTextViewText(R.id.id_yinli_text, yinli_month);
                mDayView.setTextColor(R.id.id_yinli_text, context.getResources().getColor(R.color.yinli_month_text));
            }else{
                mDayView.setTextViewText(R.id.id_yinli_text, yinli_day);
//                views.setTextColor(yinliId, context.getResources().getColor(R.color.other_month_text));
            }
            //特殊节日
            if(!TextUtils.isEmpty(mDateBean.festival)){
                if("消费者权益日".equals(mDateBean.festival)){
                    mDayView.setTextViewText(R.id.id_yinli_text, "打假");
                }else{
                    mDayView.setTextViewText(R.id.id_yinli_text, mDateBean.festival);
                }
                mDayView.setTextColor(R.id.id_yinli_text, context.getResources().getColor(R.color.yinli_month_text));
            }
            mDayView.setTextViewText(R.id.id_yangli_text,mDateBean.yangli_day);

            //当天
            if(ConstantParameter.sdf_year.format(new Date()).equals(mDateBean.yangli_year) &&
                    ConstantParameter.sdf_month.format(new Date()).equals(mDateBean.yangli_month) &&
                    ConstantParameter.sdf_day.format(new Date()).equals(mDateBean.yangli_day)){
                mDayView.setInt(R.id.id_itemLayout, "setBackgroundResource", R.drawable.btn_b);
            }else{
                mDayView.setInt(R.id.id_itemLayout, "setBackgroundResource", R.drawable.btn_a);
            }

            //监听每天点击事件
            mDayView.setOnClickPendingIntent(R.id.id_itemLayout,
                    PendingIntent.getBroadcast(context, 0,
                            new Intent(context, CalendarWidget.class)
                                    .setAction(ConstantParameter.itemLayout+"_"+i)
                                    .putExtra("receive_date",mDateBean.date.getTime()),
                            PendingIntent.FLAG_UPDATE_CURRENT));

            mRowView.addView(R.id.id_calendar_row,mDayView);
            if (mRowView != null && i % 7 == 6){
                mRemoteView.addView(R.id.id_calendar,mRowView);
            }
        }

        //上月
        mRemoteView.setOnClickPendingIntent(R.id.id_month_previous,
                PendingIntent.getBroadcast(context, 0,
                        new Intent(context, CalendarWidget.class)
                                .setAction(ConstantParameter.month_previous)
                                .putExtra("receive_date",receive_date.getTime()),
                        PendingIntent.FLAG_UPDATE_CURRENT));

        //下月
        mRemoteView.setOnClickPendingIntent(R.id.id_month_next,
                PendingIntent.getBroadcast(context, 0,
                        new Intent(context, CalendarWidget.class)
                                .setAction(ConstantParameter.month_next)
                                .putExtra("receive_date",receive_date.getTime()),
                        PendingIntent.FLAG_UPDATE_CURRENT));

        //返回今天
        mRemoteView.setOnClickPendingIntent(R.id.id_today_view_a,
                PendingIntent.getBroadcast(context, 0,
                        new Intent(context, CalendarWidget.class)
                                .setAction(ConstantParameter.back_today)
                                .putExtra("receive_date",new Date().getTime()),
                        PendingIntent.FLAG_UPDATE_CURRENT));

        // 当前时间
        Calendar calendar= Calendar.getInstance();
        calendar.setTime(receive_date);
        Lunar lunar = new Lunar(calendar);
        lunar.festival(null);

        mRemoteView.setTextViewText(R.id.id_today_view_a,ConstantParameter.sdf.format(receive_date));
        mRemoteView.setTextViewText(R.id.id_today_view_b,lunar.cyclical() + " " + lunar.animalsYear() + "年 " + lunar.toString());

        appWidgetManager.updateAppWidget(appWidgetId, mRemoteView);
    }


    //重要日子 发送通知
    private void sendNotification(Context mContext,String title,String content) {
        Log.i("---mzw---","title: " + title + " , content: " + content);
        int notifyId = Integer.parseInt(ConstantParameter.sdf_hhmm.format(new Date()));

        RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.notification_layout);
        remoteViews.setTextViewText(R.id.id_title,"【"+title+"】");
        content = "今天是你备注【"+content+"】的特殊日子，不要忘记哦！！";
        remoteViews.setTextViewText(R.id.id_content,content);
        //返回今天
        remoteViews.setOnClickPendingIntent(R.id.id_btn,
                PendingIntent.getBroadcast(mContext, 0,
                        new Intent(mContext, CalendarWidget.class)
                                .setAction(ConstantParameter.BIRTHDAY_REMIND)
                                .putExtra("notifyId",notifyId)
                                .putExtra("receive_date",new Date().getTime()),
                        PendingIntent.FLAG_UPDATE_CURRENT));

        // 获取通知服务对象NotificationManager
        NotificationManager notiManager = (NotificationManager)mContext.getSystemService(mContext.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            /**
             * 紧急级别（发出通知声音并显示为提示通知）
             IMPORTANCE_HIGH
             PRIORITY_HIGH或者PRIORITY_MAX

             高级别（发出通知声音并且通知栏有通知）
             IMPORTANCE_DEFAULT
             PRIORITY_DEFAULT

             中等级别（没有通知声音但通知栏有通知）
             IMPORTANCE_LOW
             PRIORITY_LOW

             低级别（没有通知声音也不会出现在状态栏上）
             IMPORTANCE_MIN
             PRIORITY_MIN
             */
            NotificationChannel channel = new NotificationChannel("channel_id", "权限设置",
                    NotificationManager.IMPORTANCE_DEFAULT);//高级别（发出通知声音并且通知栏有通知）
            channel.canBypassDnd();//是否绕过请勿打扰模式
            channel.enableLights(true);//闪光灯
            channel.setLockscreenVisibility(VISIBILITY_SECRET);//锁屏显示通知
            channel.setLightColor(Color.RED);//闪关灯的灯光颜色
            channel.canShowBadge();//桌面launcher的消息角标
            channel.enableVibration(true);//是否允许震动
//            channel.getAudioAttributes();//获取系统通知响铃声音的配置
            channel.setSound(Uri.parse("android.resource://" + mContext.getPackageName() + "/" +R.raw.aa),Notification.AUDIO_ATTRIBUTES_DEFAULT);
            channel.getGroup();//获取通知取到组
            channel.setBypassDnd(true);//设置可绕过  请勿打扰模式
            channel.setVibrationPattern(new long[]{100, 100, 200});//设置震动模式
            channel.shouldShowLights();//是否会有灯光

            notiManager.createNotificationChannel(channel);
        }

        // 创建Notification对象
        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new NotificationCompat.Builder(mContext, "channel_id");
        } else {
            builder = new NotificationCompat.Builder(mContext);
            builder.setPriority(PRIORITY_DEFAULT);
            builder.setDefaults(Notification.DEFAULT_SOUND);    // 设置声音/震动等
        }

        builder.setContent(remoteViews);
        builder.setTicker("birthday");            // 通知弹出时状态栏的提示文本
        builder.setContentInfo("别忘记哦！！！");  //
        builder.setContentTitle("重大通知")    // 通知标题
                .setContentText(content)  // 通知内容
                .setSmallIcon(R.mipmap.f000);    // 通知小图标
        builder.setAutoCancel(true);
        Notification notification = builder.build();
        notification.defaults = Notification.DEFAULT_SOUND;
        notiManager.notify(notifyId, notification);

    }
}

