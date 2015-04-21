package com.ginsberg.appshared;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageButton;

public class HLImageButton extends ImageButton
{
    public HLImageButton(Context context) {
        super(context);
        // TODO Auto-generated constructor stub

    }

    public HLImageButton(Context context, AttributeSet attrs){
        super(context,attrs);
    }

    public HLImageButton(Context context, AttributeSet attrs, int defStyle){
        super(context,attrs,defStyle);
    }

    public void setImageResource (int resId){
        super.setImageResource(resId);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e){
        if(e.getAction() == MotionEvent.ACTION_DOWN){
            this.setColorFilter(Color.argb(155, 155, 155, 155));
        }else if(e.getAction() == MotionEvent.ACTION_UP){
            this.setColorFilter(Color.argb(0, 185, 185, 185));
        }

        return super.onTouchEvent(e);
    }
}