package android.example.bluethroughv2;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    /**Tiempo de vida de un mensaje*/
    private static final String TTL = "5";
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
    private ArrayList<String> noDevices = new ArrayList<>();
    private ArrayAdapter<String> adapterNoDevices;
    /**Destinatarios posibles y su adapter*/
    private ArrayList<String> destinatarios = new ArrayList<>();
    private ArrayAdapter<String> adapterDestinatarios;
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
    Button mDestinatarios;
    ListView mChat;
    ListView mListadoConectados;
    ListView mListadoDestinatarios;
    TextView mEndpointId;
    TextView mAlias;
    EditText mChatInput;
    String mensaje;
    String destinatarioFinal;
    String alias;
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
        mListadoDestinatarios = findViewById(R.id.lv_Destinatarios);
        mDestinatarios = findViewById(R.id.bt_Destinatarios);
        mChat = findViewById(R.id.lv_Chat);
        mChatInput = findViewById(R.id.et_Chat);
        mAlias = findViewById(R.id.tv_Alias);
        adapterMensajes = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, mensajes);
        adapterConectados = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, dispositivosConectados);
        adapterDestinatarios = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, destinatarios);
        noDevices.add("No hay dispositivos conectados");
        adapterNoDevices = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, noDevices);
        connectionsClient = Nearby.getConnectionsClient(this);

        Intent intent = new Intent(getApplicationContext(), Alias.class);
        startActivityForResult(intent,2);

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
                if(dispositivosConectados.isEmpty()){
                    showToast("No hay dispositivos conectados");
                } else{
                    if(destinatarioFinal == null || destinatarioFinal ==""){
                        showToast("Seleccione un destinatario");
                    }else {
                        mensaje = mChatInput.getText().toString();
                        mensajes.add(mensaje);
                        adapterMensajes.notifyDataSetChanged();
                        mensaje = destinatarioFinal+TTL+alias+"): "+mensaje;
                        if(dispositivosConectados.contains(destinatarioFinal)){
                            mensaje = mensaje+"1";
                            payload = Payload.fromBytes(mensaje.getBytes());
                            connectionsClient.sendPayload(destinatarioFinal,payload);
                        } else {
                            mensaje = mensaje + "0";
                            payload = Payload.fromBytes(mensaje.getBytes());
                            connectionsClient.sendPayload(dispositivosConectados, payload);
                        }
                    }
                }
                mChatInput.setText("");
            }
        });
        mConectados.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListadoConectados.setAdapter(null);
                if(!dispositivosConectados.isEmpty()) {
                    mListadoConectados.setAdapter(adapterConectados);
                } else {
                    mListadoConectados.setAdapter(adapterNoDevices);
                }
            }
        });
        mListadoConectados.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(parent.getAdapter() != adapterNoDevices) {
                    PopupMenu popup = new PopupMenu(getApplicationContext(), view);
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()){
                                case R.id.desconectarPopupId:
//                                    Desconectar el dispositivo
                                    String dispositivo = dispositivosConectados.get(position);
                                    showToast("Se ha desconectado de: "+dispositivo);
                                    connectionsClient.disconnectFromEndpoint(dispositivo);
                                    dispositivosConectados.remove(dispositivo);
                                    adapterConectados.notifyDataSetChanged();
                                    return true;
                                default:
                                    return false;
                            }
                        }
                    });
                    popup.inflate(R.menu.popup_menu);
                    popup.show();
                }
            }
        });
        mDestinatarios.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListadoDestinatarios.setAdapter(null);
                mListadoDestinatarios.setAdapter(adapterDestinatarios);
            }
        });
        mListadoDestinatarios.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PopupMenu popup = new PopupMenu(getApplicationContext(), view);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()){
                            case R.id.destinatarioPopupId:
//                                Establecer dispositivo como destinatario
                                String dispositivo = destinatarios.get(position);
                                showToast("El destinatario es: "+dispositivo);
                                destinatarioFinal = normalizarDispositivo(dispositivo);
                                mostrarDestinatario();
                                return true;
                            default:
                                return false;


                        }
                    }
                });
                popup.inflate(R.menu.popup_menu_2);
                popup.show();
            }
        });
    }

    private String normalizarDispositivo(String dispositivo) {
        return dispositivo.substring(0,4);
    }

    private void mostrarDestinatario() {
        mEndpointId.setText(destinatarioFinal);
    }

    /**Al iniciar la app*/
    @Override
    protected void onStart() {
        super.onStart();
        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
        }
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
                if(!dispositivosConectados.contains(endpointId)) {
                    dispositivosConectados.add(endpointId);
                    destinatarios.add(endpointId);
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
            Integer hop = Integer.parseInt(mensaje.substring(4,5));
            String destino = mensaje.substring(0,4);
            String cuerpo = mensaje.substring(5);
            if(hop == 5){
                cuerpo = "De "+endpointId+" ("+cuerpo;
            }
            mensaje = destino+hop.toString()+cuerpo;
            if(mensaje.endsWith("1")){
                mensajes.add(endpointId+": "+mensaje.substring(5,mensaje.length()-1));
                adapterMensajes.notifyDataSetChanged();
                showToast(mensaje);
            } else {
                /**Si el TTV es mayor a 0, sigue curso. Si no, el mensaje muere.*/
                if(hop>0) {
                    hop -=1;
                    cuerpo = cuerpo.substring(0,cuerpo.length()-1);
                    mensaje = destino+hop.toString()+cuerpo;
                    if (dispositivosConectados.contains(destino)) {
                        mensaje = mensaje+"1";
                        payload = Payload.fromBytes(mensaje.getBytes());
                        connectionsClient.sendPayload(destino, payload);
                    } else {
                        mensaje = mensaje+"0";
                        payload = Payload.fromBytes(mensaje.getBytes());
                        connectionsClient.sendPayload(dispositivosConectados, payload);
                    }
                }
            }
            Log.i(TAG, "onPayloadReceived: Message Received");

        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate payloadTransferUpdate) {
//            if (payloadTransferUpdate.getStatus() == PayloadTransferUpdate.Status.SUCCESS){
//                if(!mensajes.contains(mensaje)) {
//                    mensajes.add(mensaje);
//                    adapterMensajes.notifyDataSetChanged();
//                }
//            }
        }
    };

    /**Respuestas a acciones de un endpoint remoto*/
    private final EndpointDiscoveryCallback mEndPointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
            connectionsClient.requestConnection(bluetoothAdapter.getName(), endpointId ,mConnectionLifecycleCallback);
        }

        /**Cuando perdemos un dispositivo, mismo procedimiento que si se hubiese desconectado*/
        @Override
        public void onEndpointLost(@NonNull String endpointId) {
            showToast("Se perdió la conexión a: "+endpointId);
            dispositivosConectados.remove(endpointId);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode ==2){
            String aliasRecibido = data.getStringExtra("ALIAS");
            mAlias.setText(aliasRecibido);
            alias = aliasRecibido;
        }
    }
}