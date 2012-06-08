import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;

import pl.shockah.StringTools;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.lines.LineMessage;


public class ModuleRegexReplace extends Module {
	
	@Override
	public void onEnable() {
	}
	@Override
	public void onDisable() {
	}

	@Override
	public String name() {return "regexreplace";}
	public boolean isListener() {return true;}

	@Override
	public void onMessage(MessageEvent<PircBotX> event) throws Exception {
		if (Data.isBlacklisted(event.getUser())) return;
		String s = event.getMessage();
		if (!s.startsWith("s/")) return;
		String[] args = s.split("/");
		if (args.length < 3 || args.length > 4) return;
		if (args[1].isEmpty()) return;
		int flags = 0;
		boolean single = true;
		if (args.length == 4 && !args[3].isEmpty()) {
			for (char c : args[3].toCharArray()) {
				switch (c) {
				case 'd': flags |= Pattern.UNIX_LINES; break;
				case 'g': single = false; break;
				case 'i': flags |= Pattern.CASE_INSENSITIVE; break;
				case 'm': flags |= Pattern.MULTILINE; break;
				case 's': flags |= Pattern.DOTALL; break;
				case 'u': flags |= Pattern.UNICODE_CASE; break;
				case 'x': flags |= Pattern.COMMENTS; break;
				}
			}
		}
		Module module = Module.getModule("rollback");
		if (module == null) return;
		Pattern pattern = Pattern.compile(args[1],flags);
		Matcher matcher = pattern.matcher("");
		Method method = module.getClass().getDeclaredMethod("getRollbackLines", Class.class, String.class, String.class, String.class, String.class, boolean.class, int.class, int.class);
		@SuppressWarnings("unchecked")
		ArrayList<LineMessage> lines = (ArrayList<LineMessage>) method.invoke(module, LineMessage.class, event.getChannel().getName(), null, null, s, true, 10, 0);
		
		final ExecutorService service = Executors.newFixedThreadPool(1);
		try {
		Future<String> run = service.submit(new Run(lines, matcher, single, args[2]));
		String output = run.get(5, TimeUnit.SECONDS);
		if (output != null)
			Shocky.sendChannel(event.getBot(), event.getChannel(), output);
		} catch(TimeoutException e) {
		}
		finally {
			service.shutdown();
		}
	}
	
	private static class Run implements Callable<String> {
		private final List<LineMessage> lines;
		private final Matcher matcher;
		private final boolean single;
		private final String replacement;
		
		public Run(List<LineMessage> lines, Matcher matcher, boolean single, String replacement) {
			this.lines = lines;
			this.matcher = matcher;
			this.single = single;
			this.replacement = replacement;
		}

		@Override
		public String call() throws Exception {
			boolean found = false;
			StringBuffer sb = new StringBuffer();
			for (int i = lines.size()-1; i>=0 && !found; i--) {
				LineMessage line = lines.get(i);
				matcher.reset(line.text);
				while (matcher.find() && !(single && found)) {
					found = true;
					matcher.appendReplacement(sb, replacement);
				}
				if (found) {
					matcher.appendTail(sb);
					return StringTools.limitLength(sb);
				}
			}
			return null;
		}
	}
}
