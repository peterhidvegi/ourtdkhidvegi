package hu.nik.uniobuda.tdk;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

//https://github.com/peterhidvegi/ourtdkhidvegi.git
public class MainActivity extends Activity {


    ListView listView;

    IntentFilter filter = new IntentFilter("android.bluetooth.device.action.PAIRING_REQUEST");
    IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_FOUND);


    BluetoothDeviceConnector mDeviceConnector = new BluetoothDeviceConnector();


    ArrayAdapter<MyBluetoothDevice> myAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final EditText editText = (EditText) findViewById(R.id.editText);
        Button button = (Button) findViewById(R.id.BtnSend);
        Button buttonFind = (Button) findViewById(R.id.BtnFind);

        listView = (ListView) findViewById(R.id.listView);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MyBluetoothDevice myBluetoothDevice = (MyBluetoothDevice) (parent.getItemAtPosition(position));
                mDeviceConnector.setmBluetoothDevice(myBluetoothDevice.getBluetoothDevice());
                mDeviceConnector.createBound();
                mDeviceConnector.connect();
            }
        });
        myAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(editText.getText().toString());
            }
        });

        buttonFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDeviceConnector.getmAdapter().startDiscovery();
                registerReceiver(mReceiver, filter2);
                registerReceiver(pairingRequest, filter);
            }
        });
        listView.setAdapter(myAdapter);
        mDeviceConnector.removeBounds();
    }


    private void sendMessage(CharSequence chars) {
        if (chars.length() > 0) {
            mDeviceConnector.sendAsciiMessage(chars);
        }
    }


    private final BroadcastReceiver pairingRequest = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals("android.bluetooth.device.action.PAIRING_REQUEST")) {
                try {
                    byte[] pin = (byte[]) BluetoothDevice.class.getMethod("convertPinToBytes", String.class).invoke(BluetoothDevice.class, "1234");
                    Method m = mDeviceConnector.getmBluetoothDevice().getClass().getMethod("setPin", byte[].class);
                    m.invoke(mDeviceConnector.getmBluetoothDevice(), pin);
                    mDeviceConnector.getmBluetoothDevice().getClass().getMethod("setPairingConfirmation", boolean.class).invoke(mDeviceConnector.getmBluetoothDevice(), true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                myAdapter.add(new MyBluetoothDevice(device));

            }
        }
    };




    @Override
    protected void onDestroy() {

        unregisterReceiver(pairingRequest);
        unregisterReceiver(mReceiver);
        super.onDestroy();

    }
}
