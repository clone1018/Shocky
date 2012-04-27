package pl.shockah.nbt;

import java.util.ArrayList;

import pl.shockah.BinBuffer;

public class TagList extends Tag {
	public ArrayList<Tag> tags = new ArrayList<Tag>();
	public int tagType;
	
	public void readData(BinBuffer buffer) {
		super.readData(buffer);
		
		tagType = buffer.readByte();
		int length = buffer.readUShort();
		for (int i = 0; i < length; i++) {
			Tag tag = getTag(tagType);
			buffer.readByte();
			tag.readData(buffer);
		}
	}
	
	public void writeData(BinBuffer buffer) {
		super.writeData(buffer);
		
		buffer.writeByte(tagType);
		buffer.writeUShort(tags.size());
		for (Tag tag : tags) tag.writeData(buffer);
	}
}