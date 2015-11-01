package hu.nik.uniobuda.tdk;

/**
 * Created by Peter on 2015.11.01..
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;


public class BluetoothDeviceConnector implements DeviceConnector {

    private static final String TAG = BluetoothDeviceConnector.class.getSimpleName();
    private static final boolean D = true;

    private static final int STATE_NONE = 0;       // we're doing nothing
    private static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    private static final int STATE_CONNECTED = 3;  // now connected to a remote device


    private final BluetoothAdapter mAdapter;
    private BluetoothDevice mBluetoothDevice;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    public int getmState() {
        return mState;
    }

    private int mState;

    public BluetoothDevice getmBluetoothDevice() {
        return mBluetoothDevice;
    }

    public void setmBluetoothDevice(BluetoothDevice mBluetoothDevice) {
        this.mBluetoothDevice = mBluetoothDevice;
    }

    public BluetoothAdapter getmAdapter() {
        return mAdapter;
    }


    public BluetoothDeviceConnector() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
    }


    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
    }

    private BluetoothAdapter getBluetoothAdapter() {
        return BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public synchronized void connect() {
        BluetoothDevice device = getBluetoothAdapter().getRemoteDevice(mBluetoothDevice.getAddress());
        connect(device);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        try {
            mConnectThread = new ConnectThread(device);
            mConnectThread.start();
            setState(STATE_CONNECTING);
        } catch (SecurityException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    @Override
    public synchronized void disconnect() {
        if (D) Log.d(TAG, "shutdown");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.shutdown();
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_NONE);
    }

    @Override
    public void sendAsciiMessage(CharSequence chars) {
        write((chars.toString() + "\n").getBytes());
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    private void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }



    @Override
    public void removeBounds()
    {
        Set<BluetoothDevice> pairedDevices = mAdapter.getBondedDevices();
        if (pairedDevices.size()> 0) {
            for (BluetoothDevice device : pairedDevices) {
                try {
                    Method m = device.getClass()
                            .getMethod("removeBond", (Class[]) null);
                    m.invoke(device, (Object[]) null);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
    }

    @Override
    public void createBound() {
            try {
                Method m = mBluetoothDevice.getClass().getMethod("createBond", (Class[]) null);
                m.invoke(mBluetoothDevice, (Object[]) null);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
    }


    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        setState(STATE_NONE);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        setState(STATE_NONE);
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
            mmDevice = device;
            BluetoothSocket tmp = null;
            try {
                Method m = device.getClass().getMethod("createRfcommSocket", new Class[]{ int.class });
                tmp = (BluetoothSocket) m.invoke(device, 1);  // channel = 1
                Log.i(TAG, "setting socket to result of createRfcommSocket");
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");
             mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                mmSocket.connect();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
                connectionFailed();
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothDeviceConnector.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        private boolean stop = false;
        private boolean hasReadAnything = false;

        public void shutdown() {
            stop = true;
            if (!hasReadAnything) return;
            if (mmInStream != null) {
                try {
                    mmInStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "close() of InputStream failed.");
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

}