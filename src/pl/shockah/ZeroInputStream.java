package pl.shockah;

import java.io.InputStream;

public class ZeroInputStream extends InputStream {
	public int read() {
		return -1;
	}
}