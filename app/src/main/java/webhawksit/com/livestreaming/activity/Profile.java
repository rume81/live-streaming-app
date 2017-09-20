package webhawksit.com.livestreaming.activity;


import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import webhawksit.com.livestreaming.R;
import webhawksit.com.livestreaming.utils.PrefManager;
import webhawksit.com.livestreaming.utils.ExternalStoragePermission;

public class Profile extends AppCompatActivity {

    ImageView mProfileEditImage;
    private int REQUEST_CAMERA = 0, SELECT_FILE = 1;
    private String userChoosenTask;
    String mEncodedImage;
    LinearLayout username_layout, gender_layout, address_layout, email_layout;
    TextView tvUserName, tvUserEmail, tvUserGender, tvUserAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);

        // create our manager instance after the content view is set
        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        // enable status bar tint
        tintManager.setStatusBarTintEnabled(true);
        // enable navigation bar tint
        tintManager.setNavigationBarTintEnabled(true);
        // set a custom tint color for all system bars
        tintManager.setTintColor(Color.parseColor("#08427a"));

        final CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.profile_toolbar_layout);
        collapsingToolbarLayout.setTitle(getString(R.string.profile_toolbar));

        Toolbar toolbar = (Toolbar) findViewById(R.id.profile_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        ActionBar ctBr = getSupportActionBar();
        ctBr.setDisplayHomeAsUpEnabled(true);
        ctBr.setDisplayShowHomeEnabled(true);

        username_layout = (LinearLayout)findViewById(R.id.username_layout);
        username_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editUserName();
            }
        });

        email_layout = (LinearLayout)findViewById(R.id.email_layout);
        email_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(Profile.this, getString(R.string.don_not_edit_email_address), Toast.LENGTH_SHORT).show();
            }
        });

        gender_layout = (LinearLayout)findViewById(R.id.gender_layout);
        gender_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editGender();
            }
        });

        address_layout = (LinearLayout)findViewById(R.id.address_layout);
        address_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editAddress();
            }
        });

        tvUserName = (TextView)findViewById(R.id.tvUserName);
        if (!PrefManager.getUserFullName(Profile.this).isEmpty()) {
            tvUserName.setText(PrefManager.getUserFullName(Profile.this));
        } else {
            tvUserName.setText(getString(R.string.enter_data));
        }

        tvUserEmail = (TextView)findViewById(R.id.tvUserEmail);
        tvUserEmail.setText(PrefManager.getUserName(Profile.this)+"@livestreaming.com");

        tvUserGender = (TextView)findViewById(R.id.tvUserGender);
        if (!PrefManager.getUserGender(Profile.this).isEmpty()) {
            tvUserGender.setText(PrefManager.getUserGender(Profile.this));
        } else {
            tvUserGender.setText(getString(R.string.enter_data));
        }
        tvUserAddress = (TextView)findViewById(R.id.tvUserAddress);
        if (!PrefManager.getUserAddress(Profile.this).isEmpty()) {
            tvUserAddress.setText(PrefManager.getUserAddress(Profile.this));
        } else {
            tvUserAddress.setText(getString(R.string.enter_data));
        }

        mProfileEditImage = (ImageView)findViewById(R.id.profile_collapsing_image);
        String mStoredImageUrl = PrefManager.getUserPhoto(Profile.this);
        if( !mStoredImageUrl.equalsIgnoreCase("") ){
            byte[] b = Base64.decode(mStoredImageUrl, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(b, 0, b.length);
            mProfileEditImage.setImageBitmap(bitmap);
        }
        else {
            mProfileEditImage.setBackgroundResource(R.drawable.photo_empty);
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.profile_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage();
            }
        });
    }

    private void editUserName() {
        final String[] str = {""};
        MaterialDialog builder = new MaterialDialog.Builder(this)
                .title(getString(R.string.edit_display_name))
                .backgroundColor(getResources().getColor(R.color.common_page_bg))
                .widgetColor(getResources().getColor(R.color.Lemon))
                .inputRange(20, R.color.soft_red)
                .inputRange(4, 20)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input(getString(R.string.profile_edit_hint), "", new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog dialog, CharSequence input) {
                        str[0] = input.toString();
                        tvUserName.setText(str[0]);
                        PrefManager.setUserFullName(Profile.this, str[0]);
                    }
                }).negativeText(getString(R.string.cancel)).show();
    }

    private void editAddress() {
        final String[] str = {""};
        MaterialDialog builder = new MaterialDialog.Builder(this)
                .title(getString(R.string.edit_address))
                .backgroundColor(getResources().getColor(R.color.common_page_bg))
                .widgetColor(getResources().getColor(R.color.Lemon))
                .inputRange(20, R.color.soft_red)
                .inputRange(4, 150)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input(getString(R.string.profile_edit_hint), "", new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog dialog, CharSequence input) {
                        str[0] = input.toString();
                        //add function
                        tvUserAddress.setText(str[0]);
                        PrefManager.setUserAddress(Profile.this, str[0]);
                    }
                }).negativeText(getString(R.string.cancel)).show();
    }

    public void editGender(){
        new MaterialDialog.Builder(this)
                .title(getString(R.string.edit_gender))
                .items(R.array.gender)
                .backgroundColor(getResources().getColor(R.color.common_page_bg))
                .itemsCallbackSingleChoice(-1, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        /**
                         * If you use alwaysCallSingleChoiceCallback(), which is discussed below,
                         * returning false here won't allow the newly selected radio button to actually be selected.
                         **/
                        if (which == 0){
                            tvUserGender.setText(getString(R.string.male));
                            PrefManager.setUserGender(Profile.this, getString(R.string.male));
                        }
                        else if (which == 1){
                            tvUserGender.setText(getString(R.string.female));
                            PrefManager.setUserGender(Profile.this, getString(R.string.female));
                        }
                        else {
                            Toast.makeText(Profile.this, getString(R.string.select_one_item), Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    }
                })
                .positiveText(getString(R.string.choose))
                .show();
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
                    // write some custom features
                }
                break;
        }
    }

    private void selectImage() {
        final CharSequence[] items = { getString(R.string.take_photo), getString(R.string.choose_from_gallery),
                getString(R.string.cancel) };

        AlertDialog.Builder builder = new AlertDialog.Builder(Profile.this);
        builder.setTitle(getString(R.string.add_photo));
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean result= ExternalStoragePermission.checkPermission(Profile.this);

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
        mEncodedImage = Base64.encodeToString(b, Base64.DEFAULT);
        PrefManager.setUserPhoto(Profile.this, mEncodedImage);

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

        mProfileEditImage.setImageBitmap(thumbnail);
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
                mEncodedImage = Base64.encodeToString(b, Base64.DEFAULT);
                PrefManager.setUserPhoto(Profile.this, mEncodedImage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mProfileEditImage.setImageBitmap(bm);

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
        startActivity(new Intent(Profile.this, MainActivity.class));
        finish();
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_password_reset) {
            Intent intent = new Intent(Profile.this, ForgotPassword.class);
            intent.putExtra("comes_from", "profile");
            startActivity(intent);
            finish();
            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            super.onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
