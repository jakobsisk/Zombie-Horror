import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class MapWindow extends JFrame implements ActionListener {
	private final JPanel map;
	private final ClientWindow owner;
	private JButton redraw_button = new JButton("Re-draw map");
	
	private class MapPanel extends JPanel {
		public MapPanel() {
			super();
		}

		private double distance(double lat1, double lon1, double lat2, double lon2) {
			double R = 6371000; // Average radius of the earth in meters
			lat1 = lat1 / 180.0 * Math.PI;
			lat2 = lat2 / 180.0 * Math.PI;
			lon1 = lon1 / 180.0 * Math.PI;
			lon2 = lon2 / 180.0 * Math.PI;
			double dlat = Math.abs(lat1 - lat2);
			double dlon = Math.abs(lon1 - lon2);
			double a = Math.sin(dlat/2) * Math.sin(dlat/2) +
					Math.cos(lat1) * Math.cos(lat2) * 
					Math.sin(dlon/2) * Math.sin(dlon/2); 
			double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
			double d = R * c;
			// System.out.println("*** Distance = " + d + " m");
			return d;
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponents(g);

			System.out.println("paint map, " + players.size() + " players...");

			int window_width = getWidth();
			int window_height = getHeight();

			g.clearRect(0,  0, window_width, window_height);

			Set<String> player_names = players.keySet();

			g.setColor(Color.black);
			g.drawString(player_names.size() + " spelare", 20, 20); 

			if (player_names.size() == 0) {
				// There are no players to put on the map
			}
			else {
				// There is at least one player to put on the map

				// First, we find out how big a map we need to draw
				double min_lat = 1000, max_lat = -1000, min_long = 1000, max_long = -1000;

				for (String name : players.keySet()) {
					PlayerStatus s = players.get(name);
					if (s.latitude < min_lat)
						min_lat = s.latitude;
					if (s.latitude > max_lat)
						max_lat = s.latitude;
					if (s.longitude < min_long)
						min_long = s.longitude;
					if (s.longitude > max_long)
						max_long = s.longitude;
				}

				double lat_span_degrees = max_lat - min_lat; // In latitude degrees!
				double long_span_degrees = max_long - min_long; // In longitude degrees!

				System.out.println("lat_span_degrees = " + lat_span_degrees);
				System.out.println("long_span_degrees = " + long_span_degrees);
				
				// If all players (possibly just one) have exactly the same lat or long, we use a 100 meters high (or wide) map
				if (lat_span_degrees == 0) {
					lat_span_degrees = 0.00090; // In Sweden (and everywhere else): 1 lat deg = approx 112 km, 0.00090 degrees = 100 m
					min_lat = min_lat - lat_span_degrees / 2;
					max_lat = max_lat + lat_span_degrees / 2;
				}
				if (long_span_degrees == 0) {
					long_span_degrees = 0.00175; // In Sweden: 1 long deg = approx 57 km, 0.00175 degrees = 100 m
					min_long = min_long - long_span_degrees / 2;
					max_long = max_long + long_span_degrees / 2;
				}

				double long_span_meters = distance(min_lat, min_long, min_lat, max_long);
				double lat_span_meters = distance(min_lat, min_long, max_lat, min_long);
				double lat_pixels_per_meter = window_height / lat_span_meters;
				double long_pixels_per_meter = window_width / long_span_meters;
				
				System.out.println("lat_span_meters = " + lat_span_meters);
				System.out.println("long_span_meters = " + long_span_meters);
				System.out.println("lat_pixels_per_meter = " + lat_pixels_per_meter);
				System.out.println("long_pixels_per_meter = " + long_pixels_per_meter);

				// For the map to fit in the window, we must scale according to the axis with the LEAST pixels per meter
				double pixels_per_meter;
				if (lat_pixels_per_meter < long_pixels_per_meter)
					pixels_per_meter = lat_pixels_per_meter;
				else
					pixels_per_meter = long_pixels_per_meter;

				// We also add 10% extra height and width, to get a 5% margin on each side
				pixels_per_meter *= 0.90;

				int margin_in_pixels;
				if (window_height < window_width)
					margin_in_pixels = (int)(window_height * 0.05 + 0.5);
				else
					margin_in_pixels = (int)(window_width * 0.05 + 0.5);
				int usable_window_height = window_height - 2 * margin_in_pixels;
				int usable_window_width = window_width - 2 * margin_in_pixels;

				
				System.out.println("pixels_per_meter = " + pixels_per_meter);
				System.out.println("margin_in_pixels = " + margin_in_pixels);
				System.out.println("usable_window_height = " + usable_window_height);
				System.out.println("usable_window_width = " + usable_window_width);
				
				// Draw a map scale in the lower right corner

				g.setColor(Color.blue);
				final int margin = 10; // How many pixels from the window edge?
				double max_possible_scale_length = (window_width - 2 * margin) / pixels_per_meter; // In meters
				int scale_length = 0;

				// Font font = g.getFont();
				FontMetrics fm = g.getFontMetrics();

				if (max_possible_scale_length <= 1) {
				    // No scale!
				}
				else if (max_possible_scale_length < 10) {
				    scale_length = (int)(1 * pixels_per_meter);
				    g.drawString("1 m", window_width - margin - fm.stringWidth("1 m"), window_height - 2 * margin);
				}
				else if (max_possible_scale_length < 100) {
				    scale_length = (int)(10 * pixels_per_meter);
				    g.drawString("10 m", window_width - margin - fm.stringWidth("10 m"), window_height - 2 * margin);   
				}
				else if (max_possible_scale_length < 1000) {
				    scale_length = (int)(100 * pixels_per_meter);
				    g.drawString("100 m", window_width - margin - fm.stringWidth("100 m"), window_height - 2 * margin);   
				}
				else if (max_possible_scale_length < 10000) {
				    scale_length = (int)(1000 * pixels_per_meter);
				    g.drawString("1 km", window_width - margin - fm.stringWidth("1 km"), window_height - 2 * margin);   
				}
				else {
				    scale_length = (int)(10000 * pixels_per_meter);
				    g.drawString("10 km", window_width - margin - fm.stringWidth("10 km"), window_height - 2 * margin);
				}
				if (scale_length != 0) {
				    g.drawLine(window_width - margin - scale_length, window_height - 2 * margin, window_width - margin - scale_length, window_height);
				    g.drawLine(window_width - margin - scale_length, window_height - margin, window_width - margin, window_height - margin);
				    g.drawLine(window_width - margin, window_height - 2 * margin, window_width - margin, window_height);
				}

				for (String name : players.keySet()) {
					PlayerStatus s = players.get(name);

					int xpos = margin_in_pixels + (int)((s.longitude - min_long) / long_span_degrees * usable_window_width + 0.5);
					// The y axis increases downwards
					int ypos = window_height - margin_in_pixels - (int)((s.latitude - min_lat) / lat_span_degrees * usable_window_height + 0.5);
					
					System.out.println("paint map, x = " + xpos + ", y = " + ypos);
					System.out.println("s = " + s + ", my_status = " + my_status);
					if (s == my_status)
						g.setColor(Color.black);
					else if (s.is_zombie == my_status.is_zombie)
						g.setColor(Color.green);
					else
						g.setColor(Color.red);
					g.fillOval(xpos - 5, ypos - 5, 10, 10);
					String text = s.name + (s.is_zombie ? "(Z)" : "(H)");
					if (s != my_status) {
						int d = (int)(distance(my_status.latitude, my_status.longitude, s.latitude, s.longitude) + 0.5);
						text = text + ", " + d + " m";
					}
					int text_width = fm.stringWidth(text);
					if (xpos < window_width / 2)
						g.drawString(text, xpos + 10, ypos + 5);
					else
						g.drawString(text, xpos - text_width - 10, ypos + 5);
				} // for each player name
			} // There was at least one player to put on the map
		} // paintComponent
	} // class MapPanel
	
	public MapWindow(ClientWindow owner) {
		super("Zombie Map");
		this.owner = owner;
		Container pane = getContentPane();		
		pane.setLayout(new BorderLayout());
		
		JPanel button_panel = new JPanel();
		button_panel.add(redraw_button);
	    pane.add(button_panel, BorderLayout.NORTH);
	    
	    redraw_button.addActionListener(this);  

		map = new MapPanel();
		pane.add(map);
		setSize(new Dimension(300, 300));
		// pack(); -- No!?
        setVisible(true);

        // addWindowListener(owner);
        // this.addWindowStateListener(owner);
		// setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}
	
	private class PlayerStatus {
		public PlayerStatus(String name, boolean is_zombie, double latitude, double longitude) {
			this.name = name;
			this.is_zombie = is_zombie;
			this.latitude = latitude;
			this.longitude = longitude;
		}
		public String name;
		public boolean is_zombie;
		public double latitude;
		public double longitude;
	} // PlayerStatus

	private Map<String, PlayerStatus> players = new HashMap<String, PlayerStatus>();
	private String my_name;
	private PlayerStatus my_status;

	public PlayerStatus lookup_player(String name) {
		return players.get(name);
	}
	
	public void new_player_status(String name, boolean is_zombie, double latitude, double longitude) {
		System.out.println("MapWindow.new_player_status('" + name + "'...)");
		PlayerStatus s = players.get(name);
		if (s == null) {
			s = new PlayerStatus(name, is_zombie, latitude, longitude);
			players.put(name, s);
		}
		else {
			s.is_zombie = is_zombie;
			s.latitude = latitude;
			s.longitude = longitude;
		}
		if (name.equals(my_name))
			my_status = s;
		map.repaint();
	}

	public void forget_one_player(String name) {
		System.out.println("MapWindow.forget_one_player('" + name + "'...)");
		players.remove(name);
		if (name.equals(my_name)) // Can this happen?
			my_status = null;
		map.repaint();
	}

	public void forget_all_players() {
		System.out.println("MapWindow.forget_all_players");
		players.clear();
		my_status = null;
		map.repaint();
	}
	
	public void set_my_name(String name) {
		this.my_name = name;
		this.my_status = players.get(name);
		map.repaint();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == redraw_button) {
			owner.please_list_visible_players();
		}
	}
} // class MapWindow
