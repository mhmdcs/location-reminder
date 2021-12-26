package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 * set this as the starting activity through AndroidManifest.xml by adding the appropriate intent-filter action MAIN and category LAUNCHER tags
 */
class AuthenticationActivity : AppCompatActivity() {

    companion object {
        const val  TAG = "AuthenticationActivity"
        const val LOG_IN_REQUEST_CODE = 1001
    }

    private lateinit var binding: ActivityAuthenticationBinding

    private val viewModel by viewModels<AuthenticationViewModel>()
   // private val viewModel: AuthenticationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_authentication)

        binding.textviewLoginButton.setOnClickListener{startLoginFlow()}

        //Implement the create account and sign in using FirebaseUI, use sign in using email and sign in using Google

        //If the user was authenticated, send him to RemindersActivity

        // Observe the authentication state so we can know if the user has logged in successfully.
        // If the user has logged in successfully, bring them back to the settings screen.
        // If the user did not log in successfully, display an error message.
        viewModel.authenticationState.observe(this, Observer { authenticationState ->
            when (authenticationState) {
                AuthenticationViewModel.AuthenticationState.AUTHENTICATED -> {
                    startRemindersActivity()
                }
                else -> Log.e(
                    TAG,
                    "Authentication state that doesn't require any UI change $authenticationState"
                )
            }
        })


    }

    //perform the login procedure flow
    private fun startLoginFlow() {
        // Give users the option to sign in / register with their email or Google account. If users
        // choose to register with their email, they will need to create a password as well.
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build()
        )

        // Create and launch sign-in intent. We listen to the response of this activity with the
        // SIGN_IN_RESULT_CODE code.
        startActivityForResult(
            AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(
                providers
            ).build(), LOG_IN_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOG_IN_REQUEST_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK && response != null) startRemindersActivity()
        }
    }

    private fun startRemindersActivity() {
        //launch the RemindersActivity,  and finish the current activity (AuthenticationActivity)
        val intent = Intent(this, RemindersActivity::class.java)
        startActivity(intent)
        finish()    }

}
