package nl.idsklijnsma.carlevel.ui.level;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LevelViewModel extends ViewModel {

    private final MutableLiveData<Float> mLevelX;
    private final MutableLiveData<Float> mLevelY;

    public LevelViewModel() {
        mLevelX = new MutableLiveData<>();
        mLevelX.setValue(0f);
        mLevelY = new MutableLiveData<>();
        mLevelY.setValue(10f);
    }

    public LiveData<Float> getLevelX() {
        return mLevelX;
    }
    public LiveData<Float> getLevelY() {
        return mLevelY;
    }

    public void setLevelX(Float val) {
        mLevelX.postValue(val);
    }

    public void setLevelY(Float val) {
        mLevelY.postValue(val);
    }
}