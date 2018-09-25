package com.dantou.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

/**
 * @author cinhori
 * @date 18-8-27
 * @email lilei93s@163.com
 * @Description 节点bean
 */
public class Point implements Parcelable{
    public final static int SAFE_LOCATED = 0;
    public final static int UNSAFE_LOCATED = 1;
    public final static int SAFE_UNLOCATED = 2;
    public final static int UNSAFE_UNLOCATED = 3;
    private int id;
    private double latitude;
    private double longitude;
    private Date date;
    private int status;

    public Point() {
    }

    public Point(int id, double latitude, double longitude, Date date, int status) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.date = date;
        this.status = status;
    }

    protected Point(Parcel source){
        id = source.readInt();
        latitude = source.readDouble();
        longitude = source.readDouble();
        date = (Date)source.readSerializable();
        status = source.readInt();
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

    @Override
    public String toString() {
        return "Point{" +
                "id=" + id +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", date=" + date +
                ", status=" + status +
                '}' + "\n";
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
        dest.writeInt(status);
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

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
