package com.msisuzney.seekbarclient

import android.util.Log
import com.msisuzney.common.Constants
import org.fourthline.cling.UpnpServiceConfiguration
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration
import org.fourthline.cling.android.AndroidUpnpServiceImpl
import org.fourthline.cling.binding.xml.ServiceDescriptorBinder
import org.fourthline.cling.binding.xml.UDA10ServiceDescriptorBinderImpl
import org.fourthline.cling.model.types.*


class UpnpService : AndroidUpnpServiceImpl() {

    override fun onCreate() {
        super.onCreate()
        Log.d(MainActivity.TAG, "created")
    }

    override fun createConfiguration(): UpnpServiceConfiguration {
        return object : AndroidUpnpServiceConfiguration() {

            override fun getRegistryMaintenanceIntervalMillis(): Int {
                return 5000
            }

            override fun getRemoteDeviceMaxAgeSeconds(): Int {
                return 20
            }

//            override fun getExclusiveServiceTypes(): Array<ServiceType> {
//                return arrayOf(UDAServiceType(Constants.SERVICE_TYPE))
//            }

            override fun getServiceDescriptorBinderUDA10(): ServiceDescriptorBinder {
                return UDA10ServiceDescriptorBinderImpl()
            }
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(MainActivity.TAG, "onDestroy")
    }


}