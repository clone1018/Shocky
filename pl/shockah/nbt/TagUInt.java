package pl.shockah.nbt;

import pl.shockah.BinBuffer;

public class TagUInt extends Tag {
	long value;
	
	public void readData(BinBuffer buffer) {
		super.readData(buffer);
		value = buffer.readUInt();
	}
	
	public void writeData(BinBuffer buffer) {
		super.writeData(buffer);
		buffer.writeUInt(value);
	}
}