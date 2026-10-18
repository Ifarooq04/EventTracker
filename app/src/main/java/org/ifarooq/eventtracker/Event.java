package org.ifarooq.eventtracker;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Event implements Serializable {

    private final String eventType;
    private final String name;
    private final String location;
    private final String date;      // MM/DD/YYYY
    private final String notes;
    private final int rating;       // 1 - 5

    public Event(String eventType, String name, String location,
                 String date, String notes, int rating) {
        this.eventType = eventType;
        this.name = name;
        this.location = location;
        this.date = date;
        this.notes = notes;
        this.rating = rating;
    }

    public String getEventType() {
        return eventType;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public String getDate() {
        return date;
    }

    public String getNotes() {
        return notes;
    }

    public int getRating() {
        return rating;
    }

    // converts the MM/DD/YYYY text into a real date so the events can be sorted accordngly
    public Date getDateValue() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
            sdf.setLenient(false);
            return sdf.parse(date);
        } catch (ParseException e) {
            return new Date(0);
        }
    }

    @NonNull
    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}