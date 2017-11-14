import java.text.SimpleDateFormat;
import java.util.Date;

public class SimpleLogger {
	private final String program_name;
	private final SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z");

	public SimpleLogger(String program_name) {
		this.program_name = program_name;
	}

	public void log(String message) {
		Date now = new Date();
		String formatted_date = date_format.format(now);
		String line = program_name + " " + formatted_date + ": " + message;
		System.out.println(line);
	}
} // class SimpleLogger
