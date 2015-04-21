package com.ginsberg.api;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.HttpResponse;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;


/**
 * @brief      Main controlling class
 *
 * @details    Description of GAPI goes here
 *
 * Note the following example code:
 * @code
 *    GAPI.Instance().Setup(this, "0C3E41287051F805D76F1ABE5B0C7450F79CBC64", "2C36455FAF3A3B3EF5423TFDE7F1B7B6FAF89425", this);
 * @endcode
 *
 * @note       This class is the only SDK class, other than the callback interface.
 * @attention  Should only be used by those who
 *             know what they are doing.
 * @warning    Not certified for use within mission
 *             critical or life sustaining systems.
 */
public class GAPI
{
    //
    // Consts
    //

    //private static String baseUrl = "project-ginsberg.com";
    private static String baseUrl = "ginsberg.io";

    /*
    //Testing
    private static String signupUrl = "https://splatform.ginsberg.io/account/signup";
    private static String connectionsUrl = "https://splatform.ginsberg.io/account/myconnections";
    private static String authorizationUrlStart = "https://splatform."+baseUrl+"/authorisation/auth?response_type=code&client_id=";
    private static String accessTokenUrl = "https://splatform."+baseUrl+"/authorisation/token";
    private static String HTTPAPI = "https://sapi."+baseUrl;
    private static String HTTPWWW = "https://www."+baseUrl;
    private static String HTTPPLAT = "https://splatform."+baseUrl;
    */

    //Release
    private static String signupUrl = "https://platform.ginsberg.io/account/signup";
    private static String connectionsUrl = "https://platform.ginsberg.io/account/myconnections";
    private static String authorizationUrlStart = "https://platform."+baseUrl+"/authorisation/auth?response_type=code&client_id=";
    private static String accessTokenUrl = "https://platform."+baseUrl+"/authorisation/token";
    private static String HTTPAPI = "https://api."+baseUrl;
    private static String HTTPWWW = "https://www."+baseUrl;
    private static String HTTPPLAT = "https://platform."+baseUrl;

    /*
    //Local
    private static String signupUrl = "https://platform.ginsberg.io/account/signup";
    private static String connectionsUrl = "https://platform.ginsberg.io/account/myconnections";
    private static String localPlatformUrl = "http://chriswebtest:16912";
    private static String localAPIUrl = "http://chriswebtest:56924";
    private static String authorizationUrlStart = localPlatformUrl+"/authorisation/auth?response_type=code&client_id=";
    private static String accessTokenUrl = localPlatformUrl+"/authorisation/token";
    private static String HTTPAPI = localAPIUrl;
    private static String HTTPWWW = "https://www."+baseUrl;
    private static String HTTPPLAT = localPlatformUrl;
    */
    private static String authorizationUrlEnd = "&scope=BasicDemographicRead%20SubjectiveRead%20SubjectiveWrite%20ObjectiveRead%20ObjectiveWrite&redirect_uri=ginsberg://activation_code";
    private static String TokenStoreKey = "Token";


    //
    // Variables
    //

    //Setup
    private String clientID = "";
    private String clientSecret = "";
    private String auth = "";
    //For data
    private String token;
    //General
        //Set false if do not want to show reconnection dialog when no internet connection can be found
    public boolean showReconnect = true;
        //Reference to runtime created logins webview
    private WebView webView = null;
        //Reference to runtime created grey busy backing
    private ImageView vActivityBacking = null;
        //Reference to runtime created busy spinner
    private ProgressBar vActivityProgress = null;
        //Changing reference for current callbakcs destination object
    private IGAPICallbacks callbacks;
        //Changing reference for current activity
    private Activity activity;
        //Truth of current busy state
    private boolean busy = false;
        //Truth of have todays event data
    private boolean gotEventToday = false;
        //Truth of have todays subjective data
    private boolean gotSubjectiveToday = false;
        //Truth of if need to skip next error that might popup
    private boolean skipError = false;
        //Count of initial return datas before carrying on from initial login
    private boolean webSignup = false;
    private int initialChecks = 6;
        //State of current posted data
    private enum PostingState {INACTIVE,QUESTIONING,RETRY};
    private static PostingState CurrentPosting = PostingState.INACTIVE;


    //Obtained data
    /**
      *  @brief      Current users numeric id
      */
    public String userID;

    /**
     *  @brief      Current users numeric id
     */
    public String userFirstName;
    /**
     *  @brief      Current users numeric id
     */
    public String userLastName;
    /**
     *  @brief      Current users numeric id
     */
    public String userPhoneNumber;
    /**
     *  @brief      Current users numeric id
     */
    public String userCountry;
    /**
     *  @brief      Current users tags
     */
    public List<String> countries;
    /**
      *  @brief      Current users tags
      */
    public List<String> userTags;
    /**
     *  @brief      Current users tags
     */
    public List<String> userTagsOrdered;
    /**
     *  @brief      Current users tags
     */
    public List<String> tagsEmotions;
    /**
     *  @brief      Current users tags
     */
    public List<String> tagsScots;
    /**
      *  @brief      Current users subjective questions
      */
    public List<String> userQuestions;
    /**
      *  @brief      Question ids for questions listed in userQuestions
      */
    public List<String> userQuestionsID;
    /**
      *  @brief      Current users subjective answers for today
      */
    public List<Integer> userQuestionsToday;
    /**
      *  @brief      Current users event id for todays event
      */
    public long todaysEventID = -1;
    /**
      *  @brief      Current users event for today
      */
    public String todaysEvent = null;
    /*
     *  @brief       Date of last saved subjective entry for current user
     */
    public String lastSaveDateSubjective = null;



    //
    // Setup
    //

    //Singleton
    private static GAPI instance = null;
    protected GAPI()
    {
    }
    /**
      * @brief      Singleton instance
      *
      * @retval     Singleton instance
      *
      */
    public static GAPI Instance()
    {
        if(instance == null) {
            instance = new GAPI();
        }
        return instance;
    }


    public void ClearMemoryStorage()
    {
        countries = null;
        userTags = null;
        userTagsOrdered = null;
        tagsEmotions = null;
        tagsScots = null;
        userQuestions = null;
        userQuestionsID = null;
        userQuestionsToday = null;
    }


    /**
      * @brief      Initial setup method
      *
      * @details    Setup GAPI with initial details, when app first loads. If the user has previously
      *             logged in successfully then the GainedAccess method on the callback will be called,
      *             else NeedLogin will be called.
      *
      * @param      Activity        Calling activity
      * @param      String          Client ID
      * @param      String          Client Secret
      * @param      IGAPICallbacks  Callback reference
      *
      */
    public void Setup(Activity _activity, String _clientID, String _clientSecret, IGAPICallbacks _callbacks)
    {
        webView = null;
        vActivityBacking = null;

        //Assign defaults
        activity = _activity;
        clientID = _clientID;
        clientSecret = _clientSecret;
        callbacks = _callbacks;

        if(HaveToken(activity))
        {
            initialChecks = 0;
            LoadCache();
            token = GetToken(activity);
            GainedAccess();
        }
        else
        {
            NeedLogin();
        }
    }


