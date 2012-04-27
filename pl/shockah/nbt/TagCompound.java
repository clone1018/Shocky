package pl.shockah.nbt;

import java.util.ArrayList;
import pl.shockah.BinBuffer;
import pl.shockah.Helper;

public class TagCompound extends Tag {
	private ArrayList<Tag> tags = new ArrayList<Tag>();
	
	public void readData(BinBuffer buffer) {
		super.readData(buffer);
		
		Tag tag;
		while ((tag = getTag(buffer.readByte())) != null) tag.readData(buffer);
	}
	
	public void writeData(BinBuffer buffer) {
		super.writeData(buffer);
		for (Tag tag : tags) {
			tag.writeBase(buffer);
			tag.writeData(buffer);
		}
		buffer.writeByte(0);
	}
	
	public boolean tagExists(String name) {
		for (Tag tag : tags) if (tag.name.equals(name)) return true;
		return false;
	}
	public void tagReplace(String name, Tag tag) {
		for (int i = 0; i < tags.size(); i++)
		if (tags.get(i).name.equals(name)) {
			tags.set(i,tag); break;
		}
	}
	public void tagRemove(String name) {
		for (int i = 0; i < tags.size(); i++) if (tags.get(i).name.equals(name)) tags.remove(i--);
	}
	public void tagsClear() {
		tags.clear();
	}
	public Tag tagGet(String name) {
		for (Tag tag : tags) if (tag.name.equals(name)) return tag;
		return null;
	}
	
	public TagCompound getCompound(String name) {
		TagCompound compound = (TagCompound)tagGet(name);
		if (compound == null) {
			compound = new TagCompound();
			compound.name = name;
		}
		return compound;
	}
	public int getByte(String name) {Tag tag = tagGet(name); return tag != null ? ((TagByte)tag).value : 0;}
	public int getShort(String name) {Tag tag = tagGet(name); return tag != null ? ((TagShort)tag).value : 0;}
	public int getUShort(String name) {Tag tag = tagGet(name); return tag != null ? ((TagUShort)tag).value : 0;}
	public int getInt(String name) {Tag tag = tagGet(name); return tag != null ? ((TagInt)tag).value : 0;}
	public long getUInt(String name) {Tag tag = tagGet(name); return tag != null ? ((TagUInt)tag).value : 0;}
	public float getFloat(String name) {Tag tag = tagGet(name); return tag != null ? ((TagFloat)tag).value : 0;}
	public double getDouble(String name) {Tag tag = tagGet(name); return tag != null ? ((TagDouble)tag).value : 0;}
	public String getString(String name) {Tag tag = tagGet(name); return tag != null ? ((TagString)tag).value : "";}
	public TagList getList(String name, int tagType) {
		TagList list = (TagList)tagGet(name);
		if (list == null) {
			list = new TagList();
			list.name = name; list.tagType = tagType;
		}
		return list;
	}
	public <T extends Tag> ArrayList<T> getListElements(String name, Class<T> tagType) {
		return Helper.getAllOfType(getList(name,getID(tagType)).tags,tagType);
	}
	
	public void setCompound(String name, TagCompound compound) {
		if (tagExists(name)) tagReplace(name,compound); else {
			compound.name = name; tags.add(compound);
		}
	}
	public void setByte(String name, int value) {
		if (tagExists(name)) ((TagByte)tagGet(name)).value = value; else {
			TagByte tag = new TagByte();
			tag.name = name; tag.value = value; tags.add(tag);
		}
	}
	public void setShort(String name, int value) {
		if (tagExists(name)) ((TagShort)tagGet(name)).value = value; else {
			TagShort tag = new TagShort();
			tag.name = name; tag.value = value; tags.add(tag);
		}
	}
	public void setUShort(String name, int value) {
		if (tagExists(name)) ((TagUShort)tagGet(name)).value = value; else {
			TagUShort tag = new TagUShort();
			tag.name = name; tag.value = value; tags.add(tag);
		}
	}
	public void setInt(String name, int value) {
		if (tagExists(name)) ((TagInt)tagGet(name)).value = value; else {
			TagInt tag = new TagInt();
			tag.name = name; tag.value = value; tags.add(tag);
		}
	}
	public void setUInt(String name, long value) {
		if (tagExists(name)) ((TagUInt)tagGet(name)).value = value; else {
			TagUInt tag = new TagUInt();
			tag.name = name; tag.value = value; tags.add(tag);
		}
	}
	public void setFloat(String name, float value) {
		if (tagExists(name)) ((TagFloat)tagGet(name)).value = value; else {
			TagFloat tag = new TagFloat();
			tag.name = name; tag.value = value; tags.add(tag);
		}
	}
	public void setDouble(String name, double value) {
		if (tagExists(name)) ((TagDouble)tagGet(name)).value = value; else {
			TagDouble tag = new TagDouble();
			tag.name = name; tag.value = value; tags.add(tag);
		}
	}
	public void setString(String name, String value) {
		if (tagExists(name)) ((TagString)tagGet(name)).value = value; else {
			TagString tag = new TagString();
			tag.name = name; tag.value = value; tags.add(tag);
		}
	}
	public void setList(String name, TagList list) {
		if (tagExists(name)) tagReplace(name,list); else {
			list.name = name; tags.add(list);
		}
	}
}