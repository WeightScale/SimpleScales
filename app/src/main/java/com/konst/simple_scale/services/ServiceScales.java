package com.konst.simple_scale.services;

import android.app.*;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v7.app.NotificationCompat;
import android.widget.RemoteViews;
import android.widget.Toast;
import com.konst.module.ErrorDeviceException;
import com.konst.module.InterfaceModule;
import com.konst.module.Module;
import com.konst.module.scale.InterfaceCallbackScales;
import com.konst.module.scale.ScaleModule;
import com.konst.simple_scale.*;

public class ServiceScales extends Service {
    private Globals globals;
    private ScaleModule scaleModule;
    private Vibrator vibrator; //вибратор
    private NotificationManager notificationManager;
    private NotificationCompat.Builder builder;
    private BaseReceiver baseReceiver;
    public static final String ACTION_CONNECT_SCALES = "com.konst.simple_scale.ACTION_CONNECT_SCALES";
    public static final String ACTION_POWER_OFF_SCALES = "com.konst.simple_scale.ACTION_POWER_OFF_SCALES";
    public static final String ACTION_OFFSET_SCALES = "com.konst.simple_scale.ACTION_OFFSET_SCALES";
    public static final String ACTION_CLOSED_SCALES = "com.konst.simple_scale.ACTION_CLOSED_SCALES";
    public static final String EXTRA_OBJECT_SCALES = "com.konst.simple_scale.EXTRA_OBJECT_SCALES";
    public static final String EXTRA_SCALES_MODULE = "com.konst.simple_scale.EXTRA_SCALES_MODULE";
    public static final String EXTRA_VERSION = "com.konst.simple_scale.EXTRA_VERSION";
    public static final String EXTRA_DEVICE = "com.konst.simple_scale.EXTRA_DEVICE";
    public static final String EXTRA_BUNDLE = "com.konst.simple_scale.EXTRA_BUNDLE";
    public static final String ACTION_LOAD_SCALES = "com.konst.simple_scale.ACTION_LOAD_SCALES";
    public static final int DEFAULT_NOTIFICATION_ID = 10;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null){
            String action = intent.getAction();
            if (action != null){
                switch (action){
                    case ACTION_CONNECT_SCALES:
                        connectScales(intent);
                        break;
                    case ACTION_POWER_OFF_SCALES:
                        try {scaleModule.powerOff();}catch (Exception e){}
                        stopSelf();
                        break;
                    case ACTION_OFFSET_SCALES:
                        try {scaleModule.setOffsetScale();}catch (Exception e){}
                        break;
                    case ACTION_CLOSED_SCALES:
                        stopSelf();
                        break;
                    default:
                }
            }
        }

        sendNotification(getString(R.string.app_name),getString(R.string.app_name),"Программа \"Весы автомобильные\" ");

        return Service.START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //android.os.Debug.waitForDebugger();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        globals = Globals.getInstance();
        globals.initialize(this);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        baseReceiver = new BaseReceiver(getApplicationContext());
        baseReceiver.register();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        baseReceiver.unregister();
        try{
            scaleModule.stopProcess();
            scaleModule.dettach();
        }catch (Exception e){}
        BluetoothAdapter.getDefaultAdapter().disable();
        while (BluetoothAdapter.getDefaultAdapter().isEnabled());
        //notificationManager.cancel(DEFAULT_NOTIFICATION_ID);
        //todo System.exit(0);
    }

    private void connectScales(Intent intent){
        Bundle bundle = intent.getBundleExtra(EXTRA_BUNDLE);
        if (bundle != null){
            String version = bundle.getString(EXTRA_VERSION);
            String device = bundle.getString(EXTRA_DEVICE);
            try {
                //scaleModule = new ScaleModule(globals.getPackageInfo().versionName, globals.getPreferencesScale().read(getString(R.string.KEY_LAST_SCALES), ""), connectResultCallback);
                //ScaleModule.create(globals.getPackageInfo().versionName, globals.getPreferencesScale().read(getString(R.string.KEY_LAST_SCALES), ""), connectResultCallback);
                ScaleModule.create(getApplicationContext(), version, device, interfaceCallbackScales);
                Toast.makeText(getBaseContext(), R.string.bluetooth_off, Toast.LENGTH_SHORT).show();
                //globals.setScaleModule(scaleModule);
            } catch (Exception e) {
                Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                stopSelf();
            } catch (ErrorDeviceException e) {
                //connectResultCallback.resultConnect(Module.ResultConnect.CONNECT_ERROR, e.getMessage(), null);
                Toast.makeText(getBaseContext(),e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

    }

    final InterfaceCallbackScales interfaceCallbackScales = new InterfaceCallbackScales() {

        /** Сообщение о результате соединения.
         * @param module Модуль с которым соединились. */
        @Override
        public void onCallback(Module module) {
            scaleModule = (ScaleModule)module;
            scaleModule.setStepScale(globals.preferencesScale.read(getString(R.string.KEY_STEP), getResources().getInteger(R.integer.default_step_scale)));
            scaleModule.startProcess();
            globals.setScaleModule(scaleModule);
            globals.getPreferencesScale().write(getString(R.string.KEY_LAST_SCALES), scaleModule.getAddressBluetoothDevice());
        }


        /*@Override
        public void resultConnect(final Module.ResultConnect resultConnect, final String msg, final Object module) {
            *//*runOnUiThread(new Runnable() {
                @Override
                public void run() {*//*
                    switch (resultConnect) {
                        case STATUS_LOAD_OK:
                            vibrator.vibrate(200);
                            scaleModule = (ScaleModule) module;
                            globals.setScaleModule(scaleModule);
                            *//*try {
                                setTitle(getString(R.string.app_name) + " \"" + scaleModule.getNameBluetoothDevice() + "\", v." + scaleModule.getNumVersion()); //установить заголовок
                            } catch (Exception e) {
                                setTitle(getString(R.string.app_name) + " , v." + scaleModule.getNumVersion()); //установить заголовок
                            }*//*
                            globals.getPreferencesScale().write(getString(R.string.KEY_LAST_SCALES), scaleModule.getAddressBluetoothDevice());
                            *//*setupListView();
                            setupWeightView();*//*
                            scaleModule.setTimerNull(globals.getPreferencesScale().read(getString(R.string.KEY_TIMER_NULL), getResources().getInteger(R.integer.default_max_time_auto_null)));
                            scaleModule.setWeightError(globals.getPreferencesScale().read(getString(R.string.KEY_MAX_NULL), getResources().getInteger(R.integer.default_limit_auto_null)));
                            scaleModule.startProcess();
                            //batteryTemperatureCallback = new BatteryTemperatureCallback();
                            //weightCallback = new WeightCallback();
                            //scaleModule.startMeasuringWeight(weightCallback);
                            //scaleModule.startMeasuringBatteryTemperature(batteryTemperatureCallback);
                            //startThread();
                            break;
                        case STATUS_VERSION_UNKNOWN:
                            connectResultCallback.resultConnect(Module.ResultConnect.CONNECT_ERROR, getString(R.string.not_scale), null);
                            break;
                        case STATUS_ATTACH_START:
                            *//*if(dialogSearch != null){
                                if(dialogSearch.isShowing())
                                    break;
                            }
                            dialogSearch = new ProgressDialog(getBaseContext());
                            dialogSearch.setCancelable(true);
                            dialogSearch.setIndeterminate(false);
                            dialogSearch.show();
                            dialogSearch.setContentView(R.layout.custom_progress_dialog);
                            TextView tv1 = (TextView) dialogSearch.findViewById(R.id.textView1);
                            tv1.setText(getString(R.string.Connecting) + '\n' + msg);*//*
                            Intent i = new Intent(getApplicationContext(), ActivityDialog.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(i);
                            setNotifyProgress(getString(R.string.Connecting) + '\n' + msg, true);
                            break;
                        case STATUS_ATTACH_FINISH:
                            *//*if (dialogSearch.isShowing()) {
                                dialogSearch.dismiss();
                            }*//*
                            setNotifyProgress("", false);
                            break;
                        case TERMINAL_ERROR:
                            scaleModule = (ScaleModule)module;
                            globals.setScaleModule(scaleModule);
                            dialog = new AlertDialog.Builder(ServiceScales.this);
                            dialog.setTitle(getString(R.string.preferences_error));
                            dialog.setCancelable(false);
                            dialog.setNegativeButton(getString(R.string.Close), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                    //doubleBackToExitPressedOnce = true;
                                    //onBackPressed();
                                }
                            });
                            dialog.setMessage(msg);
                            Toast.makeText(getBaseContext(), R.string.preferences_error, Toast.LENGTH_SHORT).show();
                            //setTitle(getString(R.string.app_name) + ": " + getString(R.string.preferences_error));
                            dialog.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    startActivity(new Intent(ServiceScales.this, ActivityPreferences.class));
                                    dialogInterface.dismiss();
                                }
                            });
                            dialog.show();
                            break;
                        case MODULE_ERROR:
                            scaleModule = (ScaleModule)module;
                            globals.setScaleModule(scaleModule);
                            dialog = new AlertDialog.Builder(ServiceScales.this);
                            dialog.setTitle("Ошибка в настройках");
                            dialog.setCancelable(false);
                            dialog.setNegativeButton(getString(R.string.Close), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                    //onBackPressed();
                                }
                            });
                            dialog.setMessage("Запросите настройки у администратора. Настройки должен выполнять опытный пользователь. Ошибка(" + msg + ')');
                            Toast.makeText(getBaseContext(), R.string.preferences_error, Toast.LENGTH_SHORT).show();
                            //setTitle(getString(R.string.app_name) + ": админ настройки неправельные");
                            dialog.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    startActivity(new Intent(ServiceScales.this, ActivityTuning.class));
                                    dialogInterface.dismiss();
                                }
                            });
                            dialog.show();
                            break;
                        case CONNECT_ERROR:
                            *//*setTitle(getString(R.string.app_name) + ' ' + getString(R.string.NO_CONNECT)); //установить заголовок
                            layoutScale.setVisibility(View.INVISIBLE);*//*
                            Intent intent = new Intent(getBaseContext(), ActivitySearch.class);
                            intent.putExtra("message", msg);
                            //startActivityForResult(intent, REQUEST_SEARCH_SCALE);
                            break;
                        default:
                    }
                //}
            //});

        }*/

        /*@Override
        public void eventData(final ScaleModule.ResultWeight what, final ObjectScales obj) {
            *//*runOnUiThread(new Runnable() {
                @Override
                public void run() {*//*
                    //ObjectScales objectScales = (ObjectScales) msg.obj;
                    if (obj == null)
                        return;
                    *//*moduleWeight = getWeightToStepMeasuring(obj.getWeight());
                    final String textWeight = String.valueOf(moduleWeight);
                    switch (what) {
                        case WEIGHT_NORMAL:
                            w = new SpannableStringBuilder(textWeight);
                            //w.setSpan(new AbsoluteSizeSpan(getResources().getDimensionPixelSize(R.dimen.text_big)), 0, w.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                            w.setSpan(new ForegroundColorSpan(Color.WHITE), 0, w.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                            w.append(textKg);
                            progressBarWeight.setProgress(obj.getTenzoSensor());
                            bounds = progressBarWeight.getProgressDrawable().getBounds();
                            progressBarWeight.setProgressDrawable(dProgressWeight);
                            progressBarWeight.getProgressDrawable().setBounds(bounds);
                            break;
                        case WEIGHT_LIMIT:
                            w = new SpannableStringBuilder(textWeight);
                            //w.setSpan(new AbsoluteSizeSpan(getResources().getDimensionPixelSize(R.dimen.text_big)), 0, w.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                            w.setSpan(new ForegroundColorSpan(Color.RED), 0, w.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                            w.append(textKg);
                            progressBarWeight.setProgress(obj.getTenzoSensor());
                            bounds = progressBarWeight.getProgressDrawable().getBounds();
                            progressBarWeight.setProgressDrawable(dWeightDanger);
                            progressBarWeight.getProgressDrawable().setBounds(bounds);
                            break;
                        case WEIGHT_MARGIN:
                            w = new SpannableStringBuilder(String.valueOf(moduleWeight));
                            //w.setSpan(new AbsoluteSizeSpan(getResources().getDimensionPixelSize(R.dimen.text_big)), 0, w.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                            w.setSpan(new ForegroundColorSpan(Color.RED), 0, w.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                            progressBarWeight.setProgress(obj.getTenzoSensor());
                            vibrator.vibrate(100);
                            break;
                        case WEIGHT_ERROR:
                            w = new SpannableStringBuilder("- - -");
                            //w.setSpan(new AbsoluteSizeSpan(getResources().getDimensionPixelSize(R.dimen.text_big)), 0, w.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                            w.setSpan(new ForegroundColorSpan(Color.RED), 0, w.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                            moduleWeight = 0;
                            progressBarWeight.setProgress(0);
                            break;
                        default:
                    }
                    weightTextView.setText(w, TextView.BufferType.SPANNABLE);
                    textViewTemperature.setText(obj.getTemperature() + "°C");
                    if (obj.getBattery() > 15) {
                        textViewBattery.setText(*//**//*"заряд батареи " +*//**//* obj.getBattery() + "%" *//**//*+ "   " + temperature + '°' + 'C'*//**//*);
                        textViewBattery.setTextColor(Color.WHITE);
                        textViewBattery.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_battery, 0, 0, 0);
                    } else if (obj.getBattery() >= 0) {
                        textViewBattery.setText(*//**//*"заряд низкий!!! " +*//**//* obj.getBattery() + "%" *//**//*+ "   " + temperature + '°' + 'C'*//**//*);
                        textViewBattery.setTextColor(Color.RED);
                        textViewBattery.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_battery_red, 0, 0, 0);
                    }else {
                        textViewBattery.setText("нет данных!!!" *//**//*+ "   " + temperature + '°' + 'C'*//**//*);
                        textViewBattery.setTextColor(Color.BLUE);
                        textViewBattery.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_battery_red, 0, 0, 0);
                    }*//*
            //    }
            //});
        }*/
    };

    public void sendNotification(String Ticker, String Title, String Text) {

        //These three lines makes Notification to open main activity after clicking on it
        Intent notificationIntent = new Intent(this, ActivityMain.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder = new NotificationCompat.Builder(this);
        builder.setContentIntent(contentIntent)
                .setOngoing(true)   //Can't be swiped out
                .setSmallIcon(R.drawable.ic_stat_scales)
                //.setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.large))   // большая картинка
                .setTicker(Ticker)
                .setContentTitle(Title) //Заголовок
                .setContentText(Text) // Текст уведомления
                //.setProgress(100,50, false)
                .setWhen(System.currentTimeMillis());
        Notification notification = Build.VERSION.SDK_INT <= 15 ? builder.getNotification() : builder.build();
        notificationManager.notify(DEFAULT_NOTIFICATION_ID, notification);
        //startForeground(DEFAULT_NOTIFICATION_ID, notification);
    }

    public void setNotifyContentText(String text){
        builder.setContentText(text);
        notificationManager.notify(DEFAULT_NOTIFICATION_ID, builder.build());
    }

    public void setNotifyProgress(String text, boolean f){
        if (f){
            final RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.custom_notify_dialog);
            remoteViews.setTextViewText(R.id.textView1, "Custom notification");
            builder.setContent(remoteViews);
        }else {
            builder.setContent(null);
            builder.setContentText(text);
        }
        notificationManager.notify(DEFAULT_NOTIFICATION_ID, builder.build());
    }

    public void sendNotifySubText(String text){
        builder.setSubText("Вес = "+text);
        notificationManager.notify(DEFAULT_NOTIFICATION_ID, builder.build());
    }

    class BaseReceiver extends BroadcastReceiver{
        private Context context;
        private IntentFilter intentFilter;
        private boolean isRegistered;

        BaseReceiver(Context context){
            this.context = context;
            intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        switch (BluetoothAdapter.getDefaultAdapter().getState()) {
                            case BluetoothAdapter.STATE_OFF:
                                Toast.makeText(getBaseContext(), R.string.bluetooth_off, Toast.LENGTH_SHORT).show();
                                new Internet(getApplicationContext()).turnOnWiFiConnection(false);
                                BluetoothAdapter.getDefaultAdapter().enable();
                            break;
                            case BluetoothAdapter.STATE_ON:
                                Toast.makeText(getBaseContext(), R.string.bluetooth_on, Toast.LENGTH_SHORT).show();
                            break;
                            default:
                                break;
                        }
                    break;
                    default:
                }
            }
        }

        public void register() {
            isRegistered = true;
            context.registerReceiver(this, intentFilter);
        }

        public void unregister() {
            if (isRegistered) {
                context.unregisterReceiver(this);  // edited
                isRegistered = false;
            }
        }
    }
}
