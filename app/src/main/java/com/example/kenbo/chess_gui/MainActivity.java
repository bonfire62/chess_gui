package com.example.kenbo.chess_gui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button beginNewGame = findViewById(R.id.beginNewGame);
    }

    public void newGameAction(View v)
    {
        Intent intent = new Intent(this, newGame.class);
        startActivity(intent);
    }
}
