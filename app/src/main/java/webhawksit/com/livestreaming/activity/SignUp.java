package webhawksit.com.livestreaming.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import cn.pedant.SweetAlert.SweetAlertDialog;
import webhawksit.com.livestreaming.R;
import webhawksit.com.livestreaming.utils.PrefManager;
import webhawksit.com.livestreaming.utils.ExternalStoragePermission;

public class SignUp extends AppCompatActivity {

    private static final String TAG = SignUp.class.getSimpleName();
    TextInputLayout inputLayoutName, inputLayoutEmail, inputLayoutPassword, inputLayoutRePassword;
    EditText _nameText, _emailText, _passwordText, _reEnterPasswordText;
    ImageView mProfilePhoto;
    Button _signupButton;
    Handler handler;
    private int REQUEST_CAMERA = 0, SELECT_FILE = 1;
    private String userChoosenTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.signup);

        // create our manager instance after the content view is set
        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        // enable status bar tint
        tintManager.setStatusBarTintEnabled(true);
        // enable navigation bar tint
        tintManager.setNavigationBarTintEnabled(true);
        // set a custom tint color for all system bars
        tintManager.setTintColor(Color.parseColor("#08427a"));

        // Set up the toolbar.
        Toolbar signup_toolbar = (Toolbar) findViewById(R.id.signup_toolbar);
        setSupportActionBar(signup_toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        ActionBar ctBr = getSupportActionBar();
        ctBr.setDisplayHomeAsUpEnabled(true);
        ctBr.setDisplayShowHomeEnabled(true);

        inputLayoutName = (TextInputLayout) findViewById(R.id.inputLayoutName);
        inputLayoutEmail = (TextInputLayout) findViewById(R.id.inputLayoutEmail);
        inputLayoutPassword = (TextInputLayout) findViewById(R.id.inputLayoutPassword);
        inputLayoutRePassword = (TextInputLayout) findViewById(R.id.inputLayoutRePassword);

        _nameText = (EditText) findViewById(R.id.input_name);
        _emailText = (EditText) findViewById(R.id.input_email);
        _passwordText = (EditText) findViewById(R.id.input_password);
        _reEnterPasswordText = (EditText) findViewById(R.id.input_reEnterPassword);

        mProfilePhoto = (ImageView) findViewById(R.id.signup_profile_image);
        mProfilePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });

        _signupButton = (Button) findViewById(R.id.btn_signup);
        _signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signup();
            }
        });
    }

    public void signup() {
        Log.d(TAG, "Signup");

        if (!validate()) {
            onSignupFailed();
            return;
        }

        _signupButton.setEnabled(false);
        String userName = _emailText.getText().toString();
        PrefManager.setUserName(SignUp.this, userName);
        String password = _passwordText.getText().toString();
        PrefManager.setUserPassword(SignUp.this, password);
        String UserFullName = _nameText.getText().toString();
        PrefManager.setUserFullName(SignUp.this, UserFullName);
        // Go to Main Page
        handler = new Handler();
        final SweetAlertDialog savedDialog = new SweetAlertDialog(SignUp.this, SweetAlertDialog.SUCCESS_TYPE);
        savedDialog.setTitleText(getString(R.string.success_dialog_title))
                .setContentText(getString(R.string.sign_up_dialog_message))
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
        startActivity(new Intent(SignUp.this, MainActivity.class));
        finish();
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
    }

    public void onSignupFailed() {
        Toast.makeText(getBaseContext(), getString(R.string.sign_up_failed), Toast.LENGTH_LONG).show();
        _signupButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String name = _nameText.getText().toString();
        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();
        String reEnterPassword = _reEnterPasswordText.getText().toString();

        if (name.isEmpty() || name.length() < 4 || name.length() > 20) {
            _nameText.setError(getString(R.string.valid_name));
            valid = false;
        } else {
            _nameText.setError(null);
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError(getString(R.string.valid_email_address));
            valid = false;
        } else {
            _emailText.setError(null);
        }

        if (password.isEmpty() || password.length() < 8 || password.length() > 16) {
            _passwordText.setError(getString(R.string.valid_password));
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        if (reEnterPassword.isEmpty() || reEnterPassword.length() < 8 || reEnterPassword.length() > 16 || !(reEnterPassword.equals(password))) {
            _reEnterPasswordText.setError(getString(R.string.password_does_not_match));
            valid = false;
        } else {
            _reEnterPasswordText.setError(null);
        }
        return valid;
    }

    private void toast(String text) {
        Toast.makeText(SignUp.this, text, Toast.LENGTH_SHORT).show();
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
        startActivity(new Intent(SignUp.this, Login.class));
        finish();
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        super.onBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case ExternalStoragePermission.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(userChoosenTask.equals("Take Photo"))
                        cameraIntent();
                    else if(userChoosenTask.equals("Choose from Gallery"))
                        galleryIntent();
                } else {
                    //code for deny
                }
                break;
        }
    }

    private void selectImage() {
        final CharSequence[] items = {  getString(R.string.take_photo), getString(R.string.choose_from_gallery),
                getString(R.string.cancel) };

        AlertDialog.Builder builder = new AlertDialog.Builder(SignUp.this);
        builder.setTitle(getString(R.string.add_photo));
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean result= ExternalStoragePermission.checkPermission(SignUp.this);

                if (items[item].equals("Take Photo")) {
                    userChoosenTask ="Take Photo";
                    if(result)
                        cameraIntent();

                } else if (items[item].equals("Choose from Gallery")) {
                    userChoosenTask ="Choose from Gallery";
                    if(result)
                        galleryIntent();

                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void galleryIntent()
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);//
        startActivityForResult(Intent.createChooser(intent, "Select File"),SELECT_FILE);
    }

    private void cameraIntent()
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_FILE)
                onSelectFromGalleryResult(data);
            else if (requestCode == REQUEST_CAMERA)
                onCaptureImageResult(data);
        }
    }

    private void onCaptureImageResult(Intent data) {
        Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes);

        // to store in shared preference
        byte[] b = bytes.toByteArray();
        String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
        PrefManager.setUserPhoto(SignUp.this, encodedImage);

        File destination = new File(Environment.getExternalStorageDirectory(),
                System.currentTimeMillis() + ".jpg");

        FileOutputStream fo;
        try {
            destination.createNewFile();
            fo = new FileOutputStream(destination);
            fo.write(bytes.toByteArray());
            fo.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mProfilePhoto.setImageBitmap(thumbnail);
    }

    @SuppressWarnings("deprecation")
    private void onSelectFromGalleryResult(Intent data) {

        Bitmap bm=null;
        if (data != null) {
            try {
                bm = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), data.getData());
                // to store in shared preference
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] b = baos.toByteArray();
                String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
                PrefManager.setUserPhoto(SignUp.this, encodedImage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mProfilePhoto.setImageBitmap(bm);
    }

}