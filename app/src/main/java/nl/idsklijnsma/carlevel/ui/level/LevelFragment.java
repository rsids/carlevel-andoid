package nl.idsklijnsma.carlevel.ui.level;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import nl.idsklijnsma.carlevel.UIViewModel;
import nl.idsklijnsma.carlevel.databinding.FragmentLevelBinding;

public class LevelFragment extends Fragment {

    private FragmentLevelBinding mBinding;
    private TextView mTextViewY;
    private TextView mTextViewX;
    private ImageView mImgLevelX;
    private ImageView mImgLevelY;

    private float offsetX = 0;
    private float offsetY = 0;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        LevelViewModel levelViewModel = new ViewModelProvider(requireActivity()).get(LevelViewModel.class);

        UIViewModel uiViewModel = new ViewModelProvider(requireActivity()).get(UIViewModel.class);

        mBinding = FragmentLevelBinding.inflate(inflater, container, false);
        View root = mBinding.getRoot();
        mTextViewX = mBinding.txtLevelX;
        mTextViewY = mBinding.txtLevelY;
        mImgLevelX = mBinding.imgLevelX;
        mImgLevelY = mBinding.imgLevelY;
        levelViewModel.getLevelX().observe(getViewLifecycleOwner(), this::setLevelX);
        levelViewModel.getLevelY().observe(getViewLifecycleOwner(), this::setLevelY);
        levelViewModel.getOffsetX().observe(getViewLifecycleOwner(), s -> offsetX = s);
        levelViewModel.getOffsetY().observe(getViewLifecycleOwner(), s -> offsetY = s);
        uiViewModel.setActiveView(UIViewModel.LEVEL);
        Log.d("CARLVL", "onCreateView Level");
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    private void setLevelX(Float value) {
        float formatted = (value < 180 ? value : value - 360) - offsetX;
        mTextViewX.setText(String.format("%.0fº", formatted));
        mImgLevelX.setRotation(value - offsetX);
    }

    private void setLevelY(Float value) {
        float formatted = (value < 180 ? value : value - 360) - offsetY;
        mTextViewY.setText(String.format("%.0fº", formatted));
        mImgLevelY.setRotation(value - offsetY);
    }
}