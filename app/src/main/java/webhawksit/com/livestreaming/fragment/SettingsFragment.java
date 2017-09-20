package webhawksit.com.livestreaming.fragment;


import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import cn.pedant.SweetAlert.SweetAlertDialog;
import webhawksit.com.livestreaming.R;

public class SettingsFragment extends Fragment {

    //Defining Variables
    private static final String TAG = SettingsFragment.class.getSimpleName();
    TextInputLayout inputLayoutURL;
    EditText _urlText;
    Button _requestButton;
    Handler handler;

    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }


    public SettingsFragment() {
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
        return inflater.inflate(R.layout.settings, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        //view initialize and functionality declare
        inputLayoutURL = (TextInputLayout) view.findViewById(R.id.inputLayoutURL);
        _urlText = (EditText) view.findViewById(R.id.input_url);

        _requestButton = (Button) view.findViewById(R.id.btn_request);
        _requestButton.setOnClickListener(new View.OnClickListener() {
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

        //_requestButton.setEnabled(false);

        // Go to Main Page
        handler = new Handler();
        final SweetAlertDialog savedDialog = new SweetAlertDialog(getActivity(), SweetAlertDialog.SUCCESS_TYPE);
        savedDialog.setTitleText(getString(R.string.success_dialog_title))
                .setContentText(getString(R.string.reset_dialog_message))
                .setConfirmText(getString(R.string.success_dialog_button))
                .show();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 1500ms
                savedDialog.dismissWithAnimation();
            }
        }, 1500);
    }

    public void onRequestFailed() {
        toast(getString(R.string.request_failed_message));
        _requestButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String email = _urlText.getText().toString();

        if (email.isEmpty()) {
            _urlText.setError(getString(R.string.valid_server_address));
            valid = false;
        } else {
            _urlText.setError(null);
        }

        return valid;
    }

    private void toast(String text) {
        Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
    }

}
