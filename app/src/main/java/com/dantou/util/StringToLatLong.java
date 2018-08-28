package com.dantou.util;

import android.util.Log;

import java.util.ArrayList;
import com.dantou.model.Point;

public class StringToLatLong {
    public static Point toLatLong(String llString){
        if (llString.startsWith("050082")){

            int id  = Integer.valueOf(llString.substring(8, 12), 16);
            //System.out.println("id:" + id);

            double longitude = Integer.valueOf(llString.substring(18, 26), 16) * 0.00001;
            //System.out.println("longitude:" + longitude);

            double latitude = Integer.valueOf(llString.substring(26, 34), 16) * 0.00001;
            //System.out.println("latitude:" + latitude);

            return new Point(id, latitude, longitude, null);
        }
        return null;
    }

    public static ArrayList<Point> toLatLongs(String llStrings){
        /*Log.d("进入toLatLongs方法", llStrings.trim());
        Log.e("分割后的string数组", "==" + llStrings.charAt(0) + "==");

        if(llStrings.contains("\n")) {
            llStrings = llStrings.replace('\n', 'M');
            Log.e("包含回车键", "" + llStrings);
        }
        else
            Log.d("不包含回车键", "don't contain enter key");*/
        String[] lls = llStrings.split("\n");
        ArrayList<Point> latLongList = new ArrayList<>();
        for(String s : lls){
            Log.d("切分后的字符串", s);
            if(toLatLong(s) != null){
                latLongList.add(toLatLong(s.trim()));
            }
        }
        return latLongList;
    }

    public static void main(String[] args){
        String s = "05 00 82 13 00 01 00 0F 01 00 AE 40 0C 00 2E C2 27 12 08 1B 13 05 38 9D";
        Point point = toLatLong(s.replaceAll(" ", ""));
        System.out.println("id:" + point.getId() + "\nlatitude:" +point.getLatitude() + "\nlongitude:" + point.getLongitude());
    }
}
