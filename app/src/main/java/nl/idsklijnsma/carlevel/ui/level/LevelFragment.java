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
import nl.idsklijnsma.carlevel.ui.config.ConfigViewModel;

public class LevelFragment extends Fragment {

    private FragmentLevelBinding mBinding;
    private TextView mTextViewY;
    private TextView mTextViewX;
    private ImageView mImgLevelX;
    private ImageView mImgLevelY;

    private boolean isInvertedX = false;
    private boolean isInvertedY = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        LevelViewModel levelViewModel = new ViewModelProvider(requireActivity()).get(LevelViewModel.class);
        ConfigViewModel configViewModel = new ViewModelProvider(requireActivity()).get(ConfigViewModel.class);

        UIViewModel uiViewModel = new ViewModelProvider(requireActivity()).get(UIViewModel.class);

        mBinding = FragmentLevelBinding.inflate(inflater, container, false);
        View root = mBinding.getRoot();
        mTextViewX = mBinding.txtLevelX;
        mTextViewY = mBinding.txtLevelY;
        mImgLevelX = mBinding.imgLevelX;
        mImgLevelY = mBinding.imgLevelY;
        levelViewModel.getLevelX().observe(getViewLifecycleOwner(), this::setLevelX);
        levelViewModel.getLevelY().observe(getViewLifecycleOwner(), this::setLevelY);
        configViewModel.levelConfig().observe(getViewLifecycleOwner(), levelConfig -> {
            isInvertedX = levelConfig.isInvertX();
            isInvertedY = levelConfig.isInvertY();
        });
        uiViewModel.setActiveView(UIViewModel.LEVEL);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    private void setLevelX(int value) {
        int val = isInvertedX ? value * -1 : value;
        mTextViewX.setText(val + "º");
        mImgLevelX.setRotation(val);
    }

    private void setLevelY(int value) {
        int val = isInvertedY ? value * -1 : value;
        mTextViewY.setText(val + "º");
        mImgLevelY.setRotation(val);
    }
}