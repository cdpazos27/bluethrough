package android.example.bluethroughv2;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    /**
     * Debug TAG
     */
    private static final String TAG = "BluethroughV2";
    /**Variables para verificar los permisos*/
    private static final String[] REQUIRED_PERMISSIONS =
            new String[] {
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
            };
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;
    /**
     * Estrategia de conexión que vamos a usar
     */
    private static final com.google.android.gms.nearby.connection.Strategy STRATEGY = Strategy.P2P_CLUSTER;
    /**
     * Identificador de servicio para trabajar con dispositivos usando la misma aplicación
     */
    private static final String SERVICE_ID = "BluethroughV2_SERVICE_ID";
    /**
     * Handler para conexiones
     */
    private ConnectionsClient connectionsClient;
    /**
     * El adaptador Bluetooth
     */
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    /**Dispositivos conectados y su adapter para actualizar el listado de dispositivos*/
    private ArrayList<String> dispositivosConectados = new ArrayList<>();
    private ArrayAdapter<String> adapterConectados;

    /**Mensajes y su adapter para actualizar el chat*/
    private ArrayList<String> mensajes = new ArrayList<>();
    private ArrayAdapter<String> adapterMensajes;
    /**
     * Variables para las distintas vistas y otras necesarias
     */
    Button mPublicar;
    Button mDescubrir;
    Button mConectados;
    Button mEnviar;
    ListView mChat;
    ListView mListadoConectados;
    TextView mEndpointId;
    EditText mChatInput;
    String mensaje;
    Payload payload;


    private void startDiscovery() {
        connectionsClient.startDiscovery(getPackageName(), mEndPointDiscoveryCallback,
                new DiscoveryOptions.Builder().setStrategy(STRATEGY).build());
    }

    private void startAdvertising(){
        connectionsClient.startAdvertising(bluetoothAdapter.getName(),getPackageName(), mConnectionLifecycleCallback,
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build());
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mPublicar = findViewById(R.id.bt_Publicar);
        mDescubrir = findViewById(R.id.bt_Descubrir);
        mEnviar = findViewById(R.id.bt_Enviar);
        mEndpointId = findViewById(R.id.tv_EndpointId);
        mConectados = findViewById(R.id.bt_Conectados);
        mListadoConectados = findViewById(R.id.lv_Conectados);
        mChat = findViewById(R.id.lv_Chat);
        mChatInput = findViewById(R.id.et_Chat);
        adapterMensajes = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, mensajes);
        adapterConectados = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, dispositivosConectados);
        connectionsClient = Nearby.getConnectionsClient(this);

        mDescubrir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast("Buscando dispositivos...");
                startDiscovery();
            }
        });

        mPublicar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast("Publicando...");
                startAdvertising();
            }
        });
        /**Agrego el adapter al chat para que cargue los mensajes a medida se actualiza el listado de mensajes*/
        mChat.setAdapter(adapterMensajes);
        mEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mensaje = mChatInput.getText().toString();
            mensajes.add(mensaje);
            payload = Payload.fromBytes(mensaje.getBytes());
            connectionsClient.sendPayload(dispositivosConectados.get(0), payload);
            }
        });
        mConectados.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListadoConectados.setAdapter(null);
                mListadoConectados.setAdapter(adapterConectados);
            }
        });
    }
    /**Al iniciar la app*/
    @Override
    protected void onStart() {
        super.onStart();
        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
        }
        mEndpointId.setText("Este dispositivo: "+connectionsClient.toString());

    }
    /**Al cerrar la app*/
    @Override
    protected void onStop() {
        connectionsClient.stopAllEndpoints();
        super.onStop();
    }

    /**
     * Respuestas a los distintos eventos de las actividades de conexión
     */
    private final ConnectionLifecycleCallback mConnectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo connectionInfo) {
            connectionsClient.acceptConnection(endpointId, mPayloadCallback);
        }

        @Override
        public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
            if (connectionResolution.getStatus().isSuccess()) {
                Log.i(TAG, "onConnectionResult: connection successful");
                connectionsClient.stopDiscovery();
                connectionsClient.stopAdvertising();
                if(!dispositivosConectados.contains(endpointId)) {
                    dispositivosConectados.add(endpointId);
                }
                showToast("Conectado a: "+endpointId);
            }
        }            @Override
            public void onDisconnected (@NonNull String endpointId ){
                showToast("Se ha desconectado de: "+endpointId);
                dispositivosConectados.remove(endpointId);
            }
        };


    /**Respuestas a un payload que nos envió otro dispositivo*/
    private final PayloadCallback mPayloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
            mensaje = new String(payload.asBytes());
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate payloadTransferUpdate) {
            if (payloadTransferUpdate.getStatus() == PayloadTransferUpdate.Status.SUCCESS){
                if(!mensajes.contains(mensaje)) {
                    mensajes.add(mensaje);
                    adapterMensajes.notifyDataSetChanged();
                }
            }
        }
    };

    /**Respuestas a acciones de un endpoint remoto*/
    private final EndpointDiscoveryCallback mEndPointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
            connectionsClient.requestConnection(bluetoothAdapter.getName(), endpointId ,mConnectionLifecycleCallback);
        }

        /**De momento no hace falta implementar*/
        @Override
        public void onEndpointLost(@NonNull String endpointId) {

        }
    };

    /**Verdadero si tiene todos los permisos, falso en caso contrario*/
    private static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    /**Tostadora express*/
    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}