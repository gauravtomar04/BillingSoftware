package com.mylifejournal.app;
import android.content.*;import android.database.*;import android.database.sqlite.*;import org.json.*;import java.text.SimpleDateFormat;import java.util.*;
public class JournalDb extends SQLiteOpenHelper{
 public JournalDb(Context c){super(c,"mylifejournal.db",null,1);}
 public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE tasks(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,done INTEGER DEFAULT 0,created TEXT)");d.execSQL("CREATE TABLE journals(id INTEGER PRIMARY KEY AUTOINCREMENT,good TEXT,hard TEXT,tomorrow TEXT,created TEXT)");d.execSQL("CREATE TABLE moods(id INTEGER PRIMARY KEY AUTOINCREMENT,mood TEXT,created TEXT)");}
 public void onUpgrade(SQLiteDatabase d,int o,int n){}
 String today(){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());}
 public void mood(String m){ContentValues v=new ContentValues();v.put("mood",m);v.put("created",today());getWritableDatabase().insert("moods",null,v);}
 public void journal(String g,String h,String t){ContentValues v=new ContentValues();v.put("good",g);v.put("hard",h);v.put("tomorrow",t);v.put("created",today());getWritableDatabase().insert("journals",null,v);}
 public String latest(){Cursor c=getReadableDatabase().rawQuery("SELECT good,hard,tomorrow,created FROM journals ORDER BY id DESC LIMIT 1",null);try{if(!c.moveToFirst())return "";return c.getString(3)+"\nWent well: "+c.getString(0)+"\nDifficult: "+c.getString(1)+"\nTomorrow: "+c.getString(2);}finally{c.close();}}
 public void add(String s){ContentValues v=new ContentValues();v.put("name",s);v.put("done",0);v.put("created",today());getWritableDatabase().insert("tasks",null,v);}
 public void check(int id,boolean yes){ContentValues v=new ContentValues();v.put("done",yes?1:0);getWritableDatabase().update("tasks",v,"id=?",new String[]{""+id});}
 public JSONArray tasks(){JSONArray a=new JSONArray();Cursor c=getReadableDatabase().rawQuery("SELECT id,name,done FROM tasks WHERE created=? ORDER BY id DESC",new String[]{today()});try{while(c.moveToNext()){JSONObject o=new JSONObject();o.put("id",c.getInt(0));o.put("name",c.getString(1));o.put("done",c.getInt(2));a.put(o);}}catch(Exception e){}finally{c.close();}return a;}
 public int total(){return count("SELECT COUNT(*) FROM tasks WHERE created=?",new String[]{today()});}public int done(){return count("SELECT COUNT(*) FROM tasks WHERE created=? AND done=1",new String[]{today()});}
 public int weekTasks(){return count("SELECT COUNT(*) FROM tasks WHERE created>=date('now','-6 day')",null);}public int weekDone(){return count("SELECT COUNT(*) FROM tasks WHERE done=1 AND created>=date('now','-6 day')",null);}public int weekJournals(){return count("SELECT COUNT(*) FROM journals WHERE created>=date('now','-6 day')",null);}
 int count(String q,String[] a){Cursor c=getReadableDatabase().rawQuery(q,a);try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}}
}