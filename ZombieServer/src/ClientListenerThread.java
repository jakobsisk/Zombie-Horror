import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientListenerThread extends Thread {
	private static int instances = 0;
	private int number = ++instances;
	private final SimpleLogger logger = new SimpleLogger("ClientListenerThread " + number);
	private final MainServerThread owner;
	private final Socket socket;
	private final BufferedReader from_client;
	final PrintWriter to_client;
	private boolean quit = false;
	private Player associated_player = null;
	
	public ClientListenerThread(MainServerThread owner, Socket socket, BufferedReader from_client, PrintWriter to_client) {
		this.owner = owner;
		this.socket = socket;
		this.from_client = from_client;
		this.to_client = to_client;
	}
	
	public ClientListenerThread(MainServerThread owner, Socket socket) throws IOException {
		this.owner = owner;
		this.socket = socket;
		from_client = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		to_client = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
		// true: PrintWriter is line buffered
		owner.client_connected(this);
		this.send_line("ZombieServer " + ZombieServer.VERSION);
	}

	public void run() {
		while (!quit) {
			String line_from_client;
			try {
				line_from_client = from_client.readLine();
				logger.log("From client: " + line_from_client);
				if (line_from_client == null) {
					quit = true;
					owner.received_line(this, null); // null means disconnected
				}
				else
					owner.received_line(this, line_from_client);
			}
			catch (IOException e) {
				logger.log("IOException when reading from client, e = " + e);
				quit = true;
				owner.received_line(this, null); // null means disconnected
			}
		} // while
	    to_client.close();
	    try {
			from_client.close();
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
	    // logger.log("Gonna disc " + this.getName() + ", player = " + this.associated_player);
	    owner.client_disconnected(this, associated_player);
	} // run
	
	public void please_quit() {
		quit = true;
	}

	// Can be called both by this client listener thread, and by the main server thread
	synchronized public void send_line(String line) {
		to_client.println(line);		
	}

	public Player getPlayer() {
		return associated_player;
	}

	public void associate_to_player(Player player) {
		this.associated_player = player;
	}
} // class ClientListenerThread
