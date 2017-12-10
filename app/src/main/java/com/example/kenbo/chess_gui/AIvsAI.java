package com.example.kenbo.chess_gui;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
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
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.felhr.usbserial.usbserial.SerialBuffer;

import java.lang.ref.WeakReference;
import java.util.Set;

public class AIvsAI extends AppCompatActivity {

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
    Button endTurn;
    Button logButton;
    Button endGameButton;
    Boolean gameOver;
    UsbService usbService;
    TextView statusTextView;
    MyHandler mHandler;
    String serialOut;
    StringBuilder serialBuffer = new StringBuilder();
    StringBuilder logBuffer = new StringBuilder();

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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE);

        mHandler = new MyHandler(AIvsAI.this);


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aiai);

        statusTextView = findViewById(R.id.statusText_aiai);
        logButton = findViewById(R.id.logButton_aiai);
        endGameButton = findViewById(R.id.endGameButton_aiai);
        final Button startGameButton = findViewById(R.id.startButton_aiai);
        statusTextView.setMovementMethod(new ScrollingMovementMethod());

        endGameButton.setVisibility(View.GONE);
        gameOver = false;
        //log button
        logButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AlertDialog logDialog = new AlertDialog.Builder(AIvsAI.this).create();
                logDialog.setMessage(logBuffer.toString());
                logDialog.setTitle("Log");
                logDialog.setButton(Dialog.BUTTON_POSITIVE, "Hide", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        logDialog.hide();
                    }
                });
                logDialog.show();
            }
        });
        //endGameButton
        endGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (usbService != null) {
                    if(gameOver == false) {
                        usbService.write("6\n\r".getBytes());
                        gameOver = true;
                    }
                    finish();
                }
                //TODO write dialog for end game to reset pieces
            }
        });

        //startGameButton
        startGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(usbService != null){
                    usbService.write("1 a,a\n\r".getBytes());
                    statusTextView.append("1 a,a");
                    startGameButton.setVisibility(View.GONE);
                    endGameButton.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void onResume(){
        super.onResume();
        getWindow().getDecorView().setSystemUiVisibility(          View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE);
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
        private final WeakReference<AIvsAI> mActivity;

        public MyHandler(AIvsAI activity) {
            mActivity = new WeakReference<>(activity);
        }


        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;
                    mActivity.get().serialBuffer.append(data);
                    //checks for return carraige in serial data
                        //checks for return carraige in serial data
                        //player turn return

                        if(mActivity.get().serialBuffer.toString().contains("\n")) {
                            mActivity.get().statusTextView.append(mActivity.get().serialBuffer.toString());
                            mActivity.get().logBuffer.append(mActivity.get().serialBuffer.toString());
                            mActivity.get().serialBuffer = new StringBuilder();
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

    }

}
