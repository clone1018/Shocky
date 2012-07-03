package pl.shockah.shocky.lines;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import pl.shockah.BinBuffer;

public abstract class Line {
	private static final HashMap<Byte,Class<? extends Line>> lineIDMap = new HashMap<Byte,Class<? extends Line>>();
	private static final HashMap<Class<? extends Line>,Byte> lineClassMap = new HashMap<Class<? extends Line>,Byte>();
	private static final SimpleDateFormat sdf;
	protected static boolean withChannels = false;
	
	static {
		sdf = new SimpleDateFormat("[HH:mm:ss]");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	public static void setWithChannels(boolean b) {
		withChannels = b;
	}
	public static boolean getWithChannels() {
		return withChannels;
	}
	
	public final Date time;
	public final String channel;
	
	public Line(String channel) {this(new Date(),channel);}
	public Line(long ms, String channel) {this(new Date(ms),channel);}
	public Line(Date time, String channel) {
		this.time = time;
		this.channel = channel.toLowerCase();
	}
	
	public Line(BinBuffer buffer) {
		this(buffer.readXBytes(8),buffer.readString());
	}
	
	public void save(BinBuffer buffer) {
		buffer.writeXBytes(time.getTime(),8);
		buffer.writeString(channel);
	}
	
	public String toString() {
		return (withChannels ? "["+channel+"] " : " ")+sdf.format(time)+" "+getMessage();
	}
	public abstract String getMessage();
	
	public abstract boolean containsUser(String user);
	
	public static void registerLineType(Byte id, Class<? extends Line> type) {
		lineIDMap.put(id, type);
		lineClassMap.put(type, id);
	}
	
	public static byte getLineID(Line line) {
		Class<? extends Line> type = line.getClass();
		if (!lineClassMap.containsKey(type)) return -1;
		return lineClassMap.get(type);
	}
	
	public static Line readLine(BinBuffer buffer) throws Exception {
		byte type = (byte) buffer.readByte();
		Class<? extends Line> classLine = lineIDMap.get(type);
		if (classLine == null) return null;
		return classLine.getConstructor(BinBuffer.class).newInstance(buffer);
	}
}