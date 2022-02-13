package nl.idsklijnsma.carlevel.ui.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.util.ArrayList;
import java.util.Locale;

import nl.idsklijnsma.carlevel.UIViewModel;
import nl.idsklijnsma.carlevel.UsbDeviceListAdapter;
import nl.idsklijnsma.carlevel.databinding.FragmentConfigBinding;
import nl.idsklijnsma.carlevel.models.UsbItem;
import nl.idsklijnsma.carlevel.ui.level.LevelViewModel;

public class ConfigFragment extends Fragment implements UsbDeviceListAdapter.OnDeviceSelectListener {

    private ConfigViewModel configViewModel;
    private FragmentConfigBinding binding;

    private UsbDeviceListAdapter mAdapter;
    private TextView mTextViewY;
    private TextView mTextViewX;
    private TextInputEditText mInputOffsetX;
    private TextInputEditText mInputOffsetY;

    private SharedPreferences mPrefs;

    private final ArrayList<UsbItem> listItems = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        configViewModel =
                new ViewModelProvider(requireActivity()).get(ConfigViewModel.class);
        UIViewModel uiViewModel = new ViewModelProvider(requireActivity()).get(UIViewModel.class);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        LevelViewModel levelViewModel = new ViewModelProvider(requireActivity()).get(LevelViewModel.class);
        levelViewModel.getLevelX().observe(getViewLifecycleOwner(), this::setLevelX);
        levelViewModel.getLevelY().observe(getViewLifecycleOwner(), this::setLevelY);

        binding = FragmentConfigBinding.inflate(inflater, container, false);
        mTextViewX = binding.txtLevelX;
        mInputOffsetX = binding.inputOffsetX;
        mTextViewY = binding.txtLevelY;
        SwitchCompat mInvertX = binding.switchInvertX;
        SwitchCompat mInvertY = binding.switchInvertY;
        mInputOffsetY = binding.inputOffsetY;
        RecyclerView mRecyclerView = binding.listDevices;
        mRecyclerView.setHasFixedSize(false);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        mAdapter = new UsbDeviceListAdapter(listItems, this);
        mRecyclerView.setAdapter(mAdapter);
        View root = binding.getRoot();

        Button offsetBtn = binding.btnZero;
        Button searchBtn = binding.btnSearch;
        searchBtn.setOnClickListener(v -> scanDevices());
        offsetBtn.setOnClickListener(v -> setOffset());
        mInvertY.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mPrefs.edit()
                    .putBoolean("invertY", isChecked)
                    .apply();
            updateConfig();
        });
        mInvertX.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mPrefs.edit()
                    .putBoolean("invertX", isChecked)
                    .apply();

            updateConfig();
        });
        uiViewModel.setActiveView(UIViewModel.CONFIG);

        mInputOffsetX.setText(String.format(Locale.US, "%d", mPrefs.getInt("offsetX", 0)));
        mInputOffsetY.setText(String.format(Locale.US, "%d", mPrefs.getInt("offsetY", 0)));
        mInvertX.setChecked(mPrefs.getBoolean("invertX", false));
        mInvertY.setChecked(mPrefs.getBoolean("invertY", false));
        return root;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
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
        UsbManager usbManager = (UsbManager) requireActivity().getSystemService(Context.USB_SERVICE);
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
        }
        mAdapter.notifyDataSetChanged();
    }

    void setOffset() {
        mPrefs.edit()
                .putInt("offsetX", Integer.parseInt(mInputOffsetX.getText().toString()))
                .putInt("offsetY", Integer.parseInt(mInputOffsetY.getText().toString()))
                .apply();
        updateConfig();
    }

    private void setLevelX(int value) {
        mTextViewX.setText(Integer.toString(value));
    }

    private void setLevelY(int value) {
        mTextViewY.setText(Integer.toString(value));
    }

    private void updateConfig() {
        LevelConfig cfg = new LevelConfig(
                mPrefs.getInt("offsetX", 0),
                mPrefs.getInt("offsetY", 0),
                mPrefs.getBoolean("invertX", false),
                mPrefs.getBoolean("invertY", false));
        configViewModel.setLevelConfig(cfg);
    }
}