package nl.idsklijnsma.carlevel;

import android.Manifest;
import android.app.PictureInPictureParams;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Rational;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.UUID;

import nl.idsklijnsma.carlevel.databinding.ActivityMainBinding;
import nl.idsklijnsma.carlevel.ui.config.ConfigViewModel;
import nl.idsklijnsma.carlevel.ui.level.LevelViewModel;

public class MainActivity extends AppCompatActivity implements StartScanListener {

    private ActivityMainBinding binding;
    private UIViewModel uiViewModel;
    private LevelViewModel levelViewModel;
    private ConfigViewModel configViewModel;

    private NavController mNavController;

    private static final String TAG = "CarLevel";
    private static final UUID LEVEL_SERVICE = UUID.fromString("ac159216-381a-11ec-8d3d-0242ac130003");
    private static final UUID LEVEL_CHAR = UUID.fromString("b08cd61a-381a-11ec-8d3d-0242ac130003");
    private static final UUID LEVEL_DESC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSION_REQ_CODE = 2;

    private BluetoothAdapter mBluetoothAdapter;
    //    private SparseArray<BluetoothDevice> mDevices;
    private BluetoothDevice mDevice;

    private BluetoothGatt mConnectedGatt;
    private BluetoothLeScanner mBluetoothLeScanner;

    private int mActiveView = UIViewModel.LEVEL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        uiViewModel = new ViewModelProvider(this).get(UIViewModel.class);
        levelViewModel = new ViewModelProvider(this).get(LevelViewModel.class);
        configViewModel = new ViewModelProvider(this).get(ConfigViewModel.class);
        uiViewModel.getActiveView().observe(this, s -> mActiveView = s);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_level, R.id.navigation_incline, R.id.navigation_config)
                .build();

        mNavController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, mNavController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, mNavController);

        // Initializes a Bluetooth adapter. For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        configViewModel.selectedDevice().observe(this, s -> {
            mDevice = s;
            connect();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        clearValues();
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if (mDevice != null) {
            connect();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mStopRunnable);
        mHandler.removeCallbacks(mStartRunnable);
        mBluetoothLeScanner.stopScan(mLeCallback);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mConnectedGatt != null) {
            mConnectedGatt.disconnect();
            mConnectedGatt = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, final @NonNull String[] permissions, final @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION_REQ_CODE: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // We have been granted the Manifest.permission.ACCESS_FINE_LOCATION permission. Now we may proceed with scanning.
                    startScan();
                } else {
                    Toast.makeText(this, R.string.no_required_permission, Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    @Override
    public void onUserLeaveHint() {
        if (mActiveView == UIViewModel.INCLINE) {
            Rational ratio
                    = new Rational(1, 1);
            PictureInPictureParams.Builder
                    pip_Builder
                    = new PictureInPictureParams
                    .Builder();
            pip_Builder.setAspectRatio(ratio).build();
            enterPictureInPictureMode(pip_Builder.build());
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        uiViewModel.setIsPip(isInPictureInPictureMode);
    }

    private Runnable mStopRunnable = this::stopScan;
    private Runnable mStartRunnable = this::startScan;

    private void startScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // When user pressed Deny and still wants to use this functionality, show the rationale
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.d(TAG, "Explain why permission is needed");
            } else {
                Log.d(TAG, "request permission");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_REQ_CODE);
            }

            return;
        } else {
            Log.d(TAG, "checkSelfPermission not needed");
        }
        mBluetoothLeScanner.startScan(mLeCallback);
        configViewModel.setScanning(true);
        mHandler.postDelayed(mStopRunnable, 2500);
    }

    private void stopScan() {
        mBluetoothLeScanner.stopScan(mLeCallback);
        configViewModel.setScanning(false);
    }

    private void clearValues() {
        levelViewModel.setLevelX(0f);
        levelViewModel.setLevelY(0f);
        Log.d(TAG, "clearValues");
    }

    private void connect() {
        Log.i(TAG, "Connecting to " + mDevice.getName());
        mConnectedGatt = mDevice.connectGatt(this, true, mGattCallback, 2);
        mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Connecting to " + mDevice.getName() + "..."));
    }

    // BT Callbacks
    private final ScanCallback mLeCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            Log.i(TAG, "New LE Device: " + device.getName() + " / " + device.getAddress());
