package com.unipi.xdimtsasp17027.speedometer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity2 extends AppCompatActivity {

    TextView mainTextView;
    SQLiteDatabase database;
    String mode;
    Button showLastWeekButton,showAllButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        showLastWeekButton=findViewById(R.id.showLastWeekButton);
        showAllButton=findViewById(R.id.showAllButton);

        showAllButton.setBackgroundColor(Color.parseColor("#273c75"));
        showLastWeekButton.setBackgroundColor(Color.TRANSPARENT);
        database=openOrCreateDatabase("speedTransgressions", Context.MODE_PRIVATE,null);
        mainTextView=findViewById(R.id.mainTextView);
        mode="showingAll";
        showAll();



    }

    public void showAll(){
        Cursor cursor=database.rawQuery("SELECT*FROM Transgression",null);
        if(cursor.getCount()>0){
            StringBuilder builder=new StringBuilder();
            while(cursor.moveToNext()){
                builder.append("Latitude:"+cursor.getString(0)+"\n");
                builder.append("Longitude:"+cursor.getString(1)+"\n");
                builder.append("Speed:"+cursor.getString(2)+" km"+"\n");
                builder.append("Datetime:"+cursor.getString(3)+"\n");
                builder.append("\n");
            }
            mainTextView.setText(builder);

        }else{
            mainTextView.setText("There are no transgressions yet");
        }
    }

    public void showLastWeek(){
        Cursor cursor2=database.rawQuery("SELECT* FROM Transgression  WHERE  std_timestamp BETWEEN datetime('now', '-6 days') AND datetime('now', 'localtime')",null);
        if(cursor2.getCount()>0){
            StringBuilder builder=new StringBuilder();
            while(cursor2.moveToNext()){
                builder.append("latitude:"+cursor2.getString(0)+"\n");
                builder.append("longitude:"+cursor2.getString(1)+"\n");
                builder.append("speed:"+cursor2.getString(2)+" km"+"\n");
                builder.append("timestamp:"+cursor2.getString(3)+"\n");
                builder.append("\n");
            }
            mainTextView.setText(builder);

        }else{
            mainTextView.setText("There are no transgressions yet");
        }

    }

    public void buttonAllClicked(View view){
        if(mode.equals("showingLastWeek")){
            showAll();
            showAllButton.setBackgroundColor(Color.parseColor("#273c75"));
            showLastWeekButton.setBackgroundColor(Color.TRANSPARENT);
            mode="showingAll";
        }

    }

    public void buttonLastWeekClicked(View view){
        if(mode.equals("showingAll")){
            showLastWeek();
            showLastWeekButton.setBackgroundColor(Color.parseColor("#273c75"));
            showAllButton.setBackgroundColor(Color.TRANSPARENT);
            mode="showingLastWeek";
        }
    }



    @Override
    public void onBackPressed() {

        finish();
        startActivity(new Intent(getApplicationContext(),MainActivity.class));

    }
}