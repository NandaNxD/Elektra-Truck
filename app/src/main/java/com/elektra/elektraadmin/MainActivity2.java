package com.elektra.elektraadmin;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity2 extends AppCompatActivity {
    TextView speedTextView,locationTextView,distanceTextView,temperatureTextView,collisionsTextView,humidityTextView;
    Button liveLocationButton;

    void openMap(){
        Intent it=new Intent(getApplicationContext(),MapsActivity.class);
        startActivity(it);
    }


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        speedTextView=findViewById(R.id.speedTextView);
        locationTextView=findViewById(R.id.locationTextView);
        distanceTextView=findViewById(R.id.temperatureTextView);
        temperatureTextView=findViewById(R.id.temperatureTextView);
        collisionsTextView=findViewById(R.id.collisionsTextView);
        humidityTextView=findViewById(R.id.humidityTextView);
        liveLocationButton=findViewById(R.id.liveLocationButton);

        liveLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openMap();
            }
        });


    }
}