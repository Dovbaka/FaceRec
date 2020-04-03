package com.example.facerec;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class DataBase extends SQLiteOpenHelper {

    public DataBase(Context context) {
        // конструктор суперкласса
        super(context, "FaceRec_DataBase", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("myLog", "--- onCreate database ---");
        db.execSQL("create table FaceRec_DB ("
                + "_id integer primary key autoincrement,"
                + "name text,"
                + "date text"
                + ");");
    }

    public boolean checkForTables(String table_name){

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + table_name, null);

        if(cursor != null){

            cursor.moveToFirst();

            int count = cursor.getInt(0);

            if(count > 0){
                return true;
            }

            cursor.close();
        }

        return false;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
