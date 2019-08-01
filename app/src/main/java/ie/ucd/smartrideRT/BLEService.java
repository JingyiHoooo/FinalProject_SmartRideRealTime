package ie.ucd.smartrideRT;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;



/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */

public class BLEService extends Service {
    private static byte[] testData = { 1, 2, 3 };


    private Context mContext;// 上下文
    private static final String TAG = BLEService.class.getSimpleName();// TAG


    private BluetoothManager mBluetoothManager = null;// 蓝牙管理器
    private BluetoothAdapter mBluetoothAdapter = null;// 本地设备
    private String mBluetoothDeviceAddress = null;// 远程设备地址
    private BluetoothGatt mBluetoothGatt = null;// GATT通信
    private BluetoothGattCharacteristic mCharacteristic = null;// 可读写可通知的

    private ArrayList<BluetoothDevice> listDevice;
    private List<BluetoothGattService> serviceList;//服务
    private List<BluetoothGattCharacteristic> characterList;//特征

    MyDBHandler dbHandler;

    private SharedPreferences sp = null;


    private static final boolean AUTO_CONNECT = true;// 是否自动连接
    private static final boolean NOTIFICATION_ENABLED = true;


    private int mConnectionState = STATE_DISCONNECTED;// 连接状态
    private static final int STATE_DISCONNECTED = 0;// 断开连接
    private static final int STATE_CONNECTING = 1;// 连接中
    private static final int STATE_CONNECTED = 2;// 已连接

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Bluetooth is set up as a service which allows it to run in the background across multiple activities
    public class LocalBinder extends Binder {
        BLEService getService() {
            return BLEService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        initialize();
        //Get ability to pull data from database
        dbHandler = new MyDBHandler(this, null, null, 1);
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */

    public boolean initialize() {
        //listDevice = new ArrayList<BluetoothDevice>();
        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            toast("BLE not support this device");
            ((Activity) mContext).finish();
        }
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
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
            // request for turn on the Bluetooth
            //startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            //Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            //return false;
        }

        return true;
    }


    @Override
    public void onCreate() {
// TODO Auto-generated method stub
        super.onCreate();
        initialize();
    }


    @Override
    public void onDestroy() {
// TODO Auto-generated method stub
        super.onDestroy();
        suiside();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
// TODO Auto-generated method stub
// 如果地址不为空就尝试连接 如果为空就跳到BLEActivity让用户去选择BLE设备
        if (mBluetoothDeviceAddress != null) {
            if (connect(mBluetoothDeviceAddress)) {
            } else {
            }
        } else {
            Intent bleIntent = new Intent();
            bleIntent.setClass(mContext, MainActivity.class);
            bleIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(bleIntent);
        }
// return super.onStartCommand(intent, flags, startId);
        return Service.START_STICKY;
    }



    /**
     * 吐丝
     */
    private void toast(String text) {
        Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
    }


    /**
     * 自殺
     */
    private void suiside() {
        disconnect();
        close();
        try {
            if (mBluetoothAdapter != null) {
                mBluetoothAdapter.disable();
                mBluetoothAdapter = null;
                mBluetoothManager = null;
                sp = null;
            }
        } catch (Exception e) {
            toast("关闭蓝牙失败，请手动关闭");
        }
    }


    /**
     * 关闭通信
     */
    private void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }


    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    private boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null
                && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }


        final BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, AUTO_CONNECT, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }


    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    private void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }


    /**
     * GATT communication callback
     */
    @SuppressLint("NewApi")
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
// TODO Auto-generated method stub
// super.onCharacteristicChanged(gatt, characteristic);
            System.out.println("我收到的：" + new String(characteristic.getValue()));
    }


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
// TODO Auto-generated method stub
            if (mBluetoothAdapter == null || mBluetoothGatt == null) {
                Log.w(TAG, "BluetoothAdapter not initialized");
                return;
            }
            Log.e(TAG, "onCharacteristicRead");
            super.onCharacteristicRead(gatt, characteristic, status);

        }


        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
