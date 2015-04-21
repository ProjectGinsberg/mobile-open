package com.ginsberg.ginsberg;

import com.ginsberg.api.GAPI;
import com.ginsberg.api.IGAPICallbacks;

import android.app.Activity;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Intent;

import org.json.JSONArray;

import java.lang.reflect.Method;


public class AddSubjective extends FragmentActivity implements IGAPICallbacks
{
    static final int SelectedColour = 0x40404040;
    static final int UnselectedColour = 0x00ffffff;

    private boolean skipDoneAnim;
    private int day;
    private int sH;
    private int sW;

    private int selectedQ1 = 0;
    private int selectedQ2 = 0;
    private int selectedQ3 = 0;
    private int selectedYQ1 = 0;
    private int selectedYQ2 = 0;
    private int selectedYQ3 = 0;
    private int selectedPQ1 = 0;
    private int selectedPQ2 = 0;
    private int selectedPQ3 = 0;

    private float doneAlpha = 1.0f;
    private float dateAlpha = 1.0f;

    private LinearLayout llQuestions;
    private FrameLayout flDate;

    // This is a handle so that we can call methods on our service
    private ScheduleClient scheduleClient;

    private boolean viewDashEnabled = false;

    private boolean updatingJustQuestion = false;



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


    public static Point getRealSize(Display display) {
        Point outPoint = new Point();
        Method mGetRawH;
        try {
            mGetRawH = Display.class.getMethod("getRawHeight");
            Method mGetRawW = Display.class.getMethod("getRawWidth");
            outPoint.x = (Integer) mGetRawW.invoke(display);
            outPoint.y = (Integer) mGetRawH.invoke(display);
            return outPoint;
        } catch (Throwable e) {
            return null;
        }
    }

    public static Point getSize(Display display) {
        if (Build.VERSION.SDK_INT >= 17) {
            Point outPoint = new Point();
            DisplayMetrics metrics = new DisplayMetrics();
            display.getRealMetrics(metrics);
            outPoint.x = metrics.widthPixels;
            outPoint.y = metrics.heightPixels;
            return outPoint;
        }
        if (Build.VERSION.SDK_INT >= 14) {
            Point outPoint = getRealSize(display);
            if (outPoint != null)
                return outPoint;
        }
        Point outPoint = new Point();
        if (Build.VERSION.SDK_INT >= 13) {
            display.getSize(outPoint);
        } else {
            outPoint.x = display.getWidth();
            outPoint.y = display.getHeight();
        }
        return outPoint;
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addsubjective);

        GinsbergApp.ApplyScaling(this,(ViewGroup) findViewById(android.R.id.content));

        // Create a new service client and bind our activity to this service
        scheduleClient = new ScheduleClient(this);
        scheduleClient.doBindService();

        llQuestions = (LinearLayout) findViewById(R.id.llQuestions);
        flDate = (FrameLayout) findViewById(R.id.flDate);

        //Update fonts
        Typeface osr = Typeface.createFromAsset(getAssets(), "OpenSans-Regular.ttf");
        Typeface osl = Typeface.createFromAsset(getAssets(), "OpenSans-Light.ttf");
        Typeface osb = Typeface.createFromAsset(getAssets(), "OpenSans-Bold.ttf");
        Typeface ossb = Typeface.createFromAsset(getAssets(), "OpenSans-Semibold.ttf");

        //Questions
        ((TextView) findViewById(R.id.tvQuestDate)).setTypeface(osr);

