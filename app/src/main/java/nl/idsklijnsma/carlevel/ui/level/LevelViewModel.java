package nl.idsklijnsma.carlevel.ui.level;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LevelViewModel extends ViewModel {

    private final MutableLiveData<Float> mLevelX;
    private final MutableLiveData<Float> mLevelY;

    private final MutableLiveData<Float> mOffsetX;
    private final MutableLiveData<Float> mOffsetY;

    public LevelViewModel() {
        mLevelX = new MutableLiveData<>();
        mLevelX.setValue(0f);
        mLevelY = new MutableLiveData<>();
        mLevelY.setValue(10f);
        mOffsetX = new MutableLiveData<>();
        mOffsetX.setValue(0f);
        mOffsetY = new MutableLiveData<>();
        mOffsetY.setValue(10f);
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

    public LiveData<Float> getOffsetX() {
        return mOffsetX;
    }
    public LiveData<Float> getOffsetY() {
        return mOffsetY;
    }

    public void setOffsetX(Float val) {
        mOffsetX.postValue(val);
    }
    public void setOffsetY(Float val) {
        mOffsetY.postValue(val);
    }
}