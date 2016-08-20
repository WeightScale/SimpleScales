package com.konst.simple_scale;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.konst.module.InterfaceModule;
import com.konst.simple_scale.services.ServiceScales;

/**
 * @author Kostya 13.08.2016.
 */
public class ActivityMain extends Activity {
    private FragmentScales fragmentScales;
    private FragmentSearch fragmentSearch;
    private FragmentTransaction fragmentTransaction;
    private BaseReceiver baseReceiver;
    private Globals globals;
    private boolean doubleBackToExitPressedOnce;
    private static final String TAG_FRAGMENT = ActivityMain.class.getName() + "TAG_FRAGMENT";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        globals = Globals.getInstance();
        globals.initialize(this);

        fragmentScales = new FragmentScales();
        fragmentSearch = new FragmentSearch();

        baseReceiver = new BaseReceiver(this);
        baseReceiver.register();

        if (savedInstanceState == null){
            lockOrientation();
            Bundle bundle = new Bundle();
            bundle.putString(ServiceScales.EXTRA_VERSION, "WeightScales");
            bundle.putString(ServiceScales.EXTRA_DEVICE, globals.getPreferencesScale().read(getString(R.string.KEY_LAST_SCALES), ""));
            Intent intent = new Intent(getApplicationContext(), ServiceScales.class);
            intent.setAction(ServiceScales.ACTION_CONNECT_SCALES);
            intent.putExtra(ServiceScales.EXTRA_BUNDLE, bundle);
            startService(intent);
        }else {
            String tag = savedInstanceState.getString(TAG_FRAGMENT);
            Fragment fragment = getFragmentManager().findFragmentByTag(tag);
            if (fragment != null){
                if (fragment instanceof FragmentScales){
                    fragmentScales = (FragmentScales) fragment;
                }else if(fragment instanceof  FragmentSearch){
                    fragmentSearch = (FragmentSearch) fragment;
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Fragment fragment = getFragmentManager().findFragmentById(R.id.fragmentCont);
        if (fragment != null){
            outState.putString(TAG_FRAGMENT, fragment.getTag());
        }
    }

    @Override
    public void onDestroy() { //при разрушении активности
        super.onDestroy();
        baseReceiver.unregister();
        //exit();
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            exit();
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

    public void unlockOrientation() {
        setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    public void lockOrientation() {

        if (Build.VERSION.SDK_INT < 18)
            setRequestedOrientation(getOrientation());
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
    }

    private int getOrientation() throws AssertionError {

        int port = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        int revP = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
        int land = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        int revL = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
        if (Build.VERSION.SDK_INT < 9) {
            revL = land;
            revP = port;
        } else if (isLandscape270()) {
            land = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            revL = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        }

        Display display = getWindowManager().getDefaultDisplay();
        boolean wide = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        switch (display.getRotation()) {
            case Surface.ROTATION_0:
                return wide ? land : port;
            case Surface.ROTATION_90:
                return wide ? land : revP;
            case Surface.ROTATION_180:
                return wide ? revL : revP;
            case Surface.ROTATION_270:
                return wide ? revL : port;
            default:
                throw new AssertionError();
        }
    }

    private static boolean isLandscape270() {

        return "Amazon".equals(Build.MANUFACTURER) && !("KFOT".equals(Build.MODEL) || "Kindle Fire".equals(Build.MODEL));
    }

    /**
     * Открыть активность поиска весов.
     */
    public void openSearch() {
        try{ globals.getScaleModule().dettach(); }catch (Exception e){}
        fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragmentCont, fragmentSearch, fragmentSearch.getClass().getName());
        fragmentTransaction.commit();
    }

    protected void exit() {

        /*try{
            scaleModule.stopProcess();
            scaleModule.dettach();
        }catch (Exception e){}*/
        //BluetoothAdapter.getDefaultAdapter().disable();
        //while (BluetoothAdapter.getDefaultAdapter().isEnabled());
        //startService(new Intent(this, ServiceScales.class).setAction(ServiceScales.ACTION_CLOSED_SCALES));
        stopService(new Intent(this, ServiceScales.class));
        //todo System.exit(0);
        //int pid = android.os.Process.myPid();
        //android.os.Process.killProcess(pid);
        //System.runFinalization();
    }

    class BaseReceiver extends BroadcastReceiver {
        final Context mContext;
        ProgressDialog dialogSearch;
        final IntentFilter intentFilter;
        protected boolean isRegistered;

        BaseReceiver(Context context){
            mContext = context;
            intentFilter = new IntentFilter(InterfaceModule.ACTION_LOAD_OK);
            intentFilter.addAction(InterfaceModule.ACTION_ATTACH_START);
            intentFilter.addAction(InterfaceModule.ACTION_ATTACH_FINISH);
            intentFilter.addAction(InterfaceModule.ACTION_CONNECT_ERROR);
        }

        @Override
        public void onReceive(Context context, Intent intent) { //обработчик Bluetooth
            String action = intent.getAction();
            if (action != null) {
                fragmentTransaction = getFragmentManager().beginTransaction();
                switch (action) {
                    case InterfaceModule.ACTION_LOAD_OK:
                        unlockOrientation();
                        fragmentScales = new FragmentScales();
                        fragmentTransaction.replace(R.id.fragmentCont, fragmentScales, fragmentScales.getClass().getName());
                        fragmentTransaction.commit();
                        break;
                    case InterfaceModule.ACTION_ATTACH_START:
                        if(dialogSearch != null){
                            if(dialogSearch.isShowing())
                                break;
                        }
                        dialogSearch = new ProgressDialog(ActivityMain.this);
                        dialogSearch.setCancelable(true);
                        dialogSearch.setIndeterminate(false);
                        dialogSearch.show();
                        dialogSearch.setContentView(R.layout.custom_progress_dialog);
                        String msg = intent.getStringExtra(InterfaceModule.EXTRA_DEVICE_NAME);
                        TextView tv1 = (TextView) dialogSearch.findViewById(R.id.textView1);
                        tv1.setText(getString(R.string.Connecting) + '\n' + msg);
                        break;
                    case InterfaceModule.ACTION_ATTACH_FINISH:
                        if (dialogSearch.isShowing()) {
                            dialogSearch.dismiss();
                        }
                        break;
                    case InterfaceModule.ACTION_CONNECT_ERROR:
                        String value = intent.getStringExtra(InterfaceModule.EXTRA_MESSAGE);
                        fragmentTransaction.replace(R.id.fragmentCont, fragmentSearch, fragmentSearch.getClass().getName());
                        if (value!=null){
                            Bundle bundle = new Bundle();
                            bundle.putString(InterfaceModule.EXTRA_MESSAGE, value);
                            fragmentSearch.setArguments(bundle);
                        }
                        fragmentTransaction.commit();
                        break;
                    default:
                }
            }
        }

        public void register() {
            isRegistered = true;
            mContext.registerReceiver(this, intentFilter);
        }

        public void unregister() {
            if (isRegistered) {
                mContext.unregisterReceiver(this);  // edited
                isRegistered = false;
            }
        }
    }

    public void removeWeightOnClick(View view) {
        fragmentScales.removeWeightOnClick(view);
    }
}
