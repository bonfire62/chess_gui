package com.example.kenbo.chess_gui;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.felhr.usbserial.usbserial.UsbSerialDevice;

import com.felhr.usbserial.usbserial.UsbSerialDevice;
import com.felhr.usbserial.usbserial.UsbSerialInterface;

public class AIvsAI extends AppCompatActivity {

    UsbDevice device;
    UsbDeviceConnection usbConnection;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aivs_ai);

        //pl2303 device :9600,8,1,None,flow off
        UsbSerialDevice serial = UsbSerialDevice.createUsbSerialDevice(device, usbConnection);
        serial.open();
        serial.setBaudRate(9600);
        serial.setDataBits(UsbSerialInterface.DATA_BITS_8);
        serial.setParity(UsbSerialInterface.PARITY_NONE);
        serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);


    }
}
