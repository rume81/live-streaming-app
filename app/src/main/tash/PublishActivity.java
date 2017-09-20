package webhawksit.com.livestreaming.activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
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
import com.red5pro.streaming.source.R5AdaptiveBitrateController;
import com.red5pro.streaming.source.R5Camera;
import com.red5pro.streaming.source.R5Microphone;
import com.red5pro.streaming.view.R5VideoView;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

import static webhawksit.com.livestreaming.utils.Const.QUERY_STRING_FOR_STREAMING_SERVER_PING;
import static webhawksit.com.livestreaming.utils.Const.STREAMING_AND_CHAT_SERVER_ADDRESS;
import static webhawksit.com.livestreaming.utils.Const.STREAMING_SERVER_ACCESS_TOKEN;
import static webhawksit.com.livestreaming.utils.Const.STREAMING_SERVER_PORT;
import static webhawksit.com.livestreaming.utils.NetworkChecking.getConnectivityStatusString;


public class PublishActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    // common variable
    String TAG = PublishActivity.class.getSimpleName();
    Toolbar mPublishToolbar;
    ActionBar actbar;
    private Menu menu;

    // streaming variable
    // ui component
    Button mStopButton;
    Button publishButton;

    // red5 component
    public R5Configuration configuration;
    protected int cameraSelection = -1;
    protected int cameraOrientation = 0;
    protected Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
    protected List<Camera.Size> sizes = new ArrayList<Camera.Size>();
    public static String selected_item = null;
    protected boolean override = false;
    protected R5Camera r5Cam;
    protected R5Microphone r5Mic;
    protected R5VideoView surfaceForCamera;
    protected Camera camera;
    protected R5Stream stream;
    protected boolean isPublishing = false;

    // others
    boolean mReadyToStop = false;
    Handler handler, handler1;

    // chat variable
    // ui component
    RelativeLayout mChatLayout;
    RecyclerView mRecyclerView;
    ChatAdapter adapter;
    LinearLayoutManager mLinearLayoutManager;
    ImageView mBlurEffectPublish;
    private ProgressDialog mProgressDialog;
    SweetAlertDialog mRecordingStoppedDialog, mServerErrorDialog;

    SweetAlertDialog mSessionErrorDialog;
    int ServerCount = 0;

    // xmpp variable
    private ConnectXmpp mService;
    private boolean mBounded;

    // others
    ArrayList<ChatItem> chatItem;
    ChatItem chatListObject;
    List<String> blurStatus;
    List<String> orientationStatus;
    private BroadcastReceiver mBroadcastReceiver;
    boolean isRoomCreated = false;
    private boolean typingStarted;
    String userName;
    String mChat;
    String mSystemChat;
    boolean mBlurStatus = false;
    boolean isFrontCamera = true;

    // orientation detection listener variable
    OrientationEventListener myOrientationEventListener;

    // new
    boolean isWiFiConnected = false;
    Snackbar snackbar;
    RelativeLayout mPublishParent;
    private boolean internetConnected = true;
    boolean stopByButton = false;

    String ResponseStatus;
    int ResponseCode;
    private String tag_json_obj = "jobj_req";       // These tags will be used to cancel the requests

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
        String chatID = event.messageID;
        Log.d("xmpp: ", "From: " + from + "\nSubject: " + subject + "\nChat: " + chat + "\nChat ID: " + chatID);
        addAMessage(from, chat, subject, chatID);
    }

    // add messages to the array list item from event bus
    private void addAMessage(String user, String message, String subject, String messageID) {
        if (subject.equals("blur")) {
            Log.d("xmpp: ", "system message 1 received");
            blurStatus.add(message);
            //checkLastSystemMessage();
        } else if (subject.equals("orientation")) {
            Log.d("xmpp: ", "system message 2 received");
            orientationStatus.add(message);
        } else if (subject.equals("comment")) {
            chatListObject = new ChatItem();
            // check last message ID with last entered message ID in array list
            if (!chatItem.isEmpty()) {
                if (chatItem.get(chatItem.size() - 1).getChatMessageID() != messageID) {
                    chatListObject.setChatText(message);
                    chatListObject.setChatUserName(user);
                    chatListObject.setChatMessageID(messageID);
                    chatItem.add(chatListObject);
                }
            } else {
                chatListObject.setChatText(message);
                chatListObject.setChatUserName(user);
                chatListObject.setChatMessageID(messageID);
                chatItem.add(chatListObject);
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                    mRecyclerView.scrollToPosition(chatItem.size() - 1);
                }
            });
        }
    }

    // we used this method previously but from now we are not using this method as we are showing the blur effect
    //after tapping the blur on/off icon in camera view
    // check last status of the blur effect from event bus message coming from chat room
    public void checkLastSystemMessage() {
        if (blurStatus != null && !blurStatus.isEmpty()) {
            Log.d("xmpp", "Last blur status is: " + blurStatus.get(blurStatus.size() - 1));

            String mBlurStatus = blurStatus.get(blurStatus.size() - 1);
            if (mBlurStatus.equals("on")) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initBlureffect();
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBlurEffectPublish.setVisibility(View.GONE);
                        if (mRecyclerView.isShown()) {
                            mChatLayout.setBackground(getResources().getDrawable(R.drawable.bottom_gradient));
                        }
                    }
                });
            }
        }
    }

    // check last status of the orientation in degree from event bus message coming from chat room
    public String lastOrientationMessage() {
        String lastOrientation = null;
        if (orientationStatus != null && !orientationStatus.isEmpty()) {
            Log.d("xmpp:: ", "Last orientation status is: " + orientationStatus.get(orientationStatus.size() - 1));
            lastOrientation = orientationStatus.get(orientationStatus.size() - 1);
        }
        return lastOrientation;
    }

    // create dialog pop up for login error
    public void SessionErrorWarning() {
        mSessionErrorDialog
                .setTitleText(getString(R.string.session_error_dialog_title))
                .setContentText(getString(R.string.session_error_dialog_message))
                .setConfirmText(getString(R.string.create_again))
                .setCancelText(getString(R.string.no))
                .showCancelButton(true)
                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.cancel();
                    }
                })
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.cancel();
                        XmppLogin();
                    }
                })
                .show();
        mSessionErrorDialog.setCancelable(false);
        mSessionErrorDialog.setCanceledOnTouchOutside(false);
    }

    // create snack bar
    public void initSnackBar() {
        // show no internet image
        snackbar = Snackbar
                .make(mPublishParent, getString(R.string.no_internet_connection), Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.retry), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // go to wifi settings page tapping on retry button
                        startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
                        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                    }
                });

        // Changing message text color
        snackbar.setActionTextColor(Color.RED);

        // Changing action button text color
        View sbView = snackbar.getView();
        TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.YELLOW);
        snackbar.setDuration(Snackbar.LENGTH_INDEFINITE);
    }

    // xmpp login method
    public void XmppLogin() {
        mProgressDialog.show();
        // Initializing Internet Check
        if (NetworkChecking.hasConnection(PublishActivity.this)) {
            isWiFiConnected = true;
            if (snackbar.isShown()) {
                snackbar.dismiss();
            }
            try {
                Intent intent = new Intent(PublishActivity.this, ConnectXmpp.class);
                intent.putExtra("user", userName);
                intent.putExtra("pwd", userName);
                intent.putExtra("code", "0");
                startService(intent);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("xmpp: ", "UI:: Main Login Error: " + e.getMessage());
                mProgressDialog.dismiss();
                SessionErrorWarning();
            }
        } else {
            // if there is no internet
            isWiFiConnected = false;
            // Show Snack bar message
            snackbar.show();
        }
    }


    //Method to register runtime broadcast receiver to show internet connection status
    private void registerInternetCheckReceiver() {
        IntentFilter internetFilter = new IntentFilter();
        internetFilter.addAction("android.net.wifi.STATE_CHANGE");
        internetFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(broadcastReceiver, internetFilter);
    }


    //Runtime Broadcast receiver inner class to capture internet connectivity events
    public BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = getConnectivityStatusString(context);
            String internetStatus = "";
            if (status.equalsIgnoreCase("Wifi enabled") || status.equalsIgnoreCase("Mobile data enabled")) {
                internetStatus = "Internet Connected";
            } else {
                internetStatus = "Lost Internet Connection";
            }

            if (internetStatus.equalsIgnoreCase("Lost Internet Connection")) {
                if (internetConnected) {
                    if (!snackbar.isShown()) {
                        snackbar.show();
                    }
                    Log.d("xmpp", "connectivity1:: " + internetStatus);
                    internetConnected = false;
                }
            } else {
                if (!internetConnected) {
                    if (snackbar.isShown()) {
                        snackbar.dismiss();
                    }
                    Log.d("xmpp", "connectivity2:: " + internetStatus);
                    internetConnected = true;
                }
            }
        }
    };

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
        // register run time internet checking broadcast receiver
        registerInternetCheckReceiver();
        // register receiver for room creation
        LocalBroadcastManager.getInstance(PublishActivity.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("create"));
        // register receiver for room creation error
        LocalBroadcastManager.getInstance(PublishActivity.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("roomcreateerror"));
        // register receiver for room destroy error
        LocalBroadcastManager.getInstance(PublishActivity.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("destroyerror"));
        // register receiver for room destroy
        LocalBroadcastManager.getInstance(PublishActivity.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("destroy"));
        // register receiver for internet connection
        LocalBroadcastManager.getInstance(PublishActivity.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("internet"));
        // register receiver for room info
        LocalBroadcastManager.getInstance(PublishActivity.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("roominfo"));
        // register receiver for destroy duplicate room
        LocalBroadcastManager.getInstance(PublishActivity.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("destroyduplicate"));
        // register receiver for destroy duplicate room error
        LocalBroadcastManager.getInstance(PublishActivity.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("destroyduplicateerror"));
        // register receiver for room join
        LocalBroadcastManager.getInstance(PublishActivity.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("join"));
        // register receiver for join error
        LocalBroadcastManager.getInstance(PublishActivity.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("joinerror"));
        // register receiver for login
        LocalBroadcastManager.getInstance(PublishActivity.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("signin"));
        // register receiver for login error
        LocalBroadcastManager.getInstance(PublishActivity.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("signinerror"));
        // register receiver for user status
        LocalBroadcastManager.getInstance(PublishActivity.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("userstatus"));
        // register receiver for connection closed
        LocalBroadcastManager.getInstance(PublishActivity.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("connectionclosed"));
        // register receiver for connection closed error
        LocalBroadcastManager.getInstance(PublishActivity.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("connectionclosederror"));
    }

    @Override
    protected void onPause() {
        Log.d("xmpp", "onPause");
        // unregister camera status detector and device sensor
        if (isPublishing) {
            // front and back camera status
            isFrontCamera = true;
            // orientation detection listener disable
            //webchat
            myOrientationEventListener.disable();
        }
        // unregister all receiver
        unregisterReceiver(broadcastReceiver);
        // unregister receiver for the activity
        LocalBroadcastManager.getInstance(PublishActivity.this).unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("xmpp: ", "onCreate");

        // configure red5 server communication
        configuration = new R5Configuration(R5StreamProtocol.RTSP, Const.STREAMING_AND_CHAT_SERVER_ADDRESS, Const.STREAMING_PORT_ADDRESS, Const.STREAMING_SERVER_APP_NAME, Const.STREAMING_BUFFER_TIME);
        configuration.setLicenseKey(Const.RED5_SDK_LICENSE);
        configuration.setBundleID(PublishActivity.this.getPackageName());

        setContentView(R.layout.publish_stream);

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

        // Set up the toolbar.
        mPublishToolbar = (Toolbar) findViewById(R.id.publish_toolbar);
        setSupportActionBar(mPublishToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        actbar = getSupportActionBar();
        actbar.setDisplayHomeAsUpEnabled(true);
        actbar.setDisplayShowHomeEnabled(true);

        // get current user name from shared preference
        userName = PrefManager.getUserName(PublishActivity.this);

        // get device screen size
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        // and calculate for circle size
        final int width = (metrics.widthPixels) / (11) * 8;
        final int height = (metrics.heightPixels) / (5) * 2;
        Log.d("xmpp: ", "Screen Height: " + height + " and width: " + width);

        // init parent screen
        mPublishParent = (RelativeLayout) findViewById(R.id.publishParent);

        // waring dialog init
        mSessionErrorDialog = new SweetAlertDialog(PublishActivity.this, SweetAlertDialog.WARNING_TYPE);

        // blur effect image
        mBlurEffectPublish = (ImageView) findViewById(R.id.blurredImagePublish);


        // init snackbar
        initSnackBar();

        // progress dialog during creating room
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getString(R.string.getting_ready));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);

        // initialization of chat list
        // blur status string array
        blurStatus = new ArrayList<String>();
        // orientation status string array
        orientationStatus = new ArrayList<String>();
        // chat as comment array
        chatItem = new ArrayList<ChatItem>();
        // set the recycler view to inflate the list
        mRecyclerView = (RecyclerView) findViewById(R.id.chat_list);
        mLinearLayoutManager = new LinearLayoutManager(getApplicationContext());
        adapter = new ChatAdapter(getApplicationContext(), chatItem);

        mChatLayout = (RelativeLayout) findViewById(R.id.chat_layout);
        publishButton = (Button) findViewById(R.id.publishButton);

        // Go live button and it's action
        publishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Initializing Internet Check
                if (NetworkChecking.hasConnection(PublishActivity.this)) {
                    isWiFiConnected = true;
                    if (snackbar.isShown()) {
                        snackbar.dismiss();
                    }
                    // get streaming server status
                    GoToCheckStreamServer();
                } else {
                    // if there is no internet
                    isWiFiConnected = false;
                    // Show Snack bar message
                    snackbar.show();
                }
            }
        });

        // handler to show progress for certain time
        handler = new Handler();

        // stop live button and it's action
        mStopButton = (Button) findViewById(R.id.stopButton);
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mReadyToStop) {
                    mProgressDialog.setMessage(getString(R.string.stopping));
                    mProgressDialog.show();
                    // destroy chat room
                    DestroyChatRoom();
                } else {
                    // red5 server not ready to stop live
                    CommonUtilities.MakeToast(PublishActivity.this, getString(R.string.can_not_stop_live));
                }
            }
        });

        // screen initialization with camera
        initScreen();

        // Receive broadcast messages from the MyXMPP class
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // checking for type intent filter
                // get user online/offline status
                if (intent.getAction().equals("userstatus")) {
                    String userStatus = intent.getStringExtra("action");
                    if (userStatus.equals("online")) {
                        Log.d("xmpp: ", "user is Online");
                        PrefManager.setServerStatus(PublishActivity.this, "Yes");
                        // check room status
                        checkRoomStatus();
                    } else if (userStatus.equals("offline")) {
                        Log.d("xmpp: ", "user is offline");
                        PrefManager.setServerStatus(PublishActivity.this, "No");
                        // go to login
                        XmppLogin();
                    }
                }
                // get room information if there any room with same name
                else if (intent.getAction().equals("roominfo")) {
                    String infoStatus = intent.getStringExtra("action");
                    if (infoStatus.equals("yes")) {
                        // create new room
                        CreateChatRoom();
                    } else {
                        Log.d("xmpp: ", "we have duplicate room");
                        //join chat room
                        JoinChatRoom();
                    }
                }
                // get room creation response
                else if (intent.getAction().equals("create")) {
                    mProgressDialog.dismiss();
                    Log.d("xmpp: ", "successfully room Created");
                    isRoomCreated = true;
                    // configure room for getting messages
                    roomConfigForChat();
                    // view re-organize for live stream screen to show chat and stop button
                    publishButton.setVisibility(View.GONE);
                    mChatLayout.setVisibility(View.VISIBLE);
                    mStopButton.setVisibility(View.VISIBLE);
                    actbar.setDisplayHomeAsUpEnabled(false);
                    actbar.setDisplayShowHomeEnabled(false);

                    // inflate chat list adapter
                    mRecyclerView.setLayoutManager(mLinearLayoutManager);
                    mRecyclerView.setAdapter(adapter);
                    mLinearLayoutManager.setStackFromEnd(true);

                    // go to live broadcast
                    beginStream();
                }
                // get room creation error response
                else if (intent.getAction().equals("roomcreateerror")) {
                    mProgressDialog.dismiss();
                    Log.d("xmpp: ", "Error in room Creation");
                    String roomCreationStatus = intent.getStringExtra("action");
                    Log.d("xmpp: ", roomCreationStatus);
                    if (roomCreationStatus.equals("Creation failed - Missing acknowledge of room creation.")) {
                        Log.d("xmpp: ", "Conflict with existing room");
                        Toast.makeText(PublishActivity.this, getString(R.string.live_disconnected), Toast.LENGTH_LONG).show();
                    }
                }
                // get room join response
                else if (intent.getAction().equals("join")) {
                    Log.d("xmpp: ", "user has joined!");
                    // destroy duplicate room
                    destroyDuplicateRoom();
                }
                // get room join error response
                else if (intent.getAction().equals("joinerror")) {
                    Log.d("xmpp: ", "user can't joined!");
                    mProgressDialog.dismiss();
                    Toast.makeText(PublishActivity.this, getString(R.string.live_disconnected), Toast.LENGTH_LONG).show();
                }
                // get duplicate room destroy response
                else if (intent.getAction().equals("destroyduplicate")) {
                    Log.d("xmpp: ", "destroy duplicate room successfully");
                    //create new room
                    CreateChatRoom();
                }
                // get duplicate room destroy error response
                else if (intent.getAction().equals("destroyduplicateerror")) {
                    Log.d("xmpp: ", "destroy duplicate room error");
                    mProgressDialog.dismiss();
                    Toast.makeText(PublishActivity.this, getString(R.string.live_disconnected), Toast.LENGTH_LONG).show();
                }
                // get room destroy response
                else if (intent.getAction().equals("destroy")) {
                    Log.d("xmpp: ", "Record Stopped successfully");
                    stopByButton = true;
                    mProgressDialog.dismiss();
                    // stop publish after successful room destroy
                    finish();
                }
                // get room destroy error response
                else if (intent.getAction().equals("destroyerror")) {
                    Log.d("xmpp: ", "Error in room Destroy");
                    mProgressDialog.dismiss();
                    Toast.makeText(PublishActivity.this, getString(R.string.live_disconnected) + "\nOr" + "\n" + getString(R.string.can_not_stop_live), Toast.LENGTH_LONG).show();
                    finish();
                }
                // get sign in response
                else if (intent.getAction().equals("signin")) {
                    Log.d("xmpp", "Publish successfully Logged in ");
                    if (mSessionErrorDialog.isShowing()) {
                        mSessionErrorDialog.hide();
                    }
                    // room status
                    checkRoomStatus();
                }
                // get sign in error response
                else if (intent.getAction().equals("signinerror")) {
                    mProgressDialog.dismiss();
                    SessionErrorWarning();
                }
                // get connection closed (logout) response
                else if (intent.getAction().equals("connectionclosed")) {
                    PrefManager.setServerStatus(PublishActivity.this, "No");
                    if (isPublishing) {
                        Log.d("xmpp: ", "user disconnected and stop record stream");
                        RecordingErrorWarning(getString(R.string.network_error_title), getString(R.string.network_error_message_record_stream));
                    }
                }
                // get connection closed by error(logout) response
                else if (intent.getAction().equals("connectionclosederror")) {
                    PrefManager.setServerStatus(PublishActivity.this, "No");
                    if (isPublishing) {
                        Log.d("xmpp: ", "user disconnected and stop record stream");
                        RecordingErrorWarning(getString(R.string.network_error_title), getString(R.string.network_error_message_record_stream));
                    }
                }
                // get internet connection status response
                if (intent.getAction().equals("internet")) {
                    String internetStatus = intent.getStringExtra("action");
                    Log.d("xmpp: ", internetStatus);
                    if (isPublishing) {
                        if (internetStatus.equals("Lost Internet Connection")) {
                            Log.d("xmpp: ", "net disconnected and record stream");
                            RecordingErrorWarning(getString(R.string.network_error_title), getString(R.string.network_error_message_record_stream));
                        } else if (internetStatus.equals("Internet Connected")) {
                            Log.d("xmpp: ", "net connected and able to record stream");
                        }
                    }
                }

            }
        };

        final EditText chat_edit_text = (EditText) findViewById(R.id.publish_stream_type_comment);
        ImageView chat_send = (ImageView) findViewById(R.id.publish_stream_chat_send);

        // to set hint in edit text
        chat_edit_text.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    chat_edit_text.setHint("");
                else
                    chat_edit_text.setHint(getString(R.string.chat_hint));
            }
        });

        // chat message send button action
        chat_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mChat = chat_edit_text.getText().toString();
                if (!mChat.isEmpty()) {
                    String mSubject = "comment";
                    // send message method
                    sendMessage(mChat, mSubject);
                    chat_edit_text.setText("");
                }
            }
        });

        // check typing status method of user
        chat_edit_text.addTextChangedListener(new TextWatcher() {

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            public void afterTextChanged(Editable s) {

                if (!TextUtils.isEmpty(s.toString()) && s.toString().trim().length() == 1) {

                    Log.d("xmpp: ", "typing started event…");

                    typingStarted = true;

                    //send typing started status

                } else if (s.toString().trim().length() == 0 && typingStarted) {

                    Log.d("xmpp: ", "typing stopped event…");

                    typingStarted = false;

                    //send typing stopped status

                }

            }

        });

        final View contentView = findViewById(R.id.publishParent);
        contentView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                Rect r = new Rect();
                contentView.getWindowVisibleDisplayFrame(r);
                int screenHeight = contentView.getRootView().getHeight();

                // r.bottom is the position above soft keypad or device button.
                // if keypad is shown, the r.bottom is smaller than that before.
                int keypadHeight = screenHeight - r.bottom;

                Log.d("xmpp: ", "keypadHeight = " + keypadHeight);

                if (keypadHeight > screenHeight * 0.15) { // 0.15 ratio is perhaps enough to determine keypad height.
                    // keyboard is opened
                    Log.d("xmpp: ", "keyboard open");
                    // set the custom size of the effect circle
                    int open_width = (int) (width/1.5);
                    int open_height = (int) (height/1.5);
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(open_width, open_height);
                    params.addRule(RelativeLayout.CENTER_IN_PARENT);
                    mBlurEffectPublish.setLayoutParams(params);
                }
                else {
                    // keyboard is closed
                    Log.d("xmpp: ", "keyboard close");
                    // set the custom size of the effect circle
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
                    params.addRule(RelativeLayout.CENTER_IN_PARENT);
                    mBlurEffectPublish.setLayoutParams(params);
                }
            }
        });

    }

    // streaming server connection check method
    public void GoToCheckStreamServer() {
        mProgressDialog.show();
        ServerCount++;
        Log.d("xmpp: ", "Streaming Server Ping count: " + ServerCount);
        if (ServerCount % 4 == 0) {
            Log.d("xmpp: ", "Streaming Server time out");
            mProgressDialog.dismiss();
            // after 3rd attempt
            ServerErrorWarning(getString(R.string.stream_server_error_title), getString(R.string.stream_server_error_message));
        } else {
            getStreamingServerStatus();
        }
    }

    // orientation detection listener implementation
    private void CheckOrientation() {
        myOrientationEventListener
                = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {

            @Override
            public void onOrientationChanged(int arg0) {
                Log.d("xmpp: ", "DeviceOrientation: " + String.valueOf(arg0));
                String mSubject = "orientation";
                String mOrientation = null;
                // TODO Auto-generated method stub
                if (arg0 >= -25 && arg0 <= 25) {
                    mOrientation = "0";
                    if (orientationStatus != null && !orientationStatus.isEmpty()) {
                        if (isFrontCamera) {
                            if (!lastOrientationMessage().equals("0~f")) {
                                sendMessage(mOrientation + "~f", mSubject);
                            }
                        } else {
                            if (!lastOrientationMessage().equals("0~b")) {
                                sendMessage(mOrientation + "~b", mSubject);
                            }
                        }
                    } else {
                        if (isFrontCamera) {
                            sendMessage(mOrientation + "~f", mSubject);
                        } else {
                            sendMessage(mOrientation + "~b", mSubject);
                        }
                    }
                } else if (arg0 >= 65 && arg0 <= 115) {
                    mOrientation = "90";
                    if (orientationStatus != null && !orientationStatus.isEmpty()) {
                        if (isFrontCamera) {
                            if (!lastOrientationMessage().equals("90~f")) {
                                sendMessage(mOrientation + "~f", mSubject);
                            }
                        } else {
                            if (!lastOrientationMessage().equals("90~b")) {
                                sendMessage(mOrientation + "~b", mSubject);
                            }
                        }
                    } else {
                        if (isFrontCamera) {
                            sendMessage(mOrientation + "~f", mSubject);
                        } else {
                            sendMessage(mOrientation + "~b", mSubject);
                        }
                    }
                } else if (arg0 >= 155 && arg0 <= 205) {
                    mOrientation = "180";
                    if (orientationStatus != null && !orientationStatus.isEmpty()) {
                        if (isFrontCamera) {
                            if (!lastOrientationMessage().equals("180~f")) {
                                sendMessage(mOrientation + "~f", mSubject);
                            }
                        } else {
                            if (!lastOrientationMessage().equals("180~b")) {
                                sendMessage(mOrientation + "~b", mSubject);
                            }
                        }
                    } else {
                        if (isFrontCamera) {
                            sendMessage(mOrientation + "~f", mSubject);
                        } else {
                            sendMessage(mOrientation + "~b", mSubject);
                        }
                    }
                } else if (arg0 >= 245 && arg0 <= 295) {
                    mOrientation = "270";
                    if (orientationStatus != null && !orientationStatus.isEmpty()) {
                        if (isFrontCamera) {
                            if (!lastOrientationMessage().equals("270~f")) {
                                sendMessage(mOrientation + "~f", mSubject);
                            }
                        } else {
                            if (!lastOrientationMessage().equals("270~b")) {
                                sendMessage(mOrientation + "~b", mSubject);
                            }
                        }
                    } else {
                        if (isFrontCamera) {
                            sendMessage(mOrientation + "~f", mSubject);
                        } else {
                            sendMessage(mOrientation + "~b", mSubject);
                        }
                    }
                } else if (arg0 >= 345 && arg0 <= 360) {
                    mOrientation = "360";
                    if (orientationStatus != null && !orientationStatus.isEmpty()) {
                        if (isFrontCamera) {
                            if (!lastOrientationMessage().equals("360~f")) {
                                sendMessage(mOrientation + "~f", mSubject);
                            }
                        } else {
                            if (!lastOrientationMessage().equals("360~b")) {
                                sendMessage(mOrientation + "~b", mSubject);
                            }
                        }
                    } else {
                        if (isFrontCamera) {
                            sendMessage(mOrientation + "~f", mSubject);
                        } else {
                            sendMessage(mOrientation + "~b", mSubject);
                        }
                    }
                }
            }
        };

        // check if device able to detect orientation
        if (myOrientationEventListener.canDetectOrientation()) {
            Log.d("xmpp: ", "Can DetectOrientation");
            // orientation detection listener enable
            myOrientationEventListener.enable();
        } else {
            Log.d("xmpp: ", "Can't DetectOrientation");
        }
    }

    // create blur effect over the video view
    public void initBlureffect() {
        mBlurEffectPublish.setVisibility(View.VISIBLE);
        if (mRecyclerView.isShown()) {
            mChatLayout.setBackground(getResources().getDrawable(R.drawable.bottom_gradient));
        }
        Picasso.with(this)
                .load(R.drawable.mosiclayer21)
                .transform(new BlurTransformation(50))   // blur density
                .into(mBlurEffectPublish);
    }

    // create chat room
    public void CreateChatRoom() {
        Intent intent = new Intent(getBaseContext(), ConnectXmpp.class);
        intent.putExtra("user", userName);
        intent.putExtra("code", "5");
        startService(intent);
    }

    // check user status
    public void CheckUserStatus() {
        Intent intent = new Intent(getBaseContext(), ConnectXmpp.class);
        intent.putExtra("user", userName);
        intent.putExtra("code", "12");
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

    // destroy chat room
    public void DestroyChatRoom() {
        Intent intent = new Intent(getBaseContext(), ConnectXmpp.class);
        intent.putExtra("code", "6");
        startService(intent);
    }

    // configure room to receive chat
    public void roomConfigForChat() {
        Intent intent = new Intent(getBaseContext(), ConnectXmpp.class);
        intent.putExtra("code", "7");
        startService(intent);
    }

    // check if there already a chat room with same name
    public void checkRoomStatus() {
        Intent intent = new Intent(getBaseContext(), ConnectXmpp.class);
        intent.putExtra("code", "10");
        startService(intent);
    }

    // destroy duplicate chat room
    public void destroyDuplicateRoom() {
        Intent intent = new Intent(getBaseContext(), ConnectXmpp.class);
        intent.putExtra("code", "11");
        startService(intent);
    }

    // join chat room
    public void JoinChatRoom() {
        Intent intent = new Intent(getBaseContext(), ConnectXmpp.class);
        intent.putExtra("user", userName);
        intent.putExtra("room", userName);
        intent.putExtra("code", "1");
        startService(intent);
    }

    // device orientation detection
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // set always portrait mood
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    // new method after fixing camera issue
    public void initScreen() {
        // set Device default camera
        setDefaultCamera();
        //activate the camera
        Camera.getCameraInfo(cameraSelection, cameraInfo);
        setOrientationMod();
        // if we want to make more smoother camera event then stop previously opened camera
        //stopCamera();
        // for configuring the stream with the camera
        configureStream(true);
        // set the preview for camera
        surfaceForCamera.showDebugView(false);
        if (camera != null)
            camera.startPreview();
    }

    // set Device default camera
    protected void setDefaultCamera() {

        if (Camera.getNumberOfCameras() < 2) {
            // if has no front camera
            // make some message or logic
            CommonUtilities.MakeToast(PublishActivity.this, getString(R.string.no_front_camera));
            cameraSelection = 0;
        } else {
            Camera.CameraInfo tempInfo = new Camera.CameraInfo();
            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                Camera.getCameraInfo(i, tempInfo);
                if (tempInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    cameraSelection = i;
                    break;
                }
            }
            if (cameraSelection < 0) cameraSelection = 0;
        }
    }

    // Camera Orientation Method
    protected void setOrientationMod() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_90:
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                    degrees = 90;
                else
                    degrees = 270;
                break;
            case Surface.ROTATION_270:
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                    degrees = 270;
                else
                    degrees = 90;
                break;
        }
        degrees += cameraInfo.orientation;
        cameraOrientation = degrees;
    }

    // Stream Record Method
    protected void beginStream() {
        // configure stream
        configureStream(false);

        // set Camera preview with layout view
        surfaceForCamera.showDebugView(false);
        isPublishing = true;
        // set Random String as stream Name
        String uuid = UUID.randomUUID().toString();
        Log.d("xmpp: " + "uuid = ", uuid);
        // set user name as stream name
        PrefManager.setUserRecentStream(PublishActivity.this, userName);
        // Start stream (Core method)
        stream.publish(userName, R5Stream.RecordType.Live);

        // Start camera preview
        if (camera != null)
            camera.startPreview();

        // just send a init message to fire chat in web interface
        sendMessage("ok", "init");

        // orientation detection listener implementation
        //webchat
        CheckOrientation();
    }

    // Configure the stream to broadcast
    protected void configureStream(boolean startPreview) {
        stream = new R5Stream(new R5Connection(configuration));
        stream.setLogLevel(R5Stream.LOG_LEVEL_DEBUG);
        stream.connection.addListener(new R5ConnectionListener() {
            @Override
            public void onConnectionEvent(R5ConnectionEvent event) {
                Log.d("publish", "connection event code " + event.value() + "\n");
                switch (event.value()) {
                    case 0://open
                        Log.d("xmpp: ", "Connection Listener - Open");
                        break;
                    case 1://close
                        Log.d("xmpp: ", "Connection Listener - Close");
                        break;
                    case 2://error
                        Log.d("xmpp: ", "Connection Listener - Error: " + event.message);
                        break;
                }
            }
        });

        // Check the status of the stream
        stream.setListener(new R5ConnectionListener() {
            @Override
            public void onConnectionEvent(R5ConnectionEvent event) {
                switch (event) {
                    case CONNECTED:
                        Log.d("xmpp: ", "Stream Listener - Connected");
                        break;
                    case DISCONNECTED:
                        Log.d("xmpp: ", "Stream Listener - Disconnected");
                        // show dialog when recording going to be paused
                        if (isPublishing && !stopByButton) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    RecordingErrorWarning(getString(R.string.stream_error_title), getString(R.string.stream_error_message_play_stream));
                                }
                            });
                        }
                        break;
                    case START_STREAMING:
                        Log.d("xmpp: ", "Stream Listener - Started Streaming");
                        // Stop live button will enable after 15 seconds
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //Do after 15 seconds
                                mReadyToStop = true;
                            }
                        }, 15000);
                        break;
                    case STOP_STREAMING:
                        Log.d("xmpp: ", "Stream Listener - Stopped Streaming");
                        break;
                    case CLOSE:
                        Log.d("xmpp: ", "Stream Listener - Close");
                        break;
                    case TIMEOUT:
                        Log.d("xmpp: ", "Stream Listener - Timeout");
                        break;
                    case ERROR:
                        Log.d("xmpp: ", "Stream Listener - Error: " + event.message);
                        break;
                }
            }
        });

        // Set Audio
        r5Mic = new R5Microphone();

        // Attach camera to render as broadcast
        if (true) {
            setCamera();
            if (camera != null)
                stream.attachCamera(r5Cam);
        }

        //assign the surface to show the camera output
        surfaceForCamera = new R5VideoView(this);
        FrameLayout frame = ((FrameLayout) findViewById(R.id.preview_container));
        frame.removeAllViews();
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        frame.addView(surfaceForCamera, params);

        // attach broadcasting stream with local view
        surfaceForCamera.attachStream(stream);

        // Attach local Audio record with broadcasting stream
        if (true) {
            stream.attachMic(r5Mic);
        }

        if (false) {
            final R5AdaptiveBitrateController adaptiveBitrateController = new R5AdaptiveBitrateController();
            adaptiveBitrateController.AttachStream(stream);

            if (true) {
                //adaptiveBitrateController.requiresVideo = true;
            }
        }
        if (startPreview && camera != null)
            camera.startPreview();
    }

    // Set camera method
    protected void setCamera() {
        if (camera == null) {
            try {
                camera = Camera.open(cameraSelection);
            } catch (Exception e) {
                //e.printStackTrace();
                Log.d("xmpp: ", "Cannot connect to camera - moving on without it");
                CommonUtilities.MakeToast(PublishActivity.this, getString(R.string.can_not_connect_camera));
                return;
            }
            Camera.getCameraInfo(cameraSelection, cameraInfo);
            setOrientationMod();
            camera.setDisplayOrientation((cameraOrientation + (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT ? 180 : 0)) % 360);
            sizes = camera.getParameters().getSupportedPreviewSizes();
        }

        if (camera != null)
            camera.stopPreview();

        //add the camera for streaming
        if (selected_item != null) {
            Log.d("publisher", "selected_item " + selected_item);
            String bits[] = selected_item.split("x");
            int pW = Integer.valueOf(bits[0]);
            int pH = Integer.valueOf(bits[1]);

            if (r5Cam == null) {
                r5Cam = new R5Camera(camera, pW, pH); // pW, pH
                r5Cam.setBitrate(1000);   // for high quality 4500, medium 1000
            } else
                r5Cam.setCamera(camera);
        } else {
            Log.d("publisher", "does not have any selected_item ");
            if (r5Cam == null) {
                r5Cam = new R5Camera(camera, 426 , 240); //320, 240
                r5Cam.setBitrate(400);    // 1000
            } else
                r5Cam.setCamera(camera);
        }
        r5Cam.setOrientation(cameraOrientation);
    }

    // Camera Switch Method
    protected void toggleCamera() {
        cameraSelection = (cameraSelection + 1) % Camera.getNumberOfCameras();
        try {
            Camera.getCameraInfo(cameraSelection, cameraInfo);
            cameraSelection = cameraInfo.facing;
        } catch (Exception e) {
            // can't find camera at that index, set default
            cameraSelection = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        setOrientationMod();

        try {
            stopCamera();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("xmpp: ", e.getMessage());
        }
        setCamera();

        if (camera != null)
            camera.startPreview();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        // code added to avoid
        stopCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
    }

    // Stop camera
    private void stopCamera() {
        if (camera != null) {
            sizes.clear();

            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    // stop recording as broadcasting
    protected void stopPublishing() {
        if (stream != null && isPublishing) {
            stream.stop();
        }
        isPublishing = false;
        r5Cam = null;
    }

    @Override
    protected void onDestroy() {
        stopPublishing();
        try {
            stopCamera();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("xmpp: ", e.getMessage());
        }
        super.onDestroy();
    }

    // back arrow action
    @Override
    public boolean onSupportNavigateUp() {
        super.onBackPressed();
        return true;
    }

    // back button press method
    @Override
    public void onBackPressed() {
        if (isPublishing) {
            // no action
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.publish_menu, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.camera_switch) {
            if (isPublishing) {
                if (isFrontCamera) {
                    isFrontCamera = false;
                    Log.d("xmpp: ", "back camera");
                } else {
                    isFrontCamera = true;
                    Log.d("xmpp: ", "front camera");
                }
            }
            toggleCamera();
            return true;
        } else if (id == R.id.camera_filter) {
            String mSubject = "blur";
            if (isPublishing) {
                if (!mBlurStatus) {
                    mSystemChat = "on";
                    menu.getItem(1).setIcon(getResources().getDrawable(R.drawable.ic_action_image_colorize_green));
                    //webchat
                    sendMessage(mSystemChat, mSubject);
                    mBlurStatus = true;
                    initBlureffect();
                } else {
                    mSystemChat = "off";
                    menu.getItem(1).setIcon(getResources().getDrawable(R.drawable.ic_action_image_colorize));
                    //webchat
                    sendMessage(mSystemChat, mSubject);
                    mBlurStatus = false;
                    mBlurEffectPublish.setVisibility(View.GONE);
                    if (mRecyclerView.isShown()) {
                        mChatLayout.setBackground(getResources().getDrawable(R.drawable.bottom_gradient));
                    }
                }
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // create warning dialog pop up for any kind of error
    public void RecordingErrorWarning(String title, String message) {
        mRecordingStoppedDialog = new SweetAlertDialog(PublishActivity.this, SweetAlertDialog.WARNING_TYPE);
        mRecordingStoppedDialog
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
        mRecordingStoppedDialog.setCancelable(false);
        mRecordingStoppedDialog.setCanceledOnTouchOutside(false);
    }

    // create warning dialog pop up for streaming server error
    public void ServerErrorWarning(String title, String message) {
        mServerErrorDialog = new SweetAlertDialog(PublishActivity.this, SweetAlertDialog.WARNING_TYPE);
        mServerErrorDialog
                .setTitleText(title)
                .setContentText(message)
                .setConfirmText(getString(R.string.create_again))
                .setCancelText(getString(R.string.cancel))
                .showCancelButton(true)
                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.cancel();
                    }
                })
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.cancel();
                        GoToCheckStreamServer();
                    }
                })
                .show();

        mServerErrorDialog.setCancelable(false);
        mServerErrorDialog.setCanceledOnTouchOutside(false);
    }

    // method to check stream status is stream available or not
    public void getStreamingServerStatus() {
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                Const.STREAMING_SERVER_REQUEST_PROTOCOL + STREAMING_AND_CHAT_SERVER_ADDRESS + ":" + STREAMING_SERVER_PORT + QUERY_STRING_FOR_STREAMING_SERVER_PING + STREAMING_SERVER_ACCESS_TOKEN, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            ResponseStatus = response.getString("status");
                            ResponseCode = response.getInt("code");
                            Log.d("Xmpp: ", "Streaming Server Status: " + ResponseStatus + " and Response Code: " + ResponseCode);
                            if (ResponseCode == 200) {
                                String serverStatus = PrefManager.getServerStatus(PublishActivity.this);
                                if (serverStatus.equals("Yes")) {
                                    // check user status
                                    CheckUserStatus();
                                } else if (serverStatus.equals("No")) {
                                    // go to login
                                    XmppLogin();
                                } else {
                                    // go to login
                                    XmppLogin();
                                }
                            } else {
                                // write error code
                                GoToCheckStreamServer();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, "Error: " + error.getMessage());
                // write error code
                GoToCheckStreamServer();
            }
        });

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(jsonObjReq,
                tag_json_obj);
    }

}
