package com.example.jzsef.myapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO make some changes
        Toast.makeText(this,"Uj verzio",Toast.LENGTH_LONG).show();

        // TODO make other changes

    }
}
