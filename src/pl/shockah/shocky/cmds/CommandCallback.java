package pl.shockah.shocky.cmds;

import pl.shockah.shocky.cmds.Command.EType;

public class CommandCallback implements CharSequence {
	public EType type = EType.Channel;
	public final StringBuilder output;

	public CommandCallback(StringBuilder sb) {
		output = sb;
	}

	public CommandCallback(CharSequence cs) {
		this(new StringBuilder(cs));
	}

	public CommandCallback() {
		this(new StringBuilder());
	}

	@Override
	public int length() {
		return output.length();
	}

	@Override
	public char charAt(int index) {
		return output.charAt(index);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return output.subSequence(start, end);
	}

	@Override
	public String toString() {
		return output.toString();
	}

	public StringBuilder append(Object obj) {
		return output.append(obj);
	}

	public StringBuilder append(String str) {
		return output.append(str);
	}

	public StringBuilder append(StringBuffer sb) {
		return output.append(sb);
	}

	public StringBuilder append(CharSequence s) {
		return output.append(s);
	}

	public StringBuilder append(CharSequence s, int start, int end) {
		return output.append(s, start, end);
	}

	public StringBuilder append(char str[]) {
		return output.append(str);
	}

	public StringBuilder append(char str[], int offset, int len) {
		return output.append(str, offset, len);
	}

	public StringBuilder append(boolean b) {
		return output.append(b);
	}

	public StringBuilder append(char c) {
		return output.append(c);
	}

	public StringBuilder append(int i) {
		return output.append(i);
	}

	public StringBuilder append(long lng) {
		return output.append(lng);
	}

	public StringBuilder append(float f) {
		return output.append(f);
	}

	public StringBuilder append(double d) {
		return output.append(d);
	}

	public StringBuilder insert(int index, char str[], int offset, int len) {
		return output.insert(index, str, offset, len);
	}

	public StringBuilder insert(int offset, Object obj) {
		return output.insert(offset, obj);
	}

	public StringBuilder insert(int offset, String str) {
		return output.insert(offset, str);
	}

	public StringBuilder insert(int offset, char str[]) {
		return output.insert(offset, str);
	}

	public StringBuilder insert(int dstOffset, CharSequence s) {
		return output.insert(dstOffset, s);
	}

	public StringBuilder insert(int dstOffset, CharSequence s, int start,
			int end) {
		return output.insert(dstOffset, s, start, end);
	}

	public StringBuilder insert(int offset, boolean b) {
		return output.insert(offset, b);
	}

	public StringBuilder insert(int offset, char c) {
		return output.insert(offset, c);
	}

	public StringBuilder insert(int offset, int i) {
		return output.insert(offset, i);
	}

	public StringBuilder insert(int offset, long l) {
		return output.insert(offset, l);
	}

	public StringBuilder insert(int offset, float f) {
		return output.insert(offset, f);
	}

	public StringBuilder insert(int offset, double d) {
		return output.insert(offset, d);
	}
}
