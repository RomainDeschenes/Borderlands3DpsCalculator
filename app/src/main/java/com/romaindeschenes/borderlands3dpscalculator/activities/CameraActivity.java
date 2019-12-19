package com.romaindeschenes.borderlands3dpscalculator.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.romaindeschenes.borderlands3dpscalculator.R;
import com.romaindeschenes.borderlands3dpscalculator.models.Weapon;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CameraActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 0;

    private TextView mDamageTextView;
    private TextView mAccuracyTextView;
    private TextView mHandlingTextView;
    private TextView mReloadTimeTextView;
    private TextView mFireRateTextView;
    private TextView mMagazineSizeTextView;
    private Button mPickWeaponButton;

    private SurfaceView mCameraView;
    private CameraSource mCameraSource;

    private String mDamageString;
    private String mAccuracyString;
    private String mHandlingString;
    private String mFireRateString;
    private String mMagazineSizeString;
    private String mReloadTimeString;

    private static final String NUMBER_PATTERN = "([0-9]+\\.*[0-9]*)";
    private static final String NORMALIZE_STRING = "[^\\p{ASCII}]";

    private Pattern mPattern;
    private Weapon mCurrentWeapon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mCameraView = findViewById(R.id.cameraView);
        mDamageTextView = findViewById(R.id.damageTextView);
        mAccuracyTextView = findViewById(R.id.accuracyTextview);
        mHandlingTextView = findViewById(R.id.handlingTextView);
        mReloadTimeTextView = findViewById(R.id.reloadTimeTextView);
        mFireRateTextView = findViewById(R.id.fireRateTextView);
        mMagazineSizeTextView = findViewById(R.id.magazineSizeTextview);
        mPickWeaponButton = findViewById(R.id.pick_weapon_button);
        mMagazineSizeString = getNormalizedString(getResources().getString(R.string.magazine_size).toUpperCase());

        mDamageString = getNormalizedString(getResources().getString(R.string.damage).toUpperCase());
        mAccuracyString = getNormalizedString(getResources().getString(R.string.accuracy).toUpperCase());
        mHandlingString = getNormalizedString(getResources().getString(R.string.handling).toUpperCase());
        mReloadTimeString = getNormalizedString(getResources().getString(R.string.reload_time).toUpperCase());
        mFireRateString = getNormalizedString(getResources().getString(R.string.fire_rate).toUpperCase());

        mPickWeaponButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("weapon", mCurrentWeapon);
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        });

        mPattern = Pattern.compile(NUMBER_PATTERN);
        startCameraSource();

    }

    private String getNormalizedString(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll(NORMALIZE_STRING, "");
    }

    private void updateWeaponTextViews() {
        mDamageTextView.setText(String.valueOf(mCurrentWeapon.getDamage()));
        mAccuracyTextView.setText(String.valueOf(mCurrentWeapon.getAccuracy()));
        mHandlingTextView.setText(String.valueOf(mCurrentWeapon.getHandling()));
        mReloadTimeTextView.setText(String.valueOf(mCurrentWeapon.getReloadTime()));
        mFireRateTextView.setText(String.valueOf(mCurrentWeapon.getFireRate()));
        mMagazineSizeTextView.setText(String.valueOf(mCurrentWeapon.getMagazineSize()));
    }

    private void startCameraSource() {

        //Create the TextRecognizer
        final TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        if (!textRecognizer.isOperational()) {
            Log.w("Camera", "Detector dependencies not loaded yet");
        } else {

            //Initialize camerasource to use high resolution and set Autofocus on.
            mCameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setAutoFocusEnabled(true)
                    .setRequestedFps(2.0f)
                    .build();

            /**
             * Add call back to SurfaceView and check if camera permission is granted.
             * If permission is granted we can start our cameraSource and pass it to surfaceView
             */
            mCameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    try {

                        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                            ActivityCompat.requestPermissions(CameraActivity.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA);
                            return;
                        }
                        mCameraSource.start(mCameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                }

                /**
                 * Release resources for cameraSource
                 */
                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    mCameraSource.stop();
                }
            });

            //Set the TextRecognizer's Processor.
            textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {
                }

                /**
                 * Detect all the text from camera using TextBlock and the values into a stringBuilder
                 * which will then be set to the textView.
                 * */
                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {
                    final SparseArray<TextBlock> items = detections.getDetectedItems();
                    if (items.size() != 0 ){

                        mDamageTextView.post(new Runnable() {
                            @Override
                            public void run() {
                                StringBuilder stringBuilder = new StringBuilder();
                                for(int i=0;i<items.size();i++){
                                    TextBlock item = items.valueAt(i);
                                    stringBuilder.append(item.getValue());
                                    stringBuilder.append("\n");

                                    if (item.getValue().contains(mDamageString)
                                            && item.getValue().contains(mFireRateString)
                                            && item.getValue().contains(mReloadTimeString)
                                            && item.getValue().contains(mMagazineSizeString)
                                            && i+1 < items.size()) {
                                        List<String> allMatches = new ArrayList<String>();
                                        Matcher m = mPattern.matcher(items.valueAt(i+1).getValue());
                                        while (m.find()) {
                                            allMatches.add(m.group());
                                        }

                                        // It is all the numbers in the tooltip
                                        if(allMatches.size() == 6) {
                                            mCurrentWeapon = new Weapon(
                                                Integer.parseInt(allMatches.get(0)),
                                                Integer.parseInt(allMatches.get(1)),
                                                Integer.parseInt(allMatches.get(2)),
                                                Float.parseFloat(allMatches.get(3)),
                                                Float.parseFloat(allMatches.get(4)),
                                                Integer.parseInt(allMatches.get(5))
                                            );

                                            updateWeaponTextViews();
                                        }
                                    }
                                }
                            }
                        });
                    }
                }
            });
        }
    }
}