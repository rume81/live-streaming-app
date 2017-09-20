package webhawksit.com.livestreaming.activity;


import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.red5pro.streaming.R5Connection;
import com.red5pro.streaming.R5Stream;
import com.red5pro.streaming.R5StreamProtocol;
import com.red5pro.streaming.config.R5Configuration;
import com.red5pro.streaming.event.R5ConnectionEvent;
import com.red5pro.streaming.event.R5ConnectionListener;
import com.red5pro.streaming.view.R5VideoView;
import com.rey.material.widget.ProgressView;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import webhawksit.com.livestreaming.R;
import webhawksit.com.livestreaming.chat.adapter.ChatAdapter;
import webhawksit.com.livestreaming.chat.model.ChatEvent;
import webhawksit.com.livestreaming.chat.model.ChatItem;
import webhawksit.com.livestreaming.chat.services.ConnectXmpp;
import webhawksit.com.livestreaming.chat.utils.LocalBinder;
import webhawksit.com.livestreaming.utils.AppController;
import webhawksit.com.livestreaming.utils.BlurTransformation;
import webhawksit.com.livestreaming.utils.CommonUtilities;
import webhawksit.com.livestreaming.utils.Const;
import webhawksit.com.livestreaming.utils.NetworkChecking;
import webhawksit.com.livestreaming.utils.PrefManager;

import static webhawksit.com.livestreaming.utils.Const.STREAMING_SERVER_ACCESS_TOKEN;

public class PlayStream extends AppCompatActivity {

    // common variable
    String TAG = PlayStream.class.getSimpleName();
    Toolbar mPlayToolbar;
    MenuItem menuItem;

    // streaming variable
    // UI component
    RelativeLayout mProgressLayout, mCommentLayout;
    ImageView mBlurEffect;
    ProgressView circularProgressBar;
    RelativeLayout mPlayStreamMainContent;

    // Red5 Component
    public R5Configuration configuration;
    protected R5Stream stream;
    R5VideoView videoView;

    // others
    Boolean isPlaying;
    String mParsedStreamName;
    Handler handler;
    private String tag_json_obj = "jobj_req";       // These tags will be used to cancel the requests
    // Single Stream Information from server
    String wholeData;
    String ResponseStatus;
    int ResponseCode;
    String activeUser;
    String isRecording;
    int ActualActiveUser;
    String visitedTimes;
    String createdAt;

    // chat variable
    // xmpp component
    private ConnectXmpp mService;
    private boolean mBounded;

    // UI component
    RecyclerView mRecyclerView;
    ChatAdapter adapter;
    LinearLayoutManager mLinearLayoutManager;
    EditText mChatInput;
    private ProgressDialog mProgressDialog;
    SweetAlertDialog mStreamStoppedDialog;

    // others
    boolean isWiFiConnected = false;
    ArrayList<ChatItem> chatItem;
    String userName;
    String mChat;
    String mSystemChat;
    boolean mBlurStatus = false;
    boolean isJoined;
    ChatItem chatListObject;
    String mCurrentUserName;
    List<String> blurStatus;
    private BroadcastReceiver mBroadcastReceiver;
    boolean mAlreadyDestroyedRoom = false;

