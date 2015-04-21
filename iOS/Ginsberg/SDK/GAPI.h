/**
 * @file   GAPI.h
 * @Author Ginsberg
 * @date   January, 2015
 * @brief  Main header file for GAPI SDK
 */
#import <UIKit/UIKit.h>
#import <Foundation/Foundation.h>


#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wobjc-missing-property-synthesis"


#import <Availability.h>
#undef weak_ref
#if __has_feature(objc_arc) && __has_feature(objc_arc_weak)
#define weak_ref weak
#else
#define weak_ref unsafe_unretained
#endif




/**
 * @brief      Main protocol for feedback from sdk
 *
 * @details    Protocol for providing information back from sdk as it posts and gets data.
 *
 * Note the following example code:
 * @code
 *    class ViewControllerPost: UIViewController, GAPIProtocol
 *    {
 * @endcode
 */
@protocol GAPIProtocol <NSObject>


@required

/** 
 *  @brief       Callback for comments from sdk
 *  @details     When the system has simple messages to pass back to system, they will be sent here for either ignoring or displaying to user.
 *  @param text  Text of comment.
 */
-(void)Comment:(NSString*)text;


@optional

/**
 *  @brief       Callback for when app has access
 *  @details     After the SDK is initialized and finds no valid user login details, else a connection fault, this method will be called
 */
-(void)NeedLogin;

/**
 *  @brief       Callback for when app has access
 *  @details     After sdk setup, and the user has accepted access, this method will be called
 */
-(void)GainedAccess;

/**
 *  @brief            Callback for when app receives data from the server
 *  @details          When ever the app requests data, this will be where valid returned data will be sent
 *  @param endPoint   Endpoint data was recieved from, e.g. "/v1/o/wellbeing"
 *  @param data       Date received from that end point
 *  @param string     Readable string version of data param
 */
-(void)DataReceived:(NSString*)endPoint withData:(NSObject*)data andString:(NSString*)string;

/**
 *  @brief       Callback for when the sdk busy state has changed
 *  @details
 *  @param truth Truth of if sdk is currently busy doing something or not.
 */
-(void)SetBusy:(BOOL)truth;

/**
 *  @brief       Callback for error messages from sdk
 *  @details     When the system has an error message to pass back to system, they will be sent here for either ignoring or displaying to user.
 *  @param text  Text of comment.
 */
-(void)CommentError:(NSString*)text;

/**
 *  @brief       Callback for result messages from sdk //Not currently used
 *  @details     When the system has a result message to pass back to system, they will be sent here for either ignoring or displaying to user.
 *  @param text  Text of comment.
 */
-(void)CommentResult:(NSString*)text;

/**
 *  @brief       Callback for system messages from sdk //Not currently used
 *  @details     When the system has a system derived message to pass back to system, they will be sent here for either ignoring or displaying to user.
 *  @param text  Text of comment.
 */
-(void)CommentSystem:(NSString*)text;

@end




/**
 * @brief      Main controlling class
 *
 * @details    The is the main sdk class, to access all the main functionality of Ginsberg. It runs through the creation of a singleton of it.
 *
 * Note the following example code:
 * @code
 *    GAPI.Instance().Setup(this, "0C3E41287051F805D76F1ABE5B0C7550F79CBC64", this);
 *    GAPI.Instance().GetProfile();
 * @endcode
 *
 * @note
 * @attention
 * @warning
 */
@interface GAPI : NSObject<UIWebViewDelegate,UIAlertViewDelegate>

/**
 *  @brief  Get Singleton instance of GAPI SDKs main class
 */
+(GAPI*)Instance;

/**
 *  @brief  Check to see if token is stored in system
 */
+(BOOL)HaveToken;

/**
 *  @brief  Clear token so user must log back in
 */
-(void)ClearToken;



//
// Date/time strings
//

/**
 *  @brief  Get the current string of date time for today
 *  @note   Return example: 2015-01-19T10:31:55+0000
 */
+(NSString*) GetDateTime;

/**
 *  @brief  Get the current string of date time for today + the days difference parameter
 *  @param  daysDifference  The days difference, from today, to get string for
 *  @note   Return example: 2015-01-19T10:31:55+0000
 */
+(NSString*) GetDateTime:(NSInteger)daysDifference;

/**
 *  @brief  Get the current string of date for today
 *  @note   Return example: 19th Jan, 2015
 */
+(NSString*) GetDate;

/**
 *  @brief  Get the current string of date for today + days difference parameter
 *  @param  daysDifference  The days difference, from today, to get string for
 *  @note   Return example: 19th Jan, 2015
 */
