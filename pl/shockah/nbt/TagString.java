package pl.shockah.nbt;

import pl.shockah.BinBuffer;

public class TagString extends Tag {
	String value = "";
	
	public void readData(BinBuffer buffer) {
		super.readData(buffer);
		value = buffer.readChars(buffer.readUShort());
	}
	
	public void writeData(BinBuffer buffer) {
		super.writeData(buffer);
		
		buffer.writeUShort(value.length());
		buffer.writeChars(value);
	}
}