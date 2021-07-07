package com.borderlandsdpscalculator.borderlands3dpscalculator.models;

import java.io.Serializable;

import androidx.annotation.NonNull;

public class Weapon implements Serializable {

    /**
     * Period over which damage is calculated
     */
    public static final int DAMAGE_PERIOD = 60;

    public Weapon() {

    }

    @NonNull
    @Override
    public String toString() {
        return "damage " + mDamage + ", accuracy " + mAccuracy + ", handling "
                + mHandling + ", reloadTime " + mReloadTime + ", fireRate" + mFireRate + ", magazineSize" + mMagazineSize;
    }

    public Weapon(int damage, float accuracy, int handling, float reloadTime, float fireRate, int magazineSize) {
        mDamage = damage;
        mAccuracy = accuracy;
        mHandling = handling;
        mReloadTime = reloadTime;
        mFireRate = fireRate;
        mMagazineSize = magazineSize;
    }

    public Weapon(int damage, float accuracy, float fireRate, float reloadTime, int magazineSize) {
        mDamage = damage;
        mAccuracy = accuracy;
        mFireRate = fireRate;
        mReloadTime = reloadTime;
        mMagazineSize = magazineSize;
    }

    public int getDamage() {
        return mDamage;
    }

    public void setDamage(int damage) {
        this.mDamage = damage;
    }

    public float getAccuracy() { return mAccuracy; }

    public void setAccuracy(float accuracy) {
        this.mAccuracy = accuracy;
    }

    public int getHandling() {
        return mHandling;
    }

    public void setHandling(int handling) {
        this.mHandling = handling;
    }

    public float getReloadTime() {
        return mReloadTime;
    }

    public void setReloadTime(float reloadTime) {
        this.mReloadTime = reloadTime;
    }

    public float getFireRate() {
        return mFireRate;
    }

    public void setFireRate(float fireRate) {
        this.mFireRate = fireRate;
    }

    public int getMagazineSize() {
        return mMagazineSize;
    }

    public void setMagazineSize(int magazineSize) {
        this.mMagazineSize = magazineSize;
    }

    public int getDamagePerMagazine() {
        return mDamage * mMagazineSize;
    }

    public float getTimeToEmptyMagazine() {
        if (mFireRate <= 0) {
            return 0;
        }
        return Math.round((mMagazineSize / mFireRate) * 100f) / 100f;
    }

    /**
     * @return damage per second
     */
    public float getDamagePerSecond () {
       return mDamage * mFireRate;
    }

    public int getTimeSpentReloading() {
        if (mReloadTime + getTimeToEmptyMagazine() == 0) {
            return 0;
        }
        return Math.round(mReloadTime * 100 / (mReloadTime + getTimeToEmptyMagazine()));
    }

    public int getTimeSpentShooting() {
        if (mReloadTime + getTimeToEmptyMagazine() == 0) {
            return 0;
        }
        return Math.round(getTimeToEmptyMagazine() * 100 / (mReloadTime + getTimeToEmptyMagazine()));
    }

    public float getDPSSustained() {
        return mDamage * mMagazineSize / (mMagazineSize / mFireRate + mReloadTime);
    }

    private int mDamage;
    private float mAccuracy;
    private int mHandling;
    private float mReloadTime;
    private float mFireRate;
    private int mMagazineSize;

}
