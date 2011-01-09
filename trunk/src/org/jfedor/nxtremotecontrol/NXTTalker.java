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
 * http://developer.android.com/resources/samples/BluetoothChat/src/com/example/android/BluetoothChat/BluetoothChatService.html
 * 
 * Copyright (c) 2009 The Android Open Source Project
 */

package org.jfedor.nxtremotecontrol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class NXTTalker {

    public static final int STATE_NONE = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    
    private int mState;
    private Handler mHandler;
    private BluetoothAdapter mAdapter;
    
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    
    public NXTTalker(Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
        setState(STATE_NONE);
    }

    private synchronized void setState(int state) {
        mState = state;
        if (mHandler != null) {
            mHandler.obtainMessage(NXTRemoteControl.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
        } else {
            //XXX
        }
    }
    
    public synchronized int getState() {
        return mState;
    }
    
    public synchronized void setHandler(Handler handler) {
        mHandler = handler;
    }
    
    private void toast(String text) {
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage(NXTRemoteControl.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(NXTRemoteControl.TOAST, text);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        } else {
            //XXX
        }
    }

    public synchronized void connect(BluetoothDevice device) {
        //Log.i("NXT", "NXTTalker.connect()");
        
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }
        
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }
    
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        
        //toast("Connected to " + device.getName());
        
        setState(STATE_CONNECTED);
    }
    
    public synchronized void stop() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        setState(STATE_NONE);
    }
    
    private void connectionFailed() {
        setState(STATE_NONE);
        //toast("Connection failed");
    }
    
    private void connectionLost() {
        setState(STATE_NONE);
        //toast("Connection lost");
    }
    
    public void motors(byte l, byte r, boolean speedReg, boolean motorSync) {
        byte[] data = { 0x0c, 0x00, (byte) 0x80, 0x04, 0x02, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                        0x0c, 0x00, (byte) 0x80, 0x04, 0x01, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00 };
        
        //Log.i("NXT", "motors: " + Byte.toString(l) + ", " + Byte.toString(r));
        
        data[5] = l;
        data[19] = r;
        if (speedReg) {
            data[7] |= 0x01;
            data[21] |= 0x01;
        }
        if (motorSync) {
            data[7] |= 0x02;
            data[21] |= 0x02;
        }
        write(data);
    }
    
    public void motor(int motor, byte power, boolean speedReg, boolean motorSync) {
        byte[] data = { 0x0c, 0x00, (byte) 0x80, 0x04, 0x02, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00 };
        
        //Log.i("NXT", "motor: " + Integer.toString(motor) + ", " + Byte.toString(power));
        
        if (motor == 0) {
            data[4] = 0x02;
        } else {
            data[4] = 0x01;
        }
        data[5] = power;
        if (speedReg) {
            data[7] |= 0x01;
        }
        if (motorSync) {
            data[7] |= 0x02;
        }
        write(data);
    }
    
    public void motors3(byte l, byte r, byte action, boolean speedReg, boolean motorSync) {
        byte[] data = { 0x0c, 0x00, (byte) 0x80, 0x04, 0x02, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                        0x0c, 0x00, (byte) 0x80, 0x04, 0x01, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                        0x0c, 0x00, (byte) 0x80, 0x04, 0x00, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00 };
        
        //Log.i("NXT", "motors3: " + Byte.toString(l) + ", " + Byte.toString(r) + ", " + Byte.toString(action));
        
        data[5] = l;
        data[19] = r;
        data[33] = action;
        if (speedReg) {
            data[7] |= 0x01;
            data[21] |= 0x01;
        }
        if (motorSync) {
            data[7] |= 0x02;
            data[21] |= 0x02;
        }
        write(data);
    }
    
    private void write(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) {
                return;
            }
            r = mConnectedThread;
        }
        r.write(out);
    }
    
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        
        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            
            try {
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmSocket = tmp;
        }
        
        public void run() {
            setName("ConnectThread");
            mAdapter.cancelDiscovery();
            
            try {
                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed();
                try {
                    mmSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                return;
            }
            
            synchronized (NXTTalker.this) {
                mConnectThread = null;
            }
            
            connected(mmSocket, mmDevice);
        }
        
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    //toast(Integer.toString(bytes) + " bytes read from device");
                } catch (IOException e) {
                    e.printStackTrace();
                    connectionLost();
                    break;
                }
            }
        }
        
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
                // XXX?
            }
        }
        
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
