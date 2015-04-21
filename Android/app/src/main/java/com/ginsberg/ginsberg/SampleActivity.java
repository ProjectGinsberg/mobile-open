package com.ginsberg.ginsberg;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.widget.RadioGroup;

import com.ginsberg.ginsberg.segmentcontrol.SegmentedGroup;


public class SampleActivity extends FragmentActivity implements RadioGroup.OnCheckedChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

            SegmentedGroup segmented4 = (SegmentedGroup) findViewById(R.id.sgInputType);
            segmented4.setTintColor(Color.parseColor("#40FFFFFF"), Color.parseColor("#FFFFFFFF"));

            segmented4.setOnCheckedChangeListener(this);

        }

        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId)
        {
            if(checkedId == R.id.rbKeyboard)
            {
                int here = 1;
            }
            else
            if(checkedId == R.id.rbHashtag)
            {
               int here = 1;
            }
        }
}
