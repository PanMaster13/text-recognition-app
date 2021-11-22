package com.example.textrecognitionapp;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    // Variable declaration
    private Button captureImageBtn, extractTextBtn;
    private ImageView capturedImage;
    private TextView extractedText;

    static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        capturedImage = findViewById(R.id.capturedImage);
        extractedText = findViewById(R.id.extractedText);
        captureImageBtn = findViewById(R.id.captureImageBtn);
        extractTextBtn = findViewById(R.id.extractTextBtn);

        captureImageBtn.setOnClickListener(v -> {
            dispatchTakePictureIntent();
        });
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } catch (ActivityNotFoundException e) {
            // display error state to the user
            Toast.makeText(getApplicationContext(), "Error: Failed to open camera app!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            capturedImage.setImageBitmap(imageBitmap);
            Toast.makeText(getApplicationContext(), "Image captured successfully!", Toast.LENGTH_SHORT).show();
        }
    }
}