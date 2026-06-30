package com.example.safetyapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safetyapp.model.Contact;

import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {

    public interface OnDeleteListener {
        void onDelete(int position);
    }

    private List<Contact> contactList;
    private OnDeleteListener deleteListener;

    public ContactAdapter(List<Contact> contactList, OnDeleteListener deleteListener) {
        this.contactList = contactList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Contact contact = contactList.get(position);

        holder.tvAvatar.setText(
                contact.getName() != null && !contact.getName().isEmpty()
                        ? String.valueOf(contact.getName().charAt(0)).toUpperCase()
                        : "?");

        holder.tvName.setText(contact.getName());
        holder.tvPhone.setText(contact.getPhone()
                + " • Priority " + contact.getPriority());

        holder.tvDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvPhone, tvDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvName   = itemView.findViewById(R.id.tvName);
            tvPhone  = itemView.findViewById(R.id.tvPhone);
            tvDelete = itemView.findViewById(R.id.tvDelete);
        }
    }
}