        ((TextView) findViewById(R.id.q1)).setTypeface(osl);
        ((TextView) findViewById(R.id.a11)).setTypeface(osb);
        ((TextView) findViewById(R.id.a12)).setTypeface(osb);
        ((TextView) findViewById(R.id.a13)).setTypeface(osb);
        ((TextView) findViewById(R.id.a14)).setTypeface(osb);
        ((TextView) findViewById(R.id.a15)).setTypeface(osb);
        ((TextView) findViewById(R.id.q2)).setTypeface(osl);
        ((TextView) findViewById(R.id.a21)).setTypeface(osb);
        ((TextView) findViewById(R.id.a22)).setTypeface(osb);
        ((TextView) findViewById(R.id.a23)).setTypeface(osb);
        ((TextView) findViewById(R.id.a24)).setTypeface(osb);
        ((TextView) findViewById(R.id.a25)).setTypeface(osb);
        ((TextView) findViewById(R.id.q3)).setTypeface(osl);
        ((TextView) findViewById(R.id.a31)).setTypeface(osb);
        ((TextView) findViewById(R.id.a32)).setTypeface(osb);
        ((TextView) findViewById(R.id.a33)).setTypeface(osb);
        ((TextView) findViewById(R.id.a34)).setTypeface(osb);
        ((TextView) findViewById(R.id.a35)).setTypeface(osb);

        //Done screen
        ((TextView) findViewById(R.id.tvDate)).setTypeface(ossb);
        ((TextView) findViewById(R.id.tvDoney)).setTypeface(osl);
        ((Button) findViewById(R.id.btReenter)).setTypeface(osb);
        ((Button) findViewById(R.id.btSubDone)).setTypeface(osb);

        //Date screen
        //((TimePicker)findViewById(R.id.tpNotify)).setTypeface(osl);
        ((CheckBox) findViewById(R.id.cbNotify)).setTypeface(osl);
        ((Button) findViewById(R.id.btDone)).setTypeface(osb);

        //Update text
        ((TextView) findViewById(R.id.tvQuestDate)).setText(GAPI.GetDate());

        // ViewDidLoad
        UpdateInitialView();
        skipDoneAnim = false;

        GAPI.Instance().SetCallbacks(this, this);
        Analytics.Instance().Setup(this);

        //Cut down for simple
        if (GinsbergApp.SIMPLE)
        {
            //Hide done button
            findViewById(R.id.btSubDone).setVisibility(View.GONE);
            Login.LoadValues(this, R.id.cbNotify, R.id.tpNotify);
        }
        else
        if(GinsbergApp.DIRECT)
        {
            Button reenter = (Button)findViewById(R.id.btReenter);
            reenter.setText("UPDATE QUESTIONS");
            Button done = (Button)findViewById(R.id.btSubDone);
            done.setText("UPDATE EVENTS");
            Login.LoadValues(this, R.id.cbNotify, R.id.tpNotify);
        }
        else
        {
            findViewById(R.id.btMenu).setVisibility(View.GONE);
        }

