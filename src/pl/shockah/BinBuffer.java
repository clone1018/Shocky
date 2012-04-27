package pl.shockah;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class BinBuffer {
	protected byte[] buffer;
	
	protected int pos,bytes;
	
	public BinBuffer() {this(8192);}
	public BinBuffer(int startBytes) {
		buffer = new byte[startBytes];
	}
	
	protected void increaseBufferSize() {
		buffer = Arrays.copyOf(buffer,buffer.length*2);
	}
	
	public int getPos() {return pos;}
	public void setPos(int pos) {this.pos = Math.min(Math.max(pos,0),bytes);}
	
	public boolean isEmpty() {return getSize() == 0;}
	public int getSize() {return bytes;}
	public int getSizeReal() {return buffer.length;}
	public int bytesLeft() {return bytes-pos;}
	
	public void clear() {
		pos = 0;
		bytes = 0;
	}
	
	public void writeByte(int value) {
		if (pos > buffer.length-1) increaseBufferSize();
		if (pos > bytes-1) bytes++;
		int val = value % 256; if (val < 0) val += 256;
		buffer[pos++] = (byte)val;
	}
	protected void writeByte(long value) {
		long val = value % 256; if (val < 0) val += 256;
		writeByte((int)val);
	}
	public void writeUXBytes(long value, int x) {
		for (int i = 0; i < x; i++) {
			writeByte(value);
			value >>= 8;
		}
	}
	public void writeXBytes(long value, int x) {writeUXBytes(value+((long)(Math.pow(256,x))>>1),x);}
	
	public void writeSByte(int value) {writeXBytes(value,1);}
	public void writeUShort(int value) {writeUXBytes(value,2);}
	public void writeShort(int value) {writeXBytes(value,2);}
	public void writeUInt(long value) {writeUXBytes(value,4);}
	public void writeInt(int value) {writeXBytes(value,4);}
	public void writeFloat(float value) {writeUXBytes(Float.floatToRawIntBits(value),4);}
	public void writeDouble(double value) {writeUXBytes(Double.doubleToRawLongBits(value),8);}
	public void writeChars(String text) {for (int i = 0; i < text.length(); i++) writeByte(text.charAt(i));}
	public void writeUChars(String text) {for (int i = 0; i < text.length(); i++) writeUShort(text.charAt(i));}
	public void writeString(String text) {writeChars(text); writeByte(0);}
	public void writeString2(String text) {writeUInt(text.length()); writeChars(text);}
	public void writeUString(String text) {writeUChars(text); writeUShort(0);}
	public void writeUString2(String text) {writeUInt(text.length()); writeUChars(text);}
	
	public int readByte() {
		if (pos > buffer.length-1) return 0;
		return buffer[pos++] & 0xff;
	}
	public long readUXBytes(int x) {
		long value = 0;
		for (int i = 0; i < x; i++) value += readByte()*Math.pow(256,i);
		return value;
	}
	public long readXBytes(int x) {return readUXBytes(x)-((long)(Math.pow(256,x))>>1);}
	
	public int readSByte() {return (int)readXBytes(1);}
	public int readUShort() {return (int)readUXBytes(2);}
	public int readShort() {return (int)readXBytes(2);}
	public long readUInt() {return readUXBytes(4);}
	public int readInt() {return (int)readXBytes(4);}
	public float readFloat() {return Float.intBitsToFloat((int)readUXBytes(4));}
	public double readDouble() {return Double.longBitsToDouble(readUXBytes(8));}
	public String readChars(int length) {StringBuilder s = new StringBuilder(); for (int i = 0; i < length; i++) s.append((char)readByte()); return s.toString();}
	public String readUChars(int length) {StringBuilder s = new StringBuilder(); for (int i = 0; i < length; i++) s.append((char)readUShort()); return s.toString();}
	public String readString() {StringBuilder s = new StringBuilder(); int v; while ((v = readByte()) != 0) s.append((char)v); return s.toString();}
	public String readString2() {return readChars((int)readUInt());}
	public String readUString() {StringBuilder s = new StringBuilder(); int v; while ((v = readUShort()) != 0) s.append((char)v); return s.toString();}
	public String readUString2() {return readUChars((int)readUInt());}
	public String readWholeString() {StringBuilder s = new StringBuilder(); while (bytesLeft() > 0) s.append((char)readByte()); return s.toString();}
	public String readWholeUString() {StringBuilder s = new StringBuilder(); while (bytesLeft() > 0) s.append((char)readUShort()); return s.toString();}
	
	public void fillByte(int value) {for (int i = 0; i < getSizeReal(); i++) writeByte(value);}
	
	public BinBuffer copy() {return copy(bytesLeft());}
	public BinBuffer copy(int bytes) {
		BinBuffer binb = new BinBuffer();
		for (int i = 0; i < bytes; i++) binb.writeByte(readByte());
		return binb;
	}
	
	public void writeBinBuffer(BinBuffer binb) {writeBinBuffer(binb,binb.bytesLeft());}
	public void writeBinBuffer(BinBuffer binb, int bytes) {
		for (int i = 0; i < bytes; i++) writeByte(binb.readByte());
	}
	
	public ByteBuffer getByteBuffer() {
		return ByteBuffer.wrap(buffer,getPos(),getSize());
	}
}