    // xmpp connection service binder
    private final ServiceConnection mConnection = new ServiceConnection() {

        @SuppressWarnings("unchecked")
        @Override
        public void onServiceConnected(final ComponentName name,
                                       final IBinder service) {
            mService = ((LocalBinder<ConnectXmpp>) service).getService();
            mBounded = true;
            Log.d("xmpp:", "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            mService = null;
            mBounded = false;
            Log.d("xmpp:", "onServiceDisconnected");
        }
    };

    // event bus data received
    @Subscribe
    public void onMessageEvent(ChatEvent event) {
        String chat = event.message;
        String from = event.from;
        String subject = event.subject;
        Log.d("xmpp: ", "From: " + from + "\nSubject: " + subject + "\nChat: " + chat);
        addAMessage(from, chat, subject);
    }

    // add messages to the array list item from event bus
    private void addAMessage(String user, String message, String subject) {
        if (subject.equals("comment")) {
            chatListObject = new ChatItem();
            chatListObject.setChatText(message);
            chatListObject.setChatUserName(user);
            chatItem.add(chatListObject);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                    mRecyclerView.scrollToPosition(chatItem.size()-1);
                }
            });
        } else if (subject.equals("blur")) {
            Log.d("xmpp: ", "system message received");
            blurStatus.add(message);
            checkLastSystemMessage();
        }
    }

    // check last status of the blur effect from event bus message coming from chat room
    public void checkLastSystemMessage(){
        if (blurStatus != null && !blurStatus.isEmpty()) {
            Log.d("xmpp", "Last blur status is: "+blurStatus.get(blurStatus.size()-1));
            String mBlurStatus = blurStatus.get(blurStatus.size()-1);
            if (mBlurStatus.equals("on")) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initBlureffect();
                    }
                });
            }
            else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBlurEffect.setVisibility(View.GONE);
                        if(mRecyclerView.isShown()){
                            mCommentLayout.setBackground(getResources().getDrawable(R.drawable.bottom_gradient));
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("xmpp", "onStart");
        // register event bus
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        // unregister event bus
        EventBus.getDefault().unregister(this);
        Log.d("xmpp", "onStop");
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("xmpp", "onResume");
        // register receiver for room creation
        LocalBroadcastManager.getInstance(PlayStream.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("join"));

        // register receiver for internet connection
        LocalBroadcastManager.getInstance(PlayStream.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("internet"));

        // Initializing Internet Check
        if (NetworkChecking.hasConnection(PlayStream.this)) {
            isWiFiConnected = true;
            // if connected
            getStreamStatus(mParsedStreamName);

        } else {
            // if there is no internet
            isWiFiConnected = false;
            PlayErrorWarning(getString(R.string.network_error_title) , getString(R.string.network_error_message_play_stream));
        }
    }

    @Override
    protected void onPause() {
        Log.d("xmpp", "onPause");
        // unregister receiver for the activity
        LocalBroadcastManager.getInstance(PlayStream.this).unregisterReceiver(mBroadcastReceiver);
        stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        // Red5 Pro server communication establishment
        // configure red5 server communication
        configuration = new R5Configuration(R5StreamProtocol.RTSP, Const.STREAMING_AND_CHAT_SERVER_ADDRESS,  Const.STREAMING_PORT_ADDRESS, Const.STREAMING_SERVER_APP_NAME, Const.STREAMING_BUFFER_TIME);
        configuration.setLicenseKey(Const.RED5_SDK_LICENSE);
        configuration.setBundleID(PlayStream.this.getPackageName());

        setContentView(R.layout.play_stream);

        // create our manager instance after the content view is set
        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        // enable status bar tint
        tintManager.setStatusBarTintEnabled(true);
        // enable navigation bar tint
        tintManager.setNavigationBarTintEnabled(true);
        // set a custom tint color for all system bars
        tintManager.setTintColor(Color.parseColor("#08427a"));

        // keep device screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // keyboard adjustment
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        mPlayStreamMainContent = (RelativeLayout)findViewById(R.id.play_stream_main_content);

        // Set up the toolbar.
        mPlayToolbar = (Toolbar) findViewById(R.id.play_toolbar);
        setSupportActionBar(mPlayToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // get data from previous Activity
        Bundle bundle = getIntent().getExtras();
        mParsedStreamName = bundle.getString("stream_name");

        // get current user name from local storage
        mCurrentUserName = PrefManager.getUserName(PlayStream.this);

        mBlurEffect = (ImageView)findViewById(R.id.blurredImage);

        // initialization of chat list
        // blur status string array
        blurStatus = new ArrayList<String>();

        // chat as comment array
        chatItem = new ArrayList<ChatItem>();
        // set the recycler view to inflate the list
        mRecyclerView = (RecyclerView) findViewById(R.id.play_stream_chat_list);
        mLinearLayoutManager = new LinearLayoutManager(getApplicationContext());
        adapter = new ChatAdapter(getApplicationContext(), chatItem);

        // edit text layout and logic
        mChatInput = (EditText) findViewById(R.id.play_stream_type_comment);

        // check edit text focus
        mChatInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View arg0, boolean hasfocus) {
                if (hasfocus) {
                    Log.d("xmpp", "edit text focused");
                    mChatInput.setHint("");
                    mRecyclerView.setVisibility(View.VISIBLE);
                    // inflate chat list adapter when user pressed chat box
                    mRecyclerView.setLayoutManager(mLinearLayoutManager);
                    mRecyclerView.setAdapter(adapter);
                    mLinearLayoutManager.setStackFromEnd(true);
                } else {
                    Log.d("xmpp", "edit text not focused");
                    mChatInput.setHint(getString(R.string.chat_hint));
                    mRecyclerView.setVisibility(View.GONE);
                }
            }
        });

        // chat message send functionality
        ImageView chat_send = (ImageView) findViewById(R.id.play_stream_chat_send);

        // cha message send button action
        chat_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mChat = mChatInput.getText().toString();
                if (!mChat.isEmpty()) {
                    String mSubject = "comment";
                    // send message method
                    sendMessage(mChat, mSubject);
                    mChatInput.setText("");
                }
            }
        });

        mProgressLayout = (RelativeLayout) findViewById(R.id.player_loading_progress);
        mCommentLayout = (RelativeLayout) findViewById(R.id.play_stream_chat_layout);
        circularProgressBar = (ProgressView) findViewById(R.id.circular_progress);

        videoView = (R5VideoView) findViewById(R.id.subscribeView);

        // progress wheel thread sleep after certain time
        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do after 10 seconds
                mProgressLayout.setVisibility(View.GONE);
                mCommentLayout.setVisibility(View.VISIBLE);
            }
        }, 10000);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // checking for type intent filter
                if (intent.getAction().equals("join")) {
                    isJoined = true;
                    Log.d("xmpp: ", "user has joined!");
                    // start streaming
                    start(mParsedStreamName);
                }
                else if(intent.getAction().equals("exiterror")){
                    Log.d("xmpp: ", "error occurred when user tried to exit from the chat room");
                }
                if (intent.getAction().equals("internet")) {
                    String internetStatus = intent.getStringExtra("action");
                    Log.d(TAG, internetStatus);
                    if (isPlaying){
                        if(internetStatus.equals("Lost Internet Connection")){
                            Log.d(TAG, "net disconnected and stop stream");
                            PlayErrorWarning(getString(R.string.network_error_title) , getString(R.string.network_error_message_play_stream));
                        }
                        else if(internetStatus.equals("Internet Connected")){
                            Log.d(TAG, "net connected and able to start stream");
                        }
                    }
                }
            }
        };

    }

    // create blur effect over the video view
    public void initBlureffect(){
        mBlurEffect.setVisibility(View.VISIBLE);
        if(mRecyclerView.isShown()){
            mCommentLayout.setBackground(getResources().getDrawable(R.drawable.bottom_gradient));
        }
        Picasso.with(this)
                .load(R.drawable.mosaic_main)
                .transform(new BlurTransformation(20))     // blur density
                .into(mBlurEffect);

    }

    // join chat room
    public void JoinChatRoom() {
        Intent intent = new Intent(getBaseContext(), ConnectXmpp.class);
        intent.putExtra("user", mCurrentUserName);
        intent.putExtra("room", mParsedStreamName);
        intent.putExtra("code", "1");
        startService(intent);
    }

    // send message
    public void sendMessage(String chat, String subject) {
        Intent intent = new Intent(getBaseContext(), ConnectXmpp.class);
        intent.putExtra("chat", chat);
        intent.putExtra("subject", subject);
        intent.putExtra("code", "2");
        startService(intent);
    }

    // exit from chat room
    public void ExitFromChatRoom() {
        Intent intent = new Intent(getBaseContext(), ConnectXmpp.class);
        intent.putExtra("code", "3");
        startService(intent);
    }

    // create warning dialog pop up for stream playing error
    public void PlayErrorWarning(String title, String message) {
        mStreamStoppedDialog = new SweetAlertDialog(PlayStream.this, SweetAlertDialog.WARNING_TYPE);
        mStreamStoppedDialog
                .setTitleText(title)
                .setContentText(message)
                .setConfirmText(getString(R.string.go_back))
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.cancel();
                        finish();
                    }
                })
                .show();
        mStreamStoppedDialog.setCancelable(false);
        mStreamStoppedDialog.setCanceledOnTouchOutside(false);
    }

    // Start play stream in video view
    public void start(String streamName) {
        stream = new R5Stream(new R5Connection(configuration));
        videoView.attachStream(stream);
        stream.play(streamName);
        isPlaying = true;
        //set up streaming event listener
        stream.setListener(new R5ConnectionListener() {
            @Override
            public void onConnectionEvent(R5ConnectionEvent r5event) {
                //this is getting called from the network thread, so handle appropriately
                final R5ConnectionEvent event = r5event;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Context context = getApplicationContext();
                        CharSequence text = event.message;
                        String mEventMessage = String.valueOf(text);
                        Log.d(TAG, mEventMessage);
                        if (mEventMessage.equals("NetStream.Play.UnpublishNotify")) {
                            Log.d(TAG, getString(R.string.publisher_stop_broadcast));
                            PlayErrorWarning(getString(R.string.stream_finish), getString(R.string.publisher_stop_broadcast));
                        }
                        else if (mEventMessage.equals("No Valid Media Found")){
                            Log.d(TAG, getString(R.string.streaming_disconnected));
                            PlayErrorWarning(getString(R.string.stream_finish), getString(R.string.stream_ended));
                        }
                        // different state of the streams are as follows::
                        // 1) Started Streaming 2) Invalid License 3) NetStream.Play.UnpublishNotify 4) Disconnected 5) Closed
                    }
                });
            }
        });
    }

    // Stop Play stream and exit from the chat room
    public void stop() {
        if (stream != null) {
            // stop playing stream
            stream.stop();
            isPlaying = false;
        }
        if (isJoined){
          isJoined = false;
            // exit from chat room
            ExitFromChatRoom();
        }
    }

    // method to check stream status is stream available or not
    public void getStreamStatus(final String streamName) {

        final String streamNameParsed = streamName;

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                Const.JSON_URL_FOR_A_STREAMS + streamName + "?accessToken="+STREAMING_SERVER_ACCESS_TOKEN, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            ResponseStatus = response.getString("status");
                            ResponseCode = response.getInt("code");
                            Log.d(TAG, "Status: " + ResponseStatus + "\nCode: " + ResponseCode);
                            if (ResponseCode == 200) {
                                wholeData = response.getString("data");
                                JSONObject dataobj = new JSONObject(wholeData);
                                activeUser = dataobj.getString("active_subscribers");
                                // add myself (+1) as a active user
                                ActualActiveUser = Integer.parseInt(activeUser) + 1;
                                visitedTimes = dataobj.getString("total_subscribers");
                                isRecording = dataobj.getString("is_recording");
                                createdAt = dataobj.getString("creation_time");
                                // convert mil second to data format
                                String CreatedAt = convertDate(createdAt, "dd/MM/yyyy hh:mm:ss");
                                Log.d(TAG, "Visited " + visitedTimes + " Times\nActive User: " + ActualActiveUser + "\nIs Recording: " + isRecording + "\nCreated At: " + CreatedAt);
                                if (isRecording.equals("true")) {
                                    // if it's recording means on air then first join into the chat room and then go for play stream page
                                    // Join chat room
                                    JoinChatRoom();
                                } else {
                                    PlayErrorWarning(getString(R.string.stream_finish), getString(R.string.stream_ended));
                                }

                            } else {
                                PlayErrorWarning(getString(R.string.stream_finish), getString(R.string.stream_ended));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, "Error: " + error.getMessage());
                PlayErrorWarning(getString(R.string.stream_finish), getString(R.string.stream_ended));
            }
        });

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(jsonObjReq,
                tag_json_obj);
    }

    // method to convert mil seconds to date format
    public static String convertDate(String dateInMilliseconds, String dateFormat) {
        return DateFormat.format(dateFormat, Long.parseLong(dateInMilliseconds)).toString();
    }

    // back button press method
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.play_menu, menu);
        menuItem = menu.findItem(R.id.close);
        MenuItemCompat.getActionView(menuItem);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.close) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
