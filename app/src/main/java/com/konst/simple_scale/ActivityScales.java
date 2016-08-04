package com.konst.simple_scale;

import android.app.*;
import android.bluetooth.BluetoothAdapter;
import android.content.*;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.TextAppearanceSpan;
import android.view.*;
import android.widget.*;
import com.konst.module.ErrorDeviceException;
import com.konst.module.InterfaceModule;
import com.konst.module.InterfaceResultCallback;
import com.konst.module.Module;
import com.konst.module.scale.ObjectScales;
import com.konst.module.scale.ScaleModule;
import com.konst.simple_scale.services.ServiceScales;
import com.konst.simple_scale.settings.ActivityPreferences;
import com.konst.simple_scale.settings.ActivityTuning;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ActivityScales extends Activity implements View.OnClickListener, Runnable {
    private Globals globals;
    private ScaleModule scaleModule;
    private SpannableStringBuilder textKg;
    private SpannableStringBuilder textBattery;
    private TextView textViewBattery, textViewTemperature;
    private ListView listView;
    private ArrayList<WeightObject> arrayList;
    private ArrayAdapter<WeightObject> customListAdapter;
    private ProgressBar progressBarStable;
    private ProgressBar progressBarWeight;
    private TextView weightTextView;
    private Drawable dProgressWeight, dWeightDanger;
    private SimpleGestureFilter detectorWeightView;
    private ImageView buttonFinish, imageViewWait;
    private Vibrator vibrator; //вибратор
    private LinearLayout layoutScale;
    private BaseReceiver baseReceiver; //приёмник намерений

    //private BatteryTemperatureCallback batteryTemperatureCallback;
    //private WeightCallback weightCallback;

    public int numStable;
    private int moduleWeight;
    //int moduleSensorValue;
    protected int tempWeight;
    private Thread threadAutoWeight;
    boolean running;
    /**
     * Количество стабильных показаний веса для авто сохранения
     */
    public static final int COUNT_STABLE = 64;
    static final int REQUEST_SEARCH_SCALE = 2;

    protected boolean isStable;
    private boolean flagExit = true;
    private boolean touchWeightView;
    private boolean weightViewIsSwipe;
    private boolean doubleBackToExitPressedOnce;

    enum Action{
        /** Остановка взвешивания.          */
        STOP_WEIGHTING,
        /** Пуск взвешивания.               */
        START_WEIGHTING,
        /** Сохранить результат взвешивания.*/
        STORE_WEIGHTING,
        /** Обновить данные веса.           */
        UPDATE_PROGRESS,
        /** Начало процесса.                */
        START
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonFinish:
                onBackPressed();
            break;
            case R.id.imageMenu:
                openOptionsMenu();
            break;
            default:
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {startThread();
            screenUnlock();
        }catch (Exception e){}
        //try {scaleModule.startProcess();}catch (Exception e){}

    }

    @Override
    protected void onPause() {
        super.onPause();
        try {stopThread();}catch (Exception e){}
        //try {scaleModule.stopProcess();}catch (Exception e){}
    }

    /**
     * Called when the activity is first created.
     */
    //@TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        //Thread.setDefaultUncaughtExceptionHandler(new ReportHelper(this));
        setContentView(R.layout.scale);

        globals = Globals.getInstance();
        globals.initialize(this);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        progressBarWeight = (ProgressBar) findViewById(R.id.progressBarWeight);
        progressBarStable = (ProgressBar)findViewById(R.id.progressBarStable);
        weightTextView = (TextView) findViewById(R.id.weightTextView);

        layoutScale = (LinearLayout)findViewById(R.id.screenScale);
        layoutScale.setVisibility(View.INVISIBLE);

        buttonFinish = (ImageView) findViewById(R.id.buttonFinish);
        buttonFinish.setOnClickListener(this);

        imageViewWait = (ImageView)findViewById(R.id.imageViewWait);

        textViewBattery = (TextView)findViewById(R.id.textBattery);
        textViewTemperature = (TextView)findViewById(R.id.textTemperature);

        listView = (ListView)findViewById(R.id.listView);
        listView.setCacheColorHint(getResources().getColor(R.color.transparent));
        listView.setVerticalFadingEdgeEnabled(false);

        findViewById(R.id.imageMenu).setOnClickListener(this);

        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = 1.0f;
        getWindow().setAttributes(layoutParams);

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo != null) {
            if (networkInfo.isAvailable()) {//Если используется
                new Internet(this).turnOnWiFiConnection(false); // для телефонов у которых один модуль wifi и bluetooth
            }
        }

        textKg = new SpannableStringBuilder(getResources().getString(R.string.scales_kg));
        textKg.setSpan(new TextAppearanceSpan(this, R.style.SpanTextKg),0,textKg.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        textBattery = new SpannableStringBuilder("Заряд батареи ");
        textBattery.setSpan(new TextAppearanceSpan(this, R.style.SpanTextBattery), 0, textBattery.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        baseReceiver = new BaseReceiver(this);
        baseReceiver.register();

        startService(new Intent(getApplicationContext(), ServiceScales.class).setAction(ServiceScales.ACTION_CONNECT_SCALES));

        /*try {
            //scaleModule = new ScaleModule(globals.getPackageInfo().versionName, globals.getPreferencesScale().read(getString(R.string.KEY_LAST_SCALES), ""), connectResultCallback);
            //ScaleModule.create(globals.getPackageInfo().versionName, globals.getPreferencesScale().read(getString(R.string.KEY_LAST_SCALES), ""), connectResultCallback);
            ScaleModule.create(getApplicationContext(), "WeightScales", globals.getPreferencesScale().read(getString(R.string.KEY_LAST_SCALES), ""), connectResultCallback);
            Toast.makeText(getBaseContext(), R.string.bluetooth_off, Toast.LENGTH_SHORT).show();
            //globals.setScaleModule(scaleModule);
        } catch (Exception e) {
            Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        } catch (ErrorDeviceException e) {
            connectResultCallback.resultConnect(Module.ResultConnect.CONNECT_ERROR, e.getMessage(), null);
        }*/
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            //exit();
            return;
        }
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        doubleBackToExitPressedOnce = true;
        Toast.makeText(this, R.string.press_again_to_exit , Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    @Override
    public void onDestroy() { //при разрушении активности
        super.onDestroy();
        exit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_scales, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.preferences:
                startActivity(new Intent(this, ActivityPreferences.class));
                break;
            /*case R.id.tuning:
                startActivity(new Intent(this, ActivityTuning.class));
            break;*/
            case R.id.search:
                vibrator.vibrate(100);
                openSearch();
                break;
            case R.id.exit:
                closeOptionsMenu();
                break;
            case R.id.power_off:
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setTitle(getString(R.string.Scale_off));
                dialog.setCancelable(false);
                dialog.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (i == DialogInterface.BUTTON_POSITIVE) {
                            //if (globals.isScaleConnect())
                            sendBroadcast(new Intent(getApplicationContext(), ServiceScales.class).setAction(ServiceScales.ACTION_POWER_OFF_SCALES));
                            //try {scaleModule.powerOff();}catch (Exception e){}
                            finish();
                        }
                    }
                });
                dialog.setNegativeButton(getString(R.string.Close), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        //finish();
                    }
                });
                dialog.setMessage(getString(R.string.TEXT_MESSAGE15));
                dialog.show();
                break;
            default:

        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setProgressBarIndeterminateVisibility(false);
        switch (resultCode) {
            case RESULT_OK:
                //ScaleModule.getInstance().setConnectResultCallback(connectResultCallback);
                //connectResultCallback.resultConnect(Module.ResultConnect.STATUS_LOAD_OK,"", ScaleModule.getInstance());
                break;
            case RESULT_CANCELED:
                //scaleModule.obtainMessage(RESULT_CANCELED, "Connect error").sendToTarget();
                break;
            default:
        }
    }

    @Override
    public void run() {
        handler.obtainMessage(Action.START.ordinal()).sendToTarget();
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        while (running) {

            weightViewIsSwipe = false;
            numStable = 0;

            while (running && !isCapture() && !weightViewIsSwipe) {                                                     //ждём начала нагружения
                try { Thread.sleep(50); } catch (InterruptedException ignored) { }
            }
            handler.obtainMessage(Action.START_WEIGHTING.ordinal()).sendToTarget();
            isStable = false;
            while (running && !(isStable || weightViewIsSwipe)) {                                                       //ждем стабилизации веса или нажатием выбора веса
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                if (!touchWeightView) {                                                                                 //если не прикасаемся к индикатору тогда стабилизируем вес
                    isStable = processStable(moduleWeight);
                    handler.obtainMessage(Action.UPDATE_PROGRESS.ordinal(), numStable, 0).sendToTarget();
                }
            }
            numStable = COUNT_STABLE;
            if (!running) {
                break;
            }
            tempWeight = moduleWeight;
            if (isStable || weightViewIsSwipe) {
                handler.obtainMessage(Action.STORE_WEIGHTING.ordinal(), moduleWeight, 0).sendToTarget();                 //сохраняем стабильный вес
            }

            weightViewIsSwipe = false;

            while (running && !((moduleWeight >= tempWeight + globals.getDefaultMinAutoCapture())
                    || (moduleWeight <= tempWeight- globals.getDefaultMinAutoCapture()))) {

                try { Thread.sleep(50); } catch (InterruptedException ignored) {}                                       // ждем изменения веса
            }

            handler.obtainMessage(Action.STOP_WEIGHTING.ordinal()).sendToTarget();
            try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e) {}
        }
    }

    private void setupWeightView() {

        Intent intent = getIntent();
        scaleModule = (ScaleModule) intent.getSerializableExtra(ServiceScales.EXTRA_SCALES_MODULE);
        if (scaleModule != null){
            progressBarWeight.setMax(scaleModule.getMarginTenzo());
            progressBarWeight.setSecondaryProgress(scaleModule.getLimitTenzo());
        }

        //progressBarStable = (ProgressBar)findViewById(R.id.progressBarStable);
        progressBarStable.setMax(COUNT_STABLE);
        progressBarStable.setProgress(numStable = 0);

        //weightTextView = (TextView) findViewById(R.id.weightTextView);


        dProgressWeight = getResources().getDrawable(R.drawable.progress_weight);
        dWeightDanger = getResources().getDrawable(R.drawable.progress_weight_danger);

        SimpleGestureFilter.SimpleGestureListener weightViewGestureListener = new SimpleGestureFilter.SimpleGestureListener() {
            @Override
            public void onSwipe(int direction) {

                switch (direction) {
                    case SimpleGestureFilter.SWIPE_RIGHT:
                    case SimpleGestureFilter.SWIPE_LEFT:
                        weightViewIsSwipe = true;
                        break;
                    default:
                }
            }

            @Override
            public void onDoubleTap() {
                progressBarStable.setProgress(0);
                vibrator.vibrate(100);
                new ZeroThread(ActivityScales.this).start();
            }
        };

        detectorWeightView = new SimpleGestureFilter(this, weightViewGestureListener);
        detectorWeightView.setSwipeMinVelocity(50);
        weightTextView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                detectorWeightView.setSwipeMaxDistance(v.getMeasuredWidth());
                detectorWeightView.setSwipeMinDistance(detectorWeightView.getSwipeMaxDistance() / 3);
                detectorWeightView.onTouchEvent(event);
                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        touchWeightView = true;
                        vibrator.vibrate(5);
                        int progress = (int) (event.getX() / (detectorWeightView.getSwipeMaxDistance() / progressBarStable.getMax()));
                        progressBarStable.setProgress(progress);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        progressBarStable.setProgress(0);
                        touchWeightView = false;
                        break;
                    default:
                }
                return false;
            }
        });

        layoutScale.setVisibility(View.VISIBLE);
    }

    private void setupListView(){
        arrayList = new ArrayList<>();
        customListAdapter = new CustomListAdapter(this, R.layout.list_item_weight, arrayList);
        listView.setAdapter(customListAdapter);
    }

    /**
     * Открыть активность поиска весов.
     */
    private void openSearch() {
        //try{ scaleModule.dettach(); }catch (Exception e){}
        startActivityForResult(new Intent(getBaseContext(), ActivitySearch.class), REQUEST_SEARCH_SCALE);
    }

    /**
     * Захват веса для авто сохранения веса.
     * Задержка захвата от ложных срабатываний. Устанавливается значения в настройках.
     *
     * @return true - Условия захвата истины. */
    public boolean isCapture() {
        boolean capture = false;
        while (getWeightToStepMeasuring(moduleWeight) > globals.getAutoCapture()) {
            if (capture) {
                return true;
            } else {
                try { TimeUnit.SECONDS.sleep(globals.getTimeDelayDetectCapture()); } catch (InterruptedException ignored) {}
                capture = true;
            }
        }
        return false;
    }

    public boolean processStable(int weight) {
        if (tempWeight - globals.getStepMeasuring() <= weight && tempWeight + globals.getStepMeasuring() >= weight) {
            if (++numStable >= COUNT_STABLE) {
                return true;
            }
        } else {
            numStable = 0;
        }
        tempWeight = weight;
        return false;
    }

    /**
     * Преобразовать вес в шкалу шага веса.
     * Шаг измерения установливается в настройках.
     *
     * @param weight Вес для преобразования.
     * @return Преобразованый вес. */
    private int getWeightToStepMeasuring(int weight) {
        int i = weight / globals.getStepMeasuring();
        i*=globals.getStepMeasuring();
        return i;
        //return weight / globals.getStepMeasuring() * globals.getStepMeasuring();
    }

    protected void exit() {
        stopThread();
        baseReceiver.unregister();
        /*try{
            scaleModule.stopProcess();
            scaleModule.dettach();
        }catch (Exception e){}*/
        BluetoothAdapter.getDefaultAdapter().disable();
        while (BluetoothAdapter.getDefaultAdapter().isEnabled());
        stopService(new Intent(this, ServiceScales.class));
        //todo System.exit(0);
        //int pid = android.os.Process.myPid();
        //android.os.Process.killProcess(pid);
        //System.runFinalization();
    }

    private void wakeUp(){
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
        wakeLock.acquire();
    }

    private void screenUnlock(){
        KeyguardManager keyguardManager = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("TAG");
        keyguardLock.disableKeyguard();
    }



    /**
     * Обработчик сообщений.
     */
    final Handler handler = new Handler() {
        /** Сообщение от обработчика авто сохранения.
         * @param msg Данные сообщения.
         */
        @Override
        public void handleMessage(Message msg) {
            switch (Action.values()[msg.what]) {
                case UPDATE_PROGRESS:
                    progressBarStable.setProgress(msg.arg1);
                    break;
                case STORE_WEIGHTING:
                    vibrator.vibrate(100);
                    arrayList.add(new WeightObject(msg.arg1));
                    customListAdapter.notifyDataSetChanged();
                    break;
                case START:
                case STOP_WEIGHTING:
                    imageViewWait.setVisibility(View.VISIBLE);
                    progressBarStable.setProgress(0);
                    flagExit = true;
                    break;
                case START_WEIGHTING:
                    imageViewWait.setVisibility(View.INVISIBLE);
                    flagExit = false;
                    break;
                default:
            }
        }
    };

    /**
     * Обработка обнуления весов.
     */
    private class ZeroThread extends Thread {
        private final ProgressDialog dialog;

        ZeroThread(Context context) {
            // Создаём новый поток
            super(getString(R.string.Zeroing));
            dialog = new ProgressDialog(context);
            dialog.setCancelable(false);
            dialog.setIndeterminate(false);
            dialog.show();
            dialog.setContentView(R.layout.custom_progress_dialog);
            TextView tv1 = (TextView) dialog.findViewById(R.id.textView1);
            tv1.setText(R.string.Zeroing);
            //start(); // Запускаем поток
        }

        @Override
        public void run() {
            //scaleModule.setOffsetScale();
            sendBroadcast(new Intent(getApplicationContext(), ServiceScales.class).setAction(ServiceScales.ACTION_OFFSET_SCALES));
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    }

    class BaseReceiver extends BroadcastReceiver {
        Context mContext;
        ProgressDialog dialogSearch;
        IntentFilter intentFilter;
        protected boolean isRegistered;

        BaseReceiver(Context context){
            mContext = context;
            intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            intentFilter.addAction(InterfaceModule.ACTION_LOAD_OK);
            intentFilter.addAction(InterfaceModule.ACTION_ATTACH_START);
            intentFilter.addAction(InterfaceModule.ACTION_ATTACH_FINISH);
        }

        @Override
        public void onReceive(Context context, Intent intent) { //обработчик Bluetooth
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
                            case BluetoothAdapter.STATE_TURNING_ON:
                                Toast.makeText(getBaseContext(), R.string.bluetooth_turning_on, Toast.LENGTH_SHORT).show();
                            break;
                            case BluetoothAdapter.STATE_ON:
                                Toast.makeText(getBaseContext(), R.string.bluetooth_on, Toast.LENGTH_SHORT).show();
                            break;
                            default:
                                break;
                        }
                        break;
                    case InterfaceModule.ACTION_LOAD_OK:
                        ObjectScales objectScales = (ObjectScales) intent.getSerializableExtra(InterfaceModule.EXTRA_MODULE);
                        int str = objectScales.getBattery();
                    break;
                    case InterfaceModule.ACTION_ATTACH_START:
                        if(dialogSearch != null){
                                if(dialogSearch.isShowing())
                                    break;
                            }
                        dialogSearch = new ProgressDialog(ActivityScales.this);
                        dialogSearch.setCancelable(true);
                        dialogSearch.setIndeterminate(false);
                        dialogSearch.show();
                        dialogSearch.setContentView(R.layout.custom_progress_dialog);
                        if (intent != null){
                            String msg = intent.getStringExtra(InterfaceModule.EXTRA_DEVICE_NAME);
                            TextView tv1 = (TextView) dialogSearch.findViewById(R.id.textView1);
                            tv1.setText(getString(R.string.Connecting) + '\n' + msg);
                        }

                    break;
                    case InterfaceModule.ACTION_ATTACH_FINISH:
                        if (dialogSearch.isShowing()) {
                                dialogSearch.dismiss();
                            }
                        break;
                    default:
                }
            }
        }

        public Intent register() {
            isRegistered = true;
            return mContext.registerReceiver(this, intentFilter);
        }

        public boolean unregister() {
            if (isRegistered) {
                mContext.unregisterReceiver(this);  // edited
                isRegistered = false;
                return true;
            }
            return false;
        }
    };

    public void startThread(){
        if(threadAutoWeight != null)
            if(threadAutoWeight.isAlive())
                return;
        running = true;
        threadAutoWeight = new Thread(this);
        //threadAutoWeight.setDaemon(true);
        threadAutoWeight.start();
    }

    public void stopThread(){
        if(threadAutoWeight != null){
            running = false;
            boolean retry = true;
            while(retry){
                try {
                    threadAutoWeight.join();
                    retry = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class CustomListAdapter extends ArrayAdapter<WeightObject> {
        final ArrayList<WeightObject> item;

        public CustomListAdapter(Context context, int textViewResourceId, ArrayList<WeightObject> objects) {
            super(context, textViewResourceId, objects);
            item = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater layoutInflater = (LayoutInflater) super.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = layoutInflater.inflate(R.layout.list_item_weight, parent, false);
            }

            WeightObject o = getItem(position);
            if(o != null){
                TextView tt = (TextView) view.findViewById(R.id.topText);
                TextView bt = (TextView) view.findViewById(R.id.bottomText);

                tt.setText(o.getWeight() +" кг");
                bt.setText(o.getTime() + "   " + o.getDate());
            }


            return view;
        }
    }

    static class WeightObject {
        final String date;
        final String time;
        final int weight;

        WeightObject(int weight){
            this.weight = weight;
            Date d = new Date();
            date = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(d);
            time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(d);
        }

        public int getWeight() { return weight; }

        public String getDate() { return date;  }

        public String getTime() { return time;  }
    }

    public void removeWeightOnClick(View view) {

        //ListView list = getListView();
        int position = listView.getPositionForView(view);
        arrayList.remove(position);
        vibrator.vibrate(50);
        customListAdapter.notifyDataSetChanged();
    }

    public class ReportHelper implements Thread.UncaughtExceptionHandler {
        private AlertDialog dialog;
        private final Context context;

        public ReportHelper(Context context) {
            this.context = context;
        }

        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            String text = ex.getMessage();
            if(text == null){
                text = "";
            }
            showToastInThread(text);
        }

        public void showToastInThread(final CharSequence str){
            new Thread() {
                @Override
                public void run() {
                    Looper.prepare();
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage(str)
                            .setTitle("Ошибка приложения")
                            .setCancelable(false)
                            .setNegativeButton("Выход", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                    exit();
                                }
                            });
                    dialog = builder.create();

                    //Toast.makeText(context, str, Toast.LENGTH_LONG).show();
                    if(!dialog.isShowing())
                        dialog.show();
                    Looper.loop();
                }
            }.start();
        }
    }

    void showNotify(){
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mBuilder.setContentTitle("test").build();
        mBuilder.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, ActivityScales.class), PendingIntent.FLAG_UPDATE_CURRENT));
        NotificationManager mNotificationManager =  (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, mBuilder.build());
    }

}
