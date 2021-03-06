package com.konst.simple_scale.bootloader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.konst.bootloader.AVRProgrammer;
import com.konst.bootloader.HandlerBootloader;
import com.konst.module.*;
import com.konst.module.boot.BootModule;
import com.konst.module.scale.ObjectScales;
import com.konst.module.scale.ScaleModule;
import com.konst.simple_scale.ActivityMain;
import com.konst.simple_scale.Globals;
import com.konst.simple_scale.R;
import com.konst.simple_scale.services.ServiceScales;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;


/**
 * @author Kostya
 */
public class ActivityBootloader extends Activity implements View.OnClickListener {
    private ImageView startBoot, buttonBack;
    private TextView textViewLog;
    private ProgressDialog progressDialog;
    private Globals globals;
    private BootModule bootModule;

    private String addressDevice = "";
    private String hardware = "362";
    private boolean powerOff;
    private static final String dirDeviceFiles = "device";
    private static final String dirBootFiles = "bootfiles";

    protected boolean flagProgramsFinish = true;
    protected boolean flagAutoPrograming;

    static final int REQUEST_CONNECT_BOOT = 1;
    static final int REQUEST_CONNECT_SCALE = 2;

    enum CodeDevice{
        ATMEGA88("atmega88.xml",0x930a),    /* 37642 */
        ATMEGA168("atmega168.xml", 0x9406), /* 37894 */
        ATMEGA328("atmega328.xml", 0x9514); /* 38164 */

        final String device;
        final int code;

        CodeDevice(String d, int c){
            device = d;
            code = c;
        }

        public String getDevice() {return device;}
        //public int getCode() {return code;}

        public static CodeDevice contains(int c){
            for(CodeDevice choice : values())
                if (choice.code == c)
                    return choice;
            return null;
        }
    }

