package com.ginsberg.ginsberg;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import java.util.HashMap;


/**
 * Created by Christopher on 03/09/2014.
 */
public class GinsbergApp extends Application
{
    public static Boolean SIMPLE = false;
    public static Boolean DIRECT = true;
    public static Boolean TEST = false;

    /**
     * Enum used to identify the tracker that needs to be used for tracking.
     *
     * A single tracker is usually enough for most purposes. In case you do need multiple trackers,
     * storing them all in Application object helps ensure that they are created only once per
     * application instance.
     */
    public enum TrackerName {
        APP_TRACKER, // Tracker used only in this app.
        GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
        ECOMMERCE_TRACKER, // Tracker used by all ecommerce transactions from a company.
    }

    HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();


    @Override
    public void onCreate()//Bundle savedInstanceState)
    {
        super.onCreate();
    }


    synchronized Tracker getTracker(TrackerName trackerId)
    {
        if (!mTrackers.containsKey(trackerId))
        {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            Tracker t = (trackerId == TrackerName.APP_TRACKER) ? analytics.newTracker("UA-44891997-3")
                    : (trackerId == TrackerName.GLOBAL_TRACKER) ? analytics.newTracker(R.xml.global_tracker)
                    : analytics.newTracker(R.xml.ecommerce_tracker);
            mTrackers.put(trackerId, t);

        }
        return mTrackers.get(trackerId);
    }


    //
    //General methods
    //

    public static void hideKeyboard(Activity activity)
    {
        View v = activity.getWindow().getCurrentFocus();
        if (v != null)
        {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }


    public static void showKeyboard(Activity activity, View v)
    {
        if (v != null)
        {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(v,0);
        }
    }


    public static boolean ApplyScaling(Activity activity, ViewGroup parent)
    {
        for(int i = 0; i < parent.getChildCount(); ++i)
        {
            if(!ApplyScaling(activity, parent.getChildAt(i))) return false;
        }

        return true;
    }


    public static boolean ApplyScaling(Activity activity, View view)
    {
        Boolean isButton = view instanceof Button;

        if(activity == null)
        {
            int needToCheck = 1;
            return false;
        }


        if(view instanceof ImageView)
        {
            ImageView tv = (ImageView)view;
            Point size = new Point();
            DisplayMetrics metrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

            float height = metrics.heightPixels;
            float oldPixels = tv.getMeasuredHeight();
            float dp = oldPixels*160.0f/metrics.densityDpi;
            float newpixels = height*dp*0.0015625f;

            com.ginsberg.appshared.Scale.scaleViewAndChildren(tv, newpixels/oldPixels, 0);
        }

        if(view instanceof TextView)
        {
            TextView tv = (TextView)view;
            Point size = new Point();
            DisplayMetrics metrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

            float height = metrics.heightPixels;
            float oldPixels = tv.getTextSize();
            float dp = tv.getTextSize()*160.0f/metrics.densityDpi;
            float newpixels = height*dp*0.0015625f;

            if(oldPixels != 0.0)
            {
                tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, newpixels);
            }
            else
            {
                int needToCheck = 1;
                return false;
            }
        }

        if(view instanceof Button || view instanceof ImageView)
        {
            DisplayMetrics metrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

            float height = metrics.heightPixels;
            float oldPixels = view.getMeasuredHeight();
            float dp = oldPixels*160.0f/metrics.densityDpi;
            float newpixels = height*dp*0.0015625f;

            //Fix up sizes
            if(oldPixels != 0.0)
            {
                ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                layoutParams.width = view.getMeasuredWidth();
                layoutParams.height = (int) oldPixels;
                view.setLayoutParams(layoutParams);

                com.ginsberg.appshared.Scale.scaleViewAndChildren(view, newpixels / oldPixels, 0);
            }
            else
            {
                int needToCheck = 1;
                return false;
            }
        }

        if(view instanceof ViewGroup && !isButton)
        {
            return ApplyScaling(activity, (ViewGroup)view);
        }

        return true;
    }
}
