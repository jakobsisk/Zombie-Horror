package com.oru.jakobsisk.oru_5;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by jakobsisk on 2017-10-25.
 */

public class ServerConn {

    // TODO: 2017-10-26 Change from Class to Service

    private LoginActivity loginActivity;
    private MapsActivity mapsActivity;

    // Server
    private final static int SERVER_PORT = 2002;
    public final static String SERVER_ADDR = "basen.oru.se";
    //private final static String SERVER_ADDR = "localhost";
    private final static String SERVER_VERSION = "0.3";
    private final static int SERVER_QUEUE_START = 5500;

    private InetAddress host;
    private Socket socket;
    private PrintWriter lineSender;
    private String status;
    private ServerListenerThread serverListenerThread;
    private TreeMap<Integer, Command> commandQueue = new TreeMap<>();

    // Getters
    public String getStatus() { return status; }

    public ServerConn(LoginActivity loginActivity) {
        this.loginActivity = loginActivity;

        Initialize init = new Initialize();
        init.execute();
    }

    public ServerConn(MapsActivity mapsActivity) {
        this.mapsActivity = mapsActivity;

        Initialize init = new Initialize();
        init.execute();
    }

    public void handleReceivedLine(String line) {
        Log.d("log", "Server: " + line);
        Log.d("log", "Processing server response.");

        Response response = new Response(line);

        // If server response has no number
        if (response.getQueueNr() == 0) {
            switch (response.getType()) {
                case "ASYNC":
                    if (mapsActivity != null) {
                        // TODO: 2017-10-30 Handle async responses
                    }
                    
                    break;
                case "ERROR":
                    handleError(response.getParams()[0]);

                    break;
            }
        }
        else {
            // Save the corresponding command if it exists
            Command command = commandQueue.get(response.getQueueNr());

            // Check if response matches a sent command
            if (command != null) {
                Log.d("log", "Found original command.");
                response.setCommand(command);

                if (loginActivity != null) {
                    switch(response.getType()) {
                        case "REGISTERED": case "WELCOME":
                            loginActivity.loginSuccess(command.getParams());

                            break;
                        case "ERROR": default :
                            handleError(response.getParams()[0]);

                            break;
                    }
                }
                else if (mapsActivity != null) {
                    //mapsActivity.handleServerResponse(response);
                }

                // Remove command from queue after server has confirmed that it has been handled
                Log.d("log", "Removing original command.");
                commandQueue.remove(response.getQueueNr());
            }
            else {
                String errorMsg = "client_no_origin";
                handleError(errorMsg);
            }
        }
    }

    public int prepCommand(String type, String[] params) {
        int queueNr = SERVER_QUEUE_START;

        if (!commandQueue.isEmpty()) {
            queueNr += commandQueue.lastKey();
        }

        Command command = new Command(queueNr, type, params);
        commandQueue.put(queueNr, command);

        return queueNr;
    }

    public void sendCommand (int queueNr) {
        commandQueue.get(queueNr).send();
    }

    public void handleError(String errorMsg) {
        Log.d("log", "Error: " + errorMsg);

        if (loginActivity != null) {
            loginActivity.handleError(errorMsg);
        }
        else if (mapsActivity != null) {
            mapsActivity.handleError(errorMsg);
        }
    }


    // <<<--- CLASSES --->>> //

    public class Initialize extends AsyncTask<Void, Void, Boolean> {

        private String errorMsg;

        @Override
        protected Boolean doInBackground(Void... v) {
            Boolean success = false;

            try {
                host = InetAddress.getByName(SERVER_ADDR);
                socket = new Socket(host, SERVER_PORT);

                serverListenerThread = new ServerListenerThread(ServerConn.this, socket);
                serverListenerThread.start();
                lineSender = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                status = "connected";
                success = true;
            }
            catch (IOException e) {
                status = "error";
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

            put("getlocation",          new Config(1, "WHERE-AM-I"));
            put("setlocation",          new Config(1, "I-AM-AT"));
            put("getstatus",            new Config(1, "WHAT-AM-I"));
            put("setStatus",            new Config(1, "TURN"));
            put("getvisibleplayers",    new Config(1, "LIST-VISIBLE-PLAYERS"));
            put("setvisibility",        new Config(1, "SET-VISIBILITY"));
        }};

        private int queueNr;
        private Config config;
        private String[] params;

        public int getPriority() { return config.getPriority(); }
        public String[] getParams() { return params; }

        public Command(int queueNr, String type, String[] params) {
            Log.d("log", "New command:");
            Log.d("log", "  Type - : " + type);
            if (params != null) {
                String outp = "  Params - :";
                for (String param: params) {
                    outp += " " + param;
                }
                Log.d("log", outp);
            }

            this.queueNr = queueNr;
            this.config = configs.get(type);
            this.params = params;
        }

        public String generateLine() {
            Log.d("log", "  Generating lines.");

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
                    Log.d("log", "Me: " + line);

                    lineSender.println(line);

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
