package com.oru.jakobsisk.oru_5;

import android.support.v7.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Created by jakobsisk on 2017-10-26.
 */

public class ServerListenerThread extends Thread {

    // TODO: 2017-10-26 Find a better way to handle returning data to several activities. Service maybe?
    private ServerConn owner;

    private Boolean mQuit;
    private Socket mSocket;
    private BufferedReader mLineReceiver;

    public ServerListenerThread(ServerConn owner, Socket socket) {
        mQuit = false;

        this.owner = owner;
        this.mSocket = socket;

        try {
            mLineReceiver = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
        }
        catch (IOException e) {
            mQuit = true;
        }
    }

    public void run() {
        while (!mQuit) {
            String line = "";
            try {
                line = mLineReceiver.readLine();
                if (line == null) {
                    // TODO: 2017-10-26 Handle null (null means disconnected)
                    mQuit = true;
                }
            }
            catch (IOException e) {
                mQuit = true;
            }
            owner.handleReceivedLine(line);
        }
    }
}
