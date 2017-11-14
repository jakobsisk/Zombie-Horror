// ZombieServer
// By Thomas Padron-McCarthy (thomas.padron-mccarthy@oru.se)
// Free software. No warranty. Share and enjoy.

public class ZombieServer {
	public final static int PORT = 2002;
	public final static double ATTACK_DISTANCE_LIMIT = 20;
	public final static double DEFAULT_VISIBILITY_LIMIT = 20*1000*1000; // Around the world
	public final static String VERSION = "0.3.2";
	
	public static void main(String[] args) {
		String secret_password = null;
		if (args.length == 0)
			;
		else if (args.length == 1)
			secret_password = args[0];
		else
			System.err.println("Warning from ZombieServer. Usage: java ZombieServer [ password ]");
		MainServerThread server_thread = new MainServerThread(secret_password);
		server_thread.start();
	} // main
} // class ZombieServer
