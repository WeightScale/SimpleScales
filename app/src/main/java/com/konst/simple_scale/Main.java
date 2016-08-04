package com.konst.simple_scale;

import android.app.Application;
import android.content.Intent;
import com.konst.simple_scale.services.ServiceScales;

/**
 * Created by Kostya on 04.08.2016.
 */
public class Main extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        startService(new Intent(getApplicationContext(), ServiceScales.class).setAction(ServiceScales.ACTION_CONNECT_SCALES));
    }
}
