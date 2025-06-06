/*
 * Copyright (c) 2010 Jacek Fedorynski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This file is derived from:
 * 
 * http://developer.android.com/resources/samples/BluetoothChat/src/com/example/android/BluetoothChat/DeviceListActivity.html
 * 
 * Copyright (c) 2009 The Android Open Source Project
 */

package org.jfedor.nxtremotecontrol;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Set;

public class ChooseDeviceActivity extends AppCompatActivity {
    
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    private static final int REQUEST_PERMISSIONS_LOCATION = 2;
    
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    private BluetoothAdapter mBtAdapter; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);
        setResult(Activity.RESULT_CANCELED);
        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                doDiscovery();
                v.setVisibility(View.GONE);
            }
        });
        
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);
        
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);
        
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);
        
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
        
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        
        boolean empty = true;
        
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if ((device.getBluetoothClass() != null) && (device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.TOY_ROBOT)) {
                    mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    empty = false;
                }
            }
        }
        if (!empty) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            findViewById(R.id.no_devices).setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (mBtAdapter != null) {
            try {
                mBtAdapter.cancelDiscovery();
            } catch (SecurityException e) {
            }
        }
        
        this.unregisterReceiver(mReceiver);
    }

    private void doDiscovery() {
        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)) {
            reallyDoDiscovery();
        } else {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                reallyDoDiscovery();
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setMessage("In order to scan for devices, you have to grant this app location access.")
                        .setPositiveButton("OK", (dialog, which) -> {
                            requestLocationPermissions();
                        }).show();
            } else {
                requestLocationPermissions();
            }
        }
    }

    void requestLocationPermissions() {
        requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION
        }, REQUEST_PERMISSIONS_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS_LOCATION) {
            boolean allGranted = true;
            for (int granted : grantResults) {
                if (granted != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (grantResults.length > 0 && allGranted) {
                reallyDoDiscovery();
            } else {
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setMessage("To scan for devices, you have to grant this app location access. If you don't want to do that, you can pair your NXT brick in system settings. If you're seeing this message and the app didn't even request location access, you'll have to grant it in system settings, or uninstall and reinstall the app.")
                        .setPositiveButton("OK", (dialog, which) -> {
                        }).show();
            }
        }
    }

    private void reallyDoDiscovery() {
        setProgressBarIndeterminateVisibility(true);
        setTitle("Scanning...");
        
        //findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
        
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }
        
        mBtAdapter.startDiscovery();
        
        mNewDevicesArrayAdapter.clear();
        findViewById(R.id.title_new_devices).setVisibility(View.GONE);
        if (mPairedDevicesArrayAdapter.getCount() == 0) {
            findViewById(R.id.no_devices).setVisibility(View.VISIBLE);
        }
    }

    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            try {
                mBtAdapter.cancelDiscovery();
            } catch (SecurityException e) {
            }
            
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
            
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };
    
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if ((device.getBondState() != BluetoothDevice.BOND_BONDED) && (device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.TOY_ROBOT)) {
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
                    findViewById(R.id.no_devices).setVisibility(View.GONE);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle("Select device");
                findViewById(R.id.button_scan).setVisibility(View.VISIBLE);
            }
        }
    };
}