    private class ThreadDoDeviceDependent extends AsyncTask<Void, Void, Boolean> {
        protected AlertDialog.Builder dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            buttonBack.setEnabled(false);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                programmer.doDeviceDependent();
            } catch (Exception e) {
                handlerProgrammed.obtainMessage(HandlerBootloader.Result.MSG_LOG.ordinal(), e.getMessage() + " \r\n").sendToTarget();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean b) {
            super.onPostExecute(b);
            flagProgramsFinish = true;
            buttonBack.setEnabled(true);
            dialog = new AlertDialog.Builder(ActivityBootloader.this);
            dialog.setCancelable(false);

            if (b) {
                dialog.setTitle(getString(R.string.Warning_Loading_settings));
                dialog.setMessage(getString(R.string.TEXT_MESSAGE1));
                dialog.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switch (i) {
                            case DialogInterface.BUTTON_POSITIVE:
                                Intent intent = new Intent(getBaseContext(), ActivityConnect.class);
                                intent.putExtra("address", addressDevice);
                                startActivityForResult(intent, REQUEST_CONNECT_SCALE);
                                break;
                            default:
                        }
                    }
                });
                dialog.setNegativeButton(getString(R.string.Close), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                });
            } else {
                dialog.setTitle(getString(R.string.Warning_Error));
                dialog.setMessage(getString(R.string.TEXT_MESSAGE2));
                dialog.setNegativeButton(getString(R.string.Close), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
            }
            dialog.show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bootloder);

        globals = Globals.getInstance();
        addressDevice = getIntent().getStringExtra(getString(R.string.KEY_ADDRESS));
        hardware = getIntent().getStringExtra(Commands.HRW.getName());
        powerOff = getIntent().getBooleanExtra("com.konst.simple_scale.POWER", false);

        //Spinner spinnerField = (Spinner) findViewById(R.id.spinnerField);
        textViewLog = (TextView) findViewById(R.id.textLog);
        startBoot = (ImageView) findViewById(R.id.buttonBoot);
        startBoot.setOnClickListener(this);
        startBoot.setEnabled(false);
        startBoot.setAlpha(128);
        buttonBack = (ImageView) findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(this);

        progressDialog = new ProgressDialog(this);
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(getString(R.string.Warning_Connect));
        dialog.setCancelable(false);
        dialog.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (i) {
                    case DialogInterface.BUTTON_POSITIVE:
                        try {
                            //bootModule = new BootModule("BOOT", addressDevice, connectResultCallback);
                            Intent intent = new Intent(getApplicationContext(), ServiceScales.class);
                            intent.setAction(ServiceScales.ACTION_CONNECT_SCALES);
                            Bundle bundle = new Bundle();
                            bundle.putString(ServiceScales.EXTRA_VERSION, "BOOT");
                            bundle.putString(ServiceScales.EXTRA_DEVICE, addressDevice);
                            intent.putExtra(ServiceScales.EXTRA_BUNDLE, bundle);
                            startService(intent);
                            //BootModule.create(getApplicationContext(), "BOOT", addressDevice/*, connectResultCallback*/);
                            log(getString(R.string.bluetooth_off));
                        } catch (Exception e) {
                            log(e.getMessage());
                            finish();
                        } /*catch (ErrorDeviceException e) {
                            connectResultCallback.resultConnect(Module.ResultConnect.CONNECT_ERROR, e.getMessage(), null);
                        }*/
                        /*try {
                            globals.setBootModule(bootModule);
                            //bootModule.init(addressDevice);
                            //bootModule.attach();
                        } catch (Exception e) {
                            connectResultCallback.connectError(Module.ResultError.CONNECT_ERROR, e.getMessage());
                        }*/
                    break;
                    default:
                }
            }
        });
        dialog.setNegativeButton(getString(R.string.Close), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        if (powerOff)
            dialog.setMessage("На весах нажмите кнопку включения и не отпускайте пока индикатор не погаснет. После этого нажмите ОК");
        else
            dialog.setMessage(getString(R.string.TEXT_MESSAGE));
        dialog.show();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonBack:
                finish();
                break;
            case R.id.buttonBoot:
                if (!startProgramed()) {
                    flagProgramsFinish = true;
                }
                break;
            default:
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            log("Connected...");
            switch (requestCode) {
                case REQUEST_CONNECT_BOOT:
                    connectResultCallback.resultConnect(Module.ResultConnect.STATUS_LOAD_OK, "", BootModule.getInstance());
                    break;
                case REQUEST_CONNECT_SCALE:
                    log(getString(R.string.Loading_settings));
                    /*if (ScaleModule.isScales()) {
                        //restorePreferences(); //todo сделать загрузку настроек которые сохранены пере перепрограммированием.
                        log(getString(R.string.Settings_loaded));
                        break;
                    }*/
                    log(getString(R.string.Scale_no_defined));
                    log(getString(R.string.Setting_no_loaded));
                    break;
                default:
            }
        } else {
            log("Not connected...");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        exit();
    }

    final InterfaceResultCallback connectResultCallback = new InterfaceResultCallback() {
        private AlertDialog.Builder dialog;

        @Override
        public void resultConnect(final Module.ResultConnect result, String msg, final Object module) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (result) {
                        case STATUS_LOAD_OK:
                            bootModule = (BootModule)module;
                            globals.setBootModule(bootModule);
                            dialog = new AlertDialog.Builder(ActivityBootloader.this);
                            dialog.setTitle(getString(R.string.Warning_update));
                            dialog.setCancelable(false);
                            int numVersion = bootModule.getBootVersion();
                            if(numVersion > 1){
                                hardware = bootModule.getModuleHardware();
                                dialog.setMessage("После нажатия кнопки ОК начнется программирование");
                                flagAutoPrograming = true;
                            }else {
                                dialog.setMessage(getString(R.string.TEXT_MESSAGE5));
                            }
                            startBoot.setEnabled(true);
                            startBoot.setAlpha(255);
                            dialog.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    switch (i) {
                                        case DialogInterface.BUTTON_POSITIVE:
                                            if(flagAutoPrograming){
                                                if (bootModule.startProgramming())
                                                    if (!startProgramed()) {
                                                        flagProgramsFinish = true;
                                                    }
                                            }
                                        break;
                                        default:
                                    }
                                }
                            });
                            dialog.setNegativeButton(getString(R.string.Close), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    finish();
                                }
                            });

                            dialog.show();
                            break;
                        case CONNECT_ERROR:
                            //Intent intent = new Intent(getBaseContext(), ActivityConnect.class);
                            Intent intent = new Intent(getBaseContext(), ActivityMain.class);
                            intent.putExtra("address", addressDevice);
                            intent.setAction("com.kostya.cranescale.BOOTLOADER");
                            startActivityForResult(intent, REQUEST_CONNECT_BOOT);
                            break;
                        default:
                    }
                }
            });
        }

        @Override
        public void eventData(ScaleModule.ResultWeight what, ObjectScales obj) {

        }

    };

    final HandlerBootloader handlerProgrammed = new HandlerBootloader() {

        @Override
        public void handleMessage(Message msg) {
            switch (HandlerBootloader.Result.values()[msg.what]) {
                case MSG_LOG:
                    log(msg.obj.toString());// обновляем TextView
                    break;
                case MSG_SHOW_DIALOG:
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.setMessage(msg.obj.toString());
                    progressDialog.setMax(msg.arg1);
                    progressDialog.setProgress(0);
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.show();
                    break;
                case MSG_UPDATE_DIALOG:
                    progressDialog.setProgress(msg.arg1);
                    break;
                case MSG_CLOSE_DIALOG:
                    progressDialog.dismiss();
                    break;
                default:
            }
        }
    };

    void log(String string) { //для текста
        //textViewLog.append(string);
        textViewLog.setText(string + '\n' + textViewLog.getText());
    }

    void exit() {
        if (flagProgramsFinish) {
            //Preferences.load(getSharedPreferences(Preferences.PREFERENCES, Context.MODE_PRIVATE));
            globals.getPreferencesScale().write(getString(R.string.KEY_FLAG_UPDATE), true);
            if(bootModule != null)
                bootModule.dettach();
            BluetoothAdapter.getDefaultAdapter().disable();
            while (BluetoothAdapter.getDefaultAdapter().isEnabled()) ;
            finish();
        }
    }

    /*boolean isBootloader() { //Является ли весами и какой версии
        String vrs = bootModule.getModuleVersion(); //Получаем версию загрузчика
        return vrs.startsWith("BOOT");
    }*/

    private final AVRProgrammer programmer = new AVRProgrammer(handlerProgrammed) {
        @Override
        public void sendByte(byte b) {
            bootModule.sendByte(b);
        }

        @Override
        public int getByte() {
            return bootModule.getByte();
        }
    };

    boolean startProgramed() {

        if (!programmer.isProgrammerId()) {
            log(getString(R.string.Not_programmer));
            return false;
        }
        flagProgramsFinish = false;
        log(getString(R.string.Programmer_defined));
        try {

            int descriptor = programmer.getDescriptor();

            CodeDevice codeDevice = CodeDevice.contains(descriptor);

            if(codeDevice == null){
                throw new Exception("Фаил с дескриптором " + descriptor + " не найден! ");
            }

            /*if (mapCodeDevice.get(desc) == null) {
                throw new Exception("Фаил с дескриптором " + desc + " не найден! ");
            }*/

            //String deviceFileName = mapCodeDevice.get(desc);
            String deviceFileName = codeDevice.getDevice();
            if (deviceFileName.isEmpty()) {
                throw new Exception("Device name not specified!");
            }

            log("Device " + deviceFileName);

            /*37894_mbc04.36.2_4.hex пример имени файла прошивки
            |desc||hardware ||version     desc- это сигнатура 1 и сигнатура 2     микроконтролера 0x94 ## 0x06
                                        hardware- это версия платы              mbc04.36.2
                                        version- этоверсия программы платы      4                   */
            String constructBootFile = new StringBuilder()
                    .append(descriptor).append('_')               //дескриптор сигнатура 1 и сигнатура 2
                    .append(hardware.toLowerCase())         //hardware- это версия платы
                    .append('_')
                    .append(globals.getMicroSoftware())             //version- этоверсия программы платы
                    .append(".hex").toString();
            log(getString(R.string.TEXT_MESSAGE3) + constructBootFile);
            String[] bootFiles = getAssets().list(dirBootFiles);
            String bootFileName = "";
            if (Arrays.asList(bootFiles).contains(constructBootFile)) {
                bootFileName = constructBootFile;
            }

            if (bootFileName.isEmpty()) {
                throw new Exception("Boot фаил отсутствует для этого устройства!\r\n");
            }

            InputStream inputDeviceFile = getAssets().open(dirDeviceFiles + '/' + deviceFileName);
            InputStream inputHexFile = getAssets().open(dirBootFiles + '/' + bootFileName);


            startBoot.setEnabled(false);
            startBoot.setAlpha(128);
            programmer.doJob(inputDeviceFile, inputHexFile);
            new ThreadDoDeviceDependent().execute();
        } catch (IOException e) {
            handlerProgrammed.obtainMessage(HandlerBootloader.Result.MSG_LOG.ordinal(), e.getMessage()).sendToTarget();
            return false;
        } catch (Exception e) {
            handlerProgrammed.obtainMessage(HandlerBootloader.Result.MSG_LOG.ordinal(), e.getMessage()).sendToTarget();
            return false;
        }
        return true;
    }

    /*public boolean backupPreference() {
        Preferences.load(getSharedPreferences(Preferences.PREF_UPDATE, Context.MODE_PRIVATE));

        Preferences.write(InterfaceVersions.CMD_FILTER, ScaleModule.getFilterADC());
        Preferences.write(InterfaceVersions.CMD_TIMER, ScaleModule.getTimeOff());
        Preferences.write(InterfaceVersions.CMD_BATTERY, ScaleModule.getBattery());
        //Main.preferencesUpdate.write(InterfaceVersions.CMD_CALL_TEMP, String.valueOf(coefficientTemp));
        Preferences.write(InterfaceVersions.CMD_SPREADSHEET, ScaleModule.getSpreadSheet());
        Preferences.write(InterfaceVersions.CMD_G_USER, ScaleModule.getUserName());
        Preferences.write(InterfaceVersions.CMD_G_PASS, ScaleModule.getPassword());
        Preferences.write(InterfaceVersions.CMD_DATA_CFA, ScaleModule.getCoefficientA());
        Preferences.write(InterfaceVersions.CMD_DATA_WGM, ScaleModule.getWeightMax());

        //editor.apply();
        return true;
    }*/

    /*public boolean restorePreferences() {
        if (ScaleModule.isScales()) {
            log("Соединились");
            Preferences.load(getSharedPreferences(Preferences.PREF_UPDATE, Context.MODE_PRIVATE));
            ScaleModule.setModuleFilterADC(Preferences.read(InterfaceVersions.CMD_FILTER, Main.default_adc_filter));
            log("Фмльтер "+ BootModule.getFilterADC());
            ScaleModule.setModuleTimeOff(Preferences.read(InterfaceVersions.CMD_TIMER, Main.default_max_time_off));
            log("Время отключения "+ BootModule.getTimeOff());
            ScaleModule.setModuleBatteryCharge(Preferences.read(InterfaceVersions.CMD_BATTERY, Main.default_max_battery));
            log("Заряд батареи "+ BootModule.getBattery());
            //command(InterfaceScaleModule.CMD_CALL_TEMP + Main.preferencesUpdate.read(InterfaceScaleModule.CMD_CALL_TEMP, "0"));
            ScaleModule.setModuleSpreadsheet(Preferences.read(InterfaceVersions.CMD_SPREADSHEET, "weightscale"));
            log("Имя таблици "+ BootModule.getSpreadSheet());
            ScaleModule.setModuleUserName(Preferences.read(InterfaceVersions.CMD_G_USER, ""));
            log("Имя пользователя "+ BootModule.getUserName());
            ScaleModule.setModulePassword(Preferences.read(InterfaceVersions.CMD_G_PASS, ""));
            log("Пароль");
            ScaleModule.setCoefficientA(Preferences.read(InterfaceVersions.CMD_DATA_CFA, 0.0f));
            log("Коэффициент А "+ ScaleModule.getCoefficientA());
            ScaleModule.setWeightMax(Preferences.read(InterfaceVersions.CMD_DATA_WGM, Main.default_max_weight));
            log("Максимальный вес "+ ScaleModule.getWeightMax());
            ScaleModule.setLimitTenzo((int) (ScaleModule.getWeightMax() / ScaleModule.getCoefficientA()));
            log("Лимит датчика "+ ScaleModule.getLimitTenzo());
            ScaleModule.writeData();
        }
        return true;
    }*/


}
