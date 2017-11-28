package com.example.kenbo.chess_gui;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    public UsbDevice device;
    public UsbDeviceConnection connection;
    public Button beginNewGame;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().getDecorView().setSystemUiVisibility(          View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button beginNewGame = findViewById(R.id.beginNewGame);
        //usb serial initialization
        TextView statusText = findViewById(R.id.statusText);
        beginNewGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Select Type:");
                builder.setItems(R.array.select_game, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        if(which == 0)

                        {
                            Intent pVAiIntent = new Intent(MainActivity.this, PlayerVsAI.class);
                            pVAiIntent.putExtra("GAME_TYPE", "PAI");
                            startActivity(pVAiIntent);

                        }
                        else
                        {
                            Intent aiVAiIntent = new Intent(MainActivity.this, AIvsAI.class);
                            aiVAiIntent.putExtra("GAME_TYPE", "AIAI");
                            startActivity(aiVAiIntent);
                        }


                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }

        });

    }

protected void OnResume(Bundle savedInstanceState){
    getWindow().getDecorView().setSystemUiVisibility(          View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE);
}



//    public void newGameAction(View v)
//    {
//        Intent intent = new Intent(this, PlayerVsAI.class);
//        startActivity(intent);
//
//    }



}


