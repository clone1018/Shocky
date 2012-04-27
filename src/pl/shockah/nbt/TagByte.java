package pl.shockah.nbt;

import pl.shockah.BinBuffer;

public class TagByte extends Tag {
	int value;
	
	public void readData(BinBuffer buffer) {
		super.readData(buffer);
		value = buffer.readByte();
	}
	
	public void writeData(BinBuffer buffer) {
		super.writeData(buffer);
		buffer.writeByte(value);
	}
}