+(NSString*) GetDate:(NSInteger)daysDifference;

/**
 *  @brief  Get the current string of date time for the start of today + days difference parameter
 *  @param  daysDifference  The days difference, from today, to get string for
 *  @note   Return example: 2015-01-19T00:00:00+0000
 */
+(NSString*) GetDateTimeStart:(NSInteger)daysDifference;

/**
 *  @brief  Get the current string of date time for the end of today + days difference parameter
 *  @param  daysDifference  The days difference, from today, to get string for
 *  @note   Return example: 2015-01-19T23:59:59+0000
 */
+(NSString*) GetDateTimeEnd:(NSInteger)daysDifference;

/** Returns a new NSDate object with the time set to the indicated hour, minute, and second.
 * @param hour The hour to use in the new date.
 * @param minute The number of minutes to use in the new date.
 * @param second The number of seconds to use in the new date.
 */
+(NSDate *) DateFromDate:(NSDate*)date
                withHour:(NSInteger)hour
                  minute:(NSInteger)minute
                  second:(NSInteger)second;

/**
 *  @brief  Get the current string of date time for today and showing week day + days difference parameter
 *  @param  daysDifference  The days difference, from today, to get string for
 *  @param  showWeekDay     Truth of if to append the day of the week to the string
 *  @note   Return example: 19th Jan, 2015   OR   (Today) Mon 19th Jan, 2015
 */
+(NSString*) GetDate:(NSInteger)daysDifference withWeekDay:(BOOL)showWeekDay;

/**
 *  @brief  Get the current attributed string for today including two different fonts and showing week day + days difference parameter
 *  @param  daysDifference  The days difference, from today, to get string for
 *  @param  font1           The font to start the string with
 *  @param  font2           The font to end the string with
 *  @note   Return example: (Today) Mon 19th Jan, 2015
 */
+(NSMutableAttributedString*) GetDateAttributed:(NSInteger)daysDifference withFont1:(UIFont*)font1 withFont2:(UIFont*)font2;

/**
 *  @brief  Get the current short string for today
 *  @note   Return example: 2015-01-19
 */
+(NSString*) GetDateShort;

/**
 *  @brief  Get the current short string for today + days difference parameter
 *  @param  daysDifference  The days difference, from today, to get string for
 *  @note   Return example: 2015-01-19
 */
+(NSString*) GetDateShort:(NSInteger)daysDifference;


//
// General setup
//

/**
 *  @brief  Start signup process via popover web view
 */
-(void) SignUpWeb;

/**
 *  @brief  Create new user, e.g. via custom signup dialog.
 *  Swift example call:
 *  @code
 *  GAPI.Instance()!.SignUp("John", lastName:"Smith", password:"Password", cpassword:"Password", email:"john@smith.com", countryID:1, wbIDs:nil);
 *  @endcode
 *  @param  firstName  Users first name
 *  @param  lastNAme   Users last name
 *  @param  password   Password for user
 *  @param  cpassword  Password confirmation for user
 *  @param  email      Users email address
 *  @param  country    Users country id
 *  @param  wbIDs      Users selected wellbeing questions IDs
 */
-(void) SignUp:(NSString*)firstName lastName:(NSString*)lastName password:(NSString*)password cpassword:(NSString*)cpassword email:(NSString*)email countryID:(int)countryID wbIDs:(int[])wbIDs;

/**
 *  @brief  Start 3rd partys connections via popover web view
 *  @param  background  Image to show behind popover web view
 */
-(void) ConnectionsWeb:(UIImage*)background;

/**
 *  @brief  Initial setup of SDK. This should be the first call to gain access to the SDK.
 *  Swift example call:
 *  @code
 *  GAPI.Instance()!.Setup(CLIENT_ID, secret:CLIENT_SECRET, callbacks:self);
 *  @endcode
 *  @param  clientID      String id of developers client ID
 *  @param  clientSecret  String of developers secret
 *  @param  callbacks     Reference to protocol instance that will recieve callbacks from the SDK
 */
-(void) Setup:(NSString*)_clientID secret:(NSString*)_clientSecret callbacks:(id<GAPIProtocol>)_callbacks;

/**
 *  @brief  Start login process via popover web view
 */
-(void) Login;

/**
 *  @brief  Set instance to send SDK callbacks to
 */
-(void) SetCallbacks:(id<GAPIProtocol>)_callbacks;


//
// General calls
//

/**
 *  @brief  Get users country id code
 */
-(int32_t) GetUserCountryID;

/**
 *  @brief  Check, and setup if needed, storage for todays users subjective questions
 */
