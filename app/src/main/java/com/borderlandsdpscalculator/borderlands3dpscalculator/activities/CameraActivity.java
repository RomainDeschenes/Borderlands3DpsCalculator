package com.borderlandsdpscalculator.borderlands3dpscalculator.activities;

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
import com.borderlandsdpscalculator.borderlands3dpscalculator.R;
import com.borderlandsdpscalculator.borderlands3dpscalculator.models.Weapon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CameraActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 0;
    private int scan_count = 0;

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
    private static final String NUMBER_PATTERN = "([0-9]+([.x])?[0-9]*[^\"])";

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
                    final SparseArray<TextBlock> items = detections.getDetectedItems(); //creates SparseArray from detected Textblocks
                    if (items.size() != 0 ){

                        mDamageTextView.post(new Runnable() {
                            @Override
                            public void run() {
                                for(int i=0;i<items.size();i++){    //for every value in items
                                    TextBlock item = items.valueAt(i);  //current element (item) is equal to element at i
                                    Log.d("Detection", item.getValue());
                                    List<String> allMatches = new ArrayList<>();  //initializes a list of strings to hold pattern matches
                                    Matcher m = mPattern.matcher(item.getValue());  //creates a Matcher that attempts to match contents contained in item to regex
                                   /* try {
                                        Log.d("Detection", String.valueOf(m.end()));
                                    } catch(IllegalStateException e){
                                        ;
                                    }*/
                                    while (m.find()) {
                                        allMatches.add(m.group());  //for every match found previously with Matcher m, add the match to allMatches
                                        Log.d("Match", allMatches.get(allMatches.size()-1));    //log each match by recording the value at the end of the allMatches list
                                    }
                                    if(allMatches.size() == 6) {
                                        if (validMatches(allMatches)){ //checks if the entries for damage, handling, and magazine size are integers
                                            mCurrentWeapon = buildWeapon(allMatches);   //if allMatches has only six elements, then build mCurrentWeapon by passing in the list of matches
                                            updateWeaponTextViews();    //update app to display stats in real time
                                        }
                                    } else if (allMatches.size() == 5) {
                                        if (validMatches(allMatches)) { //checks if the entries for damage and magazine size are integers
                                            mCurrentWeapon = buildWeapon(allMatches);   //if allMatches has five elements, then build mCurrentWeapon by passing in the list of matches
                                            updateWeaponTextViews();    //update app to display stats in real time
                                        }
                                    } else {
                                        Log.d("allMatches size", String.valueOf(allMatches.size()));
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
        if (weaponsStats.size() < 5) {
            return null;
        }

        ListIterator<String> iterator = weaponsStats.listIterator();

        // Convert values like 3x12 to their result
        while(iterator.hasNext()) {
            String next = iterator.next();
            if (next.contains("x")) {
                String[] values = next.split("x");
                if (values.length == 2) {
                    iterator.set(String.valueOf(Integer.parseInt(values[0]) * Integer.parseInt((values[1]).trim())));
                }
            }
        }

        if (weaponsStats.size() == 6) {
            return new Weapon(  //TODO: Look into making accuracy for BL3 an integer again
                    Integer.parseInt((weaponsStats.get(0)).replaceAll("[^\\d.]", "")),  //damage
                    Float.parseFloat((weaponsStats.get(1)).replaceAll("[^\\d.]", "")),  //accuracy
                    Integer.parseInt((weaponsStats.get(2)).replaceAll("[^\\d.]", "")),  //handling
                    Float.parseFloat((weaponsStats.get(3)).replaceAll("[^\\d.]", "")),  //reload time
                    Float.parseFloat((weaponsStats.get(4)).replaceAll("[^\\d.]", "")),  //fire rate
                    Integer.parseInt((weaponsStats.get(5)).replaceAll("[^\\d.]", ""))   //magazine size
            );
        } else {
            return new Weapon(
                    Integer.parseInt((weaponsStats.get(0)).replaceAll("[^\\d.]", "")),  //damage
                    Float.parseFloat((weaponsStats.get(1)).replaceAll("[^\\d.]", "")),  //accuracy
                    Float.parseFloat((weaponsStats.get(2)).replaceAll("[^\\d.]", "")),  //fire rate
                    Float.parseFloat((weaponsStats.get(3)).replaceAll("[^\\d.]", "")),  //reload "speed"
                    Integer.parseInt((weaponsStats.get(4)).replaceAll("[^\\d.]", ""))   //magazine size
            );
        }

    }

    private static boolean isInteger(String s) {
        return isInteger(s,10); //calls overload of self that takes the string and radix
    }

    private static boolean isInteger(String s, int radix) { //function that iterates over string to make sure that it is an integer (or two integers separated by 'x')
        if(s.isEmpty()) return false;
        for(int i = 0; i < s.length(); i++) {   //iterates over string
            /*if(i == 0 && s.charAt(i) == '-') {  //if a hyphen (negative) is the first character, checks if the string has a length of one and returns false if it does.  Allows for negative input.  Unnecessary for this program
                if(s.length() == 1) return false;
                else continue;
            }*/
            if(s.charAt(i) != 'x'){ //checks if the character encountered is not 'x'.  If not 'x', checks if character is valid for given base (base 10) numbering system.  Returns false if not a valid digit
                if(Character.digit(s.charAt(i),radix) < 0) return false;
            } else {
                if(i == s.length()-1 || i == 0) return false;   //checks if the 'x' that was encountered is the last character in the string or is at the beginning.  If either of these are true, returns false
            }
        }
        return true;
    }

    private static boolean validMatches(List<String> allMatches) {  //checks whether certain values are integers. if any are not, returns false, which causes the program to not attempt to construct a weapon from the data and throw an error
        if (allMatches.size() == 6) {
            boolean damage_is_int = isInteger((String.valueOf(allMatches.get(0)).replaceAll("[^\\dx.]", "")));
            boolean accuracy_is_int = isInteger((String.valueOf(allMatches.get(1)).replaceAll("[^\\d.]","")));  //accuracy is represented as an integer (whole-number percent sign) in BL3, so check for that
            boolean handling_is_int = isInteger((String.valueOf(allMatches.get(2)).replaceAll("[^\\d.]", "")));
            boolean magazine_size_is_int = isInteger((String.valueOf(allMatches.get(5)).replaceAll("[^\\d.]", "")));
            return damage_is_int && accuracy_is_int && handling_is_int && magazine_size_is_int;
        } else if (allMatches.size() == 5){
            boolean damage_is_int = isInteger((String.valueOf(allMatches.get(0)).replaceAll("[^\\dx.]", "")));
            boolean magazine_size_is_int = isInteger((String.valueOf(allMatches.get(4)).replaceAll("[^\\d.]", "")));
            return damage_is_int && magazine_size_is_int;
        } else return false;
    }
}
