import java.awt.Container;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ClientWindow extends JFrame implements ActionListener {

	public ClientWindow() {
		super("Zombie Desktop Client " + ZombieDesktopClient.VERSION);
		fill_frame();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
	}

	private JTextField connection_status_field= new JTextField("Disconnected", 10);
	private JTextField login_status_field = new JTextField("Not logged in", 10);
	private JTextField name_status_field = new JTextField("UNKNOWN", 10);
	private JTextField zombie_status_field = new JTextField("UNKNOWN", 10);
	private JTextField server_field = new JTextField(ZombieDesktopClient.DEFAULT_SERVER, 20);
	private JTextField port_field = new JTextField("" + ZombieDesktopClient.DEFAULT_PORT, 10);
	private JButton connect_button = new JButton("Connect");
	private JButton disconnect_button = new JButton("Disconnect");
	private JButton quit_button = new JButton("Quit");
	private JTextField name_field = new JTextField("tompa", 10);
	private JTextField password_field = new JTextField("apa", 10);
	private JButton login_button = new JButton("Login");
	private JButton logout_button = new JButton("Logout");
	private JButton register_button = new JButton("Register");
	private JTextField latitude_field = new JTextField("59.25403118133545", 15);
	private JTextField longitude_field = new JTextField("15.247076153755188", 15);
	private JButton send_location_button = new JButton("Send Location");
	private JTextField visibility_field = new JTextField("1000", 10);
	private JButton send_visibility_button = new JButton("Send Limit");
	private JButton zombie_button = new JButton("Zombie!");
	private JButton human_button = new JButton("Human!");
	private JButton show_map_button = new JButton("Show map");
	private JButton show_log_button = new JButton("Show com log");
	private MapWindow map_window = null;
	private LogWindow log_window = null;
	
	private ServerListenerThread server_listener = null;
	
	String latest_attempted_login_name = null;
	String actual_login_name = null;
	
	private void fill_frame() {
		Container pane = getContentPane();
		// pane.setLayout(new GridBagLayout());
    	pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));

    	connection_status_field.setEditable(false);
    	login_status_field.setEditable(false);
    	name_status_field.setEditable(false);
    	zombie_status_field.setEditable(false);
    	
    	JPanel status_panel = new JPanel();
    	status_panel.setLayout(new BoxLayout(status_panel, BoxLayout.LINE_AXIS));
    	status_panel.add(new JLabel("Connection:"));
    	status_panel.add(connection_status_field);
    	status_panel.add(new JLabel("Login:"));
    	status_panel.add(login_status_field);
    	status_panel.add(new JLabel("Name:"));
    	status_panel.add(name_status_field);
    	status_panel.add(new JLabel("Status:"));
    	status_panel.add(zombie_status_field);
    	
    	pane.add(status_panel);

    	JPanel server_panel = new JPanel();
    	server_panel.add(new JLabel("Server:"));
    	server_panel.add(server_field);
    	server_panel.add(new JLabel("Port:"));
    	server_panel.add(port_field);
    	server_panel.add(connect_button);
    	server_panel.add(disconnect_button);
    	server_panel.add(quit_button);

    	connect_button.addActionListener(this);
    	disconnect_button.addActionListener(this);
    	quit_button.addActionListener(this);

    	pane.add(server_panel);
    	
    	JPanel input_fields_panel = new JPanel();
    	//Lay out the buttons from left to right.
    	input_fields_panel.setLayout(new BoxLayout(input_fields_panel, BoxLayout.LINE_AXIS));
    	input_fields_panel.add(new JLabel("Name:"));
    	input_fields_panel.add(name_field);
    	input_fields_panel.add(new JLabel("Password:"));
    	input_fields_panel.add(password_field);
    	
    	pane.add(input_fields_panel);
    	
    	JPanel buttons_panel = new JPanel();
    	//Lay out the buttons from left to right.
    	buttons_panel.setLayout(new BoxLayout(buttons_panel, BoxLayout.LINE_AXIS));
    	buttons_panel.add(login_button);
    	buttons_panel.add(logout_button);
    	buttons_panel.add(register_button);
    	
    	login_button.addActionListener(this);
    	logout_button.addActionListener(this);
    	register_button.addActionListener(this);
    	
    	pane.add(buttons_panel);
    	
    	JPanel location_panel = new JPanel();
    	//Lay out the buttons from left to right.
    	location_panel.setLayout(new BoxLayout(location_panel, BoxLayout.LINE_AXIS));
    	location_panel.add(new JLabel("Lat:"));
    	location_panel.add(latitude_field);
    	location_panel.add(new JLabel("Long:"));
    	location_panel.add(longitude_field);
    	location_panel.add(send_location_button);

    	send_location_button.addActionListener(this);
    	
    	pane.add(location_panel);

    	JPanel visibility_panel = new JPanel();
    	//Lay out the buttons from left to right.
    	visibility_panel.setLayout(new BoxLayout(visibility_panel, BoxLayout.LINE_AXIS));
    	visibility_panel.add(new JLabel("Visibility limit:"));
    	visibility_panel.add(visibility_field);
    	visibility_panel.add(send_visibility_button);

    	send_visibility_button.addActionListener(this);
    	
    	pane.add(visibility_panel);
    	
    	JPanel lower_button_panel = new JPanel();
    	//Lay out the buttons from left to right.
    	lower_button_panel.setLayout(new BoxLayout(lower_button_panel, BoxLayout.LINE_AXIS));
    	lower_button_panel.add(zombie_button);
    	lower_button_panel.add(human_button);
    	lower_button_panel.add(show_map_button);
    	lower_button_panel.add(show_log_button);

    	zombie_button.addActionListener(this);
    	human_button.addActionListener(this);
    	show_map_button.addActionListener(this);
    	show_log_button.addActionListener(this);
    	
    	pane.add(lower_button_panel);

    	pack();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == connect_button) {
			if (server_listener != null) {
				JOptionPane.showMessageDialog(this, "Already connected.");
			}
			else {
				String server_name = server_field.getText().trim();
				String port = port_field.getText().trim();
				if (!is_single_word(server_name))
					JOptionPane.showMessageDialog(this, "The server name must be a single word.");
				else if (!is_integer_value(port))
					JOptionPane.showMessageDialog(this, "The port number must be a single integer.");
				else
					connect(server_name, Integer.parseInt(port));
			}
		}
		else if (e.getSource() == disconnect_button) {
			if (server_listener == null) {
				JOptionPane.showMessageDialog(this, "Not connected.");
			}
			else {
				server_listener.please_quit(); // This doesn't work, does it??
				server_listener.close_everything();
				setConnected(false);
				server_listener = null;
			}
		}
		else if (e.getSource() == quit_button) {
			System.exit(0);
		}
		else if (e.getSource() == login_button) {
			String name = name_field.getText().trim();
			String password = password_field.getText().trim();
			if (!is_single_word(name) || !is_single_word(password)) {
				JOptionPane.showMessageDialog(this, "The name and password must be single words.");
			}
			else {
				send_command("LOGIN " + name + " " + password);
				latest_attempted_login_name = name;
			}
		}
		else if (e.getSource() == logout_button) {
			send_command("LOGOUT");
		}
		else if (e.getSource() == register_button) {
			String name = name_field.getText().trim();
			String password = password_field.getText().trim();
			if (!is_single_word(name) || !is_single_word(password))
				JOptionPane.showMessageDialog(this, "The name and password must be single words.");
			else
				send_command("REGISTER " + name + " " + password);
		}
		else if (e.getSource() == send_location_button) {
			String latitude= latitude_field.getText().trim();
			String longitude= longitude_field.getText().trim();
			if (!is_double_value(latitude) || !is_double_value(longitude))
				JOptionPane.showMessageDialog(this, "The latitude and longitude must be single numbers.");
			else
				send_command("I-AM-AT " + latitude + " " + longitude); 
		}
		else if (e.getSource() == send_visibility_button) {
			String limit = visibility_field.getText().trim();
			if (!is_double_value(limit))
				JOptionPane.showMessageDialog(this, "The limit must be a single number.");
			else
				send_command("SET-VISIBILITY " + limit);
		}
		else if (e.getSource() == zombie_button) {
			send_command("TURN ZOMBIE");
		}
		else if (e.getSource() == human_button) {
			send_command("TURN HUMAN");
		}
		else if (e.getSource() == show_map_button) {
			if (map_window == null) {
				map_window = new MapWindow(this);
				map_window.set_my_name(actual_login_name);
			}
			else if (!map_window.isVisible())
				map_window.setVisible(true);
			else
				map_window.toFront();
			// else
			// JOptionPane.showMessageDialog(this, "The map is already shown.");
		}
		else if (e.getSource() == show_log_button) {
			if (log_window == null)
				log_window = new LogWindow(this);
			else if (!log_window.isVisible())
				log_window.setVisible(true);
			else
				log_window.toFront();
			// System.out.println("log_window  = " + log_window);
		}
	} // actionPerformed

	private boolean is_integer_value(String s) {
		int n = s.length();
		if (n == 0)
			return false;
		for (int i = 0; i < n; ++i)
			if (Character.isSpaceChar(s.charAt(i)))
				return false;
		try {
			// int value = Integer.parseInt(s);
			Integer.parseInt(s);
		}
		catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	private boolean is_double_value(String s) {
		int n = s.length();
		if (n == 0)
			return false;
		for (int i = 0; i < n; ++i)
			if (Character.isSpaceChar(s.charAt(i)))
				return false;
		try {
			// double value = Double.parseDouble(s);
			Double.parseDouble(s);
		}
		catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	private boolean is_single_word(String s) {
		int n = s.length();
		if (n == 0)
			return false;
		for (int i = 0; i < n; ++i)
			if (Character.isSpaceChar(s.charAt(i)))
				return false;
		return true;
	}

	private int command_number = 0;

	private void send_command(String line) {
		if (server_listener == null) {
			JOptionPane.showMessageDialog(this, "Not connected.");
		}
		else {
			line = ++command_number + " " + line;
			System.out.println("Sending to server: " + line);
			if (log_window != null)
				log_window.from_client(line);
			server_listener.send_line(line);
		}
	}

	public void server_disconnected() {
		setConnected(false);
		server_listener = null;
	}

	private void connect(final String server, final int port) {
		// BufferedReader kbd_reader = new BufferedReader(new InputStreamReader(System.in));
		final ClientWindow this_window = this;
		Thread t = new Thread("Uppkopplingstraaden") {
			public void run() {
				Socket socket = null;
				try {
					InetAddress addr = InetAddress.getByName(server);
					System.out.println("Kopplar upp mot servern...");
					socket = new Socket(addr, port);
					BufferedReader from_server = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					PrintWriter to_server = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);			
					// true: PrintWriter is line buffered
					
					server_listener = new ServerListenerThread(this_window, socket, from_server, to_server);
					setConnected(true);
					server_listener.run();
				}
				catch (UnknownHostException e) {
					JOptionPane.showMessageDialog(this_window, "Unknown server name: " + server);
				}
				catch (IOException e) {
					if (socket != null) {
						try {
							socket.close();
						}
						catch (IOException e1) {
							// Error when closing the socket. Not much we can or should do here, so do nothing.
						}
					}
					JOptionPane.showMessageDialog(this_window, "Couldn't connect to " + server);
				}
			} // run
		}; // class
		t.start();
	} // connect

	private int received_lines = 0;

	public void received_line(String line_from_server) {
		++received_lines;
		
		if (log_window != null)
			log_window.from_server(line_from_server);
		
		if (line_from_server == null) {
			--received_lines;
			// Should already have been handled, but just to be sure
			server_listener.please_quit();
			server_disconnected();
		}
		else if (received_lines == 1) {
			if (!line_from_server.startsWith("ZombieServer ")) {
				JOptionPane.showMessageDialog(this, "This doesn't seem to be a zombie-game server. It said: " + line_from_server);
			}
			else {
				String server_version = line_from_server.substring(13);
				JOptionPane.showMessageDialog(this, "Connected to a zombie-game server, server version " + server_version);
			}
		}
		else if (line_from_server.equals("ERROR MALFORMED-COMMAND")) {
			JOptionPane.showMessageDialog(this, "The server sent an error message: " + line_from_server);
		}
		else if (line_from_server.startsWith("ASYNC ")) {
			// Handle an asynchronous message from the server
			int length = line_from_server.length();
			int position = 6; // After "ASYNC "
			while (position < length && Character.isWhitespace(line_from_server.charAt(position)))
				++position;
			int command_start = position;
			while (position < length && !Character.isWhitespace(line_from_server.charAt(position)))
				++position;
			String command = line_from_server.substring(command_start, position);
			
			while (position < length && Character.isWhitespace(line_from_server.charAt(position)))
				++position;
			int arguments_start = position;
			String arguments = line_from_server.substring(arguments_start, length);
			handle_async_command(command, arguments, line_from_server);
		}
		else {
			// Handle a (synchronous) reply from the server
			int length = line_from_server.length();
			int position = 0;
			while (position < length && Character.isWhitespace(line_from_server.charAt(position)))
				++position;
			int request_number_start = position;
			while (position < length && Character.isDigit(line_from_server.charAt(position)))
				++position;
			String request_number_string = line_from_server.substring(request_number_start, position);
			if (request_number_string.length() == 0) {
				JOptionPane.showMessageDialog(this, "What?! The server sent an incorrect message: " + line_from_server);
				return;
			}
			int request_number = Integer.parseInt(request_number_string); // We already know it's just digits
			
			while (position < length && Character.isWhitespace(line_from_server.charAt(position)))
				++position;
			int command_start = position;
			// while (position < length && Character.isLetter(line_from_server.charAt(position)))
			while (position < length && !Character.isWhitespace(line_from_server.charAt(position)))
				++position;
			String command = line_from_server.substring(command_start, position);
			
			if (command.equals("ERROR")) {
				JOptionPane.showMessageDialog(this, "The server sent an error message: " + line_from_server);
			}
			else {
				while (position < length && Character.isWhitespace(line_from_server.charAt(position)))
					++position;
				int arguments_start = position;
				String arguments = line_from_server.substring(arguments_start, length);
				handle_command(request_number, command, arguments, line_from_server);
			}
		}

	} // received_line

	private void handle_async_command(String command, String arguments,	String line_from_server) {
			final String split_arguments[] = split_arguments(arguments);
			if (command.equals("YOU-ARE")) {
				if (split_arguments.length != 1) {
					JOptionPane.showMessageDialog(this, "What?! The server sent an incorrect message: " + line_from_server);
				}
				else if (split_arguments[0].equals("ZOMBIE")) {
					setZombie(true);
					JOptionPane.showMessageDialog(this, "Oh no! You are a zombie!");
				}
				else if (split_arguments[0].equals("HUMAN")) {
					setZombie(false);
					JOptionPane.showMessageDialog(this, "Great! You are human!");
				}
				else {
					JOptionPane.showMessageDialog(this, "What?! The server sent an incorrect message: " + line_from_server);
				}
			}
			else if (command.equals("PLAYER")) {
				if (split_arguments.length == 2 && split_arguments[1].equals("GONE")) {
					// That player is no longer visible
					String name = split_arguments[0];
					if (map_window != null)
						map_window.forget_one_player(name);
				}
				else if (split_arguments.length == 4) {
					String name = split_arguments[0];
					String status = split_arguments[1];
					String latitude_string = split_arguments[2];
					String longitude_string = split_arguments[3];
					boolean is_zombie = status.equals("ZOMBIE"); // Trust the server, assume it is either ZOMBIE or HUMAN
					double latitude = Double.parseDouble(latitude_string); // Trust the server, assume it is a valid number
					double longitude = Double.parseDouble(longitude_string); // Trust the server, assume it is a valid number
					
//					System.out.println("split_arguments[0] = " + split_arguments[0]);
//					System.out.println("split_arguments[1] = " + split_arguments[1]);
//					System.out.println("split_arguments[2] = " + split_arguments[2]);
//					System.out.println("split_arguments[3] = " + split_arguments[3]);
					
					if (map_window != null)
						map_window.new_player_status(name, is_zombie, latitude, longitude);
					if (name.equals(actual_login_name))
						setZombie(is_zombie);
				}
				else {
					JOptionPane.showMessageDialog(this, "What?! The server sent an incorrect message: " + line_from_server);
				}
			}
			else {
				JOptionPane.showMessageDialog(this, "What?! The server sent an incorrect message: " + line_from_server);
			}
		} // handle_async_command
		


	String[] split_arguments(String arguments) {
		String[] result = arguments.split("[\t ]+");
		if (result.length == 1 && result[0].equals("")) {
			result = new String[0];
		}
		return result;
	}

	private void handle_command(int request_number, String command, String arguments, String line_from_server) {
		final String split_arguments[] = split_arguments(arguments);
		if (command.equals("REGISTERED")) {
			if (split_arguments.length != 1) {
				JOptionPane.showMessageDialog(this, "What?! The server sent an incorrect message: " + line_from_server);
			}
			else {
				JOptionPane.showMessageDialog(this, "You have been registered as player number " + split_arguments[0]);
			}
		}
		else if (command.equals("WELCOME")) {
			if (split_arguments.length != 1) {
				JOptionPane.showMessageDialog(this, "What?! The server sent an incorrect message: " + line_from_server);
			}
			else {
				setLoggedIn(latest_attempted_login_name);
				if (map_window != null)
					map_window.set_my_name(latest_attempted_login_name);
				JOptionPane.showMessageDialog(this, "You have been logged in as player number " + split_arguments[0]);
			}
		}
		else if (command.equals("GOODBYE")) {
			if (split_arguments.length != 0) {
				JOptionPane.showMessageDialog(this, "What?! The server sent an incorrect message: " + line_from_server);
			}
			else {
				setLoggedIn(null);
				JOptionPane.showMessageDialog(this, "You have been logged out.");
			}
		}
		else if (command.equals("OK")) {
			// Generic reply
			if (split_arguments.length != 0) {
				JOptionPane.showMessageDialog(this, "What?! The server sent an incorrect message: " + line_from_server);
			}
			else {
				JOptionPane.showMessageDialog(this, "The server says OK.");
			}
		}
		else if (command.equals("YOU-ARE")) {
			if (split_arguments.length != 1) {
				JOptionPane.showMessageDialog(this, "What?! The server sent an incorrect message: " + line_from_server);
			}
			else if (split_arguments[0].equals("HUMAN")) {
				JOptionPane.showMessageDialog(this, "You are a human.");
			}
			else if (split_arguments[0].equals("ZOMBIE")) {
				JOptionPane.showMessageDialog(this, "You are a zombie.");
			}
			else {
				JOptionPane.showMessageDialog(this, "What?! The server sent an incorrect message: " + line_from_server);
			}
		}
		else if (command.equals("YOU-ARE-AT")) {
			if (split_arguments.length != 2) {
				JOptionPane.showMessageDialog(this, "What?! The server sent an incorrect message: " + line_from_server);
			}
			else {
				JOptionPane.showMessageDialog(this, "Your position: lat " + split_arguments[0] + ", long " + split_arguments[1]);
			}
		}
		else if (command.equals("VISIBLE-PLAYERS")) {
			// This is a reply to a "LIST-VISIBLE-PLAYERS" command. A list of players will follow.
			if (split_arguments.length != 2) {
				JOptionPane.showMessageDialog(this, "What?! The server sent an incorrect message: " + line_from_server);
			}
			else {
				// Reset player list
				if (map_window != null)
					map_window.forget_all_players();
			}
		}
		else if (command.equals("PLAYER")) {
			// This is part of the player list after a "VISIBLE_PLAYERS" reply to a "LIST-VISIBLE-PLAYERS" command
			if (split_arguments.length != 4) {
				JOptionPane.showMessageDialog(this, "What?! The server sent an incorrect message: " + line_from_server);
			}
			else {
				// Add to player list
				String name = split_arguments[0];
				String status = split_arguments[1];
				String latitude_string = split_arguments[2];
				String longitude_string = split_arguments[3];
				boolean is_zombie = status.equals("ZOMBIE"); // Trust the server, assume it is either ZOMBIE or HUMAN
				double latitude = Double.parseDouble(latitude_string); // Trust the server, assume it is a validnumber
				double longitude = Double.parseDouble(longitude_string); // Trust the server, assume it is a validnumber
				if (map_window != null)
					map_window.new_player_status(name, is_zombie, latitude, longitude);
			}
		}
		else {
			JOptionPane.showMessageDialog(this, "What?! The server sent an incorrect message: " + line_from_server);
		}
	} // handle_command
	
	private void setLoggedIn(String logged_in_user_name) {
		actual_login_name = logged_in_user_name;
		if (logged_in_user_name == null) {
			login_status_field.setText("Not logged in");
			name_status_field.setText("UNKNOWN");
		}
		else {
			login_status_field.setText("Logged in");
			name_status_field.setText(logged_in_user_name);

		}
	}
	
	private void setConnected(boolean is_connected) {
		String status = is_connected ? "Connected" : "Not connected"; 
		this.connection_status_field.setText(status);
	}

	private void setZombie(boolean is_zombie) {
		this.zombie_status_field.setText(is_zombie ? "Zombie" : "Human");
	}

	public void please_list_visible_players() {
		send_command("LIST-VISIBLE-PLAYERS");
	}

} // class ClientWindow 