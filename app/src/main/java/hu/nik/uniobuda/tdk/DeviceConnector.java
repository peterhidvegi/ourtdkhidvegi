package hu.nik.uniobuda.tdk;

import android.bluetooth.BluetoothAdapter;

/**
 * Created by Peter on 2015.11.01..
 */
public interface DeviceConnector {

    void connect();

    void disconnect();

    void sendAsciiMessage(CharSequence chars);

    void removeBounds();

    void createBound();

}