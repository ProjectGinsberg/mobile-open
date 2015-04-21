//
//  ViewController.swift
//  Example1
//
//  Created by Chris on 22/07/2014.
//  Copyright (c) 2014 Ginsberg. All rights reserved.
//

import UIKit


struct Notification
{
    //Consts
    static let NotifyStoreKey:NSString = "token";
    static let HourStoreKey:NSString = "nothour";
    static let MinStoreKey:NSString = "notmin";
    
    static var hour:Int = 18;
    static var minute:Int = 0;
    static var notification:Bool = true;
}


class VCLogin: GAITrackedViewController, UIPickerViewDelegate, GAPIProtocol
{
    //Outlets
    @IBOutlet weak var vMain: UIView!
    @IBOutlet weak var btLogin: UIButton!
    @IBOutlet weak var lbLogin: UILabel!
    
    
    var CLIENT_ID : String = ""
    var CLIENT_SECRET : String = ""
    var results: Int = 0;
    var sH = UIScreen.mainScreen().bounds.size.height;
    
    
    //View Controller
    override func viewDidLoad()
    {
        super.viewDidLoad()
        
        //Load strings
        CLIENT_ID = NSBundle.mainBundle().objectForInfoDictionaryKey("GinsbergClientID") as! String;
        CLIENT_SECRET = NSBundle.mainBundle().objectForInfoDictionaryKey("GinsbergClientSecret") as! String;
        
        btLogin.layer.borderWidth = 0.0;
        btLogin.layer.cornerRadius = 15;
        
        btLogin.hidden = true;
        lbLogin.hidden = true;
        
        VCLogin.CheckInitialNotification();
    }
    
    
    override func didReceiveMemoryWarning()
    {
        super.didReceiveMemoryWarning()
    }
    
    
    override func viewWillAppear(animated: Bool)
    {
        super.viewWillAppear(animated)
        
        screenName = "Login Screen";
        Analytics.LogScreen("Login Screen");
        Analytics.LogEventParams("Screen",params:"Change","Login Screen");
        GAPI.Instance()!.Setup(CLIENT_ID, secret:CLIENT_SECRET, callbacks:self);
    }
    
    
    override func viewDidAppear(animated: Bool)
    {
        if UIApplication.sharedApplication().respondsToSelector("registerUserNotificationSettings:")
        {
            UIApplication.sharedApplication().registerUserNotificationSettings(UIUserNotificationSettings(forTypes: .Alert | .Sound | .Badge, categories: nil))
        }
    }
    
    
    func MoveOn()
    {
        if(GAPI.Instance()?.userID != nil)
        {
            Analytics.LogID(GAPI.Instance()!.userID);
            Analytics.LogProfileValue("Notification", val: Notification.notification);
        }
        
        
        self.performSegueWithIdentifier("segueToSubjective", sender: self);
    }
    
    
    //Actions
    @IBAction func pressedLogin(sender: UIButton)
    {
        GAPI.Instance()!.Login();
    }
    
    
    //
    // Notifications
    //
    
    internal class func CheckInitialNotification()
    {
        var defaults:NSUserDefaults = NSUserDefaults.standardUserDefaults();
        var obj:AnyObject? = defaults.objectForKey(NSBundle.mainBundle().bundleIdentifier!+(Notification.NotifyStoreKey as String));
        
        if(obj == nil)
        {
            SaveValues(nil, scNotDate:nil);
        }
    }

    
    internal class func AllowedNotification() -> Bool
    {
        if NSProcessInfo().respondsToSelector("isOperatingSystemAtLeastVersion:") &&
            NSProcessInfo().isOperatingSystemAtLeastVersion(NSOperatingSystemVersion(majorVersion: 8, minorVersion: 0, patchVersion: 0))
        {
            var settings:UIUserNotificationSettings = UIApplication.sharedApplication().currentUserNotificationSettings();
            if settings.types == .None
            {
                return false;
            }
        }
    
        return true;
    }
    
    
    internal class func UpdateNotification()
    {
        if !AllowedNotification() {return;}
        
        //Clear notifications
        UIApplication.sharedApplication().cancelAllLocalNotifications();
        
        //Setup future notifications
        if(Notification.notification)
        {
            #if TEST
            var date:NSDate = GAPI.DateFromDate(NSDate(timeIntervalSinceNow:0), withHour:Notification.hour, minute:Notification.minute, second:0);
            #else
            var date:NSDate = GAPI.DateFromDate(NSDate(timeIntervalSinceNow:60*60*24), withHour:Notification.hour, minute:Notification.minute, second:0);
            #endif
            
            var localNotification:UILocalNotification = UILocalNotification();
            localNotification.fireDate = date;
            localNotification.alertBody = "Enter how you feel today.";
            //localNotification.alertAction = [NSString stringWithFormat:@"Swipe to add how you feel today."];
            localNotification.soundName = UILocalNotificationDefaultSoundName;
            localNotification.applicationIconBadgeNumber = 1;
            localNotification.repeatCalendar = NSCalendar.currentCalendar();
            localNotification.repeatInterval = NSCalendarUnit.CalendarUnitDay;
            localNotification.timeZone = NSTimeZone.defaultTimeZone();
            
            UIApplication.sharedApplication().scheduleLocalNotification(localNotification);
        }
    }
    
    
    internal class func LoadValues(dpNotDate:UIDatePicker?, scNotDate:UISegmentedControl?)
    {
        //Load values
        var defaults:NSUserDefaults = NSUserDefaults.standardUserDefaults();
        var obj:AnyObject? = defaults.objectForKey(NSBundle.mainBundle().bundleIdentifier!+(Notification.NotifyStoreKey as String));
        
