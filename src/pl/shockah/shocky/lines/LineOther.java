package pl.shockah.shocky.lines;

import java.util.Date;

public class LineOther extends Line {
	public final String text;
	
	public LineOther(String text) {this(new Date(),text);}
	public LineOther(long ms, String text) {this(new Date(ms),text);}
	public LineOther(Date time, String text) {
		super(time);
		this.text = text;
	}

	public String getMessage() {
		return text;
	}
}