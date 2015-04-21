//
//  Ginsberg.swift
//  TestApp1-GUI
//
//  Created by Chris on 09/07/2014.
//  Copyright (c) 2014 Chris. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

#import "GAPI.h"

#pragma GCC diagnostic ignored "-Wobjc-missing-property-synthesis"
#pragma GCC diagnostic ignored "-Wdirect-ivar-access"
#pragma GCC diagnostic ignored "-Wgnu"


#import <Availability.h>
#if !__has_feature(objc_arc)
#error This class requires automatic reference counting
#endif


@implementation GAPI : NSObject

//
// Consts
//

#define signupUrl               @"https://platform.ginsberg.io/account/signup"
#define connectionsUrl          @"https://platform.ginsberg.io/account/myconnections"
#define authorizationUrlStart   @"https://platform.ginsberg.io/authorisation/auth?response_type=code&client_id="
#define ACCESS_TOKEN_URL        @"https://platform.ginsberg.io/authorisation/token"
#define HTTPAPI                 @"https://api.ginsberg.io"
#define HTTPWWW                 @"https://www.ginsberg.io"
#define HTTPPLAT                @"https://platform.ginsberg.io"

const NSString* authorizationUrlEnd = @"&scope=BasicDemographicRead%20SubjectiveRead%20SubjectiveWrite%20ObjectiveRead%20ObjectiveWrite&redirect_uri=ginsberg://activation_code";

#define TOKEN_STORE_KEY         @"Token"


//
//Variables
//

//Setup
NSString* clientID = @"";
NSString* clientSecret = @"";
NSString* auth = @"";
//For data
NSString* token = @"";
//General
bool showReconnect = true;
int activeStack = 0;
id<GAPIProtocol> callbacks;
UIView* webViewParent = nil;
UIView* vActivityBacking = nil;
UIActivityIndicatorView* vActivity = nil;
bool busy = false;
bool gotEventToday = false;
bool gotSubjectiveToday = false;
bool skipError = false;
bool webSignup = false;
int initialChecks = 6;

enum PostingState { INACTIVE, CHECKING, RETRY, CANCEL };
static enum PostingState CurrentPosting = INACTIVE;


//
// Setup
//

+ (GAPI*)Instance
{
    static GAPI *instance = nil;
    static dispatch_once_t onceToken;
    
    dispatch_once(&onceToken,
    ^{
        instance = [[self alloc] init];
    });
    
    return instance;
}


- (id)init
{
    if (self = [super init])
    {
        //Setup
        [self ClearMemoryStorage];
    }
    
    return self;
}
    

-(void)dealloc
{
    [[GAPI Instance] Login];
    [[GAPI Instance] SignUp:@"Please" lastName:@"Replace" password:@"password" cpassword:@"password" email:@"john@example.com" countryID:1 wbIDs:nil];
}


-(void)ClearMemoryStorage
{
    _countries = [[NSMutableArray alloc] init];
    _userTags = [[NSMutableArray alloc] init];
    _userTagsOrdered = [[NSMutableArray alloc] init];
    _tagsEmotions = [[NSMutableArray alloc] init];
    _tagsScots = [[NSMutableArray alloc] init];
    _userQuestions = [[NSMutableArray alloc] init];
    _userQuestionsID = [[NSMutableArray alloc] init];
    _userQuestionsToday = [[NSMutableArray alloc] init];
}


//Checks for valid token, which will trigger login if has
-(void)Setup:(NSString*)_clientID secret:(NSString*)_clientSecret callbacks:(id<GAPIProtocol>)_callbacks;
{
    webViewParent = nil;
    
    //Assign defaults
    clientID = _clientID;
    clientSecret = _clientSecret;
    callbacks = _callbacks;
    
    //Look for token
    if([GAPI HaveToken])
    {
        initialChecks = 0;
        [self LoadCache];
        
        token = [GAPI GetToken];
        [self GainedAccess];
    }
    else
    {
        [self NeedLogin];
    }
}


-(void)SetCallbacks:(id<GAPIProtocol>)_callbacks
{
    callbacks = _callbacks;
}


//
// Signup
//

-(void) SignUpWeb
{
    webSignup = true;
    [self ShowWeb:signupUrl];
}


-(void)SignUp:(NSString*)firstName lastName:(NSString*)lastName password:(NSString*)password cpassword:(NSString*)cpassword email:(NSString*)email countryID:(int)countryID wbIDs:(int[])wbIDs
{
    //Check values
    if(![password isEqualToString:cpassword])
    {
        [[[UIAlertView alloc] initWithTitle:@"Invalid password" message:@"Passwords must match." delegate:self cancelButtonTitle:@"OK" otherButtonTitles:nil, nil] show];
        return;
    }
    
    [self PostData:[HTTPPLAT copy] endPoint:@"/account/externalsignup" params:14,@"first_name",firstName,@"last_name",lastName,@"password",password,@"confirm_password",cpassword,
     @"email",email,@"country_id",[NSNumber numberWithInt:1],@"wellbeing_measure_ids",@"[1,2,3]"];
}


//
// Connections
//

-(void) ConnectionsWeb:(UIImage*)background
{
    //webSignup = true;
    [self ShowWeb:[connectionsUrl copy] withBackground:background];
}


//
// Login
//

-(void)Login
{
    [self GetAuthorizationCode];
}


-(void)NeedLogin
{
    dispatch_async(dispatch_get_main_queue(), ^(void)
    {
        [self RemoveWebView];
        
        if(callbacks != nil)
        {
            if([callbacks respondsToSelector:@selector(NeedLogin)])
                [callbacks NeedLogin];
        }
    });
}


-(void) GetAuthorizationCode
{
    NSString* urlString = [NSString stringWithFormat:@"%@%@%@", authorizationUrlStart, clientID, authorizationUrlEnd];
    [self ShowWeb:urlString];
}


-(void) ShowWeb:(NSString*)urlString
{
    [self ShowWeb:urlString withBackground:nil];
}


-(UIViewController*)CurrentViewController
{
    UIViewController* vc = [[[UIApplication sharedApplication] keyWindow] rootViewController];
    
    while (vc.presentedViewController)
    {
        vc = vc.presentedViewController;
    }
    
    return vc;
}


-(void) ShowWeb:(NSString*)urlString withBackground:(UIImage*)image
{
    NSURL* url = [NSURL URLWithString:urlString];
    NSURLRequest* urlRequest= [[NSURLRequest alloc] initWithURL:url];
    UIViewController* vc = [self CurrentViewController];
    
    webViewParent = [[UIView alloc] init];
    webViewParent.backgroundColor = [UIColor colorWithRed:0.0 green:0.0 blue:0.0 alpha:0.5];
    webViewParent.frame = vc.view.frame;
    [vc.view addSubview:webViewParent];
    
    CGFloat bt = 20.0;
    CGFloat bb = 0.0;
    CGFloat w = webViewParent.frame.size.width;
    CGFloat h = webViewParent.frame.size.height;
    
    if(image != nil)
    {
        bb = 40.0;
        
        //Add image background and cancel button
        UIImageView* vImageBacking = [[UIImageView alloc] initWithFrame:webViewParent.frame];
        vImageBacking.backgroundColor = [UIColor whiteColor];
        [vImageBacking setImage:image];
        [webViewParent addSubview:vImageBacking];
        
        //Close button
        UIButton *button = [UIButton buttonWithType:UIButtonTypeSystem];
        [button addTarget:self
                   action:@selector(DismissView)
         forControlEvents:UIControlEventTouchUpInside];
        [button setTitle:@"CLOSE" forState:UIControlStateNormal];
        button.frame = CGRectMake((w-80.0)/2.0, h - 35.0, 80.0, 30.0);
        button.layer.borderWidth = 0.0;
        button.layer.cornerRadius = 15;
        button.backgroundColor = [UIColor whiteColor];
        button.titleLabel.font = [UIFont systemFontOfSize:12.0];
        [button setTitleColor:[UIColor colorWithRed:0.0 green:0.5 blue:0.0 alpha:1.0] forState:UIControlStateNormal];
        [webViewParent addSubview:button];
    }
    
    UIWebView* webView = [[UIWebView alloc] init];
    h -= bt+bb;
    webView.frame = CGRectMake(0,bt,w,h);
    webView.delegate = self;
    [webViewParent addSubview:webView];
    
    //Set activity indicator
    vActivityBacking = [[UIView alloc] initWithFrame:webViewParent.frame];
    vActivityBacking.backgroundColor = [UIColor colorWithRed:0.0 green:0.0 blue:0.0 alpha:0.5];
    [webViewParent addSubview:vActivityBacking];
    
    vActivity = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleWhiteLarge];
    [webViewParent addSubview:vActivity];
    vActivity.frame = CGRectMake((webViewParent.frame.size.width-vActivity.frame.size.width)/2,
                                 (webViewParent.frame.size.height-vActivity.frame.size.height)/2,
                                 vActivity.frame.size.width, vActivity.frame.size.height);
    [vActivity startAnimating];
    [self SetBusy:true];
    
    NSLog(@"Manual load %@",urlRequest.URL.absoluteString);
    
    [webView loadRequest:urlRequest];
}


