package pl.shockah.nbt;

import pl.shockah.BinBuffer;

public class TagUShort extends Tag {
	int value;
	
	public void readData(BinBuffer buffer) {
		super.readData(buffer);
		value = buffer.readUShort();
	}
	
	public void writeData(BinBuffer buffer) {
		super.writeData(buffer);
		buffer.writeUShort(value);
	}
}