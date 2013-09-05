package pl.shockah.shocky.interfaces;

import java.util.ArrayList;

import pl.shockah.shocky.lines.Line;

public interface IRollback {
	ArrayList<Line> getRollbackLines(String channel, String user, String regex, String cull, boolean newest, int lines, int seconds);
	<T extends Line> ArrayList<T> getRollbackLines(Class<T> type, String channel, String user, String regex, String cull, boolean newest, int lines, int seconds);
}
