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

    private SharedPreferences sharedPref;
    private ServerConn serverConn;

    // UI
    private EditText editTxtName;
    private EditText editTxtPassword;
    private Button btnLogin;
    private Button btnReg;
    private TextView txtViewStatus;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sharedPref = getSharedPreferences("prefs", Context.MODE_PRIVATE);

        Log.d("log", "Connecting to server.");
        serverConn = new ServerConn(LoginActivity.this);

        // UI
        editTxtName = (EditText)findViewById(R.id.username);
        editTxtPassword = (EditText)findViewById(R.id.password);
        btnLogin = (Button)findViewById(R.id.login);
        btnReg = (Button)findViewById(R.id.register);
        txtViewStatus = (TextView)findViewById(R.id.status);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);

        // -- USER INTERACTION -- //

        // Login button
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = editTxtName.getText().toString();
                String password = editTxtPassword.getText().toString();

                txtViewStatus.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                txtViewStatus.setText("Logging in");

                String[] params = {name, password};
                int commandNr = serverConn.prepCommand("login", params);
                progressBar.setProgress(50);
                serverConn.sendCommand(commandNr);
                progressBar.setProgress(100);
            }
        });

        // Register button
        btnReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = editTxtName.getText().toString();
                String password = editTxtPassword.getText().toString();

                txtViewStatus.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                txtViewStatus.setText("Registering");

                register(name, password);
            }
        });
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
                            int commandNr = serverConn.prepCommand("reg", params);
                            progressBar.setProgress(50);
                            serverConn.sendCommand(commandNr);
                            progressBar.setProgress(100);
                        }
                    })
                    .setNegativeButton(getString(R.string.login_reg_btn_neg), null)
                    .show();
        }
    }

    public void loginSuccess(String[] commandParams) {
        progressBar.setProgress(100);

        Log.d("log", "Login succeeded. Loading maps activity.");

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("name", commandParams[0]);
        editor.putString("password", commandParams[1]);
        editor.apply();

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
