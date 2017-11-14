import java.util.ArrayList;
import java.util.LinkedList;

public class MainServerThread extends Thread {
	private SimpleLogger logger = new SimpleLogger("ZombieServerThread");
	private boolean quit;
	private ClientAcceptorThread acceptor;
	private final ArrayList<ClientListenerThread> clients = new ArrayList<ClientListenerThread>();
	private LinkedList<MessageToServer> inqueue = new LinkedList<MessageToServer>();
	private ArrayList<Player> players = new ArrayList<Player>(); // Some of these may not be logged in
	private String secret_password = null;
	
	public MainServerThread(String secret_password) {
		this.secret_password = secret_password;
		acceptor = new ClientAcceptorThread(this, ZombieServer.PORT);
		acceptor.start();
	}
	
	private boolean something_has_changed = true; // If the simulation should be run
	
	public void run() {
		while (!quit) {
			MessageToServer m = get(); // Will wait for a message
			if (m == null) {
				logger.log("*** get returned null! This isn't supposed to happen.");
			}
			else {
				something_has_changed = false;
				m.handle();
				if (something_has_changed == true) {
					// Run a simulation step, but as next step in the queue
					put(new MessageToServer(null, null, 0) {
						void handle() {
							simulation_step();
						}
					});
				}
				else {
					// Nothing has changed in the world
				}
			}
		} // while
	} // run
	
	// private LinkedList<Player> players = new LinkedList<Player>();

	// We only care about players who are logged in
	private void simulation_step() {
		int nr_registered = players.size();
		int nr_logged_in = 0;

		logger.log("Simulation step starting, " + nr_registered + " registered players...");
		
		for (Player p : players) {
			if (p.getClient() != null) {
				p.nr_close_humans = 0;
				p.nr_close_zombies = 0;
			}
		}
			
		// We must iterate over ALL players, for nr_logged_in to be correct
		for (int i1 = 0; i1 < nr_registered; ++i1) {
			Player p1 = players.get(i1);
			if (p1.getClient() != null) {
				++nr_logged_in;
				for (int i2 = i1 + 1; i2 < nr_registered; ++i2) {
					Player p2 = players.get(i2);
					if (p2.getClient() != null)
						interact(p1, p2);
				}
			}
		}

		for (Player p : players) {
			ClientListenerThread client = p.getClient();
			if (client != null) {
				if (p.is_zombie() && p.nr_close_zombies == 0 && p.nr_close_humans >= 2) {
					p.humanize();
					logger.log(p.getName() + " turned human!");
					client.send_line("ASYNC YOU-ARE HUMAN");
					broadcast_player_status(p, false);
				}
				else if (!p.is_zombie() && p.nr_close_humans == 0 && p.nr_close_zombies >= 2) {
					p.zombiefy();
					logger.log(p.getName() + " turned zombie!");
					client.send_line("ASYNC YOU-ARE ZOMBIE");
					broadcast_player_status(p, false);
				}
			}
		}
		
		logger.log("Simulation step finished, " + nr_logged_in + " players logged in.");
	}

	private void interact(Player p1, Player p2) {
		Location l1 = p1.getLocation();
		Location l2 = p2.getLocation();
		if (l1 != null && l2 != null) {
			double distance = l1.distance(l2);
			if (distance < ZombieServer.ATTACK_DISTANCE_LIMIT) {
				if (p1.is_zombie() && !p2.is_zombie()) {
					p1.nr_close_humans += 1;
					p2.nr_close_zombies += 1;
				}
				else if (!p1.is_zombie() && p2.is_zombie()) {
					p1.nr_close_zombies += 1;
					p2.nr_close_humans += 1;
				}
			}
		}
	}

	public void please_quit() {
		quit = true;
	}

	public void client_connected(ClientListenerThread listener) {
		put(new ClientConnectedMessage(listener));
	} // client_connected

