package webhawksit.com.livestreaming.activity;


import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.readystatesoftware.systembartint.SystemBarTintManager;

import cn.pedant.SweetAlert.SweetAlertDialog;
import webhawksit.com.livestreaming.R;

public class ForgotPassword extends AppCompatActivity {

    private static final String TAG = ForgotPassword.class.getSimpleName();
    TextInputLayout inputLayoutEmail;
    EditText _emailText;
    Button _resetButton;
    Handler handler;
    Bundle bundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.forgot_password);

        // create our manager instance after the content view is set
        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        // enable status bar tint
        tintManager.setStatusBarTintEnabled(true);
        // enable navigation bar tint
        tintManager.setNavigationBarTintEnabled(true);
        // set a custom tint color for all system bars
        tintManager.setTintColor(Color.parseColor("#08427a"));

        // Set up the toolbar.
        Toolbar forgot_toolbar = (Toolbar) findViewById(R.id.forgot_password_toolbar);
        setSupportActionBar(forgot_toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        ActionBar ctBr = getSupportActionBar();
        ctBr.setDisplayHomeAsUpEnabled(true);
        ctBr.setDisplayShowHomeEnabled(true);

        bundle = getIntent().getExtras();

        inputLayoutEmail = (TextInputLayout) findViewById(R.id.inputLayoutEmail);
        _emailText = (EditText) findViewById(R.id.input_email);

        _resetButton = (Button) findViewById(R.id.btn_reset);
        _resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendRequest();
            }
        });
    }

    public void sendRequest() {
        Log.d(TAG, "SendRequest");

        if (!validate()) {
            onRequestFailed();
            return;
        }

        _resetButton.setEnabled(false);
        // Go to Main Page
        handler = new Handler();
        final SweetAlertDialog savedDialog = new SweetAlertDialog(ForgotPassword.this, SweetAlertDialog.SUCCESS_TYPE);
        savedDialog.setTitleText(getString(R.string.success_dialog_title))
                .setContentText(getString(R.string.reset_dialog_message))
                .setConfirmText(getString(R.string.success_dialog_button))
                .show();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 1000ms
                savedDialog.dismissWithAnimation();
                startNewActivity();
            }
        }, 2500);
    }

    private void startNewActivity() {
        Log.d(TAG, "startNewActivity");
        startActivity(new Intent(ForgotPassword.this, Login.class));
        finish();
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
    }

    public void onRequestFailed() {
        Toast.makeText(getBaseContext(), getString(R.string.request_failed_message), Toast.LENGTH_LONG).show();
        _resetButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String email = _emailText.getText().toString();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError(getString(R.string.valid_email_address));
            valid = false;
        } else {
            _emailText.setError(null);
        }

        return valid;
    }

    private void toast(String text) {
        Toast.makeText(ForgotPassword.this, text, Toast.LENGTH_SHORT).show();
    }

    // back arrow action
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // back button press method
    @Override
    public void onBackPressed() {
        if (bundle.getString("comes_from").equals("profile")) {
            startActivity(new Intent(ForgotPassword.this, Profile.class));
            finish();
            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            super.onBackPressed();
        } else {
            startActivity(new Intent(ForgotPassword.this, Login.class));
            finish();
            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            super.onBackPressed();
        }
    }

}