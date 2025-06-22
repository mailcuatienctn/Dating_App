package com.example.datingapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.example.datingapp.adapter.MessageAdapter;
import com.example.datingapp.model.Message;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    private RecyclerView recyclerView;
    private EditText editMessage;
    private Button btnSend;
    private MessageAdapter adapter;
    private List<Message> messageList = new ArrayList<>();

    public ChatFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        recyclerView = view.findViewById(R.id.recycler_messages);
        editMessage = view.findViewById(R.id.edit_message);
        btnSend = view.findViewById(R.id.btn_send);

        adapter = new MessageAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        btnSend.setOnClickListener(v -> {
            String msg = editMessage.getText().toString().trim();
            if (!msg.isEmpty()) {
                messageList.add(new Message(msg, true)); // true: người dùng gửi
                messageList.add(new Message("Dạ có", false)); // giả lập phản hồi
                adapter.notifyItemRangeInserted(messageList.size() - 2, 2);
                recyclerView.scrollToPosition(messageList.size() - 1);
                editMessage.setText("");
            }
        });
        return view;
    }
}
