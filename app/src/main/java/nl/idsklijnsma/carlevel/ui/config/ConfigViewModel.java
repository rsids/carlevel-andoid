package nl.idsklijnsma.carlevel.ui.config;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.logging.Level;

import nl.idsklijnsma.carlevel.models.UsbItem;

public class ConfigViewModel extends ViewModel {

    private final MutableLiveData<UsbItem> mSelectedDevice;
    private final MutableLiveData<LevelConfig> mLevelConfig;

    public ConfigViewModel() {
        mSelectedDevice = new MutableLiveData<>();
        mLevelConfig = new MutableLiveData<>();
    }

    public LiveData<UsbItem> selectedDevice() {
        return mSelectedDevice;
    }
    public LiveData<LevelConfig> levelConfig() {
        return mLevelConfig;
    }

    public void selectDevice(UsbItem device) {
        mSelectedDevice.setValue(device);
    }
    public void setLevelConfig(LevelConfig cfg) {
        mLevelConfig.setValue(cfg);
    }
}