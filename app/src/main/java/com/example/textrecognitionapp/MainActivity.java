package com.example.textrecognitionapp;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Variable declaration
    private Button captureImageBtn, extractTextBtn;
    private ImageView capturedImage;
    private TextView extractedText;
    private Bitmap imageBitmap;
    private String tempText;

    static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        capturedImage = findViewById(R.id.capturedImage);
        extractedText = findViewById(R.id.extractedText);
        captureImageBtn = findViewById(R.id.captureImageBtn);
        extractTextBtn = findViewById(R.id.extractTextBtn);

        // Calls camera activity function when 'Take Picture' button is pressed
        captureImageBtn.setOnClickListener(v -> {
            dispatchTakePictureIntent();
        });

        extractTextBtn.setOnClickListener(v -> {
            extractTextFromImage();
        });
    }

    // Function for launching camera activity to capture image
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
        // When previous activity was from 'dispatchTakePictureIntent' function
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            capturedImage.setImageBitmap(imageBitmap);
            Toast.makeText(getApplicationContext(), "Image captured successfully!", Toast.LENGTH_SHORT).show();
            extractedText.setText("");
        }
    }

    // Function to extract text from image
    private void extractTextFromImage() {
        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(imageBitmap);
        FirebaseVisionTextDetector firebaseVisionTextDetector = FirebaseVision.getInstance().getVisionTextDetector();
        firebaseVisionTextDetector.detectInImage(firebaseVisionImage).addOnSuccessListener(firebaseVisionText -> {
            displayImageText(firebaseVisionText);
        }).addOnFailureListener(e -> { // Failure listener
            Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.d("ERROR", e.getMessage());
        });
    }

    private void displayImageText(FirebaseVisionText firebaseVisionText) {
        List<FirebaseVisionText.Block> blockList = firebaseVisionText.getBlocks();
        if (blockList.size() == 0) {
            Toast.makeText(getApplicationContext(), "Error: No text found in image provided!", Toast.LENGTH_SHORT).show();
        } else {
            for (FirebaseVisionText.Block block: firebaseVisionText.getBlocks()) {
                String currentText = extractedText.getText().toString();
                tempText = block.getText();
                extractedText.setText(currentText + tempText);
            }
        }
    }
}