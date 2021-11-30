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
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Rational;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.EnumSet;

import nl.idsklijnsma.carlevel.databinding.ActivityMainBinding;
import nl.idsklijnsma.carlevel.models.UsbItem;
import nl.idsklijnsma.carlevel.ui.config.ConfigViewModel;
import nl.idsklijnsma.carlevel.ui.level.LevelViewModel;

public class MainActivity extends AppCompatActivity implements SerialInputOutputManager.Listener {
    private final ByteBuffer res = ByteBuffer.allocate(15);
    @Override
    public void onNewData(byte[] data) {
        for(int i = 0; i < data.length; i++) {
            if(!res.hasRemaining()) {
                try {
                    String value = new String(res.array(), "utf-8").trim();
                    updateLevelValue(value);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                res.clear();
            }
            res.put(data[i]);
        }

    }

    @Override
    public void onRunError(Exception e) {

    }

    private enum UsbPermission {Unknown, Requested, Granted, Denied}

    private ActivityMainBinding binding;
    private UIViewModel uiViewModel;
    private LevelViewModel levelViewModel;
    private ConfigViewModel configViewModel;
    private BroadcastReceiver broadcastReceiver;
    private int deviceId, portNum, baudRate;
    private boolean withIoManager = true;
    private boolean connected = false;

    private Handler mainLooper;
    private ControlLines controlLines;
    private SerialInputOutputManager usbIoManager;
    private UsbSerialPort usbSerialPort;
    private UsbPermission usbPermission = UsbPermission.Unknown;

    private NavController mNavController;
    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";

    private static final String TAG = "CarLevelUSB";

    private int mActiveView = UIViewModel.LEVEL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (INTENT_ACTION_GRANT_USB.equals(intent.getAction())) {
                    usbPermission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                            ? UsbPermission.Granted : UsbPermission.Denied;
                    connect();
                }
            }
        };

        mainLooper = new Handler(Looper.getMainLooper());
        baudRate = 115200;
        controlLines = new ControlLines(binding.getRoot());

        uiViewModel = new ViewModelProvider(this).get(UIViewModel.class);
        levelViewModel = new ViewModelProvider(this).get(LevelViewModel.class);
        configViewModel = new ViewModelProvider(this).get(ConfigViewModel.class);
        uiViewModel.getActiveView().observe(this, s -> mActiveView = s);
        configViewModel.selectedDevice().observe(this, s -> setSelectedDevice(s));

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_level, R.id.navigation_incline, R.id.navigation_config)
                .build();

        mNavController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, mNavController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, mNavController);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if (prefs.contains("device")) {
            connect();
        }
//        try {
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

