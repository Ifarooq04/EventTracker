package org.ifarooq.eventtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.ifarooq.eventtracker.databinding.ActivityEditBinding;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class EditActivity extends AppCompatActivity {

    private ActivityEditBinding binding;
    private int position = -1;   // -1 means a NEW event. otherwise the index of the event being edited. stores where the event came from in Main's list, or -1 for a new one.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.toolbar.setTitle(R.string.edit_event);
        binding.toolbar.setTitleTextColor(getColor(R.color.white));
        setSupportActionBar(binding.toolbar);

        // Fill in the fields if an existing event was sent over.(Only first event)
        // (After a rotation the EditTexts and Spinners restore themselves.)
        if (savedInstanceState == null) {
            loadEventFromIntent();
        } else {
            position = savedInstanceState.getInt("POSITION", -1);
        }

        // save the event prompted when pressing back.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showSaveDialog();
            }
        });
    }

    private void loadEventFromIntent() {
        Intent intent = getIntent();
        if (intent.hasExtra(getString(R.string.event_key))) {
            Event event = intent.getSerializableExtra(getString(R.string.event_key), Event.class);
            if (event != null) {
                binding.eventName.setText(event.getName());
                binding.eventLocation.setText(event.getLocation());
                binding.eventDate.setText(event.getDate());
                binding.eventNotes.setText(event.getNotes());

                // In the spinner list, find the event type's position
                List<String> types = Arrays.asList(getResources().getStringArray(R.array.edit_types));
                int typePos = types.indexOf(event.getEventType());
                binding.typeSpinner.setSelection(Math.max(typePos, 0));

                // Rating 1-5 is at spinner position 1-5 pos 0 is select rating. so the rating number equals the pos
                binding.ratingSpinner.setSelection(event.getRating());
            }
            position = intent.getIntExtra(getString(R.string.position_key), -1);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.edit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuSave) {
            doSave();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void doSave() {
        String name = binding.eventName.getText().toString().trim();
        String location = binding.eventLocation.getText().toString().trim();
        String date = binding.eventDate.getText().toString().trim();
        String notes = binding.eventNotes.getText().toString().trim();
        int typePos = binding.typeSpinner.getSelectedItemPosition();
        int ratingPos = binding.ratingSpinner.getSelectedItemPosition();

        // Name, type, rating, location, and date are mandatory (position 0 = "Select..."). empty filed or a spinner still on "select..." --> toast and stop
        if (name.isEmpty() || location.isEmpty() || date.isEmpty() || typePos == 0 || ratingPos == 0) {
            Toast.makeText(this, R.string.missing_values, Toast.LENGTH_SHORT).show();
            return;
        }
        // The year must have 4 digits (so "26" isn't read as the year 26, realzied during trial runs)
        String[] parts = date.split("/");
        if (parts.length != 3 || parts[2].length() != 4) {
            Toast.makeText(this, R.string.bad_date, Toast.LENGTH_SHORT).show();
            return;
        }

        // Verifying date here...date must be a real MM/DD/YYYY date so the list can be sorted
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
            sdf.setLenient(false);
            sdf.parse(date);
        } catch (ParseException e) {
            Toast.makeText(this, R.string.bad_date, Toast.LENGTH_SHORT).show();
            return;
        }

        String type = binding.typeSpinner.getSelectedItem().toString();
        Event event = new Event(type, name, location, date, notes, ratingPos);

        // To send the event (and which position it came from) back to MainActivity. puts the event and its position in an intent then setResult + finish
        Intent data = new Intent();
        data.putExtra(getString(R.string.event_key), event);
        data.putExtra(getString(R.string.position_key), position);
        setResult(RESULT_OK, data);
        finish();
    }

    private void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.save_event_title);
        builder.setMessage(R.string.save_event_msg);
        builder.setPositiveButton(R.string.yes, (dialog, id) -> doSave());
        builder.setNegativeButton(R.string.no, (dialog, id) -> finish());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt("POSITION", position);

        // Call super last
        super.onSaveInstanceState(outState);
    }
}