//Ищет весы
package com.konst.simple_scale.bootloader;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import com.konst.module.InterfaceResultCallback;
import com.konst.module.Module;
import com.konst.module.boot.BootModule;
import com.konst.module.scale.ObjectScales;
import com.konst.module.scale.ScaleModule;
import com.konst.simple_scale.Internet;
import com.konst.simple_scale.R;
import com.konst.simple_scale.services.ServiceScales;

public class ActivityConnect extends Activity implements View.OnClickListener {

    //==================================================================================================================

    private Vibrator vibrator; //вибратор

    private BroadcastReceiver broadcastReceiver; //приёмник намерений
    //private final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); //блютуз адаптер
    //private BluetoothDevice bluetoothDevice;
    private TextView textViewLog; //лог событий
    //private LinearLayout linearScreen;//лайаут для экрана показывать когда загрузились настройки
    private BootModule bootModule;

    public static int versionNumber;
    public static String versionName;

    private boolean doubleBackToExitPressedOnce;

    //==================================================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.connect);
        textViewLog = (TextView) findViewById(R.id.textLog);
        try {
            //bootModule = new BootModule("bootloader", getIntent().getStringExtra("address"), connectResultCallback);
            Intent intent = new Intent(getApplicationContext(), ServiceScales.class);
            intent.setAction(ServiceScales.ACTION_CONNECT_SCALES);
            Bundle bundle = new Bundle();
            bundle.putString(ServiceScales.EXTRA_VERSION, "bootloader");
            bundle.putString(ServiceScales.EXTRA_DEVICE, getIntent().getStringExtra("address"));
            intent.putExtra(ServiceScales.EXTRA_BUNDLE, bundle);
            startService(intent);
            //BootModule.create(getApplicationContext(), "bootloader", getIntent().getStringExtra("address")/*, connectResultCallback*/);
            log(R.string.bluetooth_off, true);
            setupScale();
        } catch (Exception e) {
            Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        } /*catch (ErrorDeviceException e) {
            e.printStackTrace();
        }*/
    }

    //==================================================================================================================
    private void exit() {
        if (BluetoothAdapter.getDefaultAdapter().isDiscovering()) {
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        }
        if (broadcastReceiver != null)
            unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        exit();
    }

    //==================================================================================================================
    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            //exit();
            return;
        }
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        doubleBackToExitPressedOnce = true;
        Toast.makeText(this, R.string.press_again_to_exit /*Please click BACK again to exit*/, Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;

            }
        }, 2000);
    }

    //==================================================================================================================
    void log(int resource) { //для ресурсов
        textViewLog.setText(getString(resource) + '\n' + textViewLog.getText());
    }

    //==================================================================================================================
    public void log(String string) { //для текста
        textViewLog.setText(string + '\n' + textViewLog.getText());
    }

    //==================================================================================================================
    void log(int resource, boolean toast) { //для текста
        textViewLog.setText(getString(resource) + '\n' + textViewLog.getText());
        if (toast) {
            Toast.makeText(getBaseContext(), resource, Toast.LENGTH_SHORT).show();
        }
    }

    //==================================================================================================================
    void log(int resource, String str) { //для ресурсов с текстовым дополнением
        textViewLog.setText(getString(resource) + ' ' + str + '\n' + textViewLog.getText());
    }

    void setupScale() {
        /*Window window = getWindow();
        window.requestFeature(Window.FEATURE_CUSTOM_TITLE);*/
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);



        setProgressBarIndeterminateVisibility(true);

        //linearScreen = (LinearLayout) findViewById(R.id.searchScreen);
        //linearScreen.setVisibility(View.INVISIBLE);



        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo != null) {
            if (networkInfo.isAvailable()) //Если используется
            {
                new Internet(this).turnOnWiFiConnection(false); // для телефонов у которых один модуль wifi и bluetooth
            }
        }

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) { //обработчик Bluetooth
                String action = intent.getAction();
                if (action != null) {
                    switch (action) {
                        case BluetoothAdapter.ACTION_STATE_CHANGED:
                            if (BluetoothAdapter.getDefaultAdapter().getState() == BluetoothAdapter.STATE_OFF) {
                                log(R.string.bluetooth_off);
                                BluetoothAdapter.getDefaultAdapter().enable();
                            } else if (BluetoothAdapter.getDefaultAdapter().getState() == BluetoothAdapter.STATE_TURNING_ON) {
                                log(R.string.bluetooth_turning_on, true);
                            } else if (BluetoothAdapter.getDefaultAdapter().getState() == BluetoothAdapter.STATE_ON) {
                                log(R.string.bluetooth_on, true);
                            }
                            break;
                        case BluetoothDevice.ACTION_ACL_DISCONNECTED:  //устройство отсоеденено
                            vibrator.vibrate(200);
                            log(R.string.bluetooth_disconnected);
                            break;
                        case BluetoothDevice.ACTION_ACL_CONNECTED:  //найдено соеденено
                            vibrator.vibrate(200);
                            log(R.string.bluetooth_connected);
                            break;
                        case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:  //поиск завершён
                            setTitle(getString(R.string.app_name) + " \"" + versionName + "\", v." + versionNumber); //установить заголовок

                            setProgressBarIndeterminateVisibility(false);
                            break;
                        default:
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(broadcastReceiver, intentFilter);

        //}

        PackageInfo packageInfo = null;
        try {
            PackageManager packageManager = getPackageManager();
            if (packageManager != null) {
                packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
            }
        } catch (PackageManager.NameNotFoundException e) {
            log(e.getMessage());
        }

        if (packageInfo != null) {
            versionNumber = packageInfo.versionCode;
        }
        if (packageInfo != null) {
            versionName = packageInfo.versionName;
        }

        setTitle(getString(R.string.app_name) + " \"" + versionName + "\", v." + versionNumber);                        //установить заголовок
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        findViewById(R.id.buttonSearchBluetooth).setOnClickListener(this);
        findViewById(R.id.buttonBack).setOnClickListener(this);

        //scaleModule.initBoot("bootloader", bluetoothDevice.getAddress());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonBack:
                onBackPressed();
                break;
            case R.id.buttonSearchBluetooth:
                try {
                    /*bootModule.init(getIntent().getStringExtra("address"));
                    bootModule.attach();*/
                } catch (Exception e) {
                    connectResultCallback.resultConnect(Module.ResultConnect.CONNECT_ERROR, e.getMessage(), null);
                }
                break;
            default:
        }
    }

    final InterfaceResultCallback connectResultCallback = new InterfaceResultCallback() {
        @Override
        public void resultConnect(Module.ResultConnect result, String msg, Object module) {
            switch (result) {
                case STATUS_LOAD_OK:
                    setResult(RESULT_OK, new Intent());
                    finish();
                    break;
                default:
            }
        }

        @Override
        public void eventData(ScaleModule.ResultWeight what, ObjectScales obj) {

        }

    };

}