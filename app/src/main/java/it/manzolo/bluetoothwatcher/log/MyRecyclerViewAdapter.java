package it.manzolo.bluetoothwatcher.log;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import it.manzolo.bluetoothwatcher.R;

public class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.MyViewHolder> {
    private final ArrayList<Bluelog> mLogs;

    public MyRecyclerViewAdapter(ArrayList<Bluelog> log) {
        this.mLogs = log;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //Inflate RecyclerView row
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false);

        //Create View Holder

        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.textViewData.setText(mLogs.get(position).getData());

        String message = mLogs.get(position).getMessage();
        holder.textViewMessage.setText(message);

        String type = mLogs.get(position).getType();

        switch (type) {
            case Bluelog.logEvents.ERROR:
                holder.textViewMessage.setTextColor(Color.RED);
                holder.imageViewType.setImageResource(android.R.drawable.presence_busy);
                break;
            case Bluelog.logEvents.INFO:
                holder.textViewMessage.setTextColor(Color.BLACK);
                holder.imageViewType.setImageResource(0);
                break;
            case Bluelog.logEvents.WARNING:
                holder.textViewMessage.setTextColor(Color.BLACK);
                holder.imageViewType.setImageResource(android.R.drawable.presence_invisible);
                break;
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
        private final TextView textViewData;
        private final TextView textViewMessage;
        private final ImageView imageViewType;

        MyViewHolder(@NonNull View itemView) {
            super(itemView);

            textViewData = itemView.findViewById(R.id.logData);
            textViewMessage = itemView.findViewById(R.id.logMessage);
            imageViewType = itemView.findViewById(R.id.logstatus);
        }
    }
}