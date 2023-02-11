package com.msisuzney.seekbarserver

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.SeekBar
import android.widget.TextView
import com.msisuzney.common.Constants
import org.fourthline.cling.android.AndroidUpnpService
import org.fourthline.cling.model.ServiceManager
import org.fourthline.cling.model.meta.LocalService
import org.fourthline.cling.model.types.UDAServiceType

class MainActivity : Activity() {

    companion object {
        const val TAG = "SeekbarDevice"
    }

    private var upnpService: AndroidUpnpService? = null
    private var binder: UpnpService.MyBinder? = null
    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder = service as UpnpService.MyBinder
            upnpService = binder?.upnpService
            val localDevice =
                upnpService?.registry?.getLocalDevice(
                    binder?.getSeekbarDeviceUDN(),
                    true
                ) ?: return

            val localService: LocalService<SeekBarStatus> =
                localDevice.findService(UDAServiceType(Constants.SERVICE_TYPE)) as LocalService<SeekBarStatus>
            localService.manager.implementation.propertyChangeSupport.addPropertyChangeListener {
                if (it.propertyName == ServiceManager.EVENTED_STATE_VARIABLES) return@addPropertyChangeListener
                if (it.propertyName == Constants.SERVICE_ARGUMENT_NAME) {
                    runOnUiThread {
                        seekBar.setProgress(it.newValue as Int, true)
                        tv.text = "${it.newValue as Int}"
                    }
                }
            }
            seekBarController = localService.manager.implementation
        }

        override fun onServiceDisconnected(name: ComponentName?) {

        }

        override fun onBindingDied(name: ComponentName?) {
            super.onBindingDied(name)

        }
    }

    private var seekBarController: SeekBarStatus? = null
    private lateinit var seekBar: SeekBar
    private lateinit var tv: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        seekBar = findViewById(R.id.seek_bar)
        tv = findViewById(R.id.tv)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    seekBarController?.apply { this.progress = progress }
                    tv.text = "$progress"
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
        bindService(Intent(this, UpnpService::class.java), mServiceConnection, BIND_AUTO_CREATE)

    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(mServiceConnection)
        stopService(Intent(this, UpnpService::class.java))
    }
}