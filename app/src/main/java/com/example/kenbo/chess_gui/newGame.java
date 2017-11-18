package com.example.kenbo.chess_gui;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.felhr.usbserial.usbserial.UsbSerialDevice;
import com.felhr.usbserial.usbserial.PL2303SerialDevice;
import com.felhr.usbserial.usbserial.UsbSerialInterface;
import java.util.HashMap;
import java.util.Map;

public class newGame extends AppCompatActivity {
    TextView statusText;
    TextView timer;
    CountDownTimer countDownTimer;
    Button endTurn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_game);
        statusText = findViewById(R.id.statusText);
        Button endTurn = findViewById(R.id.endTurn);

        onStart(savedInstanceState);


    }
    protected void onStart(Bundle savedInstanceState)
    {
//identify buttons
         timer = findViewById(R.id.countdownText);

        //conter begin
        countDownTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long l) {
                timer.setText(String.valueOf(l/1000));
            }

            @Override
            public void onFinish() {
                timer.setText("Turn Over!");
            }
        }.start();


    }

    public void turnMade(View v)
    {
    countDownTimer.cancel();
    countDownTimer.start();

    }

    public void endGame()
    {

    }

}
    