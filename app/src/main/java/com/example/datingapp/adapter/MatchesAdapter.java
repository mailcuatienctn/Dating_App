package com.example.datingapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.datingapp.R;
import com.example.datingapp.model.Match;

import java.util.List;

public class MatchesAdapter extends RecyclerView.Adapter<MatchesAdapter.MatchViewHolder> {

    private Context context;
    private List<Match> matchList;

    public MatchesAdapter(Context context, List<Match> matchList) {
        this.context = context;
        this.matchList = matchList;
    }

    @NonNull
    @Override
    public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_new_match_avatar, parent, false);
        return new MatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MatchViewHolder holder, int position) {
        Match match = matchList.get(position);
        holder.nameTextView.setText(match.getNameMatcher());

        // Sử dụng Glide hoặc Picasso để tải ảnh từ URL (nếu có)
        Glide.with(context)
                .load(match.getUrlAvartar())
                .into(holder.avatarImageView);
    }

    @Override
    public int getItemCount() {
        return matchList.size();
    }

    public class MatchViewHolder extends RecyclerView.ViewHolder {

        ImageView avatarImageView;
        TextView nameTextView;

        public MatchViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.image_avatar);
            nameTextView = itemView.findViewById(R.id.text_new_match_name);
        }
    }
}

