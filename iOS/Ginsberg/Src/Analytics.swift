//
//  Analytics.m
//  ginsberg
//
//  Created by Chris on 19/08/2014.
//  Copyright (c) 2014 Ginsberg. All rights reserved.
//

import CoreData


class Analytics
{
    static var google:Bool = false;
    static var localytics:Bool = false;
 
    
    class func Setup(launchOptions:[NSObject : AnyObject]?)
    {
        var gid:AnyObject? = NSBundle.mainBundle().objectForInfoDictionaryKey("GinsbergGoogleAnalyticsID");
    
        if(gid != nil)
        {
            //Google
            // Optional: automatically send uncaught exceptions to Google Analytics.
            GAI.sharedInstance().trackUncaughtExceptions = true;
    
            // Optional: set Google Analytics dispatch interval to e.g. 20 seconds.
            GAI.sharedInstance().dispatchInterval = 10;
    
            // Optional: set Logger to VERBOSE for debug information.
            //GAI.sharedInstance().setLogLevel(kGAILogLevelVerbose);
    
            // Initialize tracker. Replace with your tracking ID.
            GAI.sharedInstance().trackerWithTrackingId(gid as! String);
        
            google = true;
        }
    
        //Localytics
        #if !NOLOC
        var lid:AnyObject? = NSBundle.mainBundle().objectForInfoDictionaryKey("GinsbergLocalyticsID");
        
        if(lid != nil)
        {
            LocalyticsSession.shared().integrateLocalytics(lid as! String, launchOptions:launchOptions);
        
            localytics = true;
        }
        #endif
    }


    class func LogEvent(event:String)
    {
        #if !NOLOC
        if(localytics)
        {
            LocalyticsSession.shared().tagEvent(event);
        }
        #endif
    }

    
    class func LogEventParams(event:String, params:String...)
    {
        var attributes: [String: String] = [:];
    
        var count = 0;
        var preParam:String = "";
    
        for param in params
        {
            if(count%2 == 1)
            {
                attributes[preParam] = param;
            }
    
            preParam = param;
            ++count;
        }
    
        #if !NOLOC
        if(localytics)
        {
            LocalyticsSession.shared().tagEvent(event, attributes:attributes);
        }
        #endif
    }
    

    class func LogScreen(screen:String)
    {
        #if !NOLOC
        if(localytics)
        {
            LocalyticsSession.shared().tagScreen(screen);
        }
        #endif
    }
    
    
    class func LogID(ID:String)
    {
        #if !NOLOC
        if(localytics)
        {
            LocalyticsSession.shared().setCustomerId(ID);
        }
        #endif
    }
    
    
    class func LogProfileValue(name:String, val:NSObject)
    {
        #if !NOLOC
        if(localytics)
        {
            LocalyticsSession.shared().setProfileValue(val, forAttribute: name);
        }
        #endif
    }
}