-(void) CheckQuestionsToday;

/**
 *  @brief  Set more recently answered subjective question time to now
 *  @param  saveResult  Backup result if required
 */
-(void) SetDoneTodaySubjective:(BOOL)saveResult;

/**
 *  @brief  Get truth of if subjective questions answered for today
 */
-(BOOL) GetDoneTodaySubjective;

/**
 *  @brief  Clear answered subjective question time
 *  @param  saveResult  Clear from backup if required too
 */
-(void) ClearDoneTodaySubjective:(BOOL)saveResult;


//
// Main API Calls
//

/**
 *  @brief  Get users event for today
 */
-(void) GetTodaysEvent;

/**
 *  @brief  Get users subjective answers for today
 */
-(void) GetTodaysSubjective;

//-(void) GetAggregate:(NSString*)period typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID;

/**
 *  @brief  Get users correlations
 */
-(void) GetCorrelations;

/**
 *  @brief  Get users daily summary.
 *  Swift example call:
 *  @code
 *  GAPI.Instance().GetDailySummary("All",typeFrom:"yeterday",dateFrom:nil,typeTo:"yesterday",dateTo:nil);
 *  @endcode
 *  @param  range  Range to get data for. Is one of: "All":Get all available data, "From":From the given typeFrom param, "To":Upto the given typeTo param, "FromTo":Between the given typeFrom/typeTo params.
 *  @param  typeFrom  Type of range to start getting data from. Is one of: "Yesterday", "Lastweek", "Lastyear", "Date"
 *  @param  dateFrom  If using typeFrom of "Date", the string date to get data from
 *  @param  typeTo  Type of range to end getting data upto. Is one of: "Yesterday", "Lastweek", "Lastyear", "Date"
 *  @param  dateTo  If using typeTo of "Date", the string date to get data upto
 */
-(void) GetDailySummary:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo;

//-(void) PostNotifications:(NSString*)timeStamp value:(int)value;
-(void) GetNotifications:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID;

-(void) GetProfile;
-(void) PostProfile:(NSString*)firstName lastName:(NSString*)lastName phoneNumber:(NSString*)phoneNumber country:(int)country;

-(void) GetTag:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo;

//Subjective/Objective data
-(void) PostWellbeing:(NSString*)timeStamp value:(int)value wbques:(NSString*)wbques wbtype:(int)wbtype;

/**
 *  @brief  Get users wellbeing data
 *  Swift example call:
 *  @code
 *  GAPI.Instance().GetDailySummary("All",typeFrom:"yeterday",dateFrom:nil,typeTo:"yesterday",dateTo:nil);
 *  @endcode
 *  @param  range  Range to get data for. Is one of: "All":Get all available data, "ID":For particular wellbeing entry given by the ID param, "From":From the given typeFrom param, "To":Upto the given typeTo param, "FromTo":Between the given typeFrom/typeTo params.
 *  @param  typeFrom  Type of range to start getting data from. Is one of: "Yesterday", "Lastweek", "Lastyear", "Date"
 *  @param  dateFrom  If using typeFrom of "Date", the string date to get data from
 *  @param  typeTo  Type of range to end getting data upto. Is one of: "Yesterday", "Lastweek", "Lastyear", "Date"
 *  @param  dateTo  If using typeTo of "Date", the string date to get data upto
 *  @param  ID  If using range of "ID", this is the id of the particular entry to get
 */
-(void) GetWellbeing:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID;
-(void) DeleteWellbeing:(int)ID;

-(void) PostEvent:(NSString*)timeStamp event:(NSString*)event ID:(long)ID;
-(void) GetEvent:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID;
-(void) DeleteEvent:(int)ID;

-(void) PostExercise:(NSString*)timeStamp activity:(NSString*)activity distance:(double)distance;
-(void) GetExercise:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID;
-(void) DeleteExercise:(int)ID;

-(void) PostSleep:(NSString*)timeStamp timesAwoken:(int)timesAwoken awake:(double)awake lightSleep:(double)lightSleep remSleep:(double)remSleep deepSleep:(double)deepSleep totalSleep:(double)totalSleep quality:(int)quality;
-(void) GetSleep:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID;
-(void) DeleteSleep:(int)ID;

-(void) PostAlcohol:(NSString*)timeStamp units:(double)units;
-(void) GetAlcohol:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID;
-(void) DeleteAlcohol:(int)ID;

-(void) PostActivity:(NSString*)timeStamp start:(NSString*)start end:(NSString*)end dist:(double)dist cal:(double)cal steps:(int)steps;
-(void) GetActivity:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID;
-(void) DeleteActivity:(int)ID;

