package webhawksit.com.livestreaming.activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.readystatesoftware.systembartint.SystemBarTintManager;

import cn.pedant.SweetAlert.SweetAlertDialog;
import webhawksit.com.livestreaming.R;
import webhawksit.com.livestreaming.chat.services.ConnectXmpp;
import webhawksit.com.livestreaming.utils.MyExceptionHandler;
import webhawksit.com.livestreaming.utils.NetworkChecking;
import webhawksit.com.livestreaming.utils.PrefManager;

import static webhawksit.com.livestreaming.utils.NetworkChecking.getConnectivityStatusString;

public class Login extends AppCompatActivity {

    private static final String TAG = Login.class.getSimpleName();
    TextInputLayout inputLayoutUsername, inputLayoutPassword;
    EditText _usernameText, _passwordText;
    TextView mForgotPassword;
    Button _loginButton;
    String userName;
    String password;
    Handler handler;
    private BroadcastReceiver mBroadcastReceiver;
    private ProgressDialog mProgressDialog;
    RelativeLayout mLoginMainContent;
    Snackbar snackbar;
    boolean isWiFiConnected = false;
    boolean runtimeInternetReceiver = false;
    SweetAlertDialog savedDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.login);

        // create our manager instance after the content view is set
        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        // enable status bar tint
        tintManager.setStatusBarTintEnabled(true);
        // enable navigation bar tint
        tintManager.setNavigationBarTintEnabled(true);
        // set a custom tint color for all system bars
        tintManager.setTintColor(Color.parseColor("#08427a"));

        // Set up the toolbar.
        Toolbar login_toolbar = (Toolbar) findViewById(R.id.login_toolbar);
        setSupportActionBar(login_toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mLoginMainContent = (RelativeLayout) findViewById(R.id.login_main_content);

        // initialization for success dialog
        savedDialog = new SweetAlertDialog(Login.this, SweetAlertDialog.SUCCESS_TYPE);

        // initialize handler to auto hide success dialog
        handler = new Handler();

        // initialize snack bar
        initSnackBar();

        // initialize progress dialog
        initProgressbar();

        inputLayoutUsername = (TextInputLayout) findViewById(R.id.inputLayoutUsername);
        inputLayoutPassword = (TextInputLayout) findViewById(R.id.inputLayoutPassword);

        _usernameText = (EditText) findViewById(R.id.input_username);
        _passwordText = (EditText) findViewById(R.id.input_password);

        mForgotPassword = (TextView) findViewById(R.id.forgot_password);
        mForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // commented this code as we are not sure about this features from client
                //goToRestPassword();
            }
        });

        _loginButton = (Button) findViewById(R.id.btn_login);
        _loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // show progress
                showProgress();
                // Initializing Internet Check
                if (isWiFiConnected) {
                    // go to login method
                    login();
                } else {
                    // hide progress
                    hideProgress();
                    // if there is no internet
                    // Show Snack bar message
                    showSnackBar();
                }
            }
        });

        // get broadcast messages for different action
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // checking for type intent filter
                // if successfully login
                if (intent.getAction().equals("signin")) {
                    Log.d("login_xmpp: ", "successfully Logged in from login screen");
                    // hide progress
                    hideProgress();

                    // save all user data for login action
                    PrefManager.setServerStatus(Login.this, "Yes");
                    PrefManager.setUserLoggedData(Login.this, "Yes");
                    PrefManager.setUserName(Login.this, userName);
                    PrefManager.setUserPassword(Login.this, password);

                    // show login success dialog
                    LoginSuccess();

                    // auto hide the dialog by handler
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 2500 ms
                            //savedDialog.dismissWithAnimation();
                            // if dialog showing then hide it
                            hideSuccessDialog();
                            // start new activity for launching home screen
                            startNewActivity();
                        }
                    }, 2500);
                }
                // if connection error from chat server thenshow error message
                else if (intent.getAction().equals("connectionerror")) {
                    Log.d("login_xmpp: ", "connection error from login screen");
                    // show registration error
                    showRegistrationError(getString(R.string.connection_error_dialog_title));
                }
                // if registration error with chat server
                else if (intent.getAction().equals("signuperror")) {
                    String errorMessage = intent.getStringExtra("action");
                    // if conflict with old user
                    if (errorMessage.equals("XMPPError: conflict - cancel")) {
                        Log.d("login_xmpp: ", "registration conflict error from login screen");
                        // call login method
                        XmppLogin();
                    }
                    // if not then show registration error message
                    else {
                        Log.d("login_xmpp: ", "registration error from login screen");
                        // show registration error
                        showRegistrationError(getString(R.string.xmpp_signup_error));
                    }
                }
                // if login error with chat server then show message
                else if (intent.getAction().equals("signinerror")) {
                    String loginStatus = intent.getStringExtra("action");
                    if (!loginStatus.contains("Client is not, or no longer, connected")) {
                        Log.d("login_xmpp: ", "login error from login screen");
                        // show registration error
                        showRegistrationError(getString(R.string.xmpp_signin_error));
                    }
                }
            }
        };

        // crash handler initialization
        //Thread.setDefaultUncaughtExceptionHandler(new MyExceptionHandler(this));
    }

    // registration error method
    public void showRegistrationError(String message) {
        // hide progress
        hideProgress();
        // change button action details
        _loginButton.setEnabled(true);
        _loginButton.setText(getString(R.string.try_again));
        // make toast with error message
        toast(message);
    }

    // create login warning dialog for login error
    public void LoginSuccess() {
        savedDialog
                .setTitleText(getString(R.string.success_dialog_title))
                .setContentText(getString(R.string.sign_up_dialog_message))
                .setConfirmText(getString(R.string.thanks))
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        // no action as it is auto hide
                    }
                })
                .show();
        savedDialog.setCancelable(false);
        savedDialog.setCanceledOnTouchOutside(false);
    }

    // initialize progress dialog
    private void initProgressbar() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getString(R.string.getting_ready));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
    }

    // to go to password reset screen
    private void goToRestPassword() {
        Intent intent = new Intent(Login.this, ForgotPassword.class);
        intent.putExtra("comes_from", "login");
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
    }

    // stat login method
    public void login() {
        if (!validate()) {
            showRegistrationError(getString(R.string.login_failed_message));
            return;
        }
        // call xmpp registration method
        XmppRegistration();
    }

    // go to home page
    private void startNewActivity() {
        startActivity(new Intent(Login.this, MainActivity.class));
        finish();
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
    }

    // check login data validity
    public boolean validate() {
        boolean valid = true;

        String username = _usernameText.getText().toString();
        String password = _passwordText.getText().toString();

        if (username.isEmpty() || username.length() < 3 || username.length() > 64) {
            _usernameText.setError(getString(R.string.valid_username));
            valid = false;
        } else if (username.contains(" ") || username.contains("@") || username.contains("#")) {
            _usernameText.setError(getString(R.string.valid_username2));
            valid = false;
        } else if (username.contains("test")) {
            _usernameText.setError(getString(R.string.valid_username3));
            valid = false;
        } else if (username.contains("samsung")) {
            _usernameText.setError(getString(R.string.valid_username3));
            valid = false;
        } else {
            _usernameText.setError(null);
        }

        if (password.isEmpty() || password.length() < 3 || password.length() > 32) {
            _passwordText.setError(getString(R.string.valid_password));
            valid = false;
        } else if (password.contains(" ")) {
            _passwordText.setError(getString(R.string.valid_username2));
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        return valid;
    }

    // common toast method
    private void toast(String text) {
        Toast.makeText(Login.this, text, Toast.LENGTH_SHORT).show();
    }

    // back arrow action
    @Override
    public boolean onSupportNavigateUp() {
        //onBackPressed();
        return true;
    }

    // back button press method
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onresume");
        super.onResume();

        // register run time internet checking broadcast receiver
        registerInternetCheckReceiver();

        // register receiver for connection
        LocalBroadcastManager.getInstance(Login.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("signin"));

        // register receiver for connection error
        LocalBroadcastManager.getInstance(Login.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("connectionerror"));

        // register receiver for registration error
        LocalBroadcastManager.getInstance(Login.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("signuperror"));

        // register receiver for login error
        LocalBroadcastManager.getInstance(Login.this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("signinerror"));
    }

    @Override
    protected void onPause() {
        // unregister all receiver
        unregisterReceiver(broadcastReceiver);
        LocalBroadcastManager.getInstance(Login.this).unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    public void XmppLogin() {
        // go to service to make login
        try {
            Intent intent = new Intent(getBaseContext(), ConnectXmpp.class);
            intent.putExtra("user", userName);
            intent.putExtra("pwd", password);
            intent.putExtra("code", "0");
            startService(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("login_xmpp: ", "Login exception: " + e.getMessage());
            showRegistrationError(getString(R.string.xmpp_signin_error));
        }
    }

    public void XmppRegistration() {
        // go to service to make registration
        try {
            _loginButton.setEnabled(false);
            userName = _usernameText.getText().toString().toLowerCase();
            Log.d("login_xmpp: ", userName);
            password = _passwordText.getText().toString();
            Intent intent = new Intent(getBaseContext(), ConnectXmpp.class);
            intent.putExtra("user", userName);
            intent.putExtra("pwd", password);
            intent.putExtra("code", "4");
            startService(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("login_xmpp: ", "Registration exception: " + e.getMessage());
            showRegistrationError(getString(R.string.xmpp_signup_error));
        }
    }

    // create snack bar
    public void initSnackBar() {
        // show no internet image
        snackbar = Snackbar
                .make(mLoginMainContent, getString(R.string.no_internet_connection), Snackbar.LENGTH_LONG)
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
                // show snack bar
                showSnackBar();
            }
            // if internet connected once again
            else {
                Log.d(TAG, "connectivity: " + internetStatus);
                isWiFiConnected = true;
                //hide snack bar
                hideSnackBar();
            }
        }
    };

    // show progress dialog
    public void showProgress() {
        // if previously not showing it then show that
        if (!Login.this.isFinishing()) {
            if (mProgressDialog != null) {
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

    // show snack bar
    public void showSnackBar() {
        // if previously not showing it then show that
        if (!Login.this.isFinishing()) {
            if (snackbar != null) {
                snackbar.show();
            }
        }
    }

    // hide snack bar
    public void hideSnackBar() {
        // if previously showing it then hide that
        if (snackbar != null) {
            snackbar.dismiss();
        }
    }

    // hide Success dialog
    public void hideSuccessDialog() {
        // if previously showing it then hide that
        if (savedDialog.isShowing()) {
            savedDialog.hide();
        }
    }
}