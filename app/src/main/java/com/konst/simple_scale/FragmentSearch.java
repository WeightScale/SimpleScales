//Ищет весы
package com.konst.simple_scale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.*;
import android.widget.*;
import com.konst.module.InterfaceModule;
import com.konst.module.InterfaceResultCallback;
import com.konst.module.Module;
import com.konst.module.scale.ObjectScales;
import com.konst.module.scale.ScaleModule;
import com.konst.simple_scale.services.ServiceScales;

import java.util.ArrayList;

public class FragmentSearch extends Fragment implements View.OnClickListener {
    private ActivityMain activityMain;
    private Context mContext;
    private Globals globals; /** Глобальные переменные */
    private Module module;
    private BaseReceiver broadcastReceiver; //приёмник намерений
    private ArrayList<BluetoothDevice> foundDevice; //чужие устройства
    private ArrayAdapter<BluetoothDevice> bluetoothAdapter; //адаптер имён
    private IntentFilter intentFilter; //фильтр намерений
    private ListView listView; //список весов
    private TextView textViewLog; //лог событий

    /**
     * Выбор элемента из списка найденых устройств.
     */
    //==================================================================================================================
    private final AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            String action = activityMain.getIntent().getAction();
            try {
                Intent intent = new Intent(mContext, ServiceScales.class);
                intent.setAction(ServiceScales.ACTION_CONNECT_SCALES);
                Bundle bundle = new Bundle();
                if ("com.kostya.cranescale.BOOTLOADER".equals(action)) {
                    //globals.setBootModule(new BootModule("BOOT", (BluetoothDevice) foundDevice.toArray()[i],connectResultCallback ));
                    bundle.putString(ServiceScales.EXTRA_VERSION, "BOOT");
                    //BootModule.create(getApplicationContext(), "BOOT", (BluetoothDevice) foundDevice.toArray()[i]/*, resultCallback*/);
                }else {
                    //globals.setScaleModule(new ScaleModule(globals.getPackageInfo().versionName, (BluetoothDevice) foundDevice.toArray()[i],connectResultCallback ));
                    bundle.putString(ServiceScales.EXTRA_VERSION, "WeightScales");
                    //ScaleModule.create(getApplicationContext(), "WeightScales", (BluetoothDevice) foundDevice.toArray()[i]/*, resultCallback*/);//todo временно для теста
                    //ScaleModule.create(globals.getPackageInfo().versionName, (BluetoothDevice) foundDevice.toArray()[i],connectResultCallback );
                }
                bundle.putString(ServiceScales.EXTRA_DEVICE, ((BluetoothDevice) foundDevice.toArray()[i]).getAddress());
                intent.putExtra(ServiceScales.EXTRA_BUNDLE, bundle);
                mContext.startService(intent);
            } catch (Exception /*| ErrorDeviceException*/ e) {
                foundDevice.remove(i);
                bluetoothAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activityMain = (ActivityMain) activity;
        mContext = activityMain;
        broadcastReceiver = new BaseReceiver(mContext);
        broadcastReceiver.register();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.search, null);

        textViewLog = (TextView)view.findViewById(R.id.textLog);
        Bundle bundle = getArguments();
        if (bundle !=null){
            String msg = bundle.getString(InterfaceModule.EXTRA_MESSAGE, "");
            log(msg);
        }

        view.findViewById(R.id.buttonSearchBluetooth).setOnClickListener(this);
        //view.findViewById(R.id.buttonBack).setOnClickListener(this);

        listView = (ListView)view.findViewById(R.id.listViewDevices);  //список весов
        listView.setAdapter(bluetoothAdapter);
        listView.setOnItemClickListener(onItemClickListener);