// TODO Auto-generated method stub
            Log.e(TAG, "onCharacteristicWrite");
            super.onCharacteristicWrite(gatt, characteristic, status);
        }


        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
// TODO Auto-generated method stub
// super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (mBluetoothGatt != null)
                    mBluetoothGatt.discoverServices(); //search the services support by the connected devices
                mConnectionState = STATE_CONNECTED;
                System.out.println("state connected");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
// connect(mBluetoothDeviceAddress);
                mConnectionState = STATE_DISCONNECTED;
                System.out.println("state disconnected");
            }


        }


        @Override
        public void onDescriptorRead(BluetoothGatt gatt,
                                     BluetoothGattDescriptor descriptor, int status) {
// TODO Auto-generated method stub
            super.onDescriptorRead(gatt, descriptor, status);
        }


        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
// TODO Auto-generated method stub
            super.onDescriptorWrite(gatt, descriptor, status);
        }


        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
// TODO Auto-generated method stub
            super.onReadRemoteRssi(gatt, rssi, status);
        }


        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
// TODO Auto-generated method stub
            super.onReliableWriteCompleted(gatt, status);
        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            //当设备是否找到服务时，会回调该函数
// TODO Auto-generated method stub
// super.onServicesDiscovered(gatt, status);
            Log.d(TAG, "onServicesDiscovered");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (mBluetoothGatt != null) {
                    BluetoothGattService mGattService = mBluetoothGatt
                            .getService(MY_UUID);
                    mCharacteristic = mGattService
                            .getCharacteristic(MY_UUID);
                    List<BluetoothGattDescriptor> mDescriptors = mCharacteristic
                            .getDescriptors();
                    for (BluetoothGattDescriptor mDescriptor : mDescriptors) {
                        System.out.println(mDescriptor.getUuid().toString());
                    }


                    setCharacteristicNotification(mCharacteristic,
                            NOTIFICATION_ENABLED);


                    wirteToBLE(testData);
                    System.out.println(new String(readFromBLE()));
                    wirteToBLE(testData);
                    System.out.println(new String(readFromBLE()));
                }
            }
        }


    };

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */


    private void setCharacteristicNotification(
            BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

//Get the features in the specified feature of the device, where it is monitored,
// setCharacteristicNotification and the above callback onCharacteristicChanged one by one
        //官方规定接收通知描述符的UUID
        BluetoothGattDescriptor descriptor = characteristic
                .getDescriptor(MY_UUID);
        //UUID.fromString(descriptorUUID)
        if (descriptor != null) {//不需要回复的通知 //需要回复确认的通知
            descriptor
                    .setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }


    /**
     * 获取此实例
     * 
     * @return
     */
    /*
    public static BLEService self() {
        if (mContext != null)
            return (BLEService) mContext;
        return null;
    }
    */


    /**
     * 通信接口 通过此函数即可向BLE设备写入数据
     * 
     * @param valueOut
     * @return
     */
    public boolean wirteToBLE(byte[] valueOut) {


        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }

        String commandSent;
        commandSent = new String(valueOut);
        // Need to remove the '!' character from this string before entering in database
        commandSent = commandSent.replace("!", "");

        mCharacteristic.setValue(commandSent);

        CommandSentData commandSentData = new CommandSentData(commandSent);
        dbHandler.addCommandSentRow(commandSentData);

        boolean isSuccess = mBluetoothGatt.writeCharacteristic(mCharacteristic);
        return isSuccess;
    }


    /**
     * Receive data from BLE device
     * 
     * @return valueIn
     */
    public byte[] readFromBLE() {
        byte[] valueIn = null;


        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return null;
        }


        boolean isSuccess = mBluetoothGatt.readCharacteristic(mCharacteristic);
        if (isSuccess) {
            valueIn = mCharacteristic.getValue();
        }


        return valueIn;
    }
}
