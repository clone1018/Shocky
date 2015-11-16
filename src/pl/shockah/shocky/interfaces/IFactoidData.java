package pl.shockah.shocky.interfaces;

import pl.shockah.shocky.sql.Factoid;

public interface IFactoidData {
	String getData(Factoid f, boolean acquire);
	boolean setData(Factoid f, CharSequence data);
	void release(Factoid f);
}
