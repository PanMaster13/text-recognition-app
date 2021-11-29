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

public class MainActivity extends AppCompatActivity {

    // Variable declaration
    private Button captureImageBtn, extractTextBtn;
    private ImageView capturedImage;
    private Bitmap imageBitmap;
    private ArrayList<String> words = new ArrayList<>();
    private final String[] unwantedWords = {"Quo-Lab A1C", "Time", "Time:", "Date", "Date:", "Result", "Result:", "DCCT", "IFCC", "Lot", "Lot:", "Inst ID", "Inst ID:", "Test ID", "Test ID:", "Operator", "Operator:"};

    private String mCurrentPhotoPath;

    static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        capturedImage = findViewById(R.id.capturedImage);
        captureImageBtn = findViewById(R.id.captureImageBtn);
        extractTextBtn = findViewById(R.id.extractTextBtn);
        imageBitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.placeholder);
        capturedImage.setImageBitmap(imageBitmap);

        // Calls camera activity function when 'Take Picture' button is pressed
        captureImageBtn.setOnClickListener(v -> {
            dispatchTakePictureIntent();
        });

        // Calls function to utilise Firebase Vision ML Kit text recognition to extract text from image
        extractTextBtn.setOnClickListener(v -> {
            extractTextFromImage();
        });
    }

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
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "HbA1c_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
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
        }
    }

    // Function to extract text from image
    private void extractTextFromImage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Please check if the image taken is correct and contains words.");
        builder.setCancelable(true);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            dialog.cancel();
            FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(imageBitmap);
            FirebaseVisionTextDetector firebaseVisionTextDetector = FirebaseVision.getInstance().getVisionTextDetector();
            firebaseVisionTextDetector.detectInImage(firebaseVisionImage).addOnSuccessListener(firebaseVisionText -> {
                displayImageText(firebaseVisionText);
            }).addOnFailureListener(e -> { // Failure listener
                Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        });

        builder.setNegativeButton("Go Back", (dialog, which) -> {
            dialog.cancel();
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void displayImageText(FirebaseVisionText firebaseVisionText) {
        List<FirebaseVisionText.Block> blockList = firebaseVisionText.getBlocks();
        if (blockList.size() == 0) {
            Toast.makeText(getApplicationContext(), "Error: No text found in image provided!", Toast.LENGTH_SHORT).show();
        } else {
            for (FirebaseVisionText.Block block: firebaseVisionText.getBlocks()) {
                String[] textArray = block.getText().split("\\r?\\n");
                words.addAll(Arrays.asList(textArray));
            }
            // Remove all unwanted words from the word list (Based on unwanted String array)
            for (String unwanted: unwantedWords) {
                words.remove(unwanted);
            }
            // Remove the "A1C" substring from the A1C data values
            for (int i = 0; i < words.size(); i++) {
                if (words.get(i).contains("A1C")){
                    words.set(i, words.get(i).replace("A1C", ""));
                }
            }

            // Transfers desired values from image to Form Activity & finishes this activity to reset data
            Intent toFormIntent = new Intent(getApplicationContext(), FormActivity.class);
            toFormIntent.putStringArrayListExtra("data", words);
            startActivity(toFormIntent);
            finish();
        }
    }
}