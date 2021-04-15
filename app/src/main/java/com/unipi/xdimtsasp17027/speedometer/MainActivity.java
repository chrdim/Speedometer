package com.unipi.xdimtsasp17027.speedometer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements LocationListener {

    SharedPreferences maxSpeed;
    SharedPreferences.Editor editor;
    Button historyButton, startButton;
    TextView speedTextView, longitudeTextView, latitudeTextView;
    EditText speedEditText;

    AlertDialog.Builder providerAlert;
    AlertDialog.Builder speedAlert;
    AlertDialog.Builder speechRecognitionLesson;
    AlertDialog.Builder speechRecognitionWrongRequest;

    LocationManager locationManager;

    ConstraintLayout currentLayout;
    int speedOvercomingcounter;

    Tts overcomingSpeedMaxMessage;

    SQLiteDatabase database;

    Timestamp timestamp;

    private static final int REC_RESULT=653;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        database=openOrCreateDatabase("speedTransgressions",Context.MODE_PRIVATE,null);
        database.execSQL("CREATE TABLE IF NOT EXISTS Transgression(std_latidute TEXT,std_longitude TEXT,std_speed INT,std_timestamp DATETIME)");

        overcomingSpeedMaxMessage=new Tts(this);

        speedOvercomingcounter =0;

        currentLayout = (ConstraintLayout) findViewById(R.id.main_layout);

        maxSpeed = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        editor = maxSpeed.edit();
        editor.putString("maxSpeed", "70");
        editor.apply();

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        historyButton = findViewById(R.id.historyButton);
        startButton = findViewById(R.id.startButton);
        speedTextView = findViewById(R.id.speedTextView);
        longitudeTextView = findViewById(R.id.longitudeTextView);
        latitudeTextView = findViewById(R.id.latitudeTextView);
        speedEditText = findViewById(R.id.editTextTextPersonName);

        speedEditText.setAlpha(0.5f);

        speedEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                speedEditText.setAlpha(1f);
                return false;
            }
        });

        speedEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if(!(speedEditText.getText().toString().equals(""))){
                        if (Integer.parseInt(speedEditText.getText().toString()) > 0 && Integer.parseInt(speedEditText.getText().toString()) <= 250) {
                        speedEditText.setAlpha(0.5f);
                        speedEditText.clearFocus();
                        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        mgr.hideSoftInputFromWindow(speedEditText.getWindowToken(), 0);
                        Toast.makeText(getApplicationContext(), "The new max speed value is " + speedEditText.getText().toString() + " km",
                                Toast.LENGTH_LONG).show();
                        editor.putString("maxSpeed", speedEditText.getText().toString());
                        editor.apply();

                        } else {
                        Toast.makeText(getApplicationContext(), "The acceptable values for max speed are between 1 km and 250 km ", Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        Toast.makeText(getApplicationContext(), "The max speed can't be empty ", Toast.LENGTH_SHORT).show();
                    }

                }

                return false;
            }
        });

        providerAlert = new AlertDialog.Builder(this);
        providerAlert.setMessage("Please make sure that GPS is enabled and the LOCATION METHOD is on 'Phone only' or 'High accuracy' mode");
        providerAlert.setTitle("GPS error");
        providerAlert.setCancelable(true);
        providerAlert.setNegativeButton("Cancel", null);
        providerAlert.setPositiveButton("Go to location settings",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));

                    }
                });


        speechRecognitionLesson = new AlertDialog.Builder(this);
        speechRecognitionLesson.setMessage("Please say:\n 'START' if you want to start the speedometer\n 'STOP' if you want to stop the speedometer\n 'HISTORY' if you want to see the history '");
        speechRecognitionLesson.setTitle("Available operations using speech recognition");
        speechRecognitionLesson.setCancelable(true);
        speechRecognitionLesson.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"How can i help you?");

                        startActivityForResult(intent,REC_RESULT);




                    }
                });


        speedAlert = new AlertDialog.Builder(this);
        speedAlert.setMessage("You overcomed the max speed.");
        speedAlert.setTitle("Speed Alert");
        speedAlert.setCancelable(true);
        speedAlert.setPositiveButton("OK",null);

        speechRecognitionWrongRequest= new AlertDialog.Builder(this);
        speechRecognitionWrongRequest.setMessage("The request you gave is not available");
        speechRecognitionWrongRequest.setCancelable(true);
        speechRecognitionWrongRequest.setPositiveButton("Try again", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"How can i help you?");

                startActivityForResult(intent,REC_RESULT);
            }
        });
        speechRecognitionWrongRequest.setNegativeButton("Cancel",null);

    }

    @Override
    public void onLocationChanged(Location location) {

        latitudeTextView.setText(String.valueOf(location.getLatitude()));
        longitudeTextView.setText(String.valueOf(location.getLongitude()));

        speedTextView.setText(String.valueOf((int) location.getSpeed() * 3600 / 1000));

        if((int) location.getSpeed() * 3600 / 1000 > Integer.parseInt(maxSpeed.getString("maxSpeed", "1"))) {

           if(speedOvercomingcounter ==0) {
               speedAlert.create().show();
               overcomingSpeedMaxMessage.speak("You overcomed the max speed");
               currentLayout.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));

               timestamp=Timestamp.valueOf(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date((System.currentTimeMillis()))));

              database.execSQL("INSERT INTO Transgression VALUES('"+latitudeTextView.getText().toString()+"','"
                       + longitudeTextView.getText().toString()+"','"+Integer.parseInt(speedTextView.getText().toString())+"','"+timestamp+"')");
           }
           speedOvercomingcounter++;

        }else{
            speedOvercomingcounter =0;
            currentLayout.setBackgroundColor(Color.parseColor("#222f3e"));
        }

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public void startStopButtonClicked(View view){

        if(startButton.getText().toString().equals("START")){
            start();
        }else if(startButton.getText().toString().equals("STOP")){
            stop();
        }
    }


    public void start(){
        if(speedEditText.getAlpha()==0.5f){

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 234);
                return;
            }
            if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                startButton.setText("STOP");
                speedEditText.setEnabled(false);
            }else{
                providerAlert.create().show();
            }

        }else{

            Toast.makeText(getApplicationContext(), "Please set the max speed value and press the 'DONE' button.", Toast.LENGTH_LONG).show();
        }
    }

    public void stop(){
        startButton.setText("START");
        locationManager.removeUpdates(this);
        speedEditText.setEnabled(true);
        currentLayout.setBackgroundColor(Color.parseColor("#222f3e"));
        speedTextView.setText("0");
    }
    public void goToDatabaseActivity(View view){
        startActivity(new Intent(getApplicationContext(),MainActivity2.class));
    }

    public void microphoneClicked(View view){
        speechRecognitionLesson.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==REC_RESULT && resultCode== Activity.RESULT_OK){
            ArrayList<String> matches =data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if(matches.contains("history")){
                startActivity(new Intent(getApplicationContext(),MainActivity2.class));

            }else if(matches.contains("start")&& startButton.getText().toString().equals("START")){
                start();
            }else if(matches.contains("stop")&& startButton.getText().toString().equals("STOP")){
                stop();
            }
            else if(!(matches.contains("stop")||matches.contains("start")||matches.contains("history"))){
                speechRecognitionWrongRequest.create().show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }
}