package hu.nik.uniobuda.tdk;

import android.bluetooth.BluetoothDevice;

/**
 * Created by Peter on 2015.10.31..
 */
public class MyBluetoothDevice {

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    private BluetoothDevice bluetoothDevice;


    public MyBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    @Override
    public String toString() {
        return bluetoothDevice.getName();
    }
}
