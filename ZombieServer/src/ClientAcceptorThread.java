import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientAcceptorThread extends Thread {
	private SimpleLogger logger = new SimpleLogger("ClientConnectionListenerThread");
	private final MainServerThread owner;
	private final int port;
	private boolean quit = false;
	
	public ClientAcceptorThread(MainServerThread owner, int port) {
		this.owner = owner;
		this.port = port;
	}

	public void please_quit() {
		quit = true;
	}
	
	public void run() {
		logger.log("Creating the server socket...");
		ServerSocket server_socket = null;
		try {
			server_socket = new ServerSocket(port);
		    logger.log("Server socket: " + server_socket);
		}
		catch (IOException e) {
			// Couldn't create the server socket
			owner.please_quit();
			quit = true;
		}

		while (!quit) {
			logger.log("Waiting for a client to connect...");
			Socket socket = null;
			try {
				// Blocks until a connection occurs:
				socket = server_socket.accept();
	        	logger.log("A client has connected.");
				// ClientListenerThread listener = new ClientListenerThread(owner, socket);
	        	new ClientListenerThread(owner, socket);
			} 
			catch (IOException e) {
				logger.log("server_socket.accept() or constructor failed, e = " + e);
				if (socket != null) {
					try {
						socket.close();
					}
					catch (IOException e1) {
						// Just ignore this error
					}
				}	
			}
        } // while
	} // run
} // class ClientConnectionListenerThread
