package com.example.iotapp;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iotapp.database.AppDatabase;
import com.example.iotapp.models.Coordinates;
import com.example.iotapp.models.Message;

import java.util.ArrayList;
import java.util.List;

public class MessageListActivity extends AppCompatActivity implements BluetoothManager.BluetoothConnectionListener {

    private MessageAdapter messageAdapter;
    private List<Message> messages;
    private BluetoothManager bluetoothManager;
    private String deviceImei; // You should get this from your device settings
    private RecyclerView messagesRecyclerView;
    private ArrayAdapter<CharSequence> statusTypeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_list);

        setupUI();
        initializeBluetoothManager();
        initializeMessageList();
        loadMessages();

        // Refresh messages every 10s
        scheduleMessageLoading();

    }

    private final Handler handler = new Handler();
    private final Runnable messageLoader = new Runnable() {
        @Override
        public void run() {
            loadMessages();
            handler.postDelayed(this, 10000); // Schedule next execution in 10 seconds
        }
    };

    private void scheduleMessageLoading() {
        handler.postDelayed(messageLoader, 10000); // Start after 10 seconds
    }

    private void setupUI() {
        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Messages");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize RecyclerView
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Set Adapter
        messageAdapter = new MessageAdapter(messages);
        messagesRecyclerView.setAdapter(messageAdapter);


    }
    private void loadMessages(){
        // Load messages from the database
        new Thread(() -> {
            List<Message> messages = AppDatabase.getInstance(this).messageDao().getAllMessages();
            runOnUiThread(() -> {
                this.messages.clear();
                this.messages.addAll(messages);
                messageAdapter.notifyDataSetChanged();
            });
        }).start();
    }
    private void initializeBluetoothManager() {
        bluetoothManager = BluetoothManager.getInstance();
        bluetoothManager.setConnectionListener(this);

        // Check if Bluetooth is available and connected
        if (!bluetoothManager.isBluetoothAvailable()) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        /*
        if (!bluetoothManager.isConnected()) {
            Toast.makeText(this, "Not connected to any device", Toast.LENGTH_SHORT).show();
            finish();
        }

        */
    }

    private void initializeMessageList() {
        messages = new ArrayList<Message>();
        messageAdapter = new MessageAdapter(messages);
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messagesRecyclerView.setAdapter(messageAdapter);
    }

    public void addMessage(Message message) {
        if (message != null) {
            messages.add(message);
            messageAdapter.notifyItemInserted(messages.size() - 1);

            // Scroll to bottom
            RecyclerView messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
            messagesRecyclerView.smoothScrollToPosition(messages.size() - 1);
        }
    }

    @Override
    public void onConnectionSuccess(String deviceName) {
        Toast.makeText(this, "Connected to " + deviceName, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(String errorMessage) {
        Toast.makeText(this, "Connection failed: " + errorMessage, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onDisconnected() {
        Toast.makeText(this, "Disconnected from device", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onMessageReceived(String message) {
        Message receivedMessage = Message.fromBluetoothMessage(message);
        if (receivedMessage != null) {
            runOnUiThread(() -> {
                // Add it to the database
                new Thread(() -> {
                    AppDatabase.getInstance(this).messageDao().insert(receivedMessage);
                    messages.add(receivedMessage);
                    messageAdapter.notifyItemInserted(messages.size() - 1);
                    if(receivedMessage.getType().equals("1")){
                        String coords[] = receivedMessage.getPayload().split("-");
                        String latitude = coords[0];
                        String longitude = coords[1];
                        Coordinates coordinates = AppDatabase.getInstance(this).coordinateDao().getCoordinatesById(receivedMessage.getId());
                        if(coordinates != null){
                            // Id already exists so update them
                            AppDatabase.getInstance(this).coordinateDao().updateCoordinatesById(receivedMessage.getId(), Double.valueOf(latitude), Double.valueOf(longitude));
                            Log.println(Log.INFO, "HOME_ACTIVITY", "Updating  coordinates");

                        }else{
                            // Create them
                            Coordinates newCoordinates = new Coordinates(receivedMessage.getId(), Double.valueOf(latitude), Double.valueOf(longitude));
                            AppDatabase.getInstance(this).coordinateDao().insert(newCoordinates);
                            Log.println(Log.INFO, "HOME_ACTIVITY", "Inserted new coordinates");
                        }
                    }
                }).start();
                messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
                messagesRecyclerView.smoothScrollToPosition(messages.size() - 1);
            });
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Navigate back to the previous activity
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(messageLoader, 10000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't disconnect here as other activities might need the connection
        bluetoothManager.setConnectionListener(null);
        handler.removeCallbacks(messageLoader);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(messageLoader);
    }



    // Adapter for RecyclerView
    public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
        private final List<Message> messages;
        private String[] userStatuses = getResources().getStringArray(R.array.user_statuses);
        public MessageAdapter(List<Message> messages) {
            this.messages = messages;
        }


        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_item, parent, false);
            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            Message message = messages.get(position);
            boolean corrupted = false;
            holder.messageTextView.setTextColor(Color.BLACK);
            if(message.getType().equals("2")){
                int index = Integer.parseInt(message.getPayload());
                if(index < userStatuses.length){
                    holder.messageTextView.setText(userStatuses[index]);
                    if(userStatuses[index].equals("SOS")){
                        holder.messageTextView.setTextColor(Color.RED);
                    }
                }else{
                    corrupted = true;
                }
            }else if(message.getType().equals("1")){
                holder.messageTextView.setText(message.getPayload());
                holder.messageTextView.setTextColor(Color.rgb(26, 92, 43));
            }
            if(!corrupted){
                holder.imeiTextView.setText("ID: " + message.getId());
                long millis = Long.parseLong(message.getTimestamp());
                java.util.Date date = new java.util.Date(millis);
                holder.timestampTextView.setText(date.toString());
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        // ViewHolder class
        class MessageViewHolder extends RecyclerView.ViewHolder {
            TextView messageTextView;
            TextView imeiTextView;
            TextView timestampTextView;

            public MessageViewHolder(@NonNull View itemView) {
                super(itemView);
                messageTextView = itemView.findViewById(R.id.messageTextView);
                imeiTextView = itemView.findViewById(R.id.imeiTextView);
                timestampTextView = itemView.findViewById(R.id.timestampTextView);
            }
        }
    }

}