	public void client_disconnected(ClientListenerThread listener, Player p) {
		put(new MessageToServer(listener, p, 0) {
			void handle() {
				// logger.log("handle disconnect... player = " + player);
				if (player != null) {
					player.logout();
					something_has_changed = true;
				}
				clients.remove(listener);
				listener.please_quit(); // Probably already done, but to be sure
			}
		});
	}

	private void broadcast_player_status(Player p, final boolean send_gone) {
		put(new MessageToServer(null, p, 0) {
			void handle() {
				Location location = player.getLocation();
				Location old_location = player.getPreviousLocation();
				boolean logged_in = player.getClient() != null;
				String type = player.is_zombie() ? "ZOMBIE" : "HUMAN";
				String gone_line = "ASYNC PLAYER " + player.getName() + " GONE";

				if (!logged_in && old_location != null && send_gone) {
					// Send to players within their (their!) visibility limit
					for (ClientListenerThread c2 : clients) {
						Player p2 = c2.getPlayer();
						if (p2 != null) {
							Location l2 = p2.getLocation();
							// When logged out, only the previous location is available
							if (l2 != null && old_location.distance(l2) < p2.getVisibilityLimit()) {
								c2.send_line(gone_line);
							}
						}
					}
				}
				
				if (logged_in && location != null) {
					String status_line = "ASYNC PLAYER " + player.getName() + " " + type + " " + location.getLatitude() + " " + location.getLongitude();
					// Send to players within their (their!) visibility limit
					for (ClientListenerThread c2 : clients) {
						Player p2 = c2.getPlayer();
						if (p2 != null) {
							Location l2 = p2.getLocation();
							if (l2 != null) {
								if (location.distance(l2) < p2.getVisibilityLimit()) {
									c2.send_line(status_line);							
								}
								else if (send_gone && old_location != null) {
									if (old_location.distance(l2) < p2.getVisibilityLimit()) {
										c2.send_line(gone_line);	
									}
								}
							}
						}
					}
				}
				
			}
		});
	}

	public void received_line(ClientListenerThread listener, String line_from_client) {
		// logger.log("received_line(" + line_from_client + ")");
		if (line_from_client == null) {
			// Should already have been handled, but just to be sure
		    listener.please_quit();
		    client_disconnected(listener, listener.getPlayer());
		}
		else {
			int length = line_from_client.length();
			int position = 0;
			while (position < length && Character.isWhitespace(line_from_client.charAt(position)))
				++position;
			int request_number_start = position;
			while (position < length && Character.isDigit(line_from_client.charAt(position)))
				++position;
			String request_number_string = line_from_client.substring(request_number_start, position);
			if (request_number_string.length() == 0) {
				listener.send_line("ERROR MALFORMED-COMMAND");
				return;
			}
			int request_number = Integer.parseInt(request_number_string); // We already know it's just digits
			
			while (position < length && Character.isWhitespace(line_from_client.charAt(position)))
				++position;
			int command_start = position;
			// while (position < length && Character.isLetter(line_from_client.charAt(position)))
			while (position < length && !Character.isWhitespace(line_from_client.charAt(position)))
				++position;
			String command = line_from_client.substring(command_start, position);
			while (position < length && Character.isWhitespace(line_from_client.charAt(position)))
				++position;
			int arguments_start = position;
			String arguments = line_from_client.substring(arguments_start, length);
			
			String playername = null;
			Player p = listener.getPlayer();
			if (p != null)
				playername = p.getName();
			logger.log("From client " + playername + ": " + request_number + ", " + command + ", " + arguments);
			
			handle_command(listener, request_number, command, arguments, line_from_client);
		}

	}

	String[] split_arguments(String arguments) {
		String[] result = arguments.split("[\t ]+");
		if (result.length == 1 && result[0].equals("")) {
			result = new String[0];
		}
		return result;
	}
	