        viewWillAppear();
    }



    void viewWillAppear()
    {
        skipDoneAnim = true;

        GAPI.Instance().GetTodaysSubjective();

        Analytics.Instance().LogScreen("Subjective Screen");
        Analytics.Instance().LogEventParams("Screen", "Change", "Subjective Screen");

        //Setup scene
        day = 0;
        ShowDay();

        //Setup questions
        ShowDayDone();
        ShowQuestions();

        ShowSettings(false,true);
    }


    @Override
    public void onBackPressed()
    {
        if(doneAlpha == 1.0f && dateAlpha == 0.0f)
        {
            if(!GinsbergApp.SIMPLE && !GinsbergApp.DIRECT)
            {
                pressedDone(null);
            }
            else
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
    // Interface
    //

    void UpdateInitialView()
    {
        // Do any additional setup after loading the view.
        Display display = getWindowManager().getDefaultDisplay();
        sW = display.getWidth();
        sH = display.getHeight();

        //Update question widths
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) llQuestions.getLayoutParams();
        params.width = sW * 3;
        llQuestions.setLayoutParams(params);

        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) findViewById(R.id.p1).getLayoutParams();
        lp.width = sW;
        findViewById(R.id.p1).setLayoutParams(lp);
        lp = (LinearLayout.LayoutParams) findViewById(R.id.p2).getLayoutParams();
        lp.width = sW;
        findViewById(R.id.p2).setLayoutParams(lp);
        lp = (LinearLayout.LayoutParams) findViewById(R.id.p3).getLayoutParams();
        lp.width = sW;
        findViewById(R.id.p3).setLayoutParams(lp);
    }


    void ShowDayDone()
    {
        viewDashEnabled = false;
        final Button dashButton = (Button)findViewById(R.id.btWeb);
        final boolean truth = GAPI.Instance().GetDoneTodaySubjective();

        TextView date = (TextView) findViewById(R.id.tvDate);
        date.setText(GAPI.GetDate());
        final FrameLayout frame = (FrameLayout) findViewById(R.id.flDone);
        frame.setEnabled(truth);
        final float alpha = doneAlpha;
        dashButton.setEnabled(false);
        dashButton.setVisibility(View.VISIBLE);

        //Anim
        if ((alpha == (truth ? 0.0f : 1.0f)) || skipDoneAnim)
        {
            viewDashEnabled = false;
            AlphaAnimation anim = new AlphaAnimation(doneAlpha, truth ? 1.0f : 0.0f);
            anim.setDuration(skipDoneAnim ? 1 : 300);
            anim.setFillAfter(true);
            anim.setAnimationListener(new Animation.AnimationListener()
            {
                @Override
                public void onAnimationEnd(Animation anim)
                {
                    dashButton.setEnabled(truth);
                    if(!truth) dashButton.setVisibility(View.GONE);
                    viewDashEnabled = truth;
                }

                @Override
                public void onAnimationRepeat(Animation arg0)
                {
                }

                @Override
                public void onAnimationStart(Animation arg0)
                {
                }
            });
            frame.startAnimation(anim);
            doneAlpha = truth ? 1.0f : 0.0f;
        }
        else
        {
            viewDashEnabled = truth;
            dashButton.setEnabled(truth);
        }

        skipDoneAnim = false;
    }


    void ShowSettings(final boolean truth, final boolean skipDateAnim)
    {
        final FrameLayout frame = (FrameLayout) findViewById(R.id.flDate);
        frame.setEnabled(truth);
        final float alpha = dateAlpha;

        //Anim
        if ((alpha == (truth ? 0.0f : 1.0f)) || skipDateAnim)
        {
            AlphaAnimation anim = new AlphaAnimation(dateAlpha, truth ? 1.0f : 0.0f);
            anim.setDuration(skipDateAnim ? 0 : 300);
            anim.setFillAfter(true);

            anim.setAnimationListener(new Animation.AnimationListener()
            {
                @Override
                public void onAnimationStart(Animation animation)
                {
                    if (truth)
                    {
                        frame.setVisibility(View.VISIBLE);
                        for (int i = 0; i < frame.getChildCount(); i++)
                        {
                            View view = frame.getChildAt(i);
                            view.setVisibility(View.VISIBLE); // Or whatever you want to do with the view.
                        }
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation)
                {
                }

                @Override
                public void onAnimationEnd(Animation animation)
                {
                    if (!truth)
                    {
                        for (int i = 0; i < frame.getChildCount(); i++)
                        {
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


    void ShowDay()
    {
        float w = 320;//self.bgToday.frame.size.width;
        float o = 0;

        switch (day) {
            case 0:
                o = 0;
                break;
            case 1:
                o = -sH;
                break;
            case 2:
                o = -2 * sH;
                break;
            default:
                o = -2 * sH;
                break;
        }
    }


    float CurrentPage()
    {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) llQuestions.getLayoutParams();
        float pw = -params.leftMargin / findViewById(R.id.p1).getLayoutParams().width;

        return pw;
    }


    String QuestionLevelToText(int q)
    {
        switch(q)
        {
            case 1: return "STRONGLY DISAGREE";
            case 2: return "DISAGREE";
            case 3: return "UNDECIDED";
            case 4: return "AGREE";
            case 5: return "STRONGLY AGREE";
        }

        return "";
    }


    void ShowQuestions()
    {
        if (GAPI.Instance() != null && GAPI.Instance().userQuestions != null)
        {
            int count = GAPI.Instance().userQuestions.size();
            if (count > 0) {
                ((TextView) findViewById(R.id.q1)).setText(GAPI.Instance().userQuestions.get(0));
            }
            if (count > 1) {
                ((TextView) findViewById(R.id.q2)).setText(GAPI.Instance().userQuestions.get(1));
            }
            if (count > 2) {
                ((TextView) findViewById(R.id.q3)).setText(GAPI.Instance().userQuestions.get(2));
            }
        }
    }


    //
    // Post data
    //

    void PostWellBeing(int answer, String ques, int ID, int daysAgo)
    {
        Analytics.Instance().LogEventParams("Subjective Questions","ID",""+ID,"Question",ques);
        GAPI.Instance().PostWellbeing(answer, GAPI.GetDateTime(-daysAgo), ques, ID);
    }


    //
    // Actions
    //

    public void pressedSelectionButton(View sender)
    {
        if(doneAlpha != 0.0)
        {
            return;
        }
        //Clear other buttons
        ViewGroup parent = (ViewGroup)sender.getParent();

        for (int i = 0, j = 0; i < parent.getChildCount(); i++)
        {
            View view = parent.getChildAt(i);
            if (view instanceof Button)
            {
                ++j;
                Button btn = (Button)view;
                btn.setBackgroundColor(UnselectedColour);

                if(btn == sender)
                {
                    switch(parent.getId()) {
                        case R.id.p1:
                            selectedQ1 = 5-(j-1);
                            break;
                        case R.id.p2:
                            selectedQ2 = 5-(j-1);
                            break;
                        case R.id.p3:
                            selectedQ3 = 5-(j-1);
                            break;
                    }
                }
            }
        }

        //Set current button
        Button btn = (Button)sender;
        btn.setBackgroundColor(SelectedColour);

        pressedNextButton(null);
    }


    void pressedNextButton(Button sender)
    {
        float pw = 0.0f;

        if(day == 2)
        {
            if(GAPI.Instance().userID != null)
            {
                Analytics.Instance().LogID(GAPI.Instance().userID);
                Analytics.Instance().LogProfileValue("Notification", Login.notification);
            }

            //Send data
            if(GAPI.Instance().userQuestions.size() > 0)
            {
                GAPI.Instance().CheckQuestionsToday();
                if(GAPI.Instance().userQuestionsToday.size() < 1)
                {
                    GAPI.Instance().userQuestionsToday.add(0, selectedQ1);
                }
                else
                {
                    GAPI.Instance().userQuestionsToday.set(0, selectedQ1);
                }

                PostWellBeing(selectedQ1, GAPI.Instance().userQuestions.get(0), Integer.parseInt(GAPI.Instance().userQuestionsID.get(0)), 0);
            }
            if(GAPI.Instance().userQuestions.size() > 1)
            {
                if(GAPI.Instance().userQuestionsToday.size() < 2)
                {
                    GAPI.Instance().userQuestionsToday.add(1, selectedQ2);
                }
                else
                {
                    GAPI.Instance().userQuestionsToday.set(1, selectedQ2);
                }
                PostWellBeing(selectedQ2, GAPI.Instance().userQuestions.get(1), Integer.parseInt(GAPI.Instance().userQuestionsID.get(1)), 0);
            }
            if(GAPI.Instance().userQuestions.size() > 2)
            {
                if(GAPI.Instance().userQuestionsToday.size() < 3)
                {
                    GAPI.Instance().userQuestionsToday.add(2, selectedQ3);
                }
                else
                {
                    GAPI.Instance().userQuestionsToday.set(2, selectedQ3);
                }
                PostWellBeing(selectedQ3, GAPI.Instance().userQuestions.get(2), Integer.parseInt(GAPI.Instance().userQuestionsID.get(2)), 0);
            }

            GAPI.Instance().SetDoneTodaySubjective(true);
            ShowDayDone();

            pw = -1.0f;
            day = -1;

            if(GinsbergApp.SIMPLE)
            {

            }
            else
            if(GinsbergApp.DIRECT)
            {
                if(!updatingJustQuestion)
                {
                    pressedDone(null);
                }
            }
            else
            {
            }
        }
        else
        {
            //Move to next screen
            pw = CurrentPage();
        }

        //if(pw != -1)
        {
            View p1 = findViewById(R.id.p1);
            final float w = -findViewById(R.id.p1).getLayoutParams().width * (pw + 1.0f);
            final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) llQuestions.getLayoutParams();

            TranslateAnimation slide = new TranslateAnimation(0, w - params.leftMargin, 0, 0);

            slide.setDuration(pw == -1 ? 1 : 300);
            slide.setStartOffset(pw == -1? 1500 : 0);
            llQuestions.startAnimation(slide);

            slide.setAnimationListener(new Animation.AnimationListener()
            {
                @Override
                public void onAnimationStart(Animation animation)
                {
                }

                @Override
                public void onAnimationRepeat(Animation animation)
                {
                }

                @Override
                public void onAnimationEnd(Animation animation)
                {
                    llQuestions.clearAnimation();
                    params.setMargins((int) w, params.topMargin, params.rightMargin, params.bottomMargin);
                    llQuestions.setLayoutParams(params);
                }
            });
        }

        ++day;
    }


    private void reshowAnswer(int questionIndex, ViewGroup parent)
    {
        if(GAPI.Instance().userQuestionsToday.size() > questionIndex)
        {
            int index = 5 - GAPI.Instance().userQuestionsToday.get(questionIndex);

            for(int i = 0; i < parent.getChildCount(); ++i)
            {
                if(parent.getChildAt(i) instanceof Button)
                {
                    Button button = (Button) parent.getChildAt(i);

                    if (i-1 == index)
                    {
                        button.setBackgroundColor(SelectedColour);
                    } else
                    {
                        button.setBackgroundColor(UnselectedColour);
                    }
                }
            }
        }
    }


    public void pressedReenter(View sender)
    {
        if(GinsbergApp.DIRECT) updatingJustQuestion = true;

        //Check for current answers\
        if(GAPI.Instance() != null && GAPI.Instance().userQuestionsToday != null && GAPI.Instance().userQuestionsToday.size() > 0)
        {
            reshowAnswer(0, (ViewGroup)findViewById(R.id.p1));
            reshowAnswer(1, (ViewGroup)findViewById(R.id.p2));
            reshowAnswer(2, (ViewGroup)findViewById(R.id.p3));
        }

        GAPI.Instance().ClearDoneTodaySubjective(true);
        ShowDayDone();
    }


    public void pressedDone(View sender)
    {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run()
            {
                // Create an intent that will start the main activity.
                Intent mainIntent = new Intent(AddSubjective.this, GinsbergApp.DIRECT? AddEvents.class: Main.class);
                AddSubjective.this.startActivity(mainIntent);

                // Finish splash activity so user cant go back to it.
                AddSubjective.this.finish();

                // Apply our splash exit (fade out) and main
                //   entry (fade in) animation transitions.
                overridePendingTransition(R.anim.fadein, R.anim.fadeout);
            }
        }, 0);
    }


    public void pressedSettingsClose(View sender)
    {
        Login.SaveValues(this, R.id.cbNotify,R.id.tpNotify);
        ShowSettings(false,false);
    }


    public void pressedSettingsOpen(View sender)
    {
        ShowSettings(true,false);
    }


    public void pressedViewDashBoard(View sender)
    {
        if(viewDashEnabled)
        {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://dashboard.ginsberg.io/?utm_source=ginsbergapp&utm_medium=ginsbergapp&utm_campaign=ginsberg-internal"));
            startActivity(browserIntent);
        }
    }


    //
    // GAPI Callbacks
    //

    public void NeedLogin()
    {

    }


    public void GainedAccess()
    {
    }


    public void SetBusy(boolean truth)
    {

    }

    public void DataReceived(String endPoint, JSONArray data)
    {
        if(endPoint == null || !endPoint.toLowerCase().contains("wellbeing")) return;

        if(data != null && data.length() > 0)
        {
            GAPI.Instance().SetDoneTodaySubjective(true);
        }

        ShowDayDone();
        ShowQuestions();
    }


    //
    // Comments
    //

    public void Comment(String newText)
    {
    }


    public void CommentError(String newText)
    {
        int here = 1;
    }


    public void CommentResult(String newText)
    {
    }


    public void CommentSystem(String newText)
    {
    }
}