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
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Date;
import java.util.Set;

public class PlayerVsAI extends AppCompatActivity {

    /*
     * Notifications from UsbService will be received here.
     */
    /*TODO
    uart commands to implement
    Add button and parsing to view current board state grid

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
    public StringBuilder serialBuffer = new StringBuilder();
    public StringBuilder logBuffer = new StringBuilder();

    Button endTurnButton;
    Button endGameButton;
    Button castleButton;
    Button captureButton;
    Button logButton;
    File logfile;
    Boolean gameOver;
    Button debugButton;

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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//
//        getWindow().getDecorView().setSystemUiVisibility(          View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                | View.SYSTEM_UI_FLAG_FULLSCREEN
//                | View.SYSTEM_UI_FLAG_IMMERSIVE);
        gameOver = false;
        mHandler = new MyHandler(this);

        //serial for create game

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pai);
        //find ui elements
        statusTextview = findViewById(R.id.statusText);
        timer = findViewById(R.id.countdownText);

        endTurnButton = findViewById(R.id.endTurn);
        endGameButton = findViewById(R.id.endGameButton);
        logButton = findViewById(R.id.logButton);
        captureButton = findViewById(R.id.captureButton);
        castleButton = findViewById(R.id.castleButton);
        debugButton = findViewById(R.id.debugButton);

        final Button startGameButton = findViewById(R.id.startButton);

        endTurnButton.setEnabled(false);
        endGameButton.setEnabled(false);
        castleButton.setEnabled(false);
        captureButton.setEnabled(false);
        debugButton.setVisibility(View.GONE);

        //begin game
        startGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                countDownTimer.start();
                startGameButton.setVisibility(View.GONE);

                if (usbService != null) {
                    usbService.write("1 h\n\r".getBytes());
                }
                endGameButton.setEnabled(true);
                endTurnButton.setEnabled(true);
                castleButton.setEnabled(true);
                captureButton.setEnabled(true);
                debugButton.setVisibility(View.VISIBLE);

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
                });logDialog.show();
                if(logBuffer.length() > 3000){
                    logBuffer.delete(3000, logBuffer.length());
                }


            }
        });

        //debug button
        debugButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(PlayerVsAI.this);
                builder.setTitle("Debug Menu");
                builder.setNegativeButton("Back", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        });
                builder.setItems(R.array.debug_menu, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        if (which == 0) {
                        endGameButton.setEnabled(true);
                        endTurnButton.setEnabled(true);
                        }

                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });


        //endturn button
        endTurnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (usbService != null) {
                    usbService.write("2\n\r".getBytes());
                }
                countDownTimer.cancel();
                statusTextview.setText("Turn submitted! Waiting for Opponent...");
                endGameButton.setEnabled(false);
                endTurnButton.setEnabled(false);
                captureButton.setEnabled(true);
                castleButton.setEnabled(true);
                timer.setText("");

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

        //castlingButton
        castleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AlertDialog castleDialog = new AlertDialog.Builder(PlayerVsAI.this).create();
                castleDialog.setMessage("Move king, then press next");
                castleDialog.setButton(castleDialog.BUTTON_POSITIVE, "Next", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        usbService.write("A k\n\r".getBytes());
                        castleDialog.hide();
                        showcastleDialog();

                    }

                });
                castleDialog.show();
            }

        });

        //capture button
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AlertDialog captureDialog = new AlertDialog.Builder(PlayerVsAI.this).create();
                captureDialog.setMessage("Remove Opponent Piece, then press Next, move piece, then end turn");
                captureDialog.setButton(captureDialog.BUTTON_POSITIVE, "Next", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        usbService.write("A c\n\r".getBytes());
                        //TODO write showcastleDialog
                    }
                });
                captureDialog.show();
            }
        });


        //conter begin
        countDownTimer = new CountDownTimer(999000, 1000) {
            @Override
            public void onTick(long l) {
                timer.setText(String.valueOf(l / 1000));
            }

            @Override
            public void onFinish() {
                timer.setText("Turn Over!");
                endTurnButton.setEnabled(false);
                endGameButton.setEnabled(false);
                if(gameOver == false) {
                    usbService.write("6\n\r".getBytes());
                    gameOver = true;
                }
                statusTextview.setText("Game Over! Press the back button");

            }
        };


    }



    public void showcastleDialog() {
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
    public void onBackPressed(){
        countDownTimer.cancel();
        if(gameOver == false) {
            usbService.write("6\n\r".getBytes());
            gameOver = true;
        }
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }

    @Override
    public void onPause() {
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
                    mActivity.get().statusTextview.setText(data);
                    //checks for return carraige in serial data
                    //player turn return
                    //mActivity.get().statusTextview.setText(data);
                    // if contains \n, parse as separate command
                    if(mActivity.get().serialBuffer.toString().contains("\n")) {
                        mActivity.get().statusTextview.setText(mActivity.get().serialBuffer.toString());
                        if (mActivity.get().serialBuffer.toString().startsWith("c"))
                        {
                            mActivity.get().statusTextview.setText("Move Recieved from AI, your move!");
                            mActivity.get().endTurnButton.setEnabled(true);
                            mActivity.get().endGameButton.setEnabled(true);
                            mActivity.get().countDownTimer.start();
                            //TODO clear buffer somewhere

                        }
                        mActivity.get().logBuffer.append(mActivity.get().serialBuffer.toString());
                        mActivity.get().serialBuffer.setLength(0);
                    }
//                    mActivity.get().logBuffer.append(mActivity.get().serialBuffer.toString());

                    break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE", Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE", Toast.LENGTH_LONG).show();
                    break;
            }
        }



    }
}
    