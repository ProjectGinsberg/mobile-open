package com.ginsberg.ginsberg;

import com.ginsberg.api.GAPI;
import com.ginsberg.api.IGAPICallbacks;

import com.crashlytics.android.Crashlytics;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;

import org.json.JSONArray;

import java.util.Calendar;


public class Login extends FragmentActivity implements IGAPICallbacks
{
    static String CLIENT_ID = "";
    static String CLIENT_SECRET = "";
    private int results = 0;
    private ProgressBar pbActivity;

    // This is a handle so that we can call methods on our service
    private static ScheduleClient scheduleClient;

    //Notificaiton stuff
    static final String NotifyStoreKey = "token";
    static final String HourStoreKey = "nothour";
    static final String MinStoreKey = "notmin";

    public static int hour = 0;
    public static int min = 0;
    public static boolean notification = true;


    //Resizing
    private Activity Instance = null;
    private void ApplyRescale()
    {
        Instance = this;
        final View view = (ViewGroup)findViewById(android.R.id.content);
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout() {
                if(Instance != null) {
                    if(GinsbergApp.ApplyScaling(Instance, (ViewGroup) findViewById(android.R.id.content))) {
                        Instance = null;
                    }
                }
            }
        });
    }


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        CLIENT_ID = getString(R.string.client_id_example);
        CLIENT_SECRET = getString(R.string.client_secret_example);

        Crashlytics.start(this);
        setContentView(R.layout.login);

        pbActivity = (ProgressBar)findViewById(R.id.pbActivity);
        pbActivity.setVisibility(View.VISIBLE);
        findViewById(R.id.greyoverlay).setVisibility(View.VISIBLE);
        findViewById(R.id.btLogin).setVisibility(View.GONE);
        findViewById(R.id.lbLogin).setVisibility(View.GONE);

        //Update fonts
        Typeface osl = Typeface.createFromAsset(getAssets(), "OpenSans-Light.ttf");
        Typeface osb = Typeface.createFromAsset(getAssets(), "OpenSans-Bold.ttf");
        ((TextView)findViewById(R.id.lbLogin)).setTypeface(osl);
        ((TextView)findViewById(R.id.btLogin)).setTypeface(osb);

        Analytics.Instance().Setup(this);

        // Create a new service client and bind our activity to this service
        scheduleClient = new ScheduleClient(this);
        scheduleClient.doBindService();
        CheckInitialNotification();

        viewWillAppear();
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
    @Override
    public void	onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
    }


    public void viewWillAppear()
    {
        Analytics.Instance().LogScreen("Login Screen");
        Analytics.Instance().LogEventParams("Screen","Change","Login Screen");

        GAPI.Instance().Setup(this, CLIENT_ID, CLIENT_SECRET, this);
    }


    private void MoveOn()
    {
        if(GAPI.Instance().userID != null)
        {
            Analytics.Instance().LogID(GAPI.Instance().userID);
            Analytics.Instance().LogProfileValue("Notification", Login.notification);
        }

        // Create a new handler with which to start the main activity
        //   and close this splash activity after SPLASH_DISPLAY_TIME has
        //   elapsed.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run()
            {
                // Create an intent that will start the main activity.
                Intent mainIntent = new Intent(Login.this, GinsbergApp.SIMPLE || GinsbergApp.DIRECT? AddSubjective.class: Main.class);
                Login.this.startActivity(mainIntent);

                // Finish splash activity so user cant go back to it.
                Login.this.finish();

                // Apply our splash exit (fade out) and main
                //   entry (fade in) animation transitions.
                overridePendingTransition(R.anim.fadein, R.anim.fadeout);
            }
        }, 0);
    }


    //Actions
    public void pressedLogin(View v)
    {
        GAPI.Instance().Login();
    }


    //
    // Notifications
    //

    private void CheckInitialNotification()
    {
        SharedPreferences prefs = this.getSharedPreferences("GINSBERG", Application.MODE_PRIVATE);
        if(!prefs.contains(NotifyStoreKey))
        {
            SaveValues(this,0,0);
        }
    }


    //Alarm callback
    public static void UnbindNotification()
    {
        // When our activity is stopped ensure we also stop the connection to the service
        // this stops us leaking our activity into the system *bad*
        if (scheduleClient != null)
            scheduleClient.doUnbindService();
    }


    public static void UpdateNotification()
    {
        // Ask our service to set an alarm for that date, this activity talks to the client that talks to the service
        if(notification)
        {
            Calendar c = (Calendar)Calendar.getInstance().clone();
            c.add(Calendar.DATE, GinsbergApp.TEST? 0: 1);
            c.set(Calendar.HOUR_OF_DAY, hour);
            c.set(Calendar.MINUTE, min);
            c.set(Calendar.SECOND, 0);

            scheduleClient.setAlarmForNotification(c);
        }
        else
        {
            scheduleClient.clearAlarmForNotification();
        }
    }


    public static void LoadValues(Activity activity, int cbID, int tpID)
    {
        //Load values
        SharedPreferences prefs = activity.getSharedPreferences("GINSBERG", Application.MODE_PRIVATE);
        notification = prefs.getBoolean(NotifyStoreKey, true);
        hour = prefs.getInt(HourStoreKey, 18);
        min = prefs.getInt(MinStoreKey, 0);

        CheckBox cb = (CheckBox)activity.findViewById(cbID);//R.id.cbNotify);
        if(cb != null)
        {
            cb.setChecked(notification);
        }

        TimePicker tp = (TimePicker)activity.findViewById(tpID);//R.id.tpNotify);
        if(tp != null)
        {
            tp.setCurrentHour(hour);
            tp.setCurrentMinute(min);
        }

        //Use values
        UpdateNotification();
    }


    public static void SaveValues(Activity activity, int cbID, int tpID)
    {
        TimePicker tp = (TimePicker)activity.findViewById(tpID);
        CheckBox cb = (CheckBox)activity.findViewById(cbID);

        int newHour = tp == null? 18: tp.getCurrentHour();
        int newMin = tp == null? 0: tp.getCurrentMinute();
        boolean show = cb == null? true: cb.isChecked();

        if (notification != show || hour != newHour || min != newMin)
        {
            String period = "21-24";

            if (hour < 3)
            {
                period = "00-03";
            } else if (hour < 6)
            {
                period = "03-06";
            } else if (hour < 9)
            {
                period = "06-09";
            } else if (hour < 12)
            {
                period = "09-12";
            } else if (hour < 15)
            {
                period = "12-15";
            } else if (hour < 18)
            {
                period = "15-18";
            } else if (hour < 21)
            {
                period = "18-21";
            }

            notification = show;
            if (show)
            {
                Analytics.Instance().LogEventParams("Notification Switch", "On", "true");
                Analytics.Instance().LogEventParams("Notification Setup", "Period", period, "Hour", String.format("%02d", newHour), "Minute", String.format("%02d", newMin));
            } else
            {
                Analytics.Instance().LogEventParams("Notification Switch", "On", "false");
                Analytics.Instance().LogEventParams("Notification Disabled", "Period", period, "Hour", String.format("%02d", newHour), "Minute", String.format("%02d", newMin));
            }
        }

        hour = newHour;
        min = newMin;
        notification = show;

        //Save values
        SharedPreferences prefs = activity.getSharedPreferences("GINSBERG", Application.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(NotifyStoreKey, notification);
        editor.putInt(HourStoreKey, hour);
        editor.putInt(MinStoreKey, min);
        editor.commit();

        //Use values
        UpdateNotification();
    }


    //GAPI Callbacks
    public void NeedLogin()
    {
        pbActivity.setVisibility(View.GONE);
        findViewById(R.id.greyoverlay).setVisibility(View.GONE);
        findViewById(R.id.btLogin).setVisibility(View.VISIBLE);
        findViewById(R.id.lbLogin).setVisibility(View.VISIBLE);
    }


    public void GainedAccess()
    {
        //Get initial data
        pbActivity.setVisibility(View.VISIBLE);
        findViewById(R.id.greyoverlay).setVisibility(View.VISIBLE);
        findViewById(R.id.btLogin).setVisibility(View.GONE);
        findViewById(R.id.lbLogin).setVisibility(View.GONE);

        MoveOn();
    }


    public void SetBusy(boolean truth)
    {
        pbActivity.setVisibility(truth? View.VISIBLE: View.GONE);
        findViewById(R.id.greyoverlay).setVisibility(truth? View.VISIBLE: View.GONE);
    }


    public void Comment(String newText)
    {
    }


    public void CommentError(String newText)
    {
        NeedLogin();

        new AlertDialog.Builder(this)
                .setTitle("Connection Error")
                .setMessage("Please check internet connection.")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                        SetBusy(false);
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }


    public void CommentResult(String newText)
    {
    }


    public void CommentSystem(String newText)
    {
    }


    public void DataReceived(String endPoint, JSONArray data)
    {
        ++results;
    }
}

