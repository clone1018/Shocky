package pl.shockah.nbt;

import pl.shockah.BinBuffer;

public abstract class Tag {
	public static Tag getTag(int value) {
		switch (value) {
			case 1: return new TagCompound();
			case 2: return new TagByte();
			case 3: return new TagShort();
			case 4: return new TagUShort();
			case 5: return new TagInt();
			case 6: return new TagUInt();
			case 7: return new TagFloat();
			case 8: return new TagDouble();
			case 9: return new TagString();
			case 10: return new TagList();
			default: return null;
		}
	}
	public static int getID(Tag tag) {
		return getID(tag.getClass());
	}
	public static int getID(Class<? extends Tag> cls) {
		if (cls == TagCompound.class) return 1;
		if (cls == TagByte.class) return 2;
		if (cls == TagShort.class) return 3;
		if (cls == TagUShort.class) return 4;
		if (cls == TagInt.class) return 5;
		if (cls == TagUInt.class) return 6;
		if (cls == TagFloat.class) return 7;
		if (cls == TagDouble.class) return 8;
		if (cls == TagString.class) return 9;
		if (cls == TagList.class) return 10;
		return 0;
	}
	
	public static TagCompound readNBT(BinBuffer buffer) {
		TagCompound compound = null;
		if (buffer.bytesLeft() > 0) if (buffer.readByte() == 1) {
			compound = new TagCompound();
			compound.readBase(buffer);
			compound.readData(buffer);
		}
		return compound;
	}
	public static void writeNBT(TagCompound compound, BinBuffer buffer) {
		compound.writeBase(buffer);
		compound.writeData(buffer);
	}
	
	public String name = "";
	
	public void readBase(BinBuffer buffer) {
		name = buffer.readChars(buffer.readUShort());
	}
	public void readData(BinBuffer buffer) {}
	
	public void writeBase(BinBuffer buffer) {
		buffer.writeByte(getID(this));
		buffer.writeUShort(name.length());
		buffer.writeChars(name);
	}
	public void writeData(BinBuffer buffer) {}
}