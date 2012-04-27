package pl.shockah.nbt;

import pl.shockah.BinBuffer;

public class TagInt extends Tag {
	int value;
	
	public void readData(BinBuffer buffer) {
		super.readData(buffer);
		value = buffer.readInt();
	}
	
	public void writeData(BinBuffer buffer) {
		super.writeData(buffer);
		buffer.writeInt(value);
	}
}