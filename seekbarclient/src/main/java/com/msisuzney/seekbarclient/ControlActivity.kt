package com.msisuzney.seekbarclient

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import com.msisuzney.common.Constants
import org.fourthline.cling.android.AndroidUpnpService
import org.fourthline.cling.controlpoint.ActionCallback
import org.fourthline.cling.controlpoint.SubscriptionCallback
import org.fourthline.cling.model.ServiceReference
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.gena.CancelReason
import org.fourthline.cling.model.gena.GENASubscription
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.meta.Action
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.model.meta.RemoteDevice
import org.fourthline.cling.model.meta.Service
import org.fourthline.cling.model.state.StateVariableValue
import org.fourthline.cling.model.types.InvalidValueException
import org.fourthline.cling.model.types.UDAServiceId
import org.fourthline.cling.model.types.UDN
import org.fourthline.cling.registry.DefaultRegistryListener
import org.fourthline.cling.registry.Registry
import org.fourthline.cling.registry.RegistryListener
import java.lang.Exception

class ControlActivity : Activity() {
    companion object {
        const val TAG = "SeekbarClientControl"
        const val INTENT_UUID = "INTENT_UUID"
    }

    private lateinit var seekBar: SeekBar
    private lateinit var tv: TextView
    private var mUpnpService: AndroidUpnpService? = null
    private val serviceId = UDAServiceId(Constants.SERVICE_ID)
    private var uuid: String? = null
    private var mService: Service<*, *>? = null
    private var mRegistryListener: RegistryListener? = null
    private lateinit var cancel: Button
    private lateinit var subcribe: Button
    private var subscriptionCallback: SubscriptionCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uuid = intent?.getStringExtra(INTENT_UUID)
        if (uuid == null) {
            Log.d(TAG, "uuid is empty")
            finish()
            return
        }
        setContentView(R.layout.activity_control)
        seekBar = findViewById(R.id.seek_bar)
        seekBar.isEnabled = false
        tv = findViewById(R.id.tv)
        cancel = findViewById(R.id.cancel_btn)
        subcribe = findViewById(R.id.subscribe_btn)

        tv.text = "disconnected"

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    Log.d(TAG, "progress $progress")
                    mService?.apply {
                        updateProgress(mUpnpService!!, this, progress)
                    }
                    tv.text = "$progress"
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
        bindService(Intent(this, UpnpService::class.java), mServiceConnection, BIND_AUTO_CREATE)
        cancel.setOnClickListener {
            subscriptionCallback?.end()
        }
        subcribe.setOnClickListener {
            addProgressSubscribe(mUpnpService, mService)
        }
    }

    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            Log.d(TAG, "service connected")
            mUpnpService = iBinder as AndroidUpnpService
            mUpnpService?.let {
                mRegistryListener = createRegistryListener()
                it.registry?.addListener(mRegistryListener)
            }
            runOnUiThread { setupService() }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.d(TAG, "service disconnected")
            runOnUiThread {
                closeService()
                mUpnpService?.registry?.removeListener(mRegistryListener)
            }
        }

        override fun onBindingDied(componentName: ComponentName) {
            Log.d(TAG, "binding died")
            runOnUiThread {
                closeService()
                mUpnpService?.registry?.removeListener(mRegistryListener)
            }
        }

    }


    private fun setupService() {
        val service = mUpnpService?.registry?.getService(ServiceReference(UDN(uuid), serviceId))
        if (service == null) {
            Log.d(TAG, "service not found")
            return
        }
        mService = service
        addProgressSubscribe(mUpnpService, mService)
        seekBar.isEnabled = true
        tv.text = "connected"
    }

    private fun closeService() {
        mService = null
        seekBar.isEnabled = true
        tv.text = "disconnected"
    }

    private fun addProgressSubscribe(
        upnpService: AndroidUpnpService?,
        seekBarService: Service<*, *>?
    ) {
        subscriptionCallback = object : SubscriptionCallback(seekBarService, 10) {
            override fun failed(
                subscription: GENASubscription<out Service<*, *>>?,
                responseStatus: UpnpResponse?,
                exception: Exception?,
                defaultMsg: String?
            ) {
                Log.d(TAG, "failed:${exception}")
                changeUIState(false)
            }

            override fun established(subscription: GENASubscription<out Service<*, *>>?) {
                Log.d(TAG, "established:${subscription}")
                changeUIState(true)
            }

            override fun ended(
                subscription: GENASubscription<out Service<*, *>>?,
                reason: CancelReason?,
                responseStatus: UpnpResponse?
            ) {
                Log.d(TAG, "ended")
                changeUIState(false)
            }

            override fun eventReceived(subscription: GENASubscription<out Service<*, *>>?) {
                val values: Map<String, StateVariableValue<*>> =
                    subscription?.currentValues as Map<String, StateVariableValue<*>>
                val process = values[Constants.SERVICE_ARGUMENT_NAME] as StateVariableValue<*>
                Log.d(TAG, "process received:${process}")
                runOnUiThread {
                    seekBar.progress = process.value as Int
                    tv.text = "${process.value as Int}"
                }
            }

            override fun eventsMissed(
                subscription: GENASubscription<out Service<*, *>>?,
                numberOfMissedEvents: Int
            ) {
                Log.d(TAG, "eventsMissed")
            }
        }
        upnpService?.controlPoint?.execute(subscriptionCallback)
    }

    fun updateProgress(
        upnpService: AndroidUpnpService,
        seekbarService: Service<*, *>,
        value: Int
    ) {
        Log.d(TAG, "execute $value")
        val setProgressInvocation = SetProgressActionInvocation(value, seekbarService)
        upnpService.controlPoint.execute(
            object : ActionCallback(setProgressInvocation) {
                override fun success(invocation: ActionInvocation<*>) {
                    Log.d(TAG, "successfully called action")
                }

                override fun failure(
                    invocation: ActionInvocation<*>?,
                    operation: UpnpResponse,
                    defaultMsg: String
                ) {
                    Log.d(TAG, defaultMsg)
                }
            }
        )

    }

    internal class SetProgressActionInvocation(value: Int, service: Service<*, *>) :
        ActionInvocation<Service<Device<*, *, *>, Service<*, *>>>(service.getAction("SetProgress") as Action<Service<Device<*, *, *>, Service<*, *>>>) {
        init {
            try {
                setInput(Constants.SERVICE_INPUT_ARGUMENT_NAME, value)
            } catch (ex: InvalidValueException) {
                ex.printStackTrace()
            }
        }
    }

    fun createRegistryListener(): RegistryListener {
        return object : DefaultRegistryListener() {

            override fun remoteDeviceRemoved(registry: Registry, device: RemoteDevice) {
                val service: Service<*, *>? = device.findService(serviceId)
                if (service != null) {
                    runOnUiThread {
                        closeService()
                    }
                }
            }
        }
    }

    private fun changeUIState(isSubscribed: Boolean) {
        runOnUiThread {
            cancel.isEnabled = isSubscribed
            subcribe.isEnabled = !isSubscribed
        }
    }

}