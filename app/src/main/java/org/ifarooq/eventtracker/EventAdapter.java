package org.ifarooq.eventtracker;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.ifarooq.eventtracker.databinding.EventEntryBinding;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventViewHolder> {

    private final List<Event> eventList;
    private final MainActivity mainAct;

    EventAdapter(List<Event> eventList, MainActivity ma) {
        this.eventList = eventList;
        mainAct = ma;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        EventEntryBinding binding =
                EventEntryBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent, false);
        binding.getRoot().setOnClickListener(mainAct);
        binding.getRoot().setOnLongClickListener(mainAct);
        return new EventViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.binding.eventName.setText(event.getName());
        holder.binding.eventType.setText(event.getEventType());
        holder.binding.eventDate.setText(event.getDate());
        // The start text seen comes from the string-array: position 3 = "3 ★★★☆☆"
        String[] ratings = mainAct.getResources().getStringArray(R.array.filter_ratings);
        holder.binding.eventRating.setText(ratings[event.getRating()]);
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }
}