package com.konst.simple_scale;

import android.app.*;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.content.*;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.*;
import android.support.v4.app.*;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.TextAppearanceSpan;
import android.view.*;
import android.widget.*;
import com.konst.module.InterfaceModule;
import com.konst.module.scale.ObjectScales;
import com.konst.module.scale.ScaleModule;
import com.konst.simple_scale.services.ServiceScales;
import com.konst.simple_scale.settings.ActivityPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class FragmentScales extends Fragment implements /*View.OnClickListener,*/ Runnable {
    private ActivityMain activityMain;
    private Context mContext;
    private Globals globals;
    private ScaleModule scaleModule;
    private SpannableStringBuilder textKg;
    private SpannableStringBuilder textBattery;
    private TextView textViewBattery, textViewTemperature;
    private ListView listView;
    private ArrayList<WeightObject> arrayList = new ArrayList<>();
    private ArrayAdapter<WeightObject> customListAdapter;
    private ProgressBar progressBarStable;
    private ProgressBar progressBarWeight;
    private TextView weightTextView;
    private Drawable dProgressWeight, dWeightDanger;
    private SimpleGestureFilter detectorWeightView;
    private ImageView buttonFinish, imageViewWait;
    private Vibrator vibrator; //вибратор
    //private LinearLayout layoutScale;
    private BaseReceiver baseReceiver; //приёмник намерений

    //private BatteryTemperatureCallback batteryTemperatureCallback;
    //private WeightCallback weightCallback;

    //public int numStable;
    private int moduleWeight;
    //int moduleSensorValue;
    protected int tempWeight;
    private Thread threadAutoWeight;
    boolean running;
    /**
     * Количество стабильных показаний веса для авто сохранения
     */
    //public static final int COUNT_STABLE = 64;

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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activityMain = (ActivityMain) activity;
        mContext = activityMain;
        baseReceiver = new BaseReceiver(mContext);
        baseReceiver.register();
    }

    @Override
    public void onResume() {
        super.onResume();
        //customListAdapter.notifyDataSetChanged();
        try {//startThread();
            screenUnlock();
        }catch (Exception e){}
        try {scaleModule.startProcess();}catch (Exception e){}

    }

    @Override
    public void onPause() {
        super.onPause();
        /*try {stopThread();}catch (Exception e){}*/
        try {scaleModule.stopProcess();}catch (Exception e){}
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        globals = Globals.getInstance();
        scaleModule = globals.getScaleModule();

        vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);

        textKg = new SpannableStringBuilder(getResources().getString(R.string.scales_kg));
        textKg.setSpan(new TextAppearanceSpan(mContext, R.style.SpanTextKg),0,textKg.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        textBattery = new SpannableStringBuilder("Заряд батареи ");
        textBattery.setSpan(new TextAppearanceSpan(mContext, R.style.SpanTextBattery), 0, textBattery.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scale, null);

        progressBarWeight = (ProgressBar)view.findViewById(R.id.progressBarWeight);
        progressBarStable = (ProgressBar)view.findViewById(R.id.progressBarStable);
        weightTextView = (TextView)view.findViewById(R.id.weightTextView);

        imageViewWait = (ImageView)view.findViewById(R.id.imageViewWait);

        textViewBattery = (TextView)view.findViewById(R.id.textBattery);
        textViewTemperature = (TextView)view.findViewById(R.id.textTemperature);

        listView = (ListView)view.findViewById(R.id.listView);
        listView.setCacheColorHint(getResources().getColor(R.color.transparent));
        listView.setVerticalFadingEdgeEnabled(false);
        setupListView();

        setupWeightView();

        return view;
    }

    @Override
    public void onDestroy() { //при разрушении активности
        super.onDestroy();
        exit();
    }

    @Override
    public void run() {
        handler.obtainMessage(Action.START.ordinal()).sendToTarget();
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        while (running) {

            weightViewIsSwipe = false;
            //numStable = 0;

            while (running && !isCapture() && !weightViewIsSwipe) {                                                     //ждём начала нагружения
                try { Thread.sleep(50); } catch (InterruptedException ignored) { }
            }
            handler.obtainMessage(Action.START_WEIGHTING.ordinal()).sendToTarget();
            /*isStable = false;
            while (running && !(isStable || weightViewIsSwipe)) {                                                       //ждем стабилизации веса или нажатием выбора веса
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                if (!touchWeightView) {                                                                                 //если не прикасаемся к индикатору тогда стабилизируем вес
                    //isStable = processStable(moduleWeight);
                    //handler.obtainMessage(Action.UPDATE_PROGRESS.ordinal(), numStable, 0).sendToTarget();
                }
            }*/
            //numStable = COUNT_STABLE;
            if (!running) {
                break;
            }
            tempWeight = moduleWeight;
            /*if (isStable || weightViewIsSwipe) {
                handler.obtainMessage(Action.STORE_WEIGHTING.ordinal(), moduleWeight, 0).sendToTarget();                 //сохраняем стабильный вес
            }*/

            weightViewIsSwipe = false;

            while (running && !((moduleWeight >= tempWeight + globals.getDefaultMinAutoCapture())
                    || (moduleWeight <= tempWeight- globals.getDefaultMinAutoCapture()))) {

                try { Thread.sleep(50); } catch (InterruptedException ignored) {}                                       // ждем изменения веса
            }

            handler.obtainMessage(Action.STOP_WEIGHTING.ordinal()).sendToTarget();
            try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e) {}
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_scales, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.preferences:
                startActivity(new Intent(mContext, ActivityPreferences.class));
            break;
            case R.id.search:
                vibrator.vibrate(100);
                activityMain.openSearch();
            break;
            case R.id.exit:
                //closeOptionsMenu();
            break;
            case R.id.power_off:
                AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
                dialog.setTitle(getString(R.string.Scale_off));
                dialog.setCancelable(false);
                dialog.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (i == DialogInterface.BUTTON_POSITIVE) {
                            //if (globals.isScaleConnect())
                            mContext.startService(new Intent(mContext, ServiceScales.class).setAction(ServiceScales.ACTION_POWER_OFF_SCALES));
                            //try {scaleModule.powerOff();}catch (Exception e){}
                            activityMain.finish();
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void setupWeightView() {

        if (scaleModule != null){
            progressBarWeight.setMax(scaleModule.getMarginTenzo());
            progressBarWeight.setSecondaryProgress(scaleModule.getLimitTenzo());
            progressBarStable.setMax(scaleModule.STABLE_NUM_MAX);
        }

        //progressBarStable = (ProgressBar)findViewById(R.id.progressBarStable);

        //progressBarStable.setProgress(numStable = 0);
        progressBarStable.setProgress(0);

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
                        activityMain.sendBroadcast(new Intent(InterfaceModule.ACTION_WEIGHT_STABLE));
                    break;
                    default:
                }
            }

            @Override
            public void onDoubleTap() {
                progressBarStable.setProgress(0);
                vibrator.vibrate(100);
                new ZeroThread(mContext).start();
            }
        };

        detectorWeightView = new SimpleGestureFilter(activityMain, weightViewGestureListener);
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
    }

    private void setupListView(){

        customListAdapter = new CustomListAdapter(mContext, R.layout.list_item_weight, arrayList);
        listView.setAdapter(customListAdapter);
        customListAdapter.notifyDataSetChanged();
    }

    /**
     * Захват веса для авто сохранения веса.
     * Задержка захвата от ложных срабатываний. Устанавливается значения в настройках.
     *
     * @return true - Условия захвата истины. */
    public boolean isCapture() {
        boolean capture = false;
        while (moduleWeight > globals.getAutoCapture()) {
            if (capture) {
                return true;
            } else {
                try { TimeUnit.SECONDS.sleep(globals.getTimeDelayDetectCapture()); } catch (InterruptedException ignored) {}
                capture = true;
            }
        }
        return false;
    }

    /*public boolean processStable(int weight) {
        if (tempWeight - scaleModule.getStepScale() <= weight && tempWeight + scaleModule.getStepScale() >= weight) {
            if (++numStable >= COUNT_STABLE) {
                return true;
            }
        } else {
            numStable = 0;
        }
        tempWeight = weight;
        return false;
    }*/



    protected void exit() {
        baseReceiver.unregister();
        //todo System.exit(0);
    }

    private void wakeUp(){
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
        wakeLock.acquire();
    }

    private void screenUnlock(){
        KeyguardManager keyguardManager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
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
            scaleModule.setOffsetScale();
            //mContext.sendBroadcast(new Intent(mContext, ServiceScales.class).setAction(ServiceScales.ACTION_OFFSET_SCALES));
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    }

    class BaseReceiver extends BroadcastReceiver {
        private final Context mContext;
        private SpannableStringBuilder w;
        private Rect bounds;
        private ProgressDialog dialogSearch;
        private final IntentFilter intentFilter;
        protected boolean isRegistered;

        BaseReceiver(Context context){
            mContext = context;
            intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            intentFilter.addAction(InterfaceModule.ACTION_SCALES_RESULT);
            intentFilter.addAction(InterfaceModule.ACTION_WEIGHT_STABLE);
        }

        @Override
        public void onReceive(Context context, Intent intent) { //обработчик Bluetooth
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        switch (BluetoothAdapter.getDefaultAdapter().getState()) {
                            case BluetoothAdapter.STATE_OFF:
                                Toast.makeText(mContext, R.string.bluetooth_off, Toast.LENGTH_SHORT).show();
                                new Internet(mContext).turnOnWiFiConnection(false);
                                BluetoothAdapter.getDefaultAdapter().enable();
                            break;
                            case BluetoothAdapter.STATE_TURNING_ON:
                                Toast.makeText(mContext, R.string.bluetooth_turning_on, Toast.LENGTH_SHORT).show();
                            break;
                            case BluetoothAdapter.STATE_ON:
                                Toast.makeText(mContext, R.string.bluetooth_on, Toast.LENGTH_SHORT).show();
                            break;
                            default:
                                break;
                        }
                        break;
                    case InterfaceModule.ACTION_SCALES_RESULT:
                        ObjectScales obj = (ObjectScales) intent.getSerializableExtra(InterfaceModule.EXTRA_SCALES);
                        if (obj == null)
                            return;
                        moduleWeight = obj.getWeight();
                        final String textWeight = String.valueOf(moduleWeight);
                        /** Обновляем прогресс стабилизации веса. */
                        handler.obtainMessage(Action.UPDATE_PROGRESS.ordinal(), obj.getStableNum(), 0).sendToTarget();
                        switch (obj.getResultWeight()) {
                            case WEIGHT_NORMAL:
                                w = new SpannableStringBuilder(textWeight);
                                w.setSpan(new ForegroundColorSpan(Color.WHITE), 0, w.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                                w.append(textKg);
                                progressBarWeight.setProgress(obj.getTenzoSensor());
                                bounds = progressBarWeight.getProgressDrawable().getBounds();
                                progressBarWeight.setProgressDrawable(dProgressWeight);
                                progressBarWeight.getProgressDrawable().setBounds(bounds);
                                break;
                            case WEIGHT_LIMIT:
                                w = new SpannableStringBuilder(textWeight);
                                w.setSpan(new ForegroundColorSpan(Color.RED), 0, w.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                                w.append(textKg);
                                progressBarWeight.setProgress(obj.getTenzoSensor());
                                bounds = progressBarWeight.getProgressDrawable().getBounds();
                                progressBarWeight.setProgressDrawable(dWeightDanger);
                                progressBarWeight.getProgressDrawable().setBounds(bounds);
                                break;
                            case WEIGHT_MARGIN:
                                w = new SpannableStringBuilder(String.valueOf(moduleWeight));
                                w.setSpan(new ForegroundColorSpan(Color.RED), 0, w.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                                progressBarWeight.setProgress(obj.getTenzoSensor());
                                vibrator.vibrate(100);
                                break;
                            case WEIGHT_ERROR:
                                w = new SpannableStringBuilder("- - -");
                                w.setSpan(new ForegroundColorSpan(Color.RED), 0, w.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                                moduleWeight = 0;
                                progressBarWeight.setProgress(0);
                                break;
                            default:
                        }
                        weightTextView.setText(w, TextView.BufferType.SPANNABLE);
                        textViewTemperature.setText(obj.getTemperature() + "°C");
                        if (obj.getBattery() > 15) {
                            textViewBattery.setText(obj.getBattery() + "%");
                            textViewBattery.setTextColor(Color.WHITE);
                            textViewBattery.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_battery, 0, 0, 0);
                        } else if (obj.getBattery() >= 0) {
                            textViewBattery.setText(obj.getBattery() + "%");
                            textViewBattery.setTextColor(Color.RED);
                            textViewBattery.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_battery_red, 0, 0, 0);
                        }else {
                            textViewBattery.setText("нет данных!!!");
                            textViewBattery.setTextColor(Color.BLUE);
                            textViewBattery.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_battery_red, 0, 0, 0);
                        }
                        //    }
                    break;
                    case InterfaceModule.ACTION_WEIGHT_STABLE:
                        isStable = true;
                        handler.obtainMessage(Action.STORE_WEIGHTING.ordinal(), moduleWeight, 0).sendToTarget();                 //сохраняем стабильный вес
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
    }

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
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mBuilder.setContentTitle("test").build();
        mBuilder.setContentIntent(PendingIntent.getActivity(mContext, 0, new Intent(mContext, FragmentScales.class), PendingIntent.FLAG_UPDATE_CURRENT));
        NotificationManager mNotificationManager =  (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, mBuilder.build());
    }

}
