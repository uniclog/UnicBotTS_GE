package local.bwg;

import local.bwg.model.JsonConvertor;
import org.json.JSONObject;

import java.io.Serializable;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

public class User implements Serializable {
    private static final long serialVersionUID = -3295590026052296592L;
    private static final Logger logger = Logger.getLogger(User.class.getName());

    private String uName = "unknown";
    private int uID = 0;
    private String uUnicID = "";
    private int uPrivilegeLevel = 0;
    private long time;
    private long totalTime = 0;
    private String wakeUp = "";

    private boolean loginnotifyStatus = false;

    public JSONObject getJSONObject() {
        JSONObject json = new JSONObject();
        json.put("uName", this.getuName());
        json.put("uID", this.isuID());
        json.put("uUnicID", this.getuUnicID());
        json.put("uPrivilegeLevel", this.getuPrivilegeLevel());
        json.put("time", this.getTime());
        json.put("totalTime", this.getTotalTime());
        json.put("wakeUp", this.getWakeUp());
        json.put("isLoginnotifyStatus", this.isLoginnotifyStatus());
        return json;
    }

    public User(String name, int id, String uUnicID){
        this.uName = name;
        this.uID = id;
        this.uUnicID = uUnicID;

        updateTime();
    }

    public void updateTime() {
        Date date = new Date();
        SimpleDateFormat formatForDateNow
                = new SimpleDateFormat("HH:mm:ss");
        this.time = date.getTime();//formatForDateNow.format(date);
    }

    public void updateTotalTime() {
        Date date = new Date();
        SimpleDateFormat format
                = new SimpleDateFormat("HH:mm:ss");

        long diffTime = date.getTime() - this.time;
        //String diffTime = calcTime(this.time, format.format(date));

        this.totalTime += diffTime;
    }

    private String calcTime(String t1, String t2) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        Date d1 = null;
        Date d2 = null;
        try {
            d1 = format.parse(t1);
            d2 = format.parse(t2);
            //in milliseconds
            long diff = d2.getTime() - d1.getTime();
            long diffSeconds = diff / 1000 % 60;
            long diffMinutes = diff / (60 * 1000) % 60;
            long diffHours = diff / (60 * 60 * 1000) % 24;
            long diffDays = diff / (24 * 60 * 60 * 1000);
            return diffHours + ":" + diffMinutes + ":" + diffSeconds;
        } catch (Exception e) {
            return "00:00:00";
        }
    }

    public String getTotalTimeString() {
        Date date = new Date();
        SimpleDateFormat format
                = new SimpleDateFormat("HH:mm:ss");

        long diffTime = date.getTime() - this.time;

        long diff = this.totalTime + diffTime;
        long diffSeconds = diff / 1000 % 60;
        long diffMinutes = diff / (60 * 1000) % 60;
        long diffHours = diff / (60 * 60 * 1000) ;
        long diffDays = diff / (24 * 60 * 60 * 1000);
        return diffHours + " hour " + diffMinutes + " min " + diffSeconds +" sec";
        //return this.totalTime;
    }

    public String getTotalTimeStringNoCalc() {
        long diff = this.totalTime;
        long diffSeconds = diff / 1000 % 60;
        long diffMinutes = diff / (60 * 1000) % 60;
        long diffHours = diff / (60 * 60 * 1000) ;
        long diffDays = diff / (24 * 60 * 60 * 1000);
        return diffHours + " hour " + diffMinutes + " min " + diffSeconds +" sec";
        //return this.totalTime;
    }

    public void setuName(String uName) {
        this.uName = uName;
    }

    public void setuID(int uID) {
        this.uID = uID;
    }

    public int isuID() {
        return uID;
    }

    public String getuName() {
        return uName;
    }

    public long getTime() {
        return time;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public boolean isLoginnotifyStatus() {
        return loginnotifyStatus;
    }

    public String getuUnicID() {
        return uUnicID;
    }

    public String getWakeUp() {
        return wakeUp;
    }

    public void setWakeUp(String wakeUp) {
        this.wakeUp = wakeUp;
    }

    public void setuPrivilegeLevel(int uPrivilegeLevel) {
        this.uPrivilegeLevel = uPrivilegeLevel;
    }

    public int getuPrivilegeLevel() {
        return uPrivilegeLevel;
    }

    public void setLoginnotifyStatus(boolean loginnotifyStatus) {
        this.loginnotifyStatus = loginnotifyStatus;
    }

    public void setPrivilegeLevel(int level) {
        this.uPrivilegeLevel = level;
    }
}