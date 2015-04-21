//
//  ViewController.swift
//  Example1
//
//  Created by Chris on 22/07/2014.
//  Copyright (c) 2014 Ginsberg. All rights reserved.
//

import UIKit


class VCAddEvents: GAITrackedViewController, UIPickerViewDelegate, GAPIProtocol,
    UITextViewDelegate
{
    //Outlets
    @IBOutlet weak var aiBusy: UIActivityIndicatorView!
    @IBOutlet weak var vMain: UIView!    
    @IBOutlet weak var vMenu: UIView!
    
    @IBOutlet weak var tvPreYesterday: UITextView!
    @IBOutlet weak var tvYesterday: UITextView!
    @IBOutlet weak var tvToday: UITextView!
    
    @IBOutlet weak var vTags: UIScrollView!
    
    @IBOutlet weak var bgPreYesterday: UIView!
    @IBOutlet weak var bgYesterday: UIView!
    @IBOutlet weak var bgToday: UIView!
    @IBOutlet weak var svMenu: UIScrollView!
    @IBOutlet weak var vButtons: UIView!
    
    @IBOutlet weak var lDate: UILabel!
    @IBOutlet weak var btDone: UIButton!
    
    @IBOutlet weak var scTagInput: UISegmentedControl!
    @IBOutlet weak var scTags: UISegmentedControl!
    
    var day = 0;
    var sH = UIScreen.mainScreen().bounds.size.height;
    var showingKeyboard = false;
    var dummyView:UIView = UIView.alloc();
    
    
    //View Controller
    override func viewDidLoad()
    {
        super.viewDidLoad()
        
        GAPI.Instance()?.SetCallbacks(self);
        
        tvToday.becomeFirstResponder();
        
        self.setNeedsStatusBarAppearanceUpdate();
        
        //Update segment styling
        scTagInput.frame = CGRectMake(scTagInput.frame.origin.x,scTagInput.frame.origin.y,
            scTagInput.frame.size.width, 40);
        scTags.frame = CGRectMake(scTags.frame.origin.x,scTags.frame.origin.y,
            scTags.frame.size.width, 40);
        
        //Update segment control fonts
        let attributes1: NSDictionary = [NSFontAttributeName: UIFont(name: "OpenSans-Semibold", size: CGFloat(12.0))!, NSForegroundColorAttributeName: UIColor(red: 0.8706, green: 0.7333, blue: 0.8314, alpha: 1.0)];//.blueColor()];// (white: 1.0, alpha: 0.5)];
        scTagInput.setTitleTextAttributes(attributes1 as [NSObject : AnyObject], forState:.Normal);
        let selectedAttributes: NSDictionary = [NSFontAttributeName: UIFont(name: "OpenSans-Semibold", size: CGFloat(12.0))!, NSForegroundColorAttributeName: UIColor(red: 0.7686, green: 0.4980, blue: 0.6627, alpha: 1.0)];//(white: 1.0, alpha: 1.0)];
        scTagInput.setTitleTextAttributes(selectedAttributes as [NSObject : AnyObject], forState:.Selected);
    
        let attributes2: NSDictionary = [NSFontAttributeName: UIFont(name: "OpenSans-Semibold", size: CGFloat(12.0))!, NSForegroundColorAttributeName: UIColor(white: 1.0, alpha: 0.5)];
        scTags.setTitleTextAttributes(attributes2 as [NSObject : AnyObject], forState:.Normal);
        let selectedAttributes2: NSDictionary = [NSFontAttributeName: UIFont(name: "OpenSans-Semibold", size: CGFloat(12.0))!, NSForegroundColorAttributeName: UIColor(white: 1.0, alpha: 1.0)];
        scTags.setTitleTextAttributes(selectedAttributes2 as [NSObject : AnyObject], forState:.Selected);
    }
    
    
    override func didReceiveMemoryWarning()
    {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    
    override func viewWillAppear(animated: Bool)
    {
        super.viewWillAppear(animated)
        
        lDate.attributedText = GAPI.GetDateAttributed(0,withFont1:UIFont(name: "OpenSans-Bold", size: 14),withFont2:UIFont(name: "OpenSans", size: 13));
        
        if(GAPI.Instance().todaysEvent == nil || GAPI.Instance().todaysEvent.isEmpty)
        {
        }
        else
        {
            tvToday.text = GAPI.Instance().todaysEvent;
        }
        
        btDone.layer.borderWidth = 0.0;
        btDone.layer.cornerRadius = 20;
        
        screenName = "Add Events Screen";
        
        //Setup scene
        ShowDay();
        
        //Setup scroll view
        var w = svMenu.frame.size.width;
        svMenu.contentSize = CGSize(width:w, height:sH*3);
        svMenu.setContentOffset(CGPoint(x:0,y:sH*2), animated: false);
       
        //Setup tags
        ShowTags();
        
        //Register for keyboard
        NSNotificationCenter.defaultCenter().addObserver(self, selector: "keyboardWasShown:", name: UIKeyboardWillShowNotification, object: nil);
        NSNotificationCenter.defaultCenter().addObserver(self, selector: "keyboardWillBeHidden:", name: UIKeyboardWillHideNotification, object: nil);
    
        ShowDay();
    }
    
    
    func CheckiPhone4(uiv:UIView)
    {
        uiv.frame = CGRectMake(uiv.frame.origin.x,
            sH-(568-uiv.frame.origin.y),
            uiv.frame.size.width,uiv.frame.size.height);
    }
    
    
    func CheckShortenForiPhone4(uiv:UIView)
    {
        uiv.frame = CGRectMake(uiv.frame.origin.x,
            uiv.frame.origin.y,
            uiv.frame.size.width,uiv.frame.size.height-(568-sH));
    }
    
    
    @IBAction func ActionTagsChanged(sender: AnyObject)
    {
        ShowTags();
    }
    
    
    @IBAction func InputSwitch(sender: UISegmentedControl)
    {
        var which = sender.selectedSegmentIndex;
        
        if(which == 0)
        {
            tvToday.resignFirstResponder();
            tvToday.inputView = nil;
            tvToday.becomeFirstResponder();
        }
        else
        {
            tvToday.resignFirstResponder();
            tvToday.inputView = dummyView;
        }
    }
    
    
    func ShowTags()
    {
        var subViews = vTags.subviews;
        
        for v in subViews
        {
            v.removeFromSuperview()
        }
        var startX:CGFloat = 14.0;
        var startY:CGFloat = 10.0;
        
        //Get used tags
        var usedTags:NSMutableArray?;
        
        switch(scTags.selectedSegmentIndex)
        {
        case 0: usedTags = GAPI.Instance()?.userTags; break;
        case 1: usedTags = GAPI.Instance()?.userTagsOrdered; break;
        case 2: usedTags = GAPI.Instance()?.tagsEmotions; break;
        case 3: usedTags = GAPI.Instance()?.tagsScots; break;
        default: break;
        }
        
        if(usedTags != nil)
        {
            for(var i = 0; i < usedTags!.count; ++i)
            {
                var tag:NSString = usedTags![i] as! NSString;
                
                var button:UIButton = UIButton.buttonWithType(UIButtonType.System) as! UIButton;
                //CGSize bounds = [tag sizeWithFont:button. yourFont];
                
                var width:CGFloat = UIScreen.mainScreen().bounds.size.width - (2.0 * 14.0);
                
                //Third
                if(tag.length < 8)
                {
                    width = (width - 2.0*6.0)/3.0;
                }
                else
                //Half
                if(tag.length < 12)
                {
                    width = (width - 6.0)/2.0;
                }
                else
                //Two thirds
                if(tag.length < 16)
                {
                    width = (width - 6.0)*(2.0/3.0);
                }
                else
                {
                    //width = width;
                }
                
                if(startX + width+6.0 > UIScreen.mainScreen().bounds.size.width - 6.0)
                {
                    startX = 14.0;
                    startY += 46.0;
                }
                
                button.frame = CGRectMake(startX, startY, width, 40.0);
                startX += width+6.0;
                button.setTitle(tag as String, forState: UIControlState.Normal);
                button.setTitleColor(UIColor.whiteColor(), forState: UIControlState.Normal);
                button.layer.borderWidth = 0.0;
                button.layer.cornerRadius = 20;
                button.backgroundColor = UIColor(red: 0.8784, green: 0.6510, blue: 0.7529, alpha: 1.0);
                button.addTarget(self, action: "pressedTag:", forControlEvents: UIControlEvents.TouchUpInside);
                
                vTags.addSubview(button);
            }
            vTags.contentSize = CGSize(width:startX, height:startY+30.0);
        }
    }
    
    
    // Called when the UIKeyboardDidShowNotification is sent.
    func keyboardWasShown(aNotification:NSNotification)
    {
        showingKeyboard = true;
        var info:NSDictionary =  aNotification.userInfo!;
        var keyboardSize = (info[UIKeyboardFrameBeginUserInfoKey] as! NSValue).CGRectValue().size;
    }
    
    
    // Called when the UIKeyboardWillHideNotification is sent
    func keyboardWillBeHidden(aNotification:NSNotification)
    {
        showingKeyboard = false;
        var info:NSDictionary =  aNotification.userInfo!;
        var keyboardSize = (info[UIKeyboardFrameBeginUserInfoKey] as! NSValue).CGRectValue().size;
    }
    
    
    //Methods
    private func ShowDay()
    {
        var w = self.bgToday.frame.size.width;
        var o:CGFloat = 0;
        
        switch(day)
        {
        case 0: o = 0; break;
        case 1: o = -sH; break;
        case 2: o = -2*sH; break;
        default: o = -2*sH; break;
        }
        
        self.bgPreYesterday.frame = CGRectMake(0, o, w, sH);
        self.bgYesterday.frame = CGRectMake(0, o+sH, w, sH);
        self.bgToday.frame = CGRectMake(0, o+2*sH, w, sH);
    }
    
    
    //Actions
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
    
    
    @IBAction func pressedTag(sender: UIButton)
    {
        var title:String = sender.titleForState(UIControlState.Normal)!;
        var result:String = tvToday.text + " #\(title) ";
        tvToday.text = result;
        tvToday.selectedRange = NSMakeRange(count(result),0);
    }
    
    
    @IBAction func pressedDone(sender: UIButton)
    {
        
        if(showingKeyboard)
        {
            tvPreYesterday.resignFirstResponder();
            tvYesterday.resignFirstResponder();
            tvToday.resignFirstResponder();
        }
        
        var result:String = tvToday.text;
    
        if(result != GAPI.Instance().todaysEvent)
        {
            var newTags:Array<String> = [];
            
            //Extract tags
            var splits = result.componentsSeparatedByString(" ");
            
            for word:String in splits
            {
                if(word.hasPrefix("#"))
                {
                    var clean:NSString = (word as NSString).substringFromIndex(1);
                    
                    //Check to see if in current tags
                    var match = false;
                    if(GAPI.Instance()?.userTags != nil)
                    {
                        for(var i = 0; !match && i < GAPI.Instance()?.userTags.count; ++i)
                        {
                            var tag:NSString = GAPI.Instance()?.userTags[i] as! NSString;
                            if(tag.isEqualToString(clean as String))
                            {
                                match = true;
                            }
                        }
                    }
                    
                    if(!match)
                    {
                        if(GAPI.Instance()?.userTags != nil)
                        {
                            GAPI.Instance()?.userTags.addObject(clean);
                            newTags.append(clean as String);
                        }
                    }
                }
            }
            
            //Send event
            if(GAPI.Instance()?.userID != nil)
            {
                Analytics.LogID(GAPI.Instance()!.userID);
                Analytics.LogProfileValue("Notification", val: Notification.notification);
            }
            
            GAPI.Instance().PostEvent(GAPI.GetDateTime(0), event:result, ID:GAPI.Instance()!.todaysEventID);
        }
        
        self.dismissViewControllerAnimated(true, completion: nil);
    }
    
    
    @IBAction func pressedCancel(sender: UIButton)
    {
        if(showingKeyboard)
        {
            tvPreYesterday.resignFirstResponder();
            tvYesterday.resignFirstResponder();
            tvToday.resignFirstResponder();
        }
        else
        {
            self.dismissViewControllerAnimated(true, completion: nil);
        }
    }
    
    
    //Callbacks
    func GainedAccess()
    {
    }
    
    
    func UpdateProgress(progress: Int)
    {
        
    }
    
    
    func SetBusy(truth: Bool)
    {
    }
    
    
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

