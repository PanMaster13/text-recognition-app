package com.example.textrecognitionapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
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

    private ArrayList<TextInputLayout> textInputLayouts = new ArrayList<>();
    private TextInputLayout timeView, dateView, resultView1, resultView2, lotView, instIdView, testIdView, operatorView;
    private Button submitBtn;

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

        // Calls camera activity function when 'Take Picture' button is pressed
        captureImageBtn.setOnClickListener(v -> {
            dispatchTakePictureIntent();
        });

        submitBtn.setOnClickListener(v -> {
            submitValidation();
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
            Toast.makeText(getApplicationContext(), "Error: " + ex.getMessage(), Toast.LENGTH_LONG).show();
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

    // Function to validate data from Firebase Vision ML Kit and set values to text input layouts
    private void getImageText(FirebaseVisionText firebaseVisionText) {
        // Get blocks of extracted text
        List<FirebaseVisionText.Block> blockList = firebaseVisionText.getBlocks();

        // No blocks (Image contains no text)
        if (blockList.size() == 0) {
            Toast.makeText(getApplicationContext(), "Error: No text found in image provided!", Toast.LENGTH_LONG).show();
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
                Toast.makeText(getApplicationContext(), "Error: Incorrect number of values found, please retake the picture of a HbA1c result slip.", Toast.LENGTH_LONG).show();
                // Hide progress bar
                hideProgressView();
            } else {
                // Hide progress bar
                hideProgressView();

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

            }
        }
    }

    // Validate form values before submitting it to the database
    private void submitValidation() {
        boolean formIsValid = false;

        // Check for empty values in the text input layouts
        for (int i = 0; i < textInputLayouts.size(); i++) {
            String text = textInputLayouts.get(i).getEditText().getText().toString();
            if (text.equals("")) {
                formIsValid = false;
                break;
            } else {
                formIsValid = true;
            }
        }

        if (formIsValid) { // No empty values found (Shows confirmation alert box before submitting)
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Please confirm that all values are correct before submitting.");
            builder.setCancelable(true);

            // Uploads data to database (To be implemented) & resets image and text input layout values to default values
            builder.setPositiveButton("Confirm", (dialog, which) -> {
                dialog.cancel();
                Toast.makeText(getApplicationContext(), "Uploading data to database.", Toast.LENGTH_SHORT).show();
                capturedImage.setImageResource(R.drawable.placeholder);
                for (int i = 0; i < textInputLayouts.size(); i++) {
                    textInputLayouts.get(i).getEditText().setText("");
                }
            });

            // Closes alert dialog
            builder.setNegativeButton("Go Back", (dialog, which) -> {
                dialog.cancel();
            });

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        } else { // Empty value found
            Toast.makeText(getApplicationContext(), "There are empty values found, please make sure that they are filled in.", Toast.LENGTH_LONG).show();
        }
    }

    // Function to show the progress spinner view
    public void showProgressView() {
        if (!isProgressShown) {
            isProgressShown = true;
            progressView = (ViewGroup) getLayoutInflater().inflate(R.layout.progressbar_layout, null);
            View v = this.findViewById(android.R.id.content).getRootView();
            ViewGroup viewGroup = (ViewGroup) v;
            viewGroup.addView(progressView);
        }
    }

    // Function to hide the progress spinner view
    public void hideProgressView() {
        View v = this.findViewById(android.R.id.content).getRootView();
        ViewGroup viewGroup = (ViewGroup) v;
        viewGroup.removeView(progressView);
        isProgressShown = false;
    }

    // Un-focuses on the text input layouts when any whitespace is clicked on
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