package com.example.kenbo.chess_gui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Set;

public class PlayerVsAI extends AppCompatActivity {

    /*
     * Notifications from UsbService will be received here.
     */
    /*TODO
    uart commands to implement
    0x1 start new game (params: player_type=<human|ai>)
    0x2 end turn (flag for piece promotion) (add checkbox) with popup
    0x6 end game
    0xA end capture/castle
    add logic to capture log 9_foobar
    */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    TextView timer;
    CountDownTimer countDownTimer;
    Button endTurn;
    private UsbService usbService;
    private TextView display;
    private MyHandler mHandler;
    private String serialOut;
    private StringBuilder serialBuffer = new StringBuilder();
    private StringBuilder logBuffer = new StringBuilder();
    private CheckBox promoCheckbox;
    private CheckBox castlingCheckbox;
    private CheckBox captureCheckbox;
    private CheckBox enPassCheckbox;


    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//
//        getWindow().getDecorView().setSystemUiVisibility(          View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                | View.SYSTEM_UI_FLAG_FULLSCREEN
//                | View.SYSTEM_UI_FLAG_IMMERSIVE);
        mHandler = new MyHandler(this);

        //serial for create game

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pai);
        //find ui elements
        display = findViewById(R.id.statusText);
        Button endTurn = findViewById(R.id.endTurn);
        Button endGame = findViewById(R.id.endGameButton);
        Button log = findViewById(R.id.logButton);
        captureCheckbox = findViewById(R.id.checkBox);
        castlingCheckbox = findViewById(R.id.castlingCheckbox);
        promoCheckbox = findViewById(R.id.promoCheckbox);
        enPassCheckbox = findViewById(R.id.enPassCheckbox);

        captureCheckbox.setChecked(false);
        castlingCheckbox.setChecked(false);
        promoCheckbox.setChecked(false);

        AlertDialog logDisplay = new AlertDialog.Builder(PlayerVsAI.this).create();
        logDisplay.setTitle("Log:");
        logDisplay.getButton(R.id.logButton);


        //TODO add action listener for log button


        //endturn button
        endTurn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(usbService != null){
                    //resets on single capture
                        //capture
                        if(captureCheckbox.isChecked()) {
                            if(promoCheckbox.isChecked()) {
                                usbService.write("0xA cp".getBytes());
                            }
                            else{
                                usbService.write("0xA c".getBytes());
                                clearCheckboxes();
                            }
                        }
                        //castling
                        else if(castlingCheckbox.isChecked()) {
                            usbService.write("0xA k".getBytes());
                            clearCheckboxes();
                        }
                        //promotion
                        else if(promoCheckbox.isChecked()) {
                            usbService.write("0xA p".getBytes());
                            clearCheckboxes();
                        }
                        //en passant capture
                        else if(enPassCheckbox.isChecked()){
                            usbService.write("0xA e".getBytes());
                        }

                }
//              clearCheckboxes();
                countDownTimer.cancel();

            }

        });

        //endGameButton
        endGame.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(usbService != null){
                    usbService.write("0x6\n\r".getBytes());
                    finish();
                }
            }
        });

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

    public void clearCheckboxes(){
        promoCheckbox.setChecked(false);
        castlingCheckbox.setChecked(false);
        captureCheckbox.setChecked(false);

    }

    @Override
    public void onResume(){
        super.onResume();
//        getWindow().getDecorView().setSystemUiVisibility(          View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                | View.SYSTEM_UI_FLAG_FULLSCREEN
//                | View.SYSTEM_UI_FLAG_IMMERSIVE);
        setFilters();
        startService(UsbService.class, usbConnection, null);
    }

    @Override
    public void onPause(){
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }


    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }


    /*
   * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
   */
    private static class MyHandler extends Handler {
        private final WeakReference<PlayerVsAI> mActivity;

        public MyHandler(PlayerVsAI activity) {
            mActivity = new WeakReference<>(activity);
        }


        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;
                        mActivity.get().serialBuffer.append(data);
                        //checks for return carraige in serial data
                        if(data.contains("\r"))
                        {
                            mActivity.get().display.setText(mActivity.get().serialBuffer.toString());
                            photonResponse(mActivity.get().toString());
                            mActivity.get().serialBuffer.setLength(0);
                        }
                    break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                    break;
            }
        }
        public void photonResponse(String s)
        {
            //TODO photon responses needed 1. AI turn complete 2. game over 3. move invalid
            switch (s) {
                case("0x4"):
                    mActivity.get().countDownTimer.start();
            }
        }
    }

}
    