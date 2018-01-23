package com.radinet.ble_app;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.UUID;

/**
 * Created by user on 2017/3/15.
 */

public class BluetoothLeService extends Service {

    final static String TAG = BluetoothLeService.class.getSimpleName();

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "android-er.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "android-er.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "android-er.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "android-er.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "android-er.EXTRA_DATA";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    public static String mBluetoothDeviceAddress;
    public static BluetoothGatt mBluetoothGatt;
    private final IBinder mBinder = new LocalBinder();

    private int mConnectionState = STATE_DISCONNECTED;
/**
 imed_alert_service_UUID = CBUUID(string:"1802")
 imed_alert_char_UUID = CBUUID(string:"2A06") //1802:2A06

 tx_service_UDID = CBUUID(string:"FFF0") //FFF0:FFF5
 tx_char_UUID    = CBUUID(string:"FFF5")

 rx_service_UDID = CBUUID(string:"FFE0")
 rx_char_UUID    = CBUUID(string:"FFE2") //FFE0:FFE2
 key_char_UUID   = CBUUID(string:"FFE1") //FFE0:FFE1
 * **/
    public static BluetoothGattCharacteristic mReadCharacteristric = null;
    public static BluetoothGattCharacteristic mWriteCharacteristric = null;
    public static int mReconnectCount = 0;
    public static final UUID TRANSFER_SERVICE_READ = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB");
    public static final UUID TRANSFER_CHARACTERISTIC_READ = UUID.fromString("0000FFE2-0000-1000-8000-00805F9B34FB");
    public static final UUID TRANSFER_SERVICE_WRITE = UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB");
    public static final UUID TRANSFER_CHARACTERISTIC_WRITE = UUID.fromString("0000FFF5-0000-1000-8000-00805F9B34FB");
    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    /**取得mBluetoothManager及mBluetoothAdapter**/
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**GattServer CallBack**/
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                //  GattServer的狀態為STATE_CONNECTED
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (mBluetoothGatt == null) {
                    return;
                }
                mBluetoothGatt.disconnect();
                mBluetoothGatt.close();
                mReconnectCount++;
                intentAction = ACTION_GATT_DISCONNECTED;
                broadcastUpdate(intentAction);
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");

                Log.d("mReconnectCount", "" + mReconnectCount);
            }
            if (status == 133) {
                close();
            }
        }
        public void close() {
            Log.w(TAG, "mBluetoothGatt closed");
            //mBluetoothDeviceAddress = null;
            mBluetoothGatt = null;
//            if (mReconnectCount <= 5) {
                connect(mBluetoothDeviceAddress);
//            }
        }
        /**藉UUID取得Service**/
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);

                BluetoothGattService btGattReadService = mBluetoothGatt.getService(TRANSFER_SERVICE_READ);
                if (btGattReadService != null) {
                    mReadCharacteristric = btGattReadService.getCharacteristic(TRANSFER_CHARACTERISTIC_READ);
                    if (mReadCharacteristric != null) {
                        mBluetoothGatt.setCharacteristicNotification(mReadCharacteristric, true);
                        mBluetoothGatt.readCharacteristic(mReadCharacteristric);
                    }
                }
                BluetoothGattService btGattWriteService = mBluetoothGatt.getService(TRANSFER_SERVICE_WRITE);
                if (btGattWriteService != null) {
                    mWriteCharacteristric = btGattWriteService.getCharacteristic(TRANSFER_CHARACTERISTIC_WRITE);
                    if (mWriteCharacteristric != null) {
                        mBluetoothGatt.setCharacteristicNotification(mWriteCharacteristric, true);
                    }
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.d("test", "Tark Read");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("test", "Tark Read");
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("test", "Tark write"+status);
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
//            Log.d("test", "Tark change");
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

//        // Previously connected device.  Try to reconnect.
//        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
//                && mBluetoothGatt != null) {
//            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
//            if (mBluetoothGatt.connect()) {
//                mConnectionState = STATE_CONNECTING;
//                return true;
//            } else {
//                return false;
//            }
//        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);

        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**從藍牙收到狀態，傳遞出去**/
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /**從藍牙收到資料，傳遞出去**/
    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        final byte[] data = characteristic.getValue();
//                Log.d("Get", "success");
//        StringBuffer buffer = new StringBuffer("0x");
//        int i;
//        int c1 = 0;
//        //Log.d("test", "1:");
//        for (byte b : data) {
//            i = b & 0xff;
//            //buffer.append(Integer.toHexString(i));
//            Log.d("test", "read data:"+ String.valueOf(c1)+":" + Integer.toHexString(i));
//            c1++;
//        }
        intent.putExtra(ACTION_DATA_AVAILABLE, data);
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


}
