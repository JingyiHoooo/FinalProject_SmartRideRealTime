package ie.ucd.smartrideRT;

import java.util.ArrayList;


import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;


/**
  * @date 2015-07-22
  * @detail 设备列表适配器
  * @author Leo
  * 
  */
@SuppressLint("InflateParams")
public class BLEAdapter extends BaseAdapter {
    private ArrayList<BluetoothDevice> mLeDevices;
    private LayoutInflater mInflater;


    public BLEAdapter(Context context) {
        super();
        mLeDevices = new ArrayList<BluetoothDevice>();
        mInflater = LayoutInflater.from(context);
    }


    @Override
    public int getCount() {
// TODO Auto-generated method stub
        return mLeDevices.size();
    }


    @Override
    public Object getItem(int arg0) {
// TODO Auto-generated method stub
        if (mLeDevices.size() <= arg0) {
            return null;
        }
        return mLeDevices.get(arg0);
    }


    @Override
    public long getItemId(int arg0) {
// TODO Auto-generated method stub
        return arg0;
    }


    @Override
    public View getView(int position, View view, ViewGroup arg2) {
// TODO Auto-generated method stub
        ViewHolder viewHolder;


        if (null == view) {
            viewHolder = new ViewHolder();
            view = mInflater.inflate(R.layout.listitem_device, null);
            viewHolder.deviceName = (TextView) view
                    .findViewById(R.id.device_name);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }


        BluetoothDevice device = mLeDevices.get(position);


        final String deviceName = device.getName();
        if (deviceName != null && deviceName.length() > 0)
            viewHolder.deviceName.setText(deviceName);
        else
            viewHolder.deviceName.setText("Unknown device");


        return view;
    }


    /**
     * 列表显示控件类
     * 
     * @author Leo
     * 
     */
    private class ViewHolder {
        public TextView deviceName;
    }


    /**
     * 设置数据源
     * 
     * @param mDevices
     */
    public void setData(ArrayList<BluetoothDevice> mDevices) {
        this.mLeDevices = mDevices;
    }


    /**
     * 添加设备
     * 
     * @param mDevice
     */
    public void addDevice(BluetoothDevice mDevice) {
        if (!mLeDevices.contains(mDevice)) {
            mLeDevices.add(mDevice);
        }
    }


    /**
     * 得到设备
     * 
     * @param position
     * @return
     */
    public BluetoothDevice getDevice(int position) {
        if (mLeDevices.size() <= position) {
            return null;
        }
        return mLeDevices.get(position);
    }


    /**
     * 清空设备
     */
    public void clear() {
        mLeDevices.clear();
    }


}
