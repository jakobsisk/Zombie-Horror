
public class Player {
	private enum ZombieStatus { HUMAN, ZOMBIE, UNKNOWN };

	private String name;
	private String password;
	private static int number_of_players = 0;
	private int number = ++number_of_players;
	private ClientListenerThread current_client = null;

	private ZombieStatus zombie_status = ZombieStatus.UNKNOWN;
	private Location last_known_location = null;
	private Location previous_known_location = null;
	// private Location reasonably_current_location = null;
	// private Date last_received_message = null;

	private double visibility_limit = 20000000; // Around the world by default
	
	// private static LinkedList<Player> players = new LinkedList<Player>(); // Some of these may not be logged in!
	
	public Player(String name, String password) {
		this.name = name;
		this.password = password;
	}
	
	// Used in the simulation
	public int nr_close_zombies;
	public int nr_close_humans;
	
	public boolean is_zombie() {
		return zombie_status == ZombieStatus.ZOMBIE;
	}

	public void humanize() {
		zombie_status = ZombieStatus.HUMAN;
	}

	public void zombiefy() {
		zombie_status = ZombieStatus.ZOMBIE;
	}
	
	public void setZombie(boolean is_zombie) {
		if (is_zombie)
			zombiefy();
		else
			humanize();
	}

	public String getName() {
		return name;
	}
	
	public boolean correct_password(String password) {
		return password.equals(this.password);
	}
	
	public void associate_to_client(ClientListenerThread listener) {
		this.current_client = listener;
		if (listener.getPlayer() != this )
			listener.associate_to_player(this);
	}

	public int getNumber() {
		return number;
	}

	public ClientListenerThread getClient() {
		return current_client;
	}
	
	public void logout() {
		if (current_client != null) {
			current_client.associate_to_player(null);
			current_client = null;
		}
		previous_known_location = last_known_location;
		last_known_location = null;
	}

	public void newLocation(double latitude, double longitude) {
		previous_known_location = last_known_location;
		last_known_location = new Location(latitude, longitude, 0); 
	}

	public Location getLocation() {
		return last_known_location;
	}


	public Location getPreviousLocation() {
		return previous_known_location;
	}

	public void setVisibilityLimit(double limit) {
		this.visibility_limit = limit;
	}
	
	public double getVisibilityLimit() {
		return this.visibility_limit;
	}
} // class Player
