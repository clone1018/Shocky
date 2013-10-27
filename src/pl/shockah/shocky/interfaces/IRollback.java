package pl.shockah.shocky.interfaces;

import java.util.List;

import pl.shockah.shocky.lines.Line;

public interface IRollback extends IModule {
	List<Line> getRollbackLines(String channel, String user, String regex, String cull, boolean newest, int lines, int seconds);
	
	<T extends Line> List<T> getRollbackLines(Class<T> type, String channel, String user, String regex, String cull, boolean newest, int lines, int seconds);
	
	<T extends Line> T getRollbackLine(ILinePredicate<T> predicate, Class<T> type, String channel, String user, String regex, String cull, boolean newest, int lines, int seconds);
}
