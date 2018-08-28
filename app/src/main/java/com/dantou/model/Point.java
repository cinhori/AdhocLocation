package com.dantou.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

public class Point implements Parcelable{
    private int id;
    private double latitude;
    private double longitude;
    private Date date;

    public Point() {
    }

    public Point(int id, double latitude, double longitude, Date date) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.date = date;
    }

    protected Point(Parcel source){
        id = source.readInt();
        latitude = source.readDouble();
        longitude = source.readDouble();
        date = (Date)source.readSerializable();
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
}
