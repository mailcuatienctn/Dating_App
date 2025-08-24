package com.example.datingapp.adapter;

import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datingapp.ProfileDetailActivity;
import com.example.datingapp.R;
import com.example.datingapp.model.User;
import com.squareup.picasso.Picasso;

import java.util.List;

public class UserCardAdapter extends RecyclerView.Adapter<UserCardAdapter.UserViewHolder> {

    private List<User> userList;
    private Activity context;
    private double currentLat;
    private double currentLng;
    private static final int REQUEST_CODE_DETAIL = 1001;
    public UserCardAdapter(List<User> userList, Activity context, double currentLat, double currentLng) {
        this.userList = userList;
        this.context = context;
        this.currentLat = currentLat;
        this.currentLng = currentLng;
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

        holder.tvName.setText(user.getName() + ",");
        holder.tvAge.setText(String.valueOf(user.getAge()));
        holder.tvLocation.setText(user.getProvince());

        // Load ảnh
        Picasso.get().load(user.getFirstImg()).into(holder.rvImageList);

        // Tính khoảng cách
        if (user.getLatitude() != null && user.getLongitude() != null) {
            float[] results = new float[1];
            Location.distanceBetween(
                    currentLat, currentLng,
                    user.getLatitude(), user.getLongitude(),
                    results
            );
            float distanceInKm = results[0] / 1000f;
            // Format to one decimal place, e.g., "5.2 km"
            String distanceText = String.format("%.1f km", distanceInKm);
            holder.tvDistance.setText(distanceText);
        } else {
            holder.tvDistance.setText("N/A");
        }

        // Chuyển sang trang chi tiết khi bấm
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProfileDetailActivity.class);
            intent.putExtra(ProfileDetailActivity.EXTRA_USER_PROFILE, user);
            intent.putExtra("EXTRA_FLAG", "1");
            context.startActivityForResult(intent, REQUEST_CODE_DETAIL);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void updateUsers(List<User> newUserList) {
        this.userList.clear(); // Xóa dữ liệu cũ
        this.userList.addAll(newUserList); // Thêm dữ liệu mới
        notifyDataSetChanged(); // Thông báo cho adapter rằng toàn bộ tập dữ liệu đã thay đổi
    }

    public User getItem(int position) {
        if (position >= 0 && position < userList.size()) {
            return userList.get(position);
        }
        return null; // Trả về null nếu vị trí không hợp lệ
    }


    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAge, tvLocation, tvDistance;
        ImageView rvImageList;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvAge = itemView.findViewById(R.id.tvAge);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            rvImageList = itemView.findViewById(R.id.imgUser);
        }
    }
}