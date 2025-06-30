package com.example.datingapp.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datingapp.ProfileDetailActivity;
import com.example.datingapp.R;
import com.example.datingapp.model.User;
import com.squareup.picasso.Picasso;

import java.util.List;

public class UserCardAdapter extends RecyclerView.Adapter<UserCardAdapter.UserViewHolder> {

    private List<User> userList;
    private Context context;

    public UserCardAdapter(List<User> userList, Context context) {
        this.userList = userList;
        this.context = context;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_card, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.tvName.setText(user.getName()+",");
        holder.tvLocation.setText(user.getProvince());

        Picasso.get().load(user.getFirstImg()).into(holder.rvImageList);


        // Trong onBindViewHolder
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProfileDetailActivity.class);
            intent.putExtra(ProfileDetailActivity.EXTRA_USER_PROFILE, user);
            // Thêm flag vào Intent dưới dạng chuỗi "1"
            intent.putExtra("EXTRA_FLAG", "1"); // Hoặc bất kỳ giá trị nào bạn muốn
            context.startActivity(intent);
        });

    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView rvImageList;
        TextView tvLocation;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            rvImageList = itemView.findViewById(R.id.imgUser);
        }
    }


}
