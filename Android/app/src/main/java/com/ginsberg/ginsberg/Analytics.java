package com.ginsberg.ginsberg;

import android.support.v4.app.FragmentActivity;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.localytics.android.*;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by Christopher on 01/09/2014.
 */
public class Analytics
{
    private LocalyticsAmpSession localyticsSession;
    private FragmentActivity activity;


    //Singleton
    private static Analytics instance = null;
    protected Analytics() {
        // Exists only to defeat instantiation.
    }
    public static Analytics Instance()
    {
        if(instance == null) {
            instance = new Analytics();
        }
        return instance;
    }


    public void Setup(FragmentActivity _activity)
    {
        activity = _activity;

        if(this.localyticsSession == null)
        {
            try
            {
                //Setup localytics
                // Instantiate the object
                this.localyticsSession = new LocalyticsAmpSession(
                        activity.getApplicationContext());  // Context used to access device resources

                this.localyticsSession.open();           // open the session
                this.localyticsSession.upload();         // upload any data

                // At this point, Localytics Initialization is done.  After uploads complete nothing
                // more will happen due to Localytics until the next time you call it.
            }
            catch(Exception e)
            {

            }
        }
        else
        {
            onPause();
            onResume();
        }
    }


    public void onResume()
    {
        if(localyticsSession != null)
        {
            this.localyticsSession.open();
            this.localyticsSession.upload();
            this.localyticsSession.attach(activity);
        }
    }


    public void onPause()
    {
        if(localyticsSession != null)
        {
            this.localyticsSession.detach();
            this.localyticsSession.close();
            this.localyticsSession.upload();
        }
    }


    public void LogEvent(String event)
    {
        if(localyticsSession != null)
        {
            this.localyticsSession.tagEvent(event);
        }

        // Get tracker.
        Tracker t = ((GinsbergApp)activity.getApplication()).getTracker(GinsbergApp.TrackerName.APP_TRACKER);
        t.send(new HitBuilders.EventBuilder()
                .setCategory("Logging")
                .setAction("Log event")
                .setLabel(event)
                .build());
    }


    public void LogEventParams(String event, String... params)
    {
        Map values = new HashMap();
        int count = 0;
        String preParam = "";

        for(String param : params)
        {
            if(count%2 == 1)
            {
                values.put(preParam, param);
            }
            preParam = param;
            ++count;
        }

        if(localyticsSession != null)
        {
            this.localyticsSession.tagEvent(event, values);
        }
    }


    public void LogScreen(String screen)
    {
        if(localyticsSession != null)
        {
            this.localyticsSession.tagScreen(screen);
        }

        Tracker t = ((GinsbergApp)activity.getApplication()).getTracker(GinsbergApp.TrackerName.APP_TRACKER);
        t.send(new HitBuilders.EventBuilder()
                .setCategory("Logging")
                .setAction("Log screen")
                .setLabel(screen)
                .build());
    }


    public void LogID(String ID)
    {
        if(localyticsSession != null)
        {
            this.localyticsSession.setCustomerId(ID);
        }
    }


    public void LogProfileValue(String name, Object val)
    {
        if(localyticsSession != null)
        {
            this.localyticsSession.setProfileAttribute(name,val);
        }
    }
}
