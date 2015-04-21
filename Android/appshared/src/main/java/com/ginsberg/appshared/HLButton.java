package com.ginsberg.appshared;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Button;


public class HLButton extends Button
{
    public HLButton(Context context) {
        super(context);
        // TODO Auto-generated constructor stub

    }

    public HLButton(Context context, AttributeSet attrs){
        super(context,attrs);
    }

    public HLButton(Context context, AttributeSet attrs, int defStyle){
        super(context,attrs,defStyle);
    }


    @Override
    public boolean onTouchEvent(MotionEvent e){
        if(e.getAction() == MotionEvent.ACTION_DOWN){
            this.getBackground().setColorFilter(Color.parseColor("#B7B2B0"), PorterDuff.Mode.MULTIPLY);//Color.argb(155, 155, 155, 155));
        }else if(e.getAction() == MotionEvent.ACTION_UP){
            this.getBackground().setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.MULTIPLY);//Color.argb(0, 185, 185, 185));
        }

        return super.onTouchEvent(e);
    }
}