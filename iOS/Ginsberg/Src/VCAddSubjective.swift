//
//  ViewController.swift
//  Example1
//
//  Created by CD on 22/07/2014.
//  Copyright (c) 2014 Ginsberg. All rights reserved.
//

import UIKit


class VCAddSubjective: GAITrackedViewController, GAPIProtocol
{
    //Outlets
    @IBOutlet weak var aiBusy: UIActivityIndicatorView!
    @IBOutlet weak var vMain: UIView!
    @IBOutlet weak var vMenu: UIView!
    
    @IBOutlet weak var bgPreYesterday: UIView!
    @IBOutlet weak var bgYesterday: UIView!
    @IBOutlet weak var bgToday: UIView!
    
    @IBOutlet weak var lbQ1: UILabel!
    @IBOutlet weak var lbQ2: UILabel!
    @IBOutlet weak var lbQ3: UILabel!
    @IBOutlet weak var pcQ1: UIPageControl!
    @IBOutlet weak var pcQ2: UIPageControl!
    @IBOutlet weak var pcQ3: UIPageControl!
    @IBOutlet weak var lbYQ1: UILabel!
    @IBOutlet weak var lbYQ2: UILabel!
    @IBOutlet weak var lbYQ3: UILabel!
    @IBOutlet weak var lbPQ1: UILabel!
    @IBOutlet weak var lbPQ2: UILabel!
    @IBOutlet weak var lbPQ3: UILabel!
    
    @IBOutlet weak var vButtonsToday1: UIView!
    @IBOutlet weak var vButtonsToday2: UIView!
    @IBOutlet weak var vButtonsToday3: UIView!
    @IBOutlet weak var vButtonsYesterday1: UIView!
    @IBOutlet weak var vButtonsYesterday2: UIView!
    @IBOutlet weak var vButtonsYesterday3: UIView!
    @IBOutlet weak var vButtonsPreYesterday1: UIView!
    @IBOutlet weak var vButtonsPreYesterday2: UIView!
    @IBOutlet weak var vButtonsPreYesterday3: UIView!
    
    @IBOutlet weak var dpNotDate: UIDatePicker!;
    @IBOutlet weak var scNotDate: UISegmentedControl!;
    @IBOutlet weak var vNotDone: UIView!;
    @IBOutlet weak var btNotDone: UIButton!;
    @IBOutlet weak var btNotOpen: UIButton!;
    
    //@IBOutlet weak var vMenu: UIVisualEffectView!
    @IBOutlet weak var svMenu: UIScrollView!
    @IBOutlet weak var btReenter: UIButton!;
    @IBOutlet weak var btDone: UIButton!;
    @IBOutlet weak var btDashboard: UIButton!
    
    @IBOutlet weak var lbDoneDate: UILabel!;
    @IBOutlet weak var vDone: UIView!;
    @IBOutlet weak var lbTopDate: UILabel!;
    
    //Variables
    private var skipDoneAnim:Bool = true;
    private var sW = UIScreen.mainScreen().bounds.size.width;
    private var sH = UIScreen.mainScreen().bounds.size.height;
    private var day = 0;
    private var updatingJustQuestion:Bool = false;
    
    
    //
    // View Controller
    //
    
