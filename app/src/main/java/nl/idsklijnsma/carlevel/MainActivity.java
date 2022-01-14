package nl.idsklijnsma.carlevel;

import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Rational;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.github.anrimian.acrareportdialog.AcraReportDialog;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Locale;

import nl.idsklijnsma.carlevel.databinding.ActivityMainBinding;
import nl.idsklijnsma.carlevel.models.UsbItem;
import nl.idsklijnsma.carlevel.ui.config.ConfigViewModel;
import nl.idsklijnsma.carlevel.ui.config.LevelConfig;
import nl.idsklijnsma.carlevel.ui.incline.InclineViewModel;
import nl.idsklijnsma.carlevel.ui.level.LevelViewModel;

public class MainActivity extends AppCompatActivity implements SerialInputOutputManager.Listener {
    private final ByteBuffer res = ByteBuffer.allocate(5);

    private enum UsbPermission {Unknown, Requested, Granted, Denied}

    private UIViewModel uiViewModel;
    private LevelViewModel levelViewModel;
    private InclineViewModel inclineViewModel;
    private BroadcastReceiver broadcastReceiver;
    private int baudRate;
    private boolean connected = false;

    private SerialInputOutputManager usbIoManager;
    private UsbSerialPort usbSerialPort;
    private UsbPermission usbPermission = UsbPermission.Unknown;

    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";

    private static final String TAG = "CarLevelUSB";

    private int mActiveView = UIViewModel.LEVEL;

