/*
* Class Name: MainActivity.java
* Corresponding layout: activity_main.xml
* Author: Shaun Sweeney - shaun.sweeney@ucdconnect.ie // shaunsweeney12@gmail.com
* Date: March 2017
* Description: MainActivity is the launcher activity. It displays a combined list of bluetooth devices
* that are within range and devices that have already been paired with on the home screen. It also
* has a number of buttons which are used to launch other activities.
* */


package ie.ucd.smartrideRT;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Bundle;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.os.IBinder;
import android.content.ServiceConnection;
import android.content.ComponentName;
import java.util.ArrayList;
import ie.ucd.smartrideRT.BluetoothService.BluetoothMyLocalBinder;

public class MainActivity extends Activity implements OnItemClickListener {

    private static final String tag = "debugging";

    //private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private ListView mListView = null;
    private BLEAdapter mAdapter = null;


    private BLEService mBluetoothLeService;
    BluetoothService bluetoothService;
    boolean bluetoothIsBound=false;
    ArrayAdapter<String> activityListAdapter;
    ListView listView;
    IntentFilter filter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();

        //start service for bluetooth connection to retrieve bluetooth devices
        Intent i = new Intent(this, BLEService.class);
        bindService(i, BLEServiceConnection, Context.BIND_AUTO_CREATE);

        //register broadcast receiver to produce list of all available devices for Bluetooth connection
        registerDeviceReceiver();
    }

    //init initialises variables especially the Adapter to store all devices found by Bluetooth
    private void init() {
        listView = (ListView) findViewById(R.id.listView);
        listView.setOnItemClickListener(this);
        activityListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, 0);
        listView.setAdapter(activityListAdapter);
    }

    //this method registers the BroadcastReceiver to listen to BluetoothService for the devices
    // that are available/paired so they can be printed to the screen for user selection
    private void registerDeviceReceiver() {
        filter = new IntentFilter("ie.ucd.smartrideRT");
        registerReceiver(MyReceiver, filter);
    }

    //BroadcastReceiver listens for available devices from BluetoothService
    private final BroadcastReceiver MyReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String deviceDataReceived = intent.getStringExtra("device");
            activityListAdapter.add(deviceDataReceived);
        }
    };

    //bluetoothServiceConnection is required for Bluetooth to work in the background
    private ServiceConnection BLEServiceConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName name, IBinder service){
            mBluetoothLeService = ((BLEService.LocalBinder) service).getService();
            bluetoothIsBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name){
            mBluetoothLeService = null;
            bluetoothIsBound = false;
        }

    };

//    @Override
//    public void onDestroy(){
//        super.onDestroy();
//
//        //bluetoothService.onDestroy();
//    }

    //method to manage what happens when user clicks one of the devices that have been found by bluetooth
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                            long arg3) {

        if (activityListAdapter.getItem(arg2).contains("Paired")) {
            //Log.i(tag, "Checking if device " + activityListAdapter.getItem(arg2) + " is paired.");
            // Take the address out of the string
            String deviceAddress = activityListAdapter.getItem(arg2).split("[\\r\\n]")[1];
            Log.i(tag, "Device address clicked is " + deviceAddress);

            //bluetoothService.connectToPairedDevice(deviceAddress);
            bluetoothService.checkifPaired(arg2);
        } else {
            Toast.makeText(getApplicationContext(), "device is not paired", Toast.LENGTH_SHORT).show();
        }
    }

    /* The following methods launch different activities depending on the user selection*/

    // Method to launch activity to send manual command to bike
    public void goToSendCommand(View view){
        Log.i(tag, "About to launch SendCommand");
        Intent i = new Intent(this, SendCommand.class);
        startActivity(i);}


/*
    // Method to launch activity to view data saved in database
    public void goToDb(View view){
        Log.i(tag, "Launching database activity");
        Intent j = new Intent(this, ViewData.class);
        startActivity(j);
    }

    //method to launch activity to start closed loop feedback control for target calories burned
    public void startCaloriesActivity(View view){
        Log.i(tag, "Launching calories activity");
        Intent k = new Intent(this, MicrosoftBand.class);
        startActivity(k);
    }

    //method to launch activity to view data saved in the calories control feedback
    public void ViewCaloriesControlActivity(View view){
        Log.i(tag, "Launching view cals control activity");
        Intent m = new Intent(this, ViewCaloriesControlData.class);
        startActivity(m);
    }

    //method to launch activity to minimise cyclist inhalation of pollutants
    public void StartPollutionControlActivity(View view){
        Log.i(tag, "Launching proactive pollution control activity");
        Intent m = new Intent(this, ProactivePollutionControl.class);
        startActivity(m);
    }

    public void startTrafficLightNudgingControlActivity(View view){
        Log.i(tag, "Launching TrafficLightNudgingControl activity");
        Intent n = new Intent(this, TrafficLightNudgingControl.class);
        startActivity(n);
    }

    public void startCooperativeCompetitiveControlActivity(View view){
        Log.i(tag, "Launching CooperativeCompetitiveControl activity");
        Intent n = new Intent(this, CooperativeCompetitiveControl.class);
        startActivity(n);
    }
    */
}