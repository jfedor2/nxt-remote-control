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
 * http://developer.android.com/resources/samples/BluetoothChat/src/com/example/android/BluetoothChat/BluetoothChat.html
 * 
 * Copyright (c) 2009 The Android Open Source Project
 */

package org.jfedor.nxtremotecontrol;

/*
 * TODO:
 * 
 * tilt controls
 */

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class NXTRemoteControl extends AppCompatActivity implements OnSharedPreferenceChangeListener {
    
    private boolean NO_BT = false; 
    
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CONNECT_DEVICE = 2;
    private static final int REQUEST_SETTINGS = 3;

    private static final int REQUEST_PERMISSIONS_BLUETOOTH = 1;
    
    public static final int MESSAGE_TOAST = 1;
    public static final int MESSAGE_STATE_CHANGE = 2;
    
    public static final String TOAST = "toast";
    
    private static final int MODE_BUTTONS = 1;
    private static final int MODE_TOUCHPAD = 2;
    private static final int MODE_TANK = 3;
    private static final int MODE_TANK3MOTOR = 4;
    
    private BluetoothAdapter mBluetoothAdapter;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private NXTTalker mNXTTalker;
    
    private int mState = NXTTalker.STATE_NONE;
    private int mSavedState = NXTTalker.STATE_NONE;
    private boolean mNewLaunch = true;
    private String mDeviceAddress = null;
    private TextView mStateDisplay;
    private Button mConnectButton;
    private Button mDisconnectButton;
    private TouchPadView mTouchPadView;
    private TankView mTankView;
    private Tank3MotorView mTank3MotorView;
    private Menu mMenu;
    
    private int mPower = 80;
    private int mControlsMode = MODE_BUTTONS;
    
    private boolean mReverse;
    private boolean mReverseLR;
    private boolean mRegulateSpeed;
    private boolean mSynchronizeMotors;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.i("NXT", "NXTRemoteControl.onCreate()");
        
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        readPreferences(prefs, null);
        prefs.registerOnSharedPreferenceChangeListener(this);
        
        if (savedInstanceState != null) {
            mNewLaunch = false;
            mDeviceAddress = savedInstanceState.getString("device_address");
            if (mDeviceAddress != null) {
                mSavedState = NXTTalker.STATE_CONNECTED;
            }
            
            if (savedInstanceState.containsKey("power")) {
                mPower = savedInstanceState.getInt("power");
            }
            if (savedInstanceState.containsKey("controls_mode")) {
                mControlsMode = savedInstanceState.getInt("controls_mode");
            }
        }
        
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "NXT Remote Control");
        
        if (!NO_BT) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
       
            if (mBluetoothAdapter == null) {
                Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        
        setupUI();
        
        mNXTTalker = new NXTTalker(mHandler);
    }

    private class DirectionButtonOnTouchListener implements OnTouchListener {
        
        private double lmod;
        private double rmod;
        
        public DirectionButtonOnTouchListener(double l, double r) {
            lmod = l;
            rmod = r;
        }
        
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            //Log.i("NXT", "onTouch event: " + Integer.toString(event.getAction()));
            int action = event.getAction();
            //if ((action == MotionEvent.ACTION_DOWN) || (action == MotionEvent.ACTION_MOVE)) {
            if (action == MotionEvent.ACTION_DOWN) {
                byte power = (byte) mPower;
                if (mReverse) {
                    power *= -1;
                }
                byte l = (byte) (power*lmod);
                byte r = (byte) (power*rmod);
                if (!mReverseLR) {
                    mNXTTalker.motors(l, r, mRegulateSpeed, mSynchronizeMotors);
                } else {
                    mNXTTalker.motors(r, l, mRegulateSpeed, mSynchronizeMotors);
                }
            } else if ((action == MotionEvent.ACTION_UP) || (action == MotionEvent.ACTION_CANCEL)) {
                mNXTTalker.motors((byte) 0, (byte) 0, mRegulateSpeed, mSynchronizeMotors);
            }
            return true;
        }
    }
    
    private class TankOnTouchListener implements OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            TankView tv = (TankView) v;
            float y;
            int action = event.getAction();
            if ((action == MotionEvent.ACTION_DOWN) || (action == MotionEvent.ACTION_MOVE)) {
                byte l = 0;
                byte r = 0;
                for (int i = 0; i < event.getPointerCount(); i++) {
                    y = -1.0f*(event.getY(i)-tv.mZero)/tv.mRange;
                    if (y > 1.0f) {
                        y = 1.0f;
                    }
                    if (y < -1.0f) {
                        y = -1.0f;
                    }
                    if (event.getX(i) < tv.mWidth/2f) {
                        l = (byte) (y * 100);
                    } else {
                        r = (byte) (y * 100);
                    }
                }
                if (mReverse) {
                    l *= -1;
                    r *= -1; 
                }
                if (!mReverseLR) {
                    mNXTTalker.motors(l, r, mRegulateSpeed, mSynchronizeMotors);
                } else {
                    mNXTTalker.motors(r, l, mRegulateSpeed, mSynchronizeMotors);
                }
            } else if ((action == MotionEvent.ACTION_UP) || (action == MotionEvent.ACTION_CANCEL)) {
                mNXTTalker.motors((byte) 0, (byte) 0, mRegulateSpeed, mSynchronizeMotors);
            }
            return true;
        }
    }
    
    private class Tank3MotorOnTouchListener implements OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            Tank3MotorView t3v = (Tank3MotorView) v;
            float x;
            float y;
            int action = event.getAction();
            if ((action == MotionEvent.ACTION_DOWN) || (action == MotionEvent.ACTION_MOVE)) {
                byte l = 0;
                byte r = 0;
                byte a = 0;
                for (int i = 0; i < event.getPointerCount(); i++) {
                    y = -1.0f*(event.getY(i)-t3v.mZero)/t3v.mRange;
                    if (y > 1.0f) {
                        y = 1.0f;
                    }
                    if (y < -1.0f) {
                        y = -1.0f;
                    }
                    x = event.getX(i);
                    if (x < t3v.mWidth/3f) {
                        l = (byte) (y * 100);
                    } else if (x > 2*t3v.mWidth/3f) {
                        r = (byte) (y * 100);
                    } else {
                        a = (byte) (y * 100);
                    }
                }
                if (mReverse) {
                    l *= -1;
                    r *= -1; 
                    a *= -1;
                }
                if (!mReverseLR) {
                    mNXTTalker.motors3(l, r, a, mRegulateSpeed, mSynchronizeMotors);
                } else {
                    mNXTTalker.motors3(r, l, a, mRegulateSpeed, mSynchronizeMotors);
                }
            } else if ((action == MotionEvent.ACTION_UP) || (action == MotionEvent.ACTION_CANCEL)) {
                mNXTTalker.motors3((byte) 0, (byte) 0, (byte) 0, mRegulateSpeed, mSynchronizeMotors);
            }
            return true;
        }
    }
    
    private class TouchpadOnTouchListener implements OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            TouchPadView tpv = (TouchPadView) v;
            float x, y, power;
            int action = event.getAction();
            if ((action == MotionEvent.ACTION_DOWN) || (action == MotionEvent.ACTION_MOVE)) {
                x = (event.getX()-tpv.mCx)/tpv.mRadius;
                y = -1.0f*(event.getY()-tpv.mCy);
                if (y > 0f) {
                    y -= tpv.mOffset;
                    if (y < 0f) {
                        y = 0.01f;
                    }
                } else if (y < 0f) {
                    y += tpv.mOffset;
                    if (y > 0f) {
                        y = -0.01f;
                    }
                }
                y /= tpv.mRadius;
                float sqrt22 = 0.707106781f;
                float nx = x*sqrt22 + y*sqrt22;
                float ny = -x*sqrt22 + y*sqrt22;
                power = (float) Math.sqrt(nx*nx+ny*ny);
                if (power > 1.0f) {
                    nx /= power;
                    ny /= power;
                    power = 1.0f;
                }
                float angle = (float) Math.atan2(y, x);
                float l, r;
                if (angle > 0f && angle <= Math.PI/2f) {
                    l = 1.0f;
                    r = (float) (2.0f*angle/Math.PI);
                } else if (angle > Math.PI/2f && angle <= Math.PI) {
                    l = (float) (2.0f*(Math.PI-angle)/Math.PI);
                    r = 1.0f;
                } else if (angle < 0f && angle >= -Math.PI/2f) {
                    l = -1.0f;
                    r = (float) (2.0f*angle/Math.PI);
                } else if (angle < -Math.PI/2f && angle > -Math.PI) {
                    l = (float) (-2.0f*(angle+Math.PI)/Math.PI); 
                    r = -1.0f;
                } else {
                    l = r = 0f;
                }
                l *= power;
                r *= power;
                if (mReverse) {
                    l *= -1;
                    r *= -1;
                }
                if (!mReverseLR) {
                    mNXTTalker.motors((byte) (100*l), (byte) (100*r), mRegulateSpeed, mSynchronizeMotors);
                } else {
                    mNXTTalker.motors((byte) (100*r), (byte) (100*l), mRegulateSpeed, mSynchronizeMotors);
                }
            } else if ((action == MotionEvent.ACTION_UP) || (action == MotionEvent.ACTION_CANCEL)) {
                mNXTTalker.motors((byte) 0, (byte) 0, mRegulateSpeed, mSynchronizeMotors);
            }
            return true;
        }
    }
    
    private void updateMenu(int disabled) {
        if (mMenu != null) {
            mMenu.findItem(R.id.menuitem_buttons).setEnabled(disabled != R.id.menuitem_buttons).setVisible(disabled != R.id.menuitem_buttons);
            mMenu.findItem(R.id.menuitem_touchpad).setEnabled(disabled != R.id.menuitem_touchpad).setVisible(disabled != R.id.menuitem_touchpad);
            mMenu.findItem(R.id.menuitem_tank).setEnabled(disabled != R.id.menuitem_tank).setVisible(disabled != R.id.menuitem_tank);
            mMenu.findItem(R.id.menuitem_tank3motor).setEnabled(disabled != R.id.menuitem_tank3motor).setVisible(disabled != R.id.menuitem_tank3motor);
        }
    }
    
    private void setupUI() {
        if (mControlsMode == MODE_BUTTONS) {
            setContentView(R.layout.main);

            updateMenu(R.id.menuitem_buttons);
            
            ImageButton buttonUp = (ImageButton) findViewById(R.id.button_up);
            buttonUp.setOnTouchListener(new DirectionButtonOnTouchListener(1, 1));
            ImageButton buttonLeft = (ImageButton) findViewById(R.id.button_left);
            buttonLeft.setOnTouchListener(new DirectionButtonOnTouchListener(-0.6, 0.6));
            ImageButton buttonDown = (ImageButton) findViewById(R.id.button_down);
            buttonDown.setOnTouchListener(new DirectionButtonOnTouchListener(-1, -1));
            ImageButton buttonRight = (ImageButton) findViewById(R.id.button_right);
            buttonRight.setOnTouchListener(new DirectionButtonOnTouchListener(0.6, -0.6));

            SeekBar powerSeekBar = (SeekBar) findViewById(R.id.power_seekbar);
            powerSeekBar.setProgress(mPower);
            powerSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress,
                        boolean fromUser) {
                    mPower = progress;
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }            
            });
        } else if (mControlsMode == MODE_TOUCHPAD) {
            setContentView(R.layout.main_touchpad);

            updateMenu(R.id.menuitem_touchpad);
            
            mTouchPadView = (TouchPadView) findViewById(R.id.touchpad);
            mTouchPadView.setOnTouchListener(new TouchpadOnTouchListener());
        } else if (mControlsMode == MODE_TANK) {
            setContentView(R.layout.main_tank);
            
            updateMenu(R.id.menuitem_tank);
            
            mTankView = (TankView) findViewById(R.id.tank);
            
            mTankView.setOnTouchListener(new TankOnTouchListener());
        } else if (mControlsMode == MODE_TANK3MOTOR) {
            setContentView(R.layout.main_tank3motor);
            
            updateMenu(R.id.menuitem_tank3motor);
            
            mTank3MotorView = (Tank3MotorView) findViewById(R.id.tank3motor);
            
            mTank3MotorView.setOnTouchListener(new Tank3MotorOnTouchListener());
        }
        
        mStateDisplay = (TextView) findViewById(R.id.state_display);

        mConnectButton = (Button) findViewById(R.id.connect_button);
        mConnectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!NO_BT) {
                    findBrick();
                } else {
                    mState = NXTTalker.STATE_CONNECTED;
                    displayState();
                }
            }
        });
        
        mDisconnectButton = (Button) findViewById(R.id.disconnect_button);
        mDisconnectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mNXTTalker.stop();
            }
        });
        
