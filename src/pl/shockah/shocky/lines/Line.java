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
	
	public Line(BinBuffer buffer) {
		this(buffer.readXBytes(8));
	}
	
	public void save(BinBuffer buffer) {
		buffer.writeXBytes(time.getTime(),8);
	}
	
	public String toString() {
		return sdf.format(time)+" "+getMessage();
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