//Простой класс настроек
package com.konst.simple_scale;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public static final String PREFERENCES = "preferences"; //настройки общии для весов
    public static final String PREF_UPDATE = "pref_update";    //настройки сохраненные при обновлении прошивки

    public static final String KEY_NUMBER_SMS = "number_sms";
    public static final String KEY_SENT_SERVICE = "sent_service";

    Preferences(Context context, String name) {
        load(context.getSharedPreferences(name, Context.MODE_PRIVATE)); //загрузить настройки
    }

    public Preferences(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        editor = sharedPreferences.edit();
        editor.apply();
    }

    public void load(SharedPreferences sp) {
        sharedPreferences = sp;
        editor = sp.edit();
        editor.apply();
    }

    public void write(String key, String value) {
        editor.putString(key, value);
        editor.commit();
    }

    public void write(String key, int value) {
        editor.putInt(key, value);
        editor.commit();
    }

    public void write(String key, float value) {
        editor.putFloat(key, value);
        editor.commit();
    }

    public void write(String key, boolean value) {
        editor.putBoolean(key, value);
        editor.commit();
    }

    public String read(String key, String def) {
        return sharedPreferences.getString(key, def);
    }

    boolean read(String key, boolean def) {
        return sharedPreferences.getBoolean(key, def);
    }

    public int read(String key, int in) {
        return sharedPreferences.getInt(key, in);
    }

    public float read(String key, float in) { return sharedPreferences.getFloat(key, in); }

    boolean contains(String key) {
        return sharedPreferences.contains(key);
    }

    void remove(String key) {
        editor.remove(key);
        editor.commit();
    }
}