package com.mzw.appwidgetdemog.bean;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by think on 2018/11/4.
 */

public class DateBean implements Serializable {

    /**
     * 阳历
     */
    public Date date;
    public String yangli_year;
    public String yangli_month;
    public String yangli_day;
    public String festival;//节日
    /**
     * 阴历 农历
     */
    public String yinli;
    /**
     * 1日期 可点击，0 不是 不可点击
     */
    public int sign = 0;

    public DateBean(Date date, String yangli_year, String yangli_month, String yangli_day, String yinli, int sign, String festival) {
        this.yangli_year = yangli_year;
        this.yangli_month = yangli_month;
        this.yangli_day = yangli_day;
        this.yinli = yinli;
        this.sign = sign;
        this.date = date;
        this.festival = festival;
    }

    @Override
    public String toString() {
        return "DateBean{" +
                "date=" + date +
                ", yangli_year='" + yangli_year + '\'' +
                ", yangli_month='" + yangli_month + '\'' +
                ", yangli_day='" + yangli_day + '\'' +
                ", yinli='" + yinli + '\'' +
                ", sign=" + sign +
                ", festival=" + festival +
                '}';
    }
}
