package com.example.linda.mpower;

/**
 * Created by User on 9/28/2017.
 */

public class Record {

    String key;

    String energy, time;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getEnergy() {
        return energy;
    }

    public void setEnergy(String energy) {
        this.energy = energy;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Record(String key, String energy, String time) {

        this.key = key;
        this.energy = energy;
        this.time = time;
    }
}