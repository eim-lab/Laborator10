
package ro.pub.cs.systems.eim.lab10.siplinphone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.linphone.core.*

class MainActivity:  AppCompatActivity() {
    private lateinit var core: Core

    private val coreListener = object: CoreListenerStub() {
        override fun onAccountRegistrationStateChanged(core: Core, account: Account, state: RegistrationState?, message: String) {
            findViewById<TextView>(R.id.registration_status).text = message

            if (state == RegistrationState.Failed) {
                findViewById<Button>(R.id.register).isEnabled = true
            } else  if (state == RegistrationState.Cleared) {
                findViewById<LinearLayout>(R.id.register_layout).visibility = View.VISIBLE
                findViewById<RelativeLayout>(R.id.call_layout).visibility = View.GONE
                findViewById<Button>(R.id.register).isEnabled = true
            } else if (state == RegistrationState.Ok) {
                findViewById<LinearLayout>(R.id.register_layout).visibility = View.GONE
                findViewById<RelativeLayout>(R.id.call_layout).visibility = View.VISIBLE
                findViewById<Button>(R.id.unregister).isEnabled = true
            }
        }

        override fun onAudioDeviceChanged(core: Core, audioDevice: AudioDevice) {
            // This callback will be triggered when a successful audio device has been changed
        }

        override fun onAudioDevicesListUpdated(core: Core) {
            // This callback will be triggered when the available devices list has changed,
            // for example after a bluetooth headset has been connected/disconnected.
        }

        override fun onCallStateChanged(
            core: Core,
            call: Call,
            state: Call.State?,
            message: String
        ) {
            findViewById<TextView>(R.id.call_status).text = message

            // When a call is received
            when (state) {
                // I N C O M I N G
                Call.State.IncomingReceived -> {
                    findViewById<Button>(R.id.hang_up).isEnabled = true
                    findViewById<Button>(R.id.answer).isEnabled = true

                    val remoteAddress = call.remoteAddressAsString
                    if (remoteAddress != null)
                        findViewById<EditText>(R.id.remote_address).setText(
                            call.remoteAddressAsString ?: "unknown"
                        )
                }
                Call.State.Connected -> {
                    findViewById<Button>(R.id.mute_mic).isEnabled = true
                    findViewById<Button>(R.id.toggle_speaker).isEnabled = true
                }
                Call.State.Released -> {
                    findViewById<Button>(R.id.hang_up).isEnabled = false
                    findViewById<Button>(R.id.answer).isEnabled = false
                    findViewById<Button>(R.id.mute_mic).isEnabled = false
                    findViewById<Button>(R.id.toggle_speaker).isEnabled = false
                    findViewById<EditText>(R.id.remote_address).text.clear()
                    findViewById<Button>(R.id.call).isEnabled = true
                }

                // O U T G O I N G
                Call.State.OutgoingInit -> {
                    // First state an outgoing call will go through
                }
                Call.State.OutgoingProgress -> {
                    // Right after outgoing init
                }
                Call.State.OutgoingRinging -> {
                    // This state will be reached upon reception of the 180 RINGING
                }
                Call.State.Connected -> {
                    // When the 200 OK has been received
                }
                Call.State.StreamsRunning -> {
                    // This state indicates the call is active.
                    // You may reach this state multiple times, for example after a pause/resume
                    // or after the ICE negotiation completes
                    // Wait for the call to be connected before allowing a call update
                }
                Call.State.Paused -> {
                    // When you put a call in pause, it will became Paused
                }
                Call.State.PausedByRemote -> {
                    // When the remote end of the call pauses it, it will be PausedByRemote
                }
                Call.State.Updating -> {
                    // When we request a call update, for example when toggling video
                }
                Call.State.UpdatedByRemote -> {
                    // When the remote requests a call update
                }
                Call.State.Released -> {
                    // Call state will be released shortly after the End state
                    findViewById<EditText>(R.id.remote_address).isEnabled = true
                    findViewById<Button>(R.id.call).isEnabled = true
                    findViewById<Button>(R.id.hang_up).isEnabled = false
                }
                Call.State.Error -> {

                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)

        val factory = Factory.instance()
        factory.setDebugMode(true, "Hello Linphone")
        core = factory.createCore(null, null, this)

        findViewById<Button>(R.id.register).setOnClickListener {
            login()
            it.isEnabled = false
        }

        findViewById<Button>(R.id.hang_up).isEnabled = false
        findViewById<Button>(R.id.answer).isEnabled = false
        findViewById<Button>(R.id.mute_mic).isEnabled = false
        findViewById<Button>(R.id.toggle_speaker).isEnabled = false
        findViewById<EditText>(R.id.remote_address).isEnabled = true


        findViewById<Button>(R.id.answer).setOnClickListener {
            // if we wanted, we could create a CallParams object
            // and answer using this object to make changes to the call configuration
            // (see OutgoingCall tutorial)
            core.currentCall?.accept()
        }

        findViewById<Button>(R.id.mute_mic).setOnClickListener {
            // The following toggles the microphone, disabling completely / enabling the sound capture
            // from the device microphone
            core.setMicEnabled(!core.isMicEnabled())

        }

        findViewById<Button>(R.id.toggle_speaker).setOnClickListener {
            toggleSpeaker()
        }


        findViewById<Button>(R.id.call).setOnClickListener {
            outgoingCall()
            findViewById<EditText>(R.id.remote_address).isEnabled = false
            it.isEnabled = false
            findViewById<Button>(R.id.hang_up).isEnabled = true
            //findViewById<Button>(R.id.unregister).isEnabled = true

        }

        findViewById<Button>(R.id.hang_up).setOnClickListener {

            findViewById<EditText>(R.id.remote_address).isEnabled = true
            findViewById<Button>(R.id.call).isEnabled = true

            if (core.callsNb != 0) {
                // If the call state isn't paused, we can get it using core.currentCall
                val call = if (core.currentCall != null) core.currentCall else core.calls[0]
                // Terminating a call is quite simple
                if(call != null)
                    call.terminate()
            }
        }

        findViewById<Button>(R.id.unregister).setOnClickListener {
            // Here we will disable the registration of our Account
            val account = core.defaultAccount
            if(account != null) {

                val params = account.params
                // Returned params object is const, so to make changes we first need to clone it
                val clonedParams = params.clone()

                // Now let's make our changes
                clonedParams.setRegisterEnabled(false)

                // And apply them
                account.params = clonedParams

                it.isEnabled = false
            }
        }


    }

    private fun toggleSpeaker() {
        // Get the currently used audio device
        val currentAudioDevice = core.currentCall?.outputAudioDevice
        val speakerEnabled = currentAudioDevice?.type == AudioDevice.Type.Speaker

        // We can get a list of all available audio devices using
        // Note that on tablets for example, there may be no Earpiece device
        for (audioDevice in core.audioDevices) {
            if (speakerEnabled && audioDevice.type == AudioDevice.Type.Earpiece) {
                core.currentCall?.outputAudioDevice = audioDevice
                return
            } else if (!speakerEnabled && audioDevice.type == AudioDevice.Type.Speaker) {
                core.currentCall?.outputAudioDevice = audioDevice
                return
            }/* If we wanted to route the audio to a bluetooth headset
            else if (audioDevice.type == AudioDevice.Type.Bluetooth) {
                core.currentCall?.outputAudioDevice = audioDevice
            }*/
        }
    }


    private fun outgoingCall() {
        // As for everything we need to get the SIP URI of the remote and convert it to an Address
        val remoteSipUri = findViewById<EditText>(R.id.remote_address).text.toString()
        val remoteAddress = Factory.instance().createAddress("sip:" + remoteSipUri)
        remoteAddress ?: return // If address parsing fails, we can't continue with outgoing call process

        // We also need a CallParams object
        // Create call params expects a Call object for incoming calls, but for outgoing we must use null safely
        val params = core.createCallParams(null)
        params ?: return // Same for params

        // We can now configure it
        // Here we ask for no encryption but we could ask for ZRTP/SRTP/DTLS
        params.mediaEncryption = MediaEncryption.None
        // If we wanted to start the call with video directly
        //params.enableVideo(true)

        // Finally we start the call
        core.inviteAddressWithParams(remoteAddress, params)
        // Call process can be followed in onCallStateChanged callback from core listener
    }



    private fun login() {
        val username = findViewById<EditText>(R.id.username).text.toString()
        val password = findViewById<EditText>(R.id.password).text.toString()
        val domain = findViewById<EditText>(R.id.domain).text.toString()
        val transportType = when (findViewById<RadioGroup>(R.id.transport).checkedRadioButtonId) {
            R.id.udp -> TransportType.Udp
            R.id.tcp -> TransportType.Tcp
            else -> TransportType.Tls
        }
        val authInfo = Factory.instance().createAuthInfo(username, null, password, null, null, domain, null)

        val params = core.createAccountParams()
        val identity = Factory.instance().createAddress("sip:$username@$domain")
        params.identityAddress = identity

        val address = Factory.instance().createAddress("sip:$domain")
        address?.transport = transportType
        params.serverAddress = address
        params.setRegisterEnabled(true)
        val account = core.createAccount(params)

        core.addAuthInfo(authInfo)
        core.addAccount(account)

        core.defaultAccount = account
        core.addListener(coreListener)
        core.start()

        // We will need the RECORD_AUDIO permission for video call
        if (packageManager.checkPermission(Manifest.permission.RECORD_AUDIO, packageName) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 0)
            return
        }
    }
}