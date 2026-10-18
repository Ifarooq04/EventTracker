package org.ifarooq.eventtracker;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.Gson;

import org.ifarooq.eventtracker.databinding.ActivityMainBinding;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, View.OnLongClickListener {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;

    private final ArrayList<Event> eventList = new ArrayList<>();    // holds every event
    private final ArrayList<Event> displayList = new ArrayList<>();  // holds only what passes the filters. adapter shows the second one
    private EventAdapter adapter;

    private ActivityResultLauncher<Intent> activityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupToolbar();

        adapter = new EventAdapter(displayList, this);
        binding.recycler.setAdapter(adapter);
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));

        // Sets up the launcher that opens Edit and get event back from it. (other side --> setResult)
        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleResult);

        setupFilters();

        // reads the file into eventList at startup. Required to be here in onCreate
        ArrayList<Event> temp = loadFile();
        if (temp != null) {
            eventList.addAll(temp);
        }
        sortEvents();
        applyFilters();
    }

    private void setupToolbar() {
        binding.toolbar.setTitleTextColor(getColor(R.color.white));
        binding.toolbar.setSubtitleTextColor(getColor(R.color.white));
        setSupportActionBar(binding.toolbar);
    }

    private void updateSubtitle() {
        binding.toolbar.setSubtitle(getString(R.string.event_count, displayList.size()));
    }

    // --- the options menu ---

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuAdd) {
            // opens edit with no extras, so the fields are blank
            Intent intent = new Intent(this, EditActivity.class);
            activityResultLauncher.launch(intent);
            return true;
        } else if (item.getItemId() == R.id.menuInfo) {
            // return nun, plain startActivity
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    // ---------------- Results from the Edit Activity ----------------

    private void handleResult(ActivityResult result) {
        if (result.getResultCode() != RESULT_OK || result.getData() == null) {
            Log.d(TAG, "handleResult: nothing returned");
            return;
        }
        Intent data = result.getData();
        Event event = data.getSerializableExtra(getString(R.string.event_key), Event.class);
        int position = data.getIntExtra(getString(R.string.position_key), -1);
        if (event == null) {
            return;
        }

        if (position >= 0 && position < eventList.size()) {
            eventList.set(position, event);   // edited an existing event
        } else {
            eventList.add(event);             // brand new event
        }
        sortEvents();
        saveEventList();
        applyFilters();
    }

    // Most recent date first, descending
    private void sortEvents() {
        eventList.sort((a, b) -> b.getDateValue().compareTo(a.getDateValue()));
    }

    // ---------------- Row clicks ----------------

    // finds which row was tapped, then sends that even into its postion to edit
    @Override
    public void onClick(View v) {
        int pos = binding.recycler.getChildLayoutPosition(v);
        Event event = displayList.get(pos);
        int realPos = eventList.indexOf(event);   // row numbers on screen dont match eventList when a filter is on, so indexOf finds the real spot.

        Intent intent = new Intent(this, EditActivity.class);
        intent.putExtra(getString(R.string.event_key), event);
        intent.putExtra(getString(R.string.position_key), realPos);
        activityResultLauncher.launch(intent);
    }

    // Long-pressing a row asks to delete that event
    @Override
    public boolean onLongClick(View v) {
        int pos = binding.recycler.getChildLayoutPosition(v);
        Event event = displayList.get(pos);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.delete_event_title);
        builder.setMessage(getString(R.string.delete_event_msg, event.getName()));
        builder.setPositiveButton(R.string.yes, (dialog, id) -> {
            eventList.remove(event);
            displayList.remove(pos);
            adapter.notifyItemRemoved(pos);
            updateSubtitle();
            saveEventList();
        });
        builder.setNegativeButton(R.string.no, null);   // Cancel: leave the list unchanged. null listener means the dialog just closes
        AlertDialog dialog = builder.create();
        dialog.show();
        return true;   // return true = I handled it, false would trigger a tap too
    }

    // ---------------- Filters ----------------

    private void setupFilters() {
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        binding.typeFilter.setOnItemSelectedListener(listener);
        binding.ratingFilter.setOnItemSelectedListener(listener);
    }

    // clears the displayList and readds only events that match the type and have at least the chosen rating
    private void applyFilters() {
        int typePos = binding.typeFilter.getSelectedItemPosition();        // 0 = All Types. position 0 in each spinner means no filter
        String type = binding.typeFilter.getSelectedItem().toString();
        int minRating = binding.ratingFilter.getSelectedItemPosition();    // 0 = Any Rating. position 0 in each spinner means no filter

        displayList.clear();
        for (Event e : eventList) {
            boolean typeOk = (typePos == 0) || e.getEventType().equals(type);
            boolean ratingOk = e.getRating() >= minRating;
            if (typeOk && ratingOk) {
                displayList.add(e);
            }
        }
        adapter.notifyDataSetChanged();
        updateSubtitle();
    }

    // puts both spinners back to pos 0 and refilters. Clear button
    public void clearFilters(View v) {
        binding.typeFilter.setSelection(0);
        binding.ratingFilter.setSelection(0);
        applyFilters();
    }

    // ---------------- JSON file ----------------

    private ArrayList<Event> loadFile() {
        Log.d(TAG, "loadFile: Loading JSON File");
        try {
            InputStream is = getApplicationContext().openFileInput(getString(R.string.file_name));
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            if (sb.length() == 0) {
                Log.d(TAG, "loadFile: No File Content");
                return null;
            }
            Gson gson = new Gson();
            Event[] events = gson.fromJson(sb.toString(), Event[].class);
            ArrayList<Event> tempList = new ArrayList<>();
            Collections.addAll(tempList, events);
            return tempList;
        } catch (FileNotFoundException e) {
            // First run: no file yet. Start with an empty list and no error.
            Log.d(TAG, "loadFile: No JSON file present");
        } catch (Exception e) {
            Log.d(TAG, "loadFile: " + e.getMessage());
        }
        return null;
    }

    private void saveEventList() {
        Log.d(TAG, "saveEventList: Saving JSON File");
        try {
            FileOutputStream fos = getApplicationContext().
                    openFileOutput(getString(R.string.file_name), Context.MODE_PRIVATE);
            PrintWriter printWriter = new PrintWriter(fos);
            printWriter.print(eventList.toString());
            printWriter.close();
            fos.close();
            Log.d(TAG, "saveEventList: " + eventList);
        } catch (Exception e) {
            Log.d(TAG, "saveEventList: " + e.getMessage());
        }
    }
}