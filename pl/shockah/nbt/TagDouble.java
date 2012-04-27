package pl.shockah.nbt;

import pl.shockah.BinBuffer;

public class TagDouble extends Tag {
	double value;
	
	public void readData(BinBuffer buffer) {
		super.readData(buffer);
		value = buffer.readDouble();
	}
	
	public void writeData(BinBuffer buffer) {
		super.writeData(buffer);
		buffer.writeDouble(value);
	}
}