// Webview

-(BOOL)webView:(UIWebView *)webView shouldStartLoadWithRequest:(NSURLRequest *)request navigationType:(UIWebViewNavigationType)navigationType
{
    if(request.URL != nil)
    {
        NSLog(@"Should load? %@",request.URL.absoluteString);
        NSString* location = request.URL.absoluteString;
        
        //Special deny cases
        //Special accept cases
        if(   [location rangeOfString:@"accounts.google.com"].location != NSNotFound
           || [location rangeOfString:@"dashboard.ginsberg.io/#/connections"].location != NSNotFound
           || [location rangeOfString:@"m.facebook.com"].location != NSNotFound
           || [location rangeOfString:@"twitter.com"].location != NSNotFound
           || [location rangeOfString:@"runkeeper.com"].location != NSNotFound
           || [location rangeOfString:@"fitbit.com"].location != NSNotFound
           || [location rangeOfString:@"jawbone.com"].location != NSNotFound
           || [location rangeOfString:@"mapmyfitness.com"].location != NSNotFound
           || [location rangeOfString:@"about:blank"].location != NSNotFound
           || [location rangeOfString:@"strava.com"].location != NSNotFound
           || [location rangeOfString:@"moves-app.com"].location != NSNotFound
           || [location rangeOfString:@"withings.com"].location != NSNotFound
           )
        {
            return true;
        }
        if((  [location rangeOfString:@"denyaccess"].location != NSNotFound)
           || ([location rangeOfString:@"dashboard.ginsberg.io"].location != NSNotFound)
           || (webSignup && [location rangeOfString:@"account/SignIn"].location != NSNotFound) )
        {
            skipError = true;
            webSignup = false;
            [self NeedLogin];
            [self RemoveWebView];
            return false;
        }
        if([location rangeOfString:@"platform.ginsberg.io"].location == NSNotFound)
        {
            return false;
        }
    }
    
    return true;
}


-(void)webViewDidStartLoad:(UIWebView *)webView
{
    [self SetBusy:true];
}


- (void)webView:(UIWebView *)webView didFailLoadWithError:(NSError *)error
{
    [self RemoveWebView];
    if(skipError)
    {
        skipError = false;
    }
    else
    {
        [self CommentError:@"Internet Error"];
    }
}


-(void)webViewDidFinishLoad:(UIWebView *)webView
{
    NSLog(@"Loaded %@",webView.request.URL.absoluteString);
    
    //Get page url
    NSString* url = webView.request.URL.absoluteString.lowercaseString;
    NSString* head = [webView stringByEvaluatingJavaScriptFromString:@"document.head.innerHTML"];
    
    if(  [url rangeOfString:@"account/signup"].location != NSNotFound
        ||[url rangeOfString:@"account/signin"].location != NSNotFound
        ||[url rangeOfString:@"account/myconnections"].location != NSNotFound
        ||[url rangeOfString:@"dashboard.ginsberg.io/#/connections"].location != NSNotFound
        ||[url rangeOfString:@"account/forgottenpassword"].location != NSNotFound
        ||[url rangeOfString:@"authorisation/auth?"].location != NSNotFound
        ||[url rangeOfString:@"runkeeper"].location != NSNotFound
        ||[url rangeOfString:@"facebook.com"].location != NSNotFound
        ||[url rangeOfString:@"twitter.com"].location != NSNotFound
        ||[url rangeOfString:@"accounts.google.com"].location != NSNotFound
        ||[url rangeOfString:@"fitbit.com"].location != NSNotFound
        ||[url rangeOfString:@"jawbone.com"].location != NSNotFound
        ||[url rangeOfString:@"mapmyfitness.com"].location != NSNotFound
        ||[url rangeOfString:@"strava.com"].location != NSNotFound
        ||[url rangeOfString:@"moves-app.com"].location != NSNotFound
        ||[url rangeOfString:@"withings.com"].location != NSNotFound
       )
    {
        [self SetBusy:false];
    }
    else
      if(  ([url rangeOfString:@"grantaccess"].location != NSNotFound)
         ||([url rangeOfString:@"authorisation/auth?"].location != NSNotFound))
      {
          [self Comment:@"Getting token..."];
          
          [webView stringByEvaluatingJavaScriptFromString:@"document.open();document.close()"];
          
          //Get auth code
          NSRange range = [head rangeOfString:@"<title>"];
          NSRange rangeEnd = [head rangeOfString:@"</title>"];
          range.location += range.length;
          range.length = rangeEnd.location-range.location;
          auth = [head substringWithRange:range];
          
          //Setup POST
          NSMutableURLRequest* request = [[NSMutableURLRequest alloc] initWithURL:[NSURL URLWithString:ACCESS_TOKEN_URL]];
          NSURLSession* session = [NSURLSession sharedSession];
          request.HTTPMethod = @"POST";
          
          NSString* params = [NSString stringWithFormat:@"code=%@&client_id=%@&client_secret=%@&grant_type=authorization_code",auth,clientID,clientSecret];
          [self Comment:params];
          
          //NSError* err;
          request.HTTPBody = [params dataUsingEncoding:NSUTF8StringEncoding allowLossyConversion:false];
          [request addValue:@"application/x-www-form-urlencoded" forHTTPHeaderField:@"Content-Type"];
          [request addValue:@"application/json" forHTTPHeaderField:@"Accept"];
          
          NSURLSessionDataTask* task = [session dataTaskWithRequest:request completionHandler:^(NSData *data, NSURLResponse *response, NSError *error)
          {
                  if(error != nil)
                  {
                      [self Comment:error.localizedDescription];
                      [[[UIAlertView alloc] initWithTitle:@"Connection Problem" message:@"Please check internet connection." delegate:self cancelButtonTitle:@"OK" otherButtonTitles:nil, nil] show];
                  }
                  NSString* strData = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
                  [self Comment:strData];
              
                  NSError* err;
                  NSDictionary* json = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingMutableLeaves error:&err];
              
                  if(err != nil)
                  {
                      [[[UIAlertView alloc] initWithTitle:@"Connection Problem" message:@"Please check internet connection." delegate:self cancelButtonTitle:@"OK" otherButtonTitles:nil, nil] show];
                      [self Comment:err.localizedDescription];
                  }
                  else
                  {
                      token = (NSString*)json[@"access_token"];
                                                
                      NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
                      [defaults setValue:token forKey: [NSString stringWithFormat:@"%@%@", [NSBundle mainBundle].bundleIdentifier, TOKEN_STORE_KEY]];
                      [defaults synchronize];
                      
                      [self GainedAccess];
                  }
              
              }];
          
          [task resume];
      }
}


-(void)GainedAccess
{
    dispatch_async(dispatch_get_main_queue(), ^(void)
    {
        [self GetProfile];
        [self GetTags];
        [self GetDefaults];
        [self GetCountries];
        [self GetTodaysEvent];
        [self GetTodaysSubjective];
        
        if(initialChecks == 0)
        {
            [callbacks GainedAccess];
        }
    });
}


//
// Token methods
//

+(BOOL)HaveToken
{
    NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
    NSObject* obj = [defaults objectForKey: [[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:TOKEN_STORE_KEY]];
    
    if (obj != nil)
    {
        return true;
    }
    
    return false;
}


+(NSString*)GetToken
{
    NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
    return [defaults stringForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:TOKEN_STORE_KEY]];
}


-(void)ClearToken
{
    _userID = nil;
    _userFirstName = nil;
    _userLastName = nil;
    _userPhoneNumber = nil;
    _userCountry = nil;
    _todaysEventID = 0;
    _todaysEvent = nil;
    _lastSaveDateSubjective = nil;
    
    [self ClearMemoryStorage];
    
    NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
    
    [defaults removeObjectForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:TOKEN_STORE_KEY]];
    
    [defaults removeObjectForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"USERID"]];
    [defaults removeObjectForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"USERFIRSTNAME"]];
    [defaults removeObjectForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"USERLASTNAME"]];
    [defaults removeObjectForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"USERPHONENUMBER"]];
    [defaults removeObjectForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"USERCOUNTRY"]];
    [defaults removeObjectForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"USERTAGLENGTH"]];
    [defaults removeObjectForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"USERTAGORDEREDLENGTH"]];
    [defaults removeObjectForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"TAGEMOTIONSLENGTH"]];
    [defaults removeObjectForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"USERQUESLENGTH"]];
    [defaults removeObjectForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"USERQUESIDLENGTH"]];
    [defaults removeObjectForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"LASTSUBJECTIVEDATE"]];
    [defaults removeObjectForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"USERQUESTODAYLENGTH"]];
    [defaults removeObjectForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"USERQUESTODAYLENGTH"]];    
    
    [defaults synchronize];
    
    //Clear webview cache
    [[NSURLCache sharedURLCache] removeAllCachedResponses];
    for(NSHTTPCookie *cookie in [[NSHTTPCookieStorage sharedHTTPCookieStorage] cookies])
    {
        [[NSHTTPCookieStorage sharedHTTPCookieStorage] deleteCookie:cookie];
    }
}


