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

    private Boolean quit;
    private Socket socket;
    private BufferedReader lineReceiver;

    public ServerListenerThread(ServerConn owner, Socket socket) {
        quit = false;

        this.owner = owner;
        this.socket = socket;

        try {
            lineReceiver = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
        catch (IOException e) {
            quit = true;
        }
    }

    public void run() {
        while (!quit) {
            String line = "";
            try {
                line = lineReceiver.readLine();
                if (line == null) {
                    // TODO: 2017-10-26 Handle null (null means disconnected)
                    quit = true;
                }
            }
            catch (IOException e) {
                quit = true;
            }
            owner.handleReceivedLine(line);
        }
    }
}
