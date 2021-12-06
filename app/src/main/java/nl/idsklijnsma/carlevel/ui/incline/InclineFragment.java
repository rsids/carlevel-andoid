package nl.idsklijnsma.carlevel.ui.incline;

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
import nl.idsklijnsma.carlevel.ui.level.LevelViewModel;

public class InclineFragment extends Fragment {

    private FragmentInclineBinding mBinding;
    private TextView textView;
    private ImageView inclineImage;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        LevelViewModel levelViewModel = new ViewModelProvider(getActivity()).get(LevelViewModel.class);

        UIViewModel uiViewModel = new ViewModelProvider(requireActivity()).get(UIViewModel.class);

        mBinding = FragmentInclineBinding.inflate(inflater, container, false);
        View root = mBinding.getRoot();
        textView = mBinding.textIncline;
        inclineImage = mBinding.inclineImage;
        levelViewModel.getLevelY().observe(getViewLifecycleOwner(), s -> {
            double incline = Math.tan(Math.toRadians(s)) * 100;
            textView.setText(String.format("%.0f", incline));
        });

        uiViewModel.setActiveView(UIViewModel.INCLINE);
        uiViewModel.getIsPip().observe(getViewLifecycleOwner(), this::setPipMode);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    private void setPipMode(Boolean isPip) {
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT);
        if (isPip) {
            actionBar.hide();
            textView.setTextSize(20);
            textView.setTranslationY(-16);
            params.setMargins(0,0,0,0);
        } else {
            actionBar.show();
            textView.setTextSize(50);
            textView.setTranslationY(0);
            params.setMargins(8,8,8,8);
        }
        inclineImage.setLayoutParams(params);
    }

}