// 
// Interface
//

-(void)SetBusy:(BOOL)truth
{
    busy = truth;
    
    dispatch_async(dispatch_get_main_queue(), ^(void)
    {
        if(callbacks != nil)
        {
            if([callbacks respondsToSelector:@selector(SetBusy:)])
                [callbacks SetBusy:truth];
        }
        if(vActivityBacking != nil)
        {
            vActivityBacking.hidden = !truth;
        }
        if(vActivity != nil)
        {
            vActivity.hidden = !truth;
        }
    });
}


-(void)RemoveWebView
{
    if(webViewParent != nil)
    {
        [webViewParent removeFromSuperview];
        webViewParent = nil;
    }
    
    [self SetBusy:false];
}


-(void)DismissView
{
    [[self CurrentViewController] dismissViewControllerAnimated:true completion:nil];
}


//
// OAuth Data
//

-(void)DataReceived:(NSString*)endPoint withData:(NSDictionary*)data andString:(NSString*)string
{
    dispatch_async(dispatch_get_main_queue(), ^(void)
    {
        if(callbacks != nil)
        {
            if([callbacks respondsToSelector:@selector(DataReceived:withData:andString:)])
                [callbacks DataReceived:endPoint withData:data andString:string];
        }
    });
}

     
-(void)GetData:(NSString*)startPoint endPoint:(NSString*)endPoint range:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID
{
    //Setup GET
    NSString* fEndPoint = endPoint;
    
    if([range isEqualToString:@"All"])
    {
        
    }
    else
    {
        if([range isEqualToString:@"ID"])
        {
            fEndPoint = [NSString stringWithFormat:@"%@/%ld", fEndPoint, ID];
        }
        else
        {
            fEndPoint = [fEndPoint stringByAppendingString:@"?"];
            
            if ([range isEqualToString:@"From"] || [range isEqualToString:@"FromTo"])
            {
                fEndPoint = [NSString stringWithFormat:@"%@start=%@",fEndPoint,[typeFrom isEqualToString:@"Date"]? dateFrom: typeFrom];
            }
            
            if ([range isEqualToString:@"To"] || [range isEqualToString:@"FromTo"])
            {
                fEndPoint = [NSString stringWithFormat:@"%@%@%@",fEndPoint,[range isEqualToString:@"FromTo"]? @"&end=": @"end=",[typeTo isEqualToString:@"Date"]? dateTo: typeTo];
            }
        }
    }
    
    fEndPoint = [fEndPoint stringByReplacingOccurrencesOfString:@"+" withString:@"%2B"];
    NSString* fullUrl = [NSString stringWithFormat:@"%@%@",startPoint,fEndPoint];
    NSLog(@"Encoding %@",fullUrl);
    
    NSMutableURLRequest* request = [[NSMutableURLRequest alloc] initWithURL:[NSURL URLWithString:fullUrl]];
    NSURLSession* session = [NSURLSession sharedSession];
    
    request.HTTPMethod = @"GET";
    [request addValue:@"application/json" forHTTPHeaderField:@"Accept"];
    [request addValue:[NSString stringWithFormat:@"Bearer %@",token] forHTTPHeaderField:@"Authorization"];
    
    NSLog(@"Desc %@",request.URL.description);
    
    NSURLSessionDataTask* task = [session dataTaskWithRequest:request completionHandler:^(NSData* data, NSURLResponse* response, NSError* error)
    {
        NSError* err;
        NSString* strData = [[NSString alloc] initWithData:data encoding: NSUTF8StringEncoding];
        
        //Dont get any data if not logged in
        if(![GAPI HaveToken]) return;
        
        if(error != nil)
        {
            NSLog(@"Error: %@",error);
            
            [self CommentError:@"Problems getting data"];
            return;
        }
        
        if ([response isKindOfClass:[NSHTTPURLResponse class]])
        {
            long statusCode = ((NSHTTPURLResponse*)response).statusCode;
            
            if (statusCode >= 400)
            {
                [self SetBusy:false];
            
                NSLog(@"Error: %@",error);
                [self CommentError:@"Problems getting data"];
                
                return;
            }
            
            NSDictionary* json = (NSDictionary*)[NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingMutableLeaves error:&err];
            
            int checks = initialChecks;
            
            //Check for particulars
            if([fEndPoint isEqualToString:@"/v1/me"] && data.length > 5)
            {
                //Tags
                NSArray* tags = (NSArray*)json[@"tags_used"];
                _userTags = [[NSMutableArray alloc] init];
                
                for(int i = 0; i < tags.count; ++i)
                {
                    NSString* tag = (NSString*)tags[i];
                    [_userTags addObject:tag];
                }
                    
                //Questions
                NSArray* questions = (NSArray*)json[@"wellbeing_metrics"];
                _userQuestionsID = [[NSMutableArray alloc] init];
                _userQuestions = [[NSMutableArray alloc] init];
                    
                for(int i = 0; i < questions.count; ++i)
                {
                    NSDictionary* subJson = (NSDictionary*)questions[i];
                    [_userQuestionsID addObject:subJson[@"id"]];
                    [_userQuestions addObject:subJson[@"question"]];
                }
                
                //ID
                _userID = (NSString*)json[@"id"];
                _userFirstName = (NSString*)json[@"first_name"];
                _userLastName = (NSString*)json[@"last_name"];
                _userPhoneNumber = (NSString*)json[@"phone_number"];
                _userCountry = (NSString*)json[@"country"];
                
                [self SaveCache];
                
                initialChecks = MAX(0,initialChecks-1);
            }
            else
            if([fEndPoint isEqualToString:@"/v1/tags"] && data.length > 5)
            {
                //Tags
                NSArray* tags = (NSArray*)json[@"tags"];
                _userTagsOrdered = [[NSMutableArray alloc] init];
                
                for(int i = 0; i < tags.count; ++i)
                {
                    NSDictionary* tag = (NSDictionary*)tags[i];
                    NSString* name = (NSString*)tag[@"tag"];
                    [_userTagsOrdered addObject:name];
                }
                
                [self SaveCache];
                    
                initialChecks = MAX(0,initialChecks-1);
            }
            else
            if([fEndPoint isEqualToString:@"/defaults.json"] && data.length > 5)
            {
                //Tags
                NSDictionary* tags = (NSDictionary*)json[@"tags"];
                NSArray* emotions = (NSArray*)tags[@"emotions"];
                NSArray* scots = (NSArray*)tags[@"scots"];
                _tagsEmotions = [[NSMutableArray alloc] init];
                _tagsScots = [[NSMutableArray alloc] init];
                
                for(int i = 0; i < emotions.count; ++i)
                {
                    NSString* name = (NSString*)emotions[i];
                    [_tagsEmotions addObject:name];
                }
                    
                for(int i = 0; i < scots.count; ++i)
                {
                    NSString* name = (NSString*)scots[i];
                    [_tagsScots addObject:name];
                }
                
                [self SaveCache];
                
                initialChecks = MAX(0,initialChecks-1);
            }
            else
            if([fEndPoint isEqualToString:@"/account/signupmetadata"] && data.length > 5)
            {
                NSArray* tags = (NSArray*)json[@"countries"];
                _countries = [[NSMutableArray alloc] init];
                
                for(int i = 0; i < tags.count; ++i)
                {
                    NSDictionary* country = (NSDictionary*)tags[i];
                    NSString* name = (NSString*)country[@"name"];
                    [_countries addObject:name];
                }
                
                [self SaveCache];
                    
                initialChecks = MAX(0,initialChecks-1);
            }
            else
            if(([fEndPoint rangeOfString:@"/v1/o/events"].location == 0) && !gotEventToday)
            {
                gotEventToday = true;
                    
                if(data.length > 5)
                {
                    NSDictionary* subJson  = ((NSArray*)json)[0];
                    _todaysEvent = (NSString*)subJson[@"entry"];
                    NSNumber* eid = (NSNumber*)subJson[@"id"];
                    _todaysEventID = [eid integerValue];
                }
                else
                {
                    _todaysEvent = @"";
                    _todaysEventID = -1;
                }
                
                [self SaveCache];
                
                initialChecks = MAX(0,initialChecks-1);
            }
            else
            if(([fEndPoint rangeOfString:@"/v1/wellbeing"].location == 0) && !gotSubjectiveToday)
            {
                gotSubjectiveToday = true;
                
                if(data.length > 5)
                {
                    [self SetDoneTodaySubjective:false];
                    
                    NSArray* questions = (NSArray*)json;
                    _userQuestionsToday = [[NSMutableArray alloc] init];
                    
                    for(int i = 0; i < questions.count; ++i)
                    {
                        NSDictionary* subJson = (NSDictionary*)questions[i];
                        [_userQuestionsToday addObject:subJson[@"value"]];
                    }
                }
                else
                {
                    [self ClearDoneTodaySubjective:false];
                    _userQuestionsToday = nil;
                }
                
                [self SaveCache];
                
                initialChecks = MAX(0,initialChecks-1);
            }
                
            //Send data back
            [self DataReceived:endPoint withData:json andString:strData];
            
            if(checks != initialChecks && initialChecks == 0)
            {
                dispatch_async(dispatch_get_main_queue(), ^(void)
                {
                    //Dismiss view
                    dispatch_async(dispatch_get_main_queue(), ^(void)
                    {
                        [webViewParent removeFromSuperview];
                    });
                    
                    [callbacks GainedAccess];
                });
            }
        }
    }];
    
    [task resume];
}