/*
        CheckBox reverseCheckBox = (CheckBox) findViewById(R.id.reverse_checkbox);
        reverseCheckBox.setChecked(mReverse);
        reverseCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                mReverse = isChecked;
            }
        });
*/
        
        displayState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Log.i("NXT", "NXTRemoteControl.onStart()");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if ((ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED)
                    &&
                    (ContextCompat.checkSelfPermission(getApplicationContext(),
                            Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)) {
                startBluetooth();
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_SCAN)
                    || shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT)) {
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setMessage("In order to use Bluetooth, you have to grant this app the necessary permissions.")
                        .setPositiveButton("OK", (dialog, which) -> {
                            requestBluetoothPermissions();
                        }).show();
            } else {
                requestBluetoothPermissions();
            }
        } else {
            startBluetooth();
        }
    }

    void requestBluetoothPermissions() {
        requestPermissions(new String[]{
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
        }, REQUEST_PERMISSIONS_BLUETOOTH);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS_BLUETOOTH) {
            boolean allGranted = true;
            for (int granted : grantResults) {
                if (granted != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (grantResults.length > 0 && allGranted) {
                startBluetooth();
            } else {
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setMessage("You cannot use this app unless you grant it the necessary Bluetooth permissions. If you're seeing this message and the app didn't even request the permissions, you'll have to grant them in system settings, or uninstall and reinstall the app.")
                        .setPositiveButton("OK", (dialog, which) -> {
                            NXTRemoteControl.this.finish();
                        }).show();
            }
        }
    }

    void startBluetooth() {
        if (!NO_BT) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            } else {
                if (mSavedState == NXTTalker.STATE_CONNECTED) {
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
                    mNXTTalker.connect(device);
                } else {
                    if (mNewLaunch) {
                        mNewLaunch = false;
                        findBrick();
                    }
                }
            }
        }
    }

    private void findBrick() {
        Intent intent = new Intent(this, ChooseDeviceActivity.class);
        startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
    }
  
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_ENABLE_BT:
            if (resultCode == Activity.RESULT_OK) {
                findBrick();
            } else {
                Toast.makeText(this, "Bluetooth not enabled, exiting.", Toast.LENGTH_LONG).show();
                finish();
            }
            break;
        case REQUEST_CONNECT_DEVICE:
            if (resultCode == Activity.RESULT_OK) {
                String address = data.getExtras().getString(ChooseDeviceActivity.EXTRA_DEVICE_ADDRESS);
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                //Toast.makeText(this, address, Toast.LENGTH_LONG).show();
                mDeviceAddress = address;
                mNXTTalker.connect(device);
            }
            break;
        case REQUEST_SETTINGS:
            //XXX?
            break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Log.i("NXT", "NXTRemoteControl.onSaveInstanceState()");
        if (mState == NXTTalker.STATE_CONNECTED) {
            outState.putString("device_address", mDeviceAddress);
        }
        //outState.putBoolean("reverse", mReverse);
        outState.putInt("power", mPower);
        outState.putInt("controls_mode", mControlsMode);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //Log.i("NXT", "NXTRemoteControl.onConfigurationChanged()");
        setupUI();
    }
    
    private void displayState() {
        String stateText = null;
        int color = 0;
        switch (mState){ 
        case NXTTalker.STATE_NONE:
            stateText = "Not connected";
            color = 0xffff0000;
            mConnectButton.setVisibility(View.VISIBLE);
            mDisconnectButton.setVisibility(View.GONE);
            setProgressBarIndeterminateVisibility(false);
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
            break;
        case NXTTalker.STATE_CONNECTING:
            stateText = "Connecting...";
            color = 0xffffff00;
            mConnectButton.setVisibility(View.GONE);
            mDisconnectButton.setVisibility(View.GONE);
            setProgressBarIndeterminateVisibility(true);
            if (!mWakeLock.isHeld()) {
                mWakeLock.acquire();
            }
            break;
        case NXTTalker.STATE_CONNECTED:
            stateText = "Connected";
            color = 0xff00ff00;
            mConnectButton.setVisibility(View.GONE);
            mDisconnectButton.setVisibility(View.VISIBLE);
            setProgressBarIndeterminateVisibility(false);
            if (!mWakeLock.isHeld()) {
                mWakeLock.acquire();
            }
            break;
        }
        mStateDisplay.setText(stateText);
        mStateDisplay.setTextColor(color);
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_STATE_CHANGE:
                mState = msg.arg1;
                displayState();
                break;
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        //Log.i("NXT", "NXTRemoteControl.onStop()");
        mSavedState = mState;
        mNXTTalker.stop();
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menuitem_buttons) {
            mControlsMode = MODE_BUTTONS;
            setupUI();
        } else if (itemId == R.id.menuitem_touchpad) {
            mControlsMode = MODE_TOUCHPAD;
            setupUI();
        } else if (itemId == R.id.menuitem_tank) {
            mControlsMode = MODE_TANK;
            setupUI();
        } else if (itemId == R.id.menuitem_tank3motor) {
            mControlsMode = MODE_TANK3MOTOR;
            setupUI();
        } else if (itemId == R.id.menuitem_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivityForResult(i, REQUEST_SETTINGS);
        } else {
            return false;
        }
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        readPreferences(sharedPreferences, key);
    }
    
    private void readPreferences(SharedPreferences prefs, String key) {
        if (key == null) {
            mReverse = prefs.getBoolean("PREF_SWAP_FWDREV", false);
            mReverseLR = prefs.getBoolean("PREF_SWAP_LEFTRIGHT", false);
            mRegulateSpeed = prefs.getBoolean("PREF_REG_SPEED", false);
            mSynchronizeMotors = prefs.getBoolean("PREF_REG_SYNC", false);
            if (!mRegulateSpeed) {
                mSynchronizeMotors = false;
            }
        } else if (key.equals("PREF_SWAP_FWDREV")) {
            mReverse = prefs.getBoolean("PREF_SWAP_FWDREV", false);
        } else if (key.equals("PREF_SWAP_LEFTRIGHT")) {
            mReverseLR = prefs.getBoolean("PREF_SWAP_LEFTRIGHT", false);
        } else if (key.equals("PREF_REG_SPEED")) {
            mRegulateSpeed = prefs.getBoolean("PREF_REG_SPEED", false);
            if (!mRegulateSpeed) {
                mSynchronizeMotors = false;
            }
        } else if (key.equals("PREF_REG_SYNC")) {
            mSynchronizeMotors = prefs.getBoolean("PREF_REG_SYNC", false);
        }
    }
}