//            if (DEVICE_NAME.equals(device.getName())) {
            configViewModel.addDevice(device);
//            }
            super.onScanResult(callbackType, result);
        }
    };

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            // Todo: update
            Log.d(TAG, "Connection state change: " + status + " -> " + newState);
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
                mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Discovering services..."));
            } else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                mHandler.sendEmptyMessage(MSG_CLEAR);
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                gatt.disconnect();
            }
            super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered: " + status);
            //reset();
            //enableNextSensor();
            readSensor(gatt);
//            setNotifyNextSensor(gatt);
            super.onServicesDiscovered(gatt, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicRead: " + status);
            if (LEVEL_CHAR.equals(characteristic.getUuid())) {
                // Read char
                mHandler.sendMessage(Message.obtain(null, MSG_LEVEL, characteristic));
            }
            setNotifyNextSensor(gatt);
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicWrite: " + status);
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged: ");
            if (LEVEL_CHAR.equals(characteristic.getUuid())) {
                // Read char
                mHandler.sendMessage(Message.obtain(null, MSG_LEVEL, characteristic));
            }
            super.onCharacteristicChanged(gatt, characteristic);
        }

        private void readSensor(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic = gatt.getService(LEVEL_SERVICE).getCharacteristic(LEVEL_CHAR);
            gatt.setCharacteristicNotification(characteristic, true);
            gatt.readCharacteristic(characteristic);
        }

        private void setNotifyNextSensor(BluetoothGatt gatt) {
            Log.d(TAG, "setNotifyNextSensor level ");
            BluetoothGattCharacteristic characteristic = gatt.getService(LEVEL_SERVICE).getCharacteristic(LEVEL_CHAR);
            gatt.setCharacteristicNotification(characteristic, true);
            BluetoothGattDescriptor desc = characteristic.getDescriptor(LEVEL_DESC);
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(desc);
            mHandler.sendMessage(Message.obtain(null, MSG_DISMISS, "Setup complete..."));
        }
    };

    private static final int MSG_LEVEL = 101;
    private static final int MSG_PROGRESS = 201;
    private static final int MSG_DISMISS = 202;
    private static final int MSG_CLEAR = 301;
    private Handler mHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            BluetoothGattCharacteristic characteristic;
            switch (msg.what) {
                case MSG_LEVEL:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.w(TAG, "Error obtaining level value");
                        return;
                    }
                    updateLevelValue(characteristic);
                    break;
                case MSG_PROGRESS:
                    Log.d(TAG, "MSG_PROGRESS");
                    configViewModel.setScanning(true);
                    break;
                case MSG_DISMISS:
                    Log.d(TAG, "MSG_DISMISS");
                    configViewModel.setScanning(false);
                    //connected
                    break;
                case MSG_CLEAR:
                    Log.d(TAG, "MSG_CLEAR");
                    clearValues();
                    break;

            }
        }

    };

    private void updateLevelValue(BluetoothGattCharacteristic characteristic) {
        // Todo implement
        String value = new String(characteristic.getValue());
        String[] values = value.split(";");
        levelViewModel.setLevelX(Float.parseFloat(values[0]));
        levelViewModel.setLevelY(Float.parseFloat(values[1]));
        Log.i(TAG, "updateLevelValue " + new String(characteristic.getValue()));

        // Calc incline
        //Math.tan(Float.parseFloat(values[0])) * 100;
    }

    @Override
    public void startScanning() {
        configViewModel.clearDevices();
        startScan();
    }
}