-(void)GetData:(NSString*)endPoint range:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID
{
    NSLog(endPoint);
    [self GetData:[HTTPAPI copy] endPoint:endPoint range:range typeFrom:typeFrom dateFrom:dateFrom typeTo:typeTo dateTo:dateTo ID:ID];
}


-(NSString*) AnyToString:(id)obj
{
    if([obj isKindOfClass:[NSString class]])
    {
        NSString* result = (NSString*)obj;
        
        if(result == nil || [result isEqualToString:@""])
        {
            return @"";
        }
        
        if([result hasPrefix:@"["] || [result hasPrefix:@"{"])
        {
            return result;
        }
        else
        {
            return [NSString stringWithFormat:@"\"%@\"", result];
        }
    }
    else
    {
        if([obj isKindOfClass:[NSNumber class]])
        {
            NSNumber* n = (NSNumber*)obj;
            if(strcmp([n objCType], @encode(int)) == 0)
            {
                int result = [n intValue];
            
                if(result < 0)
                {
                    return @"";
                }
            
                return [NSString stringWithFormat:@"%d", (int)result];
            }
            else
            if(strcmp([n objCType], @encode(long)) == 0)
            {
                long result = [n longValue];
                    
                if(result < 0)
                {
                    return @"";
                }
                    
                return [NSString stringWithFormat:@"%ld", (long)result];
            }
            else
            if(strcmp([n objCType], @encode(float)) == 0)
            {
                float result = [n floatValue];
                
                if(result < 0.0)
                {
                    return @"";
                }
                
                return [NSString stringWithFormat:@"%f", result];
            }
            else
            if(strcmp([n objCType], @encode(double)) == 0)
            {
                double result = [n doubleValue];
                    
                if(result < 0.0)
                {
                    return @"";
                }
                    
                return [NSString stringWithFormat:@"%f", result];
            }
        }
    }
    
    return @"";
}


- (void)alertView:(UIAlertView *)alertView didDismissWithButtonIndex:(NSInteger)buttonIndex
{
    // the user clicked OK
    if (buttonIndex == 0)
    {
        CurrentPosting = CANCEL;
    }
    else
    {
        CurrentPosting = RETRY;
    }
}


