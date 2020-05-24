package com.example.facerec;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.bytedeco.javacpp.opencv_core.finish;


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

    //Запис в БД
    public void InsertToDB(String name, Context mContext){
        SimpleDateFormat DateS = new SimpleDateFormat("dd.MM.yyyy");
        String DateNow = DateS.format(new Date());
        if(!isRecordExist("FaceRec_DB", "name", name, "date", DateNow)){
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues cv = new ContentValues();
            Log.d("DBLOG", "--- Insert in table: ---");
            cv.put("name", name);
            cv.put("date", DateNow);
            long rowID = db.insert("FaceRec_DB", null, cv);
            this.close();
            finish();
        }
        else {
            Toast.makeText(mContext, "You are already registered today", Toast.LENGTH_SHORT).show();
        }
    }

    //Перевірка існування запису
    private boolean isRecordExist(String tableName, String field1, String value1,
                                  String field2, String value2) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + tableName + " WHERE " + field1 + " = '" + value1 + "' AND " +
                field2 + " = '" + value2 + "'";
        Cursor c = db.rawQuery(query, null);
        if (c.moveToFirst()) {
            c.close();
            return true;
        }
        c.close();
        return false;
    }
}
