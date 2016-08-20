package com.konst.simple_scale;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.WindowManager;
import android.widget.TextView;
import com.konst.module.scale.ScaleModule;

/*
 * Created by Kostya on 26.04.14.
 */
public class ActivityAbout extends Activity {
    private static Globals globals;
    private static ScaleModule scaleModule;
    enum StrokeSettings{
        VERSION(R.string.Version_scale){
            @Override
            String getValue() {
                return String.valueOf(scaleModule.getNumVersion()); }

            @Override
            int getMeasure() { return -1; }
        },
        NAME_BLUETOOTH(R.string.Name_module_bluetooth) {
            @Override
            String getValue() {return scaleModule.getNameBluetoothDevice().toString(); }

            @Override
            int getMeasure() { return -1;}
        },
        ADDRESS_BLUETOOTH(R.string.Address_bluetooth) {
            @Override
            String getValue() { return scaleModule.getAddressBluetoothDevice() + '\n'; }

            @Override
            int getMeasure() { return -1; }
        },
        /*BATTERY(R.string.Battery) {
            @Override
            String getValue() { return globals.getBattery() + " %"; }

            @Override
            int getMeasure() { return -1; }
        },
        TEMPERATURE(R.string.Temperature) {
            @Override
            String getValue() {
                String temp;
                try {
                    temp = scaleModule.getTemperature() + "°" + 'C';
                }catch (Exception e){
                    temp = "error"+ '\n';
                }
                return temp;
            }

            @Override
            int getMeasure() { return -1; }
        },*/
        COEFFICIENT_A(R.string.Coefficient) {
            @Override
            String getValue() {  return String.valueOf(scaleModule.getCoefficientA()); }

            @Override
            int getMeasure() { return -1; }
        },
        WEIGHT_MAX(R.string.MLW) {
            final int resIdKg = R.string.scales_kg;
            @Override
            String getValue() {  return scaleModule.getWeightMax() + " "; }

            @Override
            int getMeasure() { return resIdKg; }
        },
        TIME_OFF(R.string.Off_timer) {
            final int reIdMinute = R.string.minute;
            @Override
            String getValue() { return scaleModule.getTimeOff() + " "; }

            @Override
            int getMeasure() { return reIdMinute; }
        },
        STEP(R.string.Step_capacity_scale){
            final int resIdKg = R.string.scales_kg;
            @Override
            String getValue() { return scaleModule.getStepScale() + " "; }

            @Override
            int getMeasure() {  return resIdKg; }
        };

        private final int resId;
        abstract String getValue();
        abstract int getMeasure();

        StrokeSettings(int res){
            resId = res;
        }

        public int getResId() {return resId;}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        setTitle(getString(R.string.About));

        globals = Globals.getInstance();
        scaleModule = globals.getScaleModule();

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);

        TextView textSoftVersion = (TextView) findViewById(R.id.textSoftVersion);
        textSoftVersion.setText(globals.getPackageInfo().versionName + ' ' + String.valueOf(globals.getPackageInfo().versionCode));

        TextView textSettings = (TextView) findViewById(R.id.textSettings);
        parserTextSettings(textSettings);
        textSettings.append("\n");

        TextView textAuthority = (TextView) findViewById(R.id.textAuthority);
        textAuthority.append(getString(R.string.Copyright) + '\n');
        textAuthority.append(getString(R.string.Reserved) + '\n');
    }

    void parserTextSettings(TextView textView){
        for (StrokeSettings s : StrokeSettings.values()){
            try {
                SpannableStringBuilder text = new SpannableStringBuilder(getString(s.getResId()));
                text.setSpan(new StyleSpan(Typeface.NORMAL), 0, text.length(), Spanned.SPAN_MARK_MARK);
                textView.append(text);
                SpannableStringBuilder value = new SpannableStringBuilder(s.getValue());
                value.setSpan(new StyleSpan(Typeface.BOLD_ITALIC),0,value.length(), Spanned.SPAN_MARK_MARK);
                textView.append(value);
                textView.append((s.getMeasure() == -1 ? "" : getString(s.getMeasure())) + '\n');
            }catch (Exception e){
                textView.append("\n");
            }
        }
    }
}