    override func viewDidLoad()
    {
        super.viewDidLoad()
        
        UpdateInitialView();
        skipDoneAnim = false;
        GAPI.Instance()?.SetCallbacks(self);
        
        let string = "(Today) \(GAPI.GetDate())" as String
        var attributedString = NSMutableAttributedString(string: string)
        
        lbTopDate.attributedText = attributedString;
        
        #if SIMPLE
        VCLogin.LoadValues(dpNotDate, scNotDate: scNotDate);
  
        if(!VCLogin.AllowedNotification())
        {
            btNotOpen.hidden = true;
            btNotOpen.enabled = false;
            btNotOpen.userInteractionEnabled = false;
        }
        #else
        #if DIRECT
        //Update button text
        btReenter.setTitle("UPDATE QUESTIONS", forState: UIControlState.Normal);
        btDone.setTitle("UPDATE EVENTS", forState: UIControlState.Normal);
            
        VCLogin.LoadValues(dpNotDate, scNotDate: scNotDate);
            
        if(!VCLogin.AllowedNotification())
        {
            btNotOpen.hidden = true;
            btNotOpen.enabled = false;
            btNotOpen.userInteractionEnabled = false;
        }
        #else
        btNotOpen.hidden = true;
        btNotOpen.enabled = false;
        btNotOpen.userInteractionEnabled = false;
        #endif
        #endif
    }
    
    
    override func didReceiveMemoryWarning()
    {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    
    override func viewWillAppear(animated: Bool)
    {
        super.viewWillAppear(animated)
        
        skipDoneAnim = true;
        
        //DataReceived("wellbeing", withData:nil);
        GAPI.Instance()?.GetTodaysSubjective();
        
        screenName = "Subjective Screen";
        Analytics.LogScreen("Subjective Screen");
        Analytics.LogEventParams("Screen", params:"Change","Subjective Screen");
        
        //Setup scene
        day = 0;
        ShowDay();
       
        svMenu.contentSize = CGSizeMake(sW*3, sH);//*3);
        svMenu.setContentOffset(CGPointMake(0,0/*_sH*2*/), animated:false);
    
        //Setup potential done and questions
        if(GAPI.Instance() != nil)
        {
            ShowDayDone();
            ShowQuestions();
        }
        
        SettingsClose(true);
    }
    
    
    //
    // Interface
    //
    
    private func UpdateInitialView()
    {
        // Do any additional setup after loading the view.
        btReenter.layer.borderWidth = 0.0;
        btReenter.layer.cornerRadius = 20;
        btDone.layer.borderWidth = 0.0;
        btDone.layer.cornerRadius = 20;
        btNotDone.layer.borderWidth = 0.0;
        btNotDone.layer.cornerRadius = 20;
        btDashboard.layer.borderWidth = 0.0;
        btDashboard.layer.cornerRadius = 25;
        
        //Centre buttons
        var w = UIScreen.mainScreen().bounds.width;
        var bW = btReenter.frame.width + btDone.frame.width + 20.0;
        btReenter.frame = CGRectMake((w/2.0) - (bW/2.0),
            btReenter.frame.origin.y, btReenter.frame.size.width, btReenter.frame.size.height);
        btDone.frame = CGRectMake((w/2.0) + (bW/2.0) - btDone.frame.size.width,
            btDone.frame.origin.y, btDone.frame.size.width, btDone.frame.size.height);
        
        #if SIMPLE
            //Fix up buttons
            btDone.hidden = true;
            btDone.userInteractionEnabled = false;
            btDone.enabled = false;
            
            var f:CGRect = btReenter.frame;
            btReenter.frame = CGRectMake((sW-f.size.width)/2.0,f.origin.y,f.size.width,f.size.height);
        #endif
       
        //Move questions to centre
        lbQ1.frame = CGRectMake(0,lbQ1.frame.origin.y,
            sW,lbQ1.frame.size.height);
        lbQ2.frame = CGRectMake(sW,lbQ2.frame.origin.y,
            sW,lbQ2.frame.size.height);
        lbQ3.frame = CGRectMake(sW*2,lbQ3.frame.origin.y,
            sW,lbQ3.frame.size.height);
        
        pcQ1.frame = CGRectMake(0,pcQ1.frame.origin.y,
            sW,pcQ1.frame.size.height);
        pcQ2.frame = CGRectMake(sW,pcQ2.frame.origin.y,
            sW,pcQ2.frame.size.height);
        pcQ3.frame = CGRectMake(sW*2,pcQ3.frame.origin.y,
            sW,pcQ3.frame.size.height);
        
        vButtonsToday1.frame = CGRectMake(0.0,
            (sH-vButtonsToday1.frame.size.height)/2, sW,
            vButtonsToday1.frame.size.height);
        vButtonsToday2.frame = CGRectMake(CGFloat(sW),
            (sH-vButtonsToday2.frame.size.height)/2, sW,
            vButtonsToday2.frame.size.height);
        vButtonsToday3.frame = CGRectMake(CGFloat(sW*2.0),
            (sH-vButtonsToday3.frame.size.height)/2, sW,
            vButtonsToday3.frame.size.height);
    }

    
    func ShowDayDone()
    {
        var truth:Bool = GAPI.Instance()!.GetDoneTodaySubjective();
        lbDoneDate.attributedText = GAPI.GetDateAttributed(0,withFont1:UIFont(name: "OpenSans-Bold", size: 20),withFont2:UIFont(name: "OpenSans", size: 19));
        vDone.userInteractionEnabled = truth;
    
        var alpha:CGFloat = 0.0;
        if(!truth) { alpha = 1.0; }
        var currentAlpha:CGFloat = vDone.alpha;
        
        if(skipDoneAnim || (currentAlpha == alpha))
        {
            if(skipDoneAnim)
            {
                vDone.alpha = 1.0 - alpha;
            }
            else
            {
                UIView.animateWithDuration(0.3,
                                     delay:0.0,
                    options:UIViewAnimationOptions.CurveEaseInOut,
                                animations:{
                                if(truth)
                                {
                                    self.vDone.alpha = 1.0;
                                }
                                else
                                {
                                    self.vDone.alpha = 0.0;
                                }
                              },
                    completion:nil);//^(BOOL completed) {/*_vDone.hidden = !truth;*/});
            }
        }
        skipDoneAnim = false;
    }
    
    
    func ShowDay()
    {
        var o:CGFloat = 0;
    
        switch(day)
        {
        case 0: o = 0; break;
        case 1: o = -sH; break;
        case 2: o = -2*sH; break;
        default: o = -2*sH; break;
        }
    
        bgPreYesterday.frame = CGRectMake(0, o+2*sH, sW*3, sH);
        bgYesterday.frame = CGRectMake(0, o+sH, sW*3, sH);
        bgToday.frame = CGRectMake(0, o, sW*3, sH);
    }
    
    
    func CurrentPage() -> (CGFloat)
    {
        var pt:CGPoint = svMenu.contentOffset;
        var pw:CGFloat = pt.x/sW;
    
        return pw;
    }
    
    
    func QuestionLevelToText(q:Int) -> String
    {
        switch(q)
        {
            case 1: return "STRONGLY DISAGREE";
            case 2: return "DISAGREE";
            case 3: return "UNDECIDED";
            case 4: return "AGREE";
            case 5: return "STRONGLY AGREE";
            default: return "";
        }
    }
    
    
    func ShowQuestions()
    {
        if(GAPI.Instance() != nil && GAPI.Instance()?.userQuestions != nil)
        {
            var count:NSInteger = GAPI.Instance().userQuestions.count;
            if(count > 0) { lbQ1.text = GAPI.Instance().userQuestions[0] as? String; }
            if(count > 1) { lbQ2.text = GAPI.Instance().userQuestions[1] as? String; }
            if(count > 2) { lbQ3.text = GAPI.Instance().userQuestions[2] as? String; }
            if(count > 0) { lbYQ1.text = GAPI.Instance().userQuestions[0] as? String; }
            if(count > 1) { lbYQ2.text = GAPI.Instance().userQuestions[1] as? String; }
            if(count > 2) { lbYQ3.text = GAPI.Instance().userQuestions[2] as? String; }
            if(count > 0) { lbPQ1.text = GAPI.Instance().userQuestions[0] as? String; }
            if(count > 1) { lbPQ2.text = GAPI.Instance().userQuestions[1] as? String; }
            if(count > 2) { lbPQ3.text = GAPI.Instance().userQuestions[2] as? String; }
        }
    }
    
    
    //
    // Post data
    //
    
    func PostWellBeing(forButtons:UIView, ques:String, ID:NSNumber, daysAgo:NSInteger) -> Int32
    {
        var red:CGFloat, green:CGFloat, blue:CGFloat, alpha:CGFloat;
        var answer:Int32 = 0;
    
        //Find selected button
        for(var i = 0; i < forButtons.subviews.count; ++i)
        {
            var button:UIButton = forButtons.subviews[i] as! UIButton;
            var color:CGColor = button.backgroundColor!.CGColor;
            
            if(fabs(CGColorGetAlpha(color)-0.2) < 0.0001)
            {
                Analytics.LogEventParams("Subjective Questions", params:"ID", String(ID.intValue), "Question", ques);
                answer = 5 - i;
                
                GAPI.Instance().PostWellbeing(GAPI.GetDateTime(-daysAgo), value:answer, wbques:ques, wbtype:ID.intValue);
                return answer;
            }
        }
        
        return answer;
    }


    //
    // Actions
    //
    
    @IBAction func pressedSettingsClose(sender: UIButton)
    {
        SettingsClose(false);
    }
    func SettingsClose(quick:Bool)
    {
        if(quick)
        {
            self.vNotDone.frame = CGRectMake(0, -self.vNotDone.frame.size.height, self.vNotDone.frame.size.width, self.vNotDone.frame.size.height);
        }
        else
        {
            VCLogin.SaveValues(dpNotDate, scNotDate:scNotDate);
            
            UIView.animateWithDuration(0.5)
            {
                self.vNotDone.frame = CGRectMake(0, -self.vNotDone.frame.size.height, self.vNotDone.frame.size.width, self.vNotDone.frame.size.height);
            }
        }
    }
    
    
    @IBAction func pressedSettingsOpen(sender: UIButton)
    {
        UIView.animateWithDuration(0.5)
        {
            self.vNotDone.frame = CGRectMake(0, 0, self.vNotDone.frame.size.width, self.vNotDone.frame.size.height);
        }
    }
    
    
    @IBAction func pressedMenuOpen(sender: UIButton)
    {
        UIView.animateWithDuration(0.5)
        {
            self.vMenu.frame = CGRectMake(0, 0, self.vMenu.frame.size.width, self.vMenu.frame.size.height)
        }
    }
    
    
    @IBAction func pressedMenuClose(sender: UIButton)
    {
        UIView.animateWithDuration(0.5)
        {
            self.vMenu.frame = CGRectMake(0, -self.vMenu.frame.size.height, self.vMenu.frame.size.width, self.vMenu.frame.size.height)
        }
    }
    
    
    @IBAction func pressedMenuYesterday(sender: UIButton)
    {
        UIView.animateWithDuration(0.5)
        {
            if(self.day == 1)
            {
                self.day = 0;
            }
            else
            if(self.day == 2)
            {
                self.day = 1;
            }
                
            self.ShowDay();
        }
    }
    
    
    @IBAction func pressedMenuTomorrow(sender: UIButton)
    {
        UIView.animateWithDuration(0.5)
        {
            if(self.day == 0)
            {
                self.day = 1;
            }
            else
            if(self.day == 1)
            {
                self.day = 2;
            }
                
            self.ShowDay();
        }
    }
    
    
    @IBAction func pressedSelectionButton(sender: UIButton)
    {
        //Clear other buttons
        var parent = sender.superview;
        
        for( var i = 0; i < parent?.subviews.count; ++i)
        {
            var button = parent?.subviews[i] as! UIButton;
            button.backgroundColor = UIColor.clearColor();
        }
        
        //Set current button
        sender.setTitleColor(UIColor.whiteColor(), forState:UIControlState.Normal);
        sender.setTitleColor(UIColor.whiteColor(), forState:UIControlState.Selected);
        sender.setTitleColor(UIColor.whiteColor(), forState:UIControlState.Highlighted);
        sender.titleLabel!.textColor = UIColor.whiteColor();
        sender.backgroundColor = UIColor(red:1.0, green:1.0, blue:1.0, alpha:0.2);
    }

    
    
    @IBAction func releasedSelectionButton(sender: UIButton)
    {
        pressedNextButton(nil);
    }
    
    
    @IBAction func pressedDoneButton(sender: UIButton?)
    {
        #if DIRECT
        self.performSegueWithIdentifier("segueSubToEvent", sender: self);
        #else
        self.dismissViewControllerAnimated(true, completion: nil);
        #endif
    }
    
    
    @IBAction func pressedDashboardButton(sender: UIButton?)
    {
        UIApplication.sharedApplication().openURL(NSURL(string:"https://dashboard.ginsberg.io/?utm_source=ginsbergapp&utm_medium=ginsbergapp&utm_campaign=ginsberg-internal")!);
    }
    
    
    @IBAction func pressedNextButton(sender: UIButton?)
    {
        var pw:CGFloat = 0.0;
        
        if(day == 2)
        {
            if(GAPI.Instance()?.userID != nil)
            {
                Analytics.LogID(GAPI.Instance()!.userID);
                Analytics.LogProfileValue("Notification", val: Notification.notification);
            }
            
            //Send data
            if(GAPI.Instance()?.userQuestions.count > 0)
            {
                var i = NSNumber(int: PostWellBeing(vButtonsToday1, ques:GAPI.Instance()!.userQuestions[0] as! String, ID:GAPI.Instance()!.userQuestionsID[0] as! NSNumber, daysAgo:0));
                
                GAPI.Instance()?.CheckQuestionsToday();
                if(GAPI.Instance()?.userQuestionsToday.count < 1) { GAPI.Instance()?.userQuestionsToday.addObject(i); }
                GAPI.Instance()?.userQuestionsToday[0] = i;
            }
            if(GAPI.Instance()?.userQuestions.count > 1)
            {
                var i = NSNumber(int: PostWellBeing(vButtonsToday2, ques:GAPI.Instance()!.userQuestions[1] as!  String, ID:GAPI.Instance()!.userQuestionsID[1] as! NSNumber, daysAgo:0));
                if(GAPI.Instance()?.userQuestionsToday.count < 2) { GAPI.Instance()?.userQuestionsToday.addObject(i); }
                GAPI.Instance()?.userQuestionsToday[1] = i;
            }
            if(GAPI.Instance()?.userQuestions.count > 2)
            {
                var i = NSNumber(int: PostWellBeing(vButtonsToday3, ques:GAPI.Instance()!.userQuestions[2] as! String, ID:GAPI.Instance()!.userQuestionsID[2] as! NSNumber, daysAgo:0));
                if(GAPI.Instance()?.userQuestionsToday.count < 3) { GAPI.Instance()?.userQuestionsToday.addObject(i); }
                GAPI.Instance()?.userQuestionsToday[2] = i;
            }
            
            GAPI.Instance()!.SetDoneTodaySubjective(true);
            ShowDayDone();
            UIApplication.sharedApplication().applicationIconBadgeNumber=0;
            
            pw = -1.0;
            day = -1;

            if(!updatingJustQuestion)
            {
                self.performSegueWithIdentifier("segueSubToEvent", sender: self);
            }
        }
        else
        {
            pw = CurrentPage();
        }
        
        if(CGFloat(Int(pw)) == pw)
        {
            var w = sW*CGFloat(pw+1);
            println("Button Next \(sW)");
                
            UIView.animateWithDuration(0.3)
            {
                self.svMenu.setContentOffset(CGPoint(x:w,y:self.svMenu.contentOffset.y), animated: false);
            }
        }
        
        ++day;
    }
    
    
    func reshowAnswer(questionIndex:Int, parent:UIView)
    {
        if(GAPI.Instance()?.userQuestionsToday.count > questionIndex)
        {
            var index = 5 - (GAPI.Instance()!.userQuestionsToday[questionIndex] as! NSNumber).integerValue;
            
            for( var i = 0; i < parent.subviews.count; ++i)
            {
                var sub = parent.subviews[i] as! UIButton;
                sub.backgroundColor = UIColor.clearColor();
                
                if(i == index)
                {
                    //Set current button
                    sub.setTitleColor(UIColor.whiteColor(), forState:UIControlState.Normal);
                    sub.setTitleColor(UIColor.whiteColor(), forState:UIControlState.Selected);
                    sub.setTitleColor(UIColor.whiteColor(), forState:UIControlState.Highlighted);
                    sub.titleLabel!.textColor = UIColor.whiteColor();
                    sub.backgroundColor = UIColor(red:1.0, green:1.0, blue:1.0, alpha:0.2);
                }
            }
        }
    }
    
    
    @IBAction func pressedReenter(sender: UIButton)
    {
        updatingJustQuestion = true;
        
        //Check for current answers\
        if(GAPI.Instance() != nil && GAPI.Instance()?.userQuestionsToday != nil && GAPI.Instance()?.userQuestionsToday.count > 0)
        {
            reshowAnswer(0, parent:vButtonsToday1);
            reshowAnswer(1, parent:vButtonsToday2);
            reshowAnswer(2, parent:vButtonsToday3);
        }
        
        GAPI.Instance()!.ClearDoneTodaySubjective(true);
        ShowDayDone();
    }
    
    
    //
    // GAPI Callbacks
    //
    
    func SetBusy(truth: Bool)
    {
    }
  
    func DataReceived(endPoint:String, withData:NSObject?, andString:String)
    {
        if(endPoint.lowercaseString.rangeOfString("wellbeing") == nil) {return;}
        
        if(!(withData == nil))
        {
            var items:NSArray = withData as! NSArray;
            if(items.count > 0)
            {
                GAPI.Instance()!.SetDoneTodaySubjective(true);
            }
        }
        
        ShowDayDone();
        ShowQuestions();
    }

    
    //
    // Comments
    //
    
    func Comment(text: String)
    {
    }
    
    
    func CommentError(text: String)
    {
        
    }
    
    
    func CommentResult(text: String)
    {
        
    }
    
    
    func CommentSystem(text: String)
    {
        
    }
}

