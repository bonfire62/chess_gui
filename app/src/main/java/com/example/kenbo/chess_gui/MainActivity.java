package com.example.kenbo.chess_gui;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public UsbDevice device;
    public UsbDeviceConnection connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button beginNewGame = findViewById(R.id.beginNewGame);
        //usb serial initialization



        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        statusText.setText("USB Connecting...");
        if(!usbDevices.isEmpty())
        {
            boolean keep = true;
            for(Map.Entry<String, UsbDevice> entry : usbDevices.entrySet())
            {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                int devicePID = device.getProductId();
                if(deviceVID != 0x1d6b || (devicePID != 0x0001 || devicePID != 0x0002|| devicePID != 0x0002))
                {
                    //this code assumes only one device has been connected to usb
                    connection = usbManager.openDevice(device);
                    keep = false;
                    statusText.setText("USB connected!");
                }

            }
        }

    }

    public void newGameAction(View v)
    {
        Intent intent = new Intent(this, newGame.class);
        startActivity(intent);

    }
}

