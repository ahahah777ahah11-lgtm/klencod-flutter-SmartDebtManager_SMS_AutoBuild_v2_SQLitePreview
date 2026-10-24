package com.smartdebt.app;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.util.Base64;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
  private WebView web;
  @Override public void onCreate(Bundle b) {
    super.onCreate(b);
    getWindow().setStatusBarColor(android.graphics.Color.rgb(16,37,68));
    web = new WebView(this);
    WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setAllowFileAccess(true);
    web.setWebChromeClient(new WebChromeClient()); web.setWebViewClient(new WebViewClient());
    web.addJavascriptInterface(new SqliteBridge(), "AndroidDB");
    setContentView(web); web.loadUrl("file:///android_asset/index.html");
  }
  public class SqliteBridge {
    @JavascriptInterface public String inspectSqliteBase64(String base64) {
      File temp=null; SQLiteDatabase db=null;
      try {
        byte[] bytes=Base64.decode(base64,Base64.DEFAULT);
        temp=new File(getCacheDir(),"market-import.db");
        try(FileOutputStream out=new FileOutputStream(temp)){out.write(bytes);}
        db=SQLiteDatabase.openDatabase(temp.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY);
        JSONObject result=new JSONObject();
        JSONArray tables=new JSONArray();
        Cursor tc=db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name",null);
        while(tc.moveToNext()) tables.put(tc.getString(0)); tc.close();
        result.put("tables",tables);
        result.put("integrity", scalar(db,"PRAGMA integrity_check"));
        result.put("customers", readTable(db,"customers",188));
        result.put("transactions",readTable(db,"transactions",5000));
        result.put("customerCount",count(db,"customers"));
        result.put("transactionCount",count(db,"transactions"));
        return result.toString();
      } catch(Exception e) {
        JSONObject err=new JSONObject(); try{err.put("error",e.getMessage()==null?"تعذر قراءة قاعدة SQLite":e.getMessage());}catch(Exception ignored){}
        return err.toString();
      } finally { if(db!=null) db.close(); if(temp!=null)temp.delete(); }
    }
    private String scalar(SQLiteDatabase db,String sql){Cursor c=null;try{c=db.rawQuery(sql,null);return c.moveToFirst()?c.getString(0):"";}catch(Exception e){return "غير متاح";}finally{if(c!=null)c.close();}}
    private int count(SQLiteDatabase db,String table){try{Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+table,null);int n=c.moveToFirst()?c.getInt(0):0;c.close();return n;}catch(Exception e){return -1;}}
    private JSONArray readTable(SQLiteDatabase db,String table,int max) throws Exception {
      JSONArray arr=new JSONArray(); Cursor c=null;
      try {
        c=db.rawQuery("SELECT * FROM \""+table+"\" LIMIT "+max,null);
        String[] cols=c.getColumnNames();
        while(c.moveToNext()){
          JSONObject row=new JSONObject();
          for(int i=0;i<cols.length;i++){
            if(c.isNull(i)) row.put(cols[i],JSONObject.NULL);
            else switch(c.getType(i)){
              case Cursor.FIELD_TYPE_INTEGER: row.put(cols[i],c.getLong(i)); break;
              case Cursor.FIELD_TYPE_FLOAT: row.put(cols[i],c.getDouble(i)); break;
              default: row.put(cols[i],c.getString(i));
            }
          }
          arr.put(row);
        }
      } finally {if(c!=null)c.close();}
      return arr;
    }
  }
  @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
}
