package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.RemindersActivity.Companion.REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.BuildConfig
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    companion object {
        const val  TAG = "SelectLocationFragment"
        const val DEFAULT_ZOOM = 16f
    }

    private var reminderSelectedLocationStr = ""
    private lateinit var selectedPOI: PointOfInterest
    private var latitude = 0.0
    private var longitude = 0.0

    private val runningOnQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        setMapFragment()

        //call onLocationSelected function after the user confirms on the selected location
        binding.selectLocationFragmentSaveLocation.setOnClickListener {
            if (reminderSelectedLocationStr.isNotEmpty()) {
                onLocationSelected()
            } else
                _viewModel.showToast.value = "Please select a location"
        }

        return binding.root
    }

    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    private fun setMapFragment() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.select_location_fragment_map_view) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        //fusedLocationProviderClient = this.requireContext().let { LocationServices.getFusedLocationProviderClient(it) }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    //onMapReady is called when the map is ready to be used and provides a non-null instance of GoogleMap
    //this method will only be triggered when the user has installed GooglePlay Services
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        setOnMapLongClick(map)
        setPoiClick(map)
        setCurrentLocationClick(map)
        setMapStyle(map)
        enableMyLocation()

    }

    //If the requestCode is equal to REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE, then permission is granted,
    //and if the grantResults array is non empty with PackageManager.PERMISSION_GRANTED in its first slot, then call enableMyLocation():
    //Note, for Android 10+ Q we need to check background permission as well
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray) {
        // Check if location permissions are granted and if so enable the
        // location data layer.
        if (requestCode == REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.size > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                enableMyLocation()
            }
        }else{
            Snackbar.make(
                binding.root,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_LONG
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        }
    }

    private fun setCurrentLocationClick(map: GoogleMap) {
        map.setOnMyLocationButtonClickListener {
            val location = map.myLocation ?: return@setOnMyLocationButtonClickListener false
            val latLng = LatLng(location.latitude, location.longitude)
            updateCurrentLocation(latLng)
            return@setOnMyLocationButtonClickListener true
        }

        map.setOnMyLocationClickListener { location ->
            val latLng = LatLng(location.latitude, location.longitude)
            updateCurrentLocation(latLng)
        }
    }

    private fun updateCurrentLocation(latLng: LatLng) {
        val lat = String.format("%.5f",latLng.latitude);
        val lng = String.format("%.5f",latLng.longitude);

        reminderSelectedLocationStr = "Lat: $lat, Long: $lng"
        selectedPOI = PointOfInterest(latLng, reminderSelectedLocationStr, "Current Location")
        latitude = latLng.latitude
        longitude = latLng.longitude
    }

    //method stub in MapsActivity called setPoiClick() that takes a GoogleMap as an argument.
    //In the setPoiClick() method, set an OnPoiClickListener on the passed-in GoogleMap
    private fun setPoiClick(map: GoogleMap) {
        //In the onPoiClick() method, place a marker at the POI location.
        //Set the title to the name of the POI. Save the result to a variable called poiMarker.
        map.setOnPoiClickListener { poi ->
            map.clear()
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
            poiMarker?.showInfoWindow()
            reminderSelectedLocationStr = poi.name
            selectedPOI = poi
            latitude = poi.latLng.latitude
            longitude = poi.latLng.longitude
        }
    }

    //this is a method stub
    //it takes a GoogleMap as an argument, and attaches a long click listener to the map object
    private fun setOnMapLongClick(map: GoogleMap) {

        //setOnMapLongClickListener function lambda
        map.setOnMapLongClickListener { latLng ->

            //A snippet is additional text that is displayed below the title.
            //In your case the snippet displays the latitude and longitude of a marker.
            val snippet = String.format(
                Locale.getDefault(),
                //   getString(R.string.lat_long_snippet),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )

            //move the camera (camera means the google maps view) by supplying it with latLng and zoom level
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM))
            map.addMarker(MarkerOptions()
                .position(latLng)
                .title(getString(R.string.dropped_pin))
                .snippet(snippet)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
            reminderSelectedLocationStr = snippet
            selectedPOI = PointOfInterest(latLng, reminderSelectedLocationStr, "Custom Location")
            latitude = latLng.latitude
            longitude = latLng.longitude
        }
    }

    private fun setMapStyle(map: GoogleMap){
        // Customize the styling of the base map using a JSON object defined
        // in a raw resource file.
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style)
            )
            //If the styling is unsuccessful, print a log that the parsing has failed.
            if (!success){
                Log.e(TAG, "Style parsing failed.")
            }
        }
        //In the catch block if the file can't be loaded, the method throws a Resources.NotFoundException.
        catch(e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: $e")
        }
    }

    //To check if permissions are granted, create a method in the SelectLocationFragment.kt called isPermissionGranted().
    //In this method, check if the user has granted the permission.
    //it returns true if the user granted the permission, and false if he didn't
    private fun isPermissionGranted(): Boolean {
        return ContextCompat
            .checkSelfPermission(requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    //To enable location tracking in your app, create a method in the SelectLocationFragment.kt
    //called enableMyLocation() that takes no arguments and doesn't return anything.
    //Check for the ACCESS_FINE_LOCATION permission. If the permission is granted,
    //enable the location layer. Otherwise, re-request the permission:
    @SuppressLint("MissingPermission")
    private fun enableMyLocation(){
        if(isPermissionGranted()){
            map.isMyLocationEnabled = true
            fusedLocationProviderClient.lastLocation.addOnSuccessListener(requireActivity()) { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, DEFAULT_ZOOM))
                }
            }
        }
        else {
            _viewModel.showErrorMessage.postValue(getString(R.string.err_select_location))
            val permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            val resultCode = REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            requestPermissions(
                permissionsArray,
                resultCode
            )
        }
    }

    private fun onLocationSelected() {
        //When the user confirms on the selected location,
        //send back the selected location details to the view model
        //and navigate back to the previous fragment to save the reminder and add the geofence
        _viewModel.reminderSelectedLocationStr.value = reminderSelectedLocationStr
        _viewModel.selectedPOI.value = selectedPOI
        _viewModel.latitude.value = latitude
        _viewModel.longitude.value = longitude
        _viewModel.navigationCommand.value = NavigationCommand.Back
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        //Change the map type based on the user's selection.
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }


}
