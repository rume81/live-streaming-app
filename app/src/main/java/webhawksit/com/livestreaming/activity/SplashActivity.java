package webhawksit.com.livestreaming.activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.readystatesoftware.systembartint.SystemBarTintManager;

import cn.pedant.SweetAlert.SweetAlertDialog;
import webhawksit.com.livestreaming.R;
import webhawksit.com.livestreaming.chat.services.ConnectXmpp;
import webhawksit.com.livestreaming.utils.MyExceptionHandler;
import webhawksit.com.livestreaming.utils.NetworkChecking;
import webhawksit.com.livestreaming.utils.PrefManager;

import static webhawksit.com.livestreaming.utils.NetworkChecking.getConnectivityStatusString;

public class SplashActivity extends AppCompatActivity {

    private String TAG = SplashActivity.class.getSimpleName();
    private static int SPLASH_TIME_OUT = 2000;
    private BroadcastReceiver mBroadcastReceiver;
    private ProgressDialog mProgressDialog;
    public String userName;
    public String password;
    boolean isWiFiConnected;
    Window win;
    SweetAlertDialog mNetworkErrorDialog, mLoginErrorDialog, mConnectionErrorDialog, mLoginExceptionDialog, mAuthenticationErrorDialog;
    boolean isNextPhaseCalled = false;
    boolean isNetworkErrorCalled = false;
    boolean isNetworkErrorDialog = false;

    boolean isLoginErrorDialog = false;
    boolean isConnectionErrorDialog = false;
    boolean isLoginExceptionDialog = false;
    boolean isAuthenticationErrorDialog = false;

    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        // added the following methods to awake the device from lock state and keep screen on so the broadcast messages could be received
        win = this.getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        win.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        // end

        setContentView(R.layout.splash);

        // create our manager instance after the content view is set
        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        // enable status bar tint
        tintManager.setStatusBarTintEnabled(true);
        // enable navigation bar tint
        tintManager.setNavigationBarTintEnabled(true);
        // set a custom tint color for all system bars
        tintManager.setTintColor(Color.parseColor("#08427a"));

