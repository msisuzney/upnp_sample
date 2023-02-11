package com.msisuzney.seekbarserver;

import android.util.Log;

import com.msisuzney.common.Constants;

import org.fourthline.cling.binding.annotations.UpnpAction;
import org.fourthline.cling.binding.annotations.UpnpInputArgument;
import org.fourthline.cling.binding.annotations.UpnpOutputArgument;
import org.fourthline.cling.binding.annotations.UpnpService;
import org.fourthline.cling.binding.annotations.UpnpServiceId;
import org.fourthline.cling.binding.annotations.UpnpServiceType;
import org.fourthline.cling.binding.annotations.UpnpStateVariable;

import java.beans.PropertyChangeSupport;

@UpnpService(serviceId = @UpnpServiceId(Constants.SERVICE_ID),
        serviceType = @UpnpServiceType(value = Constants.SERVICE_TYPE, version = 1))
public class SeekBarStatus {

    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);


    public PropertyChangeSupport getPropertyChangeSupport() {
        return propertyChangeSupport;
    }

    @UpnpStateVariable(defaultValue = "0", sendEvents = true)
    private int progress;

    @UpnpAction(out = @UpnpOutputArgument(name = Constants.SERVICE_OUTPUT_ARGUMENT_NAME))
    public int getProgress() {
        return progress;
    }

    @UpnpAction
    public void setProgress(@UpnpInputArgument(name = Constants.SERVICE_INPUT_ARGUMENT_NAME) int progress) {
        int oldProgress = this.progress;
        this.progress = progress;
        Log.d(MainActivity.TAG, "set progress:" + progress);
        propertyChangeSupport.firePropertyChange(Constants.SERVICE_ARGUMENT_NAME, oldProgress, progress);
    }
}