        return view;
    }

    //==================================================================================================================
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        globals = Globals.getInstance();

        foundDevice = new ArrayList<>();
        for (int i = 0; globals.getPreferencesScale().contains(getString(R.string.KEY_ADDRESS) + i); i++) { //заполнение списка
            foundDevice.add(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(globals.getPreferencesScale().read(getString(R.string.KEY_ADDRESS) + i, "")));
        }
        bluetoothAdapter = new BluetoothListAdapter(mContext, foundDevice);

        if (foundDevice.isEmpty()) {
            BluetoothAdapter.getDefaultAdapter().startDiscovery();
        }
        String message = activityMain.getIntent().getStringExtra("message");
        //log(message);
        /*String msg = "0503285426 coffa=0.25687 coffb gogusr=kreogen.lg@gmail.com gogpsw=htcehc25";
        String str = encodeMessage(msg);
        decodeMessage("+380503285426",str);
        byte[] pdu = fromHexString("079183503082456201000C9183503082456200004A33DCCC56DBE16EB5DCC82C4FA7C98059AC86CBED7423B33C9D2E8FD47235DE5E07B8EB68B91A1D8FBDD543359CCC7EC7CC72F8482D57CFED7AC0FA6E46AFCD351C");

        Intent intent = new Intent(IncomingSMSReceiver.SMS_RECEIVED_ACTION);
        intent.putExtra("pdus", new Object[] { pdu });
        sendBroadcast(intent);*/
    }

    //==================================================================================================================
    private void exit() {
        if (BluetoothAdapter.getDefaultAdapter().isDiscovering()) {
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        }
        broadcastReceiver.unregister();

        for (int i = 0; globals.getPreferencesScale().contains(getString(R.string.KEY_ADDRESS) + i); i++) { //стереть прошлый список
            globals.getPreferencesScale().remove(getString(R.string.KEY_ADDRESS) + i);
        }
        for (int i = 0; i < foundDevice.size(); i++) { //сохранить новый список
            globals.getPreferencesScale().write(getString(R.string.KEY_ADDRESS) + i, ((BluetoothDevice) foundDevice.toArray()[i]).getAddress());
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        exit();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            /*case R.id.buttonMenu:
                openOptionsMenu();
                break;
            case R.id.buttonBack:
                //onBackPressed();
                break;*/
            case R.id.buttonSearchBluetooth:
                searchDevice();
                break;
            default:
        }
    }

    //==================================================================================================================
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_search, menu);
        //super.onCreateOptionsMenu(menu, inflater);
    }

    //==================================================================================================================
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search:
                searchDevice();
                break;
            case R.id.exit:
                //onDestroy();
                activityMain.finish();
                break;
            default:
        }
        return true;
    }

    void searchDevice(){
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        broadcastReceiver.register();
        BluetoothAdapter.getDefaultAdapter().startDiscovery();
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
            Toast.makeText(mContext, resource, Toast.LENGTH_SHORT).show();
        }
    }

    //==================================================================================================================
    void log(int resource, String str) { //для ресурсов с текстовым дополнением
        textViewLog.setText(getString(resource) + ' ' + str + '\n' + textViewLog.getText());
    }

    final InterfaceResultCallback resultCallback = new InterfaceResultCallback() {
        AlertDialog.Builder dialog;
        private ProgressDialog dialogSearch;

        @Override
        public void resultConnect(final Module.ResultConnect result, final String msg, Object module) {
            //runOnUiThread(new Runnable() {
                //@Override
                //public void run() {
                    switch (result) {
                        case STATUS_LOAD_OK:
                        case TERMINAL_ERROR:
                        case MODULE_ERROR:
                            //globals.setBootModule((BootModule)module);
                            //setResult(RESULT_OK, new Intent().setAction(result.toString()).putExtra("message", msg));
                            //finish();
                            break;
                        case STATUS_VERSION_UNKNOWN:
                            log(msg + ' ' + getString(R.string.not_scale));
                            break;
                        case STATUS_ATTACH_START:
                            listView.setEnabled(false);
                            dialogSearch = new ProgressDialog(mContext);
                            dialogSearch.setCancelable(false);
                            dialogSearch.setIndeterminate(false);
                            dialogSearch.show();
                            dialogSearch.setContentView(R.layout.custom_progress_dialog);
                            TextView tv1 = (TextView) dialogSearch.findViewById(R.id.textView1);
                            tv1.setText(getString(R.string.Connecting) + '\n' + msg);
                            //setProgressBarIndeterminateVisibility(true);
                            //setTitle(getString(R.string.Connecting) + getString(R.string.app_name) + ' ' + msg); //установить заголовок
                            break;
                        case STATUS_ATTACH_FINISH:
                            listView.setEnabled(true);
                            //setProgressBarIndeterminateVisibility(false);
                            if (dialogSearch.isShowing()) {
                                dialogSearch.dismiss();
                            }
                        break;
                        case CONNECT_ERROR:
                            //setTitle(getString(R.string.app_name) + getString(R.string.error_connect)); //установить заголовок
                            log(getString(R.string.Error_connect) + msg);
                        break;
                        default:
                    }
                //}
            //});
        }

        @Override
        public void eventData(ScaleModule.ResultWeight what, ObjectScales obj) {

        }


    };

    class BaseReceiver extends BroadcastReceiver {
        final Context mContext;
        SpannableStringBuilder w;
        Rect bounds;
        ProgressDialog dialogSearch;
        final IntentFilter intentFilter;
        protected boolean isRegistered;

        BaseReceiver(Context context){
            mContext = context;
            intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
            intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        }

        @Override
        public void onReceive(Context context, Intent intent) { //обработчик Bluetooth
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED: //поиск начался
                        log(R.string.discovery_started);
                        foundDevice.clear();
                        bluetoothAdapter.notifyDataSetChanged();
                        //setTitle(getString(R.string.discovery_started)); //установить заголовок

                        //setProgressBarIndeterminateVisibility(true);
                        break;
                    case BluetoothDevice.ACTION_FOUND:  //найдено устройство
                        BluetoothDevice bd = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        foundDevice.add(bd);
                        bluetoothAdapter.notifyDataSetChanged();
                        //BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        String name = null;
                        if (bd != null) {
                            name = bd.getName();
                        }
                        if (name != null) {
                            log(R.string.device_found, name);
                        }
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:  //поиск завершён
                        log("Поиск завершён");
                        //setProgressBarIndeterminateVisibility(false);
                        break;
                        /*case BluetoothDevice.ACTION_ACL_CONNECTED:
                            setProgressBarIndeterminateVisibility(false);
                            try {
                                String extras = intent.getParcelableExtra(BluetoothDevice.EXTRA_RSSI);
                                setTitle(" \"" + module.getNameBluetoothDevice() + "\", v." + module.getModuleVersion()); //установить заголовок
                            } catch (Exception e) {
                                //setTitle(" \"" + e.getMessage() + "\", v." + module.getModuleVersion()); //установить заголовок      }
                            }
                            break;
                        case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                            setTitle(getString(R.string.Search_scale)); //установить заголовок
                            break;*/
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

}