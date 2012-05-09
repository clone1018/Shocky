package pl.shockah.shocky.lines;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public abstract class Line {
	private static SimpleDateFormat sdf;
	
	static {
		sdf = new SimpleDateFormat("[HH:mm:ss]");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	public final Date time;
	
	public Line() {this(new Date());}
	public Line(long ms) {this(new Date(ms));}
	public Line(Date time) {
		this.time = time;
	}
	
	public String toString() {
		return sdf.format(time)+" "+getMessage();
	}
	public abstract String getMessage();
}