package com.manroid.freemusic;

public class Utils {

    public static String formatTime(int milliseconds) {
        String ret = "";
        int seconds = (milliseconds / 1000) % 60 ;
        int minutes = ((milliseconds / (1000*60)) % 60);
        int hours   = ((milliseconds / (1000*60*60)) % 24);
        if(hours>0) ret += hours+":";
        ret += minutes<10 ? "0"+minutes+":" : minutes+":";
        ret += seconds<10 ? "0"+seconds : seconds+"";
        return ret;
    }

}
