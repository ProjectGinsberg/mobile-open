package com.ginsberg.ginsberg;


import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.ginsberg.api.GAPI;
import com.ginsberg.api.IGAPICallbacks;

import com.ginsberg.appshared.HLButton;
import com.ginsberg.ginsberg.SampleActivity;
import com.ginsberg.ginsberg.segmentcontrol.SegmentedGroup;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Christopher on 23/09/2014.
 */
public class AddEvents extends FragmentActivity implements IGAPICallbacks
{
    int  day = 0;
    int keyboardHeight = 0;
    boolean showingKeyboard = false;
    AddEvents Instance = this;


    //Resizing
    private Activity Instance2 = null;
    private void ApplyRescale()
    {
        Instance2 = this;
        final View view = (ViewGroup)findViewById(android.R.id.content);
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout() {
                if(Instance2 != null) {
                    if(GinsbergApp.ApplyScaling(Instance2, (ViewGroup) findViewById(android.R.id.content))) {
                        Instance2 = null;
                    }
                }
            }
        });
    }


    @Override
    public void onBackPressed()
    {
        pressedEventsCancel(null);
    }


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addevents);
        //keyboardWasShown();
        Instance = this;
        ApplyRescale();

        //Check for keyboard height
        final View view = (ViewGroup)findViewById(android.R.id.content);
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                // TODO Auto-generated method stub
                Rect r = new Rect();
                view.getWindowVisibleDisplayFrame(r);

                int screenHeight = view.getRootView().getHeight();
                int heightDifference = screenHeight - (r.bottom - r.top);
                Log.d("Keyboard Size", "Size: " + heightDifference);

                if(keyboardHeight < heightDifference)
                {
                    keyboardHeight = heightDifference;

                    //Resize effected components
                    RelativeLayout rl = (RelativeLayout)findViewById(R.id.rlEventsBottom);
                    // Gets linearlayout
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)rl.getLayoutParams();
                    // Changes the height and width to the specified *pixels*
                    params.height = keyboardHeight + 180;
                    //rl.setLayoutParams(params);
                    rl.requestLayout();
                }
            }
        });


        //Update fonts
        Typeface osl = Typeface.createFromAsset(getAssets(), "OpenSans-Light.ttf");
        Typeface osb = Typeface.createFromAsset(getAssets(), "OpenSans-Bold.ttf");
        Typeface ossb = Typeface.createFromAsset(getAssets(), "OpenSans-Semibold.ttf");

        //Questions
        ((EditText) findViewById(R.id.etEventEvent)).setTypeface(osl);

        ((TextView) findViewById(R.id.tvEventDate)).setTypeface(osb);
        //((Button) findViewById(R.id.btEventDiscard)).setTypeface(osb);

        ((Button) findViewById(R.id.btEventDone)).setTypeface(ossb);


        //Fix up segments
        SegmentedGroup segInputType = (SegmentedGroup) findViewById(R.id.sgInputType);
        segInputType.setTintColor(Color.parseColor("#40FFFFFF"), Color.parseColor("#FFFFFFFF"));
        segInputType.check(R.id.rbKeyboard);
        ((RadioButton) findViewById(R.id.rbKeyboard)).setTypeface(osb);
        ((RadioButton) findViewById(R.id.rbHashtag)).setTypeface(osb);

        SegmentedGroup segTagType = (SegmentedGroup) findViewById(R.id.sgTagType);
        segTagType.setTintColor(Color.parseColor("#40FFFFFF"), Color.parseColor("#FFFFFFFF"));
        segTagType.check(R.id.rbRecent);
        ((RadioButton) findViewById(R.id.rbRecent)).setTypeface(osb);
        ((RadioButton) findViewById(R.id.rbUsed)).setTypeface(osb);
        ((RadioButton) findViewById(R.id.rbEmotions)).setTypeface(osb);
        ((RadioButton) findViewById(R.id.rbScots)).setTypeface(osb);

        viewDidLoad();
    }

    //View Controller
    private void viewDidLoad()
    {
        GAPI.Instance().SetCallbacks(this,this);

        viewWillAppear();
    }


    private void viewWillAppear()
    {
        ((TextView)findViewById(R.id.tvEventDate)).setText("Today\n"+GAPI.GetDate());
        EditText et = (EditText)findViewById(R.id.etEventEvent);
        et.setText(GAPI.Instance().todaysEvent);

        TextView date = (TextView) findViewById(R.id.tvEventDate);
        date.setText(GAPI.GetDate());

        Analytics.Instance().LogScreen("Events Screen");
        Analytics.Instance().LogEventParams("Screen", "Change", "Events Screen");

        //Setup scene
        ShowDay();

        SetupTags(0);

        ShowDay();
    }


    private void SetupTags(int index)
    {
        //Find list
        List<String> usedTags = null;

        switch(index)
        {
            case 0: usedTags = GAPI.Instance().userTags; break;
            case 1: usedTags = GAPI.Instance().userTagsOrdered; break;
            case 2: usedTags = GAPI.Instance().tagsEmotions; break;
            case 3: usedTags = GAPI.Instance().tagsScots; break;
            default: usedTags = GAPI.Instance().userTags; break;
        }

        //Setup tags
        ScrollView sv = (ScrollView)findViewById(R.id.svEventTags);
        LinearLayout ll = (LinearLayout)findViewById(R.id.llTags);
        ll.removeAllViews();
        Typeface osl = Typeface.createFromAsset(getAssets(), "OpenSans-Light.ttf");
        LinearLayout lh = null;

        float start = 10.0f;
        for(int j = 0; j < 10; ++j)
        for(int i = 0; i < usedTags.size(); ++i)
        {
            if(i%3 == 0 || lh == null)
            {
                lh = new LinearLayout(this);
                lh.setBackgroundColor(Color.TRANSPARENT);
                lh.setOrientation(LinearLayout.HORIZONTAL);

                LinearLayout.LayoutParams LLParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                                                                   LinearLayout.LayoutParams.WRAP_CONTENT);
                lh.setLayoutParams(LLParams);
                ll.addView(lh);
            }

            String tag = usedTags.get(i);
            Button bt = new Button(this);
            bt.setText(tag);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    (int) getResources().getDimension(R.dimen.tag_button_height));
            params.setMargins(5,0,5,5);
            bt.setLayoutParams(params);
            bt.setTextColor(0xffffffff);
            bt.setTypeface(osl);
            bt.setBackgroundResource(R.drawable.bttag);

            bt.setOnTouchListener(new View.OnTouchListener()
            {
                @Override
                public boolean onTouch(View v, MotionEvent event)
                {
                    if (event.getAction() == MotionEvent.ACTION_UP )
                    {
                        pressedTag(v);
                        return true;
                    }

                    return false;
                }
            });

            lh.addView(bt);
        }
    }


    // Called when the UIKeyboardDidShowNotification is sent.
    private void keyboardWasShown()//aNotification:NSNotification)
    {
        if(true)//!showingKeyboard)
        {
            showingKeyboard = true;
            GinsbergApp.showKeyboard(Instance,findViewById(R.id.etEventEvent));
        }
    }


    // Called when the UIKeyboardWillHideNotification is sent
    private void keyboardWillBeHidden()
    {
        if(true)//showingKeyboard)
        {
            showingKeyboard = false;
            GinsbergApp.hideKeyboard(Instance);
        }
    }


    //Methods
    private void ShowDay()
    {
    }


    //Actions
    public void pressedEvents(View sender)
    {
        if(!showingKeyboard)
        {
            keyboardWasShown();
        }
    }


    public void pressedTag(View sender)
    {
        EditText et = (EditText)findViewById(R.id.etEventEvent);
        Button bt = (Button)sender;

        String title = bt.getText().toString();
        String content = et.getText().toString();
        boolean addSpace = !content.endsWith(" ") && content.length() > 0 ;
        content += (addSpace? " ":"") +  "#" + title + " ";
        et.setText(content);
    }


    public void pressedEventsDone(View sender)
    {
        EditText et = (EditText)findViewById(R.id.etEventEvent);
        String result = et.getText().toString().trim();

        if(et.getTag() != result)
        {

            ArrayList<String> newTags = new ArrayList<String>();

            //Extract tags
            String[] splits = result.split(" ");

            for(int i = 0; i < splits.length; ++i)
            {
                String word = splits[i].trim().toLowerCase();

                if(word.startsWith("#"))
                {
                    String clean = word.substring(1);

                    //Check to see if in current tags
                    boolean match = false;
                    for(int j = 0; !match && j < GAPI.Instance().userTags.size(); ++i)
                    {
                        String tag = GAPI.Instance().userTags.get(i);
                        if(tag.equals(clean))
                        {
                            match = true;
                        }
                    }

                    if(!match)
                    {
                        GAPI.Instance().userTags.add(clean);
                        newTags.add(clean);
                    }
                }
            }

            //Send event
            if(GAPI.Instance().userID != null)
            {
                Analytics.Instance().LogID(GAPI.Instance().userID);
                Analytics.Instance().LogProfileValue("Notification", Login.notification);
            }
            GAPI.Instance().PostEvents(result, GAPI.GetDateTime(0), GAPI.Instance().todaysEventID);

            dismissView();
        }
    }


    public void pressedEventsCancel(View sender)
    {
        if(showingKeyboard)
        {
            keyboardWillBeHidden();
        }
        else
        {
            dismissView();
        }
    }


    public void pressedShowKeyboard(View sender)
    {
        keyboardWasShown();
    }


    public void pressedHideKeyboard(View sender)
    {
        keyboardWillBeHidden();
    }


    public void pressedTagsRecent(View sender)
    {
        SetupTags(0);
    }


    public void pressedTagsMostUsed(View sender)
    {
        SetupTags(1);
    }


    public void pressedTagsEmotions(View sender)
    {
        SetupTags(2);
    }


    public void pressedTagsScots(View sender)
    {
        SetupTags(3);
    }


    private void dismissView()
    {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run()
            {
                // Create an intent that will start the main activity.
                Intent mainIntent = new Intent(AddEvents.this, GinsbergApp.DIRECT? AddSubjective.class: Main.class);
                AddEvents.this.startActivity(mainIntent);

                // Finish splash activity so user cant go back to it.
                AddEvents.this.finish();

                // Apply our splash exit (fade out) and main
                //   entry (fade in) animation transitions.
                overridePendingTransition(R.anim.fadein, R.anim.fadeout);
            }
        }, 0);
    }


    //
    // Callbacks
    //

    public void Comment(String text) {}
    public void NeedLogin() {}
    public void GainedAccess() {}
    public void DataReceived(String endPoint, JSONArray data) {}
    public void SetBusy(boolean truth) {}
    public void CommentError(String text) {}
    public void CommentResult(String text) {}
    public void CommentSystem(String text) {}
}
