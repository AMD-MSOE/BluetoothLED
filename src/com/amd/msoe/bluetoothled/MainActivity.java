package com.amd.msoe.bluetoothled;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private static final String TAG = "Bluetooth LED";
	
	private static final int REQUEST_ENABLE_BT = 5;
	private BluetoothAdapter mBluetoothAdapter;
	private ListView lstBluetoothDevices;
	private ArrayAdapter<String> mArrayAdapter;
	private List<String> bluetoothDevices;
	private boolean ledOn;
	private Set<BluetoothDevice> pairedDevices;
	private IntentFilter intentFilter;
	
	private BroadcastReceiver intentReceiver = new BroadcastReceiver() {  // Abstract class
        public void onReceive(Context context, Intent receivedIntent) {
            if (BluetoothDevice.ACTION_FOUND.equals(receivedIntent.getAction())) 
            {
                BluetoothDevice foundDevice = receivedIntent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                
                mArrayAdapter.add(foundDevice.getName() + "\n" + foundDevice.getAddress());
		        mArrayAdapter.notifyDataSetChanged();
		        
                Log.v(TAG, foundDevice.getName()+" "+foundDevice.getAddress()+" was found");
            }
        }//END onReceive
    };//END BroadcastReceiver

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		intentFilter = new IntentFilter();
	    intentFilter.addAction("ACTION_FOUND");
		
		ledOn = false;
		
		lstBluetoothDevices = (ListView)findViewById(R.id.activity_main_lst_bluetooth_devices);
		lstBluetoothDevices.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				sendLEDSignal(position);
			}
			
		});
		bluetoothDevices = new ArrayList<String>();
		mArrayAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, bluetoothDevices);
		
		lstBluetoothDevices.setAdapter(mArrayAdapter);
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
		    // Device does not support Bluetooth
			// TODO: show a toast saying you need bluetooth and close the app
			Toast.makeText(MainActivity.this, "You need bluetooth to use this app", Toast.LENGTH_SHORT).show();
			System.exit(0);
		}
		
		if (!mBluetoothAdapter.isEnabled()) {
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		} else {
			findBluetoothDevice();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent){
		if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK) {
			Toast.makeText(MainActivity.this, "ok", Toast.LENGTH_SHORT).show();

		} else {
			Toast.makeText(MainActivity.this, "Not ok", Toast.LENGTH_SHORT).show();
		}
		if (mBluetoothAdapter.isEnabled()) {
		    findBluetoothDevice();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	public void findBluetoothDevice() {
		pairedDevices = mBluetoothAdapter.getBondedDevices();
		mBluetoothAdapter.startDiscovery();
	    
		// If there are paired devices
		if (pairedDevices.size() > 0) {
		    // Loop through paired devices
		    for (BluetoothDevice device : pairedDevices) {
		        // Add the name and address to an array adapter to show in a ListView
		        mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
		        mArrayAdapter.notifyDataSetChanged();
		    }
		}
	}

	public void sendLEDSignal(int position) {
		BluetoothDevice device = (BluetoothDevice)pairedDevices.toArray()[position];
		try {
			BluetoothSocket socket = device.createRfcommSocketToServiceRecord(UUID.fromString("23bfd07d-92a5-460d-a806-de1928cc7e85"));
			Log.d(TAG,"waiting for connection");
			socket.connect();
			Log.d(TAG,"made connection");
			if (ledOn) {
				//send the off signal
				socket.getOutputStream().write(0);
			} else {
				//send the on signal
				socket.getOutputStream().write(255);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	protected void onResume() {
	    super.onResume();
	    registerReceiver(intentReceiver, intentFilter);
	}

	@Override
	protected void onPause() {
	    super.onPause();
	    if (intentReceiver != null) {
	        unregisterReceiver(intentReceiver);
	    }
	}
}

