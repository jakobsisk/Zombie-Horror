import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerListenerThread extends Thread {
	private final ClientWindow owner;
	private final Socket socket;
	private final BufferedReader from_server;
	private final PrintWriter to_server;
	private boolean quit = false;

	public ServerListenerThread(ClientWindow owner, Socket socket, BufferedReader from_server, PrintWriter to_server) {
		this.owner = owner;
		this.socket = socket;
		this.from_server = from_server;
		this.to_server = to_server;
	}

	// public ClientListenerThread(MainServerThread owner, 

	public void run() {
		while (! quit) {
			String line_from_server;
			try {
				line_from_server = from_server.readLine();
				if (line_from_server == null) {
					System.out.println("Fick inga data från servern");
					quit = true;
				}
				else {
					System.out.println("Från servern: " + line_from_server);
					owner.received_line(line_from_server);
				}
			}
			catch (IOException e) {
				System.out.println("Fel vid mottagning från servern");
				quit = true;
			}
		} // while
		close_everything();
		owner.server_disconnected();
	} // run

	public void please_quit() {
		quit = true;
	}
	
	public void send_line(String line) {
		to_server.println(line);
	}

	public void close_everything() {
		to_server.close();
	    try {
			from_server.close();
		}
	    catch (IOException e) {
			// Just ignore that we couldn't close the BufferedReader
		}
	    try {
			socket.close();
		}
	    catch (IOException e) {
	    	// Just ignore that we couldn't close the socket
	    }
		owner.server_disconnected();
	}

} // class ServerListenerThread
