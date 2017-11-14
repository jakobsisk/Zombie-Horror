import java.util.Date;

public class Location {
	private double latitude;
	private double longitude;
	private double accuracy;
	private Date when; // Time when this object was created, not necessarily when the original GPS fix was made

	public Location(double latitude, double longitude, double accuracy) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.accuracy = accuracy;
		this.when = new Date();
	}

	public double distance(Location l2) {
		double R = 6371000; // Average radius of the earth in meters
		double lat1 = this.getLatitude() / 180.0 * Math.PI;
		double lat2 = l2.getLatitude() / 180.0 * Math.PI;
		double lon1 = this.getLongitude() / 180.0 * Math.PI;
		double lon2 = l2.getLongitude() / 180.0 * Math.PI;
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

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getAccuracy() {
		return accuracy;
	}

	public Date getWhen() {
		return when;
	}
} // class Location
