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
import java.util.ListIterator;
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

    // matches 12, 3.5, 3x12
    private static final String NUMBER_PATTERN = "([0-9]+(\\.|x)?[0-9]*)";

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
            mCameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setAutoFocusEnabled(true)
                    .setRequestedFps(30f)
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
                                for(int i=0;i<items.size();i++){
                                    TextBlock item = items.valueAt(i);
                                    List<String> allMatches = new ArrayList<String>();
                                    Matcher m = mPattern.matcher(item.getValue());
                                    while (m.find()) {
                                        allMatches.add(m.group());
                                    }
                                    if(allMatches.size() == 6) {
                                        mCurrentWeapon = buildWeapon(allMatches);
                                        updateWeaponTextViews();
                                    }
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    private Weapon buildWeapon(List<String> weaponsStats) {
        if (weaponsStats.size() < 6) {
            return null;
        }

        ListIterator<String> iterator = weaponsStats.listIterator();

        // Convert values like 3x12 to their result
        while(iterator.hasNext()) {
            String next = iterator.next();
            if (next.contains("x")) {
                String[] values = next.split("x");
                if (values.length == 2) {
                    iterator.set(String.valueOf(Integer.parseInt(values[0]) * Integer.parseInt(values[1])));
                }
            }
        }

        return new Weapon(
                Integer.parseInt(weaponsStats.get(0)),
                Integer.parseInt(weaponsStats.get(1)),
                Integer.parseInt(weaponsStats.get(2)),
                Float.parseFloat(weaponsStats.get(3)),
                Float.parseFloat(weaponsStats.get(4)),
                Integer.parseInt(weaponsStats.get(5))
        );
    }
}
