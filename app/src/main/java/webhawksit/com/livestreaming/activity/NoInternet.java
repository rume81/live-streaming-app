package webhawksit.com.livestreaming.activity;


import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import webhawksit.com.livestreaming.R;
import webhawksit.com.livestreaming.utils.PrefManager;

public class NoInternet extends AppCompatActivity {

    private static final String TAG = NoInternet.class.getSimpleName();
    TextView mGoToSettings;
    ImageView mNoInternetAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.no_internet);

        // create our manager instance after the content view is set
        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        // enable status bar tint
        tintManager.setStatusBarTintEnabled(true);
        // enable navigation bar tint
        tintManager.setNavigationBarTintEnabled(true);
        // set a custom tint color for all system bars
        tintManager.setTintColor(Color.parseColor("#08427a"));

        // Set up the toolbar.
        Toolbar no_internet_toolbar = (Toolbar) findViewById(R.id.no_internet_toolbar);
        setSupportActionBar(no_internet_toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        ActionBar ctBr = getSupportActionBar();

        mNoInternetAnimation = (ImageView) findViewById(R.id.no_internet_image);
        Glide.with(NoInternet.this)
                .load(R.drawable.nointernet)
                .asGif()
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .crossFade()
                .into(mNoInternetAnimation);

        mGoToSettings = (TextView) findViewById(R.id.no_internet_toolbar_title);
        mGoToSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToSettings();
            }
        });

    }

    @Override
    public void onResume(){
        super.onResume();
        Log.d(TAG, "OnResume");
        // check internet connection
        // Initializing Internet Check
        if (hasConnection(NoInternet.this)){
            // Go to Next Page
            // Check if user doesn't put profile data
            if (PrefManager.getUserName(NoInternet.this).isEmpty()){
                Intent i = new Intent(NoInternet.this, Login.class);
                startActivity(i);
                finish();
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            }
            else {
                launchHomeScreen();
            }
        }
        else {
            // stay idle ... Lol
        }

    }

    private void launchHomeScreen() {
        Intent i = new Intent(NoInternet.this, MainActivity.class);
        startActivity(i);
        finish();
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
    }


    // Internet check method
    public boolean hasConnection(Context context){
        ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetwork=cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetwork != null && wifiNetwork.isConnected()){
            return true;
        }
        NetworkInfo mobileNetwork=cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (mobileNetwork != null && mobileNetwork.isConnected()){
            return true;
        }
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()){
            return true;
        }
        return false;
    }

    private void goToSettings() {
        startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
    }


    private void toast(String text) {
        Toast.makeText(NoInternet.this, text, Toast.LENGTH_SHORT).show();
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

}