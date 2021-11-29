package com.example.textrecognitionapp;

import android.content.Intent;
import com.google.android.material.textfield.TextInputLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;

public class FormActivity extends AppCompatActivity {

    // Variable declaration
    private ArrayList<TextInputLayout> textInputLayouts = new ArrayList<>();
    private TextInputLayout timeView, dateView, resultView1, resultView2, lotView, instIdView, testIdView, operatorView;
    private Button submitBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);
        Intent intentFromMain = getIntent();
        ArrayList<String> words = intentFromMain.getStringArrayListExtra("data");

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

        words.set(2, words.get(2).replace("%", ""));
        words.set(3, words.get(3).split("m")[0]);

        // Loop through word list and autofill text input layouts
        for (int i = 0; i < words.size(); i++) {
            textInputLayouts.get(i).getEditText().setText(words.get(i));
        }

        submitBtn.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Please confirm that all values are correct before submitting.");
            builder.setCancelable(true);

            builder.setPositiveButton("Confirm", (dialog, which) -> {
                dialog.cancel();
                Toast.makeText(getApplicationContext(), "Uploading data to database.", Toast.LENGTH_SHORT).show();
            });

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
}