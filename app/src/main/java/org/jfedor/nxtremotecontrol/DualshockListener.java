package org.jfedor.nxtremotecontrol;

import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class DualshockListener {
    private static final int FORWARD_DPAD = 19;
    private static final int BACKWARD_DPAD = 20;
    private static final int LEFT_DPAD = 21;
    private static final int RIGHT_DPAD = 22;

    private int LEFT_MOTOR_SPEED = 0;
    private int RIGHT_MOTOR_SPEED = 0;
    private int TURN_MOTOR_SPEED = 0;

    NXTTalker mNXTTalker;

    public void handleEvent(MotionEvent event){
//        Log.d("event", event.toString());
        //
//        LEFT STICK
//        int x = 10 * Math.round (event.getX() * 10);
//        int y =  - Math.round (100f * event.getY());

        int x = 50 * Math.round (event.getAxisValue(MotionEvent.AXIS_Z) * 10);
        int y =  - Math.round (100f * event.getY());

        this.TURN_MOTOR_SPEED = x;
        // stop main motor if speed is low during
        if(y < 15 && y > -15){
            y = 0;
        }

        // FORWARD
        this.LEFT_MOTOR_SPEED = y;
        this.RIGHT_MOTOR_SPEED = y;

//        Log.d("Left Stick Y", event.getY() + "");
//        Log.d("Left Stick X", event.getX() + "");
        Log.d("Right Stick Y", event.getAxisValue(MotionEvent.AXIS_RZ) + "");
        Log.d("Right Stick X", event.getAxisValue(MotionEvent.AXIS_Z) + "");
      //  moveRobot();
    }

    public void setmNXTTalker(NXTTalker nxtTalker){
        this.mNXTTalker = nxtTalker;
    }

    public void onKeyDown(int keyCode, KeyEvent event) {
//        if(FORWARD_DPAD == keyCode){
//           LEFT_MOTOR_SPEED = 100;
//           RIGHT_MOTOR_SPEED= 100;
//        }
//        else  if(BACKWARD_DPAD == keyCode){
//            LEFT_MOTOR_SPEED = -100;
//            RIGHT_MOTOR_SPEED= -100;
//        }
//        else  if(LEFT_DPAD == keyCode){
//            TURN_MOTOR_SPEED = -50;
//        }
//        else  if(RIGHT_DPAD == keyCode){
//            TURN_MOTOR_SPEED = 50;
//        }
        moveRobot();
    }

    public void onKeyUp(int keyCode, KeyEvent event) {
        if(FORWARD_DPAD == keyCode){
            LEFT_MOTOR_SPEED = 0;
            RIGHT_MOTOR_SPEED= 0;
        }
        else  if(BACKWARD_DPAD == keyCode){
            LEFT_MOTOR_SPEED = 0;
            RIGHT_MOTOR_SPEED= 0;
        }
        else  if(LEFT_DPAD == keyCode){
            TURN_MOTOR_SPEED = 0;
        }
        else  if(RIGHT_DPAD == keyCode){
            TURN_MOTOR_SPEED = 0;
        }
        moveRobot();
    }

    private void moveRobot(){
        mNXTTalker.motors3((byte)-LEFT_MOTOR_SPEED, (byte)-RIGHT_MOTOR_SPEED, (byte) TURN_MOTOR_SPEED, false, false);
    }


}
