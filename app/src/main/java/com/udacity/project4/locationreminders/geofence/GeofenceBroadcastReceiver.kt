package com.udacity.project4.locationreminders.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.udacity.project4.locationreminders.RemindersActivity.Companion.ACTION_GEOFENCE_EVENT

/**
 * Triggered by the Geofence.  Since we can have many Geofences at once, we pull the request
 * ID from the first Geofence, and locate it within the cached data in our Room DB
 *
 * Or users can add the reminders and then close the app, So our app has to run in the background
 * and handle the geofencing in the background.
 * To do that you can use https://developer.android.com/reference/android/support/v4/app/JobIntentService to do that.
 *
 */

//BroadcastReceiver is how Android apps can send or receive messages from the Android system and other Android apps
//create a Broadcast Receiver to receive the details about the geofence transition events.
//Specifically, you want to know when the user has entered the geofence.
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    //implement the onReceive method to receive the geofencing events at the background
    override fun onReceive(context: Context, intent: Intent) {
        //A Broadcast Receiver can receive many types of actions, but in our case we only care about when the geofence is entered.
        //Check that the intent’s action is of type ACTION_GEOFENCE_EVENT.
        if (intent.action == ACTION_GEOFENCE_EVENT) {
            GeofenceTransitionsJobIntentService.enqueueWork(context, intent)
        }
    }
}