//    private void connectUsb() throws IOException {
//        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
//        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
//        if (availableDrivers.isEmpty()) {
//            return;
//        }
//
//        // Open a connection to the first available driver.
//        UsbSerialDriver driver = availableDrivers.get(0);
//        UsbDeviceConnection connection;
//        try {
//            connection = manager.openDevice(driver.getDevice());
//        } catch (SecurityException e) {
//            Log.d(TAG, "Cannot connect " + e.getMessage());
//            return;
//        }
//        if (connection == null) {
//            // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
//            return;
//        }
//
//        UsbSerialPort port = driver.getPorts().get(0); // Most devices have just one port (port 0)
//        port.open(connection);
//        port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
//    }

    /**
     * Sets & stores the selected usb device
     *
     * @param item
     */
    private void setSelectedDevice(UsbItem item) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        prefs.edit()
                .putInt("port", item.port)
                .putInt("device", item.device.getDeviceId())
                .apply();
        connect();
    }

    private void connect() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        deviceId = prefs.getInt("device", 0);
        portNum = prefs.getInt("port", 0);
        UsbDevice device = null;
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        for (UsbDevice v : usbManager.getDeviceList().values())
            if (v.getDeviceId() == deviceId)
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
            if (withIoManager) {
                usbIoManager = new SerialInputOutputManager(usbSerialPort, this);
                usbIoManager.start();
            }
            status("connected");
            connected = true;
            controlLines.start();
        } catch (Exception e) {
            status("connection failed: " + e.getMessage());
            disconnect();
        }
    }

    private void disconnect() {
        connected = false;
        controlLines.stop();
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


    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(broadcastReceiver, new IntentFilter(INTENT_ACTION_GRANT_USB));
        clearValues();
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

    private void clearValues() {
        levelViewModel.setLevelX(0f);
        levelViewModel.setLevelY(0f);
        Log.d(TAG, "clearValues");
    }

    private void updateLevelValue(String value) {
        String[] values = value.split(";");
        levelViewModel.setLevelX(Float.parseFloat(values[0]));
        levelViewModel.setLevelY(Float.parseFloat(values[1]));
        Log.i(TAG, "updateLevelValue " + value);
    }


//    private void updateLevelValue(BluetoothGattCharacteristic characteristic) {
//        // Todo implement
//        String value = new String(characteristic.getValue());
//        String[] values = value.split(";");
//        levelViewModel.setLevelX(Float.parseFloat(values[0]));
//        levelViewModel.setLevelY(Float.parseFloat(values[1]));
//        Log.i(TAG, "updateLevelValue " + new String(characteristic.getValue()));
//
//        // Calc incline
//        //Math.tan(Float.parseFloat(values[0])) * 100;
//    }


    @Override
    protected void onNewIntent(Intent intent) {
        if ("android.hardware.usb.action.USB_DEVICE_ATTACHED".equals(intent.getAction())) {
            Log.d(TAG, "ON INTENT");
        }
        super.onNewIntent(intent);
    }

    private void status(String s) {
        Log.i(TAG, s);
    }

    class ControlLines {
        private static final int refreshInterval = 200; // msec

        private final Runnable runnable;
//        private final ToggleButton rtsBtn, ctsBtn, dtrBtn, dsrBtn, cdBtn, riBtn;

        ControlLines(View view) {
            runnable = this::run; // w/o explicit Runnable, a new lambda would be created on each postDelayed, which would not be found again by removeCallbacks

//            rtsBtn = view.findViewById(R.id.controlLineRts);
//            ctsBtn = view.findViewById(R.id.controlLineCts);
//            dtrBtn = view.findViewById(R.id.controlLineDtr);
//            dsrBtn = view.findViewById(R.id.controlLineDsr);
//            cdBtn = view.findViewById(R.id.controlLineCd);
//            riBtn = view.findViewById(R.id.controlLineRi);
//            rtsBtn.setOnClickListener(this::toggle);
//            dtrBtn.setOnClickListener(this::toggle);
        }

        private void toggle(View v) {
//            ToggleButton btn = (ToggleButton) v;
//            if (!connected) {
//                btn.setChecked(!btn.isChecked());
//                Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
//                return;
//            }
//            String ctrl = "";
//            try {
//                if (btn.equals(rtsBtn)) { ctrl = "RTS"; usbSerialPort.setRTS(btn.isChecked()); }
//                if (btn.equals(dtrBtn)) { ctrl = "DTR"; usbSerialPort.setDTR(btn.isChecked()); }
//            } catch (IOException e) {
//                status("set" + ctrl + "() failed: " + e.getMessage());
//            }
        }

        private void run() {
            if (!connected)
                return;
            try {
                EnumSet<UsbSerialPort.ControlLine> controlLines = usbSerialPort.getControlLines();
//                rtsBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.RTS));
//                ctsBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.CTS));
//                dtrBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.DTR));
//                dsrBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.DSR));
//                cdBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.CD));
//                riBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.RI));
                mainLooper.postDelayed(runnable, refreshInterval);
            } catch (IOException e) {
                status("getControlLines() failed: " + e.getMessage() + " -> stopped control line refresh");
            }
        }

        void start() {
            if (!connected)
                return;
            try {
                EnumSet<UsbSerialPort.ControlLine> controlLines = usbSerialPort.getSupportedControlLines();
//                if (!controlLines.contains(UsbSerialPort.ControlLine.RTS)) rtsBtn.setVisibility(View.INVISIBLE);
//                if (!controlLines.contains(UsbSerialPort.ControlLine.CTS)) ctsBtn.setVisibility(View.INVISIBLE);
//                if (!controlLines.contains(UsbSerialPort.ControlLine.DTR)) dtrBtn.setVisibility(View.INVISIBLE);
//                if (!controlLines.contains(UsbSerialPort.ControlLine.DSR)) dsrBtn.setVisibility(View.INVISIBLE);
//                if (!controlLines.contains(UsbSerialPort.ControlLine.CD))   cdBtn.setVisibility(View.INVISIBLE);
//                if (!controlLines.contains(UsbSerialPort.ControlLine.RI))   riBtn.setVisibility(View.INVISIBLE);
                run();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "getSupportedControlLines() failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        void stop() {
            mainLooper.removeCallbacks(runnable);
//            rtsBtn.setChecked(false);
//            ctsBtn.setChecked(false);
//            dtrBtn.setChecked(false);
//            dsrBtn.setChecked(false);
//            cdBtn.setChecked(false);
//            riBtn.setChecked(false);
        }
    }
}