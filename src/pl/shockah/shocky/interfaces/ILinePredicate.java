package pl.shockah.shocky.interfaces;

import pl.shockah.shocky.lines.Line;

public interface ILinePredicate<T extends Line> {
	boolean accepts(T line);
}
