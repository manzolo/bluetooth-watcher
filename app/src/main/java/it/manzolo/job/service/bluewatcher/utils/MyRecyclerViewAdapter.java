package it.manzolo.job.service.bluewatcher.utils;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import it.manzolo.job.service.bluewatcher.R;

public class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.MyViewHolder> {
    private ArrayList<Bluelog> mLogs;

    public MyRecyclerViewAdapter(ArrayList<Bluelog> log) {
        this.mLogs = log;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //Inflate RecyclerView row
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false);

        //Create View Holder
        final MyViewHolder myViewHolder = new MyViewHolder(view);

        return myViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.textViewData.setText(mLogs.get(position).getData());

        String message = mLogs.get(position).getMessage();
        holder.textViewMessage.setText(message);

        String type = mLogs.get(position).getType();
        //holder.textViewType.setText(type);

        if (type.equals("E")) {

            holder.textViewMessage.setTextColor(Color.RED);
        }
    }

    @Override
    public int getItemCount() {
        return mLogs.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    //RecyclerView View Holder
    class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewData;
        private TextView textViewMessage;
        private TextView textViewType;

        MyViewHolder(@NonNull View itemView) {
            super(itemView);

            textViewData = itemView.findViewById(R.id.logData);
            textViewMessage = itemView.findViewById(R.id.logMessage);
        }
    }
}