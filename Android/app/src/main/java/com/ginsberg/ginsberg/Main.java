package com.ginsberg.ginsberg;


import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.ginsberg.api.GAPI;
import com.ginsberg.api.IGAPICallbacks;

import org.json.JSONArray;


/**
 * Created by Christopher on 23/09/2014.
 */
public class Main extends FragmentActivity implements IGAPICallbacks
{
    private float dateAlpha = 1.0f;
    private FrameLayout flDate;

    // This is a handle so that we can call methods on our service
    private ScheduleClient scheduleClient;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        TextView tv = (TextView)findViewById(R.id.tvMainDate);
        tv.setText(GAPI.GetDate());

        // Create a new service client and bind our activity to this service
        scheduleClient = new ScheduleClient(this);
        scheduleClient.doBindService();

        flDate = (FrameLayout) findViewById(R.id.flDateM);

        //Update fonts
        Typeface osl = Typeface.createFromAsset(getAssets(), "OpenSans-Light.ttf");
        Typeface osb = Typeface.createFromAsset(getAssets(), "OpenSans-Bold.ttf");

        //Date screen
        ((CheckBox) findViewById(R.id.cbNotifyM)).setTypeface(osl);
        ((Button) findViewById(R.id.btDoneM)).setTypeface(osb);

        //ViewDidLoad
        GAPI.Instance().SetCallbacks(this, this);
        Analytics.Instance().Setup(this);

        //Cut down for simple
        if (GinsbergApp.SIMPLE)
        {
            findViewById(R.id.btMenuM).setVisibility(View.GONE);
        }
        else
        {
            //Hide done button
            Login.LoadValues(this, R.id.cbNotifyM, R.id.tpNotifyM);
        }

        ShowSettings(false,true);
    }


    @Override
    public void onBackPressed()
    {
        if(dateAlpha == 1.0f)
        {
            ShowSettings(false, false);
        }
        else
        {
            if(dateAlpha == 0.0f)
            {
                super.onBackPressed();
            }
        }
    }


    //Localytics common callbacks
    @Override
    public void onResume()
    {
        super.onResume();
        Analytics.Instance().onResume();
    }

    @Override
    public void onPause()
    {
        Analytics.Instance().onPause();
        super.onPause();
    }

    //Alarm callback
    @Override
    protected void onStop()
    {
        // When our activity is stopped ensure we also stop the connection to the service
        // this stops us leaking our activity into the system *bad*
        if (scheduleClient != null)
            scheduleClient.doUnbindService();

        super.onStop();
    }


    //
    // Actions
    //
    public void pressedEvents(View v)
    {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run()
            {
                // Create an intent that will start the main activity.
                Intent mainIntent = new Intent(Main.this, AddEvents.class);
                Main.this.startActivity(mainIntent);

                // Finish splash activity so user cant go back to it.
                Main.this.finish();

                // Apply our splash exit (fade out) and main
                //   entry (fade in) animation transitions.
                overridePendingTransition(R.anim.fadein, R.anim.fadeout);
            }
        }, 0);
    }


    public void pressedSubjective(View v)
    {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run()
            {
                // Create an intent that will start the main activity.
                Intent mainIntent = new Intent(Main.this, AddSubjective.class);
                Main.this.startActivity(mainIntent);

                // Finish splash activity so user cant go back to it.
                Main.this.finish();

                // Apply our splash exit (fade out) and main
                //   entry (fade in) animation transitions.
                overridePendingTransition(R.anim.fadein, R.anim.fadeout);
            }
        }, 0);
    }


    public void pressedSettingsCloseM(View sender)
    {
        Login.SaveValues(this, R.id.cbNotifyM,R.id.tpNotifyM);
        ShowSettings(false, false);
    }


    public void pressedSettingsOpenM(View sender)
    {
        ShowSettings(true, false);
    }


    public void Comment(String text)
    {

    }


    /**
      * @brief      Callback for when app has access
      * @details    After sdk setup, and the user has accepted access, this method will be called
      */
    public void NeedLogin()
    {

    }


    /**
      * @brief      Callback for when app has access
      * @details    After sdk setup, and the user has accepted access, this method will be called
      */
    public void GainedAccess()
    {

    }


    /**
      * @brief      Callback for when app receives data from the server
      * @details    When ever the app requests data, this will be where valid returned data will be sent
      */
    public void DataReceived(String endPoint, JSONArray data)
    {

    }


    /**
      * @brief      Callback for when the sdk is busy doing something
      * @details   
      */
    public void SetBusy(boolean truth)
    {

    }


    /**
      * @brief      Callback for error messages from sdk
      * @details    When the system has messages to pass back to system, they will be sent here for either ignoring or displaying to user.
      */
    public void CommentError(String text)
    {

    }


    /**
      * @brief      Callback for result messages from sdk
      * @details    When the system has messages to pass back to system, they will be sent here for either ignoring or displaying to user.
      */
    public void CommentResult(String text)
    {

    }


    /**
      * @brief      Callback for system messages from sdk
      * @details    When the system has messages to pass back to system, they will be sent here for either ignoring or displaying to user.
      */
    public void CommentSystem(String text)
    {

    }


    //
    // Logic
    //

    void ShowSettings(final boolean truth, final boolean skipDateAnim)
    {
        final FrameLayout frame = (FrameLayout) findViewById(R.id.flDateM);
        frame.setEnabled(truth);
        final float alpha = dateAlpha;

        //Anim
        if ((alpha == (truth ? 0.0f : 1.0f)) || skipDateAnim) {
            AlphaAnimation anim = new AlphaAnimation(dateAlpha, truth ? 1.0f : 0.0f);
            anim.setDuration(skipDateAnim ? 0 : 300);
            anim.setFillAfter(true);

            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    if (truth) {
                        frame.setVisibility(View.VISIBLE);
                        for (int i = 0; i < frame.getChildCount(); i++) {
                            View view = frame.getChildAt(i);
                            view.setVisibility(View.VISIBLE); // Or whatever you want to do with the view.
                        }
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (!truth) {
                        for (int i = 0; i < frame.getChildCount(); i++) {
                            View view = frame.getChildAt(i);
                            view.setVisibility(View.GONE); // Or whatever you want to do with the view.
                        }
                        frame.setVisibility(View.GONE);
                    }
                }
            });

            frame.startAnimation(anim);
            dateAlpha = truth ? 1.0f : 0.0f;
        }
    }
}

