package com.example.datingapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.datingapp.R;
import com.example.datingapp.model.Message;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

    private Context context;
    private List<Message> messageList;
    private String currentUserId; // UID của người dùng hiện tại
    private String chatPartnerAvatarUrl; // URL avatar của đối tác chat

    // Constructor không cần Firebase Auth
    public MessageAdapter(Context context, List<Message> messageList, String chatPartnerAvatarUrl) {
        this.context = context;
        this.messageList = messageList;
        this.currentUserId = "user_uid_123";  // Gán giá trị cố định là "user_uid_123"
        this.chatPartnerAvatarUrl = chatPartnerAvatarUrl;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if (message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_MESSAGE_SENT; // Tin nhắn do người dùng hiện tại gửi
        } else {
            return VIEW_TYPE_MESSAGE_RECEIVED; // Tin nhắn nhận được
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(context).inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageHolder(view);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MESSAGE_SENT:
                ((SentMessageHolder) holder).messageBody.setText(message.getText());
                if (message.getTimestamp() != null) {
                    ((SentMessageHolder) holder).messageTime.setText(sdf.format(message.getTimestamp()));
                }
                break;
            case VIEW_TYPE_MESSAGE_RECEIVED:
                ((ReceivedMessageHolder) holder).messageBody.setText(message.getText());
                if (message.getTimestamp() != null) {
                    ((ReceivedMessageHolder) holder).messageTime.setText(sdf.format(message.getTimestamp()));
                }
                // Load avatar của đối tác chat
                if (chatPartnerAvatarUrl != null && !chatPartnerAvatarUrl.isEmpty()) {
                    Glide.with(context)
                            .load(chatPartnerAvatarUrl)
                            .placeholder(R.drawable.bg_avatar_circle)
                            .error(R.drawable.bg_avatar_circle)
                            .into(((ReceivedMessageHolder) holder).messageAvatar);
                } else {
                    ((ReceivedMessageHolder) holder).messageAvatar.setImageResource(R.drawable.bg_avatar_circle);
                }
                break;
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // ViewHolder cho tin nhắn gửi đi
    static class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView messageBody, messageTime;

        SentMessageHolder(View itemView) {
            super(itemView);
            messageBody = itemView.findViewById(R.id.text_message_body_sent);
            messageTime = itemView.findViewById(R.id.messageTime);
        }
    }

    // ViewHolder cho tin nhắn nhận được
    static class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView messageBody, messageTime;
        CircleImageView messageAvatar;

        ReceivedMessageHolder(View itemView) {
            super(itemView);
            messageBody = itemView.findViewById(R.id.text_message_body_received);
            messageTime = itemView.findViewById(R.id.text_message_time_received);
            messageAvatar = itemView.findViewById(R.id.image_message_profile);
        }
    }

    // Phương thức để cập nhật dữ liệu tin nhắn
    public void addMessage(Message message) {
        messageList.add(message);
        notifyItemInserted(messageList.size() - 1);
        // Có thể cần notifyDataSetChanged() hoặc notifyItemRangeInserted() nếu thêm nhiều
    }

    public void updateMessages(List<Message> newMessages) {
        this.messageList.clear();
        this.messageList.addAll(newMessages);
        notifyDataSetChanged();
    }
}
