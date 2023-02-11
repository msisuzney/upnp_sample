package com.msisuzney.seekbarclient

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.msisuzney.common.Constants
import org.fourthline.cling.android.AndroidUpnpService
import org.fourthline.cling.controlpoint.ActionCallback
import org.fourthline.cling.controlpoint.SubscriptionCallback
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.gena.CancelReason
import org.fourthline.cling.model.gena.GENASubscription
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.meta.*
import org.fourthline.cling.registry.DefaultRegistryListener
import org.fourthline.cling.registry.Registry
import org.fourthline.cling.registry.RegistryListener
import java.lang.Exception

import org.fourthline.cling.model.state.StateVariableValue
import org.fourthline.cling.model.types.*


class MainActivity : Activity() {
    companion object {
        const val TAG = "SeekbarClient"
    }

    private var mUpnpService: AndroidUpnpService? = null
    private var mRegistryListener: RegistryListener? = null

    private val serviceId = UDAServiceId(Constants.SERVICE_ID)

    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            Log.d(TAG, "service connected")
            mUpnpService = iBinder as AndroidUpnpService
            mUpnpService?.let {
                mRegistryListener = createRegistryListener()
                it.registry?.addListener(mRegistryListener)
                it.controlPoint.search()
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.d(TAG, "service disconnected")
            closeService()
            mUpnpService?.registry?.removeListener(mRegistryListener)
        }

        override fun onBindingDied(componentName: ComponentName) {
            Log.d(TAG, "binding died")
            closeService()
            mUpnpService?.registry?.removeListener(mRegistryListener)
        }
    }


    private lateinit var rv: RecyclerView
    private val deviceList = arrayListOf<RemoteDevice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rv = findViewById(R.id.rv)

        rv.layoutManager = LinearLayoutManager(this)
        rv.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL))
        rv.adapter = MyAdapter()
        bindService(Intent(this, UpnpService::class.java), mServiceConnection, BIND_AUTO_CREATE)

    }


    fun createRegistryListener(): RegistryListener {
        return object : DefaultRegistryListener() {

            override fun remoteDeviceAdded(registry: Registry, device: RemoteDevice) {
                Log.d(TAG, "remote device added:${device}")
                runOnUiThread {
                    deviceList.add(device)
                    rv.adapter?.notifyDataSetChanged()
                }
            }

            override fun remoteDeviceRemoved(registry: Registry, device: RemoteDevice) {
                Log.d(TAG, "remote device removed:${device}")
                runOnUiThread {
                    deviceList.remove(device)
                    rv.adapter?.notifyDataSetChanged()
                }
            }
        }
    }

    inner class MyVH(textView: TextView) : RecyclerView.ViewHolder(textView) {

    }

    inner class MyAdapter : RecyclerView.Adapter<MyVH>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyVH {
            return MyVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item, parent, false) as TextView
            )
        }

        override fun onBindViewHolder(holder: MyVH, position: Int) {
            if (holder.itemView is TextView) {
                val device = deviceList[position]
                holder.itemView.text = device.displayString
                device.findService(serviceId)?.apply {
                    holder.itemView.setTextColor(Color.BLACK)
                    holder.itemView.setOnClickListener {
                        val intent = Intent(this@MainActivity, ControlActivity::class.java)
                        intent.putExtra(
                            ControlActivity.INTENT_UUID,
                            this.reference.udn.identifierString
                        )
                        this@MainActivity.startActivity(intent)
                    }
                } ?: holder.itemView.setTextColor(Color.GRAY)
            }
        }

        override fun getItemCount() = deviceList.size

    }

    private fun closeService() {
        mUpnpService = null
    }
}