        // get App current version code and name start
        PackageInfo pinfo = null;
        try {
            pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        int versionNumber = pinfo.versionCode;
        Log.d("splash_xmpp: ", "App version code: " + versionNumber);
        String versionName = pinfo.versionName;
        Log.d("splash_xmpp: ", "App version name: " + versionName);
        // get current version code and name end

        //initialize login progress dialog object
        initProgressDialog();

        // handler to call login for certain time
        handler = new Handler();


        // get user login info from shared preference
        getUserInfo();

        // get broadcast messages for different action
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // checking for type intent filter
                // if successfully login
                if (intent.getAction().equals("signin")) {
                    Log.d("splash_xmpp: ", "successfully Logged in from splash");
                    // write server connection status in shared preference
                    PrefManager.setServerStatus(SplashActivity.this, "Yes");

                    // hide login progress dialog
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                    }
                    // launch home screen
                    launchHomeScreen();
                }
                // if connection error from chat server
                else if (intent.getAction().equals("connectionerror")) {
                    Log.d("splash_xmpp: ", "connection error from splash");
                    String connectionStatus = intent.getStringExtra("action");
                    if (connectionStatus.contains("after 30000ms:")) {
                        // login call after 4 seconds
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //Do after 4 seconds
                                XmppLogin();
                            }
                        }, 4000);
                    } else {
                        // hide login progress dialog
                        if (mProgressDialog != null) {
                            mProgressDialog.dismiss();
                        }
                        // show login error warning dialog
                        ConnectionErrorWarning();
                    }
                }
                // if login error from chat server
                else if (intent.getAction().equals("signinerror")) {
                    String loginStatus = intent.getStringExtra("action");
                    if (loginStatus.contains("Client is not, or no longer, connected")) {
                        Log.d("splash_xmpp: ", loginStatus);
                        // call login again
                        //XmppLogin();
                        // hide login progress dialog
                        if (mProgressDialog != null) {
                            mProgressDialog.dismiss();
                        }
                        // show login error warning dialog
                        ConnectionErrorWarning();
                    }
                    else if (loginStatus.contains("SASLError using SCRAM-SHA-1: not-authorized")) {
                        Log.d("splash_xmpp: ", loginStatus);
                        // hide login progress dialog
                        if (mProgressDialog != null) {
                            mProgressDialog.dismiss();
                        }
                        // show login error warning dialog
                        AuthenticationErrorWarning();
                    }
                    else {
                        Log.d("splash_xmpp: ", loginStatus);
                        // hide login progress dialog
                        if (mProgressDialog != null) {
                            mProgressDialog.dismiss();
                        }
                        // show login error warning dialog
                        LoginErrorWarning();
                    }
                }
            }
        };

        // crash handler initialization
        //Thread.setDefaultUncaughtExceptionHandler(new MyExceptionHandler(this));
    }

    //initialize login progress dialog object
    public void initProgressDialog() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getString(R.string.getting_start));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
    }

    // get user login information from shared preference
    public void getUserInfo() {
        userName = PrefManager.getUserName(SplashActivity.this);
        password = PrefManager.getUserPassword(SplashActivity.this);
    }

    // XMPP chat server login method
    public void XmppLogin() {
        // call login method by service class intent
        try {
            Intent intent = new Intent(SplashActivity.this, ConnectXmpp.class);
            intent.putExtra("user", userName);
            intent.putExtra("pwd", password);
            intent.putExtra("code", "0");
            startService(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("splash_xmpp: ", "Login exception: " + e.getMessage());
            // hide login progress dialog
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
            // show login exception warning dialog
            LoginExceptionWarning();
        }
    }

    // create login warning dialog for login error
    public void LoginErrorWarning() {
        mLoginErrorDialog = new SweetAlertDialog(SplashActivity.this, SweetAlertDialog.WARNING_TYPE);
        mLoginErrorDialog
                .setTitleText(getString(R.string.login_error_dialog_title))
                .setContentText(getString(R.string.login_error_dialog_message))
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
                        if (isWiFiConnected) {
                            sDialog.cancel();
                            // login progress dialog show
                            if (!SplashActivity.this.isFinishing()) {
                                if (mProgressDialog != null) {
                                    mProgressDialog.show();
                                }
                            }
                            // again call login method
                            XmppLogin();
                        } else {
                            sDialog.cancel();
                            NetworkErrorWarning();
                        }
                    }
                })
                .show();
        isLoginErrorDialog = true;
        mLoginErrorDialog.setCancelable(false);
        mLoginErrorDialog.setCanceledOnTouchOutside(false);
    }


    // create connection warning dialog for login error
    public void ConnectionErrorWarning() {
        mConnectionErrorDialog = new SweetAlertDialog(SplashActivity.this, SweetAlertDialog.WARNING_TYPE);
        mConnectionErrorDialog
                .setTitleText(getString(R.string.connection_error_dialog_title))
                .setContentText(getString(R.string.login_error_dialog_message))
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
                        if (isWiFiConnected) {
                            sDialog.cancel();
                            // login progress dialog show
                            if (!SplashActivity.this.isFinishing()) {
                                if (mProgressDialog != null) {
                                    mProgressDialog.show();
                                }
                            }
                            // again call login method
                            XmppLogin();
                        } else {
                            sDialog.cancel();
                            NetworkErrorWarning();
                        }
                    }
                })
                .show();
        isConnectionErrorDialog = true;
        mConnectionErrorDialog.setCancelable(false);
        mConnectionErrorDialog.setCanceledOnTouchOutside(false);
    }

    // create login exception warning dialog for login error
    public void LoginExceptionWarning() {
        mLoginExceptionDialog = new SweetAlertDialog(SplashActivity.this, SweetAlertDialog.WARNING_TYPE);
        mLoginExceptionDialog
                .setTitleText(getString(R.string.login_error_dialog_title))
                .setContentText(getString(R.string.login_error_dialog_message))
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
                        if (isWiFiConnected) {
                            sDialog.cancel();
                            // login progress dialog show
                            if (!SplashActivity.this.isFinishing()) {
                                if (mProgressDialog != null) {
                                    mProgressDialog.show();
                                }
                            }
                            // again call login method
                            XmppLogin();
                        } else {
                            sDialog.cancel();
                            NetworkErrorWarning();
                        }
                    }
                })
                .show();
        isLoginExceptionDialog= true;
        mLoginExceptionDialog.setCancelable(false);
        mLoginExceptionDialog.setCanceledOnTouchOutside(false);
    }

    // create network error warning dialog
    public void NetworkErrorWarning() {
        mNetworkErrorDialog = new SweetAlertDialog(SplashActivity.this, SweetAlertDialog.WARNING_TYPE);
        mNetworkErrorDialog
                .setTitleText(getString(R.string.network_error_title))
                .setContentText(getString(R.string.no_internet_connection))
                .setConfirmText(getString(R.string.no_internet_toolber))
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
        mAuthenticationErrorDialog = new SweetAlertDialog(SplashActivity.this, SweetAlertDialog.WARNING_TYPE);
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

    // common toast method
    private void toast(String text) {
        Toast.makeText(SplashActivity.this, text, Toast.LENGTH_SHORT).show();
    }

    // Go to next step after launching splash
    public void GoToNext() {
        Log.d("xmpp: ", "called next phase");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // This method will be executed once the timer is over
                // Start your app main activity
                // close this activity
                // Check if user's login data stored previously to make login
                if (PrefManager.getUserLoggedData(SplashActivity.this).equals("Yes")) {
                    // login progress dialog show
                    if (!SplashActivity.this.isFinishing()) {
                        if (mProgressDialog != null) {
                            mProgressDialog.show();
                        }
                    }
                    // call login method
                    XmppLogin();
                }
                // if user login data is not stored previously to make login
                else {
                    // launch login screen
                    launchLoginScreen();
                }
            }
        }, SPLASH_TIME_OUT);
    }

    // Launch Home Screen
    private void launchHomeScreen() {
        Intent i = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(i);
        finish();
        // animation to go to next page
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
    }

    // Launch Login Screen
    private void launchLoginScreen() {
        Intent i = new Intent(SplashActivity.this, Login.class);
        startActivity(i);
        finish();
        // animation to go to next page
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        // if any progress or dialog showing then hide them
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        // Initializing Internet Check
        if (NetworkChecking.hasConnection(SplashActivity.this)) {
            isWiFiConnected = true;
            isNextPhaseCalled = true;
            // go to next phase to make login
            GoToNext();
        } else {
            // if there is no internet
            isWiFiConnected = false;
            isNetworkErrorCalled = true;
            // show error dialog
            NetworkErrorWarning();
        }

        // register run time internet checking broadcast receiver
        registerInternetCheckReceiver();

        // register receiver for connection
        LocalBroadcastManager.getInstance(SplashActivity.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("signin"));

        // register receiver for connection error
        LocalBroadcastManager.getInstance(SplashActivity.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("connectionerror"));

        // register receiver for login error
        LocalBroadcastManager.getInstance(SplashActivity.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("signinerror"));
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        // unregister all receiver
        unregisterReceiver(broadcastReceiver);
        LocalBroadcastManager.getInstance(SplashActivity.this).unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "stop");
        super.onStop();
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
                Log.d(TAG, "connectivity: " + internetStatus);
                isWiFiConnected = false;
                // show error dialog
                if (!isNetworkErrorCalled) {
                    NetworkErrorWarning();
                    isNetworkErrorCalled = true;
                }
            }
            // and when device connected with internet
            else {
                Log.d(TAG, "connectivity: " + internetStatus);
                isWiFiConnected = true;
                // hide login error dialog and go to login phase
                if (!isNextPhaseCalled) {
                    // hide error dialog
                    if (isNetworkErrorDialog) {
                        mNetworkErrorDialog.dismiss();
                    }
                    if (isAuthenticationErrorDialog) {
                        mAuthenticationErrorDialog.dismiss();
                    }
                    if (isConnectionErrorDialog) {
                        mConnectionErrorDialog.dismiss();
                    }
                    if (isLoginErrorDialog) {
                        mLoginErrorDialog.dismiss();
                    }
                    if (isLoginExceptionDialog) {
                        mLoginExceptionDialog.dismiss();
                    }
                    // go to login phase
                    GoToNext();
                    isNextPhaseCalled = true;
                }
            }
        }
    };

}

