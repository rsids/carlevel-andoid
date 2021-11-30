package nl.idsklijnsma.carlevel.models;

import android.hardware.usb.UsbDevice;

import com.hoho.android.usbserial.driver.UsbSerialDriver;

public class UsbItem {
    public UsbDevice device;
    public int port;
    public UsbSerialDriver driver;

    public UsbItem(UsbDevice device, int port, UsbSerialDriver driver) {
        this.device = device;
        this.port = port;
        this.driver = driver;
    }


}