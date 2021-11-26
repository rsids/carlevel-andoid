package nl.idsklijnsma.carlevel.ui.config;

import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.util.SparseArray;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ConfigViewModel extends ViewModel {

    private final MutableLiveData<Boolean> mScanning;
    private final MutableLiveData<BluetoothDevice> mDevice;
    private final MutableLiveData<BluetoothDevice> mSelectedDevice;
    private final SparseArray<BluetoothDevice> mDevices = new SparseArray<>();

    public ConfigViewModel() {
        mScanning = new MutableLiveData<>();
        mScanning.setValue(false);

        mDevice = new MutableLiveData<>();
        mSelectedDevice = new MutableLiveData<>();
    }

    public LiveData<Boolean> isScanning() {
        return mScanning;
    }

    public LiveData<BluetoothDevice> deviceAdded() {
        return mDevice;
    }

    public LiveData<BluetoothDevice> selectedDevice() {
        return mSelectedDevice;
    }

    public void setScanning(Boolean val) {
        Log.d("CarLevel", "Is device scanning? " + val);
        mScanning.postValue(val);
    }

    public void addDevice(BluetoothDevice device) {
        if (mDevices.indexOfKey(device.hashCode()) < 0) {
            Log.d("CarLevel", "Adding device " + device.hashCode());
            mDevices.put(device.hashCode(), device);
            mDevice.postValue(device);
        }
    }

    public void selectDevice(BluetoothDevice device) {
        mSelectedDevice.setValue(device);
    }

    public void clearDevices() {
        mDevices.clear();
//        mDevices.postValue(mDevicesList.clone());
    }
}