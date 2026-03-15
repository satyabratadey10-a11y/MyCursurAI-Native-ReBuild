package com.mycursurai.chat;

import android.animation.ValueAnimator;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RotateDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerMessages;
    private EditText etInput;
    private ImageButton btnSend;
    private FrameLayout inputBorderContainer;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;
    private Handler handler;
    private int replyIndex;

    private static final String[] AI_REPLIES = new String[]{
        "Processing your query through neural pathways...",
        "Analyzing context with liquid intelligence.",
        "I understand. Let me formulate a precise response.",
        "Fascinating input. Here is my synthesis.",
        "Running deep analysis on your message.",
        "My circuits are fully engaged on this.",
        "Distilling knowledge from the void...",
        "Signal received. Transmitting response.",
        "Thought matrix activated. Processing.",
        "Reality parsed. Here is the output."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        replyIndex = 0;
        handler = new Handler(Looper.getMainLooper());
        messageList = new ArrayList<ChatMessage>();
        recyclerMessages = (RecyclerView) findViewById(R.id.recycler_messages);
        etInput = (EditText) findViewById(R.id.et_input);
        btnSend = (ImageButton) findViewById(R.id.btn_send);
        inputBorderContainer = (FrameLayout) findViewById(R.id.input_border_container);
        chatAdapter = new ChatAdapter(messageList);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recyclerMessages.setLayoutManager(lm);
        recyclerMessages.setAdapter(chatAdapter);
        applyTriToneBackground();
        setupMovingBorder();
        setupSendButton();
    }

    private void applyTriToneBackground() {
        GradientDrawable bg = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xFFBF40BF, 0xFF702963, 0xFF000000});
        bg.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        findViewById(R.id.root_layout).setBackground(bg);
    }

    private void setupMovingBorder() {
        final GradientDrawable sweepGd = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xFFC084FC, 0xFF7C3AED, 0xFFA78BFA,
                      0xFF60A5FA, 0xFFF87171, 0xFFC084FC});
        sweepGd.setGradientType(GradientDrawable.SWEEP_GRADIENT);
        sweepGd.setCornerRadius(dpToPx(32));
        final RotateDrawable rd = new RotateDrawable();
        rd.setDrawable(sweepGd);
        rd.setFromDegrees(0f);
        rd.setToDegrees(360f);
        rd.setPivotXRelative(true);
        rd.setPivotX(0.5f);
        rd.setPivotYRelative(true);
        rd.setPivotY(0.5f);
        inputBorderContainer.setBackground(rd);
        ValueAnimator rotAnim = ValueAnimator.ofInt(0, 10000);
        rotAnim.setDuration(4000);
        rotAnim.setRepeatCount(ValueAnimator.INFINITE);
        rotAnim.setRepeatMode(ValueAnimator.RESTART);
        rotAnim.setInterpolator(new LinearInterpolator());
        rotAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator va) {
                rd.setLevel((int) va.getAnimatedValue());
            }
        });
        rotAnim.start();
    }

    private void setupSendButton() {
        btnSend.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    ScaleAnimation dn = new ScaleAnimation(
                        1.0f, 0.9f, 1.0f, 0.9f,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);
                    dn.setDuration(100);
                    dn.setFillAfter(true);
                    btnSend.startAnimation(dn);
                } else if (action == MotionEvent.ACTION_UP
                        || action == MotionEvent.ACTION_CANCEL) {
                    ScaleAnimation up = new ScaleAnimation(
                        0.9f, 1.0f, 0.9f, 1.0f,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);
                    up.setDuration(300);
                    up.setInterpolator(new OvershootInterpolator(2.5f));
                    up.setFillAfter(true);
                    btnSend.startAnimation(up);
                    if (action == MotionEvent.ACTION_UP) {
                        v.performClick();
                    }
                }
                return true;
            }
        });
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    private void sendMessage() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty()) { return; }
        etInput.setText("");
        messageList.add(new ChatMessage(text, ChatMessage.TYPE_USER));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();
        scheduleAiReply();
    }

    private void scheduleAiReply() {
        final int insertAt = messageList.size();
        messageList.add(new ChatMessage("...", ChatMessage.TYPE_AI));
        chatAdapter.notifyItemInserted(insertAt);
        scrollToBottom();
        final int idx = replyIndex;
        replyIndex = replyIndex + 1;
        if (replyIndex >= AI_REPLIES.length) { replyIndex = 0; }
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (insertAt < messageList.size()) {
                    messageList.set(insertAt,
                        new ChatMessage(AI_REPLIES[idx], ChatMessage.TYPE_AI));
                    chatAdapter.notifyItemChanged(insertAt);
                    scrollToBottom();
                }
            }
        }, 1400);
    }

    private void scrollToBottom() {
        final int pos = messageList.size() - 1;
        if (pos >= 0) {
            recyclerMessages.post(new Runnable() {
                @Override
                public void run() {
                    recyclerMessages.smoothScrollToPosition(pos);
                }
            });
        }
    }

    private int dpToPx(int dp) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * d);
    }

    public static class ChatMessage {
        public static final int TYPE_USER = 0;
        public static final int TYPE_AI   = 1;
        public final String text;
        public final int type;
        public ChatMessage(String text, int type) {
            this.text = text; this.type = type;
        }
    }

    public static class ChatAdapter
            extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {
        private final List<ChatMessage> messages;
        public ChatAdapter(List<ChatMessage> messages) {
            this.messages = messages;
        }
        @Override
        public int getItemViewType(int pos) {
            return messages.get(pos).type;
        }
        @Override
        public MessageViewHolder onCreateViewHolder(
                ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
            return new MessageViewHolder(v);
        }
        @Override
        public void onBindViewHolder(MessageViewHolder h, int pos) {
            ChatMessage msg = messages.get(pos);
            if (msg.type == ChatMessage.TYPE_USER) {
                h.containerUser.setVisibility(View.VISIBLE);
                h.containerAi.setVisibility(View.GONE);
                h.tvUser.setText(msg.text);
            } else {
                h.containerUser.setVisibility(View.GONE);
                h.containerAi.setVisibility(View.VISIBLE);
                h.tvAi.setText(msg.text);
            }
        }
        @Override
        public int getItemCount() { return messages.size(); }
        public static class MessageViewHolder
                extends RecyclerView.ViewHolder {
            public final LinearLayout containerUser;
            public final LinearLayout containerAi;
            public final TextView tvUser;
            public final TextView tvAi;
            public MessageViewHolder(View v) {
                super(v);
                containerUser =
                    (LinearLayout) v.findViewById(R.id.container_user);
                containerAi =
                    (LinearLayout) v.findViewById(R.id.container_ai);
                tvUser = (TextView) v.findViewById(R.id.tv_user_message);
                tvAi   = (TextView) v.findViewById(R.id.tv_ai_message);
            }
        }
    }
}
