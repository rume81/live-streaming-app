package webhawksit.com.livestreaming.activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.readystatesoftware.systembartint.SystemBarTintManager;

import cn.pedant.SweetAlert.SweetAlertDialog;
import webhawksit.com.livestreaming.R;
import webhawksit.com.livestreaming.chat.services.ConnectXmpp;
import webhawksit.com.livestreaming.fragment.HomeFragment;
import webhawksit.com.livestreaming.fragment.SettingsFragment;
import webhawksit.com.livestreaming.utils.MyExceptionHandler;
import webhawksit.com.livestreaming.utils.NetworkChecking;
import webhawksit.com.livestreaming.utils.PrefManager;

import static webhawksit.com.livestreaming.utils.NetworkChecking.getConnectivityStatusString;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    //Defining Variables
    Toolbar toolbar;
    private DrawerLayout drawerLayout;
    final Context mContext = this;
    TextView mNavHeaderTitle, mNavHeaderProfileName;
    ImageView mUserProfileImage;
    private BroadcastReceiver mBroadcastReceiver;
    private ProgressDialog mProgressDialog;
    String userName, password;
    boolean isWiFiConnected = false;
    boolean isCheckedLogin = false;
    boolean isCheckedNetwork = false;
    SweetAlertDialog mCrashErrorDialog, mSessionErrorDialog, mInternetErrorDialog, mLoginExceptionDialog, mLoginErrorDialog, mConnectionErrorDialog, mAuthenticationErrorDialog;
    boolean isLoginErrorDialog = false;
    boolean isConnectionErrorDialog = false;
    boolean isLoginExceptionDialog = false;
    boolean isAuthenticationErrorDialog = false;
    boolean isCrashErrorDialog = false;
    boolean isSessionErrorDialog = false;
    boolean isInternetErrorDialog = false;
    Handler mHandler;
    boolean loginCalledFromMainPage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // create our manager instance after the content view is set
        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        // enable status bar tint
        tintManager.setStatusBarTintEnabled(true);
        // enable navigation bar tint
        tintManager.setNavigationBarTintEnabled(true);
        // set a custom tint color for all system bars
        tintManager.setTintColor(Color.parseColor("#1a746b"));

        // Initializing Toolbar and setting it as the actionbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Live Streaming");

        // initialize navigation drawer
        initNavigationDrawer();

        // get user information from shared preference
        getUserInfo();

        // initialization progress dialog
        initProgressDialog();

        // handler to call login for certain time
        mHandler = new Handler();

        // start launching home fragment
        android.support.v4.app.FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        HomeFragment fragment = new HomeFragment();
        fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.frame, fragment);
        fragmentTransaction.commit();

        // get broadcast messages for different action
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // checking for type intent filter
                // if log out from chat server
                if (intent.getAction().equals("connectionclosed")) {
                    Log.d("main_xmpp: ", "successfully Logged Out from main page");
                    // update user login information in shared preference
                    PrefManager.setUserLoggedData(MainActivity.this, "No");
                    PrefManager.setServerStatus(MainActivity.this, "No");
                    // go to login page
                    goLoginPage();
                }
                // if successfully login
                else if (intent.getAction().equals("signin")) {
                    Log.d("main_xmpp: ", "successfully Logged in from main page");
                    loginCalledFromMainPage = false;
                    // hide progress
                    hideProgress();
                    // update user login information in shared preference
                    PrefManager.setServerStatus(MainActivity.this, "Yes");
                }
                // if chat server connection closed on error
                else if (intent.getAction().equals("connectionclosederror")) {
                    String sessionStatus = intent.getStringExtra("action");
                    Log.d("main_xmpp: ", sessionStatus + " from main page");
                    if (!loginCalledFromMainPage) {
                        PrefManager.setServerStatus(MainActivity.this, "No");
                        // show error dialog that session closed on error
                        SessionErrorWarning();
                    }
                }
                // if login error
                else if (intent.getAction().equals("signinerror")) {
                    String loginStatus = intent.getStringExtra("action");
                    if (loginStatus.contains("Client is not, or no longer, connected")) {
                        Log.d("main_xmpp: ", loginStatus);
                        // call login again
                        XmppLogin();
                    } else if (loginStatus.contains("SASLError using SCRAM-SHA-1: not-authorized")) {
                        Log.d("main_xmpp: ", loginStatus);
                        // hide progress
                        hideProgress();
                        // show login error warning dialog
                        AuthenticationErrorWarning();
                    } else {
                        Log.d("main_xmpp: ", loginStatus);
                        // hide progress
                        hideProgress();
                        // show error dialog
                        LoginErrorWarning();
                    }
                }
                // get connection error response
                else if (intent.getAction().equals("connectionerror")) {
                    Log.d("main_xmpp: ", "Main connection error!");
                    String connectionStatus = intent.getStringExtra("action");
                    if (connectionStatus.contains("after 30000ms:")) {
                        // login call after 4 seconds
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //Do after 4 seconds
                                XmppLogin();
                            }
                        }, 4000);
                    } else {
                        // hide progress dialog
                        hideProgress();
                        // show error dialog
                        ConnectionErrorWarning();
                    }
                }
            }
        };

        // crash handler initialization
        //Thread.setDefaultUncaughtExceptionHandler(new MyExceptionHandler(this));

        // if the activity comes after crash
        //if (getIntent().getBooleanExtra("crash", false)) {
        //    Log.d("main_xmpp: ", "crashed");
            // show a dialog regarding crashg issue
        //    CrashErrorWarning();
       // }
    }
    // xmpp login method
    public void XmppLogin() {
        loginCalledFromMainPage = true;
        // go to service to make login
        try {
            Intent intent = new Intent(MainActivity.this, ConnectXmpp.class);
            intent.putExtra("user", userName);
            intent.putExtra("pwd", password);
            intent.putExtra("code", "0");
            startService(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("main_xmpp: ", "Login exception: " + e.getMessage());
            // hide progress
            hideProgress();
            // show error dialog
            LoginExceptionWarning();
        }
    }

    // common toast method
    private void toast(String text) {
        Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
    }

    // create dialog pop up for session error
    public void CrashErrorWarning() {
        mCrashErrorDialog = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.SUCCESS_TYPE);
        mCrashErrorDialog
                .setTitleText(getString(R.string.crash_title))
                .setContentText(getString(R.string.crash_message))
                .setConfirmText(getString(R.string.ok))
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.cancel();
                    }
                })
                .show();
        isCrashErrorDialog = true;
        mCrashErrorDialog.setCancelable(false);
        mCrashErrorDialog.setCanceledOnTouchOutside(false);
    }

    // create dialog pop up for session error
    public void SessionErrorWarning() {
        mSessionErrorDialog = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.WARNING_TYPE);
        mSessionErrorDialog
                .setTitleText(getString(R.string.session_error_dialog_title))
                .setContentText(getString(R.string.session_error_dialog_message))
                .setConfirmText(getString(R.string.create_again))
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        if (isWiFiConnected) {
                            sDialog.cancel();
                            showProgress();
                            XmppLogin();
                        } else {
                            // network error warning
                            sDialog.cancel();
                            NetworkErrorWarning();
                        }
                    }
                })
                .show();
        isSessionErrorDialog = true;
        mSessionErrorDialog.setCancelable(false);
        mSessionErrorDialog.setCanceledOnTouchOutside(false);
    }

    // create login warning dialog for Authentication error
    public void AuthenticationErrorWarning() {
        mAuthenticationErrorDialog = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.WARNING_TYPE);
        mAuthenticationErrorDialog
                .setTitleText(getString(R.string.login_error_dialog_title))
                .setContentText(getString(R.string.login_failed_message))
                .setConfirmText(getString(R.string.try_again))
                .showCancelButton(false)
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.cancel();
                        // launch login screen
                        goLoginPage();
                    }
                })
                .show();
        isAuthenticationErrorDialog = true;
        mAuthenticationErrorDialog.setCancelable(false);
        mAuthenticationErrorDialog.setCanceledOnTouchOutside(false);
    }

    // create dialog pop up for login error
    public void LoginErrorWarning() {
        mLoginErrorDialog = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.WARNING_TYPE);
        mLoginErrorDialog
                .setTitleText(getString(R.string.login_error_dialog_title))
                .setContentText(getString(R.string.login_error_dialog_message))
                .setConfirmText(getString(R.string.try_again))
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        if (isWiFiConnected) {
                            sDialog.cancel();
                            showProgress();
                            XmppLogin();
                        } else {
                            // network error warning
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

    // create dialog pop up for login exception
    public void LoginExceptionWarning() {
        mLoginExceptionDialog = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.WARNING_TYPE);
        mLoginExceptionDialog
                .setTitleText(getString(R.string.login_error_dialog_title))
                .setContentText(getString(R.string.login_error_dialog_message))
                .setConfirmText(getString(R.string.try_again))
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        if (isWiFiConnected) {
                            sDialog.cancel();
                            showProgress();
                            XmppLogin();
                        } else {
                            // network error warning
                            sDialog.cancel();
                            NetworkErrorWarning();
                        }
                    }
                })
                .show();
        isLoginExceptionDialog = true;
        mLoginExceptionDialog.setCancelable(false);
        mLoginExceptionDialog.setCanceledOnTouchOutside(false);
    }

    // create dialog pop up for session error
    public void ConnectionErrorWarning() {
        mConnectionErrorDialog = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.WARNING_TYPE);
        mConnectionErrorDialog
                .setTitleText(getString(R.string.connection_error_dialog_title))
                .setContentText(getString(R.string.login_error_dialog_message))
                .setConfirmText(getString(R.string.try_again))
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        if (isWiFiConnected) {
                            sDialog.cancel();
                            showProgress();
                            XmppLogin();
                        } else {
                            // network error warning
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

    // create dialog pop up for network error
    public void NetworkErrorWarning() {
        mInternetErrorDialog = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.WARNING_TYPE);
        mInternetErrorDialog
                .setTitleText(getString(R.string.network_error_title))
                .setContentText(getString(R.string.no_internet_connection))
                .setConfirmText(getString(R.string.no_internet_toolber))
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
        isInternetErrorDialog = true;
        mInternetErrorDialog.setCancelable(false);
        mInternetErrorDialog.setCanceledOnTouchOutside(false);
    }

    // get user login info from shared preference
    public void getUserInfo() {
        userName = PrefManager.getUserName(MainActivity.this);
        password = PrefManager.getUserPassword(MainActivity.this);
    }

    //initialize progress dialog object
    public void initProgressDialog() {
        mProgressDialog = new ProgressDialog(MainActivity.this);
        mProgressDialog.setMessage(getString(R.string.session_creating));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
    }

    // log out dialog
    public void showLogoutDialog() {
        SweetAlertDialog mLogoutDialog = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.WARNING_TYPE);
        mLogoutDialog
                .setTitleText(getString(R.string.are_you_sure))
                .setContentText(getString(R.string.log_out_message))
                .setConfirmText(getString(R.string.yes))
                .setCancelText(getString(R.string.no))
                .showCancelButton(true)
                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.cancel();
                        drawerLayout.closeDrawers();
                    }
                })
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.cancel();
                        drawerLayout.closeDrawers();
                        Logout();
                    }
                })
                .show();
        mLogoutDialog.setCancelable(false);
        mLogoutDialog.setCanceledOnTouchOutside(false);
    }

    // initialization navigation drawer
    public void initNavigationDrawer() {

        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {

                int id = menuItem.getItemId();

                switch (id) {
                    //Replacing the main content with Profile
                    /*case R.id.profile:
                        startActivity(new Intent(MainActivity.this, Profile.class));
                        finish();
                        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                        drawerLayout.closeDrawers();
                        break;*/

                    //Replacing the main content with home
                    case R.id.home:
                        android.support.v4.app.FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                        HomeFragment fragment5 = new HomeFragment();
                        fragmentTransaction = getSupportFragmentManager().beginTransaction();
                        fragmentTransaction.replace(R.id.frame, fragment5);
                        fragmentTransaction.commit();
                        drawerLayout.closeDrawers();
                        break;

                    //Replacing the main content
                    case R.id.settings:
                        SettingsFragment fragment6 = new SettingsFragment();
                        fragmentTransaction = getSupportFragmentManager().beginTransaction();
                        fragmentTransaction.replace(R.id.frame, fragment6);
                        fragmentTransaction.commit();
                        drawerLayout.closeDrawers();
                        break;

                    // call log out method
                    case R.id.logout:
                        showLogoutDialog();
                        break;

                    //Rest of the case just show toast
                    default:
                        Toast.makeText(getApplicationContext(), getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
                        return true;

                }
                return true;
            }
        });

        View header = navigationView.getHeaderView(0);

        // set user name and default profile picture in navigation header
        mNavHeaderTitle = (TextView) header.findViewById(R.id.user_name);
        mNavHeaderProfileName = (TextView) header.findViewById(R.id.nav_profile_image_username);
        if (!PrefManager.getUserName(MainActivity.this).isEmpty()) {
            mNavHeaderTitle.setText(PrefManager.getUserName(MainActivity.this));
            mNavHeaderProfileName.setText(String.valueOf(PrefManager.getUserName(MainActivity.this).toString().charAt(0)));
        } else {
            mNavHeaderTitle.setText(getString(R.string.create_display_name));
            mNavHeaderTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, Profile.class));
                    finish();
                    overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                    drawerLayout.closeDrawers();
                }
            });
        }