	private void handle_command(ClientListenerThread listener, final int request_number, String command, String arguments, String full_line) {
		Player player = listener.getPlayer();

		if (command == null) {
			// null means disconnected. Handle as a LOGOUT'command.
			put(new MessageToServer(listener, player, request_number) {
					void handle() {
						// We're not interested in the player when this message was scheduled, but the one NOW, if there has been a change.
						Player player = listener.getPlayer();
						if (player == null) {
							listener.send_line(request_number + " ERROR NOT-LOGGED-IN");
						}
						else {
							// Logout the player
							player.logout();
							// listener.send_line(request_number + " GOODBYE"); -- not connected!
							broadcast_player_status(player, true);
							something_has_changed = true;
						}
					}
				});
		}
		final String split_arguments[] = split_arguments(arguments);
		if (command.equals("REGISTER")) {
			if (split_arguments.length != 2) {
				listener.send_line(request_number + " ERROR BAD-ARGUMENTS");
			}
			else {
				put(new MessageToServer(listener, player, request_number) {
					String name = split_arguments[0];
					String password = split_arguments[1];
					void handle() {
						// Register a new player
						if (findPlayer(name) != null) {
							listener.send_line(request_number + " ERROR NAME-ALREADY-REGISTERED");
						}
						else {
							Player new_player = new Player(name, password);
							players.add(new_player);
							listener.send_line(request_number + " REGISTERED " + new_player.getNumber());
						}
					}
				});
			}
		}
		else if (command.equals("LOGIN")) {
			if (split_arguments.length != 2) {
				listener.send_line(request_number + " ERROR BAD-ARGUMENTS");
			}
			else {
				put(new MessageToServer(listener, player, request_number) {
					String name = split_arguments[0];
					String password = split_arguments[1];
					void handle() {
						if (listener.getPlayer() != null) {
							listener.send_line(request_number + " ERROR THIS-CLIENT-ALREADY-LOGGED-IN");
						}
						else {
							// Login as a player
							Player player = findPlayer(name);
							if (player == null) {
								listener.send_line(request_number + " ERROR UNKNOWN-PLAYER");
							}
							else if (!player.correct_password(password)) {
								listener.send_line(request_number + " ERROR WRONG-PASSWORD");
							}
							else if (player.getClient() != null) {
								listener.send_line(request_number + " ERROR THAT-PLAYER-ALREADY-LOGGED-IN");							
							}
							else {
								player.associate_to_client(listener);
								// listener.associate_to_player(player);
								listener.send_line(request_number + " WELCOME " + player.getNumber());
								something_has_changed = true;
							}
						}
					}
				});
			}
		}
		else if (command.equals("LOGOUT")) {
			if (split_arguments.length != 0) {
				listener.send_line(request_number + " ERROR BAD-ARGUMENTS");
			}
			else {
				put(new MessageToServer(listener, player, request_number) {
					void handle() {
						// We're not interested in the player when this message was scheduled, but the one NOW, if there has been a change.
						Player player = listener.getPlayer();
						if (player == null) {
							listener.send_line(request_number + " ERROR NOT-LOGGED-IN");
						}
						else {
							// Logout the player
							player.logout();
							listener.send_line(request_number + " GOODBYE");
							broadcast_player_status(player, true);
							something_has_changed = true;
						}
					}
				});
			}
		}
		else if (command.equals("I-AM-AT")) {
			if (split_arguments.length != 2) {
				listener.send_line(request_number + " ERROR BAD-ARGUMENTS");
			}
			else {
				try {
					final double latitude = Double.parseDouble(split_arguments[0]);
					final double longitude = Double.parseDouble(split_arguments[1]);
					if (latitude < -180 || latitude > 180 || longitude < -180 || longitude > 180) {
						listener.send_line(request_number + " ERROR BAD-ARGUMENTS");
					}
					else {
						put(new MessageToServer(listener, player, request_number) {
							void handle() {
								// We're not interested in the player when this message was scheduled, but the one NOW, if there has been a change.
								Player player = listener.getPlayer();
								if (player == null) {
									listener.send_line(request_number + " ERROR NOT-LOGGED-IN");
								}
								else {
									player.newLocation(latitude, longitude);
									broadcast_player_status(player, true);
									listener.send_line(request_number + " OK");
									something_has_changed = true;
								}
							}
						});
					}
				}
				catch (NumberFormatException e) {
					listener.send_line(request_number + " ERROR BAD-ARGUMENTS");
				}
			}
		}
		else if (command.equals("WHAT-AM-I")) {
			if (split_arguments.length != 0) {
				listener.send_line(request_number + " ERROR BAD-ARGUMENTS");
			}
			else {
				put(new MessageToServer(listener, player, request_number) {
					void handle() {
						Player player = listener.getPlayer();
						if (player == null)
							listener.send_line(request_number + " ERROR NOT-LOGGED-IN");
						else if (player.is_zombie())
							listener.send_line(request_number + " YOU-ARE ZOMBIE");
						else
							listener.send_line(request_number + " YOU-ARE HUMAN");
					}
				});
			}
		}
		else if (command.equals("WHERE-AM-I")) {
			if (split_arguments.length != 0) {
				listener.send_line(request_number + " ERROR BAD-ARGUMENTS");
			}
			else {
				put(new MessageToServer(listener, player, request_number) {
					void handle() {
						Player player = listener.getPlayer();
						if (player == null)
							listener.send_line(request_number + " ERROR NOT-LOGGED-IN");
						else {
							Location location = player.getLocation();
							if (location == null)
								listener.send_line(request_number + " ERROR UNKNOWN-POSITION");
							else
								listener.send_line(request_number + " YOU-ARE-AT " + location.getLatitude() + " " + location.getLongitude());
						}
					}
				});
			}
		}
		else if (command.equals("SET-VISIBILITY")) {
			if (split_arguments.length != 1) {
				listener.send_line(request_number + " ERROR BAD-ARGUMENTS");
			}
			else {
				try {
					final double limit = Double.parseDouble(split_arguments[0]);
					if (limit <= 0) {
						listener.send_line(request_number + " ERROR BAD-ARGUMENTS");
					}
					else {
						put(new MessageToServer(listener, player, request_number) {
							void handle() {
								// We're not interested in the player when this message was scheduled, but the one NOW, if there has been a change.
								Player player = listener.getPlayer();
								if (player == null) {
									listener.send_line(request_number + " ERROR NOT-LOGGED-IN");
								}
								else {
									player.setVisibilityLimit(limit);
									listener.send_line(request_number + " OK");
									something_has_changed = true;
								}
							}
						});
					}
				}
				catch (NumberFormatException e) {
					listener.send_line(request_number + " ERROR BAD-ARGUMENTS");
				}
			}
		}
		else if (command.equals("LIST-VISIBLE-PLAYERS")) {
			if (split_arguments.length != 0) {
				listener.send_line(request_number + " ERROR BAD-ARGUMENTS");
			}
			else {
				put(new MessageToServer(listener, player, request_number) {
					void handle() {
						Player player = listener.getPlayer();
						Location observer_location = null;
						
						if (player == null) {
							listener.send_line(request_number + " ERROR NOT-LOGGED-IN");
						}
						else if ((observer_location = player.getLocation()) == null) {
							listener.send_line(request_number + " ERROR UNKNOWN-POSITION");
						}
						else {
							LinkedList<Player> visible_players = find_visible_players(player);
							// The logged-in player will be first in the list
							listener.send_line(request_number + " VISIBLE-PLAYERS " + player.getVisibilityLimit() + " " + visible_players.size());
							for (Player p : visible_players) {
								String type = p.is_zombie() ? "ZOMBIE" : "HUMAN";
								Location l= p.getLocation();
								// The location will be non-null, otherwise that player wouldn't be in the list
								listener.send_line(request_number + " PLAYER " + p.getName() + " " + type + " " + l.getLatitude() + " " + l.getLongitude());
							}
						}
					}
				});
			}
		}
		else if (command.equals("LIST-ALL-PLAYERS")) {
			if (split_arguments.length != 1 || !split_arguments[0].equals(secret_password)) {
				// No secret password, or the wrong one, or extra arguments
				listener.send_line(request_number + " ERROR BAD-ARGUMENTS");
			}
			else {
				put(new MessageToServer(listener, player, request_number) {
					void handle() {
						// Works even if this player is not logged in
						listener.send_line(request_number + " ALL-PLAYERS " + players.size());
						for (Player p : players) {
							String type = p.is_zombie() ? "ZOMBIE" : "HUMAN";
							boolean is_logged_in = (p.getClient() != null);
							Location location = p.getLocation();
							// The location can be null, since we list ALL players
							String reply_line = "" + request_number + " PLAYER " + p.getName() + " " + type + " ";
							if (!is_logged_in)
								reply_line += "not logged in";
							else if (location == null)
								reply_line += "unknown location";
							else
								reply_line += "" + location.getLatitude() + " " + location.getLongitude();
							listener.send_line(reply_line);
						}
					}
				});
			}
		}
		else if (command.equals("TURN")) {
			if (split_arguments.length != 1 || !split_arguments[0].equals("ZOMBIE") && !split_arguments[0].equals("HUMAN")) {
				listener.send_line(request_number + " ERROR BAD-ARGUMENTS");
			}
			else {
				put(new MessageToServer(listener, player, request_number) {
					void handle() {
						Player player = listener.getPlayer();
						if (player == null) {
							listener.send_line(request_number + " ERROR NOT-LOGGED-IN");
						}
						else {
							boolean to_zombie = split_arguments[0].equals("ZOMBIE");
							boolean is_zombie = player.is_zombie();
							if (to_zombie == is_zombie) {
								listener.send_line(request_number + " ERROR CANNOT-TURN");
							}
							else {
								player.setZombie(to_zombie);
								listener.send_line(request_number + " OK");
								String type = player.is_zombie() ? "ZOMBIE" : "HUMAN";
								listener.send_line("ASYNC YOU-ARE " + type);
								broadcast_player_status(player, false);
								something_has_changed = true;
							}
						}
					}
				});
			}
		}
		else {
			listener.send_line(request_number + " ERROR UNKNOWN-COMMAND");
		}
	}

