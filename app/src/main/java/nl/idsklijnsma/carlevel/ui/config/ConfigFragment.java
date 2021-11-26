package nl.idsklijnsma.carlevel.ui.config;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.LinkedList;
import java.util.List;

import nl.idsklijnsma.carlevel.BluetoothDeviceListAdapter;
import nl.idsklijnsma.carlevel.StartScanListener;
import nl.idsklijnsma.carlevel.UIViewModel;
import nl.idsklijnsma.carlevel.databinding.FragmentConfigBinding;

public class ConfigFragment extends Fragment implements BluetoothDeviceListAdapter.OnDeviceSelectListener {

    private ConfigViewModel configViewModel;
    private UIViewModel uiViewModel;
    private FragmentConfigBinding binding;

    private ProgressBar mProgress;
    private RecyclerView mRecyclerView;
    private BluetoothDeviceListAdapter mAdapter;

    private StartScanListener startScanListener;
    private List<BluetoothDevice> mDevices;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        configViewModel =
                new ViewModelProvider(requireActivity()).get(ConfigViewModel.class);
        uiViewModel = new ViewModelProvider(requireActivity()).get(UIViewModel.class);

        binding = FragmentConfigBinding.inflate(inflater, container, false);

        mProgress = binding.progressBar;
        mRecyclerView = binding.listBluetooth;
        mRecyclerView.setHasFixedSize(false);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        mDevices = new LinkedList<>();
        mAdapter = new BluetoothDeviceListAdapter(mDevices, this);
        mRecyclerView.setAdapter(mAdapter);
        View root = binding.getRoot();

        Button searchBtn = binding.btnSearch;
        searchBtn.setOnClickListener(v -> startScanListener.startScanning());

//        final TextView textView = binding.textNotifications;
//        configViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
//            @Override
//            public void onChanged(@Nullable String s) {
//                textView.setText(s);
//            }
//        });

        configViewModel.deviceAdded().observe(getViewLifecycleOwner(), s -> {
            mDevices.add(s);
            mAdapter.notifyItemInserted(mDevices.size() - 1);
        });
        configViewModel.isScanning().observe(getViewLifecycleOwner(), isScanning -> {
            if (isScanning) {
                int l = mDevices.size();
                if (l > 0) {
                    mDevices.clear();
                    mAdapter.notifyItemRangeRemoved(0, l);
                }
            }
            mProgress.setVisibility(isScanning ? View.VISIBLE : View.INVISIBLE);
        });
        uiViewModel.setActiveView(UIViewModel.CONFIG);
        return root;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            startScanListener = (StartScanListener) context;
        } catch (ClassCastException castException) {
            // nothing
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDeviceSelect(int position) {
        configViewModel.selectDevice(mDevices.get(position));
    }
}