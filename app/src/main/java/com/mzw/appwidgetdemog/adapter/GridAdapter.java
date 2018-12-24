package com.mzw.appwidgetdemog.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mzw.appwidgetdemog.R;
import com.mzw.appwidgetdemog.bean.VitalBean;
import com.mzw.appwidgetdemog.tools.Lunar;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by think on 2018/11/11.
 */

public class GridAdapter extends BaseAdapter {
    private List<VitalBean> mVitalBeanList;
    private Context mContext;
    private LayoutInflater layoutInflater;

    public GridAdapter(List<VitalBean> mVitalBeanList, Context mContext, LayoutInflater layoutInflater) {
        this.mVitalBeanList = mVitalBeanList;
        this.mContext = mContext;
        this.layoutInflater = layoutInflater;
    }

    @Override
    public int getCount() {
        return mVitalBeanList.size();
    }

    @Override
    public Object getItem(int position) {
        return mVitalBeanList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        VitalBean bean = mVitalBeanList.get(position);
        ViewHolder mViewHolder;
        if(convertView == null){
            convertView = layoutInflater.inflate(R.layout.grid_item,parent,false);
            mViewHolder = new ViewHolder();
            mViewHolder.date_view = convertView.findViewById(R.id.date_view);
            mViewHolder.nick_view = convertView.findViewById(R.id.nick_view);
            convertView.setTag(mViewHolder);
        }else{
            mViewHolder = (ViewHolder)convertView.getTag();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        Lunar lunar = new Lunar(calendar);
        String content = lunar.birthday(mContext);
        String title = lunar.toString();

        if(lunar.toString().equals(Lunar.toString(bean.date))){
            mViewHolder.date_view.setTextColor(mContext.getResources().getColor(R.color.today_text));
            mViewHolder.nick_view.setTextColor(mContext.getResources().getColor(R.color.today_text));
        }else{
            mViewHolder.date_view.setTextColor(mContext.getResources().getColor(R.color.week_text));
            mViewHolder.nick_view.setTextColor(mContext.getResources().getColor(R.color.week_text));
        }
        mViewHolder.date_view.setText(Lunar.toString(bean.date));
        mViewHolder.nick_view.setText(bean.name);

        return convertView;
    }

    class ViewHolder{
        TextView date_view;
        TextView nick_view;
    }

}
