package nl.idsklijnsma.carlevel.ui.config;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.util.ArrayList;

import nl.idsklijnsma.carlevel.UIViewModel;
import nl.idsklijnsma.carlevel.UsbDeviceListAdapter;
import nl.idsklijnsma.carlevel.databinding.FragmentConfigBinding;
import nl.idsklijnsma.carlevel.models.UsbItem;

public class ConfigFragment extends Fragment implements UsbDeviceListAdapter.OnDeviceSelectListener {

    private ConfigViewModel configViewModel;
    private UIViewModel uiViewModel;
    private FragmentConfigBinding binding;

    private RecyclerView mRecyclerView;
    private UsbDeviceListAdapter mAdapter;

    private final ArrayList<UsbItem> listItems = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        configViewModel =
                new ViewModelProvider(requireActivity()).get(ConfigViewModel.class);
        uiViewModel = new ViewModelProvider(requireActivity()).get(UIViewModel.class);

        binding = FragmentConfigBinding.inflate(inflater, container, false);

        mRecyclerView = binding.listBluetooth;
        mRecyclerView.setHasFixedSize(false);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        mAdapter = new UsbDeviceListAdapter(listItems, this);
        mRecyclerView.setAdapter(mAdapter);
        View root = binding.getRoot();

        Button searchBtn = binding.btnSearch;
        searchBtn.setOnClickListener(v -> scanDevices());

//        final TextView textView = binding.textNotifications;
//        configViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
//            @Override
//            public void onChanged(@Nullable String s) {
//                textView.setText(s);
//            }
//        });

//        configViewModel.deviceAdded().observe(getViewLifecycleOwner(), s -> {
//            mDevices.add(s);
//            mAdapter.notifyItemInserted(mDevices.size() - 1);
//        });
//        configViewModel.isScanning().observe(getViewLifecycleOwner(), isScanning -> {
//            if (isScanning) {
//                int l = mDevices.size();
//                if (l > 0) {
//                    mDevices.clear();
//                    mAdapter.notifyItemRangeRemoved(0, l);
//                }
//            }
//            mProgress.setVisibility(isScanning ? View.VISIBLE : View.INVISIBLE);
//        });
        uiViewModel.setActiveView(UIViewModel.CONFIG);
        return root;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
//        try {
//            startScanListener = (StartScanListener) context;
//        } catch (ClassCastException castException) {
//            // nothing
//        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDeviceSelect(int position) {
        configViewModel.selectDevice(listItems.get(position));
    }

    void scanDevices() {
        UsbManager usbManager = (UsbManager) getActivity().getSystemService(Context.USB_SERVICE);
        UsbSerialProber usbDefaultProber = UsbSerialProber.getDefaultProber();
        listItems.clear();
        for (UsbDevice device : usbManager.getDeviceList().values()) {
            UsbSerialDriver driver = usbDefaultProber.probeDevice(device);
            if (driver != null) {
                for (int port = 0; port < driver.getPorts().size(); port++)
                    listItems.add(new UsbItem(device, port, driver));
            } else {
                listItems.add(new UsbItem(device, 0, null));
            }
            mAdapter.notifyItemInserted(listItems.size() - 1);
        }
    }
}