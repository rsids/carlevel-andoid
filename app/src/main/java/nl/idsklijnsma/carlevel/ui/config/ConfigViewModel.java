package nl.idsklijnsma.carlevel.ui.config;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import nl.idsklijnsma.carlevel.models.UsbItem;

public class ConfigViewModel extends ViewModel {

    private final MutableLiveData<UsbItem> mSelectedDevice;

    public ConfigViewModel() {
        mSelectedDevice = new MutableLiveData<>();
    }

    public LiveData<UsbItem> selectedDevice() {
        return mSelectedDevice;
    }

    public void selectDevice(UsbItem device) {
        mSelectedDevice.setValue(device);
    }
}