-(void) PostData:(NSString*)startPoint endPoint:(NSString*)endPoint paramsString:(NSString*)paramsString
{
    //Reset if starting a fresh
    if(CurrentPosting == CANCEL)
    {
        CurrentPosting = INACTIVE;
    }
    
    dispatch_async(dispatch_get_global_queue( DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^(void)
    {
        while(CurrentPosting != INACTIVE)
        {
            [NSThread sleepForTimeInterval:0.1];
            if(CurrentPosting == CANCEL) return;	
        }
        
        CurrentPosting = CHECKING;
        [self SetBusy:true];
    
        //Setup POST
        NSMutableURLRequest* request = [[NSMutableURLRequest alloc] initWithURL:[NSURL URLWithString:[NSString stringWithFormat:@"%@%@",startPoint,endPoint]]];
        NSURLSession* session = [NSURLSession sharedSession];
        request.HTTPMethod = @"POST";
    
        request.HTTPBody = [paramsString dataUsingEncoding:NSUTF8StringEncoding allowLossyConversion: false];
        [request addValue:@"application/json" forHTTPHeaderField: @"Content-Type"];
        if(token != nil && ![token isEqualToString:@""])
            [request addValue:[NSString stringWithFormat:@"Bearer %@", token] forHTTPHeaderField: @"Authorization"];
        [request addValue:@"application/json" forHTTPHeaderField: @"Accept"];
    
        //Thread thread = new
        NSURLSessionDataTask* task = [session dataTaskWithRequest:request completionHandler:^(NSData* data, NSURLResponse* response, NSError* error)
        {
            NSError* err;
            NSString* strData = [[NSString alloc] initWithData:data encoding: NSUTF8StringEncoding];
        
            if(error != nil)
            {
                [self Comment:[NSString stringWithFormat:@"Error: %@",error]];
        
                if(showReconnect)
                {
                    dispatch_async(dispatch_get_main_queue(), ^{
                        [[[UIAlertView alloc] initWithTitle:@"Data Not Saved" message:@"Connection error. Please check internet connection before continuing." delegate:self cancelButtonTitle:@"Cancel" otherButtonTitles:@"Retry", nil] show];
                    });
                
                    while(CurrentPosting == CHECKING)
                    {
                        [NSThread sleepForTimeInterval:0.1];
                    }
                
                    if(CurrentPosting == RETRY)
                    {
                        [self PostData:endPoint paramsString:paramsString];
                        CurrentPosting = INACTIVE;
                    }
                }
                
                [self SetBusy:false];
                return;
            }
        
            if ([response isKindOfClass:[NSHTTPURLResponse class]])
            {
                long statusCode = ((NSHTTPURLResponse*)response).statusCode;
            
                if (statusCode >= 400)
                {
                    //Special cases
                    if(data != nil && data.length > 2 && [endPoint rangeOfString:@"/account/externalsignup"].location == 0)
                    {
                        [self CommentError:strData];
                    }
                    
                    [self SetBusy:false];
                    return;
                }
            
                NSDictionary* json = (NSDictionary*)[NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingMutableLeaves error:&err];
            
                if(err != nil)
                {
                    NSLog(@"%@",strData);
                    //Check for particulars
                    if([endPoint rangeOfString:@"/v1/me"].location == 0)
                    {
                        //Get todays event if changed
                        [self Comment:@"Posted profile success!"];
                        [self GetProfile];
                    }
                    else
                    {
                        [self CommentError:@"Post Failed!"];
                    }
                }
                else
                {
                    NSLog(@"%@",strData);
                    [self Comment:@"Post Success!"];
                    
                    //Check for particulars
                    if([endPoint rangeOfString:@"/v1/o/events"].location == 0)
                    {
                        //Get todays event if changed
                        [self GetTodaysEvent];
                    }
                    if([endPoint rangeOfString:@"/account/externalsignup"].location == 0)
                    {
                        NSNumber* status = json[@"status"];
                        
                        if(status.longValue >= 400)
                        {
                            [self CommentError:json[@"message"]];
                        }
                        else
                        {
                            [self Comment:@"Signup success!"];
                        }
                    }
                }
            }
        
            [self SetBusy:false];
            CurrentPosting = INACTIVE;
        }];
    
        [task resume];
        
    });
}


-(void) PostData:(NSString*)endPoint paramsString:(NSString*)paramsString
{
    [self PostData:[HTTPAPI copy] endPoint:endPoint paramsString:paramsString];
}


-(void) PostData:(NSString*)startPoint endPoint:(NSString*)endPoint count:(long)count params:(va_list)args
{
    NSString* paramsString = @"{";
    
    for(int i = 0; i+1 < count; i+=2)
    {
        NSString* result1 = [self AnyToString:va_arg(args, id)];
        NSString* result2 = [self AnyToString:va_arg(args, id)];
        
        if(![result2 isEqualToString:@""])
        {
            paramsString = [NSString stringWithFormat:@"%@%@:%@,", paramsString, result1, result2];
        }
    }
    
    //Remove last ','
    if([paramsString hasSuffix:@","])
    {
        paramsString = [paramsString substringToIndex:paramsString.length-1];
    }
    
    [self PostData:startPoint endPoint:endPoint paramsString:[paramsString stringByAppendingString:@"}"]];
}


-(void) PostData:(NSString*)startPoint endPoint:(NSString*)endPoint params:(NSInteger)count,...
{
    va_list args;
    va_start(args, count);
    
    [self PostData:startPoint endPoint:endPoint count:count params:args];
    
    va_end(args);
}


-(void) PostData:(NSString*)endPoint params:(NSInteger)count,...
{
    va_list args;
    va_start(args, count);
    
    [self PostData:[HTTPAPI copy] endPoint:endPoint count:count params:args];
    
    va_end(args);
}


//Delete data
-(void) DeleteData:(NSString*)endPoint ID:(int)ID
{
    [self SetBusy:true];
    
    if (token == nil)
    {
        [self Comment:@"Dont have token!"];
        return;
    }
    
    NSMutableURLRequest* request = [[NSMutableURLRequest alloc] initWithURL:[NSURL URLWithString:[NSString stringWithFormat:@"%@%@/%i",HTTPAPI,endPoint,ID]]];
    NSURLSession* session = [NSURLSession sharedSession];
    
    request.HTTPMethod = @"DELETE";
    [request addValue:@"application/json" forHTTPHeaderField:@"Accept"];
    [request addValue:[NSString stringWithFormat:@"Bearer %@",token] forHTTPHeaderField:@"Authorization"];

    NSURLSessionDataTask* task = [session dataTaskWithRequest:request completionHandler:^(NSData* data, NSURLResponse* response, NSError* error)
    {
        //Dont get any data if not logged in
        if(![GAPI HaveToken]) return;
                                      
        if(error != nil)
        {
            [self CommentError:@"Connection problems deleting data"];
            return;
        }
        
        if ([response isKindOfClass:[NSHTTPURLResponse class]])
        {
            long statusCode = ((NSHTTPURLResponse*)response).statusCode;
            
            if (statusCode >= 400)
            {
                [self CommentError:@"Server problems deleting data"];
                return;
            }
            else
            {
                [self Comment:@"Delete success"];
                [self SetBusy:false];
            }
        }
    }];
    
    [task resume];
}
        

//
// Get particulars
//


-(int32_t) GetUserCountryID
{
    if(_countries != nil && _countries.count > 200 && _userCountry != nil)
    {
        for(int i = 0; i < _countries.count; ++ i)
        {
            if([_countries[i] isEqualToString:_userCountry])
            {
                return i+1;
            }
        }
    }
    
    return 1;
}


-(void)GetTodaysEvent
{
    gotEventToday = false;
    [self GetEvent:@"FromBelow" typeFrom:@"Date" dateFrom:[GAPI GetDateTimeStart:0] typeTo:@"" dateTo:@"" ID:-1];
}


-(void)GetTodaysSubjective
{
    gotSubjectiveToday = false;
    [self GetWellbeing:@"FromBelow" typeFrom:@"Date" dateFrom:[GAPI GetDateTimeStart:0] typeTo:@"" dateTo:@"" ID:-1];
}


-(void)GetTags
{
    [self GetTag:@"All" typeFrom:@"" dateFrom:@"" typeTo:@"" dateTo:@""];
}


-(void)GetDefaults
{
    @try {
        [self GetData:[HTTPWWW copy] endPoint:@"/defaults.json" range:@"All" typeFrom:@"" dateFrom:@"" typeTo:@"" dateTo:@"" ID:0];
    }
    @catch (NSException *exception) {
        NSLog(@"%@", exception.reason);
    }
    @finally {
    }
}


-(void)GetCountries
{
    @try {
        [self GetData:[HTTPPLAT copy] endPoint:@"/account/signupmetadata" range:@"All" typeFrom:@"" dateFrom:@"" typeTo:@"" dateTo:@"" ID:0];
    }
    @catch (NSException *exception) {
        NSLog(@"%@", exception.reason);
    }
    @finally {
    }
}


-(void) GetCorrelations
{
    @try {
        [self GetData:@"/v1/correlations" range:nil typeFrom:nil dateFrom:nil typeTo:nil dateTo:nil ID:-1];
    }
    @catch (NSException *exception) {
        NSLog(@"%@", exception.reason);
    }
    @finally {
    }
}


-(void) GetNotifications:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID
{
    [self GetData:@"/v1/notifications" range:range typeFrom:typeFrom dateFrom:dateFrom typeTo:typeTo dateTo:dateTo ID:ID];
}


-(void) GetTag:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo
{
    @try {
        [self GetData:@"/v1/tags" range:range typeFrom:typeFrom dateFrom:dateFrom typeTo:typeTo dateTo:dateTo ID:-1];
    }
    @catch (NSException *exception) {
        NSLog(@"%@", exception.reason);
    }
    @finally {
    }
}


-(void) GetDailySummary:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo
{
    @try {
        [self GetData:@"/v1/me/dailysummary" range:range typeFrom:typeFrom dateFrom:dateFrom typeTo:typeTo dateTo:dateTo ID:-1];
    }
    @catch (NSException *exception) {
        NSLog(@"%@", exception.reason);
    }
    @finally {
    }
}


-(void)GetProfile
{
    @try {
        [self GetData:@"/v1/me" range:@"All" typeFrom:@"" dateFrom:@"" typeTo:@"" dateTo:@"" ID:0];
    }
    @catch (NSException *exception) {
        NSLog(@"%@", exception.reason);
    }
    @finally {
    }
}
-(void)PostProfile:(NSString*)firstName lastName:(NSString*)lastName phoneNumber:(NSString*)phoneNumber country:(int)country
{
    @try {
        [self PostData:@"/v1/me" params:8,@"first_name",firstName,@"last_name",lastName,@"country_id",[NSNumber numberWithInt:country],@"phone_number",phoneNumber];
    }
    @catch (NSException *exception) {
        NSLog(@"%@", exception.reason);
    }
    @finally {
    }
}


-(void) PostWellbeing:(NSString*)timeStamp value:(int)value wbques:(NSString*)wbques wbtype:(int)wbtype
{
    [self PostData:@"/v1/wellbeing" params:8,@"measure_id",[NSNumber numberWithInt:wbtype],@"value",[NSNumber numberWithInt:value],
     @"measure",wbques,@"timestamp",timeStamp];
}
-(void) GetWellbeing:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID
{
    @try {
        [self GetData:@"/v1/wellbeing" range:range typeFrom:typeFrom dateFrom:dateFrom typeTo:typeTo dateTo:dateTo ID:ID];
    }
    @catch (NSException *exception) {
        NSLog(@"%@", exception.reason);
    }
    @finally {
    }
}
-(void) DeleteWellbeing:(int)ID { [self DeleteData:@"/v1/wellbeing" ID:ID];}


-(void) PostEvent:(NSString*)timeStamp event:(NSString*)event ID:(long)ID
{
    [self PostData:@"/v1/o/events" params:8,@"entry",event,@"timestamp",timeStamp,@"source",[[NSBundle mainBundle] bundleIdentifier],@"id",[NSNumber numberWithLong:ID]];
    //PostData(,params:"source",NSBundle.mainBundle().bundleIdentifier,"entry",entry,"timestamp",timeStamp);
}
-(void) GetEvent:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID
{
    @try {
        [self GetData:@"/v1/o/events" range:range typeFrom:typeFrom dateFrom:dateFrom typeTo:typeTo dateTo:dateTo ID:ID];
    }
    @catch (NSException *exception) {
        NSLog(@"%@", exception.reason);
    }
    @finally {
    }
}
-(void) DeleteEvent:(int)ID { [self DeleteData:@"/v1/o/events" ID:ID];}


-(void) PostExercise:(NSString*)timeStamp activity:(NSString*)activity distance:(double)distance
{
    [self PostData:@"/v1/o/exercise" params:8,@"activity_type",activity,@"distance",[NSNumber numberWithDouble:distance],
     @"timestamp",timeStamp, @"source",[[NSBundle mainBundle] bundleIdentifier]];
}
-(void) GetExercise:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID
{
    @try {
        [self GetData:@"/v1/o/exercise" range:range typeFrom:typeFrom dateFrom:dateFrom typeTo:typeTo dateTo:dateTo ID:ID];
    }
    @catch (NSException *exception) {
        NSLog(@"%@", exception.reason);
    }
    @finally {
    }
}
-(void) DeleteExercise:(int)ID { [self DeleteData:@"/v1/o/exercise" ID:ID];}


-(void)PostAlcohol:(NSString*)timeStamp units:(double)units
{
    [self PostData:@"/v1/o/alcohol" params:4,@"units",[NSNumber numberWithDouble:units],@"timestamp",timeStamp];
}
-(void)GetAlcohol:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID
{
    [self GetData:@"/v1/o/alcohol" range:range typeFrom:typeFrom dateFrom:dateFrom typeTo:typeTo dateTo:dateTo ID:ID];
}
-(void)DeleteAlcohol:(int)ID { [self DeleteData:@"/v1/o/alcohol" ID:ID];}


-(void) PostSleep:(NSString*)timeStamp timesAwoken:(int)timesAwoken awake:(double)awake lightSleep:(double)lightSleep remSleep:(double)remSleep deepSleep:(double)deepSleep totalSleep:(double)totalSleep quality:(int)quality
{
    [self PostData:@"/v1/o/sleep" params:18,@"total_sleep",[NSNumber numberWithDouble:totalSleep],@"deep_sleep",[NSNumber numberWithDouble:deepSleep],@"rem_sleep",[NSNumber numberWithDouble:remSleep],@"light_sleep",[NSNumber numberWithDouble:lightSleep],@"awake",[NSNumber numberWithDouble:awake],@"times_awoken",[NSNumber numberWithInt:timesAwoken],@"timestamp",timeStamp, @"source",[[NSBundle mainBundle] bundleIdentifier],@"quality",[NSString stringWithFormat:@"{\"value\":%d}",quality]];
}
-(void) GetSleep:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID
{
    @try {
        [self GetData:@"/v1/o/sleep" range:range typeFrom:typeFrom dateFrom:dateFrom typeTo:typeTo dateTo:dateTo ID:ID];
    }
    @catch (NSException *exception) {
        NSLog(@"%@", exception.reason);
    }
    @finally {
    }
}
-(void) DeleteSleep:(int)ID { [self DeleteData:@"/v1/o/sleep" ID:ID];}

-(void) PostActivity:(NSString*)timeStamp start:(NSString*)start end:(NSString*)end dist:(double)dist cal:(double)cal steps:(int)steps
{
    [self PostData:@"/v1/o/activity" params:16, @"step_count", [NSNumber numberWithInt:steps], @"start", start, @"distance", [NSNumber numberWithDouble:dist], @"calories", [NSNumber numberWithDouble:cal], @"end", end, @"activity_type", @"running", @"source", [[NSBundle mainBundle] bundleIdentifier], @"timestamp",timeStamp];
}
-(void) GetActivity:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID
{
    [self GetData:@"/v1/o/activity" range:range typeFrom:typeFrom dateFrom:dateFrom typeTo:typeTo dateTo:dateTo ID:ID];
}
-(void) DeleteActivity:(int)ID { [self DeleteData:@"/v1/o/activity" ID:ID];}


-(void) PostNutrition:(NSString*)timeStamp calories:(double)calories carbohydrates:(double)carbohydrates fat:(double)fat fiber:(double)fiber protein:(double)protein sugar:(double)sugar
{
    [self PostData:@"/v1/o/nutrition" params:14, @"sugar", [NSNumber numberWithDouble:sugar], @"protein", [NSNumber numberWithDouble:protein], @"fiber", [NSNumber numberWithDouble:fiber], @"fat", [NSNumber numberWithDouble:fat], @"carbohydrates", [NSNumber numberWithDouble:carbohydrates], @"calories", [NSNumber numberWithDouble:calories], @"timestamp", timeStamp];
}
-(void) GetNutrition:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID
{
    [self GetData:@"/v1/o/nutrition" range:range typeFrom:typeFrom dateFrom:dateFrom typeTo:typeTo dateTo:dateTo ID:ID];
}
-(void) DeleteNutrition:(int)ID { [self DeleteData:@"/v1/o/nutrition" ID:ID];}


-(void) PostBody:(NSString*)timeStamp weight:(double)weight fat:(double)fat
{
    [self PostData:@"/v1/o/body" params:6, @"fat", [NSNumber numberWithDouble:fat], @"weight", [NSNumber numberWithDouble:weight], @"timestamp", timeStamp];
}
-(void) GetBody:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID
{
    [self GetData:@"/v1/o/body" range:range typeFrom:typeFrom dateFrom:dateFrom typeTo:typeTo dateTo:dateTo ID:ID];
}
-(void) DeleteBody:(int)ID { [self DeleteData:@"/v1/o/body" ID:ID];}


-(void) PostCaffeine:(NSString*)timeStamp amount:(double)ef
{
    [self PostData:@"/v1/o/caffeine" params:4, @"value", [NSNumber numberWithDouble:ef], @"timestamp", timeStamp];
}
-(void) GetCaffeine:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID
{
    [self GetData:@"/v1/o/caffeine" range:range typeFrom:typeFrom dateFrom:dateFrom typeTo:typeTo dateTo:dateTo ID:ID];
}
-(void) DeleteCaffeine:(int)ID { [self DeleteData:@"/v1/o/sleep" ID:ID];}


-(void) PostSmoking:(NSString*)timeStamp amount:(int)ei
{
    [self PostData:@"/v1/o/smoking" params:4, @"quantity", [NSNumber numberWithInt:ei], @"timestamp", timeStamp];
}
-(void) GetSmoking:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID
{
    [self GetData:@"/v1/o/smoking" range:range typeFrom:typeFrom dateFrom:dateFrom typeTo:typeTo dateTo:dateTo ID:ID];
}
-(void) DeleteSmoking:(int)ID { [self DeleteData:@"/v1/o/sleep" ID:ID];}


-(void) PostMood:(NSString*)timeStamp value:(int)ei
{
    [self PostData:@"/v1/s/mood" params:4, @"value", [NSNumber numberWithInt:ei], @"timestamp", timeStamp];
}
-(void) GetMood:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID
{
    [self GetData:@"/v1/s/mood" range:range typeFrom:typeFrom dateFrom:dateFrom typeTo:typeTo dateTo:dateTo ID:ID];
}
-(void) DeleteMood:(int)ID { [self DeleteData:@"/v1/s/mood" ID:ID];}


-(void) PostHappy:(NSString*)timeStamp value:(int)ei
{
    [self PostData:@"/v1/s/happy" params:4, @"value", [NSNumber numberWithInt:ei], @"timestamp", timeStamp];
}
-(void) GetHappy:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID
{
    [self GetData:@"/v1/s/happy" range:range typeFrom:typeFrom dateFrom:dateFrom typeTo:typeTo dateTo:dateTo ID:ID];
}
-(void) DeleteHappy:(int)ID { [self DeleteData:@"/v1/s/happy" ID:ID];}


-(void) PostSad:(NSString*)timeStamp value:(int)ei
{
    [self PostData:@"/v1/s/emotion" params:6, @"emotion", [NSNumber numberWithInt:2], @"value", [NSNumber numberWithInt:ei], @"timestamp", timeStamp];
}
-(void) GetSad:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID
{
    [self GetData:@"/v1/s/sad" range:range typeFrom:typeFrom dateFrom:dateFrom typeTo:typeTo dateTo:dateTo ID:ID];
}
-(void) DeleteSad:(int)ID { [self DeleteData:@"/v1/s/sad" ID:ID];}


-(void) PostUneasy:(NSString*)timeStamp value:(int)ei
{
    [self PostData:@"/v1/s/uneasy" params:4, @"value", [NSNumber numberWithInt:ei], @"timestamp", timeStamp];
}
-(void) GetUneasy:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID
{
    [self GetData:@"/v1/s/uneasy" range:range typeFrom:typeFrom dateFrom:dateFrom typeTo:typeTo dateTo:dateTo ID:ID];
}
-(void) DeleteUneasy:(int)ID { [self DeleteData:@"/v1/s/uneasy" ID:ID];}


-(void) PostWell:(NSString*)timeStamp value:(int)ei
{
    [self PostData:@"/v1/s/well" params:4, @"value", [NSNumber numberWithInt:ei], @"timestamp", timeStamp];
}
-(void) GetWell:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID
{
    [self GetData:@"/v1/s/well" range:range typeFrom:typeFrom dateFrom:dateFrom typeTo:typeTo dateTo:dateTo ID:ID];
}
-(void) DeleteWell:(int)ID { [self DeleteData:@"/v1/s/well" ID:ID];}


-(void) PostStepcount:(NSString*)timeStamp timeStart:(NSString*)timeStart timeEnd:(NSString*)timeEnd distance:(double)distance calories:(double)calories steps:(int)steps
{
    [self PostData:@"/v1/o/stepcount" params:16, @"step_count", [NSNumber numberWithInt:steps], @"calories", [NSNumber numberWithDouble:calories], @"distance", [NSNumber numberWithDouble:distance], @"end", timeEnd, @"start", timeStart,
             @"activity_type", @"Aggregated", @"source", [[NSBundle mainBundle] bundleIdentifier], @"timestamp", timeStamp];
}
-(void) GetStepcount:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID
{
    [self GetData:@"/v1/o/stepcount" range:range typeFrom:typeFrom dateFrom:dateFrom typeTo:typeTo dateTo:dateTo ID:ID];
}
-(void) DeleteStepcount:(int)ID { [self DeleteData:@"/v1/o/stepcount" ID:ID];}


-(void) PostSocial:(NSString*) timeStamp
{
    [self PostData:@"/v1/o/social" params:6, @"source", [[NSBundle mainBundle] bundleIdentifier], @"entry", @"entry", @"timestamp", timeStamp];
}
-(void) GetSocial:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID
{
    [self GetData:@"/v1/o/social" range:range typeFrom:typeFrom dateFrom:dateFrom typeTo:typeTo dateTo:dateTo ID:ID];
}
-(void) DeleteSocial:(int)ID { [self DeleteData:@"/v1/o/social" ID:ID];}


-(void) GetMeasures
{
    [self GetData:@"/v1/measures" range:nil typeFrom:nil dateFrom:nil typeTo:nil dateTo:nil ID:-1];
}


-(void) GetSurvey:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID
{
    [self GetData:@"/v1/survey" range:range typeFrom:typeFrom dateFrom:dateFrom typeTo:typeTo dateTo:dateTo ID:ID];
}



//
// Comments
//

-(void)Comment:(NSString*)text
{
    dispatch_async(dispatch_get_main_queue(), ^(void)
    {
        if(callbacks != nil)
        {
            if([callbacks respondsToSelector:@selector(Comment:)])
                [callbacks Comment:text];
        }
        NSLog(@"%@",text);
    });
}


-(void)CommentError:(NSString*)text
{
    dispatch_async(dispatch_get_main_queue(), ^(void)
    {
        if(callbacks != nil)
        {
            @try {
                // Try something
                
                if([callbacks respondsToSelector:@selector(SetBusy:)])
                    [callbacks SetBusy:false];
                if([callbacks respondsToSelector:@selector(NeedLogin)])
                    [callbacks NeedLogin];
                if([callbacks respondsToSelector:@selector(CommentError:)])
                    [callbacks CommentError:text];
            }
            @catch (NSException * e) {
                NSLog(@"Exception: %@", e);
            }
            @finally {
                // Added to show finally works as well
            }
        }
        NSLog(@"%@",text);
    });
}


//
// Date methods
//

+(NSString*) GetDateTime
{
    return [GAPI GetDateTime:0];
}


+(NSString*) GetDateTime:(NSInteger)daysDifference
{
    NSDateFormatter* format = [[NSDateFormatter alloc] init];
    format.dateFormat = @"yyyy'-'MM'-'dd'T'HH':'mm':'ssZZ";
    format.timeZone = [NSTimeZone defaultTimeZone];
    
    NSDate* date = [NSDate dateWithTimeIntervalSinceNow:(60*60*24*daysDifference)];
    return [format stringFromDate:date];
}


+(NSString*) GetDateTimeBasic:(NSInteger)daysDifference withTime:(NSString*)time
{
    NSDateFormatter* format = [[NSDateFormatter alloc] init];
    format.dateFormat = [NSString stringWithFormat:@"yyyy'-'MM'-'dd'T%@'ZZ",(time != nil? time: @"")];
    format.timeZone = [NSTimeZone defaultTimeZone];
    
    NSDate* date = [NSDate dateWithTimeIntervalSinceNow:(60*60*24*daysDifference)];
    return [format stringFromDate:date];
}


+(NSString*) GetDateTimeStart:(NSInteger)daysDifference
{
    return [GAPI GetDateTimeBasic:daysDifference withTime:@"00:00:00"];
}


+(NSString*) GetDateTimeEnd:(NSInteger)daysDifference
{
    return [GAPI GetDateTimeBasic:daysDifference withTime:@"23:59:59"];
}


+(NSString*) GetDate
{
    return [GAPI GetDate:0];
}


+(NSString*) GetDate:(NSInteger)daysDifference
{
    return [GAPI GetDate:daysDifference withWeekDay:false];
}


+(NSString*) GetDate:(NSInteger)daysDifference withWeekDay:(BOOL)showWeekDay
{
    NSDateFormatter* format = [[NSDateFormatter alloc] init];
    format.dateFormat = @"d";
    format.timeZone = [NSTimeZone defaultTimeZone];
    
    NSDate* date = [NSDate dateWithTimeIntervalSinceNow:(60*60*24*daysDifference)];
    NSString* day = [format stringFromDate:date];
    
    switch ([day integerValue])
    {
        case 1:  day = @"st"; break;
        case 21: day = @"st"; break;
        case 31: day = @"st"; break;
        case 2:  day = @"nd"; break;
        case 22: day = @"nd"; break;
        case 3:  day = @"rd"; break;
        case 23: day = @"rd"; break;
        default: day = @"th"; break;
    }
    
    if(showWeekDay)
    {
        format.dateFormat = [NSString stringWithFormat:@"'(Today) 'EEE' 'd'%@ 'MMM', 'yyyy", day];
    }
    else
    {
        format.dateFormat = [NSString stringWithFormat:@"d'%@ 'MMM', 'yyyy", day];
    }
    
    return [format stringFromDate:date];
}


+(NSMutableAttributedString*) GetDateAttributed:(NSInteger)daysDifference withFont1:(UIFont*)font1 withFont2:(UIFont*)font2
{
    NSString* string =  [GAPI GetDate:daysDifference withWeekDay:true];
    NSMutableAttributedString* attributedString = [[NSMutableAttributedString alloc] initWithString:string];
    
    [attributedString addAttribute:NSFontAttributeName value:font1 range:NSMakeRange(0, 11)];
    [attributedString addAttribute:NSFontAttributeName value:font2 range:[string rangeOfString:[GAPI GetDate]]];

    return attributedString;
}


+(NSString*) GetDateShort
{
    return [GAPI GetDateShort:0];
}


+(NSString*) GetDateShort:(NSInteger)daysDifference
{
    NSDateFormatter* format = [[NSDateFormatter alloc] init];
    format.timeZone = [NSTimeZone defaultTimeZone];
    format.dateFormat = [NSString stringWithFormat:@"yyyy'-'MM'-'dd"];
    
    NSDate* date = [NSDate dateWithTimeIntervalSinceNow:(60*60*24*daysDifference)];
    
    return [format stringFromDate:date];
}


+(NSDate *) DateFromDate:(NSDate*)date
                withHour:(NSInteger)hour
                  minute:(NSInteger)minute
                  second:(NSInteger)second
{
    NSCalendar *calendar = [NSCalendar currentCalendar];
    NSDateComponents *components = [calendar components: NSCalendarUnitYear|
                                    NSCalendarUnitMonth|
                                    NSCalendarUnitDay
                                               fromDate:date];
    [components setHour:hour];
    [components setMinute:minute];
    [components setSecond:second];
    NSDate *newDate = [calendar dateFromComponents:components];
    return newDate;
}


//
// Cache up values
//


-(void) CheckQuestionsToday
{
    if(_userQuestionsToday == nil) _userQuestionsToday = [[NSMutableArray alloc] init];    
}


/*
-(void) SetTodaySubjective:(int)index value:(int)value
{
    if(_userQuestionsToday == nil) _userQuestionsToday = [[NSMutableArray alloc] init];
    
    while(index > _userQuestionsToday.count)
    {
        [_userQuestionsToday addObject:[NSNumber numberWithInteger:-1]];
    }
    
    [_userQuestionsToday set addObject:[NSNumber numberWithInteger:value]];
}
*/


-(void) SetDoneTodaySubjective:(BOOL)saveResult
{
    _lastSaveDateSubjective = [GAPI GetDateTimeStart:0];
    if(saveResult) [self SaveCache];
}


-(BOOL) GetDoneTodaySubjective
{
    if(_lastSaveDateSubjective == nil) return false;
    
    return [_lastSaveDateSubjective isEqualToString:[GAPI GetDateTimeStart:0]];
}


-(void) ClearDoneTodaySubjective:(BOOL)saveResult
{
    _lastSaveDateSubjective = nil;
    if(saveResult) [self SaveCache];
}


-(bool) LoadCache
{
    NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
    
    //if(_activity == null) return false;
    if([defaults objectForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"EVENT"]] == nil) return false;
    
    //Load values
    _todaysEventID = [defaults integerForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"EVENTID"]];
    _todaysEvent = [defaults stringForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"EVENT"]];
    _userID = [defaults stringForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"USERID"]];
    _userFirstName = [defaults stringForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"USERFIRSTNAME"]];
    _userLastName = [defaults stringForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"USERLASTNAME"]];
    _userPhoneNumber = [defaults stringForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"USERPHONENUMBER"]];
    _userCountry = [defaults stringForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"COUNTRY"]];
    
    
    long length = [defaults integerForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"COUNTRIESLENGTH"]];
    if(length != 0)
    {
        _countries = [[NSMutableArray alloc] init];
        for(int i = 0; i < length; ++i)
        {
            NSString* key = [[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:[NSString stringWithFormat:@"COUNTRIES%i",i]];
            [_countries addObject:[defaults stringForKey:key]];
        }
    }

    length = [defaults integerForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"USERTAGLENGTH"]];
    if(length != 0)
    {
        _userTags = [[NSMutableArray alloc] init];
        for(int i = 0; i < length; ++i)
        {
            NSString* key = [[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:[NSString stringWithFormat:@"USERTAG%i",i]];
            [_userTags addObject:[defaults stringForKey:key]];
        }
    }
    
    length = [defaults integerForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"USERTAGORDEREDLENGTH"]];
    if(length != 0)
    {
        _userTagsOrdered = [[NSMutableArray alloc] init];
        for(int i = 0; i < length; ++i)
        {
            NSString* key = [[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:[NSString stringWithFormat:@"USERTAGORDERED%i",i]];
            [_userTagsOrdered addObject:[defaults stringForKey:key]];
        }
    }

    length = [defaults integerForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"TAGEMOTIONSLENGTH"]];
    if(length != 0)
    {
        _tagsEmotions = [[NSMutableArray alloc] init];
        for(int i = 0; i < length; ++i)
        {
            NSString* key = [[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:[NSString stringWithFormat:@"TAGEMOTIONS%i",i]];
            [_tagsEmotions addObject:[defaults stringForKey:key]];
        }
    }
    
    length = [defaults integerForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"TAGSCOTSLENGTH"]];
    if(length != 0)
    {
        _tagsScots = [[NSMutableArray alloc] init];
        for(int i = 0; i < length; ++i)
        {
            NSString* key = [[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:[NSString stringWithFormat:@"TAGSCOTS%i",i]];
            [_tagsScots addObject:[defaults stringForKey:key]];
        }
    }
    
    length = [defaults integerForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"USERQUESLENGTH"]];
    if(length != 0)
    {
        _userQuestions = [[NSMutableArray alloc] init];
        for(int i = 0; i < length; ++i)
        {
            NSString* key = [[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:[NSString stringWithFormat:@"USERQUES%i",i]];
            //NSString* obj = [defaults stringForKey:key];
            [_userQuestions addObject:[defaults stringForKey:key]];
        }
    }
    
    length = [defaults integerForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"USERQUESIDLENGTH"]];
    if(length != 0)
    {
        _userQuestionsID = [[NSMutableArray alloc] init];
        for(int i = 0; i < length; ++i)
        {
            NSString* key = [[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:[NSString stringWithFormat:@"USERQUESID%i",i]];
            NSInteger obj = [defaults integerForKey:key];
            [_userQuestionsID addObject:[NSNumber numberWithInteger:obj]];
        }
    }
    
    _lastSaveDateSubjective = [defaults stringForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"LASTSUBJECTIVEDATE"]];
    
    length = [defaults integerForKey:[[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:@"USERQUESTODAYLENGTH"]];
    if(length != 0)
    {
        _userQuestionsToday = [[NSMutableArray alloc] init];
        for(int i = 0; i < length; ++i)
        {
            NSString* key = [[[NSBundle mainBundle] bundleIdentifier] stringByAppendingString:[NSString stringWithFormat:@"USERQUESTODAY%i",i]];
            NSInteger obj = [defaults integerForKey:key];
            [_userQuestionsToday addObject:[NSNumber numberWithInteger:obj]];
        }
    }
        
    return true;
}


