package com.oru.jakobsisk.oru_5;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by jakobsisk on 2017-10-25.
 */

public class ServerConn {

    // TODO: 2017-10-26 Change from Class to Service

    private LoginActivity mLoginActivity;
    private MapsActivity mMapsActivity;

    // Server
    private final static int SERVER_PORT = 2002;
    public final static String SERVER_ADDR = "basen.oru.se";
    //private final static String SERVER_ADDR = "localhost";
    private final static String SERVER_VERSION = "0.3";
    private final static int SERVER_QUEUE_START = 5500;

    private InetAddress mHost;
    private Socket mSocket;
    private PrintWriter mLineSender;
    private ServerListenerThread mServerListenerThread;
    private TreeMap<Integer, Command> mCommandQueue = new TreeMap<>();
    // Used if client expects multiple lines of response from server
    private int mResponseTotal;
    private int mResponseCount;

    public ServerConn(LoginActivity loginActivity) {
        this.mLoginActivity = loginActivity;

        Initialize init = new Initialize();
        init.execute();
    }

    public ServerConn(MapsActivity mapsActivity) {
        this.mMapsActivity = mapsActivity;

        Initialize init = new Initialize();
        init.execute();
    }

    public void handleReceivedLine(String line) {
        Response response = new Response(line);

        // If server response has no number
        if (response.getQueueNr() == 0) {
            switch (response.getType()) {
                case "ASYNC":
                    if (mMapsActivity != null) {
                        switch (response.getParams()[0]) {
                            case "YOU-ARE":
                                mMapsActivity.setStatus(response.getParams()[1]);

                                break;
                            case "PLAYER":
                                String name = response.getParams()[1];
                                switch (response.getParams()[2]) {
                                    case "GONE":
                                        mMapsActivity.removePlayer(name);

                                        break;
                                    case "ZOMBIE":case "HUMAN":
                                        String status = response.getParams()[2];
                                        String lat = response.getParams()[3];
                                        String lng = response.getParams()[4];

                                        mMapsActivity.createPlayer(name, status, lat, lng);

                                        break;
                                }

                                break;
                        }
                    }
                    
                    break;
                case "ERROR":
                    handleError(response.getParams()[0]);

                    break;
            }
        }
        else {
            // Save the corresponding command if it exists
            Command command = mCommandQueue.get(response.getQueueNr());

            // Check if response matches a sent command
            if (command != null) {
                Log.d("log", "Found original command.");
                response.setCommand(command);

                if (mLoginActivity != null) {
                    switch(response.getType()) {
                        case "REGISTERED": case "WELCOME":
                            mLoginActivity.loginSuccess(command.getParams());

                            mCommandQueue.remove(response.getQueueNr());

                            break;
                        case "GOODBYE":
                            mCommandQueue.remove(response.getQueueNr());

                            break;
                        case "ERROR": default :
                            handleError(response.getParams()[0]);

                            mCommandQueue.remove(response.getQueueNr());

                            break;
                    }
                }
                else if (mMapsActivity != null) {
                    switch(response.getType()) {
                        case "WELCOME":
                            mMapsActivity.getStatus();

                            mCommandQueue.remove(response.getQueueNr());

                            break;
                        case "YOU-ARE":
                            mMapsActivity.setStatus(response.getParams()[0]);

                            mCommandQueue.remove(response.getQueueNr());

                            break;
                        case "VISIBLE-PLAYERS":
                            mResponseTotal = Integer.parseInt(response.getParams()[1]);
                            mResponseCount = 0;

                            break;
                        case "PLAYER":
                            mResponseCount++;

                            String name = response.getParams()[0];
                            String status = response.getParams()[1];
                            String lat = response.getParams()[2];
                            String lng = response.getParams()[3];
                            mMapsActivity.createPlayer(name, status, lat, lng);

                            if (mResponseCount == mResponseTotal) {

                                mCommandQueue.remove(response.getQueueNr());
                            }

                            break;
                        case "OK":


                            break;
                        case "GOODBYE":
                            mMapsActivity.logoutSuccess();

                            mCommandQueue.remove(response.getQueueNr());

                            break;
                        case "ERROR":default:
                            handleError(response.getParams()[0]);

                            mCommandQueue.remove(response.getQueueNr());
                    }
                }
            }
            else {
                String errorMsg = "client_no_origin";
                handleError(errorMsg);
            }
        }
    }

