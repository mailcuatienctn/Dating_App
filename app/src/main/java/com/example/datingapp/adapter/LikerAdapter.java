package com.example.datingapp.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.datingapp.ProfileDetailActivity;
import com.example.datingapp.R;
import com.example.datingapp.model.Liker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import de.hdodenhof.circleimageview.CircleImageView;

public class LikerAdapter extends RecyclerView.Adapter<LikerAdapter.LikerViewHolder> {

    private List<Liker> likerList;
    private Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Liker liker);
    }

    public LikerAdapter(Context context, List<Liker> likerList, OnItemClickListener listener) {
        this.context = context;
        this.likerList = likerList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LikerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_liker_profile, parent, false);
        return new LikerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LikerViewHolder holder, int position) {
        Liker liker = likerList.get(position);
        holder.likerName.setText(liker.getName());

        // Load avatar with Glide
        if (liker.getAvatarUrl() != null && !liker.getAvatarUrl().isEmpty()) {
            Glide.with(context)
                    .load(liker.getAvatarUrl())
                    .placeholder(R.drawable.bg_avatar_circle)
                    .error(R.drawable.bg_avatar_circle)
                    .into(holder.likerAvatar);
        } else {
            holder.likerAvatar.setImageResource(R.drawable.bg_avatar_circle);
        }

        // Format like time
        if (liker.getDate() != null) {
            Date date = liker.getDate().toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy");
            sdf.setTimeZone(TimeZone.getDefault());
            holder.likeTime.setText(sdf.format(date));
        } else {
            holder.likeTime.setText("Không rõ thời gian");
        }

        // Handle item click
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProfileDetailActivity.class);
            // Truyền thông tin người dùng qua Intent
            intent.putExtra(ProfileDetailActivity.EXTRA_USER_ID, liker.getUid());
            // Thêm flag vào Intent dưới dạng chuỗi "1"
            intent.putExtra("EXTRA_FLAG", "2"); // Hoặc bất kỳ giá trị nào bạn muốn            context.startActivity(intent);

            if (listener != null) {
                listener.onItemClick(liker);
            }
        });
    }

    @Override
    public int getItemCount() {
        return likerList.size();
    }

    public void updateData(List<Liker> newLikerList) {
        this.likerList.clear();
        this.likerList.addAll(newLikerList);
        notifyDataSetChanged();
    }

    public static class LikerViewHolder extends RecyclerView.ViewHolder {
        CircleImageView likerAvatar;
        TextView likerName, likeTime;

        public LikerViewHolder(View itemView) {
            super(itemView);
            likerAvatar = itemView.findViewById(R.id.likerAvatar);
            likerName = itemView.findViewById(R.id.likerName);
            likeTime = itemView.findViewById(R.id.likeTime);
        }
    }
}
