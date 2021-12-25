package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.RemindersActivity.Companion.REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_REQUEST_CODE
import com.udacity.project4.locationreminders.RemindersActivity.Companion.REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
import com.udacity.project4.locationreminders.RemindersActivity.Companion.REQUEST_TURN_DEVICE_LOCATION_ON
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single(singleton) to be shared with the another fragment (SelectLocationFragment)
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    //A GeofencingClient is the main entry point for interacting with the geofencing APIs.
    lateinit var geofencingClient: GeofencingClient

    //A PendingIntent is a description of an Intent and target action to perform with it. Create one for the IntentService to handle the geofence transitions.
    //this PendingIntent handles the geofence transitions. Connect it to the GeofenceTransitionsBroadcastReceiver.
    private val geofencePendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = RemindersActivity.ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    companion object {
        const val GEOFENCE_RADIUS_IN_METERS = 100f
        const val TAG = "SaveReminderFragment"
        private val runningOnQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)
        geofencingClient = LocationServices.getGeofencingClient(requireContext())

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this

        binding.selectLocation.setOnClickListener {
            //Navigate to SelectLocationFragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            checkPermissionsAndStartGeofencing()

        }
    }

    private fun saveReminderAndAddGeofenceToIt(){

        val reminderDataItem = ReminderDataItem(
            title = _viewModel.reminderTitle.value,
            description = _viewModel.reminderDescription.value,
            latitude = _viewModel.latitude.value,
            longitude = _viewModel.longitude.value,
            location = _viewModel.reminderSelectedLocationStr.value
        )

        if (_viewModel.validateEnteredData(reminderDataItem)) {
            _viewModel.saveReminder(reminderDataItem)
            addGeofenceToReminder(reminderDataItem)
        }

    }

    @SuppressLint("MissingPermission")
    private fun addGeofenceToReminder(reminder: ReminderDataItem) {

        //Build the geofence using the geofence builder, the information in currentGeofenceData, like the id and the
        //latitude and longitude. Set the expiration duration using the constant set in GeofencingConstants.
        //Set the transition type to GEOFENCE_TRANSITION_ENTER. Finally, build the geofence.
        val geofence = Geofence.Builder()
            .setRequestId(reminder.id)
            .setCircularRegion(
                reminder.latitude!!,
                reminder.longitude!!,
                GEOFENCE_RADIUS_IN_METERS
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        //Build the geofence request. Set the initial trigger to INITIAL_TRIGGER_ENTER, add the geofence you just built and then build.
        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()


        //Call removeGeofences() on the geofencingClient to remove any geofences already associated to the pending intent
        geofencingClient.removeGeofences(geofencePendingIntent)?.run {
            addOnCompleteListener {

                //When removeGeofences() completes, regardless of its success or failure, add the new geofences.
                geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
            }//OnCompleteListener boundaries
        }//removeGeofences.run method boundaries
    }

    /**
     * Starts the permission check and Geofence process only if the Geofence associated with the
     * current reminder isn't yet active.
     */
    private fun checkPermissionsAndStartGeofencing() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    //Uses the Location Client to check the current state of location settings, and gives the user the opportunity to turn on location services within our app.
    @SuppressLint("MissingPermission")
    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {

        //create a LocationRequest and a LocationSettingsRequest Builder.
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        //use LocationServices to get the Settings Client and create a val called locationSettingsResponseTask to check the location settings.
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        //Since the case we are most interested in here is finding out if the location settings are not satisfied
        //add an onFailureListener() to the locationSettingsResponseTask.
        locationSettingsResponseTask.addOnFailureListener { exception ->
            //Check if the exception is of type ResolvableApiException and if so,
            //try calling the startResolutionForResult() method in order to prompt the user to turn on device location.
            if (exception is ResolvableApiException && resolve) {
                try {
                    exception.startResolutionForResult(
                        requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) { //If calling startResolutionForResult enters the catch block, print a log.
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            }
            //If the exception is not of type ResolvableApiException, present a snackbar that alerts the user
            //that location needs to be enabled to play the treasure hunt.
            else {
                Snackbar.make(
                    binding.saveReminderRootLayout,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
        //If the locationSettingsResponseTask does complete, check that it is successful, if so you will want to add the geofence.
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                saveReminderAndAddGeofenceToIt()
            }
        }
    }

    //Determines whether the app has the appropriate permissions across Android 10+ and all other Android versions
    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {

        //check if the ACCESS_FINE_LOCATION permission is granted.
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION)
                )

        //If the device is running Q or higher, check that the ACCESS_BACKGROUND_LOCATION permission is granted.
        //Return true if the device is running lower than Q where you don't need a permission to access location in the background.
        val backgroundLocationApproved = if (runningOnQOrLater){
            PackageManager.PERMISSION_GRANTED ==
                    ActivityCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            true
        }

        //this returns true if both foreground and background permissions are granted, and false if not.
        return foregroundLocationApproved && backgroundLocationApproved
    }

    //Requests ACCESS_FINE_LOCATION and (on Android 10+ (Q) ACCESS_BACKGROUND_LOCATION.
    @TargetApi(29 )
    private fun requestForegroundAndBackgroundLocationPermissions() {
        //This is where you ask the user to grant location permissions.

        //If the permissions have already been approved, you don’t need to ask again. Return out of the method.
        if(foregroundAndBackgroundLocationPermissionApproved())
            return

        //permissionsArray contains an array of the permissions that are going to be requested. Initially, add ACCESS_FINE_LOCATION since that will be needed on all API levels.
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        //requestCode will be different depending on if the device is running Q or later and will inform us if you need to check for one permission (fine location) or multiple permissions (fine and background location) when the user returns from the permission request screen.
        //Add a when statement to check the version running and assign request code to REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE if the device is running Q or later and REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE if not.
        val resultCode = when {
            runningOnQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_REQUEST_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }

        //Request permissions passing in the current activity, the permissions array and the result code.
        ActivityCompat.requestPermissions(
            requireActivity(),
            permissionsArray,
            resultCode
        )

    }


    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a singleton view model
        _viewModel.onClear()
    }
}
