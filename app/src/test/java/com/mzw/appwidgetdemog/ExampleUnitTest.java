package com.mzw.appwidgetdemog;

import android.util.Log;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        int mProgress = 200;
        String mBackgroundColor = "#FF4183";

        String _mProgress = Integer.toHexString(mProgress);
        StringBuilder sb = new StringBuilder(mBackgroundColor);//构造一个StringBuilder对象
        String _mBackgroundColor = sb.insert(1,_mProgress).toString();



        System.out.print("============" + _mBackgroundColor);
//        Log.i("---mzw---","==================");
//        assertEquals(4, 2 + 2);

    }
}