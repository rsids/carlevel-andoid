package nl.idsklijnsma.carlevel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class UIViewModel extends ViewModel {

    public static final int LEVEL = 1;
    public static final int INCLINE = 2;
    public static final int CONFIG = 3;

    private final MutableLiveData<Integer> mActive;
    private final MutableLiveData<Boolean> mIsPip;

    public UIViewModel() {
        mActive = new MutableLiveData<>();
        mActive.setValue(1);
        mIsPip = new MutableLiveData<>();
        mIsPip.setValue(false);
    }

    public LiveData<Integer> getActiveView() {
        return mActive;
    }

    public void setActiveView(Integer val) {
        mActive.setValue(val);
    }

    public LiveData<Boolean> getIsPip() {
        return mIsPip;
    }

    public void setIsPip(Boolean val) {
        mIsPip.setValue(val);
    }
}
