package com.example.facerec;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class List extends AppCompatActivity {

    DataBase dbHelper;
    ListView lvData;

    private static final int CM_DELETE_ID = 1;


    final String ATTRIBUTE_NAME = "category";
    final String ATTRIBUTE_DATE = "date";
    final String ATTRIBUTE_NAME_IMAGE = "image";

    Cursor c = null;
    //Оновлення списку
    public void ListUpdate(){
        dbHelper = new DataBase(this);
        Cursor c = null;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String [] columns = new String[] { "name", "date" };
        String orderBy = "_id";
        c = db.query("FaceRec_DB", columns, null, null, null, null, orderBy + " DESC");
        ArrayList<Map<String, Object>> data = new ArrayList<Map<String, Object>>(
                c.getCount());
        Map<String, Object> m;
        if (c != null) {
            if (c.moveToFirst()) {
                int img=0;
                do {
                    for (String cn : c.getColumnNames()) {
                        if (cn.equals("name")){
                            m = new HashMap<String, Object>();
                            m.put(ATTRIBUTE_NAME, c.getString(c.getColumnIndex(cn)));
                            m.put(ATTRIBUTE_DATE, c.getString(c.getColumnIndex(cn)+1));
                            m.put(ATTRIBUTE_NAME_IMAGE,img = 0);
                            data.add(m);
                        }
                    }
                } while (c.moveToNext());
            }
            //  c.close();
        }
        startManagingCursor(c);
        String[] from = { ATTRIBUTE_NAME, ATTRIBUTE_DATE, ATTRIBUTE_NAME_IMAGE};
        int[] to = new int[] {R.id.tvName, R.id.tvTime};

        MySimpleAdapter sAdapter = new MySimpleAdapter(this, data,
                R.layout.item, from, to);
        lvData = (ListView) findViewById(R.id.lvData);
        lvData.setAdapter(sAdapter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        ListUpdate();
        registerForContextMenu(lvData);
    }

    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, CM_DELETE_ID, 0,"Delete item");
    }

    //Вибір елементу списку
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == CM_DELETE_ID) {
            AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            String[] colums = new String[]{"_id"};
            String orderBy = "_id";
            c = db.query("FaceRec_DB", colums, null, null, null, null, orderBy + " DESC");
            int[] indexs = new int[10000];
            int i = 0;
            if (c != null) {
                if (c.moveToFirst()) {
                    do {
                        for (String cn : c.getColumnNames()) {
                            indexs[i] = Integer.parseInt(c.getString(c.getColumnIndex(cn)));
                            i++;
                        }
                    } while (c.moveToNext());
                }}
            String id = "_id";
            db.delete("FaceRec_DB", id + " = " + indexs[acmi.position], null);
            ListUpdate();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    class MySimpleAdapter extends SimpleAdapter {

        public MySimpleAdapter(Context context,
                               ArrayList<Map<String, Object>> data, int resource,
                               String[] from, int[] to) {
            super(context, data, resource, from, to);
        }

        @Override
        public void setViewText(TextView v, String text) {
            super.setViewText(v, text);
        }
    }

}
