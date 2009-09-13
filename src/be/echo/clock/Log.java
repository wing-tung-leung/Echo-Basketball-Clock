package be.echo.clock;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {

	static private SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm:ss");

	static public void log(String text) {
		System.out.print(timeFormat.format(new Date()));
		System.out.print(" ");
		System.out.println(text);
	}

}