        if(obj != nil)
        {
            Notification.notification = defaults.boolForKey(NSBundle.mainBundle().bundleIdentifier!+(Notification.NotifyStoreKey as String));
            Notification.hour = defaults.integerForKey(NSBundle.mainBundle().bundleIdentifier!+(Notification.HourStoreKey as String));
            Notification.minute = defaults.integerForKey(NSBundle.mainBundle().bundleIdentifier!+(Notification.MinStoreKey as String));
        }
        else
        {
            Notification.notification = true;
            Notification.hour = 18;
            Notification.minute = 0;
        }
        
        //Update interface
        if(scNotDate != nil && dpNotDate != nil)
        {
            scNotDate!.selectedSegmentIndex = 0;
            if(!Notification.notification) {scNotDate!.selectedSegmentIndex = 1;}
            var format:NSDateFormatter = NSDateFormatter();
        
            format.dateFormat=NSDateFormatter.dateFormatFromTemplate("HH:mm", options: 0, locale: NSLocale.currentLocale());
            var date:NSString = NSString(format:"%02d:%02d",Notification.hour,Notification.minute);
            dpNotDate!.setDate(format.dateFromString(date as String)!, animated: false);
        }
        
        //Use values
        UpdateNotification();
    }
    
    
    internal class func SaveValues(dpNotDate:UIDatePicker?, scNotDate:UISegmentedControl?)
    {
        
        //Get values
        var format:NSDateFormatter = NSDateFormatter();
        format.dateFormat = NSDateFormatter.dateFormatFromTemplate("HH", options: 0, locale: NSLocale.currentLocale());
        
        var newHour = 18;
        var newMin = 0;
        var show:Bool = true;
        
        if(dpNotDate != nil)
        {
            newHour = format.stringFromDate(dpNotDate!.date).toInt()!;
            format.dateFormat = NSDateFormatter.dateFormatFromTemplate("mm", options: 0, locale: NSLocale.currentLocale());
            newMin = format.stringFromDate(dpNotDate!.date).toInt()!;
            show = (scNotDate!.selectedSegmentIndex == 0);
        }
        
        if(Notification.notification != show || Notification.hour != newHour || Notification.minute != newMin)
        {
            var period = "21-24";
            
            if(Notification.hour < 3)
            {
                period = "00-03";
            }
            else
                if(Notification.hour < 6)
                {
                    period = "03-06";
                }
                else
                    if(Notification.hour < 9)
                    {
                        period = "06-09";
                    }
                    else
                        if(Notification.hour < 12)
                        {
                            period = "09-12";
                        }
                        else
                            if(Notification.hour < 15)
                            {
                                period = "12-15";
                            }
                            else
                                if(Notification.hour < 18)
                                {
                                    period = "15-18";
                                }
                                else
                                    if(Notification.hour < 21)
                                    {
                                        period = "18-21";
            }
            
            if(show)
            {
                Analytics.LogEventParams("Notification Switch", params:"On", "true");
                Analytics.LogEventParams("Notification Setup", params:"Range", period, "Hour", String(format:"%02d",newHour), "Minute", String(format:"%02d",newMin));
            }
            else
            {
                Analytics.LogEventParams("Notification Switch", params:"On", "false");
                Analytics.LogEventParams("Notification Disabled", params:"Range", period, "Hour", String(format:"%02d",newHour), "Minute", String(format:"%02d",newMin));
            }
        }
        
        Notification.hour = newHour;
        Notification.minute = newMin;
        Notification.notification = show;
        
        //Save values
        var defaults:NSUserDefaults = NSUserDefaults.standardUserDefaults();
        defaults.setBool(Notification.notification, forKey: NSBundle.mainBundle().bundleIdentifier!+(Notification.NotifyStoreKey as String));
        defaults.setInteger(Notification.hour, forKey: NSBundle.mainBundle().bundleIdentifier!+(Notification.HourStoreKey as String));
        defaults.setInteger(Notification.minute, forKey: NSBundle.mainBundle().bundleIdentifier!+(Notification.MinStoreKey as String));
        defaults.synchronize();
        
        //Use values
        UpdateNotification();
    }
    
   
    //Callbacks
    func NeedLogin()
    {
        btLogin.hidden = false;
        lbLogin.hidden = false;
    }
    
    
    func GainedAccess()
    {
        //Get initial data
        btLogin.hidden = true;
        lbLogin.hidden = true;
        
        MoveOn();
    }
    
    
    func SetBusy(truth: Bool)
    {
    }
    
    
    func Comment(text: String)
    {
    }
    
    
    func CommentError(text: String)
    {
        let alert = UIAlertView();
        alert.title = "Connection Error"
        alert.message = "Please check internet connection."
        alert.addButtonWithTitle("OK")
        alert.show();
    }
    
    
    func CommentResult(text: String)
    {
        
    }
    
    
    func CommentSystem(text: String)
    {
        
    }
    
    
    func DataReceived(endPoint:String, withData:NSObject?, andString:String)
    {
        ++results;
        
        if(results >= 3)
        {
        }
    }
}

