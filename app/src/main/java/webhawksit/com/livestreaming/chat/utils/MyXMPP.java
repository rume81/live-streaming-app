package webhawksit.com.livestreaming.chat.utils;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import webhawksit.com.livestreaming.chat.model.ChatEvent;
import webhawksit.com.livestreaming.chat.model.ChatItem;
import webhawksit.com.livestreaming.utils.PrefManager;

import static webhawksit.com.livestreaming.utils.Const.ALTERNATE_CHAT_ROOM_REFERENCE;
import static webhawksit.com.livestreaming.utils.Const.CHAT_ROOM_SERVICE_NAME;
import static webhawksit.com.livestreaming.utils.Const.CHAT_SERVER_PORT;
import static webhawksit.com.livestreaming.utils.Const.CHAT_SERVER_RESOURCE_NAME;
import static webhawksit.com.livestreaming.utils.Const.CHAT_SERVER_SERVICE_NAME;
import static webhawksit.com.livestreaming.utils.Const.STREAMING_AND_CHAT_SERVER_ADDRESS;


public class MyXMPP {

    String TAG = "MyXMPP";
    private String userName = "";
    private String passWord = "";
    AbstractXMPPConnection connection;
    XMPPConnectionListener connectionListener = new XMPPConnectionListener();
    XMPPConnectionListener2 connectionListener2 = new XMPPConnectionListener2();
    private boolean connected;
    private boolean isToasted;
    private boolean chat_created;
    private boolean loggedin;

    MultiUserChat multiUserChat;
    MultiUserChatManager manager;
    private Context mContext;

    StanzaListener mStanzaListener;
    StanzaFilter filter;
    String mRoomName;
    String mServiceName;
    String mOwnerNick;
    String mRoomDescription;
    String mRoomNameGetFromServer;
    RoomInfo mRoomInfo;
    Message msg;
    String mRoomDescriptionFromServer;

    ArrayList<ChatItem> chatItem;

    RoomInfo info;
    String mRoomStatus;

    String mSubject;
    String roomDestroyReason = "live Streaming stopped";


    public MyXMPP(Context context) {
        mContext = context;
    }

    //Initialize
    public void initForLogin(String userId, String pwd) {
        this.userName = userId;
        this.passWord = pwd;
        XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder();
        configBuilder.setUsernameAndPassword(userName, passWord);
        configBuilder.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
        configBuilder.setResource(CHAT_SERVER_RESOURCE_NAME);
        configBuilder.setServiceName(CHAT_SERVER_SERVICE_NAME);
        configBuilder.setHost(STREAMING_AND_CHAT_SERVER_ADDRESS);
        configBuilder.setPort(CHAT_SERVER_PORT);
        connection = new XMPPTCPConnection(configBuilder.build());
        connection.addConnectionListener(connectionListener);
        Log.d("xmpp: ", "Initializing For Login!");
    }

    //Initialize
    public void initForRegistration(String userId, String pwd) {
        this.userName = userId;
        this.passWord = pwd;
        XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder();
        configBuilder.setUsernameAndPassword(userName, passWord);
        configBuilder.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
        configBuilder.setResource(CHAT_SERVER_RESOURCE_NAME);
        configBuilder.setServiceName(CHAT_SERVER_SERVICE_NAME);
        configBuilder.setHost(STREAMING_AND_CHAT_SERVER_ADDRESS);
        configBuilder.setPort(CHAT_SERVER_PORT);
        connection = new XMPPTCPConnection(configBuilder.build());
        connection.addConnectionListener(connectionListener2);
        Log.d("xmpp: ", "Initializing For Registration!");
    }

