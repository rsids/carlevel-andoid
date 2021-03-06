package nl.idsklijnsma.carlevel.ui.incline;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class InclineViewModel extends ViewModel {

    private MutableLiveData<String> mIncline;

    public InclineViewModel() {
        mIncline = new MutableLiveData<>();
        mIncline.setValue("24");
    }

    public LiveData<String> getIncline() {
        return mIncline;
    }

    public void setIncline(String val) {
        mIncline.setValue(val);
    }
}