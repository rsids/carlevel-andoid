package nl.idsklijnsma.carlevel.ui.incline;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import nl.idsklijnsma.carlevel.UIViewModel;
import nl.idsklijnsma.carlevel.databinding.FragmentInclineBinding;
import nl.idsklijnsma.carlevel.ui.level.LevelViewModel;

public class InclineFragment extends Fragment {

    private LevelViewModel levelViewModel;
    private UIViewModel uiViewModel;
    private FragmentInclineBinding mBinding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        levelViewModel =
                new ViewModelProvider(getActivity()).get(LevelViewModel.class);

        uiViewModel = new ViewModelProvider(requireActivity()).get(UIViewModel.class);

        mBinding = FragmentInclineBinding.inflate(inflater, container, false);
        View root = mBinding.getRoot();

        final TextView textView = mBinding.textIncline;
        levelViewModel.getLevelY().observe(getViewLifecycleOwner(), s -> {
            double incline = Math.tan(Math.toRadians(s)) * 100;
            textView.setText(String.format("%.0f", incline));
        });
        uiViewModel.getIsPip().observe(getViewLifecycleOwner(), this::setPipMode);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    private void setPipMode(Boolean isPip ) {
        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        if(isPip) {
            actionBar.hide();
        } else {
            actionBar.show();
        }
    }

}