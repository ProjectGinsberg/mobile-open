package com.ginsberg.api;


import org.json.JSONArray;


/**
 * @brief      Main interface class
 *
 * @details    Interface for callbacks from GAPI
 *
 * Note the following example code:
 * @code
 *    GAPI.Instance().Setup(this, "0C3E41287051F805D76F1ABE5B0C7450F79CBC64", this);
 *    GAPI.Instance().GetMe();
 * @endcode
 *
 * @note       This class has no class.
 * @attention  Should only be used by those who
 *             know what they are doing.
 * @warning    Not certified for use within mission
 *             critical or life sustaining systems.
 */
public interface IGAPICallbacks
{
    /**
      * @brief      Callback for comments from sdk
      * @details    When the system has simple messages to pass back to system, they will be sent here for either ignoring or displaying to user.
      */
    public void Comment(String text);

    /**
      * @brief      Callback for when app has access
      * @details    After the SDK is initialized and finds no valid user login details, else a connection fault, this method will be called
      */
    public void NeedLogin();

    /**
      * @brief      Callback for when app has access
      * @details    After sdk setup, and the user has accepted access, this method will be called
      */
    public void GainedAccess();

    /**
      * @brief      Callback for when app receives data from the server
      * @details    When ever the app requests data, this will be where valid returned data will be sent
      */
    public void DataReceived(String endPoint, JSONArray data);

    /**
      * @brief      Callback for when the sdk busy state has changed
      * @details   
      */
    public void SetBusy(boolean truth);

    /**
      * @brief      Callback for error messages from sdk
      * @details    When the system has error messages to pass back to system, they will be sent here for either ignoring or displaying to user.
      */
    public void CommentError(String text);

    /**
      * @brief      Callback for result messages from sdk //Not currently used
      * @details    When the system has result messages to pass back to system, they will be sent here for either ignoring or displaying to user.
      */
    public void CommentResult(String text);

    /**
      * @brief      Callback for system messages from sdk //Not currently used
      * @details    When the system has system derived messages to pass back to system, they will be sent here for either ignoring or displaying to user.
      */
    public void CommentSystem(String text);
}
