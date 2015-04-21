package com.ginsberg.appshared;
import android.content.Context;
import android.util.AttributeSet;

public class TabControlButton extends SegmentedControlButton {

    public TabControlButton(Context context, AttributeSet attrs) {
        super(context, attrs, R.attr.tabControlButtonStyle);
    }
}
