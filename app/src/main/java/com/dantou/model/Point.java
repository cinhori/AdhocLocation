package com.dantou.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * @author cinhori
 * @date 18-8-27
 * @email lilei93s@163.com
 * @Description 节点bean
 */
public class Point implements Parcelable{
    private int id;
    private double latitude;
    private double longitude;
    private Date date;
    private Boolean safe;
    private Boolean located;

    public Point() {
    }

    public Point(int id, double latitude, double longitude, Date date, Boolean safe, Boolean located) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.date = date;
        this.safe = safe;
        this.located = located;
    }

    protected Point(Parcel source){
        id = source.readInt();
        latitude = source.readDouble();
        longitude = source.readDouble();
        date = (Date)source.readSerializable();
        safe = source.readByte() != 0;
        located = source.readByte() != 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Boolean isSafe() {
        return safe;
    }

    public void setSafe(Boolean safe) {
        this.safe = safe;
    }

    public Boolean isLocated() {
        return located;
    }

    public void setLocated(Boolean located) {
        this.located = located;
    }

    @Override
    public String toString() {
        return "Point{" +
                "id=" + id +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", date=" + date +
                ", safe=" + safe +
                ", located=" + located +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeSerializable(date);
        dest.writeByte((byte)(safe ? 1 : 0));
        dest.writeByte((byte)(located ? 1 : 0));
    }

    public static final Creator<Point> CREATOR = new Creator<Point>() {
        @Override
        public Point createFromParcel(Parcel source) {
            return new Point(source);
        }

        @Override
        public Point[] newArray(int size) {
            return new Point[size];
        }
    };

    /**
     * 数据解析
     * @param hexData
     * @return
     */
    public static Point parse(String hexData){
        DecimalFormat decimalFormat = new DecimalFormat("0.00000");

        if (hexData.startsWith("050082")){

            int id  = Integer.valueOf(hexData.substring(8, 12), 16);

            int port = Integer.valueOf(hexData.substring(16, 18), 16);

            double longitude = Integer.valueOf(hexData.substring(18, 26), 16) * 0.00001;
            longitude = Double.parseDouble(decimalFormat.format(longitude));
            double latitude = Integer.valueOf(hexData.substring(26, 34), 16) * 0.00001;
            latitude = Double.parseDouble(decimalFormat.format(latitude));

            int year = Integer.valueOf(hexData.substring(34, 36), 16);
            int month = Integer.valueOf(hexData.substring(36, 38), 16);
            int day = Integer.valueOf(hexData.substring(38, 40), 16);
            int hour = Integer.valueOf(hexData.substring(40, 42), 16);
            int minute = Integer.valueOf(hexData.substring(42, 44), 16);
            int second = Integer.valueOf(hexData.substring(44, 46), 16);
            String dateString = "20" + year +
                    "-" + month +
                    "-" + day +
                    " " + hour +
                    ":" + minute +
                    ":" + second;

            Boolean safe, located;
            if (latitude <= 0 || longitude <= 0) {
                located = false;
            }else {
                located = true;
            }

            switch (port) {
                case 1:
                    safe = true;
                    break;
                case 2:
                    safe = false;
                    break;
                default:
                    safe = true;
                    break;
            }

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            try {
                return new Point(id, latitude, longitude, simpleDateFormat.parse(dateString), safe, located);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static ArrayList<Point> toPoints(String llStrings){

        String[] lls = llStrings.split("\n");
        ArrayList<Point> latLongList = new ArrayList<>();
        for(String s : lls){
            Log.d("切分后的字符串", s);
            if(parse(s) != null){
                latLongList.add(parse(s.trim()));
            }
        }
        return latLongList;
    }

}
