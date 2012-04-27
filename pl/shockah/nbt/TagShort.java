package pl.shockah.nbt;

import pl.shockah.BinBuffer;

public class TagShort extends Tag {
	int value;
	
	public void readData(BinBuffer buffer) {
		super.readData(buffer);
		value = buffer.readShort();
	}
	
	public void writeData(BinBuffer buffer) {
		super.writeData(buffer);
		buffer.writeShort(value);
	}
}