package webhawksit.com.livestreaming.utils;

import android.content.Context;
import android.widget.Toast;

import com.red5pro.streaming.R5StreamProtocol;
import com.red5pro.streaming.config.R5Configuration;

import webhawksit.com.livestreaming.activity.PublishActivity;

public class CommonUtilities {

    // Common toast method
    public static void MakeToast(Context context, String Message){
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, Message, duration);
        toast.show();
    }
}
