package com.example.textrecognitionapp;

import android.content.Context;
import android.content.Intent;
import com.google.android.material.textfield.TextInputLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

public class FormActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        // Variable declaration
        ArrayList<TextInputLayout> textInputLayouts = new ArrayList<>();
        TextInputLayout timeView, dateView, resultView1, resultView2, lotView, instIdView, testIdView, operatorView;
        Button submitBtn;

        Intent intentFromMain = getIntent();
        ArrayList<String> words = (ArrayList<String>) intentFromMain.getSerializableExtra("words");

        // Connecting view variables to layout views in XML
        timeView = findViewById(R.id.timeView);
        dateView = findViewById(R.id.dateView);
        resultView1 = findViewById(R.id.resultView1);
        resultView2 = findViewById(R.id.resultView2);
        lotView = findViewById(R.id.lotView);
        instIdView = findViewById(R.id.instIdView);
        testIdView = findViewById(R.id.testIdView);
        operatorView = findViewById(R.id.operatorView);
        submitBtn = findViewById(R.id.submitBtn);

        // Adding text input layout views into array
        textInputLayouts.add(timeView);
        textInputLayouts.add(dateView);
        textInputLayouts.add(resultView1);
        textInputLayouts.add(resultView2);
        textInputLayouts.add(lotView);
        textInputLayouts.add(instIdView);
        textInputLayouts.add(testIdView);
        textInputLayouts.add(operatorView);

         // Remove the "A1C" substring from the A1C data values
        for (int i = 0; i < words.size(); i++) {
            if (words.get(i).contains("A1C")){
                words.set(i, words.get(i).replace("A1C", ""));
            }
        }
        // Removes the '%' and 'mmol/mol' section of the result data
        words.set(2, words.get(2).replace("%", ""));
        words.set(3, words.get(3).split("m")[0]);

        // Loop through word list and fills the text input layouts
        for (int i = 0; i < words.size(); i++) {
            textInputLayouts.get(i).getEditText().setText(words.get(i));
        }

        // Creates an alert dialog
        submitBtn.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Please confirm that all values are correct before submitting.");
            builder.setCancelable(true);

            // Uploads data to database (To be implemented) & redirects user to the previous page
            builder.setPositiveButton("Confirm", (dialog, which) -> {
                dialog.cancel();
                Toast.makeText(getApplicationContext(), "Uploading data to database.", Toast.LENGTH_SHORT).show();
                onBackPressed();
            });

            // Closes alert dialog
            builder.setNegativeButton("Go Back", (dialog, which) -> {
                dialog.cancel();
            });

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        });

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(R.drawable.back_arrow);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent backToMainIntent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(backToMainIntent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)ev.getRawX(), (int)ev.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }
}