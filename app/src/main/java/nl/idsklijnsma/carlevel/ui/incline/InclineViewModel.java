package nl.idsklijnsma.carlevel.ui.incline;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class InclineViewModel extends ViewModel {

    private final MutableLiveData<Integer> mIncline;

    public InclineViewModel() {
        mIncline = new MutableLiveData<>();
        mIncline.setValue(0);
    }

    public LiveData<Integer> getIncline() {
        return mIncline;
    }

    public void setIncline(Integer val) {
        mIncline.postValue(val);
    }
}