package com.example.datingapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datingapp.R;
// Đảm bảo PhotoPagerAdapter của bạn là một RecyclerView.Adapter
// và có thể xử lý việc hiển thị nhiều ảnh cho một người dùng.
import com.example.datingapp.adapter.PhotoPagerAdapter; // Giả định đây là RecyclerView.Adapter
import com.example.datingapp.model.User;

import java.util.List;

public class UserCardAdapter extends RecyclerView.Adapter<UserCardAdapter.ViewHolder> {

    private Context context;
    private List<User> userList;

    public UserCardAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng LayoutInflater từ Context của parent để đảm bảo đúng theme
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);

        // Hiển thị tên người dùng (có thể thêm tuổi nếu User model có)
        String nameAndAge = user.getName();
        // Nếu bạn có trường age trong User và muốn hiển thị:
        // if (user.getAge() > 0) { // Giả sử age là năm sinh, bạn cần tính tuổi thực tế ở đây
        //    // int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        //    // nameAndAge += ", " + (currentYear - user.getAge());
        //    // Hoặc nếu age là tuổi thực:
        //    nameAndAge += ", " + user.getAge();
        // }
        holder.tvName.setText(nameAndAge);

        // Lấy danh sách URL ảnh từ người dùng
        List<String> imgUrls = user.getImgUrls();
        if (imgUrls != null && !imgUrls.isEmpty()) {
            // Khởi tạo PhotoPagerAdapter với Context và danh sách URL ảnh
            // PhotoPagerAdapter này sẽ có trách nhiệm tải và hiển thị TỪNG ảnh
            PhotoPagerAdapter photoAdapter = new PhotoPagerAdapter(holder.recyclerPhotos.getContext(), imgUrls);
            holder.recyclerPhotos.setAdapter(photoAdapter);
            // Đảm bảo adapter con được làm mới khi dữ liệu thay đổi
            photoAdapter.notifyDataSetChanged();
        } else {
            // Xử lý trường hợp không có ảnh hoặc danh sách rỗng (ví dụ: hiển thị ảnh placeholder)
            // Có thể bạn muốn reset adapter hoặc hiển thị một ảnh placeholder mặc định
            holder.recyclerPhotos.setAdapter(null); // Xóa adapter cũ nếu không có ảnh
            // Hoặc hiển thị một ImageView mặc định nếu không có ảnh nào trong RecyclerView lồng
            // holder.placeholderImageView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    // ⭐ Cập nhật ViewHolder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        RecyclerView recyclerPhotos; // RecyclerView để hiển thị nhiều ảnh

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.text_name); // Đảm bảo ID này có trong item_user_card.xml
            recyclerPhotos = itemView.findViewById(R.id.recycler_photos); // Đảm bảo ID này có trong item_user_card.xml

            // Cấu hình LinearLayoutManager cho RecyclerView con
            // ⭐ Sửa lỗi Context(): Sử dụng itemView.getContext() để lấy Context
            recyclerPhotos.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.VERTICAL, false));
            // Cấu hình này cho phép RecyclerView con cuộn mượt mà hơn trong RecyclerView cha
            recyclerPhotos.setNestedScrollingEnabled(true);
        }
    }

    /**
     * Phương thức tiện ích để cập nhật danh sách người dùng và làm mới adapter.
     * @param newUserList Danh sách người dùng mới.
     */
    public void updateUserList(List<User> newUserList) {
        this.userList.clear();
        this.userList.addAll(newUserList);
        notifyDataSetChanged();
    }
}