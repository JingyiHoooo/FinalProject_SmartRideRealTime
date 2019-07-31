package ie.ucd.smartrideRT;


import java.util.List;


import android.annotation.SuppressLint;
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
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;


/**
  * @date 2015-07-22
  * @detail BLE连接通信
  * @author Leo
  * 
  */
@SuppressLint("NewApi")
public class BLEService extends Service {
    private static byte[] testData = { 1, 2, 3 };


    private static Context mContext;// 上下文
    private static final String TAG = BLEService.class.getSimpleName();// TAG


    private BluetoothManager mBluetoothManager = null;// 蓝牙管理器
    private BluetoothAdapter mBluetoothAdapter = null;// 本地设备
    private String mBluetoothDeviceAddress = null;// 远程设备地址
    private BluetoothGatt mBluetoothGatt = null;// GATT通信
    private BluetoothGattCharacteristic mCharacteristic = null;// 可读写可通知的


    private SharedPreferences sp = null;


    private static final boolean AUTO_CONNECT = true;// 是否自动连接
    private static final boolean NOTIFICATION_ENABLED = true;


    private int mConnectionState = STATE_DISCONNECTED;// 连接状态
    private static final int STATE_DISCONNECTED = 0;// 断开连接
    private static final int STATE_CONNECTING = 1;// 连接中
    private static final int STATE_CONNECTED = 2;// 已连接


    @Override
    public IBinder onBind(Intent arg0) {
// TODO Auto-generated method stub
        return null;
    }


    @Override
    public void onCreate() {
// TODO Auto-generated method stub
        super.onCreate();
        init();
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
            bleIntent.setClass(mContext, BLEActivity.class);
            bleIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(bleIntent);
        }
// return super.onStartCommand(intent, flags, startId);
        return Service.START_STICKY;
    }


    /**
     * 各种初始化信息
     */
    private void init() {
        mContext = this;
        if (!initBluetooth()) {
            stopSelf();
        }
        sp = getSharedPreferences(Constant.SP_NAME, Context.MODE_PRIVATE);
        mBluetoothDeviceAddress = sp.getString(Constant.KEY_DEVICE_ADDRESS,
                null);
    }


    /**
     * 吐丝
     */
    private void toast(String text) {
        Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
    }


    /**
     * 初始化BluetoothManager和BluetoothAdapter
     * 
     * @return
     */
    private boolean initBluetooth() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }


        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            return false;
        }


        return true;
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
     * 建立通信連接
     * 
     * @param address
     * @return
     */
    private boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            return false;
        }


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
            return false;
        }
        mBluetoothGatt = device.connectGatt(this, AUTO_CONNECT, mGattCallback);
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }


    /**
     * 斷開GATT連接
     */
    private void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.disconnect();
    }


    /**
     * GATT通信回調函數
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
            super.onCharacteristicRead(gatt, characteristic, status);
        }


        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
// TODO Auto-generated method stub
            super.onCharacteristicWrite(gatt, characteristic, status);
        }


        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
// TODO Auto-generated method stub
// super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (mBluetoothGatt != null)
                    mBluetoothGatt.discoverServices();
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
// TODO Auto-generated method stub
// super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (mBluetoothGatt != null) {
                    BluetoothGattService mGattService = mBluetoothGatt
                            .getService(SampleGattAttributes.UUID_SERVICE);
                    mCharacteristic = mGattService
                            .getCharacteristic(SampleGattAttributes.UUID_CHARACTERISTIC);
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
     * 设置后可以使用通知 设备给手机发送通知时可触发onCharacteristicChanged()
     * 
     * @param characteristic
     * @param enabled
     */


    private void setCharacteristicNotification(
            BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        BluetoothGattDescriptor descriptor = characteristic
                .getDescriptor(SampleGattAttributes.UUID_DESCRIPTOR);
        if (descriptor != null) {
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
    public static BLEService self() {
        if (mContext != null)
            return (BLEService) mContext;
        return null;
    }


    /**
     * 通信接口 通过此函数即可向BLE设备写入数据
     * 
     * @param value
     * @return
     */
    public boolean wirteToBLE(byte[] value) {


        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }


        mCharacteristic.setValue(value);
        boolean isSuccess = mBluetoothGatt.writeCharacteristic(mCharacteristic);
        return isSuccess;
    }


    /**
     * 通信接口 从BLE设备读数据
     * 
     * @return
     */
    public byte[] readFromBLE() {
        byte[] value = null;


        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return null;
        }


        boolean isSuccess = mBluetoothGatt.readCharacteristic(mCharacteristic);
        if (isSuccess) {
            value = mCharacteristic.getValue();
        }


        return value;
    }
}
