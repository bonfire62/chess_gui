package com.example.kenbo.chess_gui;

import android.app.Activity;
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


    private UsbService usbService;
    private TextView statusTextview;
    private MyHandler mHandler;
    private String serialOut;
    private StringBuilder serialBuffer = new StringBuilder();
    private StringBuilder logBuffer = new StringBuilder();
    Button endTurnButton;
    Button endGameButton;
    Button castleButton;
    Button captureButton;

/*

both capture and castle, add a button to say that first step is done (serial send)
capture-
remove opponent piece done ->
(send serial) A c\n
(wait for serial)0 0
move your piece
send serial 2\n
wait for serial 0 0
(boolean flags for both)


 */
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
        statusTextview = findViewById(R.id.statusText);
        timer = findViewById(R.id.countdownText);

        endTurnButton = findViewById(R.id.endTurn);
        endGameButton = findViewById(R.id.endGameButton);
        final Button logButton = findViewById(R.id.logButton);
        captureButton = findViewById(R.id.captureButton);
        castleButton = findViewById(R.id.castleButton);

        final Button startGameButton = findViewById(R.id.startButton);

        endTurnButton.setEnabled(false);
        endGameButton.setEnabled(false);
        castleButton.setEnabled(false);
        captureButton.setEnabled(false);

        //begin game
        startGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                countDownTimer.start();
                startGameButton.setVisibility(View.GONE);

                if (usbService != null) {
                    usbService.write("0x1 p\n".getBytes());
                }
                endGameButton.setEnabled(true);
                endTurnButton.setEnabled(true);
                castleButton.setEnabled(true);
                captureButton.setEnabled(true);
                //setGui(true);

            }
        });


        //TODO add action listener for log button
        //log button
        logButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AlertDialog logDialog = new AlertDialog.Builder(PlayerVsAI.this).create();
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


        //endturn button
        endTurnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (usbService != null) {
                    usbService.write("2".getBytes());
                }
                countDownTimer.cancel();
                statusTextview.setText("Turn submitted! Waiting for Opponent...");
                endGameButton.setEnabled(false);
                endTurnButton.setEnabled(false);
                captureButton.setEnabled(true);
                castleButton.setEnabled(true);


            }

        });

        //endGameButton
        endGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (usbService != null) {
                    usbService.write("0x6\n\r".getBytes());
                    finish();
                }
            }
        });

        //castlingButton
        castleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AlertDialog castleDialog = new AlertDialog.Builder(PlayerVsAI.this).create();
                castleDialog.setMessage("Move king, then press next");
                castleDialog.setButton(castleDialog.BUTTON_POSITIVE, "Next", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        usbService.write("A c\n".getBytes());
                        castleDialog.hide();
                        showcastleDialog();

                    }

                });
                castleDialog.show();
            }

        });

        captureButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                final AlertDialog captureDialog = new AlertDialog.Builder(PlayerVsAI.this).create();
                captureDialog.setMessage("Remove Opponent Piece, then press next");
                captureDialog.setButton(captureDialog.BUTTON_POSITIVE, "Next", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        usbService.write("A c".getBytes());
                    }
                });
            }
        });


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
        };



    }

    public void showcastleDialog(){
        final AlertDialog castleDialog2 = new AlertDialog.Builder(PlayerVsAI.this).create();
        castleDialog2.setMessage("Now move Rook, and touch Submit Turn");
        castleDialog2.setButton(castleDialog2.BUTTON_POSITIVE, "Next", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            castleDialog2.hide();
            final Button endTurnButton = findViewById(R.id.endTurn);
            final Button endGameButton = findViewById(R.id.endGameButton);
            endTurnButton.setEnabled(true);
            endGameButton.setEnabled(true);
            }
        });
        castleDialog2.show();
    }


    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
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
                            mActivity.get().statusTextview.setText(mActivity.get().serialBuffer.toString());
//                            photonResponse(mActivity.get().toString());
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
            String[] split = s.split(" ");
            //player turn return
            if(Integer.getInteger(split[0]) > 15) {
                mActivity.get().logBuffer.append(s + "\n");
            }

            else
                switch (split[0]) {
                    case("0x4"):
                        mActivity.get().statusTextview.setText("Turn Received. Your move!") ;
                        mActivity.get().endGameButton.setEnabled(true);
                        mActivity.get().countDownTimer.start();

                }

        }
    }

}
    