/*        mUserProfileImage = (ImageView)header.findViewById(R.id.nav_profile_image);
        String mStoredImageUrl = PrefManager.getUserPhoto(MainActivity.this);
        if( !mStoredImageUrl.equalsIgnoreCase("") ){
            byte[] b = Base64.decode(mStoredImageUrl, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(b, 0, b.length);
            mUserProfileImage.setImageBitmap(bitmap);
        }
        else {
            mUserProfileImage.setBackgroundResource(R.drawable.photo_empty);
        }*/
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer);

        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerClosed(View v) {
                super.onDrawerClosed(v);
            }

            @Override
            public void onDrawerOpened(View v) {
                super.onDrawerOpened(v);
            }
        };
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    // on back press from device hard key
    @Override
    public void onBackPressed() {
        // show exit dialog
        showExitDialogToUser(mContext);
    }

    // Create Dialog popup for Exit action
    public void showExitDialogToUser(final Context context) {
        SweetAlertDialog mPermissionDialog = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.WARNING_TYPE);
        mPermissionDialog
                .setTitleText(getString(R.string.hey))
                .setContentText(getString(R.string.exit_message))
                .setConfirmText(getString(R.string.yes))
                .setCancelText(getString(R.string.no))
                .showCancelButton(true)
                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                            drawerLayout.closeDrawers(); //CLOSE Nav Drawer!
                            sDialog.cancel();
                        } else {
                            sDialog.cancel();
                        }
                    }
                })
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                            drawerLayout.closeDrawers(); //CLOSE Nav Drawer!
                            sDialog.cancel();
                            finish();
                        } else {
                            sDialog.cancel();
                            finish();
                        }
                    }
                })
                .show();
        mPermissionDialog.setCancelable(false);
        mPermissionDialog.setCanceledOnTouchOutside(false);
    }

    // logout method
    private void Logout() {
        Intent intent = new Intent(getBaseContext(), ConnectXmpp.class);
        intent.putExtra("code", "9");
        startService(intent);
    }

    // after disconnected go login page
    public void goLoginPage() {
        startActivity(new Intent(MainActivity.this, Login.class));
        finish();
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        // Initializing Internet Check
        if (NetworkChecking.hasConnection(MainActivity.this)) {
            isWiFiConnected = true;
            if (!isCheckedLogin) {
                isCheckedLogin = true;
                isCheckedNetwork = false;
                Log.d("main_xmpp: ", "isCheckedLogin");
                // check internet was disconnected recently and then login
                if (PrefManager.getServerStatus(MainActivity.this).equals("No")) {
                    // show progress
                    showProgress();
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
                Log.d("main_xmpp: ", "isCheckedNetwork");
                // update user login information in shared preference
                PrefManager.setServerStatus(MainActivity.this, "No");
                // show error dialog
                NetworkErrorWarning();
            }
        }

        // register receiver for connection
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("connectionclosed"));

        // register receiver for sign in
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("signin"));

        // register receiver for connection closed on error
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("connectionclosederror"));

        // register receiver for connection error
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("connectionerror"));

        // register receiver for login error
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("signinerror"));

        // register run time internet checking broadcast receiver
        registerInternetCheckReceiver();

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
                Log.d("main_xmpp: ", "connectivity: " + internetStatus);
                isWiFiConnected = false;
                if (!isCheckedNetwork) {
                    isCheckedNetwork = true;
                    isCheckedLogin = false;
                    Log.d("main_xmpp: ", "isCheckedNetwork");
                    // update user login information in shared preference
                    PrefManager.setServerStatus(MainActivity.this, "No");
                }
            }
            // and when device connected with internet
            else {
                Log.d("main_xmpp: ", "connectivity: " + internetStatus);
                isWiFiConnected = true;
                if (!isCheckedLogin) {
                    isCheckedLogin = true;
                    isCheckedNetwork = false;
                    Log.d("main_xmpp: ", "isCheckedLogin");
                    // check internet was disconnected recently and then login
                    if (PrefManager.getServerStatus(MainActivity.this).equals("No")) {
                        // show progress
                        showProgress();
                        // hide error dialog
                        if (isSessionErrorDialog) {
                            mSessionErrorDialog.dismiss();
                        }
                        if (isInternetErrorDialog) {
                            mInternetErrorDialog.dismiss();
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
                        if (isCrashErrorDialog) {
                            mCrashErrorDialog.dismiss();
                        }
                        // go to login
                        XmppLogin();
                    }
                }
            }
        }
    };

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        // unregister all receiver
        unregisterReceiver(broadcastReceiver);
        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    // show progress dialog
    public void showProgress() {
        if (!MainActivity.this.isFinishing()) {
            // if previously not showing it then show that
            if (!mProgressDialog.isShowing()) {
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
}