	public abstract class MessageToServer {
		protected final ClientListenerThread listener;
		protected final Player player;
		protected final int request_number;
		public MessageToServer(ClientListenerThread listener, Player player, int request_number) {
			this.listener = listener;
			this.player = player;
			this.request_number = request_number;
		}
		abstract void handle();
	}

	public class ClientConnectedMessage extends MessageToServer {
		public ClientConnectedMessage(ClientListenerThread listener) {
			super(listener, null, 0);
		}
		void handle() {
			clients.add(listener);
			listener.start();
		}
	}
	
    public synchronized void put(MessageToServer m) {
        inqueue.addLast(m);
        notify();
        logger.log("After put, queue now " + inqueue.size());
    }

	public synchronized MessageToServer get() {
		while (inqueue.isEmpty()) {
			try {
				wait();
			} catch (InterruptedException e) {
	        	logger.log("wait interrupted");
				e.printStackTrace();
			}
		}
		MessageToServer m = inqueue.getFirst();
		inqueue.removeFirst();
		notify();
		logger.log("After get, queue now " + inqueue.size());
		return m;
	}
	
	private Player findPlayer(String name) {
		for (Player p : players) {
			if (name.equals(p.getName()))
				return p;
		}
		return null;
	}

	private LinkedList<Player> find_visible_players(Player observer) {
		Location observer_location = observer.getLocation();
		if (observer_location == null)
			return null;
		LinkedList<Player> found = new LinkedList<Player>();
		found.addLast(observer);
		double limit = observer.getVisibilityLimit();
		for (Player p : players) {
			if (p != observer) { 
				Location observed_location = p.getLocation();
				if (observed_location != null) {
					if (observer_location.distance(observed_location) < limit) {
						found.addLast(p);
					}
				}
			}
		}
		return found;
	}
} // class MainServerThread
