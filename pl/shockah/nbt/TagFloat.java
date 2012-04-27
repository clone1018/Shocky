package pl.shockah.nbt;

import pl.shockah.BinBuffer;

public class TagFloat extends Tag {
	float value;
	
	public void readData(BinBuffer buffer) {
		super.readData(buffer);
		value = buffer.readFloat();
	}
	
	public void writeData(BinBuffer buffer) {
		super.writeData(buffer);
		buffer.writeFloat(value);
	}
}