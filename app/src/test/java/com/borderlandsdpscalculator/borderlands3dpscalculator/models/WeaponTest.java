package com.borderlandsdpscalculator.borderlands3dpscalculator.models;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class WeaponTest {

    private Weapon wpn;

    @Before
    public void setup() {
        wpn = new Weapon(100, 0, 0, 2.0f, 1, 10);
    }

    @Test
    public void getDamagePerMagazine() {
        assertEquals(1000, wpn.getDamagePerMagazine());
    }

    @Test
    public void getDamagePerSecond() {
        assertEquals(83, wpn.getDamagePerSecond());
    }

    @Test
    public void getTimeToEmptyMagazine() { assertEquals(10f, wpn.getTimeToEmptyMagazine(), 0f); }

    @Test
    public void getTimeSpentReloading() { assertEquals(17, wpn.getTimeSpentReloading());}

    @Test
    public void getTimeSpentShooting() { assertEquals(83, wpn.getTimeSpentShooting()); }
}