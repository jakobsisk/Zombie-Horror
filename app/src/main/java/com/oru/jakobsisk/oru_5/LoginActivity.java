package com.oru.jakobsisk.oru_5;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

public class LoginActivity extends AppCompatActivity {

    private SharedPreferences mSharedPref;
    private ServerConn mServerConn;

    // UI
    private EditText mEditTxtName;
    private EditText mEditTxtPassword;
    private Button mBtnLogin;
    private Button mBtnReg;
    private TextView mTxtViewStatus;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mSharedPref = getSharedPreferences("prefs", Context.MODE_PRIVATE);

        Log.d("log", "Connecting to server.");
        mServerConn = new ServerConn(LoginActivity.this);

        // UI
        mEditTxtName = findViewById(R.id.username);
        mEditTxtPassword = findViewById(R.id.password);
        mBtnLogin = findViewById(R.id.login);
        mBtnReg = findViewById(R.id.register);
        mTxtViewStatus = findViewById(R.id.status);
        mProgressBar = findViewById(R.id.progressBar);

        // -- USER INTERACTION -- //

        // Login button
        mBtnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = mEditTxtName.getText().toString();
                String password = mEditTxtPassword.getText().toString();

                login(name, password);
            }
        });

        // Register button
        mBtnReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = mEditTxtName.getText().toString();
                String password = mEditTxtPassword.getText().toString();

                register(name, password);
            }
        });
    }

    private void login(String n, String p) {
        mTxtViewStatus.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
        mTxtViewStatus.setText("Logging in");

        String[] params = {n, p};
        int commandNr = mServerConn.prepCommand("login", params);
        mProgressBar.setProgress(25);
        mServerConn.sendCommand(commandNr);
        mProgressBar.setProgress(50);

        // Logout so that the maps activity can login
        // TODO: 2017-12-21 Improvement - Transfer socket object between activities
        params = new String[0];
        commandNr = mServerConn.prepCommand("logout", params);
        mProgressBar.setProgress(75);
        mServerConn.sendCommand(commandNr);
    }

    private void register(String n, String p) {
        final String name = n;
        final String password = p;

        if (name.isEmpty() || password.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setIcon(0)
                    .setTitle(getString(R.string.login_err_empty_fields_title))
                    .setMessage(getString(R.string.login_err_empty_fields_msg))
                    .setPositiveButton(getString(R.string.login_err_empty_fields_btn), null)
                    .show();
        }
        else {
            new AlertDialog.Builder(this)
                    .setIcon(0)
                    .setTitle(getString(R.string.login_reg_title))
                    .setMessage(getString(R.string.login_reg_msg))
                    .setPositiveButton(getString(R.string.login_reg_btn_pos), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String[] params = {name, password};
                            int commandNr = mServerConn.prepCommand("reg", params);
                            mTxtViewStatus.setVisibility(View.VISIBLE);
                            mProgressBar.setVisibility(View.VISIBLE);
                            mTxtViewStatus.setText("Registering");
                            mServerConn.sendCommand(commandNr);
                            mProgressBar.setProgress(100);
                        }
                    })
                    .setNegativeButton(getString(R.string.login_reg_btn_neg), null)
                    .show();
        }
    }

    public void loginSuccess(String[] commandParams) {
        Log.d("log", "Login succeeded. Loading maps activity.");

        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putString("name", commandParams[0]);
        editor.putString("password", commandParams[1]);
        editor.apply();

        mProgressBar.setProgress(100);

        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
    }

    public void handleError(String errorMsg) {
        switch (errorMsg) {
            // Client errors
            case "client_no_origin":
                break;
            case "client_socket":

                break;
            //Server errors
            case "THAT-PLAYER-ALREADY-LOGGED-IN":

                break;
        }
    }

    private void showError(String msg) {
        // TODO: 2017-11-01 Error UI
    }
}
