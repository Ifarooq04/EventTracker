package org.ifarooq.eventtracker;

import androidx.recyclerview.widget.RecyclerView;

import org.ifarooq.eventtracker.databinding.EventEntryBinding;

public class EventViewHolder extends RecyclerView.ViewHolder {

    public EventEntryBinding binding;

    EventViewHolder(EventEntryBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }
}