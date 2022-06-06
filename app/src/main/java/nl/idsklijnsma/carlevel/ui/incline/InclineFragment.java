package nl.idsklijnsma.carlevel.ui.incline;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import nl.idsklijnsma.carlevel.UIViewModel;
import nl.idsklijnsma.carlevel.databinding.FragmentInclineBinding;
import nl.idsklijnsma.carlevel.ui.config.ConfigViewModel;
import nl.idsklijnsma.carlevel.ui.level.LevelViewModel;

public class InclineFragment extends Fragment {

    private FragmentInclineBinding mBinding;
    private TextView textView;
    private ConstraintLayout centeredContainer;
    private boolean isInvertedY = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        InclineViewModel inclineViewModel = new ViewModelProvider(getActivity()).get(InclineViewModel.class);
        ConfigViewModel configViewModel = new ViewModelProvider(requireActivity()).get(ConfigViewModel.class);

        UIViewModel uiViewModel = new ViewModelProvider(requireActivity()).get(UIViewModel.class);

        mBinding = FragmentInclineBinding.inflate(inflater, container, false);
        View root = mBinding.getRoot();
        textView = mBinding.textIncline;
        centeredContainer = mBinding.centeredContainer;
        inclineViewModel.getIncline().observe(getViewLifecycleOwner(),this::setIncline);
        configViewModel.levelConfig().observe(getViewLifecycleOwner(), levelConfig -> {
            isInvertedY = levelConfig.isInvertY();
        });
        uiViewModel.setActiveView(UIViewModel.INCLINE);
        uiViewModel.getIsPip().observe(getViewLifecycleOwner(), this::setPipMode);

        return root;
    }


    private void setIncline(int value) {
        int val = isInvertedY ? value * -1 : value;
        textView.setText(val + "");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    private void setPipMode(Boolean isPip) {
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT);
        if (isPip) {
            textView.setTextSize(20);
//            textView.setTranslationY(-16);
            params.setMargins(0,0,0,0);
        } else {
            textView.setTextSize(54);
            textView.setTranslationY(0);
            params.setMargins(16,8,16,8);
        }
        centeredContainer.setLayoutParams(params);
    }


}