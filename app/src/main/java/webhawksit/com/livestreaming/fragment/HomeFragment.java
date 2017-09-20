package webhawksit.com.livestreaming.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cn.pedant.SweetAlert.SweetAlertDialog;
import webhawksit.com.livestreaming.R;
import webhawksit.com.livestreaming.activity.PlayStream;
import webhawksit.com.livestreaming.activity.PublishActivity;
import webhawksit.com.livestreaming.adapter.StreamsAdapter;
import webhawksit.com.livestreaming.model.Streams;
import webhawksit.com.livestreaming.utils.AppController;
import webhawksit.com.livestreaming.utils.Const;
import webhawksit.com.livestreaming.utils.ItemClickSupport;
import webhawksit.com.livestreaming.utils.NetworkChecking;
import webhawksit.com.livestreaming.utils.PrefManager;

import static android.content.Context.MODE_PRIVATE;
import static android.support.v4.content.ContextCompat.checkSelfPermission;
import static webhawksit.com.livestreaming.utils.Const.STREAMING_SERVER_ACCESS_TOKEN;

public class HomeFragment extends Fragment {
    //Defining Variables
    private static final String TAG = HomeFragment.class.getSimpleName();
    public static FloatingActionButton live_fab;
    RelativeLayout mHomeParent;
    RelativeLayout mNoInternet;
    RelativeLayout mNoStreams;
    // list inflating variable
    ArrayList<Streams> streams;
    RecyclerView mRecyclerView;
    LinearLayout mPullToRefreshLayout;
    LinearLayoutManager mLinearLayoutManager;
    StreamsAdapter adapter;
    String mStreamName;
    ProgressBar mProgress;
    RelativeLayout mLoadingProgress;
    SwipeRefreshLayout refreshLayout;
    Snackbar snackbar;
    TextView pull_to_refresh_message;
    ImageView down_arrow;
    // These tags will be used to cancel the requests
    private String tag_json_obj = "jobj_req";
    // Single Stream Information from server
    String wholeData;
    String ResponseStatus;
    int ResponseCode;
    String activeUser;
    String isRecording;
    int ActualActiveUser;
    String visitedTimes;
    String createdAt;
    // permission variable
    private static final int PERMISSION_CALLBACK_CONSTANT = 100;
    private static final int REQUEST_PERMISSION_SETTING = 101;
    String[] permissionsRequired = new String[]{Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private SharedPreferences permissionStatus;
    private boolean sentToSettings = false;
    SweetAlertDialog mPermissionDialog;
    // imageView for animation in home page which is temporary and will remove when stream list will be used
    ImageView mHomeAnimation;


    public static HomeFragment newInstance() {
        HomeFragment fragment = new HomeFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.home, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        //OnResume Fragment
        // Initializing Internet Check
        if (NetworkChecking.hasConnection(getActivity())) {
            if (mNoInternet.isShown()) {
                // hide no internet image
                mNoInternet.setVisibility(View.GONE);
            }
            // get all stream list
            GetAllStreams();
        } else {
            // if there is no internet
            if (mNoStreams.isShown()) {
                mNoStreams.setVisibility(View.GONE);
            }
            if (!mNoInternet.isShown()) {
                mNoInternet.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onPause() {
        //onPause Fragment
        super.onPause();
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //view initialize and functionality declare
        mHomeParent = (RelativeLayout) view.findViewById(R.id.home_parent_layout);
        mNoInternet = (RelativeLayout) view.findViewById(R.id.stream_no_internet);
        mNoStreams = (RelativeLayout) view.findViewById(R.id.no_streams);

        //animation in home page which is temporary and will remove when stream list will be used
        mHomeAnimation = (ImageView) view.findViewById(R.id.home_animation);
        Glide.with(getActivity())
                .load(R.drawable.selfie)
                .asGif()
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .crossFade()
                .into(mHomeAnimation);

        // shared preference initialization for permission status
        permissionStatus = getActivity().getSharedPreferences("permissionStatus", MODE_PRIVATE);

        // initializing floating action button
        live_fab = (FloatingActionButton) view.findViewById(R.id.live_fab);
        live_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Initializing Internet Check
                if (NetworkChecking.hasConnection(getActivity())) {
                    // Take permission and then go for camera
                    TakePermission();
                } else {
                    // if there is no internet
                    toast(getString(R.string.no_internet_connection));
                }
            }
        });

        // initialization of streams list
        streams = new ArrayList<Streams>();

        mPullToRefreshLayout = (LinearLayout) view.findViewById(R.id.pull_to_refresh_message_layout);
        down_arrow = (ImageView) view.findViewById(R.id.down_arrow);
        pull_to_refresh_message = (TextView) view.findViewById(R.id.pull_to_refresh_message);

        // animate the down arrow icon
        ImageView myImageView = (ImageView) view.findViewById(R.id.down_arrow);
        Animation myFadeInAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.blinking_animation);
        myImageView.startAnimation(myFadeInAnimation);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.streamItemRecycler);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mLoadingProgress = (RelativeLayout) view.findViewById(R.id.stream_loading_progress);
        mProgress = (ProgressBar) view.findViewById(R.id.stream_progress);

        refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_layout);
        refreshLayout.setColorSchemeColors(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW);
        // pull to refresh method
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // load available streams
                // Initializing Internet Check
                if (NetworkChecking.hasConnection(getActivity())) {
                    // hide no internet image if showing
                        if (mNoInternet.isShown()) {
                            mNoInternet.setVisibility(View.GONE);
                        }
                        // auto load available streams
                        GetAllStreams();

                } else {
                    // if there is no internet
                    // stop pull to refresh
                    refreshLayout.setRefreshing(false);
                    // if there is no internet
                    if (mNoStreams.isShown()) {
                        mNoStreams.setVisibility(View.GONE);
                    }
                    if (!mNoInternet.isShown()) {
                        mNoInternet.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        // Select item on list
        ItemClickSupport.addTo(mRecyclerView).setOnItemClickListener(
                new ItemClickSupport.OnItemClickListener() {
                    @Override
                    public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                        Streams data = streams.get(position);
                        mStreamName = data.getStreamName();
                        Log.d(TAG + "Stream Name:", mStreamName);
                        // Initializing Internet Check
                        if (NetworkChecking.hasConnection(getActivity())) {
                            // go to check the stream status
                            getStreamStatus(mStreamName);
                        } else {
                            // if there is no internet
                            toast(getString(R.string.no_internet_connection));
                        }
                    }
                }
        );

        // recycler view scrolling behavior listener
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (IsRecyclerViewAtTop()) {
                    mPullToRefreshLayout.setVisibility(View.VISIBLE);
                    pull_to_refresh_message.setVisibility(View.VISIBLE);
                    down_arrow.setVisibility(View.VISIBLE);
                } else {
                    mPullToRefreshLayout.setVisibility(View.GONE);
                    pull_to_refresh_message.setVisibility(View.GONE);
                    down_arrow.setVisibility(View.GONE);

                }
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_IDLE:
                        live_fab.show();
                        break;
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

                if (dy < 0) {
                    // Recycle view scrolling up...
                    live_fab.hide();
                } else if (dy > 0) {
                    // Recycle view scrolling down...
                    live_fab.hide();
                } else {
                    live_fab.show();
                }
            }
        });
    }

    // common toast method
    private void toast(String text) {
        Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
    }

    // if user is on the top of the list
    private boolean IsRecyclerViewAtTop() {
        if (mRecyclerView.getChildCount() == 0)
            return true;
        return mRecyclerView.getChildAt(0).getTop() == 0;
    }

    // Go to Play stream page with stream name info
    public void PlayStream(String streamName) {
            Intent intent = new Intent(getActivity(), PlayStream.class);
            intent.putExtra("stream_name", streamName);
            getActivity().startActivity(intent);
            // transaction animation
            getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
    }

    // method to check stream status
    public void getStreamStatus(final String streamName) {
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                Const.JSON_URL_FOR_A_STREAMS + streamName + "?accessToken=" + STREAMING_SERVER_ACCESS_TOKEN, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        //Log.d(TAG, response.toString());
                        try {
                            ResponseStatus = response.getString("status");
                            ResponseCode = response.getInt("code");
                            Log.d(TAG, "Status: " + ResponseStatus + "\nCode: " + ResponseCode);
                            if (ResponseCode == 200) {
                                wholeData = response.getString("data");
                                JSONObject dataobj = new JSONObject(wholeData);
                                activeUser = dataobj.getString("active_subscribers");
                                // add myself (+1) as a active user
                                ActualActiveUser = Integer.parseInt(activeUser) + 1;
                                visitedTimes = dataobj.getString("total_subscribers");
                                isRecording = dataobj.getString("is_recording");
                                createdAt = dataobj.getString("creation_time");
                                // convert mil second to data format
                                String CreatedAt = convertDate(createdAt, "dd/MM/yyyy hh:mm:ss");
                                CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                                        Long.parseLong(createdAt),
                                        System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS);
                                Log.d(TAG, "Visited " + visitedTimes + " Times\nActive User: " + ActualActiveUser + "\nIs Recording: " + isRecording + "\nCreated At: " + CreatedAt + "\nStarted " + timeAgo + " ago");
                                if (isRecording.equals("true")) {
                                    // if it's recording means on air then go play stream page
                                    PlayStream(streamName);
                                } else {
                                    Toast.makeText(getActivity(), getString(R.string.stream_is_not_alive), Toast.LENGTH_LONG).show();
                                }

                            } else {
                                Toast.makeText(getActivity(), getString(R.string.stream_is_not_alive), Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, "Error: " + error.getMessage());
                Toast.makeText(getActivity(), getString(R.string.stream_is_not_alive), Toast.LENGTH_LONG).show();
            }
        });

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(jsonObjReq,
                tag_json_obj);
    }

    // method to convert mil seconds to date format
    public static String convertDate(String dateInMilliseconds, String dateFormat) {
        return DateFormat.format(dateFormat, Long.parseLong(dateInMilliseconds)).toString();
    }

    // method to get all available live streams from server
    public void GetAllStreams() {
        if (mNoStreams.isShown()) {
            mNoStreams.setVisibility(View.GONE);
        }
        // showing progress
        mLoadingProgress.setVisibility(View.GONE);   // was visible , now changed for HIDE STREAM LIST
        // clear current array list to remove duplicity
        streams.clear();
        // json request
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                Const.JSON_URL_FOR_ALL_STREAMS + STREAMING_SERVER_ACCESS_TOKEN, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        //Log.d(TAG, response.toString());
                        try {
                            JSONArray arrJson = response.getJSONArray("data");
                            String[] arr = new String[arrJson.length()];
                            // Check the array size for JSON response
                            // if there is no stream
                            if (arr.length == 0) {
                                // progress invisible
                                mLoadingProgress.setVisibility(View.GONE);
                                // show no stream layout
                                mNoStreams.setVisibility(View.GONE);  // was visible , now changed for HIDE STREAM LIST
                                //Toast.makeText(getActivity(), "No Available Streams!", Toast.LENGTH_LONG).show();
                            } else if (arr.length == 1) {
                                for (int i = 0; i < arrJson.length(); i++) {
                                    arr[i] = arrJson.getString(i);
                                    Log.d(TAG + " Stream Name: ", arr[i]);
                                    String StreamName = arr[i];
                                    if (StreamName.equals(PrefManager.getUserRecentStream(getActivity()))) {
                                        // progress invisible
                                        mLoadingProgress.setVisibility(View.GONE);
                                        // show no stream layout
                                        mNoStreams.setVisibility(View.GONE);    // was visible , now changed for HIDE STREAM LIST
                                        //Toast.makeText(getActivity(), "No Available Streams!", Toast.LENGTH_LONG).show();
                                    } else {
                                        // insert array list
                                        Streams streamItem = new Streams();
                                        streamItem.setStreamName(StreamName);
                                        streams.add(streamItem);
                                        // progress invisible
                                        mLoadingProgress.setVisibility(View.GONE);
                                        // hide no stream layout
                                        if (mNoStreams.isShown()) {
                                            mNoStreams.setVisibility(View.GONE);
                                        }
                                        // set the recycler view to inflate the list
                                        mRecyclerView.setLayoutManager(mLinearLayoutManager);
                                        adapter = new StreamsAdapter(getActivity(), streams);
                                        mRecyclerView.setAdapter(adapter);
                                    }
                                }
                            }
                            // more than one stream
                            else {
                                for (int i = 0; i < arrJson.length(); i++) {
                                    arr[i] = arrJson.getString(i);
                                    Log.d(TAG + " Stream Name: ", arr[i]);
                                    String StreamName = arr[i];
                                    // insert array list
                                    Streams streamItem = new Streams();
                                    streamItem.setStreamName(StreamName);
                                    streams.add(streamItem);
                                }
                                // progress invisible
                                mLoadingProgress.setVisibility(View.GONE);
                                // hide no stream layout
                                if (mNoStreams.isShown()) {
                                    mNoStreams.setVisibility(View.GONE);
                                }
                                // set the recycler view to inflate the list
                                mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                                adapter = new StreamsAdapter(getActivity(), streams);
                                mRecyclerView.setAdapter(adapter);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, "Error: " + error.getMessage());
                mLoadingProgress.setVisibility(View.GONE);
            }
        });

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(jsonObjReq,
                tag_json_obj);
        // stop pull to refresh
        refreshLayout.setRefreshing(false);
    }

    // Check the permission for marshmallow
    private void TakePermission() {
        // First checking the current device's OS version
        // If it is M= Marshmallow or higher then execute the following code
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {

            //First checking if the app is already having the permission
            if (checkSelfPermission(getActivity(), permissionsRequired[0]) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(getActivity(), permissionsRequired[1]) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(getActivity(), permissionsRequired[2]) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(getActivity(), permissionsRequired[3]) != PackageManager.PERMISSION_GRANTED) {

                // check should show request for permission rationale(one by one)
                if (shouldShowRequestPermissionRationale(permissionsRequired[0])
                        || shouldShowRequestPermissionRationale(permissionsRequired[1])
                        || shouldShowRequestPermissionRationale(permissionsRequired[2])
                        || shouldShowRequestPermissionRationale(permissionsRequired[3])) {

                    //Show Information about why you need the permission
                    mPermissionDialog = new SweetAlertDialog(getActivity(), SweetAlertDialog.WARNING_TYPE);
                    mPermissionDialog
                            .setTitleText(getString(R.string.permission_dialog_title))
                            .setContentText(getString(R.string.permission_dialog_message))
                            .setConfirmText(getString(R.string.permission_grant))
                            .setCancelText(getString(R.string.cancel))
                            .showCancelButton(true)
                            .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                @Override
                                public void onClick(SweetAlertDialog sDialog) {
                                    sDialog.cancel();
                                }
                            })
                            .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                @Override
                                public void onClick(SweetAlertDialog sDialog) {
                                    sDialog.cancel();
                                    requestPermissions(permissionsRequired, PERMISSION_CALLBACK_CONSTANT);
                                }
                            })
                            .show();
                    mPermissionDialog.setCancelable(false);
                    mPermissionDialog.setCanceledOnTouchOutside(false);
                } else if (permissionStatus.getBoolean(permissionsRequired[0], false)) {
                    //Previously Permission Request was cancelled with 'Dont Ask Again',
                    // Redirect to Settings after showing Information about why you need the permission

                    mPermissionDialog = new SweetAlertDialog(getActivity(), SweetAlertDialog.WARNING_TYPE);
                    mPermissionDialog
                            .setTitleText(getString(R.string.permission_dialog_title))
                            .setContentText(getString(R.string.permission_dialog_message))
                            .setConfirmText(getString(R.string.permission_grant))
                            .setCancelText(getString(R.string.cancel))
                            .showCancelButton(true)
                            .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                @Override
                                public void onClick(SweetAlertDialog sDialog) {
                                    sDialog.cancel();
                                }
                            })
                            .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                @Override
                                public void onClick(SweetAlertDialog sDialog) {
                                    sDialog.cancel();
                                    sentToSettings = true;
                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                                    intent.setData(uri);
                                    getActivity().startActivityForResult(intent, REQUEST_PERMISSION_SETTING);
                                    Toast.makeText(getActivity(), getActivity().getString(R.string.permission_from_settings), Toast.LENGTH_LONG).show();
                                }
                            })
                            .show();
                    mPermissionDialog.setCancelable(false);
                    mPermissionDialog.setCanceledOnTouchOutside(false);
                } else {
                    //just request the permission
                    requestPermissions(permissionsRequired, PERMISSION_CALLBACK_CONSTANT);
                }

                SharedPreferences.Editor editor = permissionStatus.edit();
                editor.putBoolean(permissionsRequired[0], true);
                editor.commit();
            } else {
                //You already have the permission, just go ahead.
                GoToLive();
            }
        }
        // If the current device is lower than Marshmallow
        else {
            // Go for Live broadcasting
            GoToLive();
        }
    }

    // method to go live broadcast page
    public void GoToLive() {
        startActivity(new Intent(getActivity(), PublishActivity.class));
        // transaction animation
        getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CALLBACK_CONSTANT) {
            //check if all permissions are granted
            boolean allgranted = false;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    allgranted = true;
                } else {
                    allgranted = false;
                    break;
                }
            }

            if (allgranted) {
                GoToLive();
            } else if (shouldShowRequestPermissionRationale(permissionsRequired[0])
                    || shouldShowRequestPermissionRationale(permissionsRequired[1])
                    || shouldShowRequestPermissionRationale(permissionsRequired[2])
                    || shouldShowRequestPermissionRationale(permissionsRequired[3])) {

                mPermissionDialog = new SweetAlertDialog(getActivity(), SweetAlertDialog.WARNING_TYPE);
                mPermissionDialog
                        .setTitleText(getString(R.string.permission_dialog_title))
                        .setContentText(getString(R.string.permission_dialog_message))
                        .setConfirmText(getString(R.string.permission_grant))
                        .setCancelText(getString(R.string.cancel))
                        .showCancelButton(true)
                        .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sDialog) {
                                sDialog.cancel();
                            }
                        })
                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sDialog) {
                                sDialog.cancel();
                                requestPermissions(permissionsRequired, PERMISSION_CALLBACK_CONSTANT);
                            }
                        })
                        .show();
                mPermissionDialog.setCancelable(false);
                mPermissionDialog.setCanceledOnTouchOutside(false);
            } else {
                Toast.makeText(getActivity(), getActivity().getString(R.string.permission_dialog_error_message), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PERMISSION_SETTING) {
            if (checkSelfPermission(getActivity(), permissionsRequired[0]) == PackageManager.PERMISSION_GRANTED) {
                //Got Permission
                GoToLive();
            }
        }
    }
}
