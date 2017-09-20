package webhawksit.com.livestreaming.activity;

import android.annotation.SuppressLint;
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
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.appyvet.rangebar.RangeBar;
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
    protected boolean isUserDisconnected = false;

    // others
    boolean mReadyToStop = false;
    Handler handler, handler2;
    boolean loginCalledFromMainPage = false;

    // chat variable
    // ui component
    RelativeLayout mChatLayout;
    RecyclerView mRecyclerView;
    ChatAdapter adapter;
    LinearLayoutManager mLinearLayoutManager;
    ImageView mBlurEffectPublish;
    private ProgressDialog mProgressDialog;

    SweetAlertDialog mSessionErrorDialog, mNetworkErrorDialog, mAuthenticationErrorDialog;
    boolean isSessionErrorDialog = false;
    boolean isNetworkErrorDialog = false;
    boolean isAuthenticationErrorDialog = false;
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
    String userName, password;
    String mChat;
    String mSystemChat;
    boolean mBlurStatus = false;
    boolean isFrontCamera = true;
    boolean isSwitchCamera = false;

    // orientation detection listener variable
    OrientationEventListener myOrientationEventListener;
    boolean isOrientationEnabled;

    // new
    boolean isWiFiConnected = false;
    boolean isCheckedLogin = false;
    boolean isCheckedNetwork = false;
    RelativeLayout mPublishParent;
    boolean stopByButton = false;
    int mParentHeight, mParentWidth, mCircleDiameter;
    boolean isKeyboardOpen = false;

    String ResponseStatus;
    int ResponseCode;
    private String tag_json_obj = "jobj_req";       // These tags will be used to cancel the requests

    // Initializes the RangeBar in the application
    private RangeBar rangebar;

    // blur movement
    int dimensionToPixel, radiusCircle;
    private float xCoOrdinate, yCoOrdinate;
    float touchX = 0;
    float touchY = 0;
    float lastTouchX = 0;
    float lastTouchY = 0;
    float circleCenterX, circleCenterY;
    float CircleFromLeft, CircleFromTop;

    // xmpp connection service binder
    private final ServiceConnection mConnection = new ServiceConnection() {

        @SuppressWarnings("unchecked")
        @Override
        public void onServiceConnected(final ComponentName name,
                                       final IBinder service) {
            mService = ((LocalBinder<ConnectXmpp>) service).getService();
            mBounded = true;
            Log.d("publish_xmpp:", "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            mService = null;
            mBounded = false;
            Log.d("publish_xmpp:", "onServiceDisconnected");
        }
    };

    // event bus data received
    @Subscribe
    public void onMessageEvent(ChatEvent event) {
        String chat = event.message;
        String from = event.from;
        String subject = event.subject;
        String chatID = event.messageID;
        Log.d("publish_xmpp: ", "From: " + from + "\nSubject: " + subject + "\nChat: " + chat + "\nChat ID: " + chatID);
        addAMessage(from, chat, subject, chatID);
    }

    // add messages to the array list item from event bus
    private void addAMessage(String user, String message, String subject, String messageID) {
        if (subject.equals("blur")) {
            Log.d("publish_xmpp: ", "system message 1 received");
            blurStatus.add(message);
            //checkLastSystemMessage();
        } else if (subject.equals("orientation")) {
            Log.d("publish_xmpp: ", "system message 2 received");
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
            Log.d("publish_xmpp: ", "Last blur status is: " + blurStatus.get(blurStatus.size() - 1));

            String mBlurStatus = blurStatus.get(blurStatus.size() - 1);
            if (mBlurStatus.equals("on")) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initBlurEffect();
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
            //Log.d("publish_xmpp: ", "Last orientation status is: " + orientationStatus.get(orientationStatus.size() - 1));
            lastOrientation = orientationStatus.get(orientationStatus.size() - 1);
        }
        return lastOrientation;
    }

    // create dialog pop up for login error
    public void SessionErrorWarningDialog(final String buttonText, String title, String message) {
        mSessionErrorDialog = new SweetAlertDialog(PublishActivity.this, SweetAlertDialog.WARNING_TYPE);
        mSessionErrorDialog
                .setTitleText(title)
                .setContentText(message)
                .setConfirmText(buttonText)
                .setCancelText(getString(R.string.cancel))
                .showCancelButton(true)
                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        if (buttonText.equals("Go Back")) {
                            toast(getString(R.string.can_not_cancel));
                        } else {
                            sDialog.cancel();
                            finish();
                        }
                    }
                })
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        // for re-connecting stream server
                        if (buttonText.equals("Try again")) {
                            if (isWiFiConnected) {
                                sDialog.cancel();
                                GoToCheckStreamServer();
                            } else {
                                // network error warning
                                sDialog.cancel();
                                NetworkErrorWarning(getString(R.string.no_internet_toolber), getString(R.string.network_error_title), getString(R.string.no_internet_connection));
                            }
                        } else if (buttonText.equals("Re-connect")) {
                            if (isWiFiConnected) {
                                sDialog.cancel();
                                showProgress(getString(R.string.session_creating));
                                XmppLogin();
                            } else {
                                // network error warning
                                sDialog.cancel();
                                NetworkErrorWarning(getString(R.string.no_internet_toolber), getString(R.string.network_error_title), getString(R.string.no_internet_connection));
                            }
                        } else if (buttonText.equals("Go Back")) {
                            sDialog.cancel();
                            finish();
                        }
                    }
                })
                .show();
        isSessionErrorDialog = true;
        mSessionErrorDialog.setCancelable(false);
        mSessionErrorDialog.setCanceledOnTouchOutside(false);
    }

    // create dialog pop up for internet error
    public void NetworkErrorWarning(final String buttonText, String title, String message) {
        mNetworkErrorDialog = new SweetAlertDialog(PublishActivity.this, SweetAlertDialog.WARNING_TYPE);
        mNetworkErrorDialog
                .setTitleText(title)
                .setContentText(message)
                .setConfirmText(buttonText)
                .setCancelText(getString(R.string.cancel))
                .showCancelButton(true)
                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        if (buttonText.equals("Go Back")) {
                            toast(getString(R.string.can_not_cancel));
                        } else {
                            sDialog.cancel();
                            finish();
                        }
                    }
                })
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.cancel();
                        // go to wifi settings page tapping on retry button
                        startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
                        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                    }
                })
                .show();
        isNetworkErrorDialog = true;
        mNetworkErrorDialog.setCancelable(false);
        mNetworkErrorDialog.setCanceledOnTouchOutside(false);
    }

    // create login warning dialog for Authentication error
    public void AuthenticationErrorWarning() {
        mAuthenticationErrorDialog = new SweetAlertDialog(PublishActivity.this, SweetAlertDialog.WARNING_TYPE);
        mAuthenticationErrorDialog
                .setTitleText(getString(R.string.login_error_dialog_title))
                .setContentText(getString(R.string.login_failed_message))
                .setConfirmText(getString(R.string.try_again))
                .setCancelText(getString(R.string.exit))
                .showCancelButton(true)
                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.cancel();
                        finish();
                    }
                })
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.cancel();
                        // launch login screen
                        launchLoginScreen();
                    }
                })
                .show();
        isAuthenticationErrorDialog = true;
        mAuthenticationErrorDialog.setCancelable(false);
        mAuthenticationErrorDialog.setCanceledOnTouchOutside(false);
    }

    // Launch Login Screen
    private void launchLoginScreen() {
        Intent i = new Intent(PublishActivity.this, Login.class);
        startActivity(i);
        finish();
        // animation to go to next page
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
    }

    // show progress dialog
    public void showProgress(String message) {
        if (!PublishActivity.this.isFinishing()) {
            // if previously not showing it then show that
            if (!mProgressDialog.isShowing()) {
                mProgressDialog.setMessage(message);
                mProgressDialog.show();
            }
        }
    }

    // hide progress dialog
    public void hideProgress() {
        // if previously showing it then hide that
        if (mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    // hide session error
    public void hideSessionErrorDialog() {
        if (isSessionErrorDialog) {
            mSessionErrorDialog.dismiss();
        }
    }

    // hide network error
    public void hideNetworkErrorDialog() {
        if (isNetworkErrorDialog) {
            mNetworkErrorDialog.dismiss();
        }
    }

    // hide session error
    public void hideAuthenticationErrorDialog() {
        if (isAuthenticationErrorDialog) {
            mAuthenticationErrorDialog.dismiss();
        }
    }

    // get user login info from shared preference
    public void getUserInfo() {
        userName = PrefManager.getUserName(PublishActivity.this);
        password = PrefManager.getUserPassword(PublishActivity.this);
    }

    // xmpp login method
    public void XmppLogin() {
        loginCalledFromMainPage = true;
        // go to service to make login
        try {
            Intent intent = new Intent(PublishActivity.this, ConnectXmpp.class);
            intent.putExtra("user", userName);
            intent.putExtra("pwd", password);
            intent.putExtra("code", "0");
            startService(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("publish_xmpp: ", "Login exception: " + e.getMessage());
            // hide progress
            hideProgress();
            // show error dialog
            SessionErrorWarningDialog(getString(R.string.try_again), getString(R.string.signin_error), getString(R.string.login_error_dialog_message));
        }
    }

    // common toast method
    private void toast(String text) {
        Toast.makeText(PublishActivity.this, text, Toast.LENGTH_SHORT).show();
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
            // if suddenly lose internet connection
            if (internetStatus.equalsIgnoreCase("Lost Internet Connection")) {
                Log.d("publish_xmpp: ", "connectivity: " + internetStatus);
                isWiFiConnected = false;
                if (!isCheckedNetwork) {
                    isCheckedNetwork = true;
                    isCheckedLogin = false;
                    Log.d("publish_xmpp: ", "isCheckedNetwork");
                    // update user login information in shared preference
                    PrefManager.setServerStatus(PublishActivity.this, "No");
                }
            }
            // and when device connected with internet
            else {
                Log.d("publish_xmpp: ", "connectivity: " + internetStatus);
                isWiFiConnected = true;
                if (!isCheckedLogin) {
                    isCheckedLogin = true;
                    isCheckedNetwork = false;
                    Log.d("publish_xmpp: ", "isCheckedLogin");
                    if (!isPublishing) {
                        // check internet was disconnected recently and then login
                        if (PrefManager.getServerStatus(PublishActivity.this).equals("No")) {
                            // show progress
                            showProgress(getString(R.string.session_creating));
                            // hide error dialog
                            hideSessionErrorDialog();
                            hideNetworkErrorDialog();
                            hideAuthenticationErrorDialog();
                            // go to login
                            XmppLogin();
                        }
                    }
                }
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        Log.d("publish_xmpp: ", "onStart");
        // register event bus
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        // unregister event bus
        EventBus.getDefault().unregister(this);
        Log.d("publish_xmpp: ", "onStop");
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("publish_xmpp: ", "onResume");

        // Initializing Internet Check
        if (NetworkChecking.hasConnection(PublishActivity.this)) {
            isWiFiConnected = true;
            if (!isCheckedLogin) {
                isCheckedLogin = true;
                isCheckedNetwork = false;
                Log.d("publish_xmpp: ", "isCheckedLogin");
                // check internet was disconnected recently and then login
                if (PrefManager.getServerStatus(PublishActivity.this).equals("No")) {
                    // show progress
                    showProgress(getString(R.string.session_creating));
                    // go to login
                    XmppLogin();
                }
            }
        } else {
            // if there is no internet
            isWiFiConnected = false;
            if (!isCheckedNetwork) {
                isCheckedNetwork = true;
                isCheckedLogin = false;
                Log.d("publish_xmpp: ", "isCheckedNetwork");
                // update user login information in shared preference
                PrefManager.setServerStatus(PublishActivity.this, "No");
                // show error dialog
                NetworkErrorWarning(getString(R.string.no_internet_toolber), getString(R.string.network_error_title), getString(R.string.no_internet_connection));
            }
        }


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
        // register receiver for connection error
        LocalBroadcastManager.getInstance(PublishActivity.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("connectionerror"));
        // register receiver for connection closed on error
        LocalBroadcastManager.getInstance(PublishActivity.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("connectionclosederror"));
    }

    @Override
    protected void onPause() {
        Log.d("publish_xmpp: ", "onPause");
        // unregister camera status detector and device sensor
        if (isPublishing) {
            // front and back camera status
            isFrontCamera = true;
            // orientation detection listener disable
            //webchat
            if (isOrientationEnabled) {
                isOrientationEnabled = false;
                myOrientationEventListener.disable();
            }
        }
        // unregister all receiver
        unregisterReceiver(broadcastReceiver);
        // unregister receiver for the activity
        LocalBroadcastManager.getInstance(PublishActivity.this).unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    //initialize progress dialog object
    public void initProgressDialog() {
        mProgressDialog = new ProgressDialog(PublishActivity.this);
        mProgressDialog.setMessage(getString(R.string.getting_ready));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("publish_xmpp: ", "onCreate");

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

        // get user information from shared preference
        getUserInfo();

        // initialization progress dialog
        initProgressDialog();

        // Set up the toolbar.
        mPublishToolbar = (Toolbar) findViewById(R.id.publish_toolbar);
        setSupportActionBar(mPublishToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        actbar = getSupportActionBar();
        actbar.setDisplayHomeAsUpEnabled(true);
        actbar.setDisplayShowHomeEnabled(true);

        // get parent view size
        // and calculate view height and width
        mPublishParent = (RelativeLayout) findViewById(R.id.publishParent);
        mPublishParent.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @SuppressLint("NewApi")
            @SuppressWarnings("deprecation")
            @Override
            public void onGlobalLayout() {
                //now we can retrieve the width and height
                mParentWidth = mPublishParent.getWidth();
                mParentHeight = mPublishParent.getHeight();
                Log.d("publish_parent_xmpp: ", "Width: " + mParentWidth + " Height: " + mParentHeight);

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN)
                    mPublishParent.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                else
                    mPublishParent.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });

        // blur effect image
        mBlurEffectPublish = (ImageView) findViewById(R.id.blurredImagePublish);

        // Gets the RangeBar for blur image
        rangebar = (RangeBar) findViewById(R.id.rangebar);
        rangebar.setRangePinsByValue(0f, 1.0f);

        // Sets the display values of the indices and update blur image size
        rangebar.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
            @Override
            public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex,
                                              int rightPinIndex,
                                              String leftPinValue, String rightPinValue) {
                if (isKeyboardOpen) {
                    // Hide soft-keyboard:
                    Log.d("xmpp: ", "keyboard open and rangeBar clicked");
                    hideKeyboard();
                }
                int rangeValue = rightPinIndex;
                if (rangeValue == 0) {
                    mCircleDiameter = (int) (mParentHeight / 4.50);
                    radiusCircle = mCircleDiameter / 2;
                    Log.d("circle_diameter_xmpp: ", String.valueOf(mCircleDiameter));
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mCircleDiameter, mCircleDiameter);
                    mBlurEffectPublish.setLayoutParams(params);
                    sendMessage("0", "blur");
                } else if (rangeValue == 1) {
                    mCircleDiameter = (int) (mParentHeight / 3.75);
                    radiusCircle = mCircleDiameter / 2;
                    Log.d("circle_diameter_xmpp: ", String.valueOf(mCircleDiameter));
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mCircleDiameter, mCircleDiameter);
                    mBlurEffectPublish.setLayoutParams(params);
                    sendMessage("1", "blur");
                } else if (rangeValue == 2) {
                    mCircleDiameter = (int) (mParentHeight / 3.25);
                    radiusCircle = mCircleDiameter / 2;
                    Log.d("circle_diameter_xmpp: ", String.valueOf(mCircleDiameter));
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mCircleDiameter, mCircleDiameter);
                    mBlurEffectPublish.setLayoutParams(params);
                    sendMessage("2", "blur");
                } else if (rangeValue == 3) {
                    mCircleDiameter = (int) (mParentHeight / 2.75);
                    radiusCircle = mCircleDiameter / 2;
                    Log.d("circle_diameter_xmpp: ", String.valueOf(mCircleDiameter));
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mCircleDiameter, mCircleDiameter);
                    mBlurEffectPublish.setLayoutParams(params);
                    sendMessage("3", "blur");
                } else if (rangeValue == 4) {
                    mCircleDiameter = (int) (mParentHeight / 2.50);
                    radiusCircle = mCircleDiameter / 2;
                    Log.d("circle_diameter_xmpp: ", String.valueOf(mCircleDiameter));
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mCircleDiameter, mCircleDiameter);
                    mBlurEffectPublish.setLayoutParams(params);
                    sendMessage("4", "blur");
                } else if (rangeValue == 5) {
                    mCircleDiameter = (int) (mParentHeight / 2.25);
                    radiusCircle = mCircleDiameter / 2;
                    Log.d("circle_diameter_xmpp: ", String.valueOf(mCircleDiameter));
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mCircleDiameter, mCircleDiameter);
                    mBlurEffectPublish.setLayoutParams(params);
                    sendMessage("5", "blur");
                }

            }

        });

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
                if (isWiFiConnected) {
                    if (PrefManager.getServerStatus(PublishActivity.this).equals("Yes")) {
                        // get streaming server status
                        GoToCheckStreamServer();
                    } else {
                        SessionErrorWarningDialog(getString(R.string.create_again), getString(R.string.session_error_dialog_title), getString(R.string.session_error_dialog_message));
                    }
                } else {
                    NetworkErrorWarning(getString(R.string.no_internet_toolber), getString(R.string.network_error_title), getString(R.string.no_internet_connection));
                }
            }
        });

        // handler to show progress for certain time
        handler = new Handler();
        // handler to call login for certain time
        handler2 = new Handler();

        // stop live button and it's action
        mStopButton = (Button) findViewById(R.id.stopButton);
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mReadyToStop) {
                    showProgress(getString(R.string.stopping));
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
                // get room information if there any room with same name
                if (intent.getAction().equals("roominfo")) {
                    String infoStatus = intent.getStringExtra("action");
                    if (infoStatus.equals("yes")) {
                        // create new room
                        CreateChatRoom();
                    } else if (infoStatus.contains("Client is not, or no longer, connected")) {
                        Log.d("publish_xmpp: ", infoStatus);
                        // update user login information in shared preference
                        PrefManager.setServerStatus(PublishActivity.this, "No");
                        // hide progress dialog
                        hideProgress();
                        SessionErrorWarningDialog(getString(R.string.create_again), getString(R.string.session_error_dialog_title), getString(R.string.session_error_dialog_message));
                    } else {
                        Log.d("publish_xmpp: ", infoStatus + ": we have duplicate room");
                        //join chat room
                        JoinChatRoom();
                    }
                }
                // get room creation response
                else if (intent.getAction().equals("create")) {
                    Log.d("publish_xmpp: ", "successfully room Created");
                    isRoomCreated = true;
                    // hide progress
                    hideProgress();

                    // configure room for getting messages
                    roomConfigForChat();
                    // view re-organize for live stream screen to show chat and stop button
                    publishButton.setVisibility(View.GONE);
                    mChatLayout.setVisibility(View.VISIBLE);
                    // re size and positioning the chat layout
                    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, (int) ((mParentHeight - (mParentHeight / 2.25)) / 2));
                    lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    mChatLayout.setLayoutParams(lp);

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
                    // hide progress
                    hideProgress();

                    Toast.makeText(PublishActivity.this, getString(R.string.create_error) + "\n" + getString(R.string.try_again), Toast.LENGTH_LONG).show();
                    String roomCreationStatus = intent.getStringExtra("action");
                    Log.d("publish_xmpp: ", roomCreationStatus);
                    //"Creation failed - Missing acknowledge of room creation."
                }
                // get room join response
                else if (intent.getAction().equals("join")) {
                    Log.d("publish_xmpp: ", "user has joined!");
                    // destroy duplicate room
                    destroyDuplicateRoom();
                }
                // get room join error response
                else if (intent.getAction().equals("joinerror")) {
                    Log.d("xmpp: ", "user can't joined!");
                    // hide progress
                    hideProgress();
                    Toast.makeText(PublishActivity.this, getString(R.string.join_error) + "\n" + getString(R.string.try_again), Toast.LENGTH_LONG).show();
                }
                // get duplicate room destroy response
                else if (intent.getAction().equals("destroyduplicate")) {
                    Log.d("publish_xmpp: ", "destroy duplicate room successfully");
                    //create new room
                    CreateChatRoom();
                }
                // get duplicate room destroy error response
                else if (intent.getAction().equals("destroyduplicateerror")) {
                    Log.d("publish_xmpp: ", "destroy duplicate room error");
                    // hide progress
                    hideProgress();
                    Toast.makeText(PublishActivity.this, getString(R.string.destroy_error) + "\n" + getString(R.string.try_again), Toast.LENGTH_LONG).show();
                }
                // get room destroy response
                else if (intent.getAction().equals("destroy")) {
                    Log.d("publish_xmpp: ", "Record Stopped successfully");
                    stopByButton = true;
                    // hide progress
                    hideProgress();
                    // stop publish after successful room destroy
                    finish();
                }
                // get room destroy error response
                else if (intent.getAction().equals("destroyerror")) {
                    Log.d("publish_xmpp: ", "Error in room Destroy");
                    // hide progress
                    hideProgress();
                    finish();
                }
                // get sign in response
                else if (intent.getAction().equals("signin")) {
                    Log.d("publish_xmpp: ", "Publish successfully Logged in ");
                    loginCalledFromMainPage = false;
                    // hide progress dialog
                    hideProgress();
                    // update user login information in shared preference
                    PrefManager.setServerStatus(PublishActivity.this, "Yes");
                }
                // get sign in error response
                else if (intent.getAction().equals("signinerror")) {
                    String loginStatus = intent.getStringExtra("action");
                    if (loginStatus.contains("Client is not, or no longer, connected")) {
                        Log.d("publish_xmpp: ", loginStatus);
                        // call login again
                        XmppLogin();
                    } else if (loginStatus.contains("SASLError using SCRAM-SHA-1: not-authorized")) {
                        Log.d("publish_xmpp: ", loginStatus);
                        // hide progress dialog
                        hideProgress();
                        // show login error warning dialog
                        AuthenticationErrorWarning();
                    } else {
                        Log.d("publish_xmpp: ", loginStatus);
                        // hide progress dialog
                        hideProgress();
                        // show login error warning dialog
                        SessionErrorWarningDialog(getString(R.string.create_again), getString(R.string.login_error_dialog_title), getString(R.string.login_error_dialog_message));
                    }
                }
                // get connection error response
                else if (intent.getAction().equals("connectionerror")) {
                    Log.d("publish_xmpp: ", "Publish connection error!");
                    String connectionStatus = intent.getStringExtra("action");
                    if (connectionStatus.contains("after 30000ms:")) {
                        // login call after 4 seconds
                        handler2.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //Do after 4 seconds
                                XmppLogin();
                            }
                        }, 4000);
                    } else {
                        // hide progress dialog
                        hideProgress();
                        SessionErrorWarningDialog(getString(R.string.create_again), getString(R.string.login_error_dialog_title), getString(R.string.login_error_dialog_message));
                    }
                }
                // get connection closed on error response
                else if (intent.getAction().equals("connectionclosederror")) {
                    String sessionStatus = intent.getStringExtra("action");
                    Log.d("publish_xmpp: ", "Publish " + sessionStatus);
                    if (!loginCalledFromMainPage) {
                        if (isPublishing) {
                            isUserDisconnected = true;
                            // show error dialog that session closed on error
                            SessionErrorWarningDialog(getString(R.string.create_again), getString(R.string.session_error_dialog_title), getString(R.string.session_error_dialog_message));
                            // orientation detection listener disable
                            //webchat
                            if (isOrientationEnabled) {
                                isOrientationEnabled = false;
                                myOrientationEventListener.disable();
                            }
                            // update live screen UI remove stop button and show start button
                            UpdateViewWhenStreamStopped();
                            // stop live streaming
                            stopPublishing();
                        } else {
                            // show error dialog that session closed on error
                            SessionErrorWarningDialog(getString(R.string.create_again), getString(R.string.session_error_dialog_title), getString(R.string.session_error_dialog_message));
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

                    Log.d("publish_xmpp: ", "typing started event…");

                    typingStarted = true;

                    //send typing started status

                } else if (s.toString().trim().length() == 0 && typingStarted) {

                    Log.d("publish_xmpp: ", "typing stopped event…");

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

                if (keypadHeight > screenHeight * 0.15) { // 0.15 ratio is perhaps enough to determine keypad height.
                    // keyboard is opened
                    isKeyboardOpen = true;
                    // set the custom size of the effect circle
                    int mNewCircleDiameter = (int) (mParentHeight / 3.75);
                    int newRadiusCircle = mNewCircleDiameter / 2;
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mNewCircleDiameter, mNewCircleDiameter);
                    mBlurEffectPublish.setLayoutParams(params);
                } else {
                    // keyboard is closed
                    isKeyboardOpen = false;
                    // set the custom size of the effect circle
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mCircleDiameter, mCircleDiameter);
                    mBlurEffectPublish.setLayoutParams(params);
                }
            }
        });

        // blur image touch event listener
        mBlurEffectPublish.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        xCoOrdinate = view.getX() - event.getRawX();
                        yCoOrdinate = view.getY() - event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if ((circleCenterX + radiusCircle) > mParentWidth) {
                            touchX = event.getX();
                            if (touchX > lastTouchX) {
                                //don't move
                            } else {
                                view.animate().x(event.getRawX() + xCoOrdinate).y(event.getRawY() + yCoOrdinate).setDuration(0).start();
                                circleCenterX = view.getX() + radiusCircle;
                                circleCenterY = view.getY() + radiusCircle;
                                touchX = event.getX();
                                lastTouchX = touchX;
                                touchY = event.getY();
                                lastTouchY = touchY;
                                CircleFromLeft = view.getX();
                                CircleFromTop = view.getY();
                                // send coordinate messages into chat room
                                if (CircleFromLeft > 0.0 && CircleFromLeft < (mParentWidth - mCircleDiameter) && CircleFromTop > 0.0 && CircleFromTop < (mParentHeight - mCircleDiameter)) {
                                    sendMessage(CircleFromLeft + "~" + CircleFromTop, "blur");
                                }
                                Log.d("circle_first_xmpp:: ", " circle center X: " + circleCenterX + " circle center Y: " + circleCenterY + " touch center X: " + touchX + " touch center Y: " + touchY);
                            }
                        } else if (circleCenterX < radiusCircle) {
                            touchX = event.getX();
                            if (touchX < lastTouchX) {
                                //don't move
                            } else {
                                view.animate().x(event.getRawX() + xCoOrdinate).y(event.getRawY() + yCoOrdinate).setDuration(0).start();
                                circleCenterX = view.getX() + radiusCircle;
                                circleCenterY = view.getY() + radiusCircle;
                                touchX = event.getX();
                                lastTouchX = touchX;
                                touchY = event.getY();
                                lastTouchY = touchY;
                                CircleFromLeft = view.getX();
                                CircleFromTop = view.getY();
                                // send coordinate messages into chat room
                                if (CircleFromLeft > 0.0 && CircleFromLeft < (mParentWidth - mCircleDiameter) && CircleFromTop > 0.0 && CircleFromTop < (mParentHeight - mCircleDiameter)) {
                                    sendMessage(CircleFromLeft + "~" + CircleFromTop, "blur");
                                }
                                Log.d("circle_current_xmpp:: ", " circle center X: " + circleCenterX + " circle center Y: " + circleCenterY + " touch center X: " + touchX + " touch center Y: " + touchY);
                            }
                        } else if ((circleCenterY + radiusCircle) > mParentHeight) {
                            touchY = event.getY();
                            if (touchY > lastTouchY) {
                                //don't move
                            } else {
                                view.animate().x(event.getRawX() + xCoOrdinate).y(event.getRawY() + yCoOrdinate).setDuration(0).start();
                                circleCenterX = view.getX() + radiusCircle;
                                circleCenterY = view.getY() + radiusCircle;
                                touchX = event.getX();
                                lastTouchX = touchX;
                                touchY = event.getY();
                                lastTouchY = touchY;
                                CircleFromLeft = view.getX();
                                CircleFromTop = view.getY();
                                // send coordinate messages into chat room
                                if (CircleFromLeft > 0.0 && CircleFromLeft < (mParentWidth - mCircleDiameter) && CircleFromTop > 0.0 && CircleFromTop < (mParentHeight - mCircleDiameter)) {
                                    sendMessage(CircleFromLeft + "~" + CircleFromTop, "blur");
                                }
                                Log.d("circle_current_xmpp:: ", " circle center X: " + circleCenterX + " circle center Y: " + circleCenterY + " touch center X: " + touchX + " touch center Y: " + touchY);
                            }
                        } else if (circleCenterY < radiusCircle) {
                            touchY = event.getY();
                            if (touchY < lastTouchY) {
                                //don't move
                            } else {
                                view.animate().x(event.getRawX() + xCoOrdinate).y(event.getRawY() + yCoOrdinate).setDuration(0).start();
                                circleCenterX = view.getX() + radiusCircle;
                                circleCenterY = view.getY() + radiusCircle;
                                touchX = event.getX();
                                lastTouchX = touchX;
                                touchY = event.getY();
                                lastTouchY = touchY;
                                CircleFromLeft = view.getX();
                                CircleFromTop = view.getY();
                                // send coordinate messages into chat room
                                if (CircleFromLeft > 0.0 && CircleFromLeft < (mParentWidth - mCircleDiameter) && CircleFromTop > 0.0 && CircleFromTop < (mParentHeight - mCircleDiameter)) {
                                    sendMessage(CircleFromLeft + "~" + CircleFromTop, "blur");
                                }
                                Log.d("circle_current_xmpp:: ", " circle center X: " + circleCenterX + " circle center Y: " + circleCenterY + " touch center X: " + touchX + " touch center Y: " + touchY);
                            }
                        } else {
                            view.animate().x(event.getRawX() + xCoOrdinate).y(event.getRawY() + yCoOrdinate).setDuration(0).start();
                            circleCenterX = view.getX() + radiusCircle;
                            circleCenterY = view.getY() + radiusCircle;
                            touchX = event.getX();
                            lastTouchX = touchX;
                            touchY = event.getY();
                            lastTouchY = touchY;
                            CircleFromLeft = view.getX();
                            CircleFromTop = view.getY();
                            // send coordinate messages into chat room
                            if (CircleFromLeft > 0.0 && CircleFromLeft < (mParentWidth - mCircleDiameter) && CircleFromTop > 0.0 && CircleFromTop < (mParentHeight - mCircleDiameter)) {
                                sendMessage(CircleFromLeft + "~" + CircleFromTop, "blur");
                            }
                            Log.d("circle_last_xmpp:: ", " circle center X: " + circleCenterX + " circle center Y: " + circleCenterY + " touch center X: " + touchX + " touch center Y: " + touchY);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        break;
                    default:
                        return false;
                }
                return true;
            }
        });

        // crash handler initialization
        //Thread.setDefaultUncaughtExceptionHandler(new MyExceptionHandler(this));

    }

    // hide keyboard
    private void hideKeyboard() {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    // show keyboard
    private void showKeyboard() {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    // streaming server connection check method
    public void GoToCheckStreamServer() {
        showProgress(getString(R.string.getting_ready));
        ServerCount++;
        Log.d("publish_xmpp: ", "Streaming Server Ping count: " + ServerCount);
        if (ServerCount % 4 == 0) {
            Log.d("publish_xmpp: ", "Streaming Server time out");
            hideProgress();
            // after 3rd attempt
            SessionErrorWarningDialog(getString(R.string.try_again), getString(R.string.stream_server_error_title), getString(R.string.stream_server_error_message));
        } else {
            getStreamingServerStatus();
        }
    }

    // orientation detection listener implementation
    private void CheckOrientation() {
        isOrientationEnabled = true;
        myOrientationEventListener
                = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {

            @Override
            public void onOrientationChanged(int arg0) {
                //Log.d("publish_xmpp: ", "DeviceOrientation: " + String.valueOf(arg0));
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
            Log.d("publish_xmpp: ", "Can DetectOrientation");
            // orientation detection listener enable
            myOrientationEventListener.enable();
        } else {
            Log.d("publish_xmpp: ", "Can't DetectOrientation");
        }
    }

    // create blur effect over the video view
    public void initBlurEffect() {
        mBlurEffectPublish.setVisibility(View.VISIBLE);
        mCircleDiameter = (int) (mParentHeight / 3.75);
        radiusCircle = mCircleDiameter / 2;
        Log.d("circle_diameter_xmpp: ", String.valueOf(mCircleDiameter));
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mCircleDiameter, mCircleDiameter);
        mBlurEffectPublish.setX((mParentWidth / 2) - radiusCircle);
        mBlurEffectPublish.setY((mParentHeight / 2) - radiusCircle);
        mBlurEffectPublish.setLayoutParams(params);

        //send the diameter size to the chat room for web chat
        sendMessage("1", "blur");

        rangebar.setVisibility(View.VISIBLE);
        rangebar.setRangePinsByValue(0f, 1.0f);

        if (mRecyclerView.isShown()) {
            mChatLayout.setBackground(getResources().getDrawable(R.drawable.bottom_gradient));
        }
        Picasso.with(this)
                .load(R.drawable.mosaic_main)
                .transform(new BlurTransformation(100))   // blur density
                .into(mBlurEffectPublish);
    }

    // create chat room
    public void CreateChatRoom() {
        Intent intent = new Intent(getBaseContext(), ConnectXmpp.class);
        intent.putExtra("user", userName);
        intent.putExtra("code", "5");
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
        //Log.d("publish_xmpp: " + "uuid = ", uuid);
        // set user name as stream name
        PrefManager.setUserRecentStream(PublishActivity.this, userName);
        // Start stream (Core method)
        stream.publish(userName, R5Stream.RecordType.Live);

        // Start camera preview
        if (camera != null)
            camera.startPreview();

        // just send a init message with device size when fire web chat in web interface
        sendMessage(mParentWidth + "~" + mParentHeight, "init");

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
                        Log.d("publish_xmpp: ", "Connection Listener - Open");
                        break;
                    case 1://close
                        Log.d("publish_xmpp: ", "Connection Listener - Close");
                        break;
                    case 2://error
                        Log.d("publish_xmpp: ", "Connection Listener - Error: " + event.message);
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
                        Log.d("publish_xmpp: ", "Stream Listener - Connected");
                        break;
                    case DISCONNECTED:
                        Log.d("publish_xmpp: ", "Stream Listener - Disconnected");
                        // show dialog when recording going to be paused
                        if (isPublishing && !isUserDisconnected && !stopByButton) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    UpdateViewWhenStreamStopped();
                                    SessionErrorWarningDialog(getString(R.string.go_back), getString(R.string.stream_error_title), getString(R.string.stream_error_message_play_stream));
                                }
                            });
                        }
                        break;
                    case START_STREAMING:
                        Log.d("publish_xmpp: ", "Stream Listener - Started Streaming");
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
                        Log.d("publish_xmpp: ", "Stream Listener - Stopped Streaming");
                        break;
                    case CLOSE:
                        Log.d("publish_xmpp: ", "Stream Listener - Close");
                        break;
                    case TIMEOUT:
                        Log.d("publish_xmpp: ", "Stream Listener - Timeout");
                        break;
                    case ERROR:
                        Log.d("publish_xmpp: ", "Stream Listener - Error: " + event.message);
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


    // view re-organize for live stream screen to show chat and stop button
    public void UpdateViewWhenStreamStopped() {
        publishButton.setVisibility(View.VISIBLE);
        mChatLayout.setVisibility(View.GONE);
        mStopButton.setVisibility(View.GONE);
        actbar.setDisplayHomeAsUpEnabled(true);
        actbar.setDisplayShowHomeEnabled(true);

        // inflate chat list adapter
        mRecyclerView.setVisibility(View.GONE);
    }

    // Set camera method
    protected void setCamera() {
        if (camera == null) {
            try {
                camera = Camera.open(cameraSelection);
            } catch (Exception e) {
                Log.d("publish_xmpp: ", e.getMessage());
                Log.d("publish_xmpp: ", "Cannot connect to camera - moving on without it");
                // re-launch camera screen once again
                startActivity(new Intent(PublishActivity.this, PublishActivity.class));
                finish();
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
            String bits[] = selected_item.split("x");
            int pW = Integer.valueOf(bits[0]);
            int pH = Integer.valueOf(bits[1]);

            if (r5Cam == null) {
                r5Cam = new R5Camera(camera, pW, pH); // pW, pH
                r5Cam.setBitrate(1000);   // for high quality 4500, medium 1000
            } else
                r5Cam.setCamera(camera);
        } else {
            if (r5Cam == null) {
                r5Cam = new R5Camera(camera, 426, 240); //320, 240
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
            Log.d("publish_xmpp: ", e.getMessage());
        }
        setCamera();

        if (camera != null)
            camera.startPreview();

        if (isSwitchCamera) {
            isSwitchCamera = false;
            menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.ic_action_camera_switch));
        } else {
            isSwitchCamera = true;
            menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.ic_action_camera_switch_on));
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
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
            Log.d("publish_xmpp: ", "Camera release!");
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
            Log.d("publish_xmpp: ", e.getMessage());
        }
        Log.d("publish_xmpp: ", "onDestroy");
        // finally finish activity
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
                    // send camera status when blur is enable
                    sendMessage("backCamera", "blur");
                } else {
                    isFrontCamera = true;
                    // send camera status when blur is enable
                    sendMessage("frontCamera", "blur");
                }
            }
            toggleCamera();
            return true;
        } else if (id == R.id.camera_filter) {
            String mSubject = "blur";
            if (isPublishing) {
                if (!mBlurStatus) {
                    mSystemChat = "on";
                    menu.getItem(1).setIcon(getResources().getDrawable(R.drawable.ic_action_filter_switch_on));
                    //webchat
                    sendMessage(mSystemChat, mSubject);
                    mBlurStatus = true;
                    initBlurEffect();
                } else {
                    mSystemChat = "off";
                    menu.getItem(1).setIcon(getResources().getDrawable(R.drawable.ic_action_filter_switch));
                    //webchat
                    sendMessage(mSystemChat, mSubject);
                    mBlurStatus = false;
                    mBlurEffectPublish.setVisibility(View.GONE);
                    rangebar.setVisibility(View.GONE);
                    if (mRecyclerView.isShown()) {
                        mChatLayout.setBackground(getResources().getDrawable(R.drawable.bottom_gradient));
                    }
                }
            } else {
                toast(getString(R.string.can_not_set_blur));
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
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
                            Log.d("publish_xmpp: ", "Streaming Server Status: " + ResponseStatus + " and Response Code: " + ResponseCode);
                            if (ResponseCode == 200) {
                                // check user status
                                checkRoomStatus();
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
                VolleyLog.d("publish_xmpp: ", "Error: " + error.getMessage());
                // write error code
                GoToCheckStreamServer();
            }
        });

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(jsonObjReq,
                tag_json_obj);
    }

}
