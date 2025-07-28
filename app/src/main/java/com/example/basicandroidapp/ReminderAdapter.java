package com.example.basicandroidapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder> {
    private List<LocationReminder> reminders = new ArrayList<>();
    private OnReminderDeleteListener deleteListener;
    
    public interface OnReminderDeleteListener {
        void onReminderDelete(LocationReminder reminder);
    }
    
    public ReminderAdapter(OnReminderDeleteListener deleteListener) {
        this.deleteListener = deleteListener;
    }
    
    public void setReminders(List<LocationReminder> reminders) {
        this.reminders = reminders;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reminder, parent, false);
        return new ReminderViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        LocationReminder reminder = reminders.get(position);
        holder.bind(reminder);
    }
    
    @Override
    public int getItemCount() {
        return reminders.size();
    }
    
    class ReminderViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewTitle;
        private TextView textViewNote;
        private TextView textViewStatus;
        private Button buttonDelete;
        
        public ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewNote = itemView.findViewById(R.id.textViewNote);
            textViewStatus = itemView.findViewById(R.id.textViewStatus);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
        
        public void bind(LocationReminder reminder) {
            textViewTitle.setText(reminder.title);
            textViewNote.setText(reminder.note);
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String status = reminder.isActive ? "Active" : "Inactive";
            String createdDate = dateFormat.format(new Date(reminder.createdAt));
            textViewStatus.setText(status + " • Created " + createdDate);
            
            buttonDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (deleteListener != null) {
                        deleteListener.onReminderDelete(reminder);
                    }
                }
            });
        }
    }
}
