package com.zunozap.lang;

import java.util.ArrayList;

public enum Lang {

    HTTPS, DIS_PL, OFFLINE, MAL, LOAD, GO, NO_PL, CLEAR_OFFLNE, SETT, ABOUT, LANG, COMPACT;

    public String tl;
    public static ArrayList<Runnable> l2 = new ArrayList<>();

    public static final void a(Runnable a) { l2.add(a); }

    public static final void b(Runnable a) {
        a(a);
        a.run();
    }
    
    public static Lang get(String s) {
        try {
            return valueOf(s);
        } catch (Exception e) { return null; }
    }

    public static String from(String s) {
        Lang l = null;
        return null == (l = get(s)) ? s : l.tl;
    }

}