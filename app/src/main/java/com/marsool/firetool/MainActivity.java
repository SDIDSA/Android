package com.marsool.firetool;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.marsool.firetool.networking.ApiCall;
import com.marsool.firetool.networking.ConnectivityCheck;
import com.marsool.firetool.networking.Handler;
import com.marsool.firetool.networking.HttpResponse;
import com.marsool.firetool.networking.Param;
import com.marsool.firetool.ui.alerts.Alert;
import com.marsool.firetool.ui.alerts.AlertType;

import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {
    private SharedPrefManager spm;

    //Connectivity
    private View conRoot;
    private ProgressBar conProg;

    //login views
    private View content;
    private EditText editTextUsername, editTextPassword;
    private Button login;
    private ProgressBar buttonLoading;
    private TextView message;
    private int animDur = 300;
    //popup Views

    private boolean ignoreChange = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Settings.st = false;
        //initialize login fields
        conRoot = findViewById(R.id.connect);
        conProg = findViewById(R.id.connect_progress);

        //initialize login fields
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        login = findViewById(R.id.buttonLogin);
        buttonLoading = findViewById(R.id.button_loading);
        message = findViewById(R.id.message);
        content = findViewById(R.id.content);
        content.setClickable(false);
        View info = findViewById(R.id.info);
        info.setOnClickListener(e -> {
            Alert inf = new Alert(this, AlertType.INFORMATION);
            inf.setTitle("About");
            inf.setMessage("FireTool© v" + BuildConfig.VERSION_NAME);
            inf.showAndWait(findViewById(R.id.root), f -> {
                //HIDE
            });
        });
        preparePhoneField();

        spm = SharedPrefManager.getInstance(this);
        //check whether the user is logged in
        if (spm.isLoggedIn()) {
            Intent intent = new Intent(MainActivity.this, Settings.class);
            startActivity(intent);
            finish();
        } else {
            login.setOnClickListener(e -> userLogin());
        }

        ConnectivityCheck connectivityCheck = new ConnectivityCheck(this,
                e -> runOnUiThread(() -> conProg.setProgress(e)),
                () -> runOnUiThread(this::hideConnect),
                () -> {
                    Intent intent = new Intent(MainActivity.this, NoInternet.class);
                    startActivity(intent);
                });
        connectivityCheck.execute();
    }

    //adding event listener on the phone number to emphasise the rules
    public void preparePhoneField() {
        editTextUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int s, int before, int count) {
                if (ignoreChange) {
                    return;
                }
                int caretPos = editTextUsername.getSelectionStart();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < charSequence.length(); i++) {
                    char c = charSequence.charAt(i);
                    if ((i == 0 && (Character.isDigit(c) || c == '+')) || (i > 0 && Character.isDigit(c))) {
                        sb.append(c);
                    }
                }
                ignoreChange = true;
                String res = sb.toString();
                editTextUsername.setText(res);
                editTextUsername.setSelection(Math.min(caretPos, res.length()));
                ignoreChange = false;
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void userLogin() {
        //first getting the values
        final String username = editTextUsername.getText().toString();
        final String password = editTextPassword.getText().toString();

        //validating inputs
        if (TextUtils.isEmpty(username)) {
            editTextUsername.setError("Please enter your phone number");
            editTextUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Please enter your password");
            editTextPassword.requestFocus();
            return;
        }

        //if everything is fine
        hideError();
        loading();

        ApiCall loginCall = new ApiCall(getString(R.string.api_base) + "login",
                new Handler() {
                    @Override
                    public void handleResponse(HttpResponse response) {
                        if (response.getCode() == 403) {
                            showError("You're already logged " + (spm.isLoggedIn() ? "here" : "somewhere else"));
                        } else if (response.getCode() == 422) {
                            showError("Incorrect phone and/or password");
                        } else if (response.getCode() == 200) {
                            spm.storeToken(response.getBody());
                            Intent intent = new Intent(MainActivity.this, Settings.class);
                            startActivity(intent);
                            finish();
                        }
                        loaded();
                    }

                    @Override
                    public void handleError(Exception x) {
                        runOnUiThread(() -> {
                            if (x instanceof UnknownHostException) {
                                Intent intent = new Intent(MainActivity.this, NoInternet.class);
                                startActivity(intent);
                            }
                        });
                    }
                },
                new Param("phone", username),
                new Param("password", password),
                new Param("device_name", "test"));
        loginCall.execute();
    }

    public void onBackPressed() {
        finish();
        startActivity(getIntent());
        super.onBackPressed();
    }

    public void hideConnect() {
        ObjectAnimator connectHide = ObjectAnimator.ofFloat(conRoot, "alpha", 0f);
        connectHide.setDuration(animDur);
        ObjectAnimator contentShow = ObjectAnimator.ofFloat(content, "alpha", 1f);
        contentShow.setDuration(animDur);
        conRoot.setClickable(false);
        content.setClickable(true);
        connectHide.start();
        contentShow.start();
    }

    public void showError(String error) {
        message.setText(error);
        ObjectAnimator show = ObjectAnimator.ofFloat(message, "alpha", 1f);
        show.setDuration(animDur);
        show.start();
    }

    public void hideError() {
        ObjectAnimator hide = ObjectAnimator.ofFloat(message, "alpha", 0f);
        hide.setDuration(animDur);
        hide.start();
    }

    public void loading() {
        ObjectAnimator button_hide = ObjectAnimator.ofFloat(login, "alpha", 0f);
        button_hide.setDuration(animDur);
        ObjectAnimator loading_show = ObjectAnimator.ofFloat(buttonLoading, "alpha", 1f);
        loading_show.setDuration(animDur);

        button_hide.start();
        loading_show.start();
    }

    public void loaded() {
        ObjectAnimator button_show = ObjectAnimator.ofFloat(login, "alpha", 1f);
        button_show.setDuration(animDur);
        ObjectAnimator loading_hide = ObjectAnimator.ofFloat(buttonLoading, "alpha", 0f);
        loading_hide.setDuration(animDur);

        button_show.start();
        loading_hide.start();
    }
}