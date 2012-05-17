package pl.shockah.shocky.lines;

import java.util.Date;

import pl.shockah.BinBuffer;

public class LineOther extends Line {
	public final String text;
	
	public LineOther(String text) {this(new Date(),text);}
	public LineOther(long ms, String text) {this(new Date(ms),text);}
	public LineOther(Date time, String text) {
		super(time);
		this.text = text;
	}
	
	public LineOther(BinBuffer buffer) {
		super(buffer);
		this.text = buffer.readUString();
	}
	
	public void save(BinBuffer buffer) {
		super.save(buffer);
		buffer.writeUString(text);
	}

	public String getMessage() {
		return text;
	}

	public boolean containsUser(String user) {
		return false;
	}
}