    /**
      * @brief      Set callback iunstance
      *
      * @details    This should be called whenever the activity changes
      *
      * @param      Activity        Current activity user is interacting with
      * @param      IGAPICallbacks  Interface that callbacks will be sent to
      */
    public void SetCallbacks(Activity _activity, IGAPICallbacks _callbacks)
    {
        activity = _activity;
        callbacks = _callbacks;
    }


    //
    // Signup
    //

    public void SignUpWeb()
    {
        webSignup = true;
        ShowWeb(signupUrl);
    }


    //
    // Connections
    //
    public void ConnectionsWeb(int backgroundID)
    {
        //webSignup = true;
        ShowWeb(connectionsUrl, backgroundID);
    }


    //
    // Login
    //

    /**
      * @brief      Start login process
      */
    public void Login()
    {
        GetAuthorizationCode();
    }


    /**
      * @brief      Called when login is required
      *
      * @details    Starts NeedLogin callback
      */
    public void NeedLogin()
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                RemoveWebView();

                if (callbacks != null)
                {
                    callbacks.NeedLogin();
                }
            }
        });
    }


    /**
      * @brief      Start autorization process
      *
      * @details    Starts NeedLogin callback
      */
    private void GetAuthorizationCode()
    {
        String url = authorizationUrlStart + clientID + authorizationUrlEnd;
        ShowWeb(url);
    }


    private void ShowWeb(String urlString)
    {
        ShowWeb(urlString, -1);
    }

/*
    -(UIViewController*)CurrentViewController
    {
        UIViewController* vc = [[[UIApplication sharedApplication] keyWindow] rootViewController];

        while (vc.presentedViewController)
        {
            vc = vc.presentedViewController;
        }

        return vc;
    }
*/

    private void ShowWeb(String url, int backgroundID)
    {
        //Get screen size
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        FrameLayout parent = new FrameLayout(activity);
        activity.addContentView(parent,new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        float w = metrics.widthPixels;
        float h = metrics.heightPixels;
        float bt = 20.0f;
        float bb = 0.0f;

        if(backgroundID != -1)
        {
            bb = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 50, metrics);

            //Add image background and cancel button
            ImageView i = new ImageView(activity);
            i.setImageResource(backgroundID);
            parent.addView(i);//,new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

            //Close button
            Button b = new Button(activity);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 120, metrics),
                    (int)(bb*0.9f));
            params.setMargins(5,0,5,5);
            params.gravity = Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM;
            b.setLayoutParams(params);
            b.setText("CLOSE");
            b.setTextColor(0xffffffff);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // put code on click operation
                    Comment("Webview Closed");
                }
            });

            parent.addView(b);
        }

        //Create webview
        webView = new WebView(activity);
        webView.setLayoutParams(new FrameLayout.LayoutParams((int)w, (int)(h - bb)));
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebViewJSInterface(), "HTMLOUT");
        parent.addView(webView);

        //Create busy overlay
        vActivityBacking = new ImageView(activity);
        vActivityBacking.setBackgroundColor(0x80000000);
        activity.addContentView(vActivityBacking,new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
        vActivityBacking.setVisibility(busy ? View.VISIBLE : View.GONE);

        //Create busy spinner
        vActivityProgress = new ProgressBar(activity);
        activity.addContentView(vActivityProgress,new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)vActivityProgress.getLayoutParams();
        params.gravity = Gravity.CENTER;
        vActivityProgress.setLayoutParams(params);
        vActivityProgress.setVisibility(busy ? View.VISIBLE : View.GONE);

        //Start up webview
        webView.setWebViewClient(new WebViewClient()
        {
            @Override
            public void onPageFinished (WebView view, String url)
            {
                if (url.toLowerCase().contains("grantaccess"))
                {
                    //Start method to process access details
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");
                }
                else
                {
                    SetBusy(false);
                        //If need to inspect faulty page
                    //webView.loadUrl("javascript:window.HTMLOUT.showHTML('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");
                }

                super.onPageFinished(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url)
            {
                //Special deny cases
                //Special accept cases
                if(    url.contains("accounts.google.com")
                    || url.contains("dashboard.ginsberg.io/#/connections")
                    || url.contains("m.facebook.com")
                    || url.contains("twitter.com")
                    || url.contains("runkeeper.com")
                    || url.contains("fitbit.com")
                    || url.contains("jawbone.com")
                    || url.contains("mapmyfitness.com")
                    || url.contains("about:blank")
                    || url.contains("strava.com")
                    || url.contains("moves-app.com")
                    || url.contains("withings.com")
                )
                {
                    return false;
                }
                //Return to login if have errors
                if(url.contains("denyaccess")
                || url.contains("dashboard."+baseUrl)
                || (webSignup && url.contains("account/SignIn")) )
                {
                    skipError = true;
                    webSignup = false;
                    NeedLogin();
                    RemoveWebView();
                    return true;
                }
                    //Skip extra screens like facebook...
                if(!url.contains("platform."+baseUrl))
                {
                    return true;
                }

                SetBusy(true);
                Comment("Loading " + url);

                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public void onReceivedError(android.webkit.WebView view, int errorCode, java.lang.String description, java.lang.String failingUrl)
            {
                super.onReceivedError(view, errorCode, description, failingUrl);
                RemoveWebView();

                if(skipError)
                {
                    skipError = false;
                }
                else
                {
                    CommentError("Cannot access website");
                }
            }
        });

        try
        {
            SetBusy(true);

            webView.loadUrl(url);//"http://html5test.com");
        }
        catch(Exception e)
        {
            SetBusy(false);
        }
    }


    /**
      * @brief      Methods called from javascript
      */
    class WebViewJSInterface
    {
        @JavascriptInterface
        public void showHTML(String html)
        {
            if (Build.VERSION.SDK_INT < 18)
            {
                webView.clearView();
            }
            else
            {
                activity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        //webView.loadUrl("about:blank");
                        webView.loadUrl("javascript:document.open();document.close();");
                    }
                });
            }

            //Extract data
            int indexStart = html.indexOf("<title>") + 7;
            int indexEnd = html.indexOf("</title>");
            auth = html.substring(indexStart, indexEnd);

            //Get token
            getAccessToken();
        }
    }


    /**
      * @brief      Get access token once require details have been found
      */
    private void getAccessToken()
    {
        final String params =  "code="+auth+"&client_id="+clientID+"&client_secret="+clientSecret+"&grant_type=authorization_code";

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    HttpClient client = GetFixedHttpClient();
                    HttpPost post = new HttpPost(accessTokenUrl);
                    StringEntity se = new StringEntity(params);
                    post.setEntity(se);
                    post.setHeader("Accept", "application/json");
                    post.setHeader("Content-type", "application/x-www-form-urlencoded");

                    //Start call to get token
                    HttpResponse response = client.execute(post);

                    String jsonString = EntityUtils.toString(response.getEntity());
                    final JSONObject json = new JSONObject(jsonString);

                    token = json.getString("access_token");

                    //Extract and save token from returned json
                    SharedPreferences prefs = activity.getSharedPreferences("GAPI", Application.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("Token", token);
                    editor.commit();

                    Comment("Have token!");
                    GainedAccess();
                } catch (Exception e)
                {
                    if (e != null)
                    {
                        Comment(e.getMessage());
                    }
                    SetBusy(false);
                }
            }
        }).start();
    }


    /**
      * @brief      Get initial user data, and call GainedAccess callback, once user has gained initial access
      */
    private void GainedAccess()
    {
        //SetBusy(false);

        if(callbacks != null)
        {
            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    GetMe();
                    GetTags();
                    GetDefaults();
                    GetCountries();
                    GetTodaysEvent();
                    GetTodaysSubjective();

                    if(initialChecks == 0)
                    {
                        callbacks.GainedAccess();
                    }
                }
            });
        }
    }


    //
    // Token methods
    //

    /**
      * @brief      Check for valid token
      *
      * @param      Activity     Calling activity
      *
      * @return     Truth of if have valid token
      */
    public static boolean HaveToken(Activity activity)
    {
        SharedPreferences prefs = activity.getSharedPreferences("GAPI",Application.MODE_PRIVATE);
        return prefs.getString(TokenStoreKey, null) != null;
    }


    private String GetToken(Activity activity)
    {
        SharedPreferences prefs = activity.getSharedPreferences("GAPI",Application.MODE_PRIVATE);
        return prefs.getString(TokenStoreKey, null);
    }


    /**
      * @brief      Clear current stored token
      *
      * @details    Remove current token so user will have to relogin to system
      */
    public void ClearToken()
    {
        userID = null;
        userFirstName = null;
        userLastName = null;
        userPhoneNumber = null;
        userCountry = null;
        todaysEventID = 0;
        todaysEvent = null;
        lastSaveDateSubjective = null;

        ClearMemoryStorage();

        SharedPreferences prefs = activity.getSharedPreferences("GAPI",Application.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.remove("Token");

        editor.remove("USERID");
        editor.remove("USERFIRSTNAME");
        editor.remove("USERLASTNAME");
        editor.remove("USERPHONENUMBER");
        editor.remove("USERCOUNTRY");
        editor.remove("USERTAGLENGTH");
        editor.remove("USERTAGORDEREDLENGTH");
        editor.remove("TAGEMOTIONSLENGTH");
        editor.remove("USERQUESLENGTH");
        editor.remove("USERQUESIDLENGTH");
        editor.remove("LASTSUBJECTIVEDATE");
        editor.remove("USERQUESTODAYLENGTH");
        editor.remove("USERQUESTODAYLENGTH");

        editor.commit();

        //Clear webview cache
        CookieSyncManager.createInstance(activity);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();

        WebView webView = new WebView(activity);
        webView.clearCache(true);
        webView.clearFormData();
        webView.clearHistory();
        webView.clearMatches();

        activity.getApplicationContext().deleteDatabase("webview.db");
        activity.getApplicationContext().deleteDatabase("webviewCache.db");
    }


    //
    // Interface
    //

    private void SetBusy(boolean truth)
    {
        busy = truth;

        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (callbacks != null && !(vActivityBacking != null && busy))
                {
                    callbacks.SetBusy(busy);
                }
                if(vActivityBacking != null)
                {
                    vActivityBacking.setVisibility(busy ? View.VISIBLE : View.GONE);
                }
                if(vActivityProgress != null)
                {
                    vActivityProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
                }
            }
        });
    }


    private void RemoveWebView()
    {
        if(webView != null)
        {
            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    //webView.removeAllViews();
                    ViewGroup vg = (ViewGroup) webView.getParent();
                    vg.removeView(webView);
                    ((ViewGroup) vActivityBacking.getParent()).removeView(vActivityBacking);
                    ((ViewGroup) vActivityProgress.getParent()).removeView(vActivityProgress);
                    webView.destroy();
                    webView = null;
                    vActivityProgress = null;
                    vActivityBacking = null;
                }
            });
        }

        SetBusy(false);
    }


    //
    // OAuth Data
    //

    private void DataReceived(String _endPoint, JSONArray _data)
    {
        final JSONArray data = _data;
        final String endPoint = _endPoint;

        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if(callbacks != null)
                {
                    callbacks.DataReceived(endPoint, data);
                }
            }
        });
    }


    private HttpClient GetFixedHttpClient()
    {
        HttpClient client = new DefaultHttpClient();

        //Fix to be checked, and remove ALLOW_ALL_HOSTNAME_VERIFIER?
        HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

        SchemeRegistry registry = new SchemeRegistry();
        SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
        socketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);
        registry.register(new Scheme("https", socketFactory, 443));
        SingleClientConnManager mgr = new SingleClientConnManager(client.getParams(), registry);
        DefaultHttpClient fixedClient = new DefaultHttpClient(mgr, client.getParams());

        // Set verifier
        HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);

        return fixedClient;
    }


    //Get data
    private void GetData(final String startPoint, String endPoint, String period, String typeFrom, String dateFrom, String typeTo, String dateTo, int id)
    {
        if(period.equals("All"))
        {

        }
        else
        if(period.equals("ID"))
        {
            endPoint += "/"+id;
        }
        else
        {
            endPoint += "?";

            if (period.equals("From") || period.equals("FromTo"))
            {
                endPoint += "start=";
                endPoint +=  typeFrom.equals("Date")? dateFrom: typeFrom;
            }

            if (period.equals("To") || period.equals("FromTo")) {

                endPoint +=  period.equals("FromTo")? "&end=": "end=";
                endPoint +=  typeTo.equals("Date")? dateTo: typeTo;
            }
        }

        final String fEndPoint = endPoint.replace("+","%2B");

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    if (token == null) {
                        Comment("Dont have token!");
                        return;
                    }

                    SetBusy(true);
                    HttpClient client = GetFixedHttpClient();
                    HttpGet get = new HttpGet(startPoint + fEndPoint);

                    get.addHeader("Accept", "application/json");
                    get.addHeader("Authorization", "Bearer " + token);

                    HttpResponse response = client.execute(get);

                    int statusCode = response.getStatusLine().getStatusCode();

                    if (statusCode >= 400)
                    {
                        SetBusy(false);

                        return;
                    }

                    String jsonString = EntityUtils.toString(response.getEntity());

                    //Check for [] for now
                    if(jsonString.startsWith("{"))
                    {
                        jsonString = "[" + jsonString + "]";
                    }

                    JSONArray json = new JSONArray(jsonString);
                    int checks = initialChecks;

                    //Check for particulars
                    if(fEndPoint == "/v1/me" && jsonString.length() > 5)
                    {
                        //Tags
                        JSONArray tags = json.getJSONObject(0).getJSONArray("tags_used");

                        userTags = new ArrayList<String>();

                        for(int i = 0 ; i < tags.length() ; i++)
                        {
                            boolean match = false;
                            String currentTag = tags.getString(i);

                            //Check for match
                            for(int j = 0; j < userTags.size() && !match; ++j)
                            {
                                String foundTag = userTags.get(j);
                                if(foundTag.equals(currentTag)) match = true;
                            }

                            if(!match) userTags.add(currentTag);
                        }

                        //Questions
                        JSONArray questions = json.getJSONObject(0).getJSONArray("wellbeing_metrics");

                        userQuestionsID =  new ArrayList<String>();
                        userQuestions = new ArrayList<String>();

                        for (int i = 0; i < questions.length(); ++i)
                        {
                            JSONObject subJson = questions.getJSONObject(i);
                            String id = subJson.getString("id");
                            String ques = subJson.getString("question");

                            userQuestionsID.add(id);
                            userQuestions.add(ques);
                        }

                        //ID
                        JSONObject jo = json.getJSONObject(0);
                        if(jo.has("id")) userID = jo.getString("id");
                        if(jo.has("first_name")) userFirstName = jo.getString("first_name");
                        if(jo.has("last_name")) userLastName = jo.getString("last_name");
                        if(jo.has("phone_number")) userPhoneNumber = jo.getString("phone_number");
                        if(jo.has("country")) userCountry = jo.getString("country");

                        SaveCache();

                        initialChecks = Math.max(0,initialChecks-1);
                    }
                    else
                    if(fEndPoint == "/v1/tags" && jsonString.length() > 5)
                    {
                        //Tags
                        JSONArray tags = json.getJSONObject(0).getJSONArray("tags");
                        userTagsOrdered = new ArrayList<String>();

                        for(int i = 0; i < tags.length(); ++i)
                        {
                            String name = tags.getJSONObject(i).getString("tag");
                            userTagsOrdered.add(name);
                        }

                        SaveCache();
                        initialChecks = Math.max(0,initialChecks-1);
                    }
                    else
                    if(fEndPoint == "/defaults.json" && jsonString.length() > 5)
                    {
                        JSONObject o = json.getJSONObject(0).getJSONObject("tags");
                        JSONArray emotions = o.getJSONArray("emotions");
                        JSONArray scots = o.getJSONArray("scots");

                        tagsEmotions = new ArrayList<String>();
                        tagsScots = new ArrayList<String>();

                        for(int i = 0; i < emotions.length(); ++i)
                        {
                            tagsEmotions.add(emotions.getString(i));
                        }

                        for(int i = 0; i < scots.length(); ++i)
                        {
                            tagsScots.add(scots.getString(i));
                        }

                        SaveCache();
                        initialChecks = Math.max(0,initialChecks-1);
                    }
                    else
                    if(fEndPoint.startsWith("/account/signupmetadata") && jsonString.length() > 5)
                    {
                        JSONArray tags = json.getJSONObject(0).getJSONArray("countries");
                        countries = new ArrayList<String>();

                        for(int i = 0; i < tags.length(); ++i)
                        {
                            JSONObject array = (JSONObject)tags.get(i);
                            String name = array.getString("name");
                            countries.add(name);
                        }

                        SaveCache();
                        initialChecks = Math.max(0,initialChecks-1);
                    }
                    else
                    if(fEndPoint.startsWith("/v1/o/events") && !gotEventToday)
                    {
                        gotEventToday = true;

                        if(jsonString.length() > 5)
                        {
                            todaysEvent = json.getJSONObject(0).getString("entry");
                            int eid = json.getJSONObject(0).getInt("id");
                            todaysEventID = eid;
                        }
                        else
                        {
                            todaysEvent = "";
                            todaysEventID = -1;
                        }
                        SaveCache();
                        initialChecks = Math.max(0,initialChecks-1);
                    }
                    else
                    if(fEndPoint.startsWith("/v1/wellbeing") && !gotSubjectiveToday)
                    {
                        gotSubjectiveToday = true;

                        if(jsonString.length() > 5)
                        {
                            SetDoneTodaySubjective(false);

                            userQuestionsToday = new ArrayList<Integer>();

                            //Make sure get answers in right order
                            if(userQuestionsID != null)
                            for(int i = 0; i < userQuestionsID.size(); ++i)
                            {
                                Integer measure = Integer.parseInt(userQuestionsID.get(i));

                                for (int j = 0; j < json.length() && j < 3; ++j)
                                {
                                    Integer matching = json.getJSONObject(j).getInt("measure_id");
                                    if (matching == measure)
                                    {
                                        Integer result = json.getJSONObject(j).getInt("value");
                                        userQuestionsToday.add(result);
                                        break;
                                    }
                                }
                            }
                        }
                        else
                        {
                            ClearDoneTodaySubjective(false);
                            userQuestionsToday = null;
                        }

                        SaveCache();
                        initialChecks = Math.max(0,initialChecks-1);
                    }

                    DataReceived(fEndPoint,json);

                    if(checks != initialChecks && initialChecks == 0)
                    {
                        RemoveWebView();
                        activity.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                callbacks.GainedAccess();
                            }
                        });
                    }
                }
                catch (Exception e)
                {
                    CommentError("Problems getting data");
                }
            }
        }).start();
    }


    private void GetData(String endPoint, String period, String typeFrom, String dateFrom, String typeTo, String dateTo, int id)
    {
        GetData(HTTPAPI, endPoint, period, typeFrom, dateFrom, typeTo, dateTo, id);
    }


    //Post data
    private void PostDataToStart(final String startPoint, final String endPoint, final Object... values)
    {
        SetBusy(true);
        JSONObject holder = new JSONObject();

        for(int i = 0; i+1 < values.length; i+=2)
        {
            try
            {
                String s1 = (String)values[i];
                Object s2 = values[i+1];

                if(s2 != null)
                {
                    String s2s = s2.toString();

                    if (!s2s.equals("-1") && !s2s.equals("-1.0"))
                    {
                        holder.put(s1, s2);
                    }
                }
            }
            catch (Exception e)
            {
                Comment(e.getMessage());
                SetBusy(false);
                return;
            }
        }

        final String fEndPoint = endPoint;
        final String url = startPoint+endPoint;
        final JSONObject fHolder = holder;

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
               try
               {
                    HttpClient client = GetFixedHttpClient();// new DefaultHttpClient();
                    HttpPost post = new HttpPost(url);

                    String output = fHolder.toString().replace("\"{","{").replace("}\"","}").replace("\\\"","\"");
                    post.setEntity(new StringEntity(output));
                    post.setHeader("Accept", "application/json");
                    post.setHeader("Authorization", "Bearer " + token);
                    post.setHeader("Content-type", "application/json");

                    HttpResponse response = client.execute(post);

                    int statusCode = response.getStatusLine().getStatusCode();

                    if (statusCode >= 400)
                    {
                        Comment("Posting failed");
                        SetBusy(false);
                        return;
                    }

                    String jsonString = EntityUtils.toString(response.getEntity());
                    JSONArray json;

                    if (jsonString.startsWith("{")) jsonString = "[" + jsonString + "]";

                    if (jsonString.length() >= 2)
                    {
                        json = new JSONArray(jsonString);

                        //Check for particulars
                        if(endPoint.startsWith("/v1/o/events"))
                        {
                            //Get todays event if changed
                            GetTodaysEvent();
                        }

                        if(endPoint.startsWith("/account/externalsignup"))
                        {
                            int status = json.getJSONObject(0).getInt("status");

                            if(status >= 400)
                            {
                                CommentError(json.getJSONObject(0).getString("message"));
                            }
                            else
                            {
                                Comment("Signup success!");
                            }

                            return;
                        }

                    }

                    Comment("Posting success " + jsonString);

                    SetBusy(false);
               }
               catch (Exception e)
               {
                   Comment(e.getMessage());

                   if(showReconnect)
                   {
                       if (CurrentPosting == PostingState.INACTIVE || CurrentPosting == PostingState.RETRY)
                       {
                           CurrentPosting = PostingState.QUESTIONING;

                           activity.runOnUiThread(new Runnable() {
                               public void run() {
                                   new AlertDialog.Builder(activity)
                                           .setTitle("Data Not Saved")
                                           .setMessage("Connection error. Please check internet connection before continuing.")
                                           .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                               public void onClick(DialogInterface dialog, int which) {
                                                   CurrentPosting = PostingState.RETRY;
                                               }
                                           })
                                           .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                               public void onClick(DialogInterface dialog, int which) {
                                                   CurrentPosting = PostingState.INACTIVE;
                                                   SetBusy(false);
                                               }
                                           })
                                           .setIcon(android.R.drawable.ic_dialog_alert)
                                           .show();
                               }
                           });
                       }

                       while(CurrentPosting == PostingState.QUESTIONING)
                       {
                            try
                            {
                                Thread.sleep(500);
                            }
                            catch (Exception e2)
                            {

                            }
                       }

                       if(CurrentPosting == PostingState.RETRY)
                       {
                           PostData(endPoint,values);
                       }
                       else
                       {
                           SetBusy(false);
                       }
                   }
                   else
                   {
                       SetBusy(false);
                   }
               }
            }
        }).start();
    }


    public void PostData(final String endPoint, final Object... values)
    {
        PostDataToStart(HTTPAPI, endPoint, values);
    }


    //Delete data
    private void DeleteData(String endPoint, int id)
    {
        final String fEndPoint = endPoint;
        final int fid = id;

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    if (token == null)
                    {
                        Comment("Dont have token!");
                        return;
                    }
                    else
                    {
                        Comment("Deleting data " + fid + " from " + fEndPoint + "...");
                    }

                    SetBusy(true);
                    HttpClient client = GetFixedHttpClient();// new DefaultHttpClient();
                    HttpDelete delete = new HttpDelete(HTTPAPI + fEndPoint + "/" + fid);
                    delete.addHeader("Accept", "application/json");
                    delete.addHeader("Authorization", "Bearer " + token);

                    HttpResponse response = client.execute(delete);

                    String r = response.getStatusLine().toString();
                    String d = response.getAllHeaders().toString();
                    String jsonString = EntityUtils.toString(response.getEntity());

                    Comment("Get response: " + r);

                    if(r.contains(" 200 ") || r.contains(" 201 "))
                    {
                        Comment("Deleting success " + jsonString);
                    }
                    else
                    {
                        Comment("Deleting failed");
                    }

                    SetBusy(false);
                } catch (Exception e) {
                    Comment(e.getMessage());
                    SetBusy(false);
                }
            }
        }).start();
    }
	
	
	//
	// Get/Post particulars
	//

    public int GetUserCountryID()
    {
        if(countries != null && countries.size() > 200 && userCountry != null)
        {
            for(int i = 0; i < countries.size(); ++ i)
            {
                if(countries.get(i).toString().equals(userCountry))
                {
                    return i+1;
                }
            }
        }

        return 1;
    }


    public void SignUp(String fName, String lastName, String password, String cpassword, String email, int countryID, int[] wbIDs)
    {
        JSONArray obj = null;

        try {
            obj = new JSONArray("[1,2,3]");
        }
        catch (Throwable t)
        {

        }

        PostData(HTTPPLAT, "/account/externalsignup", "first_name", fName, "last_name", lastName, "password", password, "confirm_password", cpassword, "email", email, "country_id",
                 1, "wellbeing_measure_ids", obj);
    }


    /**
      * @brief      Get todays event data
      *
      * @details    Make calls to retrieve todays event data and update local store of
      */
    public void GetTodaysEvent()
    {
        gotEventToday = false;
        GetEvents("From", "Date", GetDateTimeStart(0), "", "", -1);
    }


    /**
      * @brief      Get todays subjective data
      *
      * @details    Make calls to retrieve todays subjective data and update local store of
      */
	public void GetTodaysSubjective()
    {
        gotSubjectiveToday = false;
        GetWellbeing("From", "Date", GetDateTimeStart(0), "", "", -1);
    }
	
	
    /**
      * @brief      Trigger getting users current profile
      *
      * @details    Tell the system to get the users current profile info
      */
    public void GetMe()
    {
        GetData("/v1/me", "All", "", "", "", "", 0);
    }


    public void GetTags() { GetData("/v1/tags", "All", "", "", "", "", 0); }


    public void GetDefaults() { GetData(HTTPWWW, "/defaults.json", "All", "", "", "", "", 0); }


    public void GetCountries() { GetData(HTTPPLAT, "/account/signupmetadata", "All", "", "", "", "", 0); }

    public void GetCorrelations() { GetData("/v1/correlations", null, null, null, null, null, -1); }


    /**
      * @brief      Post notifications information about user
      *
      * @details    Send new notifications information to server
      *
      * @param      units        Units of notification
      * @param      timeStamp    Timestamp of when happened
     *
     * @code
     *    GAPI.Instance().PostNotification(2,GAPI.GetDateTime(-1));
     * @endcode
     */
    public void PostNotifications(int units, String timeStamp)
    {
        PostData("/v1/notifications","units",units,"timestamp",timeStamp);
    }


    /**
      * @brief      Request particular notification information from server
      *
      * @details    Get particular notification information from server, given the past constraining parameters
      *
      * @param      period       Units of notification
      * @param      typeFrom     Type of From to get entries from
      * @param      dateFrom     Inclusive date to start getting entries from
      * @param      typeTo       Type of To to get entries upto
      * @param      dateTo       Inclusive date to get entries upto
      * @param      id           Id it entry, if single item
      *
      */
    public void GetNotifications(String period, String typeFrom, String dateFrom, String typeTo, String dateTo, int id)
    {
        GetData("/v1/notifications",period,typeFrom,dateFrom,typeTo,dateTo,id);
    }


    /**
      * @brief      Delete particular notification piece of information
      *
      * @details    Request the server to delete a particular notification entry
      *
      * @param      id     Id of entry to delete
      *
      */
    public void DeleteNotifications(int id) { DeleteData("/v1/notifications",id);}


    public void GetTag(String period, String typeFrom, String dateFrom, String typeTo, String dateTo)
    {
        GetData("/v1/tags", period, typeFrom, dateFrom, typeTo, dateTo, -1);
    }


    public void GetDailySummary(String period, String typeFrom, String dateFrom, String typeTo, String dateTo)
    {
        GetData("/v1/me/dailysummary", period, typeFrom, dateFrom, typeTo, dateTo, -1);
    }


    public void PostProfile(String firstName, String lastName, String phoneNumber, int country)
    {
        PostData("/v1/me", "first_name", firstName, "last_name", lastName, "country_id", country, "phone_number", phoneNumber);
    }
    public void GetProfile()
    {
        GetData("/v1/me", "All", "", "", "", "", 0);
    }


    /**
      * @brief      Upload wellbeing data
      *
      * @details    Upload a single value of wellbegin, for a given time and wellbeing question.
      *
      * @param      int             value - Actual wellbeing value
      * @param      String          timeStamp - Time wellbeing value valid for
      * @param      String          wbques - Wellbeing question value if for
      * @param      int             wbtype - Wellbeing question code
      *
      */
    public void PostWellbeing(int value, String timeStamp, String wbques, int wbtype)
    {
        PostData("/v1/wellbeing","measure_id",wbtype,"value",value,"measure",wbques,"timestamp",timeStamp);
    }
    /**
      * @brief      Get wellbeing data
      *
      * @details    Upload a single value of wellbegin, for a given time and wellbeing question.
      *
      * @param      String          period - Type of period to get data from, respective of the following four period strings
      * @param      String          typeFrom - Date type from
      * @param      String          dateFrom - Date from
      * @param      String          typeTo - Date type to
      * @param      String          dataTo - Date to
      * @param      int             id - ID of data to get, for a particular single piece of data if required
      *
     */
    public void GetWellbeing(String period, String typeFrom, String dateFrom, String typeTo, String dateTo, int id) { GetData("/v1/wellbeing",period,typeFrom,dateFrom,typeTo,dateTo,id); }
    /**
      * @brief      Delete wellbeing data
      *
      * @details    Delete a particular piece of wellbeing data, given the known id for it
      *
      * @param      int             id - ID of particular single piece of wellbeing data
      *
     */
    public void DeleteWellbeing(int id) { DeleteData("/v1/wellbeing",id);}


    public void PostEvents(String timeStamp, String event, long ID)
    {
        PostData("/v1/o/events","entry",event,"timestamp",timeStamp,"source",activity.getTitle().toString(),"id",ID);
    }
    public void GetEvents(String period, String typeFrom, String dateFrom, String typeTo, String dateTo, int id) { GetData("/v1/o/events",period,typeFrom,dateFrom,typeTo,dateTo,id); }
    public void DeleteEvents(int id) { DeleteData("/v1/o/events",id);}
	
	


    public void PostActivity(String start, String end, float dist, float cal, int steps, String timeStamp)
    {
        PostData("/v1/o/activity", "step_count", steps, "start", start, "distance", dist, "calories", cal,  "end", end, "timestamp",timeStamp);
    }
    public void GetActivity(String period, String typeFrom, String dateFrom, String typeTo, String dateTo, int id)
    {
        GetData("/v1/o/activity",period,typeFrom,dateFrom,typeTo,dateTo,id);
    }
    public void DeleteActivity(int id) { DeleteData("/v1/o/activity",id);}


    public void PostNutrition(float calories, float carbohydrates, float fat, float fiber, float protein, float sugar, String timeStamp)
    {
        PostData("/v1/o/nutrition","sugar",sugar,"protein",protein,"fiber",fiber,"fat",fat,"carbohydrates",carbohydrates,"calories",calories,"timestamp",timeStamp);
    }
    public void GetNutrition(String period, String typeFrom, String dateFrom, String typeTo, String dateTo, int id)
    {
        GetData("/v1/o/nutrition",period,typeFrom,dateFrom,typeTo,dateTo,id);
    }
    public void DeleteNutrition(int id) { DeleteData("/v1/o/nutrition",id);}


    public void PostSleep(String timeStamp, int timesAwoken, double awake, double lightSleep, double remSleep, double deepSleep, double totalSleep, int quality)
    {
        PostData("/v1/o/sleep", "total_sleep", totalSleep, "deep_sleep", deepSleep, "rem_sleep", remSleep, "light_sleep", lightSleep,
                                "awake", awake, "times_awoken", timesAwoken, "timestamp",timeStamp,
                                "source",activity.getTitle().toString(),"quality","{\"value\":"+quality+"}");
    }
    public void GetSleep(String period, String typeFrom, String dateFrom, String typeTo, String dateTo, int id)
    {
        GetData("/v1/o/sleep",period,typeFrom,dateFrom,typeTo,dateTo,id);
    }
    public void DeleteSleep(int id) { DeleteData("/v1/o/sleep",id);}


    public void PostBody(float weight, float fat, String timeStamp)
    {
        PostData("/v1/o/body","fat",fat,"weight",weight,"timestamp",timeStamp);
    }
    public void GetBody(String period, String typeFrom, String dateFrom, String typeTo, String dateTo, int id)
    {
        GetData("/v1/o/body",period,typeFrom,dateFrom,typeTo,dateTo,id);
    }
    public void DeleteBody(int id) { DeleteData("/v1/o/body",id);}


    public void PostAlcohol(String timeStamp, double units)
    {
        PostData("/v1/o/alcohol","units",units,"timestamp",timeStamp);
    }
    public void GetAlcohol(String period, String typeFrom, String dateFrom, String typeTo, String dateTo, int id)
    {
        GetData("/v1/o/alcohol",period,typeFrom,dateFrom,typeTo,dateTo,id);
    }
    public void DeleteAlcohol(int id) { DeleteData("/v1/o/alcohol",id);}


    public void PostCaffeine(float mgs, String timeStamp)
    {
        PostData("/v1/o/caffeine","value",mgs,"timestamp",timeStamp);
    }
    public void GetCaffeine(String period, String typeFrom, String dateFrom, String typeTo, String dateTo, int id)
    {
        GetData("/v1/o/caffeine",period,typeFrom,dateFrom,typeTo,dateTo,id);
    }
    public void DeleteCaffeine(int id) { DeleteData("/v1/o/caffeine",id);}


    public void PostSmoking(int units, String timeStamp)
    {
        PostData("/v1/o/smoking","quantity",units,"timestamp",timeStamp);
    }
    public void GetSmoking(String period, String typeFrom, String dateFrom, String typeTo, String dateTo, int id)
    {
        GetData("/v1/o/smoking",period,typeFrom,dateFrom,typeTo,dateTo,id);
    }
    public void DeleteSmoking(int id) { DeleteData("/v1/o/smoking",id);}


    public void PostStepcount(String start, String end, float distance, float calories, int steps, String timeStamp)
    {
        PostData("/v1/o/stepcount","step_count",steps,"calories",calories,"distance",distance,"end",end,"start",start,"activity_type","Walking","source",activity.getTitle().toString(),"timestamp",timeStamp);
    }
    public void GetStepcount(String period, String typeFrom, String dateFrom, String typeTo, String dateTo, int id) { GetData("/v1/o/stepcount",period,typeFrom,dateFrom,typeTo,dateTo,id); }
    public void DeleteStepcount(int id) { DeleteData("/v1/o/stepcount",id);}


    public void PostExercise(String start, String end, float distance, float calories, int steps, String timeStamp)
    {
        PostData("/v1/o/exercise","step_count",steps,"calories",calories,"distance",distance,"end",end,"start",start,"timestamp",timeStamp);
    }
    public void GetExercise(String period, String typeFrom, String dateFrom, String typeTo, String dateTo, int id) { GetData("/v1/o/exercise",period,typeFrom,dateFrom,typeTo,dateTo,id); }
    public void DeleteExercise(int id) { DeleteData("/v1/o/exercise",id);}


    public void PostMeasures(String units, String timeStamp)
    {
        PostData("/v1/measures","units",units,"timestamp",timeStamp);
    }
    public void GetMeasures(String period, String typeFrom, String dateFrom, String typeTo, String dateTo, int id) { GetData("/v1/measures",period,typeFrom,dateFrom,typeTo,dateTo,id); }
    public void DeleteMeasures(int id) { DeleteData("/v1/measures",id); }


    public void PostSurvey(String units, String timeStamp)
    {
        PostData("/v1/survey","units",units,"timestamp",timeStamp);
    }
    public void GetSurvey(String period, String typeFrom, String dateFrom, String typeTo, String dateTo, int id) { GetData("/v1/survey",period,typeFrom,dateFrom,typeTo,dateTo,id); }
    public void DeleteSurvey(int id) { DeleteData("/v1/servey",id);}

    public void PostSocial(String timeStamp)
    {
        PostData("/v1/o/social",/*"source",activity.getTitle().toString(),"entry",value,*/"timestamp",timeStamp);
    }
    public void GetSocial(String period, String typeFrom, String dateFrom, String typeTo, String dateTo, int id) { GetData("/v1/o/social",period,typeFrom,dateFrom,typeTo,dateTo,id); }
    public void DeleteSocial(int id) { DeleteData("/v1/o/social",id);}


	//
	// Comments
	//
	
    private void Comment(String text)
    {
        if(callbacks != null)
        {
            final String ownText = text.toString();

            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    callbacks.Comment(ownText);
                }
            });
        }
    }


    private void CommentError(String text)
    {
        if(callbacks != null)
        {
            final String ownText = text.toString();
            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                callbacks.CommentError(ownText);
                callbacks.SetBusy(false);
                }
            });
        }
    }


    //
	// Date methods
	//
	
    /**
      * @brief      Get string of current date and time
      *
      * @details    Returns a string showing the current date and time
      *            
      * @return     String showing date and time.
      *
      * @retval     String
      */
    public static String GetDateTime()
    {
        return GetDateTime(0);
    }


    /**
      * @brief      Get string of current date and time, given days difference from now
      *
      * @details    Return a string showing the current date and time, after adding the current number of passed days
      *
      * @param      daysDifference  Days different from now. Can be negative.
      *
      * @return     Date and time
      *
      * @retval     String
      */
    public static String GetDateTime(int daysDifference)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ssZZ");

        Calendar now = (Calendar)Calendar.getInstance().clone();
        now.add(Calendar.DAY_OF_MONTH,daysDifference);

        return dateFormat.format(now.getTime())
                + "T" + timeFormat.format(now.getTime());
    }


    public static String GetDateTimeBasic(int daysDifference, String time)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat timeFormat = new SimpleDateFormat("ZZ");

        Calendar now = (Calendar)Calendar.getInstance().clone();
        now.add(Calendar.DAY_OF_MONTH,daysDifference);

        return dateFormat.format(now.getTime())
                   + "T" + (time != null? time: "")
                   + timeFormat.format(now.getTime());
    }


    public static String GetDateTimeStart(int daysDifference)
    {
        return GetDateTimeBasic(daysDifference, "00:00:00");
    }


    public static String GetDateTimeEnd(int daysDifference)
    {
        return GetDateTimeBasic(daysDifference, "23:59:59");
    }


    public static String GetDate()
    {
        return GetDate(0);
    }


    public static String GetDate(int daysDifference)
    {
        Calendar now = (Calendar)Calendar.getInstance().clone();
        now.add(Calendar.DAY_OF_MONTH,daysDifference);
        String day = "";

        switch (now.get(Calendar.DAY_OF_MONTH))
        {
            case 1:  day = "st"; break;
            case 21: day = "st"; break;
            case 31: day = "st"; break;
            case 2:  day = "nd"; break;
            case 22: day = "nd"; break;
            case 3:  day = "rd"; break;
            case 23: day = "rd"; break;
            default: day = "th"; break;
        }

        SimpleDateFormat format = new SimpleDateFormat(" MMM, yyyy");
        return "" + now.get(Calendar.DAY_OF_MONTH) + day + format.format(now.getTime());
    }


    //
    // Cache up values
    //

    public void CheckQuestionsToday()
    {
        if(userQuestionsToday == null) userQuestionsToday = new ArrayList<Integer>();

    }


    public void SetDoneTodaySubjective(boolean saveResult)
    {
        lastSaveDateSubjective = GetDateTimeStart(0);
        if(saveResult) SaveCache();
    }


    public boolean GetDoneTodaySubjective()
    {
        if(lastSaveDateSubjective == null) return false;

        String doneToday = GetDateTimeStart(0);

        return (lastSaveDateSubjective.equals(doneToday));
    }


    public void ClearDoneTodaySubjective(boolean saveResult)
    {
        lastSaveDateSubjective = null;
        if(saveResult) SaveCache();
    }


    public boolean LoadCache()
    {
        if(activity == null) return false;

        //Load values
        SharedPreferences prefs = activity.getSharedPreferences("GINSBERG", Application.MODE_PRIVATE);

        todaysEventID = prefs.getLong("EVENTID", todaysEventID);
        todaysEvent = prefs.getString("EVENT", todaysEvent);
        userID = prefs.getString("USERID", userID);
        userFirstName = prefs.getString("USERFIRSTNAME",userFirstName);
        userLastName = prefs.getString("USERLASTNAME",userLastName);
        userPhoneNumber =prefs.getString("USERPHONENUMBER",userPhoneNumber);
        userCountry = prefs.getString("COUNTRY",userCountry);

        int length = prefs.getInt("COUNTRIESLENGTH", 0);
        if(length != 0)
        {
            countries = new ArrayList<String>();
            for(int i = 0; i < length; ++i)
            {
                countries.add(prefs.getString("COUNTRIES"+i,""));
            }
        }

        length = prefs.getInt("USERTAGLENGTH", 0);
        if(length != 0)
        {
            userTags = new ArrayList<String>();
            for(int i = 0; i < length; ++i)
            {
                userTags.add(prefs.getString("USERTAG"+i,""));
            }
        }

        length = prefs.getInt("USERTAGORDEREDLENGTH", 0);
        if(length != 0)
        {
            userTagsOrdered = new ArrayList<String>();
            for(int i = 0; i < length; ++i)
            {
                userTagsOrdered.add(prefs.getString("USERTAGORDERED"+i,""));
            }
        }

        length = prefs.getInt("TAGEMOTIONSLENGTH", 0);
        if(length != 0)
        {
            tagsEmotions = new ArrayList<String>();
            for(int i = 0; i < length; ++i)
            {
                tagsEmotions.add(prefs.getString("TAGEMOTIONS" + i, ""));
            }
        }

        length = prefs.getInt("TAGSCOTSLENGTH", 0);
        if(length != 0)
        {
            tagsScots = new ArrayList<String>();
            for(int i = 0; i < length; ++i)
            {
                tagsScots.add(prefs.getString("TAGSCOTS" + i, ""));
            }
        }

        length = prefs.getInt("USERQUESLENGTH", 0);
        if(length != 0)
        {
            userQuestions = new ArrayList<String>();
            for(int i = 0; i < length; ++i)
            {
                userQuestions.add(prefs.getString("USERQUES"+i,""));
            }
        }

        length = prefs.getInt("USERQUESIDLENGTH", 0);
        if(length != 0)
        {
            userQuestionsID = new ArrayList<String>();
            for(int i = 0; i < length; ++i)
            {
                userQuestionsID.add(prefs.getString("USERQUESID"+i,""));
            }
        }

        lastSaveDateSubjective = prefs.getString("LASTSUBJECTIVEDATE", null);

        length = prefs.getInt("USERQUESTODAYLENGTH", 0);
        if(length != 0)
        {
            userQuestionsToday = new ArrayList<Integer>();
            for(int i = 0; i < length; ++i)
            {
                userQuestionsToday.add(prefs.getInt("USERQUESTODAY"+i,-1));
            }
        }

        return true;
    }


    public boolean SaveCache()
    {
        if(activity == null) return false;

        //Save values
        SharedPreferences prefs = activity.getSharedPreferences("GINSBERG", Application.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("EVENTID", todaysEventID);
        editor.putString("EVENT", todaysEvent);

        if(userID != null)
        {
            editor.putString("USERID", userID);
        }

        if(userFirstName != null)
        {
            editor.putString("USERFIRSTNAME", userFirstName);
        }

        if(userLastName != null)
        {
            editor.putString("USERLASTNAME", userLastName);
        }

        if(userPhoneNumber != null)
        {
            editor.putString("USERPHONENUMBER", userPhoneNumber);
        }

        if(userCountry != null)
        {
            editor.putString("COUNTRY", userCountry);
        }

        if(countries != null)
        {
            editor.putInt("COUNTRIESLENGTH",countries.size());
            for (int i = 0; i < countries.size(); ++i)
            {
                editor.putString("COUNTRIES" + i, countries.get(i));
            }
        }

        if(userTags != null)
        {
            editor.putInt("USERTAGLENGTH",userTags.size());
            for (int i = 0; i < userTags.size(); ++i)
            {
                editor.putString("USERTAG" + i, userTags.get(i));
            }
        }

        if(userTagsOrdered != null)
        {
            editor.putInt("USERTAGORDEREDLENGTH",userTagsOrdered.size());
            for (int i = 0; i < userTagsOrdered.size(); ++i)
            {
                editor.putString("USERTAGORDERED" + i, userTagsOrdered.get(i));
            }
        }

        if(tagsEmotions != null)
        {
            editor.putInt("TAGEMOTIONSLENGTH",tagsEmotions.size());
            for (int i = 0; i < tagsEmotions.size(); ++i)
            {
                editor.putString("TAGEMOTIONS" + i, tagsEmotions.get(i));
            }
        }

        if(tagsScots != null)
        {
            editor.putInt("TAGSCOTSLENGTH",tagsScots.size());
            for (int i = 0; i < tagsScots.size(); ++i)
            {
                editor.putString("TAGSCOTS" + i, tagsScots.get(i));
            }
        }

        if(userQuestions != null) {
            editor.putInt("USERQUESLENGTH",userQuestions.size());
            for (int i = 0; i < userQuestions.size(); ++i) {
                editor.putString("USERQUES" + i, userQuestions.get(i));
            }
        }

        if(userQuestionsID != null) {
            editor.putInt("USERQUESIDLENGTH",userQuestionsID.size());
            for (int i = 0; i < userQuestionsID.size(); ++i) {
                editor.putString("USERQUESID" + i, userQuestionsID.get(i));
            }
        }

        if(lastSaveDateSubjective != null)
        {
            editor.putString("LASTSUBJECTIVEDATE", lastSaveDateSubjective);
        }

        if(userQuestionsToday != null) {
            editor.putInt("USERQUESTODAYLENGTH",userQuestionsToday.size());
            for (int i = 0; i < userQuestionsToday.size(); ++i) {
                editor.putInt("USERQUESTODAY" + i, userQuestionsToday.get(i));
            }
        }
        else
        {
            editor.remove("USERQUESTODAYLENGTH");
            for (int i = 0; i < 3; ++i) {
                editor.remove("USERQUESTODAY" + i);
            }
        }

        editor.commit();

        return true;
    }


    public boolean EmptyCache()
    {
        if(activity == null) return false;

        //Save values
        SharedPreferences prefs = activity.getSharedPreferences("GINSBERG", Application.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.commit();

        return true;
    }
}