    public int prepCommand(String type, String[] params) {
        int queueNr = SERVER_QUEUE_START;

        if (!mCommandQueue.isEmpty()) {
            queueNr += mCommandQueue.lastKey();
        }

        Command command = new Command(queueNr, type, params);
        mCommandQueue.put(queueNr, command);

        return queueNr;
    }

    public void sendCommand (int queueNr) {
        mCommandQueue.get(queueNr).send();
    }

    public void handleError(String errorMsg) {
        Log.d("log", "Error: " + errorMsg);

        if (mLoginActivity != null) {
            mLoginActivity.handleError(errorMsg);
        }
        else if (mMapsActivity != null) {
            mMapsActivity.handleError(errorMsg);
        }
    }


    // <<<--- CLASSES --->>> //

    public class Initialize extends AsyncTask<Void, Void, Boolean> {

        private String errorMsg;

        @Override
        protected Boolean doInBackground(Void... v) {
            Boolean success = false;

            try {
                mHost = InetAddress.getByName(SERVER_ADDR);
                mSocket = new Socket(mHost, SERVER_PORT);

                mServerListenerThread = new ServerListenerThread(ServerConn.this, mSocket);
                mServerListenerThread.start();
                mLineSender = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream())), true);

                success = true;
            }
            catch (IOException e) {
                Log.d("log", "Exception: " + e.getMessage());
                errorMsg = "client_socket";
                success = false;
            }

            return success;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (!success) {
                handleError(errorMsg);
            }
            else {
                Log.d("log", "Connected to server");
            }
        }
    }

    public class Command {

        private final Map<String, Config> configs = new HashMap<String, Config>() {{
            put("login",                new Config(1, "LOGIN"));
            put("logout",               new Config(1, "LOGOUT"));
            put("reg",                  new Config(1, "REGISTER"));

            put("getLocation",          new Config(1, "WHERE-AM-I"));
            put("setLocation",          new Config(1, "I-AM-AT"));
            put("getStatus",            new Config(1, "WHAT-AM-I"));
            put("setStatus",            new Config(1, "TURN"));
            put("getVisiblePlayers",    new Config(1, "LIST-VISIBLE-PLAYERS"));
            put("setVisibility",        new Config(1, "SET-VISIBILITY"));
        }};

        private int queueNr;
        private Config config;
        private String[] params;

        public int getQueueNr() { return queueNr; }
        public String[] getParams() { return params; }

        public Command(int queueNr, String type, String[] params) {
            this.queueNr = queueNr;
            this.config = configs.get(type);
            this.params = params;
        }

        public String generateLine() {
            String line = Integer.toString(queueNr) + " " + config.getSyntax();

            if (params != null) {
                for (String param : params) {
                    line += " " + param;
                }
            }

            return line;
        }

        public void send() {
            SendCommand sendCommand = new SendCommand();
            sendCommand.execute(generateLine());
        }

        private class Config {
            private int priority;
            private String syntax;

            // Getters
            public int getPriority() { return priority; }
            public String getSyntax() { return syntax; }

            public Config(int priority, String syntax) {
                this.priority = priority;
                this.syntax = syntax;
            }
        }

        public class SendCommand extends AsyncTask<String, Void, Boolean> {

            private String errorMsg;

            @Override
            protected Boolean doInBackground(String... params) {
                Boolean success = false;
                String line = params[0];

                try {
                    Log.d("log", "Line to server:");
                    Log.d("log", "  " + line);

                    mLineSender.println(line);

                    success = true;
                }
                catch (Exception e) {
                    errorMsg = e.getMessage();
                }

                return success;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (!success) {
                    handleError(errorMsg);
                }
            }
        }
    }

    public class Response {

        private String line;

        private int queueNr;
        private String type;
        private String[] params;

        private Command command;

        public int getQueueNr() { return queueNr; }
        public String getType() { return type; }
        public String[] getParams() { return params; }
        public Command getCommand() { return command; }
        public void setCommand(Command command) { this.command = command; }

        public Response(String line) {
            this.line = line;
            Log.d("log", "Line from server received:");
            Log.d("log", "  " + line);


            parseLine();
        }

        private void parseLine() {
            // If first word in line is digits i.e. it is an answer to a command
            if (android.text.TextUtils.isDigitsOnly(line.split(" ")[0])) {
                queueNr = Integer.parseInt(line.split(" ")[0]);
                type = line.split(" ")[1];
                params = Arrays.copyOfRange(line.split(" "), 2, line.split(" ").length);
            }
            else {
                type = line.split(" ")[0];
                params = Arrays.copyOfRange(line.split(" "), 1, line.split(" ").length);
            }
        }
    }
}
