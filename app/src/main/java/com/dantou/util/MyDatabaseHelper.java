package com.dantou.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * @author cinhori
 * @date 18-9-26
 * @email lilei93s@163.com
 * @Description
 */
public class MyDatabaseHelper extends SQLiteOpenHelper {
    public static final String CREATE_POINTS = "create table points ("
            + "id integer primary key autoincrement, "
            + "user_id integer, "
            + "latitude text, "
            + "longitude text, "
            + "date text, "
            + "safe integer, "
            + "located integer)";
    public static final String DB_NAME = "pointset.db";


    private Context myContext;

    public MyDatabaseHelper(Context context, String name,
                            SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        this.myContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_POINTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void insert(SQLiteDatabase db, ContentValues values){
        db.insert("points", null, values);
    }

    public Cursor query(SQLiteDatabase db){
        return db.query("points", null, null, null, null, null, null);
    }

    public void deleteAll(SQLiteDatabase db){
        db.delete("points", null, null);
    }
}
