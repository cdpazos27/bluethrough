package android.example.bluethrough;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_DISCOVER_BT = 1;

    BluetoothAdapter mBluetoothAdapter;
    ImageView mBluetoothOnOff;
    Button mButtonOn;
    Button mButtonOff;
    Button mButtonBuscar;
    Button mButtonEmparejados;
    ListView mListDispositivosBuscados;
    ListView mListDispositivosEmparejados;
    ArrayList<String> deviceNameList = new ArrayList<>();
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Adaptador y vistas
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothOnOff = (ImageView) findViewById(R.id.iv_bluetooth_on_off);
        mButtonOn = (Button) findViewById(R.id.btn_On);
        mButtonOff = (Button) findViewById(R.id.btn_Off);
        mButtonBuscar = (Button) findViewById(R.id.btn_Buscar);
        mButtonEmparejados = (Button) findViewById(R.id.btn_Emparejados);
        mListDispositivosBuscados = (ListView) findViewById(R.id.lv_Discovered_Devices);
        mListDispositivosEmparejados = (ListView) findViewById(R.id.lv_Paired_Devices);

        if (mBluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            showToast("Este dispositivo no dispone de Bluetooth");
        }
        // Icono Bluetooth segun este prendido o apagado
        if(mBluetoothAdapter.isEnabled()){
            mBluetoothOnOff.setImageResource(R.drawable.bt_on_foreground);
        }
        else{
            mBluetoothOnOff.setImageResource(R.drawable.bt_off_foreground);
            disableButton(mButtonBuscar);
            disableButton(mButtonEmparejados);
        }

        mButtonOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mBluetoothAdapter.isEnabled()){
                    showToast("Bluetooth ya está encendido");
                } else {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
                if(mBluetoothAdapter.isEnabled()) {
                    mBluetoothOnOff.setImageResource(R.drawable.bt_on_foreground);
                }
            }
        });

        mButtonOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mBluetoothAdapter.isEnabled()){
                    showToast("Bluetooth ya está apagado");
                } else {
                    showToast("Apagando Bluetooth...");
                    mBluetoothAdapter.disable();
                    disableButton(mButtonBuscar);
                    disableButton(mButtonEmparejados);
                    mBluetoothOnOff.setImageResource(R.drawable.bt_off_foreground);
                }
            }
        });
        // Quick permission check
        int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
        permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
        if (permissionCheck != 0) {

            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
        }
        mButtonBuscar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deviceNameList.clear();
                mListDispositivosBuscados.setAdapter(null);
                mListDispositivosBuscados.setAdapter(adapter);
                if(mBluetoothAdapter.isDiscovering()){
                    mBluetoothAdapter.cancelDiscovery();
                    mBluetoothAdapter.startDiscovery();
                    showToast("Buscando dispositivos visibles...");
                }else {
                    showToast("Buscando dispositivos visibles...");
                    mBluetoothAdapter.startDiscovery();
                }
            }
        });
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1,deviceNameList);
    }
    BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                deviceNameList.add(deviceName);
                adapter.notifyDataSetChanged();
            }
        }
    };
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    //bluetooth is on
                    mBluetoothOnOff.setImageResource(R.drawable.bt_on_foreground);
                    enableButton(mButtonBuscar);
                    enableButton(mButtonEmparejados);
                    showToast("Bluetooth encendido");
                } else {
                    //user denied to turn bluetooth on
                    showToast("Bluetooth no pudo ser encendido");
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.main, menu);
//        return true;
//    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void disableButton(Button button){
        button.setEnabled(false);
        button.setTextColor(Color.parseColor("#BDBDBD"));
    }
    private void enableButton(Button button){
        button.setEnabled(true);
        button.setTextColor(Color.parseColor("#000000"));
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
    }

}