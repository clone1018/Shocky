package pl.shockah.shocky;

import pl.shockah.shocky.sql.Factoid;

public interface IFactoidData {
	String getData(Factoid f);
	boolean setData(Factoid f, CharSequence data);
}
