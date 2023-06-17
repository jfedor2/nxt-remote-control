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

package org.jfedor.nxtremotecontrol;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class TouchPadView extends View {

    public TouchPadView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
    }
    
    public TouchPadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    public TouchPadView(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    private int mWidth;
    private int mHeight;
    public float mCx, mCy;
    public float mRadius;
    public float mOffset;
    
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRGB(0, 0, 0);
        Paint paint = new Paint();
        paint.setColor(0xff00ff00);
        paint.setStyle(Paint.Style.STROKE);
        //canvas.drawLine(0, 0, mWidth, mHeight, paint);
        for (int i = 1; i<=6; i++) {
            //canvas.drawCircle(mCx, mCy, mRadius*i/6.0f, paint);
            canvas.drawArc(new RectF(mCx-mRadius*i/6.0f, mCy-mOffset-mRadius*i/6.0f, mCx+mRadius*i/6.0f, mCy-mOffset+mRadius*i/6.0f), 180f, 180f, false, paint);
            canvas.drawArc(new RectF(mCx-mRadius*i/6.0f, mCy+mOffset-mRadius*i/6.0f, mCx+mRadius*i/6.0f, mCy+mOffset+mRadius*i/6.0f), 0f, 180f, false, paint);
        }
        canvas.drawLine(mCx+0.16666f*mRadius, mCy-mOffset, mCx+mRadius, mCy-mOffset, paint);
        canvas.drawLine(mCx-0.16666f*mRadius, mCy-mOffset, mCx-mRadius, mCy-mOffset, paint);
        canvas.drawLine(mCx+0.16666f*mRadius, mCy+mOffset, mCx+mRadius, mCy+mOffset, paint);
        canvas.drawLine(mCx-0.16666f*mRadius, mCy+mOffset, mCx-mRadius, mCy+mOffset, paint);
        canvas.drawLine(mCx, mCy+mOffset+0.16666f*mRadius, mCx, mCy+mOffset+mRadius, paint);
        canvas.drawLine(mCx, mCy-mOffset-0.16666f*mRadius, mCx, mCy-mOffset-mRadius, paint);
        canvas.drawLine(mCx+0.16666f*mRadius*0.70710f, mCy+mOffset+0.16666f*mRadius*0.70710f, mCx+mRadius*0.70710f, mCy+mOffset+mRadius*0.70710f, paint);
        canvas.drawLine(mCx-0.16666f*mRadius*0.70710f, mCy+mOffset+0.16666f*mRadius*0.70710f, mCx-mRadius*0.70710f, mCy+mOffset+mRadius*0.70710f, paint);
        canvas.drawLine(mCx+0.16666f*mRadius*0.70710f, mCy-mOffset-0.16666f*mRadius*0.70710f, mCx+mRadius*0.70710f, mCy-mOffset-mRadius*0.70710f, paint);
        canvas.drawLine(mCx-0.16666f*mRadius*0.70710f, mCy-mOffset-0.16666f*mRadius*0.70710f, mCx-mRadius*0.70710f, mCy-mOffset-mRadius*0.70710f, paint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        mWidth = w;
        mHeight = h;
        mCx = mWidth/2;
        mCy = mHeight/2;
        if (mHeight >= 1.2f*mWidth) {
            mRadius = 0.9f*mWidth*0.5f;
        } else {
            mRadius = 0.9f*mHeight*5f/12f;
        }
        mOffset = mRadius*0.2f;
        //Log.i("NXT", "w,h,r,o="+ Integer.toString(w) + "," + Integer.toString(h) + "," + Float.toString(mRadius) + "," + Float.toString(mOffset));
    }

/*
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (width > height) {
            width = height;
        } else {
            height = width;
        }

        super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        );
    }
*/
}
