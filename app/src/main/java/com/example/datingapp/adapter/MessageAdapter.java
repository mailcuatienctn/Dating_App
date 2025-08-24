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
    private String currentUserId;
    private String chatPartnerAvatarUrl;

    public MessageAdapter(Context context, List<Message> messageList, String currentUserId, String chatPartnerAvatarUrl) {
        this.context = context;
        this.messageList = messageList;
        this.currentUserId = currentUserId;
        this.chatPartnerAvatarUrl = chatPartnerAvatarUrl;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if (message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_MESSAGE_SENT;
        } else {
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(context).inflate(R.layout.item_message_sent_bubble, parent, false);
            return new SentMessageHolder(view);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.item_message_received_bubble, parent, false);
            return new ReceivedMessageHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

        if (holder.getItemViewType() == VIEW_TYPE_MESSAGE_SENT) {
            SentMessageHolder sentHolder = (SentMessageHolder) holder;
            if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                sentHolder.messageImage.setVisibility(View.VISIBLE);
                sentHolder.messageBody.setVisibility(View.GONE);
                Glide.with(context)
                        .load(message.getImageUrl())
                        .placeholder(R.drawable.placeholder_image)
                        .into(sentHolder.messageImage);
            } else {
                sentHolder.messageImage.setVisibility(View.GONE);
                sentHolder.messageBody.setVisibility(View.VISIBLE);
                sentHolder.messageBody.setText(message.getText());
            }
            if (message.getTimestamp() != null) {
                sentHolder.messageTime.setText(sdf.format(message.getTimestamp()));
            }
        } else {
            ReceivedMessageHolder receivedHolder = (ReceivedMessageHolder) holder;
            if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                receivedHolder.messageImage.setVisibility(View.VISIBLE);
                receivedHolder.messageBody.setVisibility(View.GONE);
                Glide.with(context)
                        .load(message.getImageUrl())
                        .placeholder(R.drawable.placeholder_image)
                        .into(receivedHolder.messageImage);
            } else {
                receivedHolder.messageImage.setVisibility(View.GONE);
                receivedHolder.messageBody.setVisibility(View.VISIBLE);
                receivedHolder.messageBody.setText(message.getText());
            }
            if (message.getTimestamp() != null) {
                receivedHolder.messageTime.setText(sdf.format(message.getTimestamp()));
            }
            if (chatPartnerAvatarUrl != null && !chatPartnerAvatarUrl.isEmpty()) {
                Glide.with(context)
                        .load(chatPartnerAvatarUrl)
                        .placeholder(R.drawable.bg_avatar_circle)
                        .into(receivedHolder.messageAvatar);
            } else {
                receivedHolder.messageAvatar.setImageResource(R.drawable.bg_avatar_circle);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView messageBody, messageTime;
        ImageView messageImage;

        SentMessageHolder(View itemView) {
            super(itemView);
            messageBody = itemView.findViewById(R.id.text_message_body_sent);
            messageTime = itemView.findViewById(R.id.text_message_time_sent);
            messageImage = itemView.findViewById(R.id.image_message_sent);
        }
    }

    static class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView messageBody, messageTime;
        ImageView messageImage;
        CircleImageView messageAvatar;

        ReceivedMessageHolder(View itemView) {
            super(itemView);
            messageBody = itemView.findViewById(R.id.text_message_body_received);
            messageTime = itemView.findViewById(R.id.text_message_time_received);
            messageImage = itemView.findViewById(R.id.image_message_received);
            messageAvatar = itemView.findViewById(R.id.image_message_profile);
        }
    }

    public void addMessage(Message message) {
        messageList.add(message);
        notifyItemInserted(messageList.size() - 1);
    }

    public void updateMessages(List<Message> newMessages) {
        this.messageList.clear();
        this.messageList.addAll(newMessages);
        notifyDataSetChanged();
    }
}