-(void) PostNutrition:(NSString*)timeStamp calories:(double)calories carbohydrates:(double)carbohydrates fat:(double)fat fiber:(double)fiber protein:(double)protein sugar:(double)sugar;
-(void) GetNutrition:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID;
-(void) DeleteNutrition:(int)ID;

-(void) PostBody:(NSString*)timeStamp weight:(double)weight fat:(double)fat;
-(void) GetBody:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID;
-(void) DeleteBody:(int)ID;

-(void) PostCaffeine:(NSString*)timeStamp amount:(double)ef;
-(void) GetCaffeine:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID;
-(void) DeleteCaffeine:(int)ID;

-(void) PostSmoking:(NSString*)timeStamp amount:(int)ei;
-(void) GetSmoking:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID;
-(void) DeleteSmoking:(int)ID;

-(void) PostStepcount:(NSString*)timeStamp timeStart:(NSString*)timeStart timeEnd:(NSString*)timeEnd distance:(double)distance calories:(double)calories steps:(int)stepCount;
-(void) GetStepcount:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID;
-(void) DeleteStepcount:(int)ID;

-(void) PostSocial:(NSString*) timeStamp;
-(void) GetSocial:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID;
-(void) DeleteSocial:(int)ID;

-(void) PostMood:(NSString*)timeStamp value:(int)ei;
-(void) GetMood:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID;
-(void) DeleteMood:(int)ID;

-(void) PostHappy:(NSString*)timeStamp value:(int)ei;
-(void) GetHappy:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID;
-(void) DeleteHappy:(int)ID;

-(void) PostSad:(NSString*)timeStamp value:(int)ei;
-(void) GetSad:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID;
-(void) DeleteSad:(int)ID;

-(void) PostUneasy:(NSString*)timeStamp value:(int)ei;
-(void) GetUneasy:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID;
-(void) DeleteUneasy:(int)ID;

-(void) PostWell:(NSString*)timeStamp value:(int)ei;
-(void) GetWell:(NSString*)range typeFrom:(NSString*)typeFrom dateFrom:(NSString*)dateFrom typeTo:(NSString*)typeTo dateTo:(NSString*)dateTo ID:(long)ID;
-(void) DeleteWell:(int)ID;

//-(void) GetMeasures:(long)ID;

//-(void) PostSurvey:(long)ID;
//-(void) GetSurvey;


//
// Stored data
//

/**
 *  @brief       Temporary store of current users id
 */
@property (strong, nonatomic) NSString*       userID;

/**
 *  @brief       Temporary store of current users first name
 */
@property (strong, nonatomic) NSString*       userFirstName;

/**
 *  @brief       Temporary store of current users last name
 */
@property (strong, nonatomic) NSString*       userLastName;

/**
 *  @brief       Temporary store of current users phone number
 */
@property (strong, nonatomic) NSString*       userPhoneNumber;

/**
 *  @brief       Temporary store of current users country
 */
@property (strong, nonatomic) NSString*       userCountry;

/**
 *  @brief       Temporary store of countries stored in Ginsberg
 */
@property (strong, nonatomic) NSMutableArray* countries;

/**
 *  @brief       Temporary store of current users tags ordered by recent first
 */
@property (strong, nonatomic) NSMutableArray* userTags;

/**
 *  @brief       Temporary store of current users tags ordered by most used
 */
@property (strong, nonatomic) NSMutableArray* userTagsOrdered;

/**
 *  @brief       Temporary store of current emotion tags
 */
@property (strong, nonatomic) NSMutableArray* tagsEmotions;

/**
 *  @brief       Temporary store of current scots tags
 */
@property (strong, nonatomic) NSMutableArray* tagsScots;

/**
 *  @brief       Temporary store of current user questions for wellbeing
 */
@property (strong, nonatomic) NSMutableArray* userQuestions;

/**
 *  @brief       Temporary store of current users question IDs for wellbeing
 */
@property (strong, nonatomic) NSMutableArray* userQuestionsID;

/**
 *  @brief       Temporary store of current users selected answers for today
 */
@property (strong, nonatomic) NSMutableArray* userQuestionsToday;

/**
 *  @brief       Temporary store of current users event ID for today
 */
@property (nonatomic)         long            todaysEventID;

/**
 *  @brief       Temporary store of current users event string
 */
@property (strong, nonatomic) NSString*       todaysEvent;

/**
 *  @brief       Temporary store of current users last date of subjective data saving
 */
@property (strong, nonatomic) NSString*       lastSaveDateSubjective;

@end
