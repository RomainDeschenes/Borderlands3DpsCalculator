package com.romaindeschenes.borderlands3dpscalculator.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import com.romaindeschenes.borderlands3dpscalculator.R;
import com.romaindeschenes.borderlands3dpscalculator.models.Weapon;

public class MainActivity extends AppCompatActivity {

    EditText mEditTextDamage;
    EditText mEditTextReloadTime;
    EditText mEditTextFireRate;
    EditText mEditTextMagazineSize;

    TextView mTextViewTimeToEmptyMagazine;
    TextView mTextViewDamagePerSecond;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mEditTextDamage = findViewById(R.id.editTextDamage);
        mEditTextReloadTime = findViewById(R.id.editTextReloadTime);;
        mEditTextFireRate = findViewById(R.id.editTextFireRate);;
        mEditTextMagazineSize = findViewById(R.id.editTextMagazineSize);;

        mTextViewTimeToEmptyMagazine = findViewById(R.id.textViewEmptyMagazine);
        mTextViewDamagePerSecond = findViewById(R.id.textViewDamagePerSecond);

        initListeners();
    }

    protected void initListeners() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                computeDps();
            }
        };
        mEditTextDamage.addTextChangedListener(textWatcher);
        mEditTextReloadTime.addTextChangedListener(textWatcher);
        mEditTextFireRate.addTextChangedListener(textWatcher);
        mEditTextMagazineSize.addTextChangedListener(textWatcher);
    }

    protected void computeDps() {
        // Get all values
        try {
            int damage = Integer.parseInt(mEditTextDamage.getText().toString());
            float reloadTime = Float.parseFloat(mEditTextReloadTime.getText().toString());
            float fireRate = Float.parseFloat(mEditTextFireRate.getText().toString());
            int magazineSize = Integer.parseInt(mEditTextMagazineSize.getText().toString());

            // We need all these values to compute DPS
            if (damage > 0 && reloadTime > 0 && fireRate > 0 && magazineSize > 0) {
                Weapon wpn = new Weapon(damage, 0, 0, reloadTime, fireRate, magazineSize);
                mTextViewDamagePerSecond.setText(Integer.toString(wpn.getDamagePerSecond()));
                mTextViewTimeToEmptyMagazine.setText(String.format("%f", wpn.getTimeToEmptyMagazine()));
            } else {
                mTextViewDamagePerSecond.setText("");
                mTextViewTimeToEmptyMagazine.setText("");
            }
        } catch (NumberFormatException e) {

        }
    }
}