- (bool) SaveCache
{
    //Save values
    NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
    NSString* bundleID = [[NSBundle mainBundle] bundleIdentifier];
    
    [defaults setInteger:_todaysEventID forKey: [bundleID stringByAppendingString:@"EVENTID"]];
    [defaults setValue:_todaysEvent forKey: [bundleID stringByAppendingString:@"EVENT"]];
    
    if(_userID != nil)
    {
        [defaults setValue:_userID forKey: [bundleID stringByAppendingString:@"USERID"]];
    }
        
    if(_userFirstName != nil)
    {
        [defaults setValue:_userFirstName forKey: [bundleID stringByAppendingString:@"USERFIRSTNAME"]];
    }
    
    if(_userLastName != nil)
    {
        [defaults setValue:_userLastName forKey: [bundleID stringByAppendingString:@"USERLASTNAME"]];
    }
    
    if(_userPhoneNumber != nil)
    {
        [defaults setValue:_userPhoneNumber forKey: [bundleID stringByAppendingString:@"USERPHONENUMBER"]];
    }
    
    if(_userCountry != nil)
    {
        [defaults setValue:_userCountry forKey: [bundleID stringByAppendingString:@"COUNTRY"]];
    }
    
    if(_countries != nil)
    {
        [defaults setInteger:_countries.count forKey: [bundleID stringByAppendingString:@"COUNTRIESLENGTH"]];
        for (int i = 0; i < _countries.count; ++i)
        {
            NSString* key = [bundleID stringByAppendingString:[NSString stringWithFormat:@"COUNTRIES%i",i]];
            [defaults setValue:_countries[i] forKey:key];
        }
    }
    
    if(_userTags != nil)
    {
        [defaults setInteger:_userTags.count forKey: [bundleID stringByAppendingString:@"USERTAGLENGTH"]];
        for (int i = 0; i < _userTags.count; ++i)
        {
            NSString* key = [bundleID stringByAppendingString:[NSString stringWithFormat:@"USERTAG%i",i]];
            [defaults setValue:_userTags[i] forKey:key];
        }
    }
    
    if(_userTagsOrdered != nil)
    {
        [defaults setInteger:_userTagsOrdered.count forKey: [bundleID stringByAppendingString:@"USERTAGORDEREDLENGTH"]];
        for (int i = 0; i < _userTagsOrdered.count; ++i)
        {
            NSString* key = [bundleID stringByAppendingString:[NSString stringWithFormat:@"USERTAGORDERED%i",i]];
            [defaults setValue:_userTagsOrdered[i] forKey:key];
        }
    }
    
    if(_tagsEmotions != nil)
    {
        [defaults setInteger:_tagsEmotions.count forKey: [bundleID stringByAppendingString:@"TAGEMOTIONSLENGTH"]];
        for (int i = 0; i < _tagsEmotions.count; ++i)
        {
            NSString* key = [bundleID stringByAppendingString:[NSString stringWithFormat:@"TAGEMOTIONS%i",i]];
            [defaults setValue:_tagsEmotions[i] forKey:key];
        }
    }
    
    if(_tagsScots != nil)
    {
        [defaults setInteger:_tagsScots.count forKey: [bundleID stringByAppendingString:@"TAGSCOTSLENGTH"]];
        for (int i = 0; i < _tagsScots.count; ++i)
        {
            NSString* key = [bundleID stringByAppendingString:[NSString stringWithFormat:@"TAGSCOTS%i",i]];
            [defaults setValue:_tagsScots[i] forKey:key];
        }
    }
    
    if(_userQuestions != nil)
    {
        [defaults setInteger:_userQuestions.count forKey: [bundleID stringByAppendingString:@"USERQUESLENGTH"]];
        for (int i = 0; i < _userQuestions.count; ++i)
        {
            NSString* key = [bundleID stringByAppendingString:[NSString stringWithFormat:@"USERQUES%i",i]];
            [defaults setValue:_userQuestions[i] forKey:key];
        }
    }
    
    if(_userQuestionsID != nil)
    {
        [defaults setInteger:_userQuestionsID.count forKey: [bundleID stringByAppendingString:@"USERQUESIDLENGTH"]];
        for (int i = 0; i < _userQuestionsID.count; ++i)
        {
            NSString* key = [bundleID stringByAppendingString:[NSString stringWithFormat:@"USERQUESID%i",i]];
            NSNumber* uid = _userQuestionsID[i];
            int uuid = [uid intValue];
            [defaults setInteger:uuid forKey:key];
        }
    }
    
    if(_lastSaveDateSubjective != nil)
    {
        NSString* key = [bundleID stringByAppendingString:@"LASTSUBJECTIVEDATE"];
        [defaults setValue:_lastSaveDateSubjective forKey:key];
    }
    
    if(_userQuestionsToday != nil)
    {
        [defaults setInteger:_userQuestionsToday.count forKey: [bundleID stringByAppendingString:@"USERQUESTODAYLENGTH"]];
        for (int i = 0; i < _userQuestionsToday.count; ++i)
        {
            NSString* key = [bundleID stringByAppendingString:[NSString stringWithFormat:@"USERQUESTODAY%i",i]];
            NSNumber* uid = _userQuestionsToday[i];
            int uuid = [uid intValue];
            [defaults setInteger:uuid forKey:key];
        }
    }
    else
    {
        [defaults removeObjectForKey:[bundleID stringByAppendingString:@"USERQUESTODAYLENGTH"]];
        for(int i = 0; i < 3; ++i)
        {
            NSString* key = [bundleID stringByAppendingString:[NSString stringWithFormat:@"USERQUESTODAY%i",i]];
            [defaults removeObjectForKey:key];
        }
    }
    
    [defaults synchronize];
    
    return true;
}


@end