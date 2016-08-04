//Активность для стартовой настройки весов
package com.konst.simple_scale.settings;

//import android.content.SharedPreferences;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.WindowManager;
import android.widget.Toast;
import com.konst.module.Commands;
import com.konst.module.scale.ScaleModule;
import com.konst.simple_scale.Globals;
import com.konst.simple_scale.R;
import com.konst.simple_scale.bootloader.ActivityBootloader;

//import android.preference.PreferenceManager;

public class ActivityTuning extends PreferenceActivity {
    private static ScaleModule scaleModule;
    private static Globals globals;
    private static final Point point1 = new Point(Integer.MIN_VALUE, 0);
    private static final Point point2 = new Point(Integer.MIN_VALUE, 0);
    private static boolean flag_restore;
    //boolean flag_change = false;

    enum EnumPreferenceAdmin{
        POINT1(R.string.KEY_POINT1){
            Context context;
            @Override
            void setup(final Preference name) throws Exception {
                context = name.getContext();
                if(!scaleModule.isAttach())
                    throw new Exception(" ");
                name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        return startDialog(name);
                        /*try {
                            String str = scaleModule.feelWeightSensor();
                            scaleModule.setSensorTenzo(Integer.valueOf(str));
                            point1.x = Integer.valueOf(str);
                            point1.y = 0;
                            Toast.makeText(name.getContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                            flag_restore = true;
                            return true;
                        } catch (Exception e) {
                            Toast.makeText(name.getContext(), R.string.preferences_no + e.getMessage(), Toast.LENGTH_SHORT).show();
                            return false;
                        }*/
                    }
                });
            }

            boolean startDialog(final Preference name){
                AlertDialog.Builder dialog = new AlertDialog.Builder(context);
                dialog.setTitle("Установка ноль");
                dialog.setCancelable(false);
                dialog.setPositiveButton(context.getString(R.string.OK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            String str = scaleModule.feelWeightSensor();
                            scaleModule.setSensorTenzo(Integer.valueOf(str));
                            point1.x = Integer.valueOf(str);
                            point1.y = 0;
                            Toast.makeText(name.getContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                            flag_restore = true;
                        } catch (Exception e) {
                            Toast.makeText(name.getContext(), R.string.preferences_no + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                dialog.setNegativeButton(context.getString(R.string.Close), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                dialog.setMessage("Вы действительно хотите установить ноль калибровки.");
                dialog.show();
                return true;
            }
        },
        POINT2(R.string.KEY_POINT2){
            @Override
            void setup(final Preference name) throws Exception {
                if(!scaleModule.isAttach())
                    throw new Exception(" ");
                name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        try {
                            String str = scaleModule.feelWeightSensor();
                            if (str.isEmpty()) {
                                Toast.makeText(name.getContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                                return false;
                            }
                            scaleModule.setSensorTenzo(Integer.valueOf(str));
                            point2.x = Integer.valueOf(str);
                            point2.y = Integer.valueOf(o.toString());
                            Toast.makeText(name.getContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                            flag_restore = true;
                            return true;
                        } catch (Exception e) {
                            Toast.makeText(name.getContext(), R.string.preferences_no + e.getMessage(), Toast.LENGTH_SHORT).show();
                            return false;
                        }
                    }
                });
            }
        },
        WEIGHT_MAX(R.string.KEY_WEIGHT_MAX){
            @Override
            void setup(Preference name) throws Exception {
                final Context context = name.getContext();
                name.setTitle(context.getString(R.string.Max_weight) + scaleModule.getWeightMax() + context.getString(R.string.scales_kg));
                name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        if (o.toString().isEmpty() || Integer.valueOf(o.toString()) < context.getResources().getInteger(R.integer.default_max_weight)) {
                            Toast.makeText(context, R.string.preferences_no, Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        scaleModule.setWeightMax(Integer.valueOf(o.toString()));
                        scaleModule.setWeightMargin((int) (scaleModule.getWeightMax() * 1.2));
                        preference.setTitle(context.getString(R.string.Max_weight) + scaleModule.getWeightMax() + context.getString(R.string.scales_kg));
                        Toast.makeText(context, R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                        flag_restore = true;
                        return true;
                    }
                });
            }
        },
        COEFFICIENT_A(R.string.KEY_COEFFICIENT_A){
            @Override
            void setup(Preference name) throws Exception {
                final Context context = name.getContext();
                name.setTitle(context.getString(R.string.ConstantA) + Float.toString(scaleModule.getCoefficientA()));
                name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        try {
                            scaleModule.setCoefficientA(Float.valueOf(o.toString()));
                            preference.setTitle(context.getString(R.string.ConstantA) + Float.toString(scaleModule.getCoefficientA()));
                            Toast.makeText(context, R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                            flag_restore = true;
                            return true;
                        } catch (Exception e) {
                            Toast.makeText(context, R.string.preferences_no, Toast.LENGTH_SHORT).show();
                            return false;
                        }
                    }
                });
            }
        },
        CALL_BATTERY(R.string.KEY_CALL_BATTERY){
            @Override
            void setup(Preference name) throws Exception {
                final Context context = name.getContext();
                name.setTitle(context.getString(R.string.Battery) + globals.getBattery() + '%');
                name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        if (o.toString().isEmpty() || "0".equals(o.toString()) || Integer.valueOf(o.toString()) > context.getResources().getInteger(R.integer.default_max_battery)) {
                            Toast.makeText(context, R.string.preferences_no, Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        if (scaleModule.setModuleBatteryCharge(Integer.valueOf(o.toString()))) {
                            preference.setTitle(context.getString(R.string.Battery) + o + '%');
                            Toast.makeText(context, R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                            return true;
                        }

                        Toast.makeText(context, R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                });
            }
        },
        SERVICE_COD(R.string.KEY_SERVICE_COD){
            @Override
            void setup(final Preference name) throws Exception {
                if(!scaleModule.isAttach())
                    throw new Exception(" ");
                name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (newValue.toString().length() > 32 || newValue.toString().length() < 4) {
                            Toast.makeText(name.getContext(), "Длина кода больше 32 или меньше 4 знаков", Toast.LENGTH_LONG).show();
                            return false;
                        }

                        try {
                            scaleModule.setModuleServiceCod(newValue.toString());
                            Toast.makeText(name.getContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                            return true;
                        } catch (Exception e) {
                            Toast.makeText(name.getContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                            return false;
                        }
                    }
                });
            }
        },
        UPDATE(R.string.KEY_UPDATE){
            @Override
            void setup(Preference name) throws Exception {
                final Context context = name.getContext();
                try {
                    if (scaleModule.getVersion() != null) {
                        if (scaleModule.getNumVersion() < globals.getMicroSoftware()) {
                            name.setSummary(context.getString(R.string.Is_new_version));
                        } else {
                            name.setSummary(context.getString(R.string.Scale_update));
                            name.setEnabled(false);
                        }
                    }
                }catch (Exception e){
                    name.setSummary(context.getString(R.string.TEXT_MESSAGE14));
                }
                name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    //@TargetApi(Build.VERSION_CODES.HONEYCOMB)
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(context, ActivityBootloader.class);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        else
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try{
                            intent.putExtra(context.getString(R.string.KEY_ADDRESS), scaleModule.isAttach()? scaleModule.getAddressBluetoothDevice():"");
                            intent.putExtra(Commands.HRW.getName(), scaleModule.getModuleHardware());
                            intent.putExtra(Commands.VRS.getName(), scaleModule.getNumVersion());
                            if (scaleModule.isAttach()){
                                if(scaleModule.powerOff())
                                    intent.putExtra("com.konst.simple_scale.POWER", true);
                            }
                            scaleModule.dettach();
                        }catch (Exception e){ }
                        context.startActivity(intent);
                        return false;
                    }
                });
            }
        };

        private final int resId;
        abstract void setup(Preference name)throws Exception;

        EnumPreferenceAdmin(int key){
            resId = key;
        }

        public int getResId() { return resId; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        globals = Globals.getInstance();
        scaleModule = globals.getScaleModule();
        /*PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setSharedPreferencesName("my_preferences");
        preferenceManager.setSharedPreferencesMode(MODE_PRIVATE);*/
        addPreferencesFromResource(R.xml.tuning);
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = 1.0f;
        getWindow().setAttributes(layoutParams);
        process();
    }

    void process(){
        for (EnumPreferenceAdmin enumPreferenceAdmin : EnumPreferenceAdmin.values()){
            Preference preference = findPreference(getString(enumPreferenceAdmin.getResId()));
            if(preference != null){
                try {
                    enumPreferenceAdmin.setup(preference);
                } catch (Exception e) {
                    preference.setEnabled(false);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (flag_restore) {
            if (point1.x != Integer.MIN_VALUE && point2.x != Integer.MIN_VALUE) {
                scaleModule.setCoefficientA((float) (point1.y - point2.y) / (point1.x - point2.x));
                scaleModule.setCoefficientB(point1.y - scaleModule.getCoefficientA() * point1.x);
            }
            scaleModule.setLimitTenzo((int) (scaleModule.getWeightMax() / scaleModule.getCoefficientA()));
            if (scaleModule.getLimitTenzo() > 0xffffff) {
                scaleModule.setLimitTenzo(0xffffff);
                scaleModule.setWeightMax((int) (0xffffff * scaleModule.getCoefficientA()));
            }
            if (scaleModule.writeData()) {
                Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
            }
        }

    }
}
