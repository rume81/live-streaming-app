package webhawksit.com.livestreaming.chat.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import webhawksit.com.livestreaming.chat.utils.LocalBinder;
import webhawksit.com.livestreaming.chat.utils.MyXMPP;
import webhawksit.com.livestreaming.utils.PrefManager;

import static webhawksit.com.livestreaming.utils.NetworkChecking.getConnectivityStatusString;


public class ConnectXmpp extends Service {

    private String userName;
    private String roomName;
    private String passWord;
    private String mChat;
    private String mSubject;
    private MyXMPP xmpp = new MyXMPP(this);

    Context context = this;

    public ConnectXmpp() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("xmpp: ", "connection service onCreate");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return new LocalBinder<ConnectXmpp>(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("xmpp: ", "connection service onStartCommand");

        if (intent != null) {
            try {
                roomName = intent.getStringExtra("room");
                userName = intent.getStringExtra("user");
                passWord = intent.getStringExtra("pwd");
                mChat = intent.getStringExtra("chat");
                mSubject = intent.getStringExtra("subject");
            } catch (Exception e) {
            }
            String code = intent.getStringExtra("code");
            // login
            if (code.equals("0")) {
                xmpp.initForLogin(userName, passWord);
                xmpp.connectConnection();
            }
            // join chat room
            else if (code.equals("1")) {
                xmpp.joinChatRoom(userName, roomName);
            }
            // send chat
            else if (code.equals("2")) {
                xmpp.sendChat(mChat, mSubject);
            }
            // exit from chat room
            else if (code.equals("3")) {
                xmpp.exitFromRoom();
            }
            // registration
            else if (code.equals("4")) {
                xmpp.initForRegistration(userName, passWord);
                xmpp.connectConnection();
            }
            // create chat room
            else if (code.equals("5")) {
                xmpp.createPersistentRoom(userName);
                //xmpp.createChatRoom(userName);
            }
            // destroy chat room
            else if (code.equals("6")) {
                xmpp.destroyChatRoom();
            }
            // configure chat room after create new chat room
            else if (code.equals("7")) {
                xmpp.configRoom();
            }
            // logout from the server
            else if (code.equals("9")) {
                xmpp.disconnectConnection();
            }
            // room status from the server
            else if (code.equals("10")) {
                xmpp.getRoomStatus(userName);
            }
            // destroy duplicate room from the server
            else if (code.equals("11")) {
                xmpp.destroyChatRoomToAvoidDuplicate();
            }
            // check user status from the server
            else if (code.equals("12")) {
                xmpp.UserStatus(userName);
            }
        }
        return START_REDELIVER_INTENT; //START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d("xmpp: ", "connection service destroyed");
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // set user has no running session
        PrefManager.setServerStatus(context, "No");
        // disconnect user
        xmpp.disconnectConnection();
        Log.e("xmpp: ", "connection service going to be destroyed");
    }

}
