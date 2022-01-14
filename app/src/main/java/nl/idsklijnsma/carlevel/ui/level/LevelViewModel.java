package nl.idsklijnsma.carlevel.ui.level;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LevelViewModel extends ViewModel {

    private final MutableLiveData<Integer> mLevelX;
    private final MutableLiveData<Integer> mLevelY;

    private final MutableLiveData<Integer> mOffsetX;
    private final MutableLiveData<Integer> mOffsetY;

    public LevelViewModel() {
        mLevelX = new MutableLiveData<>();
        mLevelX.setValue(0);
        mLevelY = new MutableLiveData<>();
        mLevelY.setValue(0);
        mOffsetX = new MutableLiveData<>();
        mOffsetX.setValue(0);
        mOffsetY = new MutableLiveData<>();
        mOffsetY.setValue(0);
    }

    public LiveData<Integer> getLevelX() {
        return mLevelX;
    }
    public LiveData<Integer> getLevelY() {
        return mLevelY;
    }

    public void setLevelX(Integer val) {
        mLevelX.postValue(val);
    }
    public void setLevelY(Integer val) {
        mLevelY.postValue(val);
    }

    public LiveData<Integer> getOffsetX() {
        return mOffsetX;
    }
    public LiveData<Integer> getOffsetY() {
        return mOffsetY;
    }

    public void setOffsets(Integer x, Integer y) {
        mOffsetX.postValue(x);
        mOffsetY.postValue(y);
    }
}