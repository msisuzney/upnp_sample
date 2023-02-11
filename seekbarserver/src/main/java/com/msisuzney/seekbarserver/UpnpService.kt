package com.msisuzney.seekbarserver

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.msisuzney.common.Constants
import org.fourthline.cling.UpnpServiceConfiguration
import org.fourthline.cling.android.AndroidRouter
import org.fourthline.cling.android.AndroidUpnpService
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration
import org.fourthline.cling.android.AndroidUpnpServiceImpl
import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder
import org.fourthline.cling.model.DefaultServiceManager
import org.fourthline.cling.model.meta.*
import org.fourthline.cling.model.types.UDADeviceType
import org.fourthline.cling.model.types.UDN
import org.fourthline.cling.protocol.ProtocolFactory
import java.lang.Exception
import java.lang.StringBuilder
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*

class UpnpService : AndroidUpnpServiceImpl() {

    private var device: LocalDevice? = null
    override fun onCreate() {
        super.onCreate()
        device = createDevice()
        upnpService.registry.addDevice(device)
    }

    override fun onDestroy() {
        super.onDestroy()
        upnpService.registry.removeDevice(device)
    }

    override fun createConfiguration(): UpnpServiceConfiguration {
        return object : AndroidUpnpServiceConfiguration() {
            override fun getAliveIntervalMillis(): Int {
                return 5 * 1000
            }
        }
    }

    inner class MyBinder(val upnpService: AndroidUpnpService) : android.os.Binder() {
        fun getSeekbarDeviceUDN() = getUniqueSystemIdentifier()
    }


    override fun createRouter(
        configuration: UpnpServiceConfiguration?,
        protocolFactory: ProtocolFactory?,
        context: Context?
    ): AndroidRouter {
        return super.createRouter(configuration, protocolFactory, context)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return MyBinder(binder)
    }

    private fun createDevice(): LocalDevice {
        val identity =
            DeviceIdentity(getUniqueSystemIdentifier())
        val type = UDADeviceType(Constants.DEVICE_TYPE, 1)
        val details = DeviceDetails(
            Constants.FRIENDLY_NAME,
            ManufacturerDetails("CXX"),
            ModelDetails(Constants.DEVICE_TYPE, Constants.FRIENDLY_NAME, "v1")
        )
        val seekBarService =
            AnnotationLocalServiceBinder().read(SeekBarStatus::class.java) as LocalService<SeekBarStatus>
        seekBarService.manager =
            DefaultServiceManager(seekBarService, SeekBarStatus::class.java)
        return LocalDevice(identity, type, details, seekBarService)
    }


    private fun getUniqueSystemIdentifier(): UDN {
        val salt = Constants.FRIENDLY_NAME
        val builder = StringBuilder()
        builder.append(Build.MODEL)
        builder.append(Build.MANUFACTURER)
        return try {
            val hash = MessageDigest.getInstance("MD5").digest(builder.toString().toByteArray())
            UDN(UUID(BigInteger(-1, hash).toLong(), salt.hashCode().toLong()))
        } catch (ex: Exception) {
            UDN(if (ex.message != null) ex.message else "UNKNOWN")
        }
    }

}