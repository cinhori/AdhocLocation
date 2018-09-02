package com.dantou.util;

import android.util.Log;

import java.text.DecimalFormat;
import java.util.ArrayList;
import com.dantou.model.Point;

/**
 * @author cinhori
 * @date 18-9-2
 * @email lilei93s@163.com
 * @Description 字符串中提取经纬度等信息
 */
public class StringToLatLong {
    public static Point toLatLong(String llString){

        DecimalFormat decimalFormat = new DecimalFormat("0.00000");

        if (llString.startsWith("050082")){

            int id  = Integer.valueOf(llString.substring(8, 12), 16);

            double longitude = Integer.valueOf(llString.substring(18, 26), 16) * 0.00001;
            longitude = Double.parseDouble(decimalFormat.format(longitude));

            double latitude = Integer.valueOf(llString.substring(26, 34), 16) * 0.00001;
            latitude = Double.parseDouble(decimalFormat.format(latitude));

            return new Point(id, latitude, longitude, null);
        }
        return null;
    }

    public static ArrayList<Point> toLatLongs(String llStrings){

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