    private BottomNavigationView navView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AcraReportDialog.setupCrashDialog(getApplication());
        nl.idsklijnsma.carlevel.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (INTENT_ACTION_GRANT_USB.equals(intent.getAction())) {
                    usbPermission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                            ? UsbPermission.Granted : UsbPermission.Denied;
                    if (prefs.contains("device")) {
                        connect();
                    }
                }
            }
        };

        baudRate = 115200;

        uiViewModel = new ViewModelProvider(this).get(UIViewModel.class);
        levelViewModel = new ViewModelProvider(this).get(LevelViewModel.class);
        inclineViewModel = new ViewModelProvider(this).get(InclineViewModel.class);
        ConfigViewModel configViewModel = new ViewModelProvider(this).get(ConfigViewModel.class);
        uiViewModel.getActiveView().observe(this, s -> mActiveView = s);
        configViewModel.selectedDevice().observe(this, this::setSelectedDevice);
        configViewModel.levelConfig().observe(this, this::updateConfig);

        LevelConfig cfg = new LevelConfig(
                prefs.getInt("offsetX", 0),
                prefs.getInt("offsetY", 0),
                prefs.getBoolean("invertX", false),
                prefs.getBoolean("invertY", false));
        configViewModel.setLevelConfig(cfg);

        navView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_level, R.id.navigation_incline, R.id.navigation_config)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        if (prefs.contains("device")) {
            connect();
        }
    }

    private void clearValues() {
        levelViewModel.setLevelX(0);
        levelViewModel.setLevelY(0);
        Log.d(TAG, "clearValues");
    }

    private void connect() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        int vendorId = prefs.getInt("vendorId", 0);
        int productId = prefs.getInt("productId", 0);
        int portNum = prefs.getInt("port", 0);
        UsbDevice device = null;
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        for (UsbDevice v : usbManager.getDeviceList().values())
            if (v.getVendorId() == vendorId && v.getProductId() == productId)
                device = v;
        if (device == null) {
            status("connection failed: device not found");
            return;
        }
        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);

        if (driver == null) {
            status("connection failed: no driver for device");
            return;
        }
        if (driver.getPorts().size() < portNum) {
            status("connection failed: not enough ports at device");
            return;
        }
        usbSerialPort = driver.getPorts().get(portNum);
        UsbDeviceConnection usbConnection = usbManager.openDevice(driver.getDevice());
        if (usbConnection == null && usbPermission == UsbPermission.Unknown && !usbManager.hasPermission(driver.getDevice())) {
            usbPermission = UsbPermission.Requested;
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(INTENT_ACTION_GRANT_USB), 0);
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
            return;
        }
        if (usbConnection == null) {
            if (!usbManager.hasPermission(driver.getDevice()))
                status("connection failed: permission denied");
            else
                status("connection failed: open failed");
            return;
        }

        try {
            usbSerialPort.open(usbConnection);
            usbSerialPort.setParameters(baudRate, 8, 1, UsbSerialPort.PARITY_NONE);
            usbIoManager = new SerialInputOutputManager(usbSerialPort, this);
            usbIoManager.start();
            status("connected");
            connected = true;
        } catch (Exception e) {
            status("connection failed: " + e.getMessage());
            disconnect();
        }
    }

    private void disconnect() {
        connected = false;
        if (usbIoManager != null) {
            usbIoManager.setListener(null);
            usbIoManager.stop();
        }
        usbIoManager = null;
        try {
            usbSerialPort.close();
        } catch (IOException ignored) {
        }
        usbSerialPort = null;
    }

    /**
     * Sets & stores the selected usb device
     *
     * @param item
     */
    private void setSelectedDevice(UsbItem item) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        prefs.edit()
                .putInt("port", item.port)
                .putInt("vendorId", item.device.getVendorId())
                .putInt("productId", item.device.getProductId())
                .putInt("device", item.device.getDeviceId())
                .apply();
        connect();
    }

    private void status(String s) {
        Log.i(TAG, s);
    }

    private void updateConfig(LevelConfig config) {
        if (usbIoManager != null) {
            byte a = (byte) 0x75;
            a = (byte) (a | (config.isInvertX() ? 1 << 3 : 0));
            a = (byte) (a | (config.isInvertY() ? 1 << 1 : 0));
            Log.i(TAG, String.format("Writing %02x/%02x/%02x/%02x/%02x", a, (byte) config.getOffsetX(), (byte) config.getOffsetY(), 0, 0));
            byte[] bytes = {a, (byte) config.getOffsetX(), (byte) config.getOffsetY(), 0, 0};
            usbIoManager.writeAsync(bytes);
        }
    }

    private void updateLevelValue(byte levelX, byte levelY, byte incline) {
        levelViewModel.setLevelY((int) levelX);
        levelViewModel.setLevelX((int) levelY);
        inclineViewModel.setIncline(String.format(Locale.US, "%d", incline));
        Log.d(TAG, String.format("updateLevelValue x: %d, y: %d, incline: %d", levelX, levelY, incline));
    }


    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(broadcastReceiver, new IntentFilter(INTENT_ACTION_GRANT_USB));
        clearValues();
        if (!connected) {
            connect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    @Override
    public void onUserLeaveHint() {
        if (mActiveView == UIViewModel.INCLINE) {
            Log.i(TAG, "Hide menu");
            navView.setVisibility(View.GONE);

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
        if (!isInPictureInPictureMode) {
            Log.i(TAG, "Show menu");
            navView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onNewData(byte[] data) {
        for (byte datum : data) {
            Log.d("USB-DATA", String.format("%02X", datum));
            if (!res.hasRemaining()) {
                if ((res.get(4) & (1 << 1)) > 0) {
                    // Offset mode
                    Log.i(TAG, String.format("OffsetX: %d, offsetY: %d", res.get(0), res.get(1)));
                } else {
                    updateLevelValue(res.get(0), res.get(1), res.get(2));
                }
                res.clear();
            }
            res.put(datum);
        }

    }

    @Override
    public void onRunError(Exception e) {

    }


    @Override
    protected void onNewIntent(Intent intent) {
        if ("android.hardware.usb.action.USB_DEVICE_ATTACHED".equals(intent.getAction())) {
            Log.d(TAG, "ON INTENT");
        }
        super.onNewIntent(intent);
    }


}