    // Disconnect Function
    public void disconnectConnection() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                connection.disconnect();
            }
        }).start();
    }

    // Connection function
    public void connectConnection() {
        AsyncTask<Void, Void, Boolean> connectionThread = new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... arg0) {
                // Create a connection
                try {
                    connection.connect();
                    connected = true;
                    Log.d("xmpp: ", "Connection Success");
                } catch (IOException e) {
                    Log.d("xmpp: ", "Connection Error: " + e.getMessage());
                    sendBroadCast("connectionerror", e.getMessage());
                } catch (SmackException e) {
                    Log.d("xmpp: ", "Connection Error: " + e.getMessage());
                    sendBroadCast("connectionerror", e.getMessage());
                } catch (XMPPException e) {
                    Log.d("xmpp: ", "Connection Error: " + e.getMessage());
                    sendBroadCast("connectionerror", e.getMessage());
                }
                return null;
            }
        };
        connectionThread.execute();
    }

    // registration
    public void registration() {
        // create the account:
        AccountManager accountManager = AccountManager.getInstance(connection);
        try {
            accountManager.createAccount(userName, passWord);
            Log.d("xmpp: ", "Registration Success");
            // call login method
            login();
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
            Log.d("xmpp: ", "Registration Failure No Response: " + e.getMessage());
            sendBroadCast("signuperror", e.getMessage());
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
            Log.d("xmpp: ", "Registration Failure XMPP error: " + e.getMessage());
            sendBroadCast("signuperror", e.getMessage());
            //XMPPError: conflict - cancel
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
            Log.d("xmpp: ", "Registration Failure not connected: " + e.getMessage());
            sendBroadCast("signuperror", e.getMessage());
        }
    }

    // Login function
    public void login() {
        try {
            connection.login(userName, passWord);
            //Log.i("LOGIN", "Yey! We're connected to the Xmpp server!");
            Log.d("xmpp: ", "Login Success");
            Presence presence = new Presence(Presence.Type.available);
            connection.sendPacket(presence);
            sendBroadCast("signin", "done");

            //send ping to server
            sendPing();

        } catch (XMPPException | SmackException | IOException e) {
            e.printStackTrace();
            Log.d("xmpp: ", "Login Failure: " + e.getMessage());
            sendBroadCast("signinerror", e.getMessage());
        } catch (Exception e) {
            Log.d("xmpp: ", "Login Failure: " + e.getMessage());
            sendBroadCast("signinerror", e.getMessage());
        }

    }

    // join chat room function
    public void joinChatRoom(String userName, String roomName) {
        mRoomName = roomName;
        try {
            mServiceName = connection.getServiceName();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("xmpp: ", "service name error: "+e.getMessage());
        }
        Log.d("xmpp: ", "Service Name: " + mServiceName);
        manager = MultiUserChatManager.getInstanceFor(connection);
        multiUserChat = manager.getMultiUserChat(mRoomName+ "@" +CHAT_ROOM_SERVICE_NAME+CHAT_SERVER_SERVICE_NAME);
        try {
            multiUserChat.join(userName);
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
            Log.d("xmpp: ", "Chat room join Error: " + e.getMessage());
            sendBroadCast("joinerror", e.getMessage());
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
            Log.d("xmpp: ", "Chat room join Error: " + e.getMessage());
            sendBroadCast("joinerror", e.getMessage());
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
            Log.d("xmpp: ", "Chat room join Error: " + e.getMessage());
            sendBroadCast("joinerror", e.getMessage());
        }

        // if user joined successfully
        if (multiUserChat.isJoined()) {
            Log.d("xmpp: ", "user has Joined in the chat room");
            sendBroadCast("join", "done");
            //call method to configure room
            // webchat
            // but need to call when we play stream from mobile
            //configRoom();
        }
    }

    // get room status
    public void getRoomStatus(String roomName){
        mRoomName = roomName;
        manager = MultiUserChatManager.getInstanceFor(connection);
        // Discover information about the room
        try {
            info = manager.getRoomInfo(mRoomName + "@" + CHAT_ROOM_SERVICE_NAME + CHAT_SERVER_SERVICE_NAME);
            sendBroadCast("roominfo", "ok");
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
            Log.d("xmpp: ", "Room Info Error: " + e.getMessage());
            mRoomStatus = e.getMessage();
            sendBroadCast("roominfo", "yes"); //XMPPError: item-not-found - cancel
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
            Log.d("xmpp: ", "Room Info Error: " + e.getMessage());
            mRoomStatus = e.getMessage();
            sendBroadCast("roominfo", mRoomStatus);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
            Log.d("xmpp: ", "Room Info Error: " + e.getMessage());
            mRoomStatus = e.getMessage();
            sendBroadCast("roominfo", mRoomStatus);
        }
    }

    // configure room for getting messages
    public void configRoom(){
        Log.d("xmpp: ", "ready to receive messages in the chat room");
        // add listener for receiving messages
        receiveMessages();
        try {
            //room info
            mRoomInfo = manager.getRoomInfo(mRoomName + "@" + CHAT_ROOM_SERVICE_NAME+ CHAT_SERVER_SERVICE_NAME);
            mOwnerNick = multiUserChat.getOwners().get(0).getNick();
            mRoomNameGetFromServer = mRoomInfo.getName();
            mRoomDescriptionFromServer = mRoomInfo.getDescription();

            Log.d("xmpp: ", "Room Name: " + mRoomNameGetFromServer + " Room Description: " + mRoomDescriptionFromServer + " Room Owner Nick: " + mOwnerNick);
            // room list
                /*for (int i = 0; i<manager.getHostedRooms("conference.webhawksit").size(); i++){
                    Log.d("xmpp: ", "Room List Id: "+i+"\nRoom Name: "+manager.getHostedRooms("conference.webhawksit").get(i).getName()+"\nRoom JID: "+manager.getHostedRooms("conference.webhawksit").get(i).getJid());
                }*/

        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
            Log.d("xmpp: ", "Get Room Configuration Error: "+e.getMessage());
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
            Log.d("xmpp: ", "Get Room Configuration Error: "+e.getMessage());
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
            Log.d("xmpp: ", "Get Room Configuration Error: "+e.getMessage());
        }
    }

    // check current user status
    public void UserStatus(String userName){
        Roster roster = Roster.getInstanceFor(connection);
        Presence presence = roster.getPresence(userName + "@"+ CHAT_SERVER_SERVICE_NAME);
        if (presence.getType() == Presence.Type.available) {
            // User is online...
            Log.d("xmpp: ", "user Online");
            sendBroadCast("userstatus", "online");
        } else {
            // User is Offline...
            Log.d("xmpp: ", "user Offline");
            sendBroadCast("userstatus", "offline");
        }
    }

    // send chat to the room
    public void sendChat(String chat, String subject) {
        try {
            Message newMessage = new Message();
            newMessage.setBody(chat);
            newMessage.setSubject(subject);
            multiUserChat.sendMessage(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
            Log.d("xmpp: ", "Message send Error: " + e.getMessage());
        }
    }

    // received Messages from room
    public void receiveMessages() {
        if (connected) {
            chatItem = new ArrayList<ChatItem>();
            filter = MessageTypeFilter.GROUPCHAT;
            mStanzaListener = new StanzaListener() {
                @Override
                public void processPacket(Stanza packet) throws SmackException.NotConnectedException {
                    Message message = (Message) packet;
                    if (message.getBody() != null) {
                        String from = message.getFrom();
                        String OnlyUserName = from.replace(mRoomName + "@"+ CHAT_ROOM_SERVICE_NAME + CHAT_SERVER_SERVICE_NAME+"\u002F", ""); //remove room name // (here \u002F is for forward slash)
                        Log.d("xmpp: ", "Original sender: " + from);
                        String body = message.getBody();
                        String messageID = message.getStanzaId();
                        if(message.getSubjects().isEmpty()){
                            mSubject = "comment";
                        }
                        else {
                            mSubject = message.getSubject();
                        }
                        Log.d("xmpp: ", "From: " + OnlyUserName + "\nSubject: " + mSubject + "\nMessage: " + body +"\nMessage ID: "+messageID);
                        EventBus.getDefault().postSticky(new ChatEvent(OnlyUserName, body, mSubject, messageID));
                    }
                }
            };
            connection.addSyncStanzaListener(mStanzaListener, filter);   // remove addAsyncStanzaListener to avoid duplicate messages
        }
    }

    // create persistent room
    public void createPersistentRoom(String userName){
        if (connection.isConnected()== true) {
            mRoomName = userName;
            mRoomDescription = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new Date()); // current time
            //mRoomDescription = "Live Streaming";
            mServiceName = connection.getServiceName();
            Log.d("xmpp: ", "Service Name: "+mServiceName);
            manager = MultiUserChatManager.getInstanceFor(connection);
            multiUserChat = manager.getMultiUserChat(mRoomName+"@"+ CHAT_ROOM_SERVICE_NAME + CHAT_SERVER_SERVICE_NAME);
            // Create the room
            try {
                multiUserChat.create(userName);
            } catch (XMPPException.XMPPErrorException e) {
                e.printStackTrace();
                Log.d("xmpp: ", "Chat Room Create Error: "+e.getMessage());
                sendBroadCast("roomcreateerror", e.getMessage());
            } catch (SmackException e) {
                e.printStackTrace();
                Log.d("xmpp: ", "Chat Room Create Error: "+e.getMessage());
                sendBroadCast("roomcreateerror", e.getMessage());
            }

            // send configuration for persistent room
            Form form = null;
            try {
                form = multiUserChat.getConfigurationForm();
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
                Log.d("xmpp: ", "Chat Room Configuration Error: "+e.getMessage());
                sendBroadCast("roomcreateerror", e.getMessage());
            } catch (XMPPException.XMPPErrorException e) {
                e.printStackTrace();
                Log.d("xmpp: ", "Chat Room Configuration Error: "+e.getMessage());
                sendBroadCast("roomcreateerror", e.getMessage());
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
                Log.d("xmpp: ", "Chat Room Configuration Error: "+e.getMessage());
                sendBroadCast("roomcreateerror", e.getMessage());
            }
            Form answerForm = form.createAnswerForm();
            answerForm.setAnswer("muc#roomconfig_publicroom", true);
            answerForm.setAnswer("muc#roomconfig_persistentroom", true);
            answerForm.setAnswer("muc#roomconfig_roomdesc", mRoomDescription);
            try {
                // Send room configuration form which indicates that we want
                multiUserChat.sendConfigurationForm(answerForm);
                sendBroadCast("create", "done");
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
                Log.d("xmpp: ", "Chat Room Configuration Error: "+e.getMessage());
                sendBroadCast("roomcreateerror", e.getMessage());
            } catch (XMPPException.XMPPErrorException e) {
                e.printStackTrace();
                Log.d("xmpp: ", "Chat Room Configuration Error: "+e.getMessage());
                sendBroadCast("roomcreateerror", e.getMessage());
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
                Log.d("xmpp: ", "Chat Room Configuration Error: "+e.getMessage());
                sendBroadCast("roomcreateerror", e.getMessage());
            }
        }
    }

    // destroy chat room to avoid duplicate room
    public void destroyChatRoomToAvoidDuplicate(){
        try {
            multiUserChat.destroy(roomDestroyReason, ALTERNATE_CHAT_ROOM_REFERENCE +"@"+ CHAT_ROOM_SERVICE_NAME + CHAT_SERVER_SERVICE_NAME);
            Log.d("xmpp: ", "Destroy Chat Room duplicate");
            sendBroadCast("destroyduplicate", "done");
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
            Log.d("xmpp: ", "Destroy Room duplicate Error: "+e.getMessage());
            sendBroadCast("destroyduplicateerror", e.getMessage());
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
            Log.d("xmpp: ", "Destroy Room duplicate Error: "+e.getMessage());
            sendBroadCast("destroyduplicateerror", e.getMessage());
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
            Log.d("xmpp: ", "Destroy Room duplicate Error: "+e.getMessage());
            sendBroadCast("destroyduplicateerror", e.getMessage());
        }
    }

    // destroy chat room
    // with broadcast
    public void destroyChatRoom(){
        try {
            multiUserChat.destroy(roomDestroyReason, ALTERNATE_CHAT_ROOM_REFERENCE +"@"+ CHAT_ROOM_SERVICE_NAME + CHAT_SERVER_SERVICE_NAME);
            Log.d("xmpp: ", "Destroy Chat Room by button!");
            sendBroadCast("destroy", "done");
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
            Log.d("xmpp: ", "Destroy Room by button Error: "+e.getMessage());
            sendBroadCast("destroyerror", e.getMessage());
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
            Log.d("xmpp: ", "Destroy Room by button Error: "+e.getMessage());
            sendBroadCast("destroyerror", e.getMessage());
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
            Log.d("xmpp: ", "Destroy Room by button Error: "+e.getMessage());
            sendBroadCast("destroyerror", e.getMessage());
        }
    }

    // send broadcast to the other activities
    public void sendBroadCast(String type, String message) {
        String mType = type;
        String intentText = null;
        if (mType.equals("join")) {
            intentText = "join";
        }
        else if (mType.equals("signin")) {
            intentText = "signin";
        }
        else if (mType.equals("signuperror")) {
            intentText = "signuperror";
        }
        else if (mType.equals("signinerror")) {
            intentText = "signinerror";
        }
        else if (mType.equals("connectionerror")){
            intentText = "connectionerror";
        }
        else if (mType.equals("create")){
            intentText = "create";
        }
        else if (mType.equals("roomcreateerror")){
            intentText = "roomcreateerror";
        }
        else if (mType.equals("destroyerror")){
            intentText = "destroyerror";
        }
        else if (mType.equals("destroy")){
            intentText = "destroy";
        }
        else if (mType.equals("connectionclosederror")){
            intentText = "connectionclosederror";
        }
        else if (mType.equals("connectionclosed")){
            intentText = "connectionclosed";
        }
        else if (mType.equals("roominfo")){
            intentText = "roominfo";
        }
        else if (mType.equals("destroyduplicate")){
            intentText = "destroyduplicate";
        }
        else if (mType.equals("destroyduplicateerror")){
            intentText = "destroyduplicateerror";
        }
        else if (mType.equals("userstatus")){
            intentText = "userstatus";
        }
        else if (mType.equals("joinerror")){
            intentText = "joinerror";
        }
        else if (mType.equals("ping")){
            intentText = "false";
        }
        Intent broadCastIntent = new Intent(intentText);
        broadCastIntent.putExtra("action", message);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(broadCastIntent);
    }

    // method for ping manager
    public void sendPing(){
        PingManager pm =  PingManager.getInstanceFor(connection) ;
        pm.setPingInterval(5) ;  // 5 sec
        try {
            pm.pingMyServer() ;
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
        pm.registerPingFailedListener(new PingFailedListener() {
            @Override
            public void pingFailed() {
                Log.e("xmpp: " , "Ping Failed") ;
                PrefManager.setServerStatus(mContext, "No");
                sendBroadCast("ping", "false");
            }
        });
    }

    //Connection Listener to check connection state
    public class XMPPConnectionListener implements ConnectionListener {
        @Override
        public void connected(final XMPPConnection connection) {

            Log.d("xmpp: ", "Connected Login!");
            connected = true;
            if (!connection.isAuthenticated()) {
                login();
            }
        }

        @Override
        public void connectionClosed() {
            if (isToasted)

                new Handler(Looper.getMainLooper()).post(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                    }
                });
            Log.d("xmpp: ", "Connection Closed Login!");
            PrefManager.setServerStatus(mContext, "No");
            sendBroadCast("connectionclosed", "login");
            connected = false;
            chat_created = false;
            loggedin = false;
        }

        @Override
        public void connectionClosedOnError(Exception arg0) {
            if (isToasted)
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            Log.d("xmpp: ", "Connection Closed On Error Login!");
            //PrefManager.setServerStatus(mContext, "No");
            // send broad cast when connection closed by error
            sendBroadCast("connectionclosederror", "login");
            connected = false;
            chat_created = false;
            loggedin = false;
        }

        @Override
        public void reconnectingIn(int arg0) {
            Log.d("xmpp: ", "Reconnectingin Login" + arg0);
            loggedin = false;
        }

        @Override
        public void reconnectionFailed(Exception arg0) {
            if (isToasted)
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            Log.d("xmpp: ", "Reconnection Failed Login!");
            connected = false;
            chat_created = false;
            loggedin = false;
        }

        @Override
        public void reconnectionSuccessful() {
            if (isToasted)
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub

                    }
                });
            Log.d("xmpp: ", "Reconnection Successful Login!");
            connected = true;
            chat_created = false;
            loggedin = false;
        }

        @Override
        public void authenticated(XMPPConnection arg0, boolean arg1) {
            Log.d("xmpp: ", "Authenticated Login!");
            loggedin = true;
            chat_created = false;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }
            }).start();
            if (isToasted)
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub

                    }
                });
        }
    }

    //Connection Listener to check connection state
    public class XMPPConnectionListener2 implements ConnectionListener {
        @Override
        public void connected(final XMPPConnection connection) {

            Log.d("xmpp: ", "Connected Registration!");
            connected = true;
            if (!connection.isAuthenticated()) {
                registration();
            }
        }

        @Override
        public void connectionClosed() {
            if (isToasted)

                new Handler(Looper.getMainLooper()).post(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                    }
                });
            Log.d("xmpp: ", "ConnectionCLosed2!");
            PrefManager.setServerStatus(mContext, "No");
            sendBroadCast("connectionclosed", "registration");
            connected = false;
            chat_created = false;
            loggedin = false;
        }

        @Override
        public void connectionClosedOnError(Exception arg0) {
            if (isToasted)
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            Log.d("myxmpp: ", "Connection Closed On Error Registration!");
            //PrefManager.setServerStatus(mContext, "No");
            // send broad cast when connection closed by error
            sendBroadCast("connectionclosederror", "registration");
            connected = false;
            chat_created = false;
            loggedin = false;
        }

        @Override
        public void reconnectingIn(int arg0) {
            Log.d("xmpp: ", "Reconnectingin Registration " + arg0);
            loggedin = false;
        }

        @Override
        public void reconnectionFailed(Exception arg0) {
            if (isToasted)
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            Log.d("xmpp: ", "Reconnection Failed Registration!");
            connected = false;
            chat_created = false;
            loggedin = false;
        }

        @Override
        public void reconnectionSuccessful() {
            if (isToasted)
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub

                    }
                });
            Log.d("xmpp: ", "Reconnection Successful registration!");
            connected = true;
            chat_created = false;
            loggedin = false;
        }

        @Override
        public void authenticated(XMPPConnection arg0, boolean arg1) {
            Log.d("xmpp: ", "Authenticated Registration!");
            loggedin = true;
            chat_created = false;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }
            }).start();
            if (isToasted)
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub

                    }
                });
        }
    }


    // the following three methods are not used by his app but they are important so that we have to keep this in code

    // exit from the room
    public void exitFromRoom() {
        try {
            //remove listener
            connection.removeAsyncStanzaListener(mStanzaListener);
            // leave room
            multiUserChat.leave();
            Log.d("xmpp: ", "Leave Chat Room!");
            sendBroadCast("exit", "done");
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
            Log.d("xmpp: ", "Leave Chat Room Error: " + e.getMessage());
            sendBroadCast("exiterror", e.getMessage());
        }
    }

    // exit from the room without broadcast
    public void exitFromRoomWithOutBroadcast() {
        try {
            //remove listener
            connection.removeAsyncStanzaListener(mStanzaListener);
            // leave room
            multiUserChat.leave();
            Log.d("xmpp: ", "Leave Chat Room without broadcast!");
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
            Log.d("xmpp: ", "Leave Chat Room without broadcast Error: " + e.getMessage());
        }
    }

    // create temporary chat room
    public void createChatRoom(String userName) {
        if (connection.isConnected()== true) {
            mRoomName = userName;
            manager = MultiUserChatManager.getInstanceFor(connection);
            multiUserChat = manager.getMultiUserChat(mRoomName+"@"+ CHAT_ROOM_SERVICE_NAME + CHAT_SERVER_SERVICE_NAME);
            // Create the room
            try {
                multiUserChat.create(mRoomName);
            } catch (XMPPException.XMPPErrorException e) {
                e.printStackTrace();
                Log.d("xmpp: ", "Temporary Chat Room Create Error: "+e.getMessage());
                sendBroadCast("roomcreateerror", e.getMessage());
            } catch (SmackException e) {
                e.printStackTrace();
                Log.d("xmpp: ", "Temporary Chat Room Create Error: "+e.getMessage());
                sendBroadCast("roomcreateerror", e.getMessage());
            }
            // Send an empty room configuration form which indicates that we want
            // an instant room
            try {
                multiUserChat.sendConfigurationForm(new Form(DataForm.Type.submit));
                sendBroadCast("create", "done");
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
                Log.d("xmpp: ", "Temporary Chat Room Configuration Error: "+e.getMessage());
                sendBroadCast("roomcreateerror", e.getMessage());
            } catch (XMPPException.XMPPErrorException e) {
                e.printStackTrace();
                Log.d("xmpp: ", "Temporary Chat Room Configuration Error: "+e.getMessage());
                sendBroadCast("roomcreateerror", e.getMessage());
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
                Log.d("xmpp: ", "Temporary Chat Room Configuration Error: "+e.getMessage());
                sendBroadCast("roomcreateerror", e.getMessage());
            }
        }
    }
}
