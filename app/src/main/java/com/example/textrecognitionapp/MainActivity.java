package com.example.textrecognitionapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // Variable declaration
    private Button captureImageBtn;
    private ImageView capturedImage;
    private Bitmap imageBitmap;
    private ViewGroup progressView;
    private ArrayList<String> words = new ArrayList<>();
    private final String[] unwantedWords = {"Quo-Lab A1C", "Time", "Time:", "Date", "Date:", "Result", "Result:", "DCCT", "IFCC", "Lot", "Lot:", "Inst ID", "Inst ID:", "Test ID", "Test ID:", "Operator", "Operator:"};

    private String mCurrentPhotoPath;
    private boolean isProgressShown = false;
    static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hides progress bar by default
        hideProgressView();

        // Linking variables to XML
        capturedImage = findViewById(R.id.capturedImage);
        captureImageBtn = findViewById(R.id.captureImageBtn);
        imageBitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.placeholder);
        capturedImage.setImageBitmap(imageBitmap);

        // Calls camera activity function when 'Take Picture' button is pressed
        captureImageBtn.setOnClickListener(v -> {
            dispatchTakePictureIntent();
        });
    }

    // 'dispatchTakePictureIntent()', 'createImageFile()', 'galleryAddPic()', 'setPic()', and 'onActivityResult()' taken from https://developer.android.com/training/camera/photobasics
    // Function for launching camera activity to capture image
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Create the File where the photo should go
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            // Error occurred while creating the File
            Toast.makeText(getApplicationContext(), "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
        // Continue only if the File was successfully created
        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(this,
                    "com.example.android.fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(new Date());
        String imageFileName = "HbA1c_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",   /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void setPic() {
        // Get the dimensions of the View
        int targetW = capturedImage.getWidth();
        int targetH = capturedImage.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.max(1, Math.min(photoW/targetW, photoH/targetH));

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        imageBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        capturedImage.setImageBitmap(imageBitmap);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // When previous activity was from 'dispatchTakePictureIntent' function
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            galleryAddPic();
            setPic();

            // Calls function to utilise Firebase Vision ML Kit text recognition to extract text from image
            extractTextFromImage();
        }
    }

    // 'extractTextFromImage()', & 'getImageText()' taken from https://www.youtube.com/watch?v=fmTlgwgKJmE
    private void extractTextFromImage() {
        // Show progress bar
        showProgressView();

        // Remove all previous values in 'words' arraylist
        words.removeAll(words);

        // Uses FirebaseVision library to extract text from image
        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(imageBitmap);
        FirebaseVisionTextDetector firebaseVisionTextDetector = FirebaseVision.getInstance().getVisionTextDetector();
        firebaseVisionTextDetector.detectInImage(firebaseVisionImage).addOnSuccessListener(firebaseVisionText -> {
            getImageText(firebaseVisionText);
        }).addOnFailureListener(e -> { // Failure listener
            Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void getImageText(FirebaseVisionText firebaseVisionText) {
        List<FirebaseVisionText.Block> blockList = firebaseVisionText.getBlocks();
        if (blockList.size() == 0) {
            Toast.makeText(getApplicationContext(), "Error: No text found in image provided!", Toast.LENGTH_SHORT).show();
            // Hide progress bar
            hideProgressView();
        } else {
            // Gets raw string data from each block and put into 'words' arraylist
            for (FirebaseVisionText.Block block: firebaseVisionText.getBlocks()) {
                String[] textArray = block.getText().split("\\r?\\n");
                words.addAll(Arrays.asList(textArray));
            }
            // Remove all unwanted words from the word list (Based on unwanted String array)
            for (String unwanted: unwantedWords) {
                words.remove(unwanted);
            }

            // Checks the number of values remaining (Should only be 7 values)
            if (words.size() != 7) {
                Toast.makeText(getApplicationContext(), "Error: Incorrect number of values found, please retake the picture of a HbA1c result slip.", Toast.LENGTH_SHORT).show();
                // Hide progress bar
                hideProgressView();
            } else {
                // Hide progress bar
                hideProgressView();

                 // Transfers desired values from image to Form Activity
                Intent toFromIntent = new Intent(getApplicationContext(), FormActivity.class);
                toFromIntent.putExtra("words", words);
                startActivity(toFromIntent);
                finish();
            }
        }
    }

    public void showProgressView() {
        if (!isProgressShown) {
            isProgressShown = true;
            progressView = (ViewGroup) getLayoutInflater().inflate(R.layout.progressbar_layout, null);
            View v = this.findViewById(android.R.id.content).getRootView();
            ViewGroup viewGroup = (ViewGroup) v;
            viewGroup.addView(progressView);
        }
    }

    public void hideProgressView() {
        View v = this.findViewById(android.R.id.content).getRootView();
        ViewGroup viewGroup = (ViewGroup) v;
        viewGroup.removeView(progressView);
        isProgressShown = false;
    }
}