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
import android.util.AttributeSet;
import android.view.View;

public class TankView extends View {
    
    public int mWidth;
    public int mHeight;
    public float mZero;
    public float mRange;
    
    public TankView(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    public TankView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    public TankView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
    }
    
    protected float barWidth(){
        return 0.25f;
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRGB(0, 0, 0);
        Paint paint = new Paint();
        paint.setColor(0xff00ff00);
        paint.setStyle(Paint.Style.STROKE);
        
        float x0 = 0f;
        float x1 = mWidth*barWidth();
        float x2 = mWidth*(1-barWidth());
        float x3 = mWidth-1;
        canvas.drawRect(x0, mZero-mRange, x1, mZero+mRange, paint);
        canvas.drawRect(x2, mZero-mRange, x3, mZero+mRange, paint);
        for (int i = 0; i < 4; i++) {
            canvas.drawLine(x0, mZero+i/4f*mRange, x1, mZero+i/4f*mRange, paint);
            canvas.drawLine(x2, mZero+i/4f*mRange, x3, mZero+i/4f*mRange, paint);
            canvas.drawLine(x0, mZero-i/4f*mRange, x1, mZero-i/4f*mRange, paint);
            canvas.drawLine(x2, mZero-i/4f*mRange, x3, mZero-i/4f*mRange, paint);
        }
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        mWidth = w;
        mHeight = h;
        mZero = mHeight/2f;
        mRange = 0.85f